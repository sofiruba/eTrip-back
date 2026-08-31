package com.uade.tpo.demo.service.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ExperienceCategoryRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceCategoryResponseDTO;
import com.uade.tpo.demo.entity.ExperienceCategory;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.CategoryDuplicateException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceCategoryRepository;
import com.uade.tpo.demo.service.ExperienceCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceCategoryServiceImpl implements ExperienceCategoryService {

    private final ExperienceCategoryRepository experienceCategoryRepository;

    @Override
    public Page<ExperienceCategoryResponseDTO> getCategories(Pageable pageable) {
        return experienceCategoryRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public ExperienceCategoryResponseDTO getCategoryById(Long categoryId) throws ResourceNotFoundException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(ResourceNotFoundException::new);
        return toResponse(category);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO createCategory(ExperienceCategoryRequestDTO request)
            throws BadRequestException, CategoryDuplicateException {
        String name = requireName(request.getName());
        if (experienceCategoryRepository.findByName(name).isPresent()) {
            throw new CategoryDuplicateException();
        }

        ExperienceCategory category = ExperienceCategory.builder()
                .name(name)
                .description(trimToNull(request.getDescription()))
                .build();

        return toResponse(experienceCategoryRepository.save(category));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO updateCategory(Long categoryId, ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, CategoryDuplicateException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(ResourceNotFoundException::new);

        String name = requireName(request.getName());
        Optional<ExperienceCategory> sameName = experienceCategoryRepository.findByName(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(categoryId)) {
            throw new CategoryDuplicateException();
        }

        category.setName(name);
        category.setDescription(trimToNull(request.getDescription()));

        return toResponse(experienceCategoryRepository.save(category));
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteCategory(Long categoryId) throws ResourceNotFoundException, BadRequestException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(ResourceNotFoundException::new);

        if (category.getExperiences() != null && !category.getExperiences().isEmpty()) {
            throw new BadRequestException();
        }

        experienceCategoryRepository.delete(category);
    }

    private String requireName(String name) throws BadRequestException {
        if (name == null || name.isBlank()) {
            throw new BadRequestException();
        }
        return name.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ExperienceCategoryResponseDTO toResponse(ExperienceCategory category) {
        return ExperienceCategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
