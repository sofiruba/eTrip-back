package com.uade.tpo.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.ExperienceSessionRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceSessionResponseDTO;
import com.uade.tpo.demo.dtos.response.HostAgendaItemDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceSessionService {
    // Lista fechas con filtros opcionales combinables: experiencia, solo con cupo, rango de fechas.
    Page<ExperienceSessionResponseDTO> searchSessions(
            Long experienceId, Boolean onlyAvailable, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable)
            throws ResourceNotFoundException;

    // Fechas que gestiona el usuario (ADMIN: todas; anfitrión: las suyas), incluidas pasadas y canceladas.
    List<ExperienceSessionResponseDTO> getManagedSessions(User currentUser);

    // "Fechas y huéspedes": un resumen por cada experiencia del anfitrión
    List<HostAgendaItemDTO> getHostAgenda(User currentUser);

    // Una fecha puntual por id.
    ExperienceSessionResponseDTO getSessionById(Long sessionId) throws ResourceNotFoundException;

    // Crea una fecha. Solo el dueño (publisher) de la experiencia o un ADMIN.
    ExperienceSessionResponseDTO createSession(ExperienceSessionRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Edita una fecha. Solo el dueño (publisher) de la experiencia o un ADMIN.
    ExperienceSessionResponseDTO updateSession(Long sessionId, ExperienceSessionRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Da de baja lógicamente una fecha. Solo el dueño (publisher) de la experiencia o un ADMIN.
    void deleteSession(Long sessionId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Borra del todo una fecha cancelada o ya terminada (con sus reservas e ítems de carrito).
    void removeSession(Long sessionId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;
}
