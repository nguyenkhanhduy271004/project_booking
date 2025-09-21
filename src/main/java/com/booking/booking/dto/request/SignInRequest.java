package com.booking.booking.dto.request;

import java.io.Serializable;
import lombok.Getter;

@Getter
public class SignInRequest implements Serializable {

  private String username;
  private String password;
  private String platform; // web, mobile, miniApp
  private String deviceToken;
  private String versionApp;

}
