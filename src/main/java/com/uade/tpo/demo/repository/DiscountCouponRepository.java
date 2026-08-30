package com.uade.tpo.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.DiscountCoupon;

@Repository
public interface DiscountCouponRepository extends JpaRepository<DiscountCoupon, Long> {
    Optional<DiscountCoupon> findByCode(String code);
}
