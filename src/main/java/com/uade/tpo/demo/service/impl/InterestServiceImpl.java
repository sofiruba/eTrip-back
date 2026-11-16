package com.uade.tpo.demo.service.impl;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.dtos.request.InterestRequestDTO;
import com.uade.tpo.demo.dtos.response.InterestResponseDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceCategory;
import com.uade.tpo.demo.entity.Interest;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceCategoryRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSpecifications;
import com.uade.tpo.demo.repository.InterestRepository;
import com.uade.tpo.demo.service.InterestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private static final int MAX_NAME = 40;

    private final InterestRepository interestRepository;
    private final ExperienceCategoryRepository categoryRepository;
    private final ExperienceRepository experienceRepository;

    // Todos los intereses, agrupados por categoría y con cuántos planes disponibles tiene cada uno
    // (los mismos que muestra el catálogo público con "solo con lugares").
    @Override
    @Transactional(readOnly = true)
    public List<InterestResponseDTO> getInterests() {
        Map<Long, Long> available = availableCounts();
        return interestRepository.findAllByOrderByCategoryNameAscNameAsc().stream()
                .map(interest -> toResponse(interest, available.getOrDefault(interest.getId(), 0L)))
                .toList();
    }

    // Crea un interés nuevo (solo ADMIN); el nombre tiene que ser único.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public InterestResponseDTO createInterest(InterestRequestDTO request) throws BadRequestException, ResourceNotFoundException {
        String name = requireName(request.getName());
        if (interestRepository.findByName(name).isPresent()) {
            throw new BadRequestException("Ya existe el interés \"" + name + "\"");
        }
        Interest interest = Interest.builder()
                .name(name)
                .category(findCategory(request.getCategoryId()))
                .build();
        return toResponse(interestRepository.save(interest), 0);
    }

    // Cambia el nombre o la categoría. Como las experiencias y los usuarios apuntan al interés (no a su nombre),
    // renombrarlo se ve enseguida en todos lados.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public InterestResponseDTO updateInterest(Long interestId, InterestRequestDTO request)
            throws BadRequestException, ResourceNotFoundException {
        Interest interest = findInterest(interestId);
        String name = requireName(request.getName());
        Optional<Interest> sameName = interestRepository.findByName(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(interestId)) {
            throw new BadRequestException("Ya existe otro interés con el nombre \"" + name + "\"");
        }
        interest.setName(name);
        interest.setCategory(findCategory(request.getCategoryId()));
        return toResponse(interestRepository.save(interest), availableCounts().getOrDefault(interestId, 0L));
    }

    // Borra un interés: se lo saca a las experiencias y a los usuarios que lo tenían (perder una etiqueta no rompe nada).
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteInterest(Long interestId) throws ResourceNotFoundException {
        Interest interest = findInterest(interestId);
        if (interest.getExperiences() != null) {
            interest.getExperiences().forEach(experience -> experience.getTags().remove(interest));
        }
        if (interest.getUsers() != null) {
            interest.getUsers().forEach(user -> user.getInterests().remove(interest));
        }
        interestRepository.delete(interest);
    }

    // Lo usan el registro, el perfil (hasta 20) y el editor de experiencias (hasta 8)
    @Override
    @Transactional(readOnly = true)
    public Set<Interest> resolve(List<String> names, int max) throws BadRequestException {
        Set<String> clean = new LinkedHashSet<>();
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.isBlank()) {
                    clean.add(name.trim());
                }
            }
        }
        if (clean.size() > max) {
            throw new BadRequestException("Podés elegir hasta " + max + " intereses");
        }
        Set<Interest> found = new LinkedHashSet<>(interestRepository.findByNameIn(clean));
        if (found.size() < clean.size()) {
            Set<String> missing = new LinkedHashSet<>(clean);
            found.forEach(interest -> missing.remove(interest.getName()));
            throw new BadRequestException("\"" + missing.iterator().next() + "\" no es un interés de PLAN");
        }
        return found;
    }

    // id del interés → cuántas experiencias del catálogo público con fecha disponible lo tienen
    private Map<Long, Long> availableCounts() {
        Map<Long, Long> available = new HashMap<>();
        ExperienceSearchDTO onlyAvailable = ExperienceSearchDTO.builder().onlyAvailable(true).build();
        for (Experience experience : experienceRepository.findAll(ExperienceSpecifications.withFilters(onlyAvailable, false, false))) {
            for (Interest tag : experience.getTags()) {
                available.merge(tag.getId(), 1L, Long::sum);
            }
        }
        return available;
    }

    private Interest findInterest(Long interestId) throws ResourceNotFoundException {
        return interestRepository.findById(interestId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un interés con id " + interestId));
    }

    private ExperienceCategory findCategory(Long categoryId) throws ResourceNotFoundException, BadRequestException {
        if (categoryId == null) {
            throw new BadRequestException("Elegí la categoría del interés");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una categoria con id " + categoryId));
    }

    private String requireName(String name) throws BadRequestException {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("El nombre del interés es obligatorio");
        }
        if (name.trim().length() > MAX_NAME) {
            throw new BadRequestException("El nombre del interés puede tener hasta " + MAX_NAME + " caracteres");
        }
        return name.trim();
    }

    private InterestResponseDTO toResponse(Interest interest, long availableExperiences) {
        return InterestResponseDTO.builder()
                .id(interest.getId())
                .name(interest.getName())
                .categoryId(interest.getCategory().getId())
                .categoryName(interest.getCategory().getName())
                .availableExperiences(availableExperiences)
                .build();
    }
}
