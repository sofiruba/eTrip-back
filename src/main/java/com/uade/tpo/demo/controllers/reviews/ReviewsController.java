package com.uade.tpo.demo.controllers.reviews;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.ReviewRequestDTO;
import com.uade.tpo.demo.dtos.response.ReviewResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("reviews")
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewService reviewService;

    // Listado general de reseñas: CLIENTE ve solo las propias, ADMIN ve todas. Para las de un
    // producto puntual (públicas) usar /reviews/experience/{id}.
    @GetMapping
    public ResponseEntity<Page<ReviewResponseDTO>> getReviews(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(reviewService.getReviews(currentUser, pageRequest));
    }

    // Reseñas de una experiencia puntual; siempre públicas (prueba social para cualquiera).
    @GetMapping("/experience/{experienceId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByExperience(
            @PathVariable Long experienceId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) throws ResourceNotFoundException {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(reviewService.getReviewsByExperience(experienceId, pageRequest));
    }

    // Mis reseñas.
    @GetMapping("/mine")
    public ResponseEntity<Page<ReviewResponseDTO>> getMyReviews(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(reviewService.getMyReviews(currentUser, pageRequest));
    }

    // Una reseña puntual; solo la ve el autor o un ADMIN.
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> getReviewById(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser) throws ResourceNotFoundException, ForbiddenException {
        return ResponseEntity.ok(reviewService.getReviewById(reviewId, currentUser));
    }

    // Crea una reseña; una por usuario y experiencia. El autor sale del token, no del body.
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @RequestBody ReviewRequestDTO request,
            @AuthenticationPrincipal User currentUser) throws ResourceNotFoundException, BadRequestException {
        ReviewResponseDTO result = reviewService.createReview(request, currentUser);
        return ResponseEntity.created(URI.create("/reviews/" + result.getId())).body(result);
    }

    // Borra una reseña; solo el autor o un ADMIN.
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser) throws ResourceNotFoundException, ForbiddenException {
        reviewService.deleteReview(reviewId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
