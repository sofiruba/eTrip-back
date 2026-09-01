package com.uade.tpo.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByOrderUserId(Long userId, Pageable pageable);
}
