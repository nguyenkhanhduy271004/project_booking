package com.booking.booking.service;

import com.booking.booking.controller.request.VerifyAccountInfo;
import com.booking.booking.model.Booking;
import com.google.gson.Gson;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SERVICE")
public class EmailService {

  private final Gson gson;
  @Value("${spring.sendgrid.from-email}")
  private String from;

  @Value("${spring.sendgrid.template-id}")
  private String templateId;

  @Value("${spring.sendgrid.verification-link}")
  private String verificationLink;

  private final SendGrid sendGrid;
  private final RedisService redisService;

  public void send(String to, String subject, String body) {
    Email fromEmail = new Email(from);
    Email toEmail = new Email(to);

    Content content = new Content("text/plain", body);

    Mail mail = new Mail(fromEmail, subject, toEmail, content);

    Request request = new Request();

    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      Response response = sendGrid.api(request);

      if (response.getStatusCode() == 202) {
        log.info("Email sent successfully");
      } else {
        log.error("Email sent failed");
      }

    } catch (IOException e) {
      log.error("Error occured while sending email error: {}", e.getMessage());
    }

  }

  public void emailVerification(String to, String name) {
    Email fromEmail = new Email(from, "Booking App");
    Email toEmail = new Email(to);

    String subject = "Xác thực tài khoản";

    Map<String, String> map = new HashMap<>();

    String secretCode = UUID.randomUUID().toString();

    String secretCodeLink = String.format("?secretCode=%s", secretCode);

    redisService.setWithTTL(secretCode, name, 5);

    map.put("name", name);
    map.put("verification_link", verificationLink + secretCodeLink);

    Mail mail = new Mail();
    mail.setFrom(fromEmail);
    mail.setSubject(subject);

    Personalization personalization = new Personalization();
    personalization.addTo(toEmail);

    map.forEach(personalization::addDynamicTemplateData);

    mail.addPersonalization(personalization);
    mail.setTemplateId(templateId);

    Request request = new Request();
    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      Response response = sendGrid.api(request);

      if (response.getStatusCode() == 202) {
        log.info("Verification sent successfully");
      } else {
        log.error("Verification failed");
      }

    } catch (IOException e) {
      log.error("Error occured while Verification email error: {}", e.getMessage());
    }
  }

  @KafkaListener(topics = "verify-account-topic", groupId = "verify-account-group")
  public void emailVerificationByKafka(String message) {
    Email fromEmail = new Email(from, "Booking app");

    VerifyAccountInfo accountInfo = gson.fromJson(message, VerifyAccountInfo.class);

    Email toEmail = new Email(accountInfo.getEmail());

    String subject = "Xác thực tài khoản";

    Map<String, String> map = new HashMap<>();

    String secretCode = UUID.randomUUID().toString();

    String secretCodeLink = String.format("?secretCode=%s", secretCode);

    redisService.setWithTTL(secretCode, accountInfo.getUsername(), 5);

    map.put("name", accountInfo.getUsername());
    map.put("verification_link", verificationLink + secretCodeLink);

    Mail mail = new Mail();
    mail.setFrom(fromEmail);
    mail.setSubject(subject);

    Personalization personalization = new Personalization();
    personalization.addTo(toEmail);

    map.forEach(personalization::addDynamicTemplateData);

    mail.addPersonalization(personalization);
    mail.setTemplateId(templateId);

    Request request = new Request();
    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      Response response = sendGrid.api(request);

      if (response.getStatusCode() == 202) {
        log.info("Verification sent successfully");
      } else {
        log.error("Verification failed");
      }

    } catch (IOException e) {
      log.error("Error occured while Verification email error: {}", e.getMessage());
    }
  }

  public void sendBookingConfirmation(String to, Booking booking) {
    Email fromEmail = new Email(from, "Booking App");
    Email toEmail = new Email(to);

    String subject = "Booking Confirmation - " + booking.getBookingCode();

    StringBuilder bodyBuilder = new StringBuilder();
    bodyBuilder.append("Dear ").append(booking.getGuest().getFirstName()).append(" ")
        .append(booking.getGuest().getLastName()).append(",\n\n");
    bodyBuilder.append("Your booking has been confirmed!\n\n");
    bodyBuilder.append("Booking Details:\n");
    bodyBuilder.append("Booking Code: ").append(booking.getBookingCode()).append("\n");
    bodyBuilder.append("Hotel: ").append(booking.getHotel().getName()).append("\n");
    bodyBuilder.append("Check-in Date: ").append(booking.getCheckInDate()).append("\n");
    bodyBuilder.append("Check-out Date: ").append(booking.getCheckOutDate()).append("\n");
    bodyBuilder.append("Total Price: $").append(booking.getTotalPrice()).append("\n");
    bodyBuilder.append("Payment Type: ").append(booking.getPaymentType()).append("\n");

    if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
      bodyBuilder.append("Notes: ").append(booking.getNotes()).append("\n");
    }

    bodyBuilder.append("\nThank you for choosing our service!\n\n");
    bodyBuilder.append("Best regards,\nBooking Team");

    Content content = new Content("text/plain", bodyBuilder.toString());
    Mail mail = new Mail(fromEmail, subject, toEmail, content);

    Request request = new Request();

    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      Response response = sendGrid.api(request);

      if (response.getStatusCode() == 202) {
        log.info("Booking confirmation email sent successfully to: {}", to);
      } else {
        log.error("Failed to send booking confirmation email to: {}", to);
      }

    } catch (IOException e) {
      log.error("Error occurred while sending booking confirmation email to: {}, error: {}", to, e.getMessage());
    }
  }
}
