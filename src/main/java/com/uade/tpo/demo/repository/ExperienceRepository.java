package com.uade.tpo.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Experience;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    Page<Experience> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Experience> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Experience> findByCategoryIdAndTitleContainingIgnoreCase(Long categoryId, String title, Pageable pageable);
}
