package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.ExperienceSessionRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceSessionResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceSessionService {
    Page<ExperienceSessionResponseDTO> getSessions(Pageable pageable);

    Page<ExperienceSessionResponseDTO> getSessionsByExperience(Long experienceId, Pageable pageable)
            throws ResourceNotFoundException;

    ExperienceSessionResponseDTO getSessionById(Long sessionId) throws ResourceNotFoundException;

    ExperienceSessionResponseDTO createSession(ExperienceSessionRequestDTO request)
            throws ResourceNotFoundException, BadRequestException;

    ExperienceSessionResponseDTO updateSession(Long sessionId, ExperienceSessionRequestDTO request)
            throws ResourceNotFoundException, BadRequestException;

    void deleteSession(Long sessionId) throws ResourceNotFoundException, BadRequestException;
}
