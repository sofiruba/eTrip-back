package com.uade.tpo.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByOrder_User_Id(Long userId, Pageable pageable);

    Page<Booking> findByExperienceSession_Experience_Publisher_Id(Long publisherId, Pageable pageable);

    Page<Booking> findByExperienceSession_Experience_Id(Long experienceId, Pageable pageable);
}
