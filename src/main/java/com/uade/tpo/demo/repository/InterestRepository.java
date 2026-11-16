package com.uade.tpo.demo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Interest;

@Repository
public interface InterestRepository extends JpaRepository<Interest, Long> {
    Optional<Interest> findByName(String name);

    List<Interest> findByNameIn(Collection<String> names);

    List<Interest> findAllByOrderByCategoryNameAscNameAsc();

    boolean existsByCategoryId(Long categoryId);
}
