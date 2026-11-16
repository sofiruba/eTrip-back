package com.uade.tpo.demo.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Experience;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.Order;
import com.uade.tpo.demo.entity.User;

/**
 * Arma el voucher de una reserva como imagen PNG, para descargarlo o compartirlo (por ejemplo, por WhatsApp).
 * Se dibuja con Java2D (lo trae Java, igual que el achicado de las fotos): no hace falta ninguna librería.
 * Quién puede pedirlo lo controla BookingServiceImpl.getVoucherImage.
 */
@Service
public class VoucherImageService {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 560;
    private static final int MARGIN = 56;
    private static final Color BRAND = new Color(0xB7, 0x23, 0x0F);
    private static final Color BRAND_SOFT = new Color(0xFF, 0xF1, 0xEE);
    private static final Color INK = new Color(0x1C, 0x1C, 0x19);
    private static final Color MUTED = new Color(0x6B, 0x66, 0x5E);
    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES_AR);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm", ES_AR);

    /** holder: a nombre de quién está el voucher (quien lo compró o, si fue un regalo ya entregado, quien lo recibió). */
    public byte[] render(Booking booking, User holder) {
        ExperienceSession session = booking.getExperienceSession();
        Experience experience = session.getExperience();
        Order order = booking.getOrder();

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fondo y franja de arriba con la marca
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(BRAND);
        g.fillRect(0, 0, WIDTH, 96);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        g.drawString("PLAN", MARGIN, 62);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        drawRight(g, "VOUCHER · RESERVA #" + booking.getId(), 60);

        // La experiencia, cuándo y dónde
        int y = 150;
        g.setColor(INK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        for (String line : wrap(g, experience.getTitle(), WIDTH - 2 * MARGIN, 2)) {
            g.drawString(line, MARGIN, y);
            y += 42;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.setColor(INK);
        g.drawString(capitalize(session.getStartsAt().format(DAY)) + " · " + session.getStartsAt().format(HOUR)
                + (session.getEndsAt() != null ? " a " + session.getEndsAt().format(HOUR) : "") + " hs", MARGIN, y + 4);
        g.setColor(MUTED);
        g.drawString(experience.getLocation(), MARGIN, y + 38);
        String people = booking.getQuantity() + (booking.getQuantity() == 1 ? " persona" : " personas");
        g.drawString("Para " + people + " · A nombre de " + holder.getFirstName() + " " + holder.getLastName(), MARGIN, y + 72);
        y += 110;

        // Regalo: de quién y la dedicatoria
        if (order != null && order.getGiftRecipientName() != null) {
            g.setColor(BRAND);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            g.drawString("Regalo de " + order.getUser().getFirstName() + " para " + order.getGiftRecipientName(), MARGIN, y);
            if (order.getGiftMessage() != null) {
                g.setColor(MUTED);
                g.setFont(new Font(Font.SERIF, Font.ITALIC, 20));
                for (String line : wrap(g, "“" + order.getGiftMessage() + "”", WIDTH - 2 * MARGIN, 2)) {
                    y += 28;
                    g.drawString(line, MARGIN, y);
                }
            }
        }

        // El código, en un recuadro punteado abajo
        int boxY = HEIGHT - 150;
        g.setColor(BRAND_SOFT);
        g.fillRoundRect(MARGIN, boxY, WIDTH - 2 * MARGIN, 96, 24, 24);
        g.setColor(BRAND);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[] { 10, 8 }, 0));
        g.drawRoundRect(MARGIN, boxY, WIDTH - 2 * MARGIN, 96, 24, 24);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.drawString("CÓDIGO", MARGIN + 28, boxY + 36);
        g.setColor(INK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 44));
        g.drawString(booking.getVoucherCode(), MARGIN + 28, boxY + 78);

        g.setColor(MUTED);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString("Mostralo al anfitrión al llegar. Vale solo para esta fecha y no se puede transferir a otra cuenta.",
                MARGIN, HEIGHT - 24);
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void drawRight(Graphics2D g, String text, int y) {
        g.drawString(text, WIDTH - MARGIN - g.getFontMetrics().stringWidth(text), y);
    }

    // Parte un texto en renglones que entren en el ancho; si sobra, el último termina en "…"
    private List<String> wrap(Graphics2D g, String text, int width, int maxLines) {
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= width) {
                line = new StringBuilder(candidate);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        lines.add(line.toString());
        if (lines.size() > maxLines) {
            List<String> cut = new ArrayList<>(lines.subList(0, maxLines));
            String last = cut.get(maxLines - 1);
            while (!last.isEmpty() && metrics.stringWidth(last + "…") > width) {
                last = last.substring(0, last.length() - 1);
            }
            cut.set(maxLines - 1, last + "…");
            return cut;
        }
        return lines;
    }

    private String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
