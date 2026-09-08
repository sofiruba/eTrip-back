package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface BookingService {
    // Mis vouchers; ADMIN ve los de todos.
    Page<BookingResponseDTO> getBookings(User user, Pageable pageable);

    // Vista vendedor: reservas sobre experiencias que publicó este usuario.
    Page<BookingResponseDTO> getSales(User seller, Pageable pageable);

    // Reservas de una experiencia puntual; solo el dueño de esa experiencia o un ADMIN.
    Page<BookingResponseDTO> getBookingsByExperience(Long experienceId, User requester, Pageable pageable)
            throws ResourceNotFoundException, ForbiddenException;

    // Un voucher puntual; lo ve el comprador, el vendedor, o un ADMIN.
    BookingResponseDTO getBookingById(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException;
}
