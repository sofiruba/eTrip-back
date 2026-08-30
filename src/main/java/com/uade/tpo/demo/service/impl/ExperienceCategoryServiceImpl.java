package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ExperienceCategoryRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceCategoryResponseDTO;
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
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Consultar experienceCategoryRepository.findAll(pageable).
        // 2. Mapear cada ExperienceCategory a ExperienceCategoryResponseDTO.
        // 3. Retornar la pagina mapeada manteniendo metadata de paginacion.
        return null;
    }

    @Override
    public ExperienceCategoryResponseDTO getCategoryById(Long categoryId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar ExperienceCategory por id usando experienceCategoryRepository.findById(categoryId).
        // 2. Si no existe, lanzar ResourceNotFoundException.
        // 3. Mapear la entidad encontrada a ExperienceCategoryResponseDTO.
        // 4. Retornar el DTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO createCategory(ExperienceCategoryRequestDTO request) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Validar que request.getName() no venga vacio.
        // 2. Verificar si ya existe una categoria con el mismo nombre.
        // 3. Instanciar ExperienceCategory con name y description.
        // 4. Guardar usando experienceCategoryRepository.save().
        // 5. Mapear la entidad guardada a ExperienceCategoryResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceCategoryResponseDTO updateCategory(Long categoryId, ExperienceCategoryRequestDTO request)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar la categoria por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Validar que el nuevo nombre no genere duplicados.
        // 3. Actualizar name y description.
        // 4. Guardar cambios con experienceCategoryRepository.save().
        // 5. Mapear y retornar ExperienceCategoryResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteCategory(Long categoryId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar la categoria por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Validar que no tenga experiencias asociadas, o definir regla de borrado.
        // 3. Eliminar usando experienceCategoryRepository.delete().
    }
}
