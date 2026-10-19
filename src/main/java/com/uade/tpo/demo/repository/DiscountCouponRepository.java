package com.uade.tpo.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.uade.tpo.demo.entity.DiscountCoupon;

@Repository
public interface DiscountCouponRepository extends JpaRepository<DiscountCoupon, Long> {
    Optional<DiscountCoupon> findByCode(String code);

    // Al confirmar una compra: bloquea el cupón hasta que termine la transacción, así dos compras simultáneas
    // del mismo usuario no pueden usarlo las dos (la segunda espera y ve la orden de la primera).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from DiscountCoupon c where c.code = :code")
    Optional<DiscountCoupon> findByCodeForUpdate(@Param("code") String code);
}
