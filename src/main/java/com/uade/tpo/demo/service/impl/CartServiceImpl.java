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

        // OJO: no usamos cart.getItems() aca. Si el Cart se acaba de crear/tocar en esta misma
        // transaccion (p. ej. addItem creando el carrito y guardando el primer CartItem), la
        // colección en memoria de esa instancia de Cart queda vieja/vacía (Hibernate no la
        // sincroniza sola) y el primer item agregado no aparecía en la respuesta aunque sí
        // quedaba guardado en la base. Con una query directa por cartId siempre se lee lo que
        // realmente hay en la base.
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        for (CartItem item : items) {
            ExperienceSession session = item.getExperienceSession();
            Experience experience = session.getExperience();
            // effectivePrice ya contempla el descuento individual del producto (si tiene),
            // para que el total del carrito coincida con lo que despues cobra el checkout.
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

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void removeItem(Long userId, Long cartItemId) throws ResourceNotFoundException, ForbiddenException {
          User user = userRepository.findById(userId)
             .orElseThrow(ResourceNotFoundException::new);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
             .orElseThrow(ResourceNotFoundException::new);

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            // Antes tiraba IllegalArgumentException (sin @ResponseStatus -> 500). El resto del
            // proyecto usa ForbiddenException para "esto no es tuyo" -> 403.
            throw new ForbiddenException();
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

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (!items.isEmpty()) {
            cartItemRepository.deleteAll(items);
        }
    }
}
