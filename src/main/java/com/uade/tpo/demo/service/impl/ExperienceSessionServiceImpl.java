package com.uade.tpo.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ExperienceSessionRequestDTO;
import com.uade.tpo.demo.dtos.response.ExperienceSessionResponseDTO;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.service.ExperienceSessionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceSessionServiceImpl implements ExperienceSessionService {

    private final ExperienceSessionRepository experienceSessionRepository;
    private final ExperienceRepository experienceRepository;

    @Override
    public Page<ExperienceSessionResponseDTO> getSessions(Pageable pageable) {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Consultar experienceSessionRepository.findAll(pageable).
        // 2. Mapear cada ExperienceSession a ExperienceSessionResponseDTO.
        // 3. Incluir datos minimos de la Experience asociada.
        // 4. Retornar la pagina mapeada.
        return null;
    }

    @Override
    public ExperienceSessionResponseDTO getSessionById(Long sessionId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar ExperienceSession por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Mapear id, fechas, capacidad, cupos disponibles y datos de Experience.
        // 3. Retornar ExperienceSessionResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceSessionResponseDTO createSession(ExperienceSessionRequestDTO request) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar Experience usando request.getExperienceId(). Lanzar ResourceNotFoundException si no existe.
        // 2. Validar que startsAt sea anterior a endsAt y que ambas fechas sean validas.
        // 3. Validar que capacity sea mayor a cero.
        // 4. Instanciar ExperienceSession con availableSeats igual a capacity.
        // 5. Asociar la Experience.
        // 6. Guardar usando experienceSessionRepository.save().
        // 7. Retornar ExperienceSessionResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceSessionResponseDTO updateSession(Long sessionId, ExperienceSessionRequestDTO request)
            throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar ExperienceSession por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Si cambia experienceId, buscar la Experience nueva y asociarla.
        // 3. Validar fechas y capacidad.
        // 4. Recalcular availableSeats considerando reservas ya generadas.
        // 5. Guardar usando experienceSessionRepository.save().
        // 6. Retornar ExperienceSessionResponseDTO.
        return null;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteSession(Long sessionId) throws ResourceNotFoundException {
        // TODO: Equipo, aca debemos hacer lo siguiente:
        // 1. Buscar ExperienceSession por id. Lanzar ResourceNotFoundException si no existe.
        // 2. Validar que no tenga Bookings asociados.
        // 3. Eliminar usando experienceSessionRepository.delete().
    }
}
