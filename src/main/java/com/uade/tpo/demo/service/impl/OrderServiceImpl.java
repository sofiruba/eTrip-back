package com.uade.tpo.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.GiftRequestDTO;
import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.dtos.response.OrderStatsDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.CartItem;
import com.uade.tpo.demo.entity.DiscountCoupon;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Order;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.repository.CartRepository;
import com.uade.tpo.demo.repository.DiscountCouponRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.OrderRepository;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.BookingService;
import com.uade.tpo.demo.service.MailService;
import com.uade.tpo.demo.service.NotificationService;
import com.uade.tpo.demo.service.OrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** Comisión que se queda PLAN sobre cada venta (10%). */
    public static final BigDecimal PLAN_COMMISSION_RATE = new BigDecimal("0.10");
    /** Tarjeta de prueba que el "banco" simulado siempre rechaza. */
    private static final String DECLINED_TEST_CARD = "4000000000000002";
    private static final DateTimeFormatter SESSION_DATE = DateTimeFormatter.ofPattern("EEE d/M 'a las' HH:mm", Locale.forLanguageTag("es-AR"));

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final DiscountCouponRepository discountCouponRepository;
    private final BookingRepository bookingRepository;
    private final ExperienceSessionRepository experienceSessionRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final MailService mailService;

    // Mis órdenes; ADMIN ve las de todos.
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrders(User user, Pageable pageable) {
        Page<Order> orders = isAdmin(user)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByUserId(user.getId(), pageable);
        return orders.map(order -> toResponse(order, user));
    }

    // Una orden puntual; solo el dueño o un ADMIN.
    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("La orden no existe"));

        boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        if (!isOwner && !isAdmin(user)) {
            throw new ForbiddenException("No tenés acceso a esta orden");
        }

        return toResponse(order, user);
    }

    // Ventas, reembolsos y comisión de PLAN sobre todas las órdenes; solo ADMIN.
    @Override
    @Transactional(readOnly = true)
    public OrderStatsDTO getStats(User user) throws ForbiddenException {
        if (!isAdmin(user)) {
            throw new ForbiddenException("Solo un administrador puede ver las estadísticas");
        }
        List<Order> orders = orderRepository.findAll();
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        for (Order order : orders) {
            gross = gross.add(order.getTotal());
            refunded = refunded.add(refundedAmount(order));
            commission = commission.add(planCommission(order));
        }
        return OrderStatsDTO.builder()
                .orderCount(orders.size())
                .grossSales(gross)
                .refunded(refunded)
                .netSales(gross.subtract(refunded))
                .commission(commission)
                .commissionRate(PLAN_COMMISSION_RATE.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP))
                .build();
    }

    // Cancela una orden entera: reembolsa cada reserva que todavía no empezó, usando la misma regla que el
    // reembolso de una reserva suelta (BookingService.refundBooking). Las que ya pasaron quedan como están.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrderResponseDTO cancelOrder(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        if (!isAdmin(user)) {
            throw new ForbiddenException("Solo un administrador puede cancelar una orden completa");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("La orden no existe"));

        int refunded = 0;
        for (Booking booking : order.getBookings()) {
            ExperienceSession session = booking.getExperienceSession();
            boolean started = session == null || session.getStartsAt().isBefore(LocalDateTime.now());
            if (!booking.isRefunded() && !started) {
                bookingService.refundBooking(booking.getId(), user);
                refunded++;
            }
        }
        if (refunded == 0) {
            throw new BadRequestException("Esta orden no tiene reservas para cancelar: ya se reembolsaron o ya pasaron");
        }
        return toResponse(orderRepository.findById(orderId).orElseThrow(), user);
    }

    // Estado de la orden según sus reservas: PAID, PARTIALLY_REFUNDED o CANCELLED.
    private String orderStatus(Order order) {
        List<Booking> bookings = order.getBookings() != null ? order.getBookings() : List.of();
        long refundedCount = bookings.stream().filter(Booking::isRefunded).count();
        if (refundedCount == 0) {
            return "PAID";
        }
        return refundedCount == bookings.size() ? "CANCELLED" : "PARTIALLY_REFUNDED";
    }

    // Confirma el carrito: valida el pago, el cupón, los lugares y el regalo, crea una reserva (voucher) por ítem,
    // descuenta los lugares y vacía el carrito. Todo en una transacción: si algo falla, no se cobra nada.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrderResponseDTO createOrder(User user, OrderRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Las cuentas de administrador no pueden comprar experiencias");
        }
        // El pago se valida antes de tocar nada: un rechazo no cambia los lugares
        simulatePayment(request);
        // Va antes que los lugares porque bloquea el cupón: dos compras a la vez no lo pueden usar dos veces
        DiscountCoupon coupon = resolveCoupon(request, user);
        GiftRequestDTO gift = cleanGift(request != null ? request.getGift() : null, user);

        List<CartItem> items = cartRepository.findByUserId(user.getId())
                .map(cart -> new ArrayList<>(cart.getItems()))
                .orElseGet(ArrayList::new);
        if (items.isEmpty()) {
            throw new BadRequestException("Tu carrito está vacío");
        }

        // Bloquea las fechas del carrito (SELECT ... FOR UPDATE) y las vuelve a leer: si dos personas compran
        // el último lugar a la vez, la segunda espera y ve los lugares ya descontados. En orden de id, para no trabarse.
        Map<Long, ExperienceSession> sessionsById = new TreeMap<>();
        items.forEach(item -> sessionsById.putIfAbsent(item.getExperienceSession().getId(), item.getExperienceSession()));
        sessionsById.values().forEach(session -> entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            Experience experience = session.getExperience();
            String what = describe(session);
            if (experience.getPublisher().getId().equals(user.getId())) {
                throw new BadRequestException("No podés comprar tu propia experiencia (" + what + "). Sacala del carrito.");
            }
            if (!experience.isActive() || experience.getDeletedAt() != null || !experience.getPublisher().isActive()) {
                throw new BadRequestException(what + " ya no está publicada. Sacala del carrito.");
            }
            if (!session.isActive()) {
                throw new BadRequestException("Se canceló la fecha de " + what + ". Sacala del carrito.");
            }
            if (session.getStartsAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException(what + " ya empezó. Sacala del carrito.");
            }
            if (session.getAvailableSeats() < item.getQuantity()) {
                throw new BadRequestException(session.getAvailableSeats() == 0
                        ? "Se agotaron los lugares de " + what + "."
                        : "Para " + what + " quedan " + session.getAvailableSeats() + " lugares. Cambiá la cantidad.");
            }
            subtotal = subtotal.add(experience.getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountAmount = coupon == null ? BigDecimal.ZERO
                : subtotal.multiply(coupon.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discountAmount);
        // Si el total cambió mientras pagaba (el anfitrión editó un precio), no se cobra otro monto sin avisar
        if (request != null && request.getExpectedTotal() != null
                && request.getExpectedTotal().subtract(total).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BadRequestException("El precio de alguna experiencia cambió mientras pagabas. "
                    + "Revisá el nuevo total y confirmá de nuevo.");
        }

        // Un regalo se entrega en el momento solo si el email es de una cuenta VERIFICADA. Si no, queda pendiente
        // (el voucher sigue siendo de quien pagó) hasta que esa persona cree su cuenta y verifique el email.
        User recipient = gift != null && gift.getRecipientEmail() != null ? verifiedRecipient(gift.getRecipientEmail(), user) : null;
        Order order = orderRepository.save(Order.builder()
                .user(user)
                .discountCoupon(coupon)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .total(total)
                .createdAt(LocalDateTime.now())
                .giftRecipientName(gift != null ? gift.getRecipientName() : null)
                .giftRecipientEmail(gift != null ? gift.getRecipientEmail() : null)
                .giftMessage(gift != null ? gift.getMessage() : null)
                .build());

        List<Booking> bookings = new ArrayList<>();
        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            Experience experience = session.getExperience();
            session.setAvailableSeats(session.getAvailableSeats() - item.getQuantity());
            experienceSessionRepository.save(session);
            bookings.add(bookingRepository.save(Booking.builder()
                    .order(order)
                    .experienceSession(session)
                    .quantity(item.getQuantity())
                    .unitPrice(experience.getEffectivePrice())
                    .voucherCode(generateVoucherCode())
                    .createdAt(LocalDateTime.now())
                    .recipient(recipient)
                    .build()));
            notificationService.create(experience.getPublisher(), "Nueva reserva",
                    user.getFirstName() + " reservó \"" + experience.getTitle() + "\".");
        }
        cartItemRepository.deleteAll(items);

        String titles = bookings.stream().map(b -> "\"" + b.getExperienceSession().getExperience().getTitle() + "\"")
                .collect(Collectors.joining(", "));
        if (gift == null) {
            notificationService.create(user, "Reserva confirmada", "Tu reserva para " + titles + " está confirmada.");
        } else {
            notificationService.create(user, "Regalo confirmado", "Tu regalo para " + gift.getRecipientName() + " ("
                    + titles + ") está confirmado. " + (recipient != null ? "Ya está en sus reservas."
                            : gift.getRecipientEmail() != null
                                    ? "Se lo pasamos cuando entre a PLAN con " + gift.getRecipientEmail()
                                            + " y lo verifique. Hasta entonces el voucher es tuyo."
                                    : "El voucher está en Mis reservas para que se lo des."));
            if (recipient != null) {
                notificationService.create(recipient, "¡Te regalaron una experiencia!",
                        user.getFirstName() + " te regaló " + titles + ". Ya está en tus reservas, con el voucher.");
            }
        }

        // Regalo con email: se le avisa por mail a quien lo recibe (sin el voucher). Va al final, con todo guardado;
        // si el mail falla, la compra sigue (ya está pagada) y se le avisa a quien regaló.
        if (gift != null && gift.getRecipientEmail() != null) {
            try {
                mailService.sendGift(gift.getRecipientEmail(), gift.getRecipientName(), user, gift.getMessage(), bookings,
                        recipient != null);
            } catch (MailException e) {
                log.warn("No se pudo mandar el mail del regalo de la orden {}", order.getId(), e);
                notificationService.create(user, "No pudimos mandar el mail del regalo",
                        "El regalo para " + gift.getRecipientName() + " está confirmado, pero no le pudimos mandar el mail. "
                                + "Revisá el email desde la reserva y probá de nuevo.");
            }
        }

        order.setBookings(bookings);
        return toResponse(order, user);
    }

    // "“Trekking al Refugio Frey” del sáb 22/11 a las 09:30" (para los mensajes de error del checkout)
    private String describe(ExperienceSession session) {
        return "\"" + session.getExperience().getTitle() + "\" del " + session.getStartsAt().format(SESSION_DATE);
    }

    // Valida los datos del regalo: para quién es (obligatorio), su email (opcional) y la dedicatoria (hasta 300 caracteres).
    private GiftRequestDTO cleanGift(GiftRequestDTO gift, User buyer) throws BadRequestException {
        if (gift == null) {
            return null;
        }
        String name = gift.getRecipientName() != null ? gift.getRecipientName().trim() : "";
        String email = gift.getRecipientEmail() != null ? gift.getRecipientEmail().trim().toLowerCase() : "";
        String message = gift.getMessage() != null ? gift.getMessage().trim() : "";
        if (name.isEmpty() && email.isEmpty() && message.isEmpty()) {
            return null; // no es un regalo
        }
        if (name.isEmpty() || name.length() > 100) {
            throw new BadRequestException("Decinos para quién es el regalo (hasta 100 caracteres)");
        }
        if (!email.isEmpty() && (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new BadRequestException("El email de quien recibe el regalo no es válido");
        }
        if (email.equalsIgnoreCase(buyer.getEmail())) {
            throw new BadRequestException("Ese es tu propio email: poné el de quien recibe el regalo");
        }
        if (message.length() > 300) {
            throw new BadRequestException("La dedicatoria puede tener hasta 300 caracteres");
        }
        return GiftRequestDTO.builder()
                .recipientName(name)
                .recipientEmail(email.isEmpty() ? null : email)
                .message(message.isEmpty() ? null : message)
                .build();
    }

    // Busca y valida el cupón del request (mismo criterio que GET /discount-coupons/validate),
    // y que este usuario no lo haya usado ya en una orden anterior.
    private DiscountCoupon resolveCoupon(OrderRequestDTO request, User user)
            throws ResourceNotFoundException, BadRequestException {
        if (request == null || request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            return null;
        }

        DiscountCoupon coupon = discountCouponRepository
                .findByCodeForUpdate(request.getCouponCode().trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("No existe un cupón con ese código"));

        String reason = DiscountCouponServiceImpl.reasonIfInvalid(coupon);
        if (reason != null) {
            throw new BadRequestException(DiscountCouponServiceImpl.describeReason(reason));
        }

        if (orderRepository.hasUsedCoupon(user.getId(), coupon.getId())) {
            throw new BadRequestException(DiscountCouponServiceImpl.describeReason(DiscountCouponServiceImpl.ALREADY_USED));
        }

        return coupon;
    }

    // Simula la pasarela de pago (no hay cobro real): valida el medio elegido y rechaza la tarjeta de prueba.
    private void simulatePayment(OrderRequestDTO request) throws BadRequestException {
        String method = request != null ? request.getPaymentMethod() : null;
        if (!"card".equals(method)) {
            return; // tarjeta guardada o billetera: pago simulado, siempre aprobado
        }
        String digits = request.getCardNumber() != null ? request.getCardNumber().replaceAll("\\D", "") : "";
        if (digits.length() < 13 || digits.length() > 19 || !passesLuhn(digits)) {
            throw new BadRequestException("El número de tarjeta no es válido. Revisalo.");
        }
        if (DECLINED_TEST_CARD.equals(digits)) {
            throw new BadRequestException("El banco rechazó la tarjeta. Probá con otra tarjeta o con otro medio de pago.");
        }
    }

    // Dígito verificador de Luhn (detecta errores de tipeo en el número).
    private boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleIt) {
                digit = digit * 2 > 9 ? digit * 2 - 9 : digit * 2;
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    // Lo devuelto de una orden: cada reserva cancelada devuelve lo que se pagó por ella (con el cupón repartido).
    private BigDecimal refundedAmount(Order order) {
        return order.getBookings().stream().filter(Booking::isRefunded)
                .map(BookingServiceImpl::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Lo que le queda a PLAN de una orden: lo cobrado menos lo de los anfitriones (los cupones los absorbe PLAN).
    private BigDecimal planCommission(Order order) {
        return order.getBookings().stream()
                .map(BookingServiceImpl::planCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // La comisión de PLAN sobre un monto (10 %).
    public static BigDecimal commissionOf(BigDecimal amount) {
        return amount.multiply(PLAN_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // Genera el código de voucher visible del Booking (ej. ETRIP-A1B2C3D4).
    private String generateVoucherCode() {
        return "ETRIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    // La orden con sus reservas, armadas para quien la mira (ver BookingServiceImpl.toResponse).
    private OrderResponseDTO toResponse(Order order, User viewer) {
        List<BookingResponseDTO> bookings = order.getBookings().stream()
                .map(booking -> BookingServiceImpl.toResponse(booking, viewer))
                .toList();
        return OrderResponseDTO.builder()
                .id(order.getId())
                .status(orderStatus(order))
                .userId(order.getUser().getId())
                .couponCode(order.getDiscountCoupon() != null ? order.getDiscountCoupon().getCode() : null)
                .createdAt(order.getCreatedAt())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .total(order.getTotal())
                .refunded(refundedAmount(order))
                .commission(planCommission(order))
                .bookings(bookings)
                .giftRecipientName(order.getGiftRecipientName())
                .giftRecipientEmail(order.getGiftRecipientEmail())
                .giftMessage(order.getGiftMessage())
                .giftStatus(orderGiftStatus(order))
                .build();
    }

    // Estado del regalo de la orden (null si no es un regalo): DELIVERED si alguna reserva ya tiene dueño
    private String orderGiftStatus(Order order) {
        if (order.getGiftRecipientName() == null) {
            return null;
        }
        boolean delivered = order.getBookings() != null && order.getBookings().stream().anyMatch(b -> b.getRecipient() != null);
        return delivered ? "DELIVERED" : order.getGiftRecipientEmail() != null ? "PENDING" : "IN_PERSON";
    }

    // Cuenta verificada y activa con ese email (y que no sea quien compra): a esa sí se le puede entregar el regalo
    private User verifiedRecipient(String email, User buyer) {
        return userRepository.findByEmail(email)
                .filter(found -> !found.getId().equals(buyer.getId()) && found.isEmailVerified() && found.isActive())
                .orElse(null);
    }

    // Corrige el email de un regalo pendiente. Si el email nuevo es de una cuenta verificada, se le entrega ya;
    // si no, queda pendiente. En los dos casos le mandamos el mail a la dirección nueva.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrderResponseDTO updateGiftEmail(Long orderId, String recipientEmail, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        Order order = pendingGift(orderId, user);
        String email = recipientEmail != null ? recipientEmail.trim().toLowerCase() : "";
        if (email.isEmpty() || email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BadRequestException("Ingresá un email válido");
        }
        if (email.equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("Ese es tu propio email");
        }
        order.setGiftRecipientEmail(email);
        List<Booking> active = order.getBookings().stream().filter(b -> !b.isRefunded()).toList();
        User recipient = verifiedRecipient(email, user);
        if (recipient != null) {
            active.forEach(booking -> booking.setRecipient(recipient));
            bookingRepository.saveAll(active);
            notificationService.create(recipient, "¡Te regalaron una experiencia!",
                    user.getFirstName() + " te hizo un regalo. Ya está en tus reservas, con el voucher.");
        }
        orderRepository.save(order);
        // Acá sí: si el mail no sale, no se guarda el cambio y quien regaló ve el error para reintentar
        mailService.sendGift(email, order.getGiftRecipientName(), user, order.getGiftMessage(), active, recipient != null);
        return toResponse(order, user);
    }

    // Regalos hechos a este email antes de que tuviera una cuenta verificada: pasan a ser suyos.
    // Lo llaman la verificación del email, el cambio de contraseña (también verifica) y la reactivación de la cuenta.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void claimPendingGifts(User user) {
        if (!user.isEmailVerified() || !user.isActive()) {
            return;
        }
        Map<Order, List<Booking>> byOrder = new LinkedHashMap<>();
        for (Booking booking : bookingRepository.findByRecipientIsNullAndOrder_GiftRecipientEmailIgnoreCase(user.getEmail())) {
            if (!booking.isRefunded() && !booking.getOrder().getUser().getId().equals(user.getId())) {
                booking.setRecipient(user);
                bookingRepository.save(booking);
                byOrder.computeIfAbsent(booking.getOrder(), order -> new ArrayList<>()).add(booking);
            }
        }
        byOrder.forEach((order, bookings) -> {
            notificationService.create(user, "¡Tenés un regalo esperándote!", order.getUser().getFirstName()
                    + " te regaló " + (bookings.size() == 1 ? "una experiencia" : bookings.size() + " experiencias")
                    + ". Ya está en tus reservas, con el voucher.");
            notificationService.create(order.getUser(), "Regalo entregado",
                    user.getFirstName() + " ya tiene tu regalo en sus reservas.");
        });
    }

    // Quien pagó se queda con un regalo pendiente: deja de ser un regalo y las reservas son suyas, como cualquier compra.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrderResponseDTO cancelGift(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        Order order = pendingGift(orderId, user);
        order.setGiftRecipientName(null);
        order.setGiftRecipientEmail(null);
        order.setGiftMessage(null);
        orderRepository.save(order);
        return toResponse(order, user);
    }

    // Orden de quien pide, que es un regalo todavía sin dueño y con alguna reserva vigente
    private Order pendingGift(Long orderId, User user) throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("La orden no existe"));
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Solo quien hizo el regalo puede cambiarlo");
        }
        if (order.getGiftRecipientName() == null) {
            throw new BadRequestException("Esta compra no es un regalo");
        }
        if (order.getBookings().stream().anyMatch(b -> b.getRecipient() != null)) {
            throw new BadRequestException("El regalo ya es de " + order.getGiftRecipientName() + ": no se puede cambiar");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean anyActive = order.getBookings().stream().anyMatch(b -> !b.isRefunded()
                && b.getExperienceSession() != null && b.getExperienceSession().getStartsAt().isAfter(now));
        if (!anyActive) {
            throw new BadRequestException("El regalo ya no está vigente (la fecha pasó o se reembolsó)");
        }
        return order;
    }
}
