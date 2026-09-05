package com.uade.tpo.demo.service;

import com.uade.tpo.demo.dtos.request.CartItemRequestDTO;
import com.uade.tpo.demo.dtos.response.CartResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;

public interface CartService {
    CartResponseDTO getCartByUserId(Long userId) throws ResourceNotFoundException;

    CartResponseDTO addItem(CartItemRequestDTO request) throws ResourceNotFoundException, BadRequestException;

    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    void removeItem(Long userId, Long cartItemId) throws ResourceNotFoundException, ForbiddenException;

    void clearCart(Long userId) throws ResourceNotFoundException;
}
