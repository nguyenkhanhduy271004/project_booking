package com.booking.booking.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

  @NotBlank(message = "OTP is not be blank")
  private Integer otp;
  @NotBlank(message = "Email is not be blank")
  @Email(message = "Email is not valid")
  private String email;
}
