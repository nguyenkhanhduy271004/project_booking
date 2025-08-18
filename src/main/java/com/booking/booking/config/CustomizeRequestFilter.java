package com.booking.booking.config;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.booking.booking.common.TokenType;
import com.booking.booking.exception.AccessDeniedException;
import com.booking.booking.service.JwtService;
import com.booking.booking.service.UserServiceDetail;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "CUSTOMIZE-FILTER")
@EnableMethodSecurity(prePostEnabled = true)
public class CustomizeRequestFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserServiceDetail userServiceDetail;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    log.info("{} {}", request.getMethod(), request.getRequestURI());

    final String authHeader = request.getHeader(AUTHORIZATION);

    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);
    try {
      String username = jwtService.extractUsername(token, TokenType.ACCESS_TOKEN);
      log.info("Username extracted from token: {}", username);

      UserDetails user = userServiceDetail.UserDetailsService().loadUserByUsername(username);
      if (user == null) {
        throw new AccessDeniedException("User not found");
      }

      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authToken);
      SecurityContextHolder.setContext(context);

    } catch (AccessDeniedException e) {
      log.warn("Access denied: {}", e.getMessage());
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(errorResponse(request.getRequestURI(), e.getMessage()));
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String errorResponse(String url, String message) {
    try {
      ErrorResponse error = new ErrorResponse();
      error.setTimestamp(new Date());
      error.setStatus(HttpServletResponse.SC_FORBIDDEN);
      error.setPath(url);
      error.setError("Forbidden");
      error.setMessage(message);

      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson(error);
    } catch (Exception e) {
      return "{\"status\":403,\"message\":\"Forbidden\"}";
    }
  }

  @Getter
  @Setter
  private static class ErrorResponse {
    private Date timestamp;
    private int status;
    private String path;
    private String error;
    private String message;
  }
}
