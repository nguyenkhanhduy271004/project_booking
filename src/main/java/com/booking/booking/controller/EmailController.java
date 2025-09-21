package com.booking.booking.controller;

import com.booking.booking.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-CONTROLLER")
public class EmailController {

    private final EmailService emailService;

    @GetMapping("/send-email")
    public void send(@RequestParam String to, @RequestParam String subject, @RequestParam String body) {
        log.info("Sending email to " + to);
        emailService.send(to, subject, body);

        log.info("Email sent to " + to + " successfully");
    }

    @GetMapping("/verify-email")
    public void emailVerification(@RequestParam String to, @RequestParam String name) {
        log.info("Verification email to " + to);
        emailService.emailVerification(to, name);
    }


}
