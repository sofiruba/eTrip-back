package com.uade.tpo.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndExperienceId(Long userId, Long experienceId);

    void deleteByUserIdAndExperienceId(Long userId, Long experienceId);

    // Al eliminar una experiencia
    void deleteByExperienceId(Long experienceId);

    // "N personas la guardaron"
    long countByExperienceId(Long experienceId);
}
