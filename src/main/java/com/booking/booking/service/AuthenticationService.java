package com.booking.booking.service;

import com.booking.booking.dto.request.LoginRequest;
import com.booking.booking.dto.request.RegisterRequest;
import com.booking.booking.dto.request.SignInRequest;
import com.booking.booking.dto.response.LoginResponse;
import com.booking.booking.dto.response.TokenResponse;

public interface AuthenticationService {

  LoginResponse login(LoginRequest loginRequest);

  void register(RegisterRequest registerRequest);

  TokenResponse getAccessToken(SignInRequest req);

  TokenResponse getRefreshToken(String refreshToken);

  void logout(String refreshToken);
}
