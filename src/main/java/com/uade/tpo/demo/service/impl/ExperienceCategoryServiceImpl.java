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
                .orElseThrow(() -> new ResourceNotFoundException("La categoría no existe"));
        return toResponse(category);
    }

    // Crea una categoría nueva; el nombre tiene que ser único.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO createCategory(ExperienceCategoryRequestDTO request)
            throws BadRequestException, CategoryDuplicateException {
        String name = requireName(request.getName());
        if (experienceCategoryRepository.findByName(name).isPresent()) {
            throw new CategoryDuplicateException("Ya existe una categoría con el nombre \"" + name + "\"");
        }

        ExperienceCategory category = ExperienceCategory.builder()
                .name(name)
                .description(description(request.getDescription()))
                .build();

        return toResponse(experienceCategoryRepository.save(category));
    }

    // Actualiza nombre/descripción; también valida que el nuevo nombre siga siendo único.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO updateCategory(Long categoryId, ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException, BadRequestException, CategoryDuplicateException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("La categoría no existe"));

        String name = requireName(request.getName());
        Optional<ExperienceCategory> sameName = experienceCategoryRepository.findByName(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(categoryId)) {
            throw new CategoryDuplicateException("Ya existe otra categoría con el nombre \"" + name + "\"");
        }

        category.setName(name);
        category.setDescription(description(request.getDescription()));

        return toResponse(experienceCategoryRepository.save(category));
    }

    // Borra una categoría; falla si todavía tiene experiencias o intereses asociados.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteCategory(Long categoryId) throws ResourceNotFoundException, BadRequestException {
        ExperienceCategory category = experienceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("La categoría no existe"));

        if (category.getExperiences() != null && !category.getExperiences().isEmpty()) {
            throw new BadRequestException(
                    "No se puede borrar la categoría \"" + category.getName()
                            + "\" porque tiene experiencias asociadas");
        }
        if (category.getInterests() != null && !category.getInterests().isEmpty()) {
            throw new BadRequestException(
                    "No se puede borrar la categoría \"" + category.getName()
                            + "\" porque tiene intereses asociados. Pasalos a otra categoría o borralos primero");
        }

        experienceCategoryRepository.delete(category);
    }

    private String requireName(String name) throws BadRequestException {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("El nombre de la categoría es obligatorio");
        }
        if (name.trim().length() > 100) {
            throw new BadRequestException("El nombre de la categoría puede tener hasta 100 caracteres");
        }
        return name.trim();
    }

    private String description(String description) throws BadRequestException {
        String value = trimToNull(description);
        if (value != null && value.length() > 255) {
            throw new BadRequestException("La descripción de la categoría puede tener hasta 255 caracteres");
        }
        return value;
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
                .experienceCount(category.getExperiences() == null ? 0
                        : category.getExperiences().stream().filter(experience -> experience.getDeletedAt() == null).count())
                .build();
    }
}
