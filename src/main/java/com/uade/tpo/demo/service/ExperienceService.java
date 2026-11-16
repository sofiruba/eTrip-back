package com.uade.tpo.demo.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.dtos.response.ExperienceDeleteResultDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.dtos.response.RecommendationResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceService {
    // Busca experiencias combinando filtros opcionales (includeInactive: también las pausadas, para gestión;
    // includeDeleted: también las eliminadas, solo para el historial del admin).
    Page<ExperienceResponseDTO> searchExperiences(ExperienceSearchDTO filter, Pageable pageable, boolean includeInactive,
            boolean includeDeleted) throws ResourceNotFoundException, BadRequestException;

    // Una experiencia puntual por id (una pausada solo la ven su anfitrión y los administradores).
    ExperienceResponseDTO getExperienceById(Long experienceId, User currentUser) throws ResourceNotFoundException;

    // "Para vos": recomendaciones según intereses, guardadas, reservas y vistas (viewedIds lo manda el front)
    List<RecommendationResponseDTO> recommend(User currentUser, List<Long> viewedIds);

    // Crea una experiencia nueva con sus fotos.
    ExperienceResponseDTO createExperience(ExperienceRequestDTO request, List<MultipartFile> images, Long publisherId)
            throws ResourceNotFoundException, BadRequestException, IOException;

    // Actualiza una experiencia existente. Solo el dueño o un ADMIN.
    ExperienceResponseDTO updateExperience(Long experienceId, ExperienceRequestDTO request, List<MultipartFile> images,
            User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException, IOException;

    // Gestión de descuentos sobre la experiencia individual. Solo el dueño o un ADMIN.
    ExperienceResponseDTO updateDiscount(Long experienceId, BigDecimal discountPercentage, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Elimina una experiencia: cancela sus fechas futuras, reembolsa y avisa a los compradores. Solo el dueño o un ADMIN.
    ExperienceDeleteResultDTO deleteExperience(Long experienceId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Pausa o vuelve a publicar (baja lógica). Las reservas que ya existen siguen vigentes.
    ExperienceResponseDTO updateStatus(Long experienceId, boolean active, User currentUser)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException;
}
