package com.uade.tpo.demo.controllers.bookings;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("bookings")
@RequiredArgsConstructor
public class BookingsController {

    private final BookingService bookingService;

    /** Vouchers/reservas que hizo el usuario autenticado (ADMIN ve todos). */
    @GetMapping
    public ResponseEntity<Page<BookingResponseDTO>> getBookings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getBookings(user, pageRequest(page, size)));
    }

    /** Vista de vendedor: reservas sobre las experiencias que publico el usuario. */
    @GetMapping("/sales")
    public ResponseEntity<Page<BookingResponseDTO>> getSales(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getSales(user, pageRequest(page, size)));
    }

    /** Reservas de una experiencia puntual. Solo el dueño de la experiencia o un ADMIN. */
    @GetMapping("/experience/{experienceId}")
    public ResponseEntity<Page<BookingResponseDTO>> getBookingsByExperience(
            @PathVariable Long experienceId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User user) throws ResourceNotFoundException, ForbiddenException {
        return ResponseEntity.ok(
                bookingService.getBookingsByExperience(experienceId, user, pageRequest(page, size)));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User user) throws ResourceNotFoundException, ForbiddenException {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, user));
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);
    }
}
