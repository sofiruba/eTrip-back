package com.uade.tpo.demo.controllers.experiences;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.tpo.demo.dtos.request.ExperienceCategoryRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceCategoryResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ExperienceCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("experience-categories")
@RequiredArgsConstructor
public class ExperienceCategoriesController {

    private final ExperienceCategoryService experienceCategoryService;

    @GetMapping
    public ResponseEntity<Page<ExperienceCategoryResponseDTO>> getCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Si page o size vienen null, usar PageRequest.of(0, Integer.MAX_VALUE), como CategoriesController.
        // 2. Si vienen informados, usar PageRequest.of(page, size).
        // 3. Llamar a experienceCategoryService.getCategories(pageRequest).
        // 4. Retornar ResponseEntity.ok(resultado).
        return null;
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ExperienceCategoryResponseDTO> getCategoryById(@PathVariable Long categoryId)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a experienceCategoryService.getCategoryById(categoryId).
        // 2. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @PostMapping
    public ResponseEntity<ExperienceCategoryResponseDTO> createCategory(
            @RequestBody ExperienceCategoryRequestDTO request) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar payload minimo a nivel controller si la catedra lo pide.
        // 2. Llamar a experienceCategoryService.createCategory(request).
        // 3. Retornar ResponseEntity.created(URI.create("/experience-categories/" + result.getId())).body(result).
        return null;
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ExperienceCategoryResponseDTO> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody ExperienceCategoryRequestDTO request) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a experienceCategoryService.updateCategory(categoryId, request).
        // 2. Retornar ResponseEntity.ok(dto).
        return null;
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Llamar a experienceCategoryService.deleteCategory(categoryId).
        // 2. Retornar ResponseEntity.noContent().build().
        return null;
    }
}
