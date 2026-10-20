package com.uade.tpo.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.ExperienceSession;

@Repository
public interface ExperienceSessionRepository
        extends JpaRepository<ExperienceSession, Long>, JpaSpecificationExecutor<ExperienceSession> {
    Page<ExperienceSession> findByExperienceId(Long experienceId, Pageable pageable);

    // Historial completo (pasadas y canceladas) para quien gestiona las fechas.
    List<ExperienceSession> findAllByOrderByStartsAtAsc();

    List<ExperienceSession> findByExperience_Publisher_IdOrderByStartsAtAsc(Long publisherId);

    boolean existsByExperienceIdAndActiveTrueAndStartsAtLessThanAndEndsAtGreaterThan(
            Long experienceId,
            LocalDateTime endsAt,
            LocalDateTime startsAt);

    boolean existsByExperienceIdAndActiveTrueAndIdNotAndStartsAtLessThanAndEndsAtGreaterThan(
            Long experienceId,
            Long sessionId,
            LocalDateTime endsAt,
            LocalDateTime startsAt);
}
