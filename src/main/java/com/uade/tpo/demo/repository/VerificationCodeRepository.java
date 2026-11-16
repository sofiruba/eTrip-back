package com.uade.tpo.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.CodePurpose;
import com.uade.tpo.demo.entity.VerificationCode;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByUserIdAndPurpose(Long userId, CodePurpose purpose);

    void deleteByUserIdAndPurpose(Long userId, CodePurpose purpose);
}
