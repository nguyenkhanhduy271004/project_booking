package com.booking.booking.controller.response;

import com.booking.booking.common.Gender;
import com.booking.booking.common.UserStatus;
import com.booking.booking.common.UserType;
import java.time.LocalDate;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {
  private Long id;
  private String username;
  private String fullName;
  private Gender gender;
  private LocalDate birthday;
  private String email;
  private String phone;
  private UserType type;
  private UserStatus status;
}
