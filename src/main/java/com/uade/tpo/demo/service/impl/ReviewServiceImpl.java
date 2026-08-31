package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ReviewRequestDTO;
import com.uade.tpo.demo.dtos.response.ReviewResponseDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.Review;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ReviewRepository;
import com.uade.tpo.demo.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ExperienceRepository experienceRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviewsByExperience(Long experienceId, Pageable pageable)
            throws ResourceNotFoundException {
        if (!experienceRepository.existsById(experienceId)) {
            throw new ResourceNotFoundException();
        }

        return reviewRepository.findByExperienceId(experienceId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDTO getReviewById(Long reviewId) throws ResourceNotFoundException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ResourceNotFoundException::new);

        return toResponse(review);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ReviewResponseDTO createReview(ReviewRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException {
        validateRequest(request, currentUser);

        Experience experience = experienceRepository.findById(request.getExperienceId())
                .orElseThrow(ResourceNotFoundException::new);

        if (reviewRepository.existsByExperienceIdAndUserId(experience.getId(), currentUser.getId())) {
            throw new BadRequestException();
        }

        Review review = Review.builder()
                .experience(experience)
                .user(currentUser)
                .rating(request.getRating())
                .comment(trimToNull(request.getComment()))
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteReview(Long reviewId, User currentUser)
            throws ResourceNotFoundException, ForbiddenException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ResourceNotFoundException::new);

        boolean isOwner = currentUser != null
                && review.getUser() != null
                && review.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException();
        }

        reviewRepository.delete(review);
    }

    private void validateRequest(ReviewRequestDTO request, User currentUser) throws BadRequestException {
        if (currentUser == null || request == null) {
            throw new BadRequestException();
        }

        if (request.getExperienceId() == null) {
            throw new BadRequestException();
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException();
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReviewResponseDTO toResponse(Review review) {
        Experience experience = review.getExperience();
        User user = review.getUser();

        return ReviewResponseDTO.builder()
                .id(review.getId())
                .experienceId(experience != null ? experience.getId() : null)
                .experienceTitle(experience != null ? experience.getTitle() : null)
                .userId(user != null ? user.getId() : null)
                .userName(userName(user))
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String userName(User user) {
        if (user == null) {
            return null;
        }

        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }
}
