package com.uade.tpo.demo.service;

import java.util.List;
import java.util.Set;

import com.uade.tpo.demo.dtos.request.InterestRequestDTO;
import com.uade.tpo.demo.dtos.response.InterestResponseDTO;
import com.uade.tpo.demo.entity.Interest;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;

public interface InterestService {
    List<InterestResponseDTO> getInterests();

    InterestResponseDTO createInterest(InterestRequestDTO request) throws BadRequestException, ResourceNotFoundException;

    InterestResponseDTO updateInterest(Long interestId, InterestRequestDTO request)
            throws BadRequestException, ResourceNotFoundException;

    void deleteInterest(Long interestId) throws ResourceNotFoundException;

    /** Nombres que manda el front ("Vinos", "Caminata") → intereses de la base. 400 si alguno no existe o son más de `max`. */
    Set<Interest> resolve(List<String> names, int max) throws BadRequestException;
}
