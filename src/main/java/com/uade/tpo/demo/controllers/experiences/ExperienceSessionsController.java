package com.uade.tpo.demo.controllers.experiences;

import java.net.URI;

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
import com.uade.tpo.demo.exceptions.BadRequestException;
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
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long experienceId) throws ResourceNotFoundException {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        if (experienceId != null) {
            return ResponseEntity.ok(experienceSessionService.getSessionsByExperience(experienceId, pageRequest));
        }

        return ResponseEntity.ok(experienceSessionService.getSessions(pageRequest));
    }

    @GetMapping("/experience/{experienceId}")
    public ResponseEntity<Page<ExperienceSessionResponseDTO>> getSessionsByExperience(
            @PathVariable Long experienceId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) throws ResourceNotFoundException {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(experienceSessionService.getSessionsByExperience(experienceId, pageRequest));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ExperienceSessionResponseDTO> getSessionById(@PathVariable Long sessionId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceSessionService.getSessionById(sessionId));
    }

    @PostMapping
    public ResponseEntity<ExperienceSessionResponseDTO> createSession(
            @RequestBody ExperienceSessionRequestDTO request) throws ResourceNotFoundException, BadRequestException {
        ExperienceSessionResponseDTO result = experienceSessionService.createSession(request);
        return ResponseEntity.created(URI.create("/experience-sessions/" + result.getId())).body(result);
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<ExperienceSessionResponseDTO> updateSession(
            @PathVariable Long sessionId,
            @RequestBody ExperienceSessionRequestDTO request) throws ResourceNotFoundException, BadRequestException {
        return ResponseEntity.ok(experienceSessionService.updateSession(sessionId, request));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId)
            throws ResourceNotFoundException, BadRequestException {
        experienceSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
