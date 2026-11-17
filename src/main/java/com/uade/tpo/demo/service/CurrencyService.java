package com.uade.tpo.demo.service;

import java.math.BigDecimal;
import java.util.List;

import com.uade.tpo.demo.dtos.response.CurrencyResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface CurrencyService {
    List<CurrencyResponseDTO> getCurrencies();

    // Solo ADMIN (ver SecurityConfig): cambia la cotización de una moneda
    CurrencyResponseDTO updateRate(String code, BigDecimal arsPerUnit) throws ResourceNotFoundException, BadRequestException;
}
