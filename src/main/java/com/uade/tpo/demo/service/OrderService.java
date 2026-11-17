package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.dtos.response.OrderStatsDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface OrderService {
    // Mis órdenes; ADMIN ve todas.
    Page<OrderResponseDTO> getOrders(User user, Pageable pageable);

    // Una orden puntual; solo el dueño o un ADMIN.
    OrderResponseDTO getOrderById(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException;

    // Confirma el carrito del usuario y genera la orden con sus vouchers.
    OrderResponseDTO createOrder(User user, OrderRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException;

    // Estadísticas de ventas y comisión de PLAN; solo ADMIN.
    OrderStatsDTO getStats(User user) throws ForbiddenException;

    // Cancela la orden: reembolsa todas sus reservas que todavía no empezaron; solo ADMIN.
    OrderResponseDTO cancelOrder(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException;

    // Regalo pendiente: quien pagó corrige el email (se vuelve a mandar la invitación)
    OrderResponseDTO updateGiftEmail(Long orderId, String recipientEmail, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException;

    // Le entrega a este usuario los regalos pendientes a su email (cuando lo verifica o cuando se reactiva su cuenta)
    void claimPendingGifts(User user);

    // Regalo pendiente: quien pagó se lo queda (pasan a ser reservas comunes suyas)
    OrderResponseDTO cancelGift(Long orderId, User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException;
}
