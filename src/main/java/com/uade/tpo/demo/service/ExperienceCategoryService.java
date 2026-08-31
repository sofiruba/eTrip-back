package com.uade.tpo.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uade.tpo.demo.dtos.request.ExperienceCategoryRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceCategoryResponseDTO;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.CategoryDuplicateException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface ExperienceCategoryService {
    Page<ExperienceCategoryResponseDTO> getCategories(Pageable pageable);

    ExperienceCategoryResponseDTO getCategoryById(Long categoryId) throws ResourceNotFoundException;

    ExperienceCategoryResponseDTO createCategory(ExperienceCategoryRequestDTO request)
            throws BadRequestException, CategoryDuplicateException;

    ExperienceCategoryResponseDTO updateCategory(Long categoryId, ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, CategoryDuplicateException;

    void deleteCategory(Long categoryId) throws ResourceNotFoundException, BadRequestException;
}
