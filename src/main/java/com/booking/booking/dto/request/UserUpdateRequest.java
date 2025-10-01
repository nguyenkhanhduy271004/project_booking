package com.booking.booking.dto.request;

import com.booking.booking.common.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserUpdateRequest implements Serializable {

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

  @Pattern(regexp = "^(NONE|ACTIVE|INACTIVE)$", message = "Type must be one of NONE, ACTIVE, INACTIVE")
  private String status;
}
