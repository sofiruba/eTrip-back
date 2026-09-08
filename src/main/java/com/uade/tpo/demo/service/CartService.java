package com.uade.tpo.demo.service;

import com.uade.tpo.demo.dtos.request.CartItemRequestDTO;
import com.uade.tpo.demo.dtos.response.CartResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;

public interface CartService {
    // Trae el carrito de un usuario (lo crea vacío si no tiene).
    CartResponseDTO getCartByUserId(Long userId) throws ResourceNotFoundException;

    // Agrega una sesión al carrito, validando cupo disponible.
    CartResponseDTO addItem(CartItemRequestDTO request) throws ResourceNotFoundException, BadRequestException;

    // Cambia la cantidad de un item.
    CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Saca un item del carrito.
    void removeItem(Long userId, Long cartItemId) throws ResourceNotFoundException, ForbiddenException;

    // Vacía el carrito entero.
    void clearCart(Long userId) throws ResourceNotFoundException;
}
