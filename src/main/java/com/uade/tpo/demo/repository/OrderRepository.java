package com.uade.tpo.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    boolean existsByDiscountCouponId(Long discountCouponId);

    long countByDiscountCouponId(Long discountCouponId);

    // ¿Esta persona ya usó el cupón? Una orden que se canceló entera no cuenta: el cupón se puede volver a usar
    @Query("select count(o) > 0 from Order o where o.user.id = :userId and o.discountCoupon.id = :couponId "
            + "and exists (select b from Booking b where b.order = o and b.refunded = false)")
    boolean hasUsedCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);
}
