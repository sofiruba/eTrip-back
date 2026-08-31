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

    @GetMapping
    public ResponseEntity<Page<ExperienceCategoryResponseDTO>> getCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page == null || size == null)
            return ResponseEntity.ok(experienceCategoryService.getCategories(PageRequest.of(0, Integer.MAX_VALUE)));
        return ResponseEntity.ok(experienceCategoryService.getCategories(PageRequest.of(page, size)));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ExperienceCategoryResponseDTO> getCategoryById(@PathVariable Long categoryId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceCategoryService.getCategoryById(categoryId));
    }

    @PostMapping
    public ResponseEntity<ExperienceCategoryResponseDTO> createCategory(
            @RequestBody ExperienceCategoryRequestDTO request)
            throws BadRequestException, CategoryDuplicateException {
        ExperienceCategoryResponseDTO result = experienceCategoryService.createCategory(request);
        return ResponseEntity.created(URI.create("/experience-categories/" + result.getId())).body(result);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ExperienceCategoryResponseDTO> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, CategoryDuplicateException {
        return ResponseEntity.ok(experienceCategoryService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId)
            throws ResourceNotFoundException, BadRequestException {
        experienceCategoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
