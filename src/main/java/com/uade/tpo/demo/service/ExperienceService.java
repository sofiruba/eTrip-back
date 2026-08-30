package com.uade.tpo.demo.service;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceService {
    Page<ExperienceResponseDTO> getExperiences(Pageable pageable);

    ExperienceResponseDTO getExperienceById(Long experienceId) throws ResourceNotFoundException;

    ExperienceResponseDTO createExperience(ExperienceRequestDTO request, MultipartFile image)
            throws ResourceNotFoundException, IOException;

    ExperienceResponseDTO updateExperience(Long experienceId, ExperienceRequestDTO request, MultipartFile image)
            throws ResourceNotFoundException, IOException;

    void deleteExperience(Long experienceId) throws ResourceNotFoundException;
}
