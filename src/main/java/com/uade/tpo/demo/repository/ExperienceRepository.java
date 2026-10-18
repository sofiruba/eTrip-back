package com.uade.tpo.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Experience;

@Repository
public interface ExperienceRepository
        extends JpaRepository<Experience, Long>, JpaSpecificationExecutor<Experience> {
    Page<Experience> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Experience> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Experience> findByCategoryIdAndTitleContainingIgnoreCase(Long categoryId, String title, Pageable pageable);

    long countByPublisherId(Long publisherId);

    // Tarea automática: pausa las experiencias publicadas que no tienen ninguna fecha futura disponible.
    @Modifying
    @Query("update Experience e set e.active = false, e.autoPaused = true where e.active = true and not exists "
            + "(select s.id from ExperienceSession s where s.experience = e "
            + "and s.active = true and s.finished = false and s.startsAt > :now)")
    int pauseWithoutUpcomingSessions(@Param("now") LocalDateTime now);

    boolean existsByPublisherIdAndTitleIgnoreCase(Long publisherId, String title);

    boolean existsByPublisherIdAndTitleIgnoreCaseAndIdNot(Long publisherId, String title, Long id);
}
