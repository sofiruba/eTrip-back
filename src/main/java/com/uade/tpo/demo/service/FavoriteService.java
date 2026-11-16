package com.uade.tpo.demo.service;

import java.util.List;

import com.uade.tpo.demo.dtos.response.FavoriteResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface FavoriteService {

    // Las experiencias que guardó el usuario, la más reciente primero.
    List<FavoriteResponseDTO> getMine(User user);

    // Guarda una experiencia (si ya estaba guardada no pasa nada).
    FavoriteResponseDTO add(Long experienceId, User user) throws ResourceNotFoundException, BadRequestException;

    // La saca de los guardados.
    void remove(Long experienceId, User user);
}
