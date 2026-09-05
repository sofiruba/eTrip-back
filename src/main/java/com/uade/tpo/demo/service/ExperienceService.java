package com.uade.tpo.demo.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceService {
    Page<ExperienceResponseDTO> getExperiences(Pageable pageable);

    Page<ExperienceResponseDTO> searchExperiences(ExperienceSearchDTO filter, Pageable pageable)
            throws ResourceNotFoundException, BadRequestException;

    ExperienceResponseDTO getExperienceById(Long experienceId) throws ResourceNotFoundException;

    ExperienceResponseDTO createExperience(ExperienceRequestDTO request, List<MultipartFile> images, Long publisherId)
            throws ResourceNotFoundException, BadRequestException, IOException;

    ExperienceResponseDTO updateExperience(Long experienceId, ExperienceRequestDTO request, List<MultipartFile> images,
            User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException, IOException;

    /** Gestion de descuentos sobre la experiencia individual. Solo el dueño o un ADMIN. */
    ExperienceResponseDTO updateDiscount(Long experienceId, BigDecimal discountPercentage, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    void deleteExperience(Long experienceId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;
}
