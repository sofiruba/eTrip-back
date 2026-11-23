package com.uade.tpo.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Experience;

@Repository
public interface ExperienceRepository
        extends JpaRepository<Experience, Long>, JpaSpecificationExecutor<Experience> {
    long countByPublisherIdAndDeletedAtIsNull(Long publisherId);

    List<Experience> findByPublisherIdAndDeletedAtIsNullOrderByTitleAsc(Long publisherId);
    // Una experiencia eliminada deja libre su título para volver a usarlo
    boolean existsByPublisherIdAndTitleIgnoreCaseAndDeletedAtIsNull(Long publisherId, String title);
    boolean existsByPublisherIdAndTitleIgnoreCaseAndIdNotAndDeletedAtIsNull(Long publisherId, String title, Long id);

    // Calculadora de "Ser anfitrión": precio promedio y cantidad de experiencias publicadas de una categoría
    @Query("SELECT AVG(e.price) FROM Experience e WHERE e.category.id = :categoryId AND e.active = true AND e.deletedAt IS NULL")
    Double averagePriceByCategoryId(@Param("categoryId") Long categoryId);

    long countByCategoryIdAndActiveTrueAndDeletedAtIsNull(Long categoryId);
}
