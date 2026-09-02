package com.uade.tpo.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.BookingResponseDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Cart;
import com.uade.tpo.demo.entity.CartItem;
import com.uade.tpo.demo.entity.DiscountCoupon;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Order;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.repository.CartRepository;
import com.uade.tpo.demo.repository.DiscountCouponRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.OrderRepository;
import com.uade.tpo.demo.service.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final DiscountCouponRepository discountCouponRepository;
    private final BookingRepository bookingRepository;
    private final ExperienceSessionRepository experienceSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrders(User user, Pageable pageable) {
        Page<Order> orders = isAdmin(user)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByUserId(user.getId(), pageable);
        return orders.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(ResourceNotFoundException::new);

        boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        if (!isOwner && !isAdmin(user)) {
            throw new ForbiddenException();
        }

        return toResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrderResponseDTO createOrder(User user, OrderRequestDTO request)
            throws ResourceNotFoundException, BadRequestException {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(ResourceNotFoundException::new);

        List<CartItem> items = cart.getItems() != null
                ? new ArrayList<>(cart.getItems())
                : new ArrayList<>();
        if (items.isEmpty()) {
            throw new BadRequestException();
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            if (session.getAvailableSeats() == null || session.getAvailableSeats() < item.getQuantity()) {
                throw new BadRequestException();
            }
            Experience experience = session.getExperience();
            BigDecimal unitPrice = experience != null && experience.getPrice() != null
                    ? experience.getPrice()
                    : BigDecimal.ZERO;
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        DiscountCoupon coupon = resolveCoupon(request);
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (coupon != null) {
            discountAmount = subtotal
                    .multiply(coupon.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal total = subtotal.subtract(discountAmount);
        long totalTickets = items.stream().mapToLong(CartItem::getQuantity).sum();

        Order order = Order.builder()
                .user(user)
                .discountCoupon(coupon)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .total(total)
                .count(totalTickets)
                .createdAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        List<Booking> bookings = new ArrayList<>();
        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            session.setAvailableSeats(session.getAvailableSeats() - item.getQuantity());
            experienceSessionRepository.save(session);

            Booking booking = Booking.builder()
                    .order(order)
                    .experienceSession(session)
                    .quantity(item.getQuantity())
                    .voucherCode(generateVoucherCode())
                    .createdAt(LocalDateTime.now())
                    .build();
            bookings.add(bookingRepository.save(booking));
        }

        cartItemRepository.deleteAll(items);

        order.setBookings(bookings);
        return toResponse(order);
    }

    private DiscountCoupon resolveCoupon(OrderRequestDTO request)
            throws ResourceNotFoundException, BadRequestException {
        if (request == null || request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            return null;
        }

        DiscountCoupon coupon = discountCouponRepository
                .findByCode(request.getCouponCode().trim().toUpperCase())
                .orElseThrow(ResourceNotFoundException::new);

        // Mismo criterio de validez que GET /discount-coupons/validate.
        if (DiscountCouponServiceImpl.reasonIfInvalid(coupon) != null) {
            throw new BadRequestException();
        }

        return coupon;
    }

    private String generateVoucherCode() {
        return "ETRIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private OrderResponseDTO toResponse(Order order) {
        List<BookingResponseDTO> bookingDtos = new ArrayList<>();
        List<Booking> bookings = order.getBookings();
        if (bookings != null) {
            for (Booking booking : bookings) {
                ExperienceSession session = booking.getExperienceSession();
                Experience experience = session != null ? session.getExperience() : null;
                bookingDtos.add(BookingResponseDTO.builder()
                        .id(booking.getId())
                        .voucherCode(booking.getVoucherCode())
                        .orderId(order.getId())
                        .experienceSessionId(session != null ? session.getId() : null)
                        .experienceId(experience != null ? experience.getId() : null)
                        .experienceTitle(experience != null ? experience.getTitle() : null)
                        .startsAt(session != null ? session.getStartsAt() : null)
                        .endsAt(session != null ? session.getEndsAt() : null)
                        .quantity(booking.getQuantity())
                        .createdAt(booking.getCreatedAt())
                        .build());
            }
        }

        return OrderResponseDTO.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .couponCode(order.getDiscountCoupon() != null ? order.getDiscountCoupon().getCode() : null)
                .createdAt(order.getCreatedAt())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .total(order.getTotal())
                .bookings(bookingDtos)
                .build();
    }
}
