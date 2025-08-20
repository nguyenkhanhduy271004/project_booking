package com.booking.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class BookingApplication {

  public static void main(String[] args) {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    System.out.println(passwordEncoder.encode("password"));
    SpringApplication.run(BookingApplication.class, args);
  }

}
