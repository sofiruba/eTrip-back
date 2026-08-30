package com.uade.tpo.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.ExperienceSession;

@Repository
public interface ExperienceSessionRepository extends JpaRepository<ExperienceSession, Long> {
}
