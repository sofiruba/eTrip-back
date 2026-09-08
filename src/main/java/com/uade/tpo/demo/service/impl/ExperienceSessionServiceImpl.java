package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ExperienceSessionRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceSessionResponseDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.ExperienceSessionSpecifications;
import com.uade.tpo.demo.service.ExperienceSessionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceSessionServiceImpl implements ExperienceSessionService {

    private final ExperienceSessionRepository experienceSessionRepository;
    private final ExperienceRepository experienceRepository;
    private final CartItemRepository cartItemRepository;

    // Lista turnos con filtros opcionales combinables (experiencia, con cupo, rango de fechas).
    @Override
    @Transactional(readOnly = true)
    public Page<ExperienceSessionResponseDTO> searchSessions(
            Long experienceId, Boolean onlyAvailable, LocalDateTime dateFrom, LocalDateTime dateTo,
            Pageable pageable) throws ResourceNotFoundException {
        if (experienceId != null && !experienceRepository.existsById(experienceId)) {
            throw new ResourceNotFoundException();
        }

        return experienceSessionRepository
                .findAll(ExperienceSessionSpecifications.withFilters(experienceId, onlyAvailable, dateFrom, dateTo),
                        pageable)
                .map(this::mapToResponseDTO);
    }

    // Un turno puntual por id.
    @Override
    @Transactional(readOnly = true)
    public ExperienceSessionResponseDTO getSessionById(Long sessionId) throws ResourceNotFoundException {
        ExperienceSession session = experienceSessionRepository.findById(sessionId)
                .orElseThrow(ResourceNotFoundException::new);

        return mapToResponseDTO(session);
    }

    // Crea un turno; valida fechas y solapamiento. Solo el dueño de la experiencia o un ADMIN.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceSessionResponseDTO createSession(ExperienceSessionRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        validateCreateRequest(request);

        Experience experience = experienceRepository.findById(request.getExperienceId())
                .orElseThrow(ResourceNotFoundException::new);
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
                .orElseThrow(ResourceNotFoundException::new);
        assertCanManage(session.getExperience(), currentUser);

        validateUpdateRequest(request);

        if (request.getExperienceId() != null) {
            Experience experience = experienceRepository.findById(request.getExperienceId())
                    .orElseThrow(ResourceNotFoundException::new);
            session.setExperience(experience);
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
                throw new BadRequestException();
            }

            session.setCapacity(request.getCapacity());
            session.setAvailableSeats(request.getCapacity() - bookedSeats);
        }

        ExperienceSession savedSession = experienceSessionRepository.save(session);
        return mapToResponseDTO(savedSession);
    }

    // Borra un turno; falla si ya tiene reservas. Solo el dueño de la experiencia o un ADMIN.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteSession(Long sessionId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        ExperienceSession session = experienceSessionRepository.findById(sessionId)
                .orElseThrow(ResourceNotFoundException::new);
        assertCanManage(session.getExperience(), currentUser);

        if (session.getBookings() != null && !session.getBookings().isEmpty()) {
            throw new BadRequestException("No se puede borrar el turno porque ya tiene reservas");
        }

        if (cartItemRepository.existsByExperienceSessionId(sessionId)) {
            throw new BadRequestException(
                    "No se puede borrar el turno porque esta en el carrito de algun usuario");
        }

        experienceSessionRepository.delete(session);
    }

    private void assertCanManage(Experience experience, User currentUser) throws ForbiddenException {
        boolean isOwner = experience != null
                && experience.getPublisher() != null
                && currentUser != null
                && experience.getPublisher().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException();
        }
    }

    private void validateCreateRequest(ExperienceSessionRequestDTO request) throws BadRequestException {
        if (request == null || request.getExperienceId() == null) {
            throw new BadRequestException();
        }

        validateDateRange(request.getStartsAt(), request.getEndsAt());
        validateFutureStart(request.getStartsAt());
        validateCapacity(request.getCapacity());
    }

    private void validateUpdateRequest(ExperienceSessionRequestDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException();
        }

        if (request.getCapacity() != null) {
            validateCapacity(request.getCapacity());
        }
    }

    private void validateDateRange(LocalDateTime startsAt, LocalDateTime endsAt) throws BadRequestException {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new BadRequestException();
        }
    }

    private void validateFutureStart(LocalDateTime startsAt) throws BadRequestException {
        if (startsAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException();
        }
    }

    private void validateCapacity(Integer capacity) throws BadRequestException {
        if (capacity == null || capacity <= 0) {
            throw new BadRequestException();
        }
    }

    private void validateNoOverlap(Long experienceId, Long sessionId, LocalDateTime startsAt, LocalDateTime endsAt)
            throws BadRequestException {
        boolean existsOverlap = sessionId == null
                ? experienceSessionRepository.existsByExperienceIdAndStartsAtLessThanAndEndsAtGreaterThan(
                        experienceId, endsAt, startsAt)
                : experienceSessionRepository.existsByExperienceIdAndIdNotAndStartsAtLessThanAndEndsAtGreaterThan(
                        experienceId, sessionId, endsAt, startsAt);

        if (existsOverlap) {
            throw new BadRequestException();
        }
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
                .build();
    }
}
