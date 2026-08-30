package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface OrderService {
    Page<OrderResponseDTO> getOrders(Pageable pageable);

    OrderResponseDTO getOrderById(Long orderId) throws ResourceNotFoundException;

    OrderResponseDTO createOrder(OrderRequestDTO request) throws ResourceNotFoundException;
}
