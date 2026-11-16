package com.uade.tpo.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.dtos.response.OrderStatsDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Cart;
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
import com.uade.tpo.demo.dtos.request.GiftRequestDTO;
import com.uade.tpo.demo.service.OrderService;
import com.uade.tpo.demo.service.BookingService;
import com.uade.tpo.demo.service.NotificationService;
import com.uade.tpo.demo.service.MailService;

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
        return orders.map(this::toResponse);
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

        return toResponse(order);
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
        for (Order order : orders) {
            gross = gross.add(orZero(order.getTotal()));
            refunded = refunded.add(refundedAmount(order));
        }
        BigDecimal net = gross.subtract(refunded);
        return OrderStatsDTO.builder()
                .orderCount(orders.size())
                .grossSales(gross)
                .refunded(refunded)
                .netSales(net)
                .commission(commissionOf(net))
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
        return toResponse(orderRepository.findById(orderId).orElseThrow());
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

    // Confirma el carrito del usuario: valida cupos, aplica cupón si viene, genera un Booking
    // (voucher) por cada item, descuenta los cupos y vacía el carrito, todo en una transacción.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrderResponseDTO createOrder(User user, OrderRequestDTO request)
            throws ResourceNotFoundException, BadRequestException {
        // Pago simulado: se valida antes de tocar cupos, así un rechazo no cambia nada.
        simulatePayment(request);

        // Valida y resuelve el cupón del request (si viene) y que este usuario no lo haya usado ya en una orden anterior.
        // Va primero porque bloquea el cupón: así el chequeo de "ya lo usaste" ve las compras que terminaron mientras esperaba.
        DiscountCoupon coupon = resolveCoupon(request, user);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se puede confirmar una compra con el carrito vacío"));

         // Valida que el carrito tenga items; si no, lanza excepción.
        List<CartItem> items = cart.getItems() != null
                ? new ArrayList<>(cart.getItems())
                : new ArrayList<>();
        if (items.isEmpty()) {
            throw new BadRequestException("No se puede confirmar una compra con el carrito vacío");
        }

        // Bloquea las sesiones del carrito hasta que termine la transacción y las vuelve a leer de la base
        // (SELECT ... FOR UPDATE). Si dos personas compran el último lugar a la vez, la segunda espera
        // y ve los cupos ya descontados. Se bloquean en orden de id para que dos compras no se traben entre sí.
        // Se deduplica por id (no con distinct(): el hashCode de las entidades recorre sus relaciones).
        Map<Long, ExperienceSession> sessionsById = new TreeMap<>();
        items.forEach(item -> sessionsById.putIfAbsent(item.getExperienceSession().getId(), item.getExperienceSession()));
        sessionsById.values().forEach(session -> entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE));

        // Valida que cada item tenga cupos disponibles; si no, lanza excepción.
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            if (!session.isActive()) {
                throw new BadRequestException("La sesión " + session.getId() + " ya no está disponible");
            }
            if (session.getStartsAt() == null || session.getStartsAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("La sesión " + session.getId() + " ya comenzó o ya pasó");
            }
            if (session.getAvailableSeats() == null || session.getAvailableSeats() < item.getQuantity()) {
                throw new BadRequestException("No hay cupos suficientes para la sesión " + session.getId());
            }
            // Calcula el subtotal sumando el precio unitario de la experiencia por la cantidad del item.
            Experience experience = session.getExperience();
            if (experience == null || !experience.isActive()) {
                throw new BadRequestException("La experiencia ya no está publicada");
            }
            BigDecimal unitPrice = experience != null && experience.getEffectivePrice() != null
                    ? experience.getEffectivePrice()
                    : BigDecimal.ZERO;
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (coupon != null) {
            discountAmount = subtotal
                    .multiply(coupon.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal total = subtotal.subtract(discountAmount);

        // Si el front dice cuánto le mostró al comprador y no coincide (el anfitrión cambió un precio mientras
        // pagaba), no se cobra un monto distinto sin avisar.
        if (request != null && request.getExpectedTotal() != null
                && request.getExpectedTotal().subtract(total).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BadRequestException("El precio de alguna experiencia cambió mientras pagabas. "
                    + "Revisá el nuevo total y confirmá de nuevo.");
        }
        long totalTickets = items.stream().mapToLong(CartItem::getQuantity).sum();
        // ¿Es un regalo? Se valida acá (antes de tocar cupos) y, si el destinatario tiene cuenta, se le asignan las reservas
        GiftRequestDTO gift = cleanGift(request != null ? request.getGift() : null);
        // Solo se entrega en el momento si ese email es de una cuenta VERIFICADA (probó que el email es suyo).
        // Si no, el regalo queda pendiente: las reservas siguen siendo de quien pagó hasta que esa persona cree su cuenta
        // y verifique el email (AuthenticationService.claimGifts). Así, un email mal escrito no le regala el plan a otro.
        User recipient = gift != null && gift.getRecipientEmail() != null ? verifiedRecipient(gift.getRecipientEmail(), user) : null;
        // Crea la orden, genera un Booking por cada item, descuenta los cupos de las sesiones y vacía el carrito.
        Order order = Order.builder()
                .user(user)
                .discountCoupon(coupon)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .total(total)
                .count(totalTickets)
                .createdAt(LocalDateTime.now())
                .giftRecipientName(gift != null ? gift.getRecipientName() : null)
                .giftRecipientEmail(gift != null ? gift.getRecipientEmail() : null)
                .giftMessage(gift != null ? gift.getMessage() : null)
                .build();
        order = orderRepository.save(order);

        List<Booking> bookings = new ArrayList<>();
        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            Experience experience = session.getExperience();
            session.setAvailableSeats(session.getAvailableSeats() - item.getQuantity());
            experienceSessionRepository.save(session);

            Booking booking = Booking.builder()
                    .order(order)
                    .experienceSession(session)
                    .quantity(item.getQuantity())
                    .unitPrice(experience.getEffectivePrice())
                    .voucherCode(generateVoucherCode())
                    .createdAt(LocalDateTime.now())
                    .recipient(recipient)
                    .build();
            bookings.add(bookingRepository.save(booking));
            if (gift != null) {
                notificationService.create(user, "Regalo confirmado",
                        "Tu regalo de \"" + experience.getTitle() + "\" para " + gift.getRecipientName() + " está confirmado. "
                                + (recipient != null ? "Ya está en sus reservas."
                                        : gift.getRecipientEmail() != null
                                                ? "Le mandamos una invitación a " + gift.getRecipientEmail()
                                                        + ": se lo pasamos cuando entre a PLAN con ese email. Hasta entonces el voucher es tuyo."
                                                : "El voucher está en Mis reservas para que se lo des."));
                if (recipient != null) {
                    notificationService.create(recipient, "¡Te regalaron una experiencia!",
                            user.getFirstName() + " te regaló \"" + experience.getTitle() + "\". "
                                    + "Ya está en tus reservas, con el voucher.");
                }
            } else {
                notificationService.create(user, "Reserva confirmada",
                        "Tu reserva para \"" + experience.getTitle() + "\" fue confirmada.");
            }
            if (experience.getPublisher() != null && !experience.getPublisher().getId().equals(user.getId())) {
                notificationService.create(experience.getPublisher(), "Nueva reserva",
                        user.getFirstName() + " reservó \"" + experience.getTitle() + "\".");
            }
        }

        cartItemRepository.deleteAll(items);

        // Regalo con email: le avisamos por mail a quien lo recibe (sin el voucher: lo ve en PLAN cuando el regalo es suyo).
        // Va al final, cuando ya está todo guardado. Si el mail falla, la compra no se deshace (ya está pagada).
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
        return toResponse(order);
    }

    // Valida los datos del regalo: para quién es (obligatorio), su email (opcional) y la dedicatoria (hasta 300 caracteres).
    private GiftRequestDTO cleanGift(GiftRequestDTO gift) throws BadRequestException {
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
                .orElseThrow(() -> new ResourceNotFoundException("No existe un cupón con ese código"));

        String reason = DiscountCouponServiceImpl.reasonIfInvalid(coupon);
        if (reason != null) {
            throw new BadRequestException(DiscountCouponServiceImpl.describeReason(reason));
        }

        if (orderRepository.existsByUserIdAndDiscountCouponId(user.getId(), coupon.getId())) {
            throw new BadRequestException(DiscountCouponServiceImpl.describeReason(DiscountCouponServiceImpl.ALREADY_USED));
        }

        return coupon;
    }

    // Simula la pasarela de pago (no hay cobro real): valida el medio elegido y rechaza la tarjeta de prueba.
    private void simulatePayment(OrderRequestDTO request) throws BadRequestException {
        String method = request != null ? request.getPaymentMethod() : null;
        if (!"card".equals(method)) {
            return; // sin medio (Insomnia / seed), tarjeta guardada o billetera: siempre aprobado
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

    // Lo devuelto de una orden: cada reserva reembolsada devuelve su parte del total (prorrateando el cupón).
    private BigDecimal refundedAmount(Order order) {
        if (order.getBookings() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal refunded = BigDecimal.ZERO;
        for (Booking booking : order.getBookings()) {
            if (!booking.isRefunded()) {
                continue;
            }
            Experience experience = booking.getExperienceSession() != null
                    ? booking.getExperienceSession().getExperience()
                    : null;
            refunded = refunded.add(BookingServiceImpl.paidAmount(booking, experience));
        }
        return refunded.min(orZero(order.getTotal()));
    }

    // La comisión de PLAN sobre un monto; la usan las órdenes, las estadísticas y cada reserva.
    public static BigDecimal commissionOf(BigDecimal amount) {
        return amount.multiply(PLAN_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // Genera el código de voucher visible del Booking (ej. ETRIP-A1B2C3D4).
    private String generateVoucherCode() {
        return "ETRIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private String buyerName(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }

    // Arma el DTO de la orden junto con los vouchers (Bookings) que generó.
    private OrderResponseDTO toResponse(Order order) {
        List<BookingResponseDTO> bookingDtos = new ArrayList<>();
        List<Booking> bookings = order.getBookings();
        User buyer = order.getUser();
        if (bookings != null) {
            for (Booking booking : bookings) {
                ExperienceSession session = booking.getExperienceSession();
                Experience experience = session != null ? session.getExperience() : null;
                bookingDtos.add(BookingServiceImpl.withEarnings(BookingResponseDTO.builder()
                        .id(booking.getId())
                        // Un regalo ya entregado: el voucher es de quien lo recibió, quien pagó no ve el código
                        .voucherCode(booking.getRecipient() != null ? null : booking.getVoucherCode())
                        .orderId(order.getId())
                        .experienceSessionId(session != null ? session.getId() : null)
                        .experienceId(experience != null ? experience.getId() : null)
                        .experienceTitle(experience != null ? experience.getTitle() : null)
                        .startsAt(session != null ? session.getStartsAt() : null)
                        .endsAt(session != null ? session.getEndsAt() : null)
                        .quantity(booking.getQuantity())
                        .unitPrice(BookingServiceImpl.paidUnitPrice(booking, experience))
                        .createdAt(booking.getCreatedAt())
                        .refunded(booking.isRefunded())
                        .refundedAt(booking.getRefundedAt())
                        .status(bookingStatus(booking, session))
                        .buyerId(buyer != null ? buyer.getId() : null)
                        .buyerName(buyerName(buyer))
                        .build(), booking, experience));
            }
        }

        BigDecimal refunded = refundedAmount(order);
        return OrderResponseDTO.builder()
                .id(order.getId())
                .status(orderStatus(order))
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .couponCode(order.getDiscountCoupon() != null ? order.getDiscountCoupon().getCode() : null)
                .createdAt(order.getCreatedAt())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .total(order.getTotal())
                .refunded(refunded)
                .commission(commissionOf(orZero(order.getTotal()).subtract(refunded)))
                .bookings(bookingDtos)
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
        return toResponse(order);
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
        return toResponse(order);
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

    // Estado de la reserva: REFUNDED, COMPLETED (la fecha ya terminó) o CONFIRMED.
    private String bookingStatus(Booking booking, ExperienceSession session) {
        if (booking.isRefunded()) {
            return "REFUNDED";
        }
        return session != null && session.hasEnded() ? "COMPLETED" : "CONFIRMED";
    }
}
