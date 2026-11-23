package com.uade.tpo.demo.controllers.reviews;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.ReviewReplyRequestDTO;
import com.uade.tpo.demo.dtos.request.ReviewRequestDTO;
import com.uade.tpo.demo.dtos.response.ReviewResponseDTO;
import com.uade.tpo.demo.dtos.response.ReviewStatsDTO;
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

    // Reseñas: un CLIENTE ve solo las propias y un ADMIN, todas. Las de una experiencia (públicas)
    // están en /reviews/experience/{id}.
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
    // Paginadas: ?page=0&size=4. sort = recent (default) | best | worst; q = buscar en el texto.
    @GetMapping("/experience/{experienceId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByExperience(
            @PathVariable Long experienceId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q) throws ResourceNotFoundException {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE, reviewSort(sort))
                : PageRequest.of(page, size, reviewSort(sort));

        return ResponseEntity.ok(reviewService.getReviewsByExperience(experienceId, q, pageRequest));
    }

    // Promedio, cantidad y cuántas reseñas hay de cada puntaje (sobre todas, no sobre una página).
    @GetMapping("/experience/{experienceId}/stats")
    public ResponseEntity<ReviewStatsDTO> getReviewStats(@PathVariable Long experienceId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(reviewService.getReviewStats(experienceId));
    }

    // Orden de las reseñas; a igual puntaje, primero las más nuevas
    private Sort reviewSort(String sort) {
        Sort newest = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("best".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "rating").and(newest);
        }
        if ("worst".equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "rating").and(newest);
        }
        return newest;
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

    // Respuesta del anfitrión a una reseña (crear, editar o borrar con reply vacío).
    @PutMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponseDTO> replyToReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewReplyRequestDTO request,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        return ResponseEntity.ok(reviewService.replyToReview(reviewId, request, currentUser));
    }

    // Borra una reseña; solo el autor o un ADMIN. 200 con la reseña borrada y el promedio nuevo de la experiencia.
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser) throws ResourceNotFoundException, ForbiddenException {
        return ResponseEntity.ok(reviewService.deleteReview(reviewId, currentUser));
    }
}
