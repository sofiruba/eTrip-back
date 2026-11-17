package com.uade.tpo.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.response.CurrencyResponseDTO;
import com.uade.tpo.demo.entity.Currency;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.CurrencyRepository;
import com.uade.tpo.demo.service.CurrencyService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    /** La moneda en la que se publica y se cobra todo: su cotización es siempre 1. */
    static final String BASE = "ARS";

    private final CurrencyRepository currencyRepository;

    // Todas las monedas: primero los pesos y después por nombre
    @Override
    @Transactional(readOnly = true)
    public List<CurrencyResponseDTO> getCurrencies() {
        return currencyRepository.findAll().stream()
                .sorted(Comparator.comparing((Currency currency) -> !BASE.equals(currency.getCode()))
                        .thenComparing(Currency::getName))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public CurrencyResponseDTO updateRate(String code, BigDecimal arsPerUnit)
            throws ResourceNotFoundException, BadRequestException {
        Currency currency = currencyRepository.findById(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la moneda " + code.toUpperCase()));
        if (BASE.equals(currency.getCode())) {
            throw new BadRequestException("El peso argentino es la moneda de PLAN: su cotización es siempre 1");
        }
        if (arsPerUnit == null || arsPerUnit.signum() <= 0) {
            throw new BadRequestException("La cotización tiene que ser mayor a 0");
        }
        currency.setArsPerUnit(arsPerUnit);
        currency.setUpdatedAt(LocalDateTime.now());
        return toResponse(currencyRepository.save(currency));
    }

    private CurrencyResponseDTO toResponse(Currency currency) {
        return CurrencyResponseDTO.builder()
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .arsPerUnit(currency.getArsPerUnit())
                .updatedAt(currency.getUpdatedAt())
                .build();
    }
}
