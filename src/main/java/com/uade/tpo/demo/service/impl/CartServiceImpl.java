package com.uade.tpo.demo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.CartItemRequestDTO;
import com.uade.tpo.demo.dtos.response.CartResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.repository.CartRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.CartService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ExperienceSessionRepository experienceSessionRepository;
    private final UserRepository userRepository;

    @Override
    public CartResponseDTO getCartByUserId(Long userId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar User por id usando userRepository.findById(userId). Lanzar ResourceNotFoundException si no existe.
        // 2. Buscar Cart usando cartRepository.findByUserId(userId).
        // 3. Si el usuario aun no tiene carrito, definir si se crea automaticamente o se retorna vacio.
        // 4. Mapear CartItem a CartItemResponseDTO incluyendo datos de ExperienceSession y precio de Experience.
        // 5. Calcular total del carrito sumando quantity * unitPrice.
        // 6. Retornar CartResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public CartResponseDTO addItem(CartItemRequestDTO request) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar User con request.getUserId(). Lanzar ResourceNotFoundException si no existe.
        // 2. Buscar ExperienceSession con request.getExperienceSessionId(). Lanzar ResourceNotFoundException si no existe.
        // 3. Validar que request.getQuantity() sea mayor a cero.
        // 4. Validar que la sesion tenga availableSeats suficientes.
        // 5. Buscar o crear el Cart del usuario.
        // 6. Si ya existe un CartItem para esa misma ExperienceSession, sumar cantidades.
        // 7. Si no existe, crear CartItem y asociarlo al Cart y a la ExperienceSession.
        // 8. Guardar usando cartItemRepository.save().
        // 9. Retornar el carrito actualizado con getCartByUserId().
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar User por userId. Lanzar ResourceNotFoundException si no existe.
        // 2. Buscar CartItem por cartItemId. Lanzar ResourceNotFoundException si no existe.
        // 3. Validar que el CartItem pertenezca al Cart del usuario.
        // 4. Validar que quantity sea mayor a cero.
        // 5. Validar cupos disponibles en la ExperienceSession.
        // 6. Actualizar quantity y guardar usando cartItemRepository.save().
        // 7. Retornar CartResponseDTO actualizado.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void removeItem(Long userId, Long cartItemId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar User por userId. Lanzar ResourceNotFoundException si no existe.
        // 2. Buscar CartItem por cartItemId. Lanzar ResourceNotFoundException si no existe.
        // 3. Validar que el item pertenezca al carrito del usuario.
        // 4. Eliminar usando cartItemRepository.delete().
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void clearCart(Long userId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar User por userId. Lanzar ResourceNotFoundException si no existe.
        // 2. Buscar el Cart del usuario.
        // 3. Si no existe carrito, no hacer nada o lanzar excepcion segun definicion funcional.
        // 4. Eliminar todos los CartItem asociados al Cart.
        // 5. Guardar el estado final del Cart si hace falta.
    }
}
