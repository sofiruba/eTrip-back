package com.uade.tpo.demo.controllers.experiences;

import java.net.URI;

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
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.CategoryDuplicateException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ExperienceCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("experience-categories")
@RequiredArgsConstructor
public class ExperienceCategoriesController {

    private final ExperienceCategoryService experienceCategoryService;

    // Lista todas las categorías; sin page/size trae todo.
    @GetMapping
    public ResponseEntity<Page<ExperienceCategoryResponseDTO>> getCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page == null || size == null)
            return ResponseEntity.ok(experienceCategoryService.getCategories(PageRequest.of(0, Integer.MAX_VALUE)));
        return ResponseEntity.ok(experienceCategoryService.getCategories(PageRequest.of(page, size)));
    }

    // Una categoría puntual por id.
    @GetMapping("/{categoryId}")
    public ResponseEntity<ExperienceCategoryResponseDTO> getCategoryById(@PathVariable Long categoryId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceCategoryService.getCategoryById(categoryId));
    }

    // Crea una categoría nueva (nombre único).
    @PostMapping
    public ResponseEntity<ExperienceCategoryResponseDTO> createCategory(
            @RequestBody ExperienceCategoryRequestDTO request)
            throws BadRequestException, CategoryDuplicateException {
        ExperienceCategoryResponseDTO result = experienceCategoryService.createCategory(request);
        return ResponseEntity.created(URI.create("/experience-categories/" + result.getId())).body(result);
    }

    // Actualiza nombre/descripción de una categoría existente.
    @PutMapping("/{categoryId}")
    public ResponseEntity<ExperienceCategoryResponseDTO> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, CategoryDuplicateException {
        return ResponseEntity.ok(experienceCategoryService.updateCategory(categoryId, request));
    }

    // Borra una categoría; falla si todavía tiene experiencias asociadas.
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId)
            throws ResourceNotFoundException, BadRequestException {
        experienceCategoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
