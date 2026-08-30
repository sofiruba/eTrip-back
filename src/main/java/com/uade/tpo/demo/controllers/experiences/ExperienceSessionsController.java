package com.uade.tpo.demo.controllers.experiences;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
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
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ExperienceSessionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("experience-sessions")
@RequiredArgsConstructor
public class ExperienceSessionsController {

    private final ExperienceSessionService experienceSessionService;

    @GetMapping
    public ResponseEntity<Page<ExperienceSessionResponseDTO>> getSessions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Armar PageRequest con defaults cuando page o size sean null.
        // 2. Llamar a experienceSessionService.getSessions(pageRequest).
        // 3. Retornar ResponseEntity.ok(resultado).
        return null;
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ExperienceSessionResponseDTO> getSessionById(@PathVariable Long sessionId)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a experienceSessionService.getSessionById(sessionId).
        // 2. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @PostMapping
    public ResponseEntity<ExperienceSessionResponseDTO> createSession(
            @RequestBody ExperienceSessionRequestDTO request) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Delegar en experienceSessionService.createSession(request).
        // 2. Retornar ResponseEntity.created(URI.create("/experience-sessions/" + result.getId())).body(result).
        return null;
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<ExperienceSessionResponseDTO> updateSession(
            @PathVariable Long sessionId,
            @RequestBody ExperienceSessionRequestDTO request) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Delegar en experienceSessionService.updateSession(sessionId, request).
        // 2. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Delegar en experienceSessionService.deleteSession(sessionId).
        // 2. Retornar ResponseEntity.noContent().build().
        return null;
    }
}
