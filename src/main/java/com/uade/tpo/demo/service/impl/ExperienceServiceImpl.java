package com.uade.tpo.demo.service.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.uade.tpo.demo.dtos.request.ExperienceRequestDTO;
import com.uade.tpo.demo.dtos.request.ExperienceSearchDTO;
import com.uade.tpo.demo.dtos.response.ExperienceResponseDTO;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceCategory;
import com.uade.tpo.demo.entity.ExperienceImage;
import com.uade.tpo.demo.entity.Review;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.ExperienceCategoryRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSpecifications;
import com.uade.tpo.demo.repository.UserRepository;
import com.uade.tpo.demo.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    // Lado mas largo permitido para una foto de experiencia, en pixeles.
    private static final int IMAGE_MAX_DIMENSION = 1600;
    // Calidad JPEG (0 a 1) usada al recomprimir las fotos subidas.
    private static final float IMAGE_JPEG_QUALITY = 0.75f;

    private final ExperienceRepository experienceRepository;
    private final ExperienceCategoryRepository experienceCategoryRepository;
    private final UserRepository userRepository;

    // Lista todas las experiencias sin filtrar.
    @Override
    @Transactional(readOnly = true)
    public Page<ExperienceResponseDTO> getExperiences(Pageable pageable) {
        return experienceRepository.findAll(pageable).map(this::toResponse);
    }

    // Busca experiencias combinando filtros opcionales (categoría, precio, ubicación, fechas, etc).
    @Override
    @Transactional(readOnly = true)
    public Page<ExperienceResponseDTO> searchExperiences(ExperienceSearchDTO filter, Pageable pageable)
            throws ResourceNotFoundException, BadRequestException {
        if (filter == null) {
            return getExperiences(pageable);
        }

        if (filter.getCategoryId() != null) {
            validateCategoryExists(filter.getCategoryId());
        }
        if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
            throw new BadRequestException();
        }
        if (filter.getDateFrom() != null && filter.getDateTo() != null
                && filter.getDateFrom().isAfter(filter.getDateTo())) {
            throw new BadRequestException();
        }

        return experienceRepository.findAll(ExperienceSpecifications.withFilters(filter), pageable)
                .map(this::toResponse);
    }

    // Una experiencia puntual por id.
    @Override
    @Transactional(readOnly = true)
    public ExperienceResponseDTO getExperienceById(Long experienceId) throws ResourceNotFoundException {
        return toResponse(findExperience(experienceId));
    }

    // Crea una experiencia nueva; exige al menos 1 foto y un título que no se repita para el mismo vendedor.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO createExperience(ExperienceRequestDTO request, List<MultipartFile> images,
            Long publisherId)
            throws ResourceNotFoundException, BadRequestException, IOException {
        validateData(request);
        List<MultipartFile> validImages = nonEmptyImages(images);
        if (publisherId == null || validImages.isEmpty()) {
            throw new BadRequestException();
        }

        User publisher = userRepository.findById(publisherId)
                .orElseThrow(ResourceNotFoundException::new);
        ExperienceCategory category = experienceCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(ResourceNotFoundException::new);

        String title = request.getTitle().trim();
        if (experienceRepository.existsByPublisherIdAndTitleIgnoreCase(publisherId, title)) {
            throw new BadRequestException();
        }

        Experience experience = Experience.builder()
                .title(title)
                .description(trimToNull(request.getDescription()))
                .price(request.getPrice())
                .location(trimToNull(request.getLocation()))
                .category(category)
                .publisher(publisher)
                .build();

        experience.setImages(toImageEntities(validImages, experience));

        return toResponse(experienceRepository.save(experience));
    }

    // Actualiza una experiencia existente; solo el dueño o un ADMIN. Si vienen fotos nuevas,
    // reemplaza todo el set (orphanRemoval borra las viejas).
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO updateExperience(Long experienceId, ExperienceRequestDTO request,
            List<MultipartFile> images, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException, IOException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);
        validateData(request);

        if (!request.getCategoryId().equals(experience.getCategory().getId())) {
            ExperienceCategory category = experienceCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(ResourceNotFoundException::new);
            experience.setCategory(category);
        }

        String title = request.getTitle().trim();
        Long publisherId = experience.getPublisher() != null ? experience.getPublisher().getId() : null;
        if (publisherId != null
                && experienceRepository.existsByPublisherIdAndTitleIgnoreCaseAndIdNot(publisherId, title, experienceId)) {
            throw new BadRequestException();
        }
        experience.setTitle(title);
        experience.setDescription(trimToNull(request.getDescription()));
        experience.setPrice(request.getPrice());
        experience.setLocation(trimToNull(request.getLocation()));

        List<MultipartFile> validImages = nonEmptyImages(images);
        if (!validImages.isEmpty()) {
            if (experience.getImages() != null) {
                experience.getImages().clear();
            } else {
                experience.setImages(new ArrayList<>());
            }
            experience.getImages().addAll(toImageEntities(validImages, experience));
        }

        return toResponse(experienceRepository.save(experience));
    }

    // Cambia el descuento de la experiencia (0 <= x < 100). Solo el dueño o un ADMIN.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO updateDiscount(Long experienceId, BigDecimal discountPercentage, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);

        if (discountPercentage == null || discountPercentage.signum() == 0) {
            experience.setDiscountPercentage(null);
        } else {
            if (discountPercentage.signum() < 0 || discountPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                throw new BadRequestException();
            }
            experience.setDiscountPercentage(discountPercentage);
        }

        return toResponse(experienceRepository.save(experience));
    }

    // Borra una experiencia; falla si tiene sesiones asociadas. Solo el dueño o un ADMIN.
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteExperience(Long experienceId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);

        if (experience.getSessions() != null && !experience.getSessions().isEmpty()) {
            throw new BadRequestException();
        }

        experienceRepository.delete(experience);
    }

    private Experience findExperience(Long experienceId) throws ResourceNotFoundException {
        return experienceRepository.findById(experienceId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private void validateCategoryExists(Long categoryId) throws ResourceNotFoundException {
        if (!experienceCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException();
        }
    }

    private void assertCanManage(Experience experience, User currentUser) throws ForbiddenException {
        boolean isOwner = experience.getPublisher() != null
                && currentUser != null
                && experience.getPublisher().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException();
        }
    }

    private void validateData(ExperienceRequestDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException();
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException();
        }
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new BadRequestException();
        }
        if (request.getCategoryId() == null) {
            throw new BadRequestException();
        }
    }

    private List<MultipartFile> nonEmptyImages(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        List<MultipartFile> result = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                result.add(image);
            }
        }
        return result;
    }

    // Convierte los archivos subidos en entidades ExperienceImage, comprimiendo cada una.
    private List<ExperienceImage> toImageEntities(List<MultipartFile> images, Experience experience)
            throws IOException {
        List<ExperienceImage> result = new ArrayList<>();
        int position = 0;
        for (MultipartFile image : images) {
            result.add(ExperienceImage.builder()
                    .image(compressImage(image.getBytes()))
                    .position(position++)
                    .experience(experience)
                    .build());
        }
        return result;
    }

    // Redimensiona y recomprime a JPEG antes de guardar como BLOB, así las fotos de celular no
    // inflan la tabla ni la respuesta en base64. Si no se puede decodificar, guarda el original.
    private byte[] compressImage(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                return original;
            }

            int width = source.getWidth();
            int height = source.getHeight();
            double scale = Math.min(1.0, (double) IMAGE_MAX_DIMENSION / Math.max(width, height));
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));

            BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, Color.WHITE, null);
            g.dispose();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                return original;
            }
            ImageWriter writer = writers.next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(IMAGE_JPEG_QUALITY);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(target, null, null), params);
            } finally {
                writer.dispose();
            }

            byte[] compressed = out.toByteArray();
            return compressed.length < original.length ? compressed : original;
        } catch (IOException e) {
            return original;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Arma el DTO de respuesta, incluyendo rating promedio y fotos en base64.
    private ExperienceResponseDTO toResponse(Experience experience) {
        List<Review> reviews = experience.getReviews();
        int reviewCount = reviews != null ? reviews.size() : 0;
        Double averageRating = reviewCount > 0
                ? reviews.stream().mapToInt(Review::getRating).average().orElse(0)
                : null;

        List<String> imagesBase64 = new ArrayList<>();
        if (experience.getImages() != null) {
            for (ExperienceImage image : experience.getImages()) {
                if (image.getImage() != null) {
                    imagesBase64.add(Base64.getEncoder().encodeToString(image.getImage()));
                }
            }
        }

        return ExperienceResponseDTO.builder()
                .id(experience.getId())
                .title(experience.getTitle())
                .description(experience.getDescription())
                .price(experience.getPrice())
                .discountPercentage(experience.getDiscountPercentage())
                .finalPrice(experience.getEffectivePrice())
                .location(experience.getLocation())
                .imagesBase64(imagesBase64)
                .categoryId(experience.getCategory() != null ? experience.getCategory().getId() : null)
                .categoryName(experience.getCategory() != null ? experience.getCategory().getName() : null)
                .publisherId(experience.getPublisher() != null ? experience.getPublisher().getId() : null)
                .publisherName(publisherName(experience.getPublisher()))
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .build();
    }

    private String publisherName(User publisher) {
        if (publisher == null) {
            return null;
        }
        String first = publisher.getFirstName() != null ? publisher.getFirstName() : "";
        String last = publisher.getLastName() != null ? publisher.getLastName() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? publisher.getEmail() : fullName;
    }
}
