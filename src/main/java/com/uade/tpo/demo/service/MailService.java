package com.uade.tpo.demo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manda los mails de PLAN con Gmail (configurado en application.properties + .env).
 * Si no hay cuenta configurada (MAIL_USERNAME vacío), el código se escribe en la consola del back,
 * así el proyecto se puede levantar y probar sin la contraseña del mail.
 * Si Gmail rechaza el envío, JavaMailSender tira una MailException (ver GlobalExceptionHandler).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    // "sábado 22 de noviembre, 09:00 hs"
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm 'hs'", Locale.forLanguageTag("es-AR"));

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${app.front-url:http://localhost:5173}")
    private String frontUrl;

    public void sendVerificationCode(User user, String code, int minutes) {
        send(user, "Tu código para verificar tu cuenta en PLAN: " + code,
                "Hola " + user.getFirstName() + ":\n\n"
                        + "Gracias por sumarte a PLAN. Para confirmar que este email es tuyo, ingresá este código:\n\n"
                        + "    " + code + "\n\n"
                        + "Vence en " + minutes + " minutos.\n\n"
                        + "Si no creaste una cuenta en PLAN, ignorá este mensaje.\n\n"
                        + "El equipo de PLAN");
    }

    public void sendPasswordResetCode(User user, String code, int minutes) {
        send(user, "Tu código para recuperar la contraseña de PLAN: " + code,
                "Hola " + user.getFirstName() + ":\n\n"
                        + "Pediste cambiar la contraseña de tu cuenta de PLAN. Ingresá este código:\n\n"
                        + "    " + code + "\n\n"
                        + "Vence en " + minutes + " minutos.\n\n"
                        + "Si no fuiste vos, ignorá este mensaje: tu contraseña no cambia.\n\n"
                        + "El equipo de PLAN");
    }

    /**
     * El regalo: le avisa a quien lo recibe qué le regalaron (experiencia, fecha, lugar y personas) con la dedicatoria.
     * El voucher NO va en el mail: lo ve en PLAN cuando el regalo es suyo. Así, si el email estaba mal escrito,
     * quien lo recibe por error no puede usarlo.
     * delivered: ya tiene cuenta verificada con ese email y el regalo está en sus reservas; si no, se lo pasamos
     * cuando entre a PLAN con ese email y lo verifique.
     */
    public void sendGift(String toEmail, String recipientName, User buyer, String dedication, List<Booking> bookings,
            boolean delivered) {
        StringBuilder text = new StringBuilder();
        text.append("¡Hola ").append(recipientName).append("!\n\n")
                .append(buyer.getFirstName()).append(" ").append(buyer.getLastName())
                .append(" te regaló ").append(bookings.size() == 1 ? "una experiencia" : "experiencias").append(" en PLAN:\n");
        for (Booking booking : bookings) {
            ExperienceSession session = booking.getExperienceSession();
            text.append("\n  ").append(session.getExperience().getTitle()).append("\n")
                    .append("  ").append(capitalize(session.getStartsAt().format(WHEN))).append("\n")
                    .append("  ").append(session.getExperience().getLocation()).append("\n")
                    .append("  Para ").append(booking.getQuantity()).append(booking.getQuantity() == 1 ? " persona" : " personas")
                    .append("\n");
        }
        if (dedication != null) {
            text.append("\n\"").append(dedication).append("\"\n— ").append(buyer.getFirstName()).append("\n");
        }
        text.append("\n").append(delivered
                ? "Ya está en tus reservas: entrá a " + frontUrl + "/mis-reservas con este email para ver el voucher."
                : "Para recibirlo, creá tu cuenta en " + frontUrl + " con este email y verificalo (o entrá, si ya tenés una). "
                        + "En ese momento el regalo pasa a ser tuyo y el voucher te aparece en \"Mis reservas\".")
                .append("\n\n¡Que lo disfrutes!\nEl equipo de PLAN");
        send(toEmail, "🎁 " + buyer.getFirstName() + " te regaló una experiencia en PLAN", text.toString());
    }

    private String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void send(User user, String subject, String text) {
        send(user.getEmail(), subject, text);
    }

    private void send(String to, String subject, String text) {
        if (from == null || from.isBlank()) {
            log.info("\n==============================================\n"
                    + " Mail sin enviar (no hay MAIL_USERNAME en .env)\n"
                    + " Para: {}\n Asunto: {}\n"
                    + "==============================================", to, subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("PLAN <" + from + ">");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
