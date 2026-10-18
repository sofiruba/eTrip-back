package com.uade.tpo.demo.controllers.orders;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.dtos.response.OrderStatsDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    // Mis órdenes (ADMIN ve todas).
    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User user) {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(orderService.getOrders(user, pageRequest));
    }

    // Ventas, reembolsos y comisión de PLAN (10%); solo ADMIN.
    @GetMapping("/stats")
    public ResponseEntity<OrderStatsDTO> getStats(@AuthenticationPrincipal User user) throws ForbiddenException {
        return ResponseEntity.ok(orderService.getStats(user));
    }

    // Cancela la orden entera (reembolsa sus reservas que todavía no empezaron); solo ADMIN.
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user)
            throws ResourceNotFoundException, ForbiddenException, BadRequestException {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user));
    }

    // Una orden puntual; solo la ve el dueño o un ADMIN.
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user) throws ResourceNotFoundException, ForbiddenException {
        return ResponseEntity.ok(orderService.getOrderById(orderId, user));
    }

    // Confirma el carrito del usuario logueado: valida cupos, aplica cupón si viene, genera los
    // vouchers (Booking) y vacía el carrito, todo en una transacción.
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @RequestBody(required = false) OrderRequestDTO request,
            @AuthenticationPrincipal User user) throws ResourceNotFoundException, BadRequestException {
        OrderResponseDTO result = orderService.createOrder(user, request);
        return ResponseEntity.created(URI.create("/orders/" + result.getId())).body(result);
    }
}
