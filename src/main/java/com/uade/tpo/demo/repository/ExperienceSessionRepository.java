package com.uade.tpo.demo.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.ExperienceSession;

@Repository
public interface ExperienceSessionRepository extends JpaRepository<ExperienceSession, Long> {
    Page<ExperienceSession> findByExperienceId(Long experienceId, Pageable pageable);

    boolean existsByExperienceIdAndStartsAtLessThanAndEndsAtGreaterThan(
            Long experienceId,
            LocalDateTime endsAt,
            LocalDateTime startsAt);

    boolean existsByExperienceIdAndIdNotAndStartsAtLessThanAndEndsAtGreaterThan(
            Long experienceId,
            Long sessionId,
            LocalDateTime endsAt,
            LocalDateTime startsAt);
}
