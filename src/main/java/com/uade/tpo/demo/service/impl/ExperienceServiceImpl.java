package com.uade.tpo.demo.service.impl;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceCategoryRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final ExperienceCategoryRepository experienceCategoryRepository;
    private final UserRepository userRepository;

    @Override
    public Page<ExperienceResponseDTO> getExperiences(Pageable pageable) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Consultar experienceRepository.findAll(pageable).
        // 2. Mapear cada Experience a ExperienceResponseDTO.
        // 3. Convertir image byte[] a Base64 usando Base64.getEncoder().encodeToString(...).
        // 4. Retornar Page<ExperienceResponseDTO>.
        return null;
    }

    @Override
    public ExperienceResponseDTO getExperienceById(Long experienceId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar Experience por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Mapear datos basicos, categoria y publisher.
        // 3. Convertir image byte[] a Base64 si la experiencia tiene imagen.
        // 4. Retornar ExperienceResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO createExperience(ExperienceRequestDTO request, MultipartFile image)
            throws ResourceNotFoundException, IOException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar ExperienceCategory usando request.getCategoryId(). Lanzar ResourceNotFoundException si no existe.
        // 2. Buscar User publisher usando request.getPublisherId(). Lanzar ResourceNotFoundException si no existe.
        // 3. Validar campos obligatorios: title, price, location y fechas futuras si aplica.
        // 4. Leer la imagen con image.getBytes() y guardarla en el atributo byte[] image.
        // 5. Instanciar Experience asociando category y publisher.
        // 6. Guardar usando experienceRepository.save().
        // 7. Mapear la entidad guardada a ExperienceResponseDTO incluyendo imageBase64.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO updateExperience(Long experienceId, ExperienceRequestDTO request, MultipartFile image)
            throws ResourceNotFoundException, IOException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar Experience por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Si cambia categoryId, buscar la nueva categoria y asociarla.
        // 3. Si cambia publisherId, validar que el usuario exista y que tenga permiso para publicar.
        // 4. Actualizar title, description, price y location.
        // 5. Si viene una nueva imagen, reemplazar byte[] usando image.getBytes().
        // 6. Guardar usando experienceRepository.save().
        // 7. Retornar ExperienceResponseDTO con imagen en Base64.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteExperience(Long experienceId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar Experience por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Validar que no existan sesiones con reservas activas.
        // 3. Definir si corresponde borrado fisico o logico.
        // 4. Eliminar usando experienceRepository.delete().
    }
}
