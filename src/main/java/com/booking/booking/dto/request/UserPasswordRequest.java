package com.booking.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPasswordRequest implements Serializable {

  private Long id;

  @NotBlank(message = "Old pass word must no be blank")
  private String oldPassword;

  @NotBlank(message = "Password must not be blank")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String password;

  @NotBlank(message = "Confirm password must not be blank")
  @Size(min = 6, message = "Confirm password must be at least 6 characters")
  private String confirmPassword;
}
