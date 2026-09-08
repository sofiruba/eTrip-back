package com.uade.tpo.demo.controllers.experiences;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.tpo.demo.dtos.request.ExperienceDiscountRequestDTO;
import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("experiences")
@RequiredArgsConstructor
public class ExperiencesController {

    private final ExperienceService experienceService;
    private final ObjectMapper objectMapper;

    // Busca experiencias combinando filtros opcionales (categoría, texto, ubicación, precio, vendedor, fechas, ofertas).
    @GetMapping
    public ResponseEntity<Page<ExperienceResponseDTO>> getExperiences(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) Boolean onlyDiscounted,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo)
            throws ResourceNotFoundException, BadRequestException {
        ExperienceSearchDTO filter = ExperienceSearchDTO.builder()
                .categoryId(categoryId)
                .title(title)
                .location(location)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .publisherId(publisherId)
                .onlyDiscounted(onlyDiscounted)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        return ResponseEntity.ok(experienceService.searchExperiences(filter, pageRequest(page, size)));
    }

    // Mis publicaciones (modo vendedor).
    @GetMapping("/mine")
    public ResponseEntity<Page<ExperienceResponseDTO>> getMyExperiences(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) throws ResourceNotFoundException, BadRequestException {
        ExperienceSearchDTO filter = ExperienceSearchDTO.builder()
                .publisherId(currentUser.getId())
                .build();

        return ResponseEntity.ok(experienceService.searchExperiences(filter, pageRequest(page, size)));
    }

    // Una experiencia puntual por id.
    @GetMapping("/{experienceId}")
    public ResponseEntity<ExperienceResponseDTO> getExperienceById(@PathVariable Long experienceId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(experienceService.getExperienceById(experienceId));
    }

    // Crea una experiencia: multipart con parte "experience" (JSON como texto, parseado a mano
    // para no depender del Content-Type que le ponga el cliente) + una o mas fotos "images".
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperienceResponseDTO> createExperience(
            @RequestParam("experience") String experienceJson,
            @RequestParam("images") List<MultipartFile> images,
            @AuthenticationPrincipal User publisher)
            throws ResourceNotFoundException, BadRequestException, IOException {
        ExperienceRequestDTO request = parseExperience(experienceJson);
        ExperienceResponseDTO result = experienceService.createExperience(request, images, publisher.getId());
        return ResponseEntity.created(URI.create("/experiences/" + result.getId())).body(result);
    }

    // Actualiza una experiencia; "images" es opcional (si no viene, mantiene las fotos actuales; si viene, las reemplaza todas).
    @PutMapping(value = "/{experienceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperienceResponseDTO> updateExperience(
            @PathVariable Long experienceId,
            @RequestParam("experience") String experienceJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException, IOException {
        ExperienceRequestDTO request = parseExperience(experienceJson);
        return ResponseEntity.ok(experienceService.updateExperience(experienceId, request, images, currentUser));
    }

    // Parsea el JSON de la parte "experience" del multipart a mano.
    private ExperienceRequestDTO parseExperience(String experienceJson) throws BadRequestException {
        try {
            return objectMapper.readValue(experienceJson, ExperienceRequestDTO.class);
        } catch (IOException e) {
            throw new BadRequestException();
        }
    }

    // Cambia el descuento de la experiencia (0 <= x < 100). Solo el dueño o un ADMIN.
    @PatchMapping("/{experienceId}/discount")
    public ResponseEntity<ExperienceResponseDTO> updateDiscount(
            @PathVariable Long experienceId,
            @RequestBody ExperienceDiscountRequestDTO request,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        BigDecimal discountPercentage = request != null ? request.getDiscountPercentage() : null;
        return ResponseEntity.ok(experienceService.updateDiscount(experienceId, discountPercentage, currentUser));
    }

    // Borra una experiencia; falla si tiene sesiones asociadas. Solo el dueño o un ADMIN.
    @DeleteMapping("/{experienceId}")
    public ResponseEntity<Void> deleteExperience(
            @PathVariable Long experienceId,
            @AuthenticationPrincipal User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        experienceService.deleteExperience(experienceId, currentUser);
        return ResponseEntity.noContent().build();
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return page == null || size == null
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);
    }
}
