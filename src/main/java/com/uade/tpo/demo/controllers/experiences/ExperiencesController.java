package com.uade.tpo.demo.controllers.experiences;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("experiences")
@RequiredArgsConstructor
public class ExperiencesController {

    private final ExperienceService experienceService;

    @GetMapping
    public ResponseEntity<Page<ExperienceResponseDTO>> getExperiences(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Armar PageRequest con page y size, usando defaults si vienen null.
        // 2. Llamar a experienceService.getExperiences(pageRequest).
        // 3. Retornar ResponseEntity.ok(resultado).
        return null;
    }

    @GetMapping("/{experienceId}")
    public ResponseEntity<ExperienceResponseDTO> getExperienceById(@PathVariable Long experienceId)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a experienceService.getExperienceById(experienceId).
        // 2. El DTO debe traer imageBase64 si hay imagen cargada.
        // 3. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperienceResponseDTO> createExperience(
            @RequestPart("experience") ExperienceRequestDTO request,
            @RequestParam("image") MultipartFile image) throws ResourceNotFoundException, IOException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Recibir multipart/form-data con la parte "experience" y el archivo "image".
        // 2. Delegar en experienceService.createExperience(request, image).
        // 3. Retornar ResponseEntity.created(URI.create("/experiences/" + result.getId())).body(result).
        return null;
    }

    @PutMapping(value = "/{experienceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperienceResponseDTO> updateExperience(
            @PathVariable Long experienceId,
            @RequestPart("experience") ExperienceRequestDTO request,
            @RequestParam(value = "image", required = false) MultipartFile image)
            throws ResourceNotFoundException, IOException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Recibir los nuevos datos de la experiencia y una imagen opcional.
        // 2. Delegar en experienceService.updateExperience(experienceId, request, image).
        // 3. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @DeleteMapping("/{experienceId}")
    public ResponseEntity<Void> deleteExperience(@PathVariable Long experienceId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Delegar en experienceService.deleteExperience(experienceId).
        // 2. Retornar ResponseEntity.noContent().build().
        return null;
    }
}
