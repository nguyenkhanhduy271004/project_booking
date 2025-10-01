package com.booking.booking.controller;

import com.booking.booking.dto.request.LoginRequest;
import com.booking.booking.dto.request.RegisterRequest;
import com.booking.booking.dto.request.SignInRequest;
import com.booking.booking.dto.response.LoginResponse;
import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.dto.response.TokenResponse;
import com.booking.booking.service.interfaces.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-CONTROLLER")
@Tag(name = "Authentication Controller")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Login", description = "Login by username and password")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess login(@Valid @RequestBody LoginRequest loginRequest) {

        log.info("Login request: {}", loginRequest);

        LoginResponse response = authenticationService.login(loginRequest);
        return new ResponseSuccess(HttpStatus.OK, "Login successfully!", response);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess register(@Valid @RequestBody RegisterRequest registerRequest) {

        log.info("Register request: {}", registerRequest);

        authenticationService.register(registerRequest);

        return new ResponseSuccess(HttpStatus.OK, "Register successfully!");
    }

    @Operation(summary = "Access token", description = "Get access token and refresh token by username and password")
    @PostMapping("/accessToken")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getAccessToken(@RequestBody SignInRequest req) {

        log.info("Access token request");

        TokenResponse response = authenticationService.getAccessToken(req);

        return new ResponseSuccess(HttpStatus.OK, "Get access token successfully!", response);
    }

    @Operation(summary = "Refresh token", description = "Get access token by refresh token")
    @PostMapping("/refreshToken")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess refreshToken(@RequestBody String refreshToken) {
        log.info("Refresh token request");

        TokenResponse response = authenticationService.getRefreshToken(refreshToken);
        return new ResponseSuccess(HttpStatus.OK, "Refresh token successfully!", response);
    }

    @PostMapping("/logout")
    public ResponseSuccess logout() {
        log.info("Logout request");

        return new ResponseSuccess(HttpStatus.OK, "Logout successfully!");
    }

}
