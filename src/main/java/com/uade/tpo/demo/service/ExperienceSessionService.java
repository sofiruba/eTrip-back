package com.uade.tpo.demo.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.ExperienceSessionRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceSessionResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceSessionService {
    // Lista turnos con filtros opcionales combinables: experiencia, solo con cupo, rango de fechas.
    Page<ExperienceSessionResponseDTO> searchSessions(
            Long experienceId, Boolean onlyAvailable, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable)
            throws ResourceNotFoundException;

    // Un turno puntual por id.
    ExperienceSessionResponseDTO getSessionById(Long sessionId) throws ResourceNotFoundException;

    // Crea un turno. Solo el dueño (publisher) de la experiencia o un ADMIN.
    ExperienceSessionResponseDTO createSession(ExperienceSessionRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Edita un turno. Solo el dueño (publisher) de la experiencia o un ADMIN.
    ExperienceSessionResponseDTO updateSession(Long sessionId, ExperienceSessionRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Borra un turno. Solo el dueño (publisher) de la experiencia o un ADMIN.
    void deleteSession(Long sessionId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;
}
