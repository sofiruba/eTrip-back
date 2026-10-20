package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ExperienceSessionRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceSessionResponseDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.ExperienceSessionSpecifications;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.service.ExperienceSessionService;
import com.uade.tpo.demo.service.NotificationService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceSessionServiceImpl implements ExperienceSessionService {

    private final ExperienceSessionRepository experienceSessionRepository;
    private final ExperienceRepository experienceRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final CartItemRepository cartItemRepository;
    private final EntityManager entityManager;

    // Lista turnos con filtros opcionales combinables (experiencia, con cupo, rango de fechas).
    @Override
    @Transactional(readOnly = true)
    public Page<ExperienceSessionResponseDTO> searchSessions(
            Long experienceId, Boolean onlyAvailable, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable) throws ResourceNotFoundException {
        if (experienceId != null && !experienceRepository.existsById(experienceId)) {
            throw new ResourceNotFoundException("La experiencia no existe");
        }

        return experienceSessionRepository
                .findAll(ExperienceSessionSpecifications.withFilters(experienceId, onlyAvailable, dateFrom, dateTo),
                        pageable)
                .map(this::mapToResponseDTO);
    }

    // Fechas que gestiona el usuario, con pasadas y canceladas: ADMIN ve todas; el resto, las de sus experiencias.
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceSessionResponseDTO> getManagedSessions(User currentUser) {
        List<ExperienceSession> sessions = currentUser.getRole() == Role.ADMIN
                ? experienceSessionRepository.findAllByOrderByStartsAtAsc()
                : experienceSessionRepository.findByExperience_Publisher_IdOrderByStartsAtAsc(currentUser.getId());
        return sessions.stream().map(this::mapToResponseDTO).toList();
    }

    // Un turno puntual por id.
    @Override
    @Transactional(readOnly = true)
    public ExperienceSessionResponseDTO getSessionById(Long sessionId) throws ResourceNotFoundException {
        ExperienceSession session = experienceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("La sesión solicitada no existe"));
        if (!isPubliclyActive(session)) {
            throw new ResourceNotFoundException("La sesión solicitada ya no está disponible");
        }

        return mapToResponseDTO(session);
    }

    // Crea un turno; valida fechas y solapamiento. Solo el dueño de la experiencia o un ADMIN.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceSessionResponseDTO createSession(ExperienceSessionRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        validateCreateRequest(request);

        Experience experience = experienceRepository.findById(request.getExperienceId())
                .orElseThrow(() -> new ResourceNotFoundException("La experiencia no existe"));
        assertCanManage(experience, currentUser);

        validateNoOverlap(experience.getId(), null, request.getStartsAt(), request.getEndsAt());

        ExperienceSession session = ExperienceSession.builder()
                .experience(experience)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .capacity(request.getCapacity())
                .availableSeats(request.getCapacity())
                .build();

        ExperienceSession savedSession = experienceSessionRepository.save(session);

        return mapToResponseDTO(savedSession);
    }

    // Edita fecha/capacidad de un turno. Valida ownership contra la experiencia actual del
    // turno antes de aplicar ningún cambio del body.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceSessionResponseDTO updateSession(Long sessionId, ExperienceSessionRequestDTO request,
            User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        ExperienceSession session = experienceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("La sesión solicitada no existe"));
        // Bloquea la fecha (como la compra y el reembolso): si alguien compra mientras se cambia el cupo,
        // no se pisan los lugares disponibles.
        entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
        if (!session.isActive()) {
            throw new BadRequestException("No se puede modificar una fecha cancelada");
        }
        if (session.hasEnded()) {
            throw new BadRequestException("No se puede modificar una fecha que ya terminó");
        }
        assertCanManage(session.getExperience(), currentUser);

        validateUpdateRequest(request);

        // Una fecha no se puede pasar a otra experiencia (se llevaría sus reservas a una publicación ajena)
        if (request.getExperienceId() != null && !request.getExperienceId().equals(session.getExperience().getId())) {
            throw new BadRequestException("No se puede mover una fecha a otra experiencia");
        }

        // Con compradores no se cambia el horario: adelantarla, por ejemplo, les sacaría el plazo de 48 h para
        // pedir el reembolso, y nadie les avisaría. Se cancela (se reembolsa sola) y se crea otra.
        boolean scheduleChanged = (request.getStartsAt() != null && !request.getStartsAt().equals(session.getStartsAt()))
                || (request.getEndsAt() != null && !request.getEndsAt().equals(session.getEndsAt()));
        if (scheduleChanged && hasActiveBookings(sessionId)) {
            throw new BadRequestException("Esta fecha ya tiene reservas, así que no se le puede cambiar el horario. "
                    + "Cancelala (se reembolsa a los compradores) y creá una nueva.");
        }

        if (request.getStartsAt() != null) {
            session.setStartsAt(request.getStartsAt());
        }

        if (request.getEndsAt() != null) {
            session.setEndsAt(request.getEndsAt());
        }

        validateDateRange(session.getStartsAt(), session.getEndsAt());
        validateFutureStart(session.getStartsAt());
        validateNoOverlap(session.getExperience().getId(), session.getId(), session.getStartsAt(), session.getEndsAt());

        if (request.getCapacity() != null) {
            Integer bookedSeats = calculateBookedSeats(session);

            if (request.getCapacity() < bookedSeats) {
                throw new BadRequestException("El cupo no puede ser menor a los " + bookedSeats + " lugares ya reservados");
            }

            session.setCapacity(request.getCapacity());
            session.setAvailableSeats(request.getCapacity() - bookedSeats);
        }

        ExperienceSession savedSession = experienceSessionRepository.save(session);
        return mapToResponseDTO(savedSession);
    }

    // Baja lógica un turno. Se conserva para no romper reservas históricas.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteSession(Long sessionId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        ExperienceSession session = experienceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("La sesión solicitada no existe"));
        assertCanManage(session.getExperience(), currentUser);
        // Bloquea la fecha antes de reembolsar sus reservas (mismo orden que BookingServiceImpl.refundBooking)
        entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);

        if (!session.isActive()) {
            throw new BadRequestException("Esta fecha ya está cancelada");
        }
        if (session.hasEnded()) {
            throw new BadRequestException("No se puede cancelar una fecha que ya terminó");
        }
        if (session.getStartsAt() != null && session.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("No se puede cancelar una sesión que ya comenzó");
        }

        int releasedSeats = 0;
        for (Booking booking : bookingRepository.findByExperienceSession_Id(sessionId)) {
            if (!booking.isRefunded()) {
                booking.setRefunded(true);
                booking.setRefundedAt(LocalDateTime.now());
                releasedSeats += booking.getQuantity();
                bookingRepository.save(booking);
                if (booking.getOrder() != null) {
                    notificationService.create(booking.getOrder().getUser(), "Sesión cancelada",
                            "La sesión de \"" + session.getExperience().getTitle()
                                    + "\" fue cancelada y tu reserva fue reembolsada.");
                }
            }
        }
        if (releasedSeats > 0) {
            session.setAvailableSeats(Math.min(
                    session.getCapacity() == null ? releasedSeats : session.getCapacity(),
                    (session.getAvailableSeats() == null ? 0 : session.getAvailableSeats()) + releasedSeats));
        }
        session.setActive(false);
        experienceSessionRepository.save(session);
    }

    // Borra del todo una fecha que ya no sirve (cancelada o terminada) junto con sus ítems de carrito.
    // Una fecha vigente primero se cancela (eso reembolsa a los compradores). Una fecha que tuvo reservas no se borra:
    // son los vouchers e historial de otras personas, y las órdenes y estadísticas (lo cobrado y lo reembolsado) dependen de ellas.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void removeSession(Long sessionId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        ExperienceSession session = experienceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("La fecha no existe"));
        assertCanManage(session.getExperience(), currentUser);

        // "Terminada" es que ya pasó la hora de fin: una fecha que empezó pero sigue en curso todavía no se toca
        if (session.isActive() && !session.hasEnded()) {
            throw new BadRequestException("Esta fecha sigue vigente o en curso: si todavía no empezó, cancelala "
                    + "(se reembolsa sola) y después la podés eliminar");
        }
        if (session.getBookings() != null && !session.getBookings().isEmpty()) {
            throw new BadRequestException("Esta fecha tuvo reservas y los compradores conservan sus vouchers, "
                    + "así que queda guardada en el historial.");
        }

        cartItemRepository.deleteByExperienceSessionId(sessionId);
        experienceSessionRepository.delete(session);
    }

    // ¿Alguien tiene una reserva vigente (no reembolsada) para esta fecha?
    private boolean hasActiveBookings(Long sessionId) {
        return bookingRepository.findByExperienceSession_Id(sessionId).stream()
                .anyMatch(booking -> !booking.isRefunded());
    }

    private void assertCanManage(Experience experience, User currentUser) throws ForbiddenException {
        boolean isOwner = experience != null
                && experience.getPublisher() != null
                && currentUser != null
                && experience.getPublisher().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Solo el anfitrión de la experiencia puede gestionar sus fechas");
        }
    }

    private void validateCreateRequest(ExperienceSessionRequestDTO request) throws BadRequestException {
        if (request == null || request.getExperienceId() == null) {
            throw new BadRequestException("Falta la experiencia de la fecha");
        }

        validateDateRange(request.getStartsAt(), request.getEndsAt());
        validateFutureStart(request.getStartsAt());
        validateCapacity(request.getCapacity());
    }

    private void validateUpdateRequest(ExperienceSessionRequestDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException("Faltan los datos de la fecha");
        }

        if (request.getCapacity() != null) {
            validateCapacity(request.getCapacity());
        }
    }

    private void validateDateRange(LocalDateTime startsAt, LocalDateTime endsAt) throws BadRequestException {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new BadRequestException("La hora de fin tiene que ser posterior a la de inicio");
        }
    }

    private void validateFutureStart(LocalDateTime startsAt) throws BadRequestException {
        if (startsAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("La fecha tiene que ser futura");
        }
    }

    private void validateCapacity(Integer capacity) throws BadRequestException {
        if (capacity == null || capacity <= 0) {
            throw new BadRequestException("El cupo tiene que ser mayor a 0");
        }
    }

    private void validateNoOverlap(Long experienceId, Long sessionId, LocalDateTime startsAt, LocalDateTime endsAt)
            throws BadRequestException {
        boolean existsOverlap = sessionId == null
                ? experienceSessionRepository.existsByExperienceIdAndActiveTrueAndStartsAtLessThanAndEndsAtGreaterThan(
                        experienceId, endsAt, startsAt)
                : experienceSessionRepository.existsByExperienceIdAndActiveTrueAndIdNotAndStartsAtLessThanAndEndsAtGreaterThan(
                        experienceId, sessionId, endsAt, startsAt);

        if (existsOverlap) {
            throw new BadRequestException("Ya hay otra fecha de esta experiencia en ese horario");
        }
    }

    private boolean isPubliclyActive(ExperienceSession session) {
        return session.isActive()
                && session.getExperience() != null
                && session.getExperience().isActive()
                && session.getStartsAt() != null
                && !session.getStartsAt().isBefore(LocalDateTime.now());
    }

    private Integer calculateBookedSeats(ExperienceSession session) {
        if (session.getCapacity() == null || session.getAvailableSeats() == null) {
            return 0;
        }

        return session.getCapacity() - session.getAvailableSeats();
    }

    private ExperienceSessionResponseDTO mapToResponseDTO(ExperienceSession session) {
        Experience experience = session.getExperience();

        return ExperienceSessionResponseDTO.builder()
                .id(session.getId())
                .experienceId(experience != null ? experience.getId() : null)
                .experienceTitle(experience != null ? experience.getTitle() : null)
                .startsAt(session.getStartsAt())
                .endsAt(session.getEndsAt())
                .capacity(session.getCapacity())
                .availableSeats(session.getAvailableSeats())
                .active(session.isActive())
                .status(status(session))
                .editable(session.isActive() && session.getStartsAt().isAfter(LocalDateTime.now()))
                .build();
    }

    // Estado de la fecha para mostrar en el calendario: CANCELLED, FINISHED, SOLD_OUT o PUBLISHED.
    private String status(ExperienceSession session) {
        if (!session.isActive()) {
            return "CANCELLED";
        }
        // Ya empezó o terminó (se calcula con la hora, no hay ninguna tarea que la marque)
        if (session.getStartsAt().isBefore(LocalDateTime.now())) {
            return "FINISHED";
        }
        if (session.getAvailableSeats() == null || session.getAvailableSeats() == 0) {
            return "SOLD_OUT";
        }
        return "PUBLISHED";
    }
}
