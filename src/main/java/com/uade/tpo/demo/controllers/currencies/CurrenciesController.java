package com.uade.tpo.demo.controllers.currencies;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.CurrencyRateRequestDTO;
import com.uade.tpo.demo.dtos.response.CurrencyResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.CurrencyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("currencies")
@RequiredArgsConstructor
public class CurrenciesController {

    private final CurrencyService currencyService;

    // Monedas para ver los precios, con su cotización en pesos (público).
    @GetMapping
    public ResponseEntity<List<CurrencyResponseDTO>> getCurrencies() {
        return ResponseEntity.ok(currencyService.getCurrencies());
    }

    // Cambia la cotización de una moneda (solo ADMIN).
    @PutMapping("/{code}")
    public ResponseEntity<CurrencyResponseDTO> updateRate(@PathVariable String code,
            @RequestBody CurrencyRateRequestDTO request) throws ResourceNotFoundException, BadRequestException {
        return ResponseEntity.ok(currencyService.updateRate(code, request.getArsPerUnit()));
    }
}
