package com.uade.tpo.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.ExperienceSession;

@Repository
public interface ExperienceSessionRepository
        extends JpaRepository<ExperienceSession, Long>, JpaSpecificationExecutor<ExperienceSession> {
    Page<ExperienceSession> findByExperienceId(Long experienceId, Pageable pageable);

    // Tarea automática: marca como terminadas las fechas activas cuya hora de fin ya pasó.
    @Modifying
    @Query("update ExperienceSession s set s.finished = true where s.active = true and s.finished = false and s.endsAt < :now")
    int markFinished(@Param("now") LocalDateTime now);

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
