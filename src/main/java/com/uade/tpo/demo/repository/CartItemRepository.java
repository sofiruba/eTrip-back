package com.uade.tpo.demo.repository;

import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndExperienceSessionId(
        Long cartId,
        Long experienceSessionId
    );

    List<CartItem> findByCartId(Long cartId);

    boolean existsByExperienceSessionId(Long experienceSessionId);

    // Al eliminar una experiencia: saca sus fechas de todos los carritos
    void deleteByExperienceSessionId(Long experienceSessionId);

    // Tarea automática: saca de los carritos las fechas canceladas, terminadas o que ya empezaron.
    @Modifying
    @Query("delete from CartItem i where i.experienceSession.id in "
            + "(select s.id from ExperienceSession s where s.active = false or s.finished = true or s.startsAt < :now)")
    int deleteUnavailable(@Param("now") LocalDateTime now);
}
