package com.booking.booking.service;

import com.booking.booking.controller.request.LoginRequest;
import com.booking.booking.controller.request.RegisterRequest;
import com.booking.booking.controller.request.SignInRequest;
import com.booking.booking.controller.response.LoginResponse;
import com.booking.booking.controller.response.TokenResponse;

public interface AuthenticationService {

  LoginResponse login(LoginRequest loginRequest);

  void register(RegisterRequest registerRequest);

  TokenResponse getAccessToken(SignInRequest req);

  TokenResponse getRefreshToken(String refreshToken);

  void logout(String refreshToken);
}
