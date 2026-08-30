package com.uade.tpo.demo.service;

import com.uade.tpo.demo.dtos.request.CartItemRequestDTO;
import com.uade.tpo.demo.dtos.response.CartResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface CartService {
    CartResponseDTO getCartByUserId(Long userId) throws ResourceNotFoundException;

    CartResponseDTO addItem(CartItemRequestDTO request) throws ResourceNotFoundException;

    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity) throws ResourceNotFoundException;

    void removeItem(Long userId, Long cartItemId) throws ResourceNotFoundException;

    void clearCart(Long userId) throws ResourceNotFoundException;
}
