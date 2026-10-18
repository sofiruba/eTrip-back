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
import com.uade.tpo.demo.service.OrderService;
import com.uade.tpo.demo.service.BookingService;
import com.uade.tpo.demo.service.NotificationService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

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

        // Valida y resuelve el cupón del request (si viene) y que este usuario no lo haya usado ya en una orden anterior.
        DiscountCoupon coupon = resolveCoupon(request, user);
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (coupon != null) {
            discountAmount = subtotal
                    .multiply(coupon.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal total = subtotal.subtract(discountAmount);
        long totalTickets = items.stream().mapToLong(CartItem::getQuantity).sum();

        // Crea la orden, genera un Booking por cada item, descuenta los cupos de las sesiones y vacía el carrito.
        Order order = Order.builder()
                .user(user)
                .discountCoupon(coupon)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .total(total)
                .count(totalTickets)
                .createdAt(LocalDateTime.now())
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
                    .voucherCode(generateVoucherCode())
                    .createdAt(LocalDateTime.now())
                    .build();
            bookings.add(bookingRepository.save(booking));

            notificationService.create(user, "Reserva confirmada",
                    "Tu reserva para \"" + experience.getTitle() + "\" fue confirmada.");
            if (experience.getPublisher() != null && !experience.getPublisher().getId().equals(user.getId())) {
                notificationService.create(experience.getPublisher(), "Nueva reserva",
                        user.getFirstName() + " reservó \"" + experience.getTitle() + "\".");
            }
        }

        cartItemRepository.deleteAll(items);

        order.setBookings(bookings);
        return toResponse(order);
    }

    // Busca y valida el cupón del request (mismo criterio que GET /discount-coupons/validate),
    // y que este usuario no lo haya usado ya en una orden anterior.
    private DiscountCoupon resolveCoupon(OrderRequestDTO request, User user)
            throws ResourceNotFoundException, BadRequestException {
        if (request == null || request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            return null;
        }

        DiscountCoupon coupon = discountCouponRepository
                .findByCode(request.getCouponCode().trim().toUpperCase())
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
        BigDecimal subtotal = orZero(order.getSubtotal());
        BigDecimal total = orZero(order.getTotal());
        BigDecimal refunded = BigDecimal.ZERO;
        for (Booking booking : order.getBookings()) {
            if (!booking.isRefunded()) {
                continue;
            }
            Experience experience = booking.getExperienceSession() != null
                    ? booking.getExperienceSession().getExperience()
                    : null;
            BigDecimal gross = orZero(experience != null ? experience.getEffectivePrice() : null)
                    .multiply(BigDecimal.valueOf(booking.getQuantity() != null ? booking.getQuantity() : 0));
            refunded = refunded.add(subtotal.signum() > 0
                    ? gross.multiply(total).divide(subtotal, 2, RoundingMode.HALF_UP)
                    : gross);
        }
        return refunded.min(total);
    }

    private BigDecimal commissionOf(BigDecimal amount) {
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
                bookingDtos.add(BookingResponseDTO.builder()
                        .id(booking.getId())
                        .voucherCode(booking.getVoucherCode())
                        .orderId(order.getId())
                        .experienceSessionId(session != null ? session.getId() : null)
                        .experienceId(experience != null ? experience.getId() : null)
                        .experienceTitle(experience != null ? experience.getTitle() : null)
                        .startsAt(session != null ? session.getStartsAt() : null)
                        .endsAt(session != null ? session.getEndsAt() : null)
                        .quantity(booking.getQuantity())
                        .unitPrice(experience != null ? experience.getEffectivePrice() : null)
                        .createdAt(booking.getCreatedAt())
                        .refunded(booking.isRefunded())
                        .refundedAt(booking.getRefundedAt())
                        .status(bookingStatus(booking, session))
                        .buyerId(buyer != null ? buyer.getId() : null)
                        .buyerName(buyerName(buyer))
                        .build());
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
                .build();
    }

    // Estado de la reserva: REFUNDED, COMPLETED (la fecha ya terminó) o CONFIRMED.
    private String bookingStatus(Booking booking, ExperienceSession session) {
        if (booking.isRefunded()) {
            return "REFUNDED";
        }
        boolean ended = session != null
                && (session.isFinished() || (session.getEndsAt() != null && session.getEndsAt().isBefore(LocalDateTime.now())));
        return ended ? "COMPLETED" : "CONFIRMED";
    }
}
