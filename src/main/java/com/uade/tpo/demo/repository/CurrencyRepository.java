package com.uade.tpo.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.tpo.demo.entity.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
}
