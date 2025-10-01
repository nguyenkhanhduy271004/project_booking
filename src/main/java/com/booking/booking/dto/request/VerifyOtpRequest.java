package com.booking.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

  @NotBlank(message = "OTP is not be blank")
  private Integer otp;
  @NotBlank(message = "Username is not be blank")
  @Email(message = "Username is not valid")
  private String username;
}
