package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface OrderService {
    Page<OrderResponseDTO> getOrders(User user, Pageable pageable);

    OrderResponseDTO getOrderById(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException;

    OrderResponseDTO createOrder(User user, OrderRequestDTO request)
            throws ResourceNotFoundException, BadRequestException;
}
