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
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.service.BookingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ExperienceRepository experienceRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookings(User user, Pageable pageable) {
        Page<Booking> bookings = isAdmin(user)
                ? bookingRepository.findAll(pageable)
                : bookingRepository.findByOrder_User_Id(user.getId(), pageable);
        return bookings.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getSales(User seller, Pageable pageable) {
        return bookingRepository.findByExperienceSession_Experience_Publisher_Id(seller.getId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getBookingsByExperience(Long experienceId, User requester, Pageable pageable)
            throws ResourceNotFoundException, ForbiddenException {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(ResourceNotFoundException::new);

        boolean isOwner = experience.getPublisher() != null
                && experience.getPublisher().getId().equals(requester.getId());
        if (!isOwner && !isAdmin(requester)) {
            throw new ForbiddenException();
        }

        return bookingRepository.findByExperienceSession_Experience_Id(experienceId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long bookingId, User user)
            throws ResourceNotFoundException, ForbiddenException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(ResourceNotFoundException::new);

        Order order = booking.getOrder();
        boolean isBuyer = order != null
                && order.getUser() != null
                && order.getUser().getId().equals(user.getId());

        ExperienceSession session = booking.getExperienceSession();
        boolean isSeller = session != null
                && session.getExperience() != null
                && session.getExperience().getPublisher() != null
                && session.getExperience().getPublisher().getId().equals(user.getId());

        if (!isBuyer && !isSeller && !isAdmin(user)) {
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
        Order order = booking.getOrder();
        User buyer = order != null ? order.getUser() : null;

        return BookingResponseDTO.builder()
                .id(booking.getId())
                .voucherCode(booking.getVoucherCode())
                .orderId(order != null ? order.getId() : null)
                .experienceSessionId(session != null ? session.getId() : null)
                .experienceId(experience != null ? experience.getId() : null)
                .experienceTitle(experience != null ? experience.getTitle() : null)
                .startsAt(session != null ? session.getStartsAt() : null)
                .endsAt(session != null ? session.getEndsAt() : null)
                .quantity(booking.getQuantity())
                .createdAt(booking.getCreatedAt())
                .buyerId(buyer != null ? buyer.getId() : null)
                .buyerName(fullName(buyer))
                .build();
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
