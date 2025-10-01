package com.booking.booking.service.impl;

import com.booking.booking.dto.request.LoginRequest;
import com.booking.booking.dto.request.RegisterRequest;
import com.booking.booking.dto.request.SignInRequest;
import com.booking.booking.dto.request.VerifyAccountInfo;
import com.booking.booking.dto.response.LoginResponse;
import com.booking.booking.dto.response.TokenResponse;
import com.booking.booking.exception.*;
import com.booking.booking.mapper.UserMapper;
import com.booking.booking.model.User;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.service.interfaces.AuthenticationService;
import com.booking.booking.service.EmailService;
import com.booking.booking.service.interfaces.JwtService;
import com.google.gson.Gson;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.booking.booking.common.TokenType.REFRESH_TOKEN;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class AuthenticationServiceImpl implements AuthenticationService {

    private final Gson gson;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Login request: {}", loginRequest);

        List<String> authorities = new ArrayList<>();
        try {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                            loginRequest.getPassword()));

            log.info("isAuthenticated = {}", authenticate.isAuthenticated());
            log.info("Authorities: {}", authenticate.getAuthorities().toString());
            authorities.add(authenticate.getAuthorities().toString());

            SecurityContextHolder.getContext().setAuthentication(authenticate);
        } catch (BadCredentialsException | DisabledException e) {
            log.error("errorMessage: {}", e.getMessage());
            throw new AccessDeniedException(e.getMessage());
        }

        String accessToken = jwtService.generateAccessToken(loginRequest.getUsername(), authorities);
        String refreshToken = jwtService.generateRefreshToken(loginRequest.getUsername(), authorities);

        User user = userRepository.findByUsernameAndIsDeletedFalse(loginRequest.getUsername());

        return LoginResponse.builder()
                .fullName(user.getFirstName() + " " + user.getLastName())
                .userType(user.getType().toString())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }

    @Override
    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (!registerRequest.getPassword().equals(registerRequest.getRePassword())) {
            throw new PasswordNotMatchException("Passwords do not match");
        }

        User user = userRepository.findByUsernameAndIsDeletedFalse(registerRequest.getUsername());

        if (user != null) {
            throw new UsernameIsExistException("Username already exists");
        }

        User savedUser = userRepository.save(userMapper.toUserEntity(registerRequest));

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getUsername());

        VerifyAccountInfo info = new VerifyAccountInfo(savedUser.getEmail(), savedUser.getUsername());
        String message = gson.toJson(info);

        kafkaTemplate.send("verify-account-topic", message);
    }

    @Override
    public TokenResponse getAccessToken(SignInRequest request) {
        log.info("Get access token");

        List<String> authorities = new ArrayList<>();
        try {
            // Thực hiện xác thực với username và password
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            log.info("isAuthenticated = {}", authenticate.isAuthenticated());
            log.info("Authorities: {}", authenticate.getAuthorities().toString());
            authorities.add(authenticate.getAuthorities().toString());

            SecurityContextHolder.getContext().setAuthentication(authenticate);
        } catch (BadCredentialsException | DisabledException e) {
            log.error("errorMessage: {}", e.getMessage());
            throw new AccessDeniedException(e.getMessage());
        }

        String accessToken = jwtService.generateAccessToken(request.getUsername(), authorities);
        String refreshToken = jwtService.generateRefreshToken(request.getUsername(), authorities);

        return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

    @Override
    public TokenResponse getRefreshToken(String refreshToken) {
        log.info("Get refresh token");

        if (!StringUtils.hasLength(refreshToken)) {
            throw new InvalidDataException("Token must be not blank");
        }

        try {
            // Verify token
            String userName = jwtService.extractUsername(refreshToken, REFRESH_TOKEN);

            // check user is active or inactivated
            User user = userRepository.findByUsernameAndIsDeletedFalse(userName);

            List<String> authorities = new ArrayList<>();
            user.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));

            // generate new access token
            String accessToken = jwtService.generateAccessToken(user.getUsername(), authorities);

            return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
        } catch (Exception e) {
            log.error("Access denied! errorMessage: {}", e.getMessage());
            throw new ForBiddenException(e.getMessage());
        }
    }

    @Override
    public void logout(String refreshToken) {


    }
}
