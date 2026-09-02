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
    Page<ReviewResponseDTO> getReviews(Pageable pageable);

    Page<ReviewResponseDTO> getReviewsByExperience(Long experienceId, Pageable pageable) throws ResourceNotFoundException;

    Page<ReviewResponseDTO> getMyReviews(User currentUser, Pageable pageable);

    ReviewResponseDTO getReviewById(Long reviewId) throws ResourceNotFoundException;

    ReviewResponseDTO createReview(ReviewRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException;

    void deleteReview(Long reviewId, User currentUser)
            throws ResourceNotFoundException, ForbiddenException;
}
