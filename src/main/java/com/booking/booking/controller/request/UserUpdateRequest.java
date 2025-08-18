package com.booking.booking.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class UserUpdateRequest implements Serializable {

  @NotNull(message = "User ID is required")
  private Long id;

  private String firstName;

  private String lastName;

  @Pattern(
      regexp = "^(MALE|FEMALE|OTHER)$",
      message = "Gender must be one of MALE, FEMALE, OTHER"
  )
  private String gender;

  @Pattern(
      regexp = "^\\d{4}-\\d{2}-\\d{2}$",
      message = "Birthday must be in format yyyy-MM-dd"
  )
  private String birthday;

  @Email(message = "Invalid email format")
  private String email;

  @Pattern(regexp = "^\\d{10,15}$", message = "Phone must contain 10–15 digits")
  private String phone;

  @Pattern(regexp = "^(ADMIN|MANAGER|STAFF|GUEST)$", message = "Type must be one of ADMIN, MANAGER, STAFF, GUEST")
  private String type;
}
