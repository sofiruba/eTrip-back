package com.uade.tpo.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Order;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.service.BookingService;
import com.uade.tpo.demo.service.NotificationService;
import com.uade.tpo.demo.service.VoucherImageService;
import com.uade.tpo.demo.service.WaitlistService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    /** Quien tiene el voucher puede cancelarlo con reembolso hasta esta cantidad de horas antes de que empiece. */
    static final int REFUND_DEADLINE_HOURS = 48;

    private final BookingRepository bookingRepository;
    private final ExperienceRepository experienceRepository;
    private final ExperienceSessionRepository experienceSessionRepository;
    private final NotificationService notificationService;
    private final WaitlistService waitlistService;
    private final EntityManager entityManager;
    private final VoucherImageService voucherImageService;

    // Las reservas que compré y las que me regalaron; ADMIN ve todas.
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookings(User user, Pageable pageable) {
        Page<Booking> bookings = isAdmin(user)
                ? bookingRepository.findAll(pageable)
                : bookingRepository.findByOrder_User_IdOrRecipient_Id(user.getId(), user.getId(), pageable);
        return bookings.map(booking -> toResponse(booking, user));
    }

    // Reservas sobre las experiencias que publicó este usuario.
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getSales(User seller, Pageable pageable) {
        return bookingRepository.findByExperienceSession_Experience_Publisher_Id(seller.getId(), pageable)
                .map(booking -> toResponse(booking, seller));
    }

    // Reservas de una experiencia; solo su anfitrión o un ADMIN.
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookingsByExperience(Long experienceId, User requester, Pageable pageable)
            throws ResourceNotFoundException, ForbiddenException {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("La experiencia no existe"));
        if (!isPublisher(experience, requester) && !isAdmin(requester)) {
            throw new ForbiddenException("Solo el anfitrión de la experiencia puede ver sus reservas");
        }
        return bookingRepository.findByExperienceSession_Experience_Id(experienceId, pageable)
                .map(booking -> toResponse(booking, requester));
    }

    // Una reserva: la ven quien la compró, quien la recibió de regalo, el anfitrión y un ADMIN.
    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));
        boolean isBuyer = isSameUser(buyerOf(booking), user);
        boolean isRecipient = isSameUser(booking.getRecipient(), user);
        boolean isHost = isPublisher(booking.getExperienceSession().getExperience(), user);
        if (!isBuyer && !isRecipient && !isHost && !isAdmin(user)) {
            throw new ForbiddenException("No tenés acceso a esta reserva");
        }
        return toResponse(booking, user);
    }

    // Cancela una reserva con reembolso (la plata vuelve siempre a quien pagó).
    // - Quien tiene el voucher (quien compró o, si fue un regalo ya entregado, quien lo recibió): hasta 48 h antes.
    // - El anfitrión y el ADMIN: hasta que empiece (por ejemplo, si se suspende).
    // Quien pagó un regalo que ya entregó no lo puede retirar: el voucher es de la otra persona.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public BookingResponseDTO refundBooking(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));
        ExperienceSession session = booking.getExperienceSession();
        boolean isHolder = isSameUser(holderOf(booking), user);
        boolean isManager = isPublisher(session.getExperience(), user) || isAdmin(user);
        if (!isHolder && !isManager) {
            throw new ForbiddenException(isSameUser(buyerOf(booking), user)
                    ? "Este regalo ya es de " + booking.getRecipient().getFirstName() + ": solo esa persona puede cancelarlo"
                    : "No podés cancelar esta reserva");
        }
        // Bloquea la fecha y la reserva (SELECT ... FOR UPDATE) en el mismo orden que cancelar una fecha:
        // dos pedidos a la vez (doble clic) no devuelven los lugares dos veces.
        entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(booking, LockModeType.PESSIMISTIC_WRITE);
        if (booking.isRefunded()) {
            throw new BadRequestException("Esta reserva ya fue cancelada y reembolsada");
        }
        LocalDateTime now = LocalDateTime.now();
        if (session.getStartsAt().isBefore(now)) {
            throw new BadRequestException("La experiencia ya empezó: no se puede cancelar");
        }
        if (!isManager && refundDeadline(session).isBefore(now)) {
            throw new BadRequestException("Solo se puede cancelar con reembolso hasta " + REFUND_DEADLINE_HOURS
                    + " horas antes de que empiece");
        }

        session.setAvailableSeats(session.getAvailableSeats() + booking.getQuantity());
        experienceSessionRepository.save(session);
        booking.setRefunded(true);
        booking.setRefundedAt(now);
        Booking saved = bookingRepository.save(booking);

        String title = session.getExperience().getTitle();
        User buyer = buyerOf(booking);
        User recipient = booking.getRecipient();
        if (recipient != null && isSameUser(recipient, user)) {
            notificationService.create(buyer, "Regalo cancelado",
                    recipient.getFirstName() + " canceló el regalo de \"" + title + "\". Te devolvimos el dinero.");
            notificationService.create(recipient, "Regalo cancelado", "Cancelaste el regalo de \"" + title + "\".");
        } else {
            notificationService.create(buyer, "Reserva reembolsada",
                    "Tu reserva para \"" + title + "\" fue cancelada y te devolvimos el dinero.");
            if (recipient != null) {
                notificationService.create(recipient, "Regalo cancelado",
                        "La reserva de \"" + title + "\" que te regalaron fue cancelada.");
            }
        }
        // Se liberaron lugares: se avisa a la lista de espera
        waitlistService.notifySeatsAvailable(session);
        return toResponse(saved, user);
    }

    // El voucher como imagen (PNG). Solo lo puede pedir quien lo tiene.
    @Override
    @Transactional(readOnly = true)
    public byte[] getVoucherImage(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));
        User holder = holderOf(booking);
        if (!isSameUser(holder, user)) {
            throw new ForbiddenException("El voucher solo lo puede descargar quien lo tiene");
        }
        if (booking.isRefunded()) {
            throw new BadRequestException("Este voucher fue anulado porque la reserva se canceló");
        }
        return voucherImageService.render(booking, holder);
    }

    // ---------- Reglas compartidas (las usan también las órdenes) ----------

    /** De quién es el voucher: de quien recibió el regalo (si ya se le entregó) o de quien compró. */
    static User holderOf(Booking booking) {
        return booking.getRecipient() != null ? booking.getRecipient() : buyerOf(booking);
    }

    static User buyerOf(Booking booking) {
        return booking.getOrder().getUser();
    }

    /** Hasta cuándo quien tiene el voucher puede cancelarlo con reembolso. */
    static LocalDateTime refundDeadline(ExperienceSession session) {
        return session.getStartsAt().minusHours(REFUND_DEADLINE_HOURS);
    }

    /** Lo que vale la reserva al precio de lista (sin cupón). */
    static BigDecimal grossAmount(Booking booking) {
        return booking.getUnitPrice().multiply(BigDecimal.valueOf(booking.getQuantity()));
    }

    /** Lo que se pagó por la reserva: su parte del total de la orden (el cupón se reparte en proporción). */
    static BigDecimal paidAmount(Booking booking) {
        BigDecimal gross = grossAmount(booking);
        Order order = booking.getOrder();
        return order.getSubtotal().signum() > 0
                ? gross.multiply(order.getTotal()).divide(order.getSubtotal(), 2, RoundingMode.HALF_UP)
                : gross;
    }

    /**
     * Lo que recibe el anfitrión: el precio de lista menos la comisión de PLAN. Los cupones los absorbe PLAN,
     * así que no le bajan la ganancia al anfitrión. En 0 si la reserva se canceló.
     */
    static BigDecimal hostEarnings(Booking booking) {
        if (booking.isRefunded()) {
            return BigDecimal.ZERO;
        }
        BigDecimal gross = grossAmount(booking);
        return gross.subtract(OrderServiceImpl.commissionOf(gross));
    }

    /** Lo que queda para PLAN: lo cobrado menos lo del anfitrión (con un cupón grande puede ser negativo). */
    static BigDecimal planCommission(Booking booking) {
        return booking.isRefunded() ? BigDecimal.ZERO : paidAmount(booking).subtract(hostEarnings(booking));
    }

    /**
     * Arma el DTO de una reserva para quien la mira:
     * - quien pagó un regalo ya entregado no ve el código (el voucher es de la otra persona);
     * - refundable / refundableUntil dicen si esta persona la puede cancelar y hasta cuándo.
     */
    static BookingResponseDTO toResponse(Booking booking, User viewer) {
        ExperienceSession session = booking.getExperienceSession();
        Experience experience = session.getExperience();
        Order order = booking.getOrder();
        User buyer = order.getUser();
        boolean giftedAway = booking.getRecipient() != null && isSameUser(buyer, viewer);
        boolean refunded = booking.isRefunded();

        BookingResponseDTO dto = BookingResponseDTO.builder()
                .id(booking.getId())
                .voucherCode(giftedAway ? null : booking.getVoucherCode())
                .orderId(order.getId())
                .experienceSessionId(session.getId())
                .experienceId(experience.getId())
                .experienceTitle(experience.getTitle())
                .startsAt(session.getStartsAt())
                .endsAt(session.getEndsAt())
                .quantity(booking.getQuantity())
                .unitPrice(booking.getUnitPrice())
                .createdAt(booking.getCreatedAt())
                .refunded(refunded)
                .refundedAt(booking.getRefundedAt())
                .status(refunded ? "REFUNDED" : session.hasEnded() ? "COMPLETED" : "CONFIRMED")
                .paidAmount(refunded ? BigDecimal.ZERO : paidAmount(booking))
                .hostEarnings(hostEarnings(booking))
                .commission(planCommission(booking))
                .buyerId(buyer.getId())
                .buyerName(fullName(buyer))
                .recipientId(booking.getRecipient() != null ? booking.getRecipient().getId() : null)
                .build();

        if (order.getGiftRecipientName() != null) {
            dto.setGiftRecipientName(order.getGiftRecipientName());
            dto.setGiftMessage(order.getGiftMessage());
            dto.setGiftFromName(dto.getBuyerName());
            dto.setGiftStatus(booking.getRecipient() != null ? "DELIVERED"
                    : order.getGiftRecipientEmail() != null ? "PENDING" : "IN_PERSON");
        }

        dto.setReviewable(!refunded && session.hasEnded() && isSameUser(holderOf(booking), viewer));

        // ¿La puede cancelar quien la mira? Quien tiene el voucher, hasta 48 h antes; anfitrión y admin, hasta que empiece
        boolean started = !session.getStartsAt().isAfter(LocalDateTime.now());
        if (!refunded && !started && viewer != null) {
            boolean manager = isPublisher(experience, viewer) || isAdmin(viewer);
            if (manager || isSameUser(holderOf(booking), viewer)) {
                LocalDateTime until = manager ? session.getStartsAt() : refundDeadline(session);
                dto.setRefundableUntil(until);
                dto.setRefundable(until.isAfter(LocalDateTime.now()));
            }
        }
        return dto;
    }

    private static boolean isSameUser(User user, User other) {
        return user != null && other != null && user.getId().equals(other.getId());
    }

    private static boolean isPublisher(Experience experience, User user) {
        return experience != null && isSameUser(experience.getPublisher(), user);
    }

    private static boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    static String fullName(User user) {
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
