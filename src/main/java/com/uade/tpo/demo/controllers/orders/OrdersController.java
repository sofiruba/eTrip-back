package com.uade.tpo.demo.controllers.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.OrderRequestDTO;
import com.uade.tpo.demo.dtos.response.OrderResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado tenga rol ADMIN para ver todas las ordenes.
        // 2. Armar PageRequest con defaults cuando page o size sean null.
        // 3. Llamar a orderService.getOrders(pageRequest).
        // 4. Retornar ResponseEntity.ok(resultado).
        return null;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long orderId)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a orderService.getOrderById(orderId).
        // 2. Validar que la orden pertenezca al usuario autenticado o que sea ADMIN.
        // 3. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO request)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado coincida con request.getUserId().
        // 2. Delegar en orderService.createOrder(request).
        // 3. Retornar ResponseEntity.created(URI.create("/orders/" + result.getId())).body(result).
        return null;
    }
}
