package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.DiscountCouponRequestDTO;
import com.uade.tpo.demo.dtos.response.CouponValidationDTO;
import com.uade.tpo.demo.dtos.response.DiscountCouponResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface DiscountCouponService {
    // Lista todos los cupones.
    Page<DiscountCouponResponseDTO> getCoupons(Pageable pageable);

    // Un cupón puntual por id.
    DiscountCouponResponseDTO getCouponById(Long couponId) throws ResourceNotFoundException;

    // Chequea si un código es aplicable hoy, sin gastarlo.
    CouponValidationDTO validateCoupon(String code);

    // Crea un cupón nuevo.
    DiscountCouponResponseDTO createCoupon(DiscountCouponRequestDTO request) throws BadRequestException;

    // Actualiza un cupón existente.
    DiscountCouponResponseDTO updateCoupon(Long couponId, DiscountCouponRequestDTO request)
            throws ResourceNotFoundException, BadRequestException;

    // Borra el cupón, o lo desactiva si ya fue usado en una orden.
    void deleteCoupon(Long couponId) throws ResourceNotFoundException;
}
