package com.uade.tpo.demo.service.impl;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.uade.tpo.demo.entity.Booking;
import com.uade.tpo.demo.entity.Order;
import com.uade.tpo.demo.entity.ExperienceSession;
import com.uade.tpo.demo.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    private final String host;
    private final String username;
    private final String password;
    private final String from;

    public EmailServiceImpl(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean starttls,
            @Value("${application.mail.from:no-reply@etrip.local}") String from) {
        this.host = host == null ? "" : host.trim();
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.from = from;
        mailSender.setHost(this.host);
        mailSender.setPort(port);
        mailSender.setUsername(this.username);
        mailSender.setPassword(this.password);
        mailSender.getJavaMailProperties().put("mail.smtp.auth", smtpAuth);
        mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", starttls);
    }

    @Override
    public void sendOrderConfirmation(Order order) {
        String recipient = order != null && order.getUser() != null ? order.getUser().getEmail() : null;
        if (host.isBlank() || recipient == null || recipient.isBlank()) {
            log.info("Email de confirmación omitido: SMTP no configurado o destinatario inexistente.");
            return;
        }

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(from);
            email.setTo(recipient);
            email.setSubject("eTrip · Reserva confirmada #" + order.getId());
            email.setText(buildBody(order));
            mailSender.send(email);
            log.info("Email de confirmación enviado para la orden {} a {}", order.getId(), recipient);
        } catch (RuntimeException error) {
            log.error("No se pudo enviar el email de confirmación de la orden {}: {}",
                    order != null ? order.getId() : null, error.getMessage(), error);
        }
    }

    private String buildBody(Order order) {
        StringBuilder body = new StringBuilder()
                .append("¡Tu reserva en eTrip está confirmada!\n\n")
                .append("Orden: #").append(order.getId()).append('\n')
                .append("Total: $").append(order.getTotal()).append("\n\n")
                .append("Tus vouchers:\n");

        if (order.getBookings() != null) {
            for (Booking booking : order.getBookings()) {
                ExperienceSession session = booking.getExperienceSession();
                String title = session != null && session.getExperience() != null
                        ? session.getExperience().getTitle()
                        : "Experiencia";
                body.append("- ").append(title)
                        .append(" · voucher ").append(booking.getVoucherCode());
                if (session != null && session.getStartsAt() != null) {
                    body.append(" · ").append(session.getStartsAt().format(DATE_TIME));
                }
                body.append(" · ").append(booking.getQuantity()).append(" lugar(es)\n");
            }
        }

        return body.append("\nTambién podés consultar tus reservas desde tu cuenta.")
                .toString();
    }
}
