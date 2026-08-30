package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface BookingService {
    Page<BookingResponseDTO> getBookings(Pageable pageable);

    BookingResponseDTO getBookingById(Long bookingId) throws ResourceNotFoundException;
}
