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
    // Mis órdenes; ADMIN ve todas.
    Page<OrderResponseDTO> getOrders(User user, Pageable pageable);

    // Una orden puntual; solo el dueño o un ADMIN.
    OrderResponseDTO getOrderById(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException;

    // Confirma el carrito del usuario y genera la orden con sus vouchers.
    OrderResponseDTO createOrder(User user, OrderRequestDTO request)
            throws ResourceNotFoundException, BadRequestException;
}
