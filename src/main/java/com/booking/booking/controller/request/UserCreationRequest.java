package com.booking.booking.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class UserCreationRequest implements Serializable {

  @NotBlank(message = "First name is required")
  private String firstName;

  @NotBlank(message = "Last name is required")
  private String lastName;

  @NotBlank(message = "Gender is required")
  @Pattern(
      regexp = "^(MALE|FEMALE|OTHER)$",
      message = "Gender must be one of MALE, FEMALE, OTHER"
  )
  private String gender;

  @NotBlank(message = "Birthday is required")
  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Birthday must be in format yyyy-MM-dd")
  private String birthday;

  @NotBlank(message = "Username is required")
  private String username;

  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, max = 20, message = "Password must be between 8-20 characters")
  @Pattern(
      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
      message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number and 1 special character"
  )
  private String password;

  @Email(message = "Invalid email format")
  private String email;

  @Pattern(regexp = "^\\d{10,15}$", message = "Phone must contain 10–15 digits")
  private String phone;

  @NotBlank(message = "Type is required")
  @Pattern(regexp = "^(ADMIN|MANAGER|STAFF|GUEST)$", message = "Type must be one of ADMIN, MANAGER, STAFF, GUEST")
  private String type;
}
