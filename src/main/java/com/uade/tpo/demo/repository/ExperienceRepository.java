package com.uade.tpo.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Experience;

@Repository
public interface ExperienceRepository
        extends JpaRepository<Experience, Long>, JpaSpecificationExecutor<Experience> {
    long countByPublisherIdAndDeletedAtIsNull(Long publisherId);
    // Una experiencia eliminada deja libre su título para volver a usarlo
    boolean existsByPublisherIdAndTitleIgnoreCaseAndDeletedAtIsNull(Long publisherId, String title);
    boolean existsByPublisherIdAndTitleIgnoreCaseAndIdNotAndDeletedAtIsNull(Long publisherId, String title, Long id);
}
