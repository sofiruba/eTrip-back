package com.uade.tpo.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.ExperienceImage;

@Repository
public interface ExperienceImageRepository extends JpaRepository<ExperienceImage, Long> {
    List<ExperienceImage> findByExperienceIdOrderByPositionAsc(Long experienceId);
}
