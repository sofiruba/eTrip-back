package com.uade.tpo.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.WaitlistEntry;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<WaitlistEntry> findByUserId(Long userId);

    boolean existsByUserIdAndSessionId(Long userId, Long sessionId);

    void deleteByUserIdAndSessionId(Long userId, Long sessionId);

    void deleteBySessionId(Long sessionId);

    long countBySessionId(Long sessionId);
}
