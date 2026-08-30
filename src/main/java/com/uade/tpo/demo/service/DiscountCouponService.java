package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.DiscountCouponRequestDTO;
import com.uade.tpo.demo.dtos.response.DiscountCouponResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface DiscountCouponService {
    Page<DiscountCouponResponseDTO> getCoupons(Pageable pageable);

    DiscountCouponResponseDTO getCouponById(Long couponId) throws ResourceNotFoundException;

    DiscountCouponResponseDTO createCoupon(DiscountCouponRequestDTO request);

    DiscountCouponResponseDTO updateCoupon(Long couponId, DiscountCouponRequestDTO request) throws ResourceNotFoundException;

    void deleteCoupon(Long couponId) throws ResourceNotFoundException;
}
