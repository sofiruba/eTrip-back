package com.uade.tpo.demo.controllers.experiences;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.ExperienceSessionRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceSessionResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ExperienceSessionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("experience-sessions")
@RequiredArgsConstructor
public class ExperienceSessionsController {

    private final ExperienceSessionService experienceSessionService;

    // Lista turnos con filtros opcionales combinables: experienceId, onlyAvailable (con cupo), dateFrom/dateTo.
    @GetMapping
    public ResponseEntity<Page<ExperienceSessionResponseDTO>> getSessions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long experienceId,
            @RequestParam(required = false) Boolean onlyAvailable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceSessionService.searchSessions(
                experienceId, onlyAvailable, dateFrom, dateTo, pageRequest(page, size)));
    }

    // Turnos de una experiencia puntual, con availableSeats y los mismos filtros opcionales.
    @GetMapping("/experience/{experienceId}")
    public ResponseEntity<Page<ExperienceSessionResponseDTO>> getSessionsByExperience(
            @PathVariable Long experienceId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean onlyAvailable) throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceSessionService.searchSessions(
                experienceId, onlyAvailable, null, null, pageRequest(page, size)));
    }

    // Un turno puntual por id.
    @GetMapping("/{sessionId}")
    public ResponseEntity<ExperienceSessionResponseDTO> getSessionById(@PathVariable Long sessionId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceSessionService.getSessionById(sessionId));
    }

    // Crea un turno nuevo; solo el dueño de la experiencia o un ADMIN.
    @PostMapping
    public ResponseEntity<ExperienceSessionResponseDTO> createSession(
            @RequestBody ExperienceSessionRequestDTO request,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        ExperienceSessionResponseDTO result = experienceSessionService.createSession(request, currentUser);
        return ResponseEntity.created(URI.create("/experience-sessions/" + result.getId())).body(result);
    }

    // Edita fecha/capacidad de un turno; solo el dueño de la experiencia o un ADMIN.
    @PutMapping("/{sessionId}")
    public ResponseEntity<ExperienceSessionResponseDTO> updateSession(
            @PathVariable Long sessionId,
            @RequestBody ExperienceSessionRequestDTO request,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        return ResponseEntity.ok(experienceSessionService.updateSession(sessionId, request, currentUser));
    }

    // Borra un turno; falla si ya tiene reservas. Solo el dueño de la experiencia o un ADMIN.
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        experienceSessionService.deleteSession(sessionId, currentUser);
        return ResponseEntity.noContent().build();
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);
    }
}
