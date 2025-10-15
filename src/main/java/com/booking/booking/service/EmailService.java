package com.booking.booking.service;

import com.booking.booking.dto.request.VerifyAccountInfo;
import com.google.gson.Gson;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j(topic = "EMAIL-SERVICE")
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final RedisService redisService;
    private final Gson gson;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${spring.sendgrid.verification-link}")
    private String verificationLink;

    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Đã gửi email đơn giản đến {}", to);
    }

    public void sendVerificationEmail(String to, String username) {
        String secretCode = UUID.randomUUID().toString();
        String fullLink = verificationLink + "?secretCode=" + secretCode;

        redisService.setWithTTL(secretCode, username, Duration.ofMinutes(5));

        Context context = new Context();
        context.setVariable("name", username);
        context.setVariable("verificationLink", fullLink);

        String htmlContent = templateEngine.process("email-verification", context);

        sendHtmlEmail(to, "Xác thực tài khoản", htmlContent);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Gửi email HTML đến {} thành công", to);
        } catch (MessagingException e) {
            log.error("Gửi email HTML đến {} thất bại: {}", to, e.getMessage());
        }
    }

    @KafkaListener(topics = "verify-account-topic", groupId = "verify-account-group")
    public void emailVerificationByKafka(String message) {
        VerifyAccountInfo info = gson.fromJson(message, VerifyAccountInfo.class);
        sendVerificationEmail(info.getEmail(), info.getUsername());
    }
}
