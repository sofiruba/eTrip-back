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
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.entity.Experience;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ExperienceSessionRepository experienceSessionRepository;
    private final UserRepository userRepository;

    // Trae el carrito de un usuario (lo crea vacío si todavía no tiene uno) y calcula el total.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public CartResponseDTO getCartByUserId(Long userId) throws ResourceNotFoundException {
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

        // Se consulta el repository directo (no cart.getItems()) porque si el Cart se acaba de
        // crear en esta misma transaccion, la coleccion en memoria queda vieja y no refleja el
        // item recien guardado.
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            Experience experience = session.getExperience();
            BigDecimal unitPrice = experience.getEffectivePrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            CartItemResponseDTO itemDTO = CartItemResponseDTO.builder()
                    .id(item.getId())
                    .experienceSessionId(session.getId())
                    .experienceId(experience.getId())
                    .experienceTitle(experience.getTitle())
                    .startsAt(session.getStartsAt())
                    .endsAt(session.getEndsAt())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(itemTotal)
                    .build();

            itemDTOs.add(itemDTO);
            total = total.add(itemTotal);
        }

        return CartResponseDTO.builder()
                .id(cart.getId())
                .userId(user.getId())
                .items(itemDTOs)
                .total(total)
                .build();
    }

    // Agrega una sesión al carrito (o suma cantidad si ya estaba); valida que haya cupo disponible.
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

    // Cambia la cantidad de un item existente; solo el dueño del carrito.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public CartResponseDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {

        User user = userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::new);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(ResourceNotFoundException::new);

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException();
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

    // Saca un item puntual del carrito; solo el dueño del carrito.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void removeItem(Long userId, Long cartItemId) throws ResourceNotFoundException, ForbiddenException {
        User user = userRepository.findById(userId)
                .orElseThrow(ResourceNotFoundException::new);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(ResourceNotFoundException::new);

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException();
        }

        cartItemRepository.delete(cartItem);
    }

    // Vacía el carrito entero.
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

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (!items.isEmpty()) {
            cartItemRepository.deleteAll(items);
        }
    }
}
