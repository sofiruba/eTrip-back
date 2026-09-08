package com.uade.tpo.demo.controllers.carts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.dtos.request.CartItemRequestDTO;
import com.uade.tpo.demo.dtos.response.CartResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.CartService;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("carts")
@RequiredArgsConstructor
public class CartsController {

    private final CartService cartService;

    // Solo el dueño del carrito o un ADMIN puede operar sobre él.
    private void validateUserAccess(User authenticatedUser, Long userId) {
        boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;
        boolean isOwner = authenticatedUser.getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "No tiene permisos para acceder a este carrito"
            );
        }
    }

    // Trae el carrito de un usuario (lo crea vacío si todavía no tiene uno).
    @GetMapping("/user/{userId}")
    public ResponseEntity<CartResponseDTO> getCartByUserId(@PathVariable Long userId, @AuthenticationPrincipal User authenticatedUser)
            throws ResourceNotFoundException {
        validateUserAccess(authenticatedUser, userId);
        CartResponseDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    // Agrega una sesión al carrito. userId es opcional: si no lo mandás, se usa el usuario del token.
    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(@RequestBody CartItemRequestDTO request, @AuthenticationPrincipal User authenticatedUser)
            throws ResourceNotFoundException, BadRequestException {
        if (request.getUserId() == null) {
            request.setUserId(authenticatedUser.getId());
        }
        validateUserAccess(authenticatedUser, request.getUserId());
        CartResponseDTO cart = cartService.addItem(request);
        return ResponseEntity.ok(cart);
    }

    // Cambia la cantidad de un item ya agregado al carrito.
    @PatchMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal User authenticatedUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        validateUserAccess(authenticatedUser, userId);
        CartResponseDTO cart = cartService.updateItemQuantity(userId, cartItemId, quantity);
        return ResponseEntity.ok(cart);
    }

    // Saca un item puntual del carrito.
    @DeleteMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal User authenticatedUser)
            throws ResourceNotFoundException, ForbiddenException {
        validateUserAccess(authenticatedUser, userId);
        cartService.removeItem(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    // Vacía el carrito entero.
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCart(
            @PathVariable Long userId,
            @AuthenticationPrincipal User authenticatedUser)
            throws ResourceNotFoundException {
        validateUserAccess(authenticatedUser, userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
