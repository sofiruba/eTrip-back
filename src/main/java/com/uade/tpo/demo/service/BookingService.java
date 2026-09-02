package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface BookingService {
    Page<BookingResponseDTO> getBookings(User user, Pageable pageable);

    Page<BookingResponseDTO> getSales(User seller, Pageable pageable);

    Page<BookingResponseDTO> getBookingsByExperience(Long experienceId, User requester, Pageable pageable)
            throws ResourceNotFoundException, ForbiddenException;

    BookingResponseDTO getBookingById(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException;
}
