package com.booking.booking.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyAccountInfo {

  private String email;
  private String username;

}
