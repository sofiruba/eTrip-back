package com.uade.tpo.demo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.dtos.response.CartItemResponseDTO;
import com.uade.tpo.demo.entity.Cart;
import com.uade.tpo.demo.entity.CartItem;
import com.uade.tpo.demo.entity.Role;
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
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));
        // Busca el carrito del usuario; si no existe, lo crea vacío.

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();

                    return cartRepository.save(newCart);
                });

        // Las fechas canceladas o que ya empezaron no se pueden comprar: se sacan al abrir el carrito
        cartItemRepository.deleteUnavailable(cart.getId(), LocalDateTime.now());

        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        // Calcula el total del carrito sumando los subtotales de cada item.
        BigDecimal total = BigDecimal.ZERO;

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        // Para cada item del carrito, calcula el subtotal (precio unitario * cantidad) 
        // y lo agrega a la lista de DTOs.
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
        // Devuelve el carrito con los items y el total calculado.

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
    public CartResponseDTO addItem(CartItemRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));

        // Los ADMIN son para moderación, no compran experiencias.
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Los administradores no pueden comprar experiencias");
        }

        ExperienceSession session = experienceSessionRepository
                .findById(request.getExperienceSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("La sesión solicitada no existe"));

        if (!session.isActive()
                || session.getExperience() == null
                || !session.getExperience().isActive()) {
            throw new BadRequestException("La sesión seleccionada está inactiva y no se puede agregar al carrito");
        }
        if (session.getStartsAt() == null || session.getStartsAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("La sesión seleccionada ya comenzó o ya pasó");
        }

        // Un usuario no puede comprar (agregar al carrito) su propia experiencia.
        Experience experience = session.getExperience();
        if (experience != null && experience.getPublisher() != null
                && experience.getPublisher().getId().equals(user.getId())) {
            throw new ForbiddenException("No podes comprar tu propia experiencia");
        }

         // Valida que la cantidad solicitada sea positiva.
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("La cantidad de personas tiene que ser al menos 1");
        }

        // Busca el carrito del usuario; si no existe, lo crea vacío.
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();

                    return cartRepository.save(newCart);
                });

        // Busca si ya existe un item en el carrito para la misma sesión de experiencia.
        CartItem cartItem = cartItemRepository
                .findByCartIdAndExperienceSessionId(
                        cart.getId(),
                        session.getId()
                )
                .orElse(null);

        int newQuantity = request.getQuantity();

        // Si ya existía un item para esa sesión, suma la cantidad solicitada a la existente.
        if (cartItem != null) {
            newQuantity += cartItem.getQuantity();
        }

        // Valida que la cantidad total no supere los cupos disponibles de la sesión.
        if (session.getAvailableSeats() == null
                || newQuantity > session.getAvailableSeats()) {

            throw new BadRequestException(seatsMessage(session));
        }

        // Si no existía, crea un nuevo item; si ya existía, actualiza la cantidad.
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
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ese ítem ya no está en el carrito"));

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Ese ítem no es de tu carrito");
        }

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("La cantidad de personas tiene que ser al menos 1");
        }

        // Valida que la cantidad solicitada no supere los cupos disponibles de la sesión.
        ExperienceSession session = cartItem.getExperienceSession();

        if (!session.isActive()) {
            throw new BadRequestException("La sesión seleccionada está inactiva y no se puede actualizar");
        }
        if (session.getAvailableSeats() == null
                || quantity > session.getAvailableSeats()) {

            throw new BadRequestException(seatsMessage(session));
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
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ese ítem ya no está en el carrito"));

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Ese ítem no es de tu carrito");
        }

        cartItemRepository.delete(cartItem);
    }

    // Vacía el carrito entero.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void clearCart(Long userId) throws ResourceNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));

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

    // "Solo quedan 3 lugares" / "Se agotaron los lugares de esta fecha"
    private String seatsMessage(ExperienceSession session) {
        int seats = session.getAvailableSeats() == null ? 0 : session.getAvailableSeats();
        if (seats == 0) {
            return "Se agotaron los lugares de esta fecha";
        }
        return "Solo quedan " + seats + (seats == 1 ? " lugar" : " lugares") + " en esta fecha";
    }
}
