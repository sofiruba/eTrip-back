package com.uade.tpo.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByExperienceId(Long experienceId, Pageable pageable);

    boolean existsByExperienceIdAndUserId(Long experienceId, Long userId);
}
