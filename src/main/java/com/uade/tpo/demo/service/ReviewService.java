package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.ReviewRequestDTO;
import com.uade.tpo.demo.dtos.response.ReviewResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ReviewService {
    // Listado general: CLIENTE ve solo las suyas, ADMIN ve todas.
    Page<ReviewResponseDTO> getReviews(User currentUser, Pageable pageable);

    // Reseñas de una experiencia puntual; a propósito públicas (prueba social del catálogo,
    // igual que el rating promedio), no solo visibles para el autor.
    Page<ReviewResponseDTO> getReviewsByExperience(Long experienceId, Pageable pageable) throws ResourceNotFoundException;

    // Mis reseñas.
    Page<ReviewResponseDTO> getMyReviews(User currentUser, Pageable pageable);

    // Una reseña puntual; CLIENTE solo la propia, ADMIN cualquiera.
    ReviewResponseDTO getReviewById(Long reviewId, User currentUser)
            throws ResourceNotFoundException, ForbiddenException;

    // Crea una reseña; una por usuario y experiencia.
    ReviewResponseDTO createReview(ReviewRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException;

    // Borra una reseña; solo el autor o un ADMIN.
    void deleteReview(Long reviewId, User currentUser)
            throws ResourceNotFoundException, ForbiddenException;
}
