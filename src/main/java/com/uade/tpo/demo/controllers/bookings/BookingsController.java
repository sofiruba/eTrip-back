package com.uade.tpo.demo.controllers.bookings;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("bookings")
@RequiredArgsConstructor
public class BookingsController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<Page<BookingResponseDTO>> getBookings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado tenga rol ADMIN para listar todas las reservas.
        // 2. Armar PageRequest con defaults cuando page o size sean null.
        // 3. Llamar a bookingService.getBookings(pageRequest).
        // 4. Retornar ResponseEntity.ok(resultado).
        return null;
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long bookingId)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a bookingService.getBookingById(bookingId).
        // 2. Validar que la reserva pertenezca al usuario autenticado o que sea ADMIN.
        // 3. Retornar ResponseEntity.ok(dto).
        return null;
    }
}
