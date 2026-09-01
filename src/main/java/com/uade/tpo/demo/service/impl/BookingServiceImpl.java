package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Order;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.service.BookingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookings(User user, Pageable pageable) {
        Page<Booking> bookings = isAdmin(user)
                ? bookingRepository.findAll(pageable)
                : bookingRepository.findByOrderUserId(user.getId(), pageable);
        return bookings.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(ResourceNotFoundException::new);

        Order order = booking.getOrder();
        boolean isOwner = order != null
                && order.getUser() != null
                && order.getUser().getId().equals(user.getId());
        if (!isOwner && !isAdmin(user)) {
            throw new ForbiddenException();
        }

        return toResponse(booking);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private BookingResponseDTO toResponse(Booking booking) {
        ExperienceSession session = booking.getExperienceSession();
        Experience experience = session != null ? session.getExperience() : null;

        return BookingResponseDTO.builder()
                .id(booking.getId())
                .voucherCode(booking.getVoucherCode())
                .orderId(booking.getOrder() != null ? booking.getOrder().getId() : null)
                .experienceSessionId(session != null ? session.getId() : null)
                .experienceId(experience != null ? experience.getId() : null)
                .experienceTitle(experience != null ? experience.getTitle() : null)
                .startsAt(session != null ? session.getStartsAt() : null)
                .endsAt(session != null ? session.getEndsAt() : null)
                .quantity(booking.getQuantity())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
