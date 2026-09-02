package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.DiscountCouponRequestDTO;
import com.uade.tpo.demo.dtos.response.CouponValidationDTO;
import com.uade.tpo.demo.dtos.response.DiscountCouponResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface DiscountCouponService {
    Page<DiscountCouponResponseDTO> getCoupons(Pageable pageable);

    DiscountCouponResponseDTO getCouponById(Long couponId) throws ResourceNotFoundException;

    CouponValidationDTO validateCoupon(String code);

    DiscountCouponResponseDTO createCoupon(DiscountCouponRequestDTO request) throws BadRequestException;

    DiscountCouponResponseDTO updateCoupon(Long couponId, DiscountCouponRequestDTO request)
            throws ResourceNotFoundException, BadRequestException;

    void deleteCoupon(Long couponId) throws ResourceNotFoundException;
}
