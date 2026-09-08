package com.uade.tpo.demo.controllers.coupons;

import java.net.URI;

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
import com.uade.tpo.demo.dtos.response.CouponValidationDTO;
import com.uade.tpo.demo.dtos.response.DiscountCouponResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.DiscountCouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("discount-coupons")
@RequiredArgsConstructor
public class DiscountCouponsController {

    private final DiscountCouponService discountCouponService;

    // Lista todos los cupones (activos e inactivos).
    @GetMapping
    public ResponseEntity<Page<DiscountCouponResponseDTO>> getCoupons(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        PageRequest pageRequest = page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(discountCouponService.getCoupons(pageRequest));
    }

    // Chequea si un código de cupón es aplicable hoy. Siempre responde 200 (valid true/false + motivo).
    @GetMapping("/validate")
    public ResponseEntity<CouponValidationDTO> validateCoupon(@RequestParam String code) {
        return ResponseEntity.ok(discountCouponService.validateCoupon(code));
    }

    // Un cupón puntual por id.
    @GetMapping("/{couponId}")
    public ResponseEntity<DiscountCouponResponseDTO> getCouponById(@PathVariable Long couponId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(discountCouponService.getCouponById(couponId));
    }

    // Crea un cupón nuevo (código único, porcentaje y vigencia).
    @PostMapping
    public ResponseEntity<DiscountCouponResponseDTO> createCoupon(@RequestBody DiscountCouponRequestDTO request)
            throws BadRequestException {
        DiscountCouponResponseDTO result = discountCouponService.createCoupon(request);
        return ResponseEntity.created(URI.create("/discount-coupons/" + result.getId())).body(result);
    }

    // Actualiza los datos de un cupón existente.
    @PutMapping("/{couponId}")
    public ResponseEntity<DiscountCouponResponseDTO> updateCoupon(
            @PathVariable Long couponId,
            @RequestBody DiscountCouponRequestDTO request) throws ResourceNotFoundException, BadRequestException {
        return ResponseEntity.ok(discountCouponService.updateCoupon(couponId, request));
    }

    // Borra el cupón si nunca se usó; si ya se usó en una orden, lo desactiva en vez de borrarlo.
    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) throws ResourceNotFoundException {
        discountCouponService.deleteCoupon(couponId);
        return ResponseEntity.noContent().build();
    }
}
