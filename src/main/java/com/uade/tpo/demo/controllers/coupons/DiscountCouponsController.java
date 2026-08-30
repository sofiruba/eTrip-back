package com.uade.tpo.demo.controllers.coupons;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.DiscountCouponRequestDTO;
import com.uade.tpo.demo.dtos.response.DiscountCouponResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.DiscountCouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("discount-coupons")
@RequiredArgsConstructor
public class DiscountCouponsController {

    private final DiscountCouponService discountCouponService;

    @GetMapping
    public ResponseEntity<Page<DiscountCouponResponseDTO>> getCoupons(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Armar PageRequest con defaults cuando page o size sean null.
        // 2. Llamar a discountCouponService.getCoupons(pageRequest).
        // 3. Retornar ResponseEntity.ok(resultado).
        return null;
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<DiscountCouponResponseDTO> getCouponById(@PathVariable Long couponId)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a discountCouponService.getCouponById(couponId).
        // 2. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @PostMapping
    public ResponseEntity<DiscountCouponResponseDTO> createCoupon(@RequestBody DiscountCouponRequestDTO request) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado tenga rol ADMIN.
        // 2. Delegar en discountCouponService.createCoupon(request).
        // 3. Retornar ResponseEntity.created(URI.create("/discount-coupons/" + result.getId())).body(result).
        return null;
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<DiscountCouponResponseDTO> updateCoupon(
            @PathVariable Long couponId,
            @RequestBody DiscountCouponRequestDTO request) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado tenga rol ADMIN.
        // 2. Delegar en discountCouponService.updateCoupon(couponId, request).
        // 3. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que el usuario autenticado tenga rol ADMIN.
        // 2. Delegar en discountCouponService.deleteCoupon(couponId).
        // 3. Retornar ResponseEntity.noContent().build().
        return null;
    }
}
