package com.uade.tpo.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.ExperienceCategory;

@Repository
public interface ExperienceCategoryRepository extends JpaRepository<ExperienceCategory, Long> {
    Optional<ExperienceCategory> findByName(String name);
}
