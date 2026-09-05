package com.uade.tpo.demo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.dtos.response.CartItemResponseDTO;
import com.uade.tpo.demo.entity.Cart;
import com.uade.tpo.demo.entity.CartItem;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.dtos.request.CartItemRequestDTO;
import com.uade.tpo.demo.dtos.response.CartResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.repository.CartRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.CartService;
import com.uade.tpo.demo.exceptions.BadRequestException;

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
        User user = userRepository.findById(userId)
            .orElseThrow(ResourceNotFoundException::new);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();

                    return cartRepository.save(newCart);
                });

        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        if (cart.getItems() != null) {

            for (CartItem item : cart.getItems()) {

                BigDecimal unitPrice =
                        item.getExperienceSession()
                                .getExperience()
                                .getPrice();

                CartItemResponseDTO itemDTO = CartItemResponseDTO.builder()
                        .id(item.getId())
                        .experienceSessionId(
                                item.getExperienceSession().getId()
                        )
                        .experienceTitle(
                                item.getExperienceSession()
                                        .getExperience()
                                        .getTitle()
                        )
                        .quantity(item.getQuantity())
                        .unitPrice(unitPrice)
                        .build();

                itemDTOs.add(itemDTO);

                BigDecimal itemTotal =
                        unitPrice.multiply(
                                BigDecimal.valueOf(item.getQuantity())
                        );

                total = total.add(itemTotal);
            }
        }

        return CartResponseDTO.builder()
                .id(cart.getId())
                .userId(user.getId())
                .items(itemDTOs)
                .total(total)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public CartResponseDTO addItem(CartItemRequestDTO request) throws ResourceNotFoundException, BadRequestException {
            User user = userRepository.findById(request.getUserId())
            .orElseThrow(ResourceNotFoundException::new);

    ExperienceSession session = experienceSessionRepository
                .findById(request.getExperienceSessionId())
                .orElseThrow(ResourceNotFoundException::new);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException();
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();

                    return cartRepository.save(newCart);
                });

        CartItem cartItem = cartItemRepository
                .findByCartIdAndExperienceSessionId(
                        cart.getId(),
                        session.getId()
                )
                .orElse(null);

        int newQuantity = request.getQuantity();

        if (cartItem != null) {
            newQuantity += cartItem.getQuantity();
        }

        if (session.getAvailableSeats() == null
                || newQuantity > session.getAvailableSeats()) {

            throw new BadRequestException();
        }

        if (cartItem == null) {
            cartItem = CartItem.builder()
                    .cart(cart)
                    .experienceSession(session)
                    .quantity(request.getQuantity())
                    .build();
        } else {
            cartItem.setQuantity(newQuantity);
        }

        cartItemRepository.save(cartItem);

        return getCartByUserId(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity)
            throws ResourceNotFoundException, BadRequestException {
        
        User user = userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::new);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(ResourceNotFoundException::new);

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new BadRequestException();
        }

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException();
        }

        ExperienceSession session = cartItem.getExperienceSession();

        if (session.getAvailableSeats() == null
                || quantity > session.getAvailableSeats()) {

            throw new BadRequestException();
        }

        cartItem.setQuantity(quantity);

        cartItemRepository.save(cartItem);
    
        return getCartByUserId(userId);

    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void removeItem(Long userId, Long cartItemId) throws ResourceNotFoundException {
          User user = userRepository.findById(userId)
             .orElseThrow(ResourceNotFoundException::new);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
             .orElseThrow(ResourceNotFoundException::new);

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                   "El item no pertenece al carrito del usuario"
            );
        }

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void clearCart(Long userId) throws ResourceNotFoundException {

        User user = userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::new);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElse(null);

        if (cart == null) {
            return;
        }

        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            cartItemRepository.deleteAll(cart.getItems());
        }
    }
}
