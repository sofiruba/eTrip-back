package com.uade.tpo.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByOrder_User_Id(Long userId, Pageable pageable);
    // Mis reservas: las que compré y las que me regalaron
    Page<Booking> findByOrder_User_IdOrRecipient_Id(Long userId, Long recipientId, Pageable pageable);
    java.util.List<Booking> findByRecipient_IdAndExperienceSession_Experience_Id(Long recipientId, Long experienceId);

    Page<Booking> findByExperienceSession_Experience_Publisher_Id(Long publisherId, Pageable pageable);

    Page<Booking> findByExperienceSession_Experience_Id(Long experienceId, Pageable pageable);

    boolean existsByOrder_User_IdAndExperienceSession_Experience_Id(Long userId, Long experienceId);

    java.util.List<Booking> findByOrder_User_IdAndExperienceSession_Experience_Id(Long userId, Long experienceId);

    java.util.List<Booking> findByExperienceSession_Id(Long sessionId);

}
