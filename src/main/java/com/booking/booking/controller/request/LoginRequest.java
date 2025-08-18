package com.booking.booking.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

  @NotBlank(message = "Username cannot be empty")
  private String username;

  @NotBlank(message = "Password cannot be blank")
//  @Size(min = 8, max = 20, message = "Password must be between 8-20 characters")
//  @Pattern(
//      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
//      message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number and 1 special character"
//  )
  private String password;
}