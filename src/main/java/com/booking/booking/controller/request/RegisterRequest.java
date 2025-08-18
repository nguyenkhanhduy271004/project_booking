package com.booking.booking.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

  @NotBlank(message = "First name cannot be blank")
  @Size(min = 2, max = 50, message = "First name must be between 2-50 characters")
  private String firstName;

  @NotBlank(message = "Last name cannot be blank")
  @Size(min = 2, max = 50, message = "Last name must be between 2-50 characters")
  private String lastName;

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 4, max = 20, message = "Username must be between 4-20 characters")
  @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
  private String username;

  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Email should be valid")
  private String email;

  @Pattern(regexp = "^\\d{10,15}$", message = "Phone must contain 10–15 digits")
  private String phoneNumber;

  @NotBlank(message = "Gender is required")
  @Pattern(
      regexp = "^(MALE|FEMALE|OTHER)$",
      message = "Gender must be one of MALE, FEMALE, OTHER"
  )
  private String gender;

  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, max = 20, message = "Password must be between 8-20 characters")
  @Pattern(
      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
      message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number and 1 special character"
  )
  private String password;

  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, max = 20, message = "Password must be between 8-20 characters")
  @Pattern(
      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
      message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number and 1 special character"
  )
  private String rePassword;
}