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

import com.uade.tpo.demo.dtos.request.CartItemRequestDTO;
import com.uade.tpo.demo.dtos.response.CartResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.CartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("carts")
@RequiredArgsConstructor
public class CartsController {

    private final CartService cartService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<CartResponseDTO> getCartByUserId(@PathVariable Long userId)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado coincida con userId o tenga rol ADMIN.
        // 2. Llamar a cartService.getCartByUserId(userId).
        // 3. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addItem(@RequestBody CartItemRequestDTO request)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado pueda modificar el carrito indicado.
        // 2. Delegar en cartService.addItem(request).
        // 3. Retornar ResponseEntity.ok(carritoActualizado).
        return null;
    }

    @PatchMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponseDTO> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar permisos sobre el carrito del usuario.
        // 2. Delegar en cartService.updateItemQuantity(userId, cartItemId, quantity).
        // 3. Retornar ResponseEntity.ok(carritoActualizado).
        return null;
    }

    @DeleteMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar permisos sobre el carrito del usuario.
        // 2. Delegar en cartService.removeItem(userId, cartItemId).
        // 3. Retornar ResponseEntity.noContent().build().
        return null;
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar permisos sobre el carrito del usuario.
        // 2. Delegar en cartService.clearCart(userId).
        // 3. Retornar ResponseEntity.noContent().build().
        return null;
    }
}
