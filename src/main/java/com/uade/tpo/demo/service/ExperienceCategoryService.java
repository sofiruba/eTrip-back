package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.ExperienceCategoryRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceCategoryResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.CategoryDuplicateException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceCategoryService {
    // Lista todas las categorías.
    Page<ExperienceCategoryResponseDTO> getCategories(Pageable pageable);

    // Una categoría puntual por id.
    ExperienceCategoryResponseDTO getCategoryById(Long categoryId) throws ResourceNotFoundException;

    // Crea una categoría nueva (nombre único).
    ExperienceCategoryResponseDTO createCategory(ExperienceCategoryRequestDTO request)
            throws BadRequestException, CategoryDuplicateException;

    // Actualiza una categoría existente.
    ExperienceCategoryResponseDTO updateCategory(Long categoryId, ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, CategoryDuplicateException;

    // Borra una categoría; falla si tiene experiencias asociadas.
    void deleteCategory(Long categoryId) throws ResourceNotFoundException, BadRequestException;
}
