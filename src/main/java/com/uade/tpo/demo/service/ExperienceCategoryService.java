package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.ExperienceCategoryRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceCategoryResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceCategoryService {
    Page<ExperienceCategoryResponseDTO> getCategories(Pageable pageable);

    ExperienceCategoryResponseDTO getCategoryById(Long categoryId) throws ResourceNotFoundException;

    ExperienceCategoryResponseDTO createCategory(ExperienceCategoryRequestDTO request);

    ExperienceCategoryResponseDTO updateCategory(Long categoryId, ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException;

    void deleteCategory(Long categoryId) throws ResourceNotFoundException;
}
