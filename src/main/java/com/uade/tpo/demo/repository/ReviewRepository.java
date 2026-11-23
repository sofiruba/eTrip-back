package com.uade.tpo.demo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByExperienceId(Long experienceId, Pageable pageable);

    // Búsqueda dentro de las reseñas de una experiencia (paginada igual que la lista)
    Page<Review> findByExperienceIdAndCommentContainingIgnoreCase(Long experienceId, String text, Pageable pageable);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    boolean existsByExperienceIdAndUserId(Long experienceId, Long userId);

    // Cantidad y promedio de una experiencia: se devuelven junto con la reseña creada o borrada
    long countByExperienceId(Long experienceId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.experience.id = :experienceId")
    Double averageRatingByExperienceId(@Param("experienceId") Long experienceId);

    // Cuántas reseñas hay de cada puntaje: filas [rating, cantidad]
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.experience.id = :experienceId GROUP BY r.rating")
    List<Object[]> countByRating(@Param("experienceId") Long experienceId);
}
