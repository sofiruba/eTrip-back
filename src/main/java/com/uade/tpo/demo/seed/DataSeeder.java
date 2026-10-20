package com.uade.tpo.demo.seed;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.DiscountCoupon;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceCategory;
import com.uade.tpo.demo.entity.ExperienceImage;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Notification;
import com.uade.tpo.demo.entity.Order;
import com.uade.tpo.demo.entity.Review;
import com.uade.tpo.demo.entity.Role;
import com.uade.tpo.demo.entity.User;
import com.uade.tpo.demo.repository.BookingRepository;
import com.uade.tpo.demo.repository.DiscountCouponRepository;
import com.uade.tpo.demo.repository.ExperienceCategoryRepository;
import com.uade.tpo.demo.repository.ExperienceRepository;
import com.uade.tpo.demo.repository.ExperienceSessionRepository;
import com.uade.tpo.demo.repository.NotificationRepository;
import com.uade.tpo.demo.repository.OrderRepository;
import com.uade.tpo.demo.repository.ReviewRepository;
import com.uade.tpo.demo.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Carga de datos de ejemplo, de UN SOLO USO. Solo corre si se arranca con {@code --app.seed=true}:
 * <pre>
 *   ./mvnw spring-boot:run "-Dspring-boot.run.arguments=--app.seed=true"
 * </pre>
 * BORRA TODOS LOS DATOS de la base y carga lo que hay en {@code resources/seed/seed-data.json} con las fotos de
 * {@code resources/seed/fotos} (créditos en {@code resources/seed/CREDITOS-FOTOS.md}). Después la app sigue normal.
 * Sin el flag no hace nada, así que se puede borrar esta clase cuando ya no haga falta.
 *
 * Además de lo que se puede crear por la API, arma lo que la API no deja crear a mano: fechas que ya pasaron,
 * compras de esas fechas y reseñas (el back solo deja reseñar experiencias que terminaron).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final String PASSWORD = "plan1234";
    // Siempre los mismos datos "al azar": si se vuelve a cargar, queda igual
    private final Random random = new Random(2026);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaction;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ExperienceCategoryRepository categoryRepository;
    private final ExperienceRepository experienceRepository;
    private final ExperienceSessionRepository sessionRepository;
    private final DiscountCouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    public DataSeeder(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager, ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder, UserRepository userRepository, ExperienceCategoryRepository categoryRepository,
            ExperienceRepository experienceRepository, ExperienceSessionRepository sessionRepository,
            DiscountCouponRepository couponRepository, OrderRepository orderRepository, BookingRepository bookingRepository,
            ReviewRepository reviewRepository, NotificationRepository notificationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.transaction = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.experienceRepository = experienceRepository;
        this.sessionRepository = sessionRepository;
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.warn("=== DataSeeder: se BORRAN todos los datos y se cargan los de ejemplo ===");
        JsonNode data = objectMapper.readTree(new ClassPathResource("seed/seed-data.json").getInputStream());
        wipeDatabase();
        transaction.executeWithoutResult(status -> seed(data));
    }

    /** Vacía todas las tablas de la base (TRUNCATE con las claves foráneas desactivadas, en una misma conexión). */
    private void wipeDatabase() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            List<String> tables = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'")) {
                while (rows.next()) {
                    tables.add(rows.getString(1));
                }
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : tables) {
                    statement.execute("TRUNCATE TABLE `" + table + "`");
                }
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            log.info("Base vaciada ({} tablas)", tables.size());
            return null;
        });
    }

    private void seed(JsonNode data) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        // 1. Categorías
        Map<String, ExperienceCategory> categories = new HashMap<>();
        for (JsonNode node : data.get("categories")) {
            ExperienceCategory category = categoryRepository.save(ExperienceCategory.builder()
                    .name(node.get("name").asText())
                    .description(node.get("description").asText())
                    .build());
            categories.put(category.getName(), category);
        }

        // 2. Cupones (uno vencido y uno desactivado, para probar las validaciones)
        Map<String, DiscountCoupon> coupons = new HashMap<>();
        for (JsonNode node : data.get("coupons")) {
            DiscountCoupon coupon = couponRepository.save(DiscountCoupon.builder()
                    .code(node.get("code").asText())
                    .percentage(BigDecimal.valueOf(node.get("percentage").asInt()))
                    .validFrom(now.plusDays(node.get("validFromDays").asInt()).with(LocalTime.MIDNIGHT))
                    .validUntil(now.plusDays(node.get("validUntilDays").asInt()).with(LocalTime.of(23, 59)))
                    .active(node.get("active").asBoolean())
                    .build());
            coupons.put(coupon.getCode(), coupon);
        }
        List<DiscountCoupon> usableCoupons = List.of(coupons.get("BIENVENIDA15"), coupons.get("PLAN20"));

        // 3. Usuarios: admin, compradores y anfitriones (todos con la misma contraseña, salvo el admin)
        createUser(data.get("admin"), data.get("admin").get("password").asText(), Role.ADMIN);
        List<User> buyers = new ArrayList<>();
        for (JsonNode node : data.get("buyers")) {
            buyers.add(createUser(node, PASSWORD, Role.CLIENTE));
        }
        User demoBuyer = buyers.get(0); // sofia@plan.com: la cuenta para mostrar el flujo del comprador

        // 4. Experiencias con fotos, fechas pasadas y futuras, compras y reseñas
        int experienceCount = 0;
        int sessionCount = 0;
        int bookingCount = 0;
        int reviewCount = 0;
        int refundCount = 0;
        boolean soldOutDone = false;
        // Primero los anfitriones; después sus experiencias intercaladas (una de cada anfitrión por vuelta),
        // así el catálogo no arranca con todas las de una misma ciudad
        List<User> hosts = new ArrayList<>();
        int rounds = 0;
        for (JsonNode hostNode : data.get("hosts")) {
            hosts.add(createUser(hostNode, PASSWORD, Role.CLIENTE));
            rounds = Math.max(rounds, hostNode.get("experiences").size());
        }
        for (int round = 0; round < rounds; round++) {
            for (int h = 0; h < hosts.size(); h++) {
                JsonNode hostExperiences = data.get("hosts").get(h).get("experiences");
                if (round >= hostExperiences.size()) {
                    continue;
                }
                User host = hosts.get(h);
                JsonNode node = hostExperiences.get(round);
                Experience experience = createExperience(node, host, categories.get(node.get("category").asText()));
                experienceCount++;

                int capacity = node.get("capacity").asInt();
                long minutes = Math.round(node.get("hours").asDouble() * 60);
                List<String> times = new ArrayList<>();
                node.get("times").forEach(time -> times.add(time.asText()));

                // Fechas pasadas (para que haya compras viejas y reseñas) y futuras (para comprar)
                List<ExperienceSession> past = new ArrayList<>();
                for (int daysAgo : new int[] { 8 + random.nextInt(12), 28 + random.nextInt(30) }) {
                    past.add(createSession(experience, now.minusDays(daysAgo), times.get(random.nextInt(times.size())),
                            minutes, capacity));
                }
                List<ExperienceSession> upcoming = new ArrayList<>();
                int[] daysAhead = { 1 + random.nextInt(5), 7 + random.nextInt(8), 17 + random.nextInt(12), 32 + random.nextInt(14) };
                for (int i = 0; i < daysAhead.length; i++) {
                    upcoming.add(createSession(experience, now.plusDays(daysAhead[i]), times.get(i % times.size()),
                            minutes, capacity));
                }
                sessionCount += past.size() + upcoming.size();

                // Compras de fechas pasadas: de ahí salen las reseñas
                Set<Long> reviewed = new HashSet<>();
                for (ExperienceSession session : past) {
                    for (User buyer : pickBuyers(buyers, 2 + random.nextInt(3))) {
                        int quantity = 1 + random.nextInt(2);
                        if (session.getAvailableSeats() < quantity) {
                            break;
                        }
                        buy(buyer, session, quantity, session.getStartsAt().minusDays(3 + random.nextInt(15)), pickCoupon(usableCoupons));
                        bookingCount++;
                        // En la primera experiencia la cuenta de demo no reseña (ver abajo)
                        boolean demoKeepsPending = buyer == demoBuyer && experienceCount == 1;
                        if (!demoKeepsPending && !reviewed.contains(buyer.getId()) && random.nextInt(10) < 7) {
                            reviewed.add(buyer.getId());
                            createReview(data, buyer, experience, session, now);
                            reviewCount++;
                        }
                    }
                }
                // La cuenta de demo fue a la primera experiencia y todavía no la reseñó: así se puede mostrar "Dejá tu reseña"
                if (experienceCount == 1 && past.get(0).getAvailableSeats() > 0) {
                    buy(demoBuyer, past.get(0), 1, past.get(0).getStartsAt().minusDays(5), null);
                    bookingCount++;
                }

                // Compras de fechas futuras: algunas casi llenas ("¡Quedan N!") y una agotada
                for (int i = 0; i < upcoming.size(); i++) {
                    ExperienceSession session = upcoming.get(i);
                    int target;
                    if (!soldOutDone && "tango-milonga".equals(node.get("slug").asText()) && i == 0) {
                        target = capacity; // agotada
                        soldOutDone = true;
                    } else if (i == 0 && random.nextInt(5) == 0) {
                        target = Math.max(capacity - 2 - random.nextInt(2), 0); // quedan 2 o 3 lugares
                    } else {
                        target = random.nextInt(Math.max(capacity / 3, 1) + 1);
                    }
                    while (capacity - session.getAvailableSeats() < target) {
                        int quantity = Math.min(1 + random.nextInt(3), target - (capacity - session.getAvailableSeats()));
                        // La cuenta de demo no entra en las compras al azar: solo tiene las suyas (ver abajo)
                        User buyer = buyers.get(1 + random.nextInt(buyers.size() - 1));
                        Booking booking = buy(buyer, session, quantity, now.minusDays(1 + random.nextInt(10)), pickCoupon(usableCoupons));
                        bookingCount++;
                        // Algunas reservas se reembolsaron: los lugares vuelven a la fecha
                        if (random.nextInt(25) == 0) {
                            refund(booking, now.minusDays(random.nextInt(2)));
                            refundCount++;
                        }
                    }
                }

                // La cuenta de demo tiene reservas próximas en algunas experiencias (1 de cada 6)
                if (experienceCount % 6 == 2 && upcoming.get(1).getAvailableSeats() >= 2) {
                    Booking booking = buy(demoBuyer, upcoming.get(1), 2, now.minusDays(2), null);
                    bookingCount++;
                    notify(demoBuyer, "¡Reserva confirmada!", "Tu lugar en \"" + experience.getTitle() + "\" está confirmado. "
                            + "Voucher " + booking.getVoucherCode() + ".", now.minusDays(2));
                }
            }
        }

        notify(demoBuyer, "Bienvenida a PLAN", "Usá el cupón BIENVENIDA15 en tu próxima compra.", now.minusDays(20));
        log.info("=== DataSeeder listo: {} categorías, {} usuarios, {} experiencias, {} fechas, {} reservas ({} reembolsadas), {} reseñas, {} cupones ===",
                categories.size(), userRepository.count(), experienceCount, sessionCount, bookingCount, refundCount, reviewCount, coupons.size());
        log.info("Admin: {} / {} · Compradores y anfitriones: <email del JSON> / {} · Demo: {}",
                data.get("admin").get("email").asText(), data.get("admin").get("password").asText(), PASSWORD, demoBuyer.getEmail());
    }

    private User createUser(JsonNode node, String password, Role role) {
        return userRepository.save(User.builder()
                .email(node.get("email").asText())
                .displayUsername(node.get("username").asText())
                .firstName(node.get("firstname").asText())
                .lastName(node.get("lastname").asText())
                .password(passwordEncoder.encode(password))
                .role(role)
                .active(true)
                .build());
    }

    private Experience createExperience(JsonNode node, User host, ExperienceCategory category) {
        Experience experience = Experience.builder()
                .title(node.get("title").asText())
                .description(node.get("description").asText())
                .price(BigDecimal.valueOf(node.get("price").asLong()))
                .discountPercentage(BigDecimal.valueOf(node.path("discount").asInt(0)))
                .location(node.get("location").asText())
                .adultsOnly(node.path("adultsOnly").asBoolean(false))
                .category(category)
                .publisher(host)
                .active(true)
                .build();
        // Fotos: <slug>-1.jpg, <slug>-2.jpg… (las que haya)
        List<ExperienceImage> images = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ClassPathResource photo = new ClassPathResource("seed/fotos/" + node.get("slug").asText() + "-" + i + ".jpg");
            if (!photo.exists()) {
                break;
            }
            try (InputStream in = photo.getInputStream()) {
                images.add(ExperienceImage.builder().image(in.readAllBytes()).position(i - 1).experience(experience).build());
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer la foto " + photo.getPath(), e);
            }
        }
        experience.setImages(images);
        return experienceRepository.save(experience);
    }

    private ExperienceSession createSession(Experience experience, LocalDateTime day, String time, long minutes,
            int capacity) {
        LocalDateTime startsAt = day.with(LocalTime.parse(time));
        return sessionRepository.save(ExperienceSession.builder()
                .experience(experience)
                .startsAt(startsAt)
                .endsAt(startsAt.plusMinutes(minutes))
                .capacity(capacity)
                .availableSeats(capacity)
                .active(true)
                .build());
    }

    /** Una compra de una sola fecha: orden + voucher, y descuenta los lugares (igual que OrderServiceImpl). */
    private Booking buy(User buyer, ExperienceSession session, int quantity, LocalDateTime createdAt, DiscountCoupon coupon) {
        BigDecimal unitPrice = session.getExperience().getEffectivePrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discount = coupon == null ? BigDecimal.ZERO
                : subtotal.multiply(coupon.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        Order order = orderRepository.save(Order.builder()
                .user(buyer)
                .createdAt(createdAt)
                .count((long) quantity)
                .subtotal(subtotal)
                .discountAmount(discount)
                .total(subtotal.subtract(discount))
                .discountCoupon(coupon)
                .build());
        session.setAvailableSeats(session.getAvailableSeats() - quantity);
        sessionRepository.save(session);
        return bookingRepository.save(Booking.builder()
                .order(order)
                .experienceSession(session)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .createdAt(createdAt)
                .voucherCode(String.format("ETRIP-%08X", random.nextInt()))
                .build());
    }

    private void refund(Booking booking, LocalDateTime refundedAt) {
        ExperienceSession session = booking.getExperienceSession();
        session.setAvailableSeats(session.getAvailableSeats() + booking.getQuantity());
        sessionRepository.save(session);
        booking.setRefunded(true);
        booking.setRefundedAt(refundedAt);
        bookingRepository.save(booking);
    }

    private void createReview(JsonNode data, User buyer, Experience experience, ExperienceSession session, LocalDateTime now) {
        int roll = random.nextInt(100);
        int rating = roll < 55 ? 5 : roll < 90 ? 4 : 3;
        JsonNode comments = data.get("reviewComments").get(String.valueOf(rating));
        LocalDateTime createdAt = session.getEndsAt().plusDays(1 + random.nextInt(3));
        if (createdAt.isAfter(now)) {
            createdAt = now.minusHours(2);
        }
        Review.ReviewBuilder review = Review.builder()
                .user(buyer)
                .experience(experience)
                .rating(rating)
                .comment(comments.get(random.nextInt(comments.size())).asText())
                .createdAt(createdAt);
        if (random.nextInt(100) < 35) {
            JsonNode replies = data.get("hostReplies");
            LocalDateTime repliedAt = createdAt.plusDays(1).isAfter(now) ? now.minusHours(1) : createdAt.plusDays(1);
            review.hostReply(replies.get(random.nextInt(replies.size())).asText()).hostReplyAt(repliedAt);
        }
        reviewRepository.save(review.build());
    }

    private void notify(User user, String title, String message, LocalDateTime createdAt) {
        notificationRepository.save(Notification.builder().user(user).title(title).message(message).createdAt(createdAt).build());
    }

    private List<User> pickBuyers(List<User> buyers, int count) {
        List<User> shuffled = new ArrayList<>(buyers);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /** 1 de cada 5 compras usa un cupón. */
    private DiscountCoupon pickCoupon(List<DiscountCoupon> coupons) {
        return random.nextInt(5) == 0 ? coupons.get(random.nextInt(coupons.size())) : null;
    }
}
