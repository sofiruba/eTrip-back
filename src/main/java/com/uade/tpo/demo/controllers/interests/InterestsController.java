package com.uade.tpo.demo.controllers.interests;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.InterestRequestDTO;
import com.uade.tpo.demo.dtos.response.InterestResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.InterestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("interests")
@RequiredArgsConstructor
public class InterestsController {

    private final InterestService interestService;

    // Todos los intereses (público), con su categoría y cuántos planes disponibles tiene cada uno.
    @GetMapping
    public ResponseEntity<List<InterestResponseDTO>> getInterests() {
        return ResponseEntity.ok(interestService.getInterests());
    }

    // Crea un interés (solo ADMIN, ver SecurityConfig).
    @PostMapping
    public ResponseEntity<InterestResponseDTO> createInterest(@RequestBody InterestRequestDTO request)
            throws BadRequestException, ResourceNotFoundException {
        InterestResponseDTO result = interestService.createInterest(request);
        return ResponseEntity.created(URI.create("/interests/" + result.getId())).body(result);
    }

    // Cambia el nombre o la categoría (solo ADMIN).
    @PutMapping("/{interestId}")
    public ResponseEntity<InterestResponseDTO> updateInterest(@PathVariable Long interestId,
            @RequestBody InterestRequestDTO request) throws BadRequestException, ResourceNotFoundException {
        return ResponseEntity.ok(interestService.updateInterest(interestId, request));
    }

    // Borra un interés y se lo saca a experiencias y usuarios (solo ADMIN).
    @DeleteMapping("/{interestId}")
    public ResponseEntity<Void> deleteInterest(@PathVariable Long interestId) throws ResourceNotFoundException {
        interestService.deleteInterest(interestId);
        return ResponseEntity.noContent().build();
    }
}
