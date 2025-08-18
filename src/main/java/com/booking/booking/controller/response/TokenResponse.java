package com.booking.booking.controller.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TokenResponse {

  private String accessToken;
  private String refreshToken;
}
