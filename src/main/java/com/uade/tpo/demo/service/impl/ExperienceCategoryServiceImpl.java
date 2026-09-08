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

    // Lista todas las categorías.
    @Override
    public Page<ExperienceCategoryResponseDTO> getCategories(Pageable pageable) {
        return experienceCategoryRepository.findAll(pageable).map(this::toResponse);
    }

    // Una categoría puntual por id.
    @Override
    public ExperienceCategoryResponseDTO getCategoryById(Long categoryId) throws ResourceNotFoundException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una categoria con id " + categoryId));
        return toResponse(category);
    }

    // Crea una categoría nueva; el nombre tiene que ser único.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO createCategory(ExperienceCategoryRequestDTO request)
            throws BadRequestException, CategoryDuplicateException {
        String name = requireName(request.getName());
        if (experienceCategoryRepository.findByName(name).isPresent()) {
            throw new CategoryDuplicateException("Ya existe una categoria con el nombre \"" + name + "\"");
        }

        ExperienceCategory category = ExperienceCategory.builder()
                .name(name)
                .description(trimToNull(request.getDescription()))
                .build();

        return toResponse(experienceCategoryRepository.save(category));
    }

    // Actualiza nombre/descripción; también valida que el nuevo nombre siga siendo único.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO updateCategory(Long categoryId, ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, CategoryDuplicateException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una categoria con id " + categoryId));

        String name = requireName(request.getName());
        Optional<ExperienceCategory> sameName = experienceCategoryRepository.findByName(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(categoryId)) {
            throw new CategoryDuplicateException("Ya existe otra categoria con el nombre \"" + name + "\"");
        }

        category.setName(name);
        category.setDescription(trimToNull(request.getDescription()));

        return toResponse(experienceCategoryRepository.save(category));
    }

    // Borra una categoría; falla si todavía tiene experiencias asociadas.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteCategory(Long categoryId) throws ResourceNotFoundException, BadRequestException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una categoria con id " + categoryId));

        if (category.getExperiences() != null && !category.getExperiences().isEmpty()) {
            throw new BadRequestException(
                    "No se puede borrar la categoria \"" + category.getName()
                            + "\" porque tiene experiencias asociadas");
        }

        experienceCategoryRepository.delete(category);
    }

    private String requireName(String name) throws BadRequestException {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("El nombre de la categoria es obligatorio");
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
