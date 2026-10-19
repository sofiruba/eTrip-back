package com.uade.tpo.demo.service.impl;

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
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.service.BookingService;
import com.uade.tpo.demo.service.NotificationService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    // Mismo plazo que muestra el front (utils/cancellation.js)
    private static final int REFUND_DEADLINE_HOURS = 48;

    private final BookingRepository bookingRepository;
    private final ExperienceRepository experienceRepository;
    private final ExperienceSessionRepository experienceSessionRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    // Vouchers del usuario logueado; ADMIN ve los de todos.
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookings(User user, Pageable pageable) {
        Page<Booking> bookings = isAdmin(user)
                ? bookingRepository.findAll(pageable)
                : bookingRepository.findByOrder_User_Id(user.getId(), pageable);
        return bookings.map(this::toResponse);
    }

    // Vista vendedor: reservas hechas sobre experiencias que publicó este usuario.
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getSales(User seller, Pageable pageable) {
        return bookingRepository.findByExperienceSession_Experience_Publisher_Id(seller.getId(), pageable)
                .map(this::toResponse);
    }

    // Reservas de una experiencia puntual; solo el dueño de esa experiencia o un ADMIN.
    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookingsByExperience(Long experienceId, User requester, Pageable pageable)
            throws ResourceNotFoundException, ForbiddenException {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("La experiencia no existe"));

        boolean isOwner = experience.getPublisher() != null
                && experience.getPublisher().getId().equals(requester.getId());
        if (!isOwner && !isAdmin(requester)) {
            throw new ForbiddenException("Solo el anfitrión de la experiencia puede ver sus reservas");
        }

        return bookingRepository.findByExperienceSession_Experience_Id(experienceId, pageable)
                .map(this::toResponse);
    }

    // Un voucher puntual: lo ve el comprador, el vendedor de la experiencia, o un ADMIN.
    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));

        Order order = booking.getOrder();
        // El comprador es el usuario que hizo la orden asociada a la reserva.
        boolean isBuyer = order != null
                && order.getUser() != null
                && order.getUser().getId().equals(user.getId());

        // El vendedor es el usuario que publicó la experiencia asociada a la sesión de la reserva.
        ExperienceSession session = booking.getExperienceSession();
        boolean isSeller = session != null
                && session.getExperience() != null
                && session.getExperience().getPublisher() != null
                && session.getExperience().getPublisher().getId().equals(user.getId());

        // Solo el comprador, el vendedor o un ADMIN pueden ver la reserva.
        if (!isBuyer && !isSeller && !isAdmin(user)) {
            throw new ForbiddenException("No tenés acceso a esta reserva");
        }

        return toResponse(booking);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public BookingResponseDTO refundBooking(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva solicitada no existe"));
        Order order = booking.getOrder();
        boolean isBuyer = order != null && order.getUser() != null
                && order.getUser().getId().equals(user.getId());
        ExperienceSession session = booking.getExperienceSession();
        boolean isSeller = session != null
                && session.getExperience() != null
                && session.getExperience().getPublisher() != null
                && session.getExperience().getPublisher().getId().equals(user.getId());
        if (!isBuyer && !isSeller && !isAdmin(user)) {
            throw new ForbiddenException("Solo el comprador, el anfitrión o un administrador pueden cancelar la reserva");
        }
        // Bloquea la fecha y la reserva hasta terminar y las vuelve a leer (SELECT ... FOR UPDATE), en el mismo orden
        // que cancelar una fecha: dos pedidos de reembolso a la vez (doble clic) ya no suman los lugares dos veces.
        if (session != null) {
            entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
        }
        entityManager.refresh(booking, LockModeType.PESSIMISTIC_WRITE);
        if (booking.isRefunded()) {
            throw new BadRequestException("La reserva ya fue reembolsada");
        }

        if (session == null) {
            throw new BadRequestException("La reserva no tiene una sesión asociada");
        }
        if (session.getStartsAt() != null && session.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("No se puede reembolsar una reserva cuya sesión ya comenzó");
        }
        // Política de cancelación: el comprador puede pedir el reembolso hasta 48 h antes.
        // El anfitrión y el admin pueden cancelar hasta que empiece (por ejemplo, si se suspende).
        boolean buyerOnly = isBuyer && !isSeller && !isAdmin(user);
        if (buyerOnly && session.getStartsAt() != null
                && session.getStartsAt().minusHours(REFUND_DEADLINE_HOURS).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Solo podés cancelar con reembolso hasta " + REFUND_DEADLINE_HOURS
                    + " horas antes de que empiece");
        }

        int availableSeats = session.getAvailableSeats() == null ? 0 : session.getAvailableSeats();
        session.setAvailableSeats(availableSeats + booking.getQuantity());
        experienceSessionRepository.save(session);
        booking.setRefunded(true);
        booking.setRefundedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        notificationService.create(order != null ? order.getUser() : null, "Reserva reembolsada",
                "Tu reserva para \"" + (session.getExperience() != null ? session.getExperience().getTitle() : "la experiencia")
                        + "\" fue cancelada y reembolsada.");
        return toResponse(saved);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private BookingResponseDTO toResponse(Booking booking) {
        ExperienceSession session = booking.getExperienceSession();
        Experience experience = session != null ? session.getExperience() : null;
        Order order = booking.getOrder();
        User buyer = order != null ? order.getUser() : null;

        return BookingResponseDTO.builder()
                .id(booking.getId())
                .voucherCode(booking.getVoucherCode())
                .orderId(order != null ? order.getId() : null)
                .experienceSessionId(session != null ? session.getId() : null)
                .experienceId(experience != null ? experience.getId() : null)
                .experienceTitle(experience != null ? experience.getTitle() : null)
                .startsAt(session != null ? session.getStartsAt() : null)
                .endsAt(session != null ? session.getEndsAt() : null)
                .quantity(booking.getQuantity())
                .unitPrice(paidUnitPrice(booking, experience))
                .createdAt(booking.getCreatedAt())
                .refunded(booking.isRefunded())
                .refundedAt(booking.getRefundedAt())
                .status(bookingStatus(booking, session))
                .buyerId(buyer != null ? buyer.getId() : null)
                .buyerName(fullName(buyer))
                .build();
    }

    // El precio que se pagó (guardado al comprar). Las reservas anteriores a ese campo usan el precio actual.
    static java.math.BigDecimal paidUnitPrice(Booking booking, Experience experience) {
        if (booking.getUnitPrice() != null) {
            return booking.getUnitPrice();
        }
        return experience != null ? experience.getEffectivePrice() : null;
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? user.getEmail() : name;
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
