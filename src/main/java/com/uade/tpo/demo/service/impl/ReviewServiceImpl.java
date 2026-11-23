package com.uade.tpo.demo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ReviewReplyRequestDTO;
import com.uade.tpo.demo.dtos.request.ReviewRequestDTO;
import com.uade.tpo.demo.dtos.response.ReviewResponseDTO;
import com.uade.tpo.demo.dtos.response.ReviewStatsDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.Review;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ReviewRepository;
import com.uade.tpo.demo.service.NotificationService;
import com.uade.tpo.demo.service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ExperienceRepository experienceRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    // Listado general: CLIENTE ve solo las propias, ADMIN ve todas.
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviews(User currentUser, Pageable pageable) {
        if (isAdmin(currentUser)) {
            return reviewRepository.findAll(pageable).map(this::toResponse);
        }
        return reviewRepository.findByUserId(currentUser.getId(), pageable).map(this::toResponse);
    }

    // Reseñas de una experiencia puntual; siempre públicas.
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviewsByExperience(Long experienceId, String text, Pageable pageable)
            throws ResourceNotFoundException {
        assertExperienceExists(experienceId);

        String search = trimToNull(text);
        Page<Review> page = search == null
                ? reviewRepository.findByExperienceId(experienceId, pageable)
                : reviewRepository.findByExperienceIdAndCommentContainingIgnoreCase(experienceId, search, pageable);
        return page.map(this::toResponse);
    }

    // Promedio, cantidad y distribución por puntaje (de 5 a 1, con 0 en los que no tienen reseñas).
    @Override
    @Transactional(readOnly = true)
    public ReviewStatsDTO getReviewStats(Long experienceId) throws ResourceNotFoundException {
        assertExperienceExists(experienceId);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            distribution.put(stars, 0L);
        }
        for (Object[] row : reviewRepository.countByRating(experienceId)) {
            distribution.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        Double average = reviewRepository.averageRatingByExperienceId(experienceId);

        return ReviewStatsDTO.builder()
                .experienceId(experienceId)
                .count(reviewRepository.countByExperienceId(experienceId))
                .average(average != null ? average : 0)
                .distribution(distribution)
                .build();
    }

    private void assertExperienceExists(Long experienceId) throws ResourceNotFoundException {
        if (!experienceRepository.existsById(experienceId)) {
            throw new ResourceNotFoundException("La experiencia no existe");
        }
    }

    // Mis reseñas.
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getMyReviews(User currentUser, Pageable pageable) {
        return reviewRepository.findByUserId(currentUser.getId(), pageable).map(this::toResponse);
    }

    // Una reseña puntual; solo el autor o un ADMIN.
    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDTO getReviewById(Long reviewId, User currentUser)
            throws ResourceNotFoundException, ForbiddenException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("La reseña no existe"));

        boolean isOwner = currentUser != null
                && review.getUser() != null
                && review.getUser().getId().equals(currentUser.getId());
        if (!isOwner && !isAdmin(currentUser)) {
            throw new ForbiddenException("Solo el autor puede ver esta reseña");
        }

        return toResponse(review);
    }

    // Crea una reseña; una por usuario y experiencia. El autor sale del token.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ReviewResponseDTO createReview(ReviewRequestDTO request, User currentUser)
            throws ResourceNotFoundException, BadRequestException {
        validateRequest(request, currentUser);

        Experience experience = experienceRepository.findById(request.getExperienceId())
                .orElseThrow(() -> new ResourceNotFoundException("La experiencia no existe"));

        if (reviewRepository.existsByExperienceIdAndUserId(experience.getId(), currentUser.getId())) {
            throw new BadRequestException("Ya reseñaste esta experiencia");
        }

        // Reseña quien tiene el voucher: quien compró o, si fue un regalo ya entregado, quien lo recibió (no quien pagó)
        List<Booking> myBookings = new ArrayList<>(bookingRepository
                .findByOrder_User_IdAndExperienceSession_Experience_Id(currentUser.getId(), experience.getId()).stream()
                .filter(booking -> booking.getRecipient() == null)
                .toList());
        myBookings.addAll(bookingRepository.findByRecipient_IdAndExperienceSession_Experience_Id(currentUser.getId(), experience.getId()));
        if (myBookings.isEmpty()) {
            throw new BadRequestException("Solo podés reseñar una experiencia a la que fuiste");
        }
        // Una reserva cancelada no habilita a reseñar; tiene que haber terminado
        boolean attended = myBookings.stream()
                .filter(booking -> !booking.isRefunded())
                .anyMatch(booking -> booking.getExperienceSession().hasEnded());
        if (!attended) {
            throw new BadRequestException("Vas a poder reseñarla cuando termine la experiencia");
        }

        Review review = Review.builder()
                .experience(experience)
                .user(currentUser)
                .rating(request.getRating())
                .comment(trimToNull(request.getComment()))
                .createdAt(LocalDateTime.now())
                .build();

        return withExperienceStats(toResponse(reviewRepository.save(review)));
    }

    // Borra una reseña; solo el autor o un ADMIN. Devuelve la reseña borrada con el promedio nuevo de la experiencia.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ReviewResponseDTO deleteReview(Long reviewId, User currentUser)
            throws ResourceNotFoundException, ForbiddenException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("La reseña no existe"));

        boolean isOwner = currentUser != null
                && review.getUser() != null
                && review.getUser().getId().equals(currentUser.getId());

        if (!isOwner && !isAdmin(currentUser)) {
            throw new ForbiddenException("Solo el autor o un administrador pueden borrar esta reseña");
        }

        ReviewResponseDTO deleted = toResponse(review);
        reviewRepository.delete(review);
        reviewRepository.flush();
        return withExperienceStats(deleted);
    }

    // Suma a la respuesta cómo quedó la experiencia (cantidad de reseñas y promedio), así el front no la vuelve a pedir
    private ReviewResponseDTO withExperienceStats(ReviewResponseDTO review) {
        Long experienceId = review.getExperienceId();
        if (experienceId != null) {
            review.setExperienceReviewCount(reviewRepository.countByExperienceId(experienceId));
            review.setExperienceAverageRating(reviewRepository.averageRatingByExperienceId(experienceId));
        }
        return review;
    }

    // Responde una reseña: solo el anfitrión (publisher) de la experiencia o un ADMIN. Vacío = borrar respuesta.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ReviewResponseDTO replyToReview(Long reviewId, ReviewReplyRequestDTO request, User currentUser)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("La reseña no existe"));

        User host = review.getExperience() != null ? review.getExperience().getPublisher() : null;
        boolean isHost = currentUser != null && host != null && host.getId().equals(currentUser.getId());
        if (!isHost && !isAdmin(currentUser)) {
            throw new ForbiddenException("Solo el anfitrión de la experiencia puede responder esta reseña");
        }

        String reply = trimToNull(request != null ? request.getReply() : null);
        if (reply != null && reply.length() > 1000) {
            throw new BadRequestException("La respuesta puede tener hasta 1000 caracteres");
        }
        boolean isNew = review.getHostReply() == null && reply != null;
        review.setHostReply(reply);
        review.setHostReplyAt(reply != null ? LocalDateTime.now() : null);
        reviewRepository.save(review);

        if (isNew && review.getUser() != null) {
            notificationService.create(review.getUser(), "Te respondieron una reseña",
                    "El anfitrión de \"" + review.getExperience().getTitle() + "\" respondió tu reseña.");
        }
        return toResponse(review);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private void validateRequest(ReviewRequestDTO request, User currentUser) throws BadRequestException {
        if (currentUser == null || request == null) {
            throw new BadRequestException("Faltan los datos de la reseña");
        }

        if (request.getExperienceId() == null) {
            throw new BadRequestException("Falta la experiencia a reseñar");
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("La puntuación tiene que ser de 1 a 5 estrellas");
        }

        if (request.getComment() != null && request.getComment().trim().length() > 1000) {
            throw new BadRequestException("La reseña puede tener hasta 1000 caracteres");
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
        User host = experience != null ? experience.getPublisher() : null;

        return ReviewResponseDTO.builder()
                .id(review.getId())
                .experienceId(experience != null ? experience.getId() : null)
                .experienceTitle(experience != null ? experience.getTitle() : null)
                .userId(user != null ? user.getId() : null)
                .userName(userName(user))
                .userAvatarBase64(user != null && user.getAvatar() != null
                        ? Base64.getEncoder().encodeToString(user.getAvatar())
                        : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .hostId(host != null ? host.getId() : null)
                .hostName(host != null ? host.getFirstName() : null)
                .hostReply(review.getHostReply())
                .hostReplyAt(review.getHostReplyAt())
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
