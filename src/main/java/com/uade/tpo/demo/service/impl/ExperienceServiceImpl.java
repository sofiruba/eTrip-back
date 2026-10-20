package com.uade.tpo.demo.service.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
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
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.ExperienceImage;
import com.uade.tpo.demo.entity.Review;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.exceptions.BadRequestException;
import com.uade.tpo.demo.exceptions.ForbiddenException;
import com.uade.tpo.demo.exceptions.ResourceNotFoundException;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.CartItemRepository;
import com.uade.tpo.demo.repository.ExperienceCategoryRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.ExperienceSpecifications;
import com.uade.tpo.demo.repository.ReviewRepository;
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
    // Mismos límites que el editor del front (PhotoPicker): hasta 5 fotos de hasta 5 MB.
    private static final int MAX_IMAGES = 5;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    // Una foto de pocos KB puede declarar 30000x30000 px y pedir GB de memoria al decodificarla:
    // se miran las dimensiones antes de leerla y se rechaza lo que pase de este tope (40 megapíxeles).
    private static final long MAX_SOURCE_PIXELS = 40_000_000L;
    private static final String INVALID_IMAGE = "Una de las fotos no es una imagen válida. Usá JPG, PNG o WEBP.";

    private final ExperienceRepository experienceRepository;
    private final ExperienceCategoryRepository experienceCategoryRepository;
    private final ExperienceSessionRepository experienceSessionRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CartItemRepository cartItemRepository;

    // Busca experiencias combinando filtros opcionales (ver ExperienceSpecifications).
    // Los listados mandan solo la portada: todas las fotos de todas las experiencias hacían pesadísimo el inicio.
    @Override
    @Transactional(readOnly = true)
    public Page<ExperienceResponseDTO> searchExperiences(ExperienceSearchDTO filter, Pageable pageable,
            boolean includeInactive) throws ResourceNotFoundException, BadRequestException {
        if (filter.getCategoryId() != null) {
            validateCategoryExists(filter.getCategoryId());
        }
        if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
            throw new BadRequestException("El precio mínimo no puede ser mayor al máximo");
        }
        if (filter.getDateFrom() != null && filter.getDateTo() != null
                && filter.getDateFrom().isAfter(filter.getDateTo())) {
            throw new BadRequestException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        if (filter.getDurationMin() != null && filter.getDurationMax() != null
                && filter.getDurationMin() > filter.getDurationMax()) {
            throw new BadRequestException("La duración mínima no puede ser mayor a la máxima");
        }

        return experienceRepository.findAll(ExperienceSpecifications.withFilters(filter, includeInactive), pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExperienceResponseDTO getExperienceById(Long experienceId, User currentUser)
            throws ResourceNotFoundException {
        Experience experience = findExperience(experienceId);
        if (!experience.isActive() && !canManage(experience, currentUser)) {
            throw new ResourceNotFoundException("Esta experiencia no está disponible");
        }
        return toResponse(experience);
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
            throw new BadRequestException("Subí al menos una foto de la experiencia");
        }

        User publisher = userRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no existe"));
        ExperienceCategory category = experienceCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("La categoría elegida no existe"));

        String title = request.getTitle().trim();
        if (experienceRepository.existsByPublisherIdAndTitleIgnoreCase(publisherId, title)) {
            throw new BadRequestException("Ya publicaste una experiencia con ese título");
        }

        Experience experience = Experience.builder()
                .title(title)
                .description(trimToNull(request.getDescription()))
                .price(request.getPrice())
                .location(trimToNull(request.getLocation()))
                .adultsOnly(Boolean.TRUE.equals(request.getAdultsOnly()))
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
                    .orElseThrow(() -> new ResourceNotFoundException("La categoría elegida no existe"));
            experience.setCategory(category);
        }

        String title = request.getTitle().trim();
        Long publisherId = experience.getPublisher() != null ? experience.getPublisher().getId() : null;
        if (publisherId != null
                && experienceRepository.existsByPublisherIdAndTitleIgnoreCaseAndIdNot(publisherId, title, experienceId)) {
            throw new BadRequestException("Ya publicaste una experiencia con ese título");
        }
        experience.setTitle(title);
        experience.setDescription(trimToNull(request.getDescription()));
        experience.setPrice(request.getPrice());
        experience.setLocation(trimToNull(request.getLocation()));
        experience.setAdultsOnly(Boolean.TRUE.equals(request.getAdultsOnly()));

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
            // Mismo tope que el front (editor y DiscountModal)
            if (discountPercentage.signum() < 0 || discountPercentage.compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new BadRequestException("El descuento tiene que estar entre 0 y 90%");
            }
            experience.setDiscountPercentage(discountPercentage);
        }

        return toResponse(experienceRepository.save(experience));
    }

    // Borra una experiencia en cascada: sus reseñas, sus fechas y los ítems de carrito. Solo el dueño o un ADMIN.
    // Si alguna vez se vendió (tiene reservas, aunque sean de fechas pasadas o reembolsadas) no se borra: esas reservas
    // son los vouchers y el historial de compra de otras personas, y las órdenes y las estadísticas dependen de ellas.
    // En ese caso se pausa (PATCH /experiences/{id}/status?active=false).
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteExperience(Long experienceId, User currentUser)
            throws ResourceNotFoundException, BadRequestException, ForbiddenException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);

        List<ExperienceSession> sessions = experience.getSessions() != null ? experience.getSessions() : List.of();

        boolean hasBookings = sessions.stream()
                .anyMatch(session -> session.getBookings() != null && !session.getBookings().isEmpty());
        if (hasBookings) {
            throw new BadRequestException("Esta experiencia ya tuvo reservas y los compradores conservan sus vouchers, "
                    + "así que no se puede eliminar. Podés desactivarla para que no se vea más en el catálogo.");
        }

        // Cascada: reseñas → por cada fecha, sus ítems de carrito → las fechas → la experiencia.
        // Las fotos se borran solas (cascade = ALL en Experience.images).
        if (experience.getReviews() != null) {
            reviewRepository.deleteAll(experience.getReviews());
        }
        for (ExperienceSession session : sessions) {
            cartItemRepository.deleteByExperienceSessionId(session.getId());
        }
        experienceSessionRepository.deleteAll(sessions);
        experienceRepository.delete(experience);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ExperienceResponseDTO updateStatus(Long experienceId, boolean active, User currentUser)
            throws ResourceNotFoundException, ForbiddenException {
        Experience experience = findExperience(experienceId);
        assertCanManage(experience, currentUser);
        experience.setActive(active);
        return toResponse(experienceRepository.save(experience));
    }

    private Experience findExperience(Long experienceId) throws ResourceNotFoundException {
        return experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("La experiencia no existe"));
    }

    private void validateCategoryExists(Long categoryId) throws ResourceNotFoundException {
        if (!experienceCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("La categoría elegida no existe");
        }
    }

    private void assertCanManage(Experience experience, User currentUser) throws ForbiddenException {
        if (!canManage(experience, currentUser)) {
            throw new ForbiddenException("Solo el anfitrión de la experiencia puede modificarla");
        }
    }

    private boolean canManage(Experience experience, User currentUser) {
        boolean isOwner = experience.getPublisher() != null
                && currentUser != null
                && experience.getPublisher().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        return isOwner || isAdmin;
    }

    private void validateData(ExperienceRequestDTO request) throws BadRequestException {
        if (request == null) {
            throw new BadRequestException("Faltan los datos de la experiencia");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("La experiencia necesita un título");
        }
        // Topes de las columnas de la base (si no, un texto largo terminaba en un error 500)
        if (request.getTitle().trim().length() > 150) {
            throw new BadRequestException("El título puede tener hasta 150 caracteres");
        }
        if (request.getDescription() != null && request.getDescription().trim().length() > 2000) {
            throw new BadRequestException("La descripción puede tener hasta 2000 caracteres");
        }
        if (request.getLocation() != null && request.getLocation().trim().length() > 200) {
            throw new BadRequestException("La ubicación puede tener hasta 200 caracteres");
        }
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new BadRequestException("El precio tiene que ser mayor a 0");
        }
        if (request.getCategoryId() == null) {
            throw new BadRequestException("Elegí una categoría");
        }
        if (request.getLocation() == null || request.getLocation().isBlank()
                || request.getLocation().split(",").length < 2
                || request.getLocation().matches(".*\\d.*")) {
            throw new BadRequestException("Indicá un barrio y una ciudad válidos, sin dirección exacta.");
        }
    }

    // Las fotos que vinieron con contenido, validando cantidad y peso (el front también lo hace, pero se puede saltear).
    private List<MultipartFile> nonEmptyImages(List<MultipartFile> images) throws BadRequestException {
        if (images == null) {
            return List.of();
        }
        List<MultipartFile> result = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                if (image.getSize() > MAX_IMAGE_BYTES) {
                    throw new BadRequestException("Cada foto puede pesar hasta 5 MB");
                }
                result.add(image);
            }
        }
        if (result.size() > MAX_IMAGES) {
            throw new BadRequestException("Podés subir hasta " + MAX_IMAGES + " fotos");
        }
        return result;
    }

    // Convierte los archivos subidos en entidades ExperienceImage, comprimiendo cada una.
    private List<ExperienceImage> toImageEntities(List<MultipartFile> images, Experience experience)
            throws IOException, BadRequestException {
        List<ExperienceImage> result = new ArrayList<>();
        int position = 0;
        for (MultipartFile image : images) {
            result.add(ExperienceImage.builder()
                    .image(prepareImage(image.getBytes()))
                    .position(position++)
                    .experience(experience)
                    .build());
        }
        return result;
    }

    // Deja la foto lista para guardar: rechaza lo que no es una imagen y achica/recomprime el resto.
    // WEBP se guarda tal cual porque Java no lo sabe decodificar (y el front ya lo limita a 5 MB).
    private byte[] prepareImage(byte[] original) throws BadRequestException {
        if (isWebp(original)) {
            return original;
        }
        return compressImage(readImageSafely(original), original);
    }

    // "RIFF....WEBP" al principio del archivo
    private boolean isWebp(byte[] bytes) {
        return bytes.length > 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    // Lee la imagen mirando antes ancho y alto (están en el encabezado), así una "bomba" chica que declara
    // dimensiones enormes se rechaza sin reservar la memoria que pediría decodificarla.
    private BufferedImage readImageSafely(byte[] original) throws BadRequestException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            Iterator<ImageReader> readers = input != null ? ImageIO.getImageReaders(input) : null;
            if (readers == null || !readers.hasNext()) {
                throw new BadRequestException(INVALID_IMAGE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new BadRequestException(INVALID_IMAGE);
                }
                if (width * height > MAX_SOURCE_PIXELS) {
                    throw new BadRequestException("Una de las fotos tiene una resolución demasiado grande. Achicala y probá de nuevo.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new BadRequestException(INVALID_IMAGE);
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BadRequestException(INVALID_IMAGE);
        }
    }

    // Redimensiona y recomprime a JPEG antes de guardar como BLOB, así las fotos de celular no
    // inflan la tabla ni la respuesta en base64. Si no se puede recomprimir, guarda el original.
    private byte[] compressImage(BufferedImage source, byte[] original) {
        try {
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
        return toResponse(experience, true);
    }

    // Versión para listados: solo la portada (las tarjetas usan la primera foto; el detalle pide la experiencia por id).
    private ExperienceResponseDTO toListResponse(Experience experience) {
        return toResponse(experience, false);
    }

    // Tiene alguna fecha vigente por delante: es lo mismo que pide el catálogo público para mostrarla
    private boolean hasUpcomingSessions(Experience experience) {
        LocalDateTime now = LocalDateTime.now();
        return experience.getSessions() != null && experience.getSessions().stream()
                .anyMatch(session -> session.isActive() && session.getStartsAt() != null && session.getStartsAt().isAfter(now));
    }

    private ExperienceResponseDTO toResponse(Experience experience, boolean allImages) {
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
                    if (!allImages) {
                        break;
                    }
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
                .active(experience.isActive())
                .status(!experience.isActive() ? "INACTIVE" : hasUpcomingSessions(experience) ? "ACTIVE" : "NO_DATES")
                .adultsOnly(experience.isAdultsOnly())
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
