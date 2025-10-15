package com.booking.booking.exception;

import com.booking.booking.dto.response.ResponseFailure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private Map<String, Object> buildDetails(Exception ex, WebRequest request, String defaultError) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("timestamp", LocalDateTime.now());
        details.put("error", defaultError);
        details.put("exception", ex.getClass().getName());
        details.put("message", ex.getMessage());
        details.put("path", request.getDescription(false).replace("uri=", ""));
        return details;
    }

    @ExceptionHandler(InvalidUserIdsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleInvalidUserIdsException(InvalidUserIdsException ex) {

        log.warn("User ids exception: ", ex);
        return new ResponseFailure(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidBookingIdsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleInvalidBookingIdsException(InvalidBookingIdsException ex) {

        log.warn("Booking ids exception: ", ex);
        return new ResponseFailure(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidHotelIdsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleInvalidHotelIdsException(InvalidHotelIdsException ex) {

        log.warn("Hotel ids exception: ", ex);
        return new ResponseFailure(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidRoomIdsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleInvalidRoomIdsException(InvalidRoomIdsException ex) {
        log.warn("Invalid Room Ids", ex);
        return new ResponseFailure(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseFailure handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseFailure(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UsernameIsExistException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleUsernameIsExistException(UsernameIsExistException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseFailure(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleBadRequest(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON: {}", ex.getMessage());
        return new ResponseFailure(HttpStatus.BAD_REQUEST, "Malformed or invalid request body");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseFailure(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseFailure handleDuplicateResource(DuplicateResourceException ex, WebRequest request) {
        return new ResponseFailure(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PasswordNotMatchException.class)
    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    public ResponseFailure handlePasswordNotMatchException(PasswordNotMatchException ex, WebRequest request) {
        return new ResponseFailure(HttpStatus.EXPECTATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleBadRequestException(BadRequestException ex, WebRequest request) {
        return new ResponseFailure(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseFailure handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String msg = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
            errors.put(field, msg);
        });

        log.error("Validation error: {}", errors);

        Map<String, Object> details = buildDetails(ex, request, "Validation Failed");
        details.put("errors", errors);

        return new ResponseFailure(HttpStatus.BAD_REQUEST, "Invalid input data", errors);
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseFailure handleJwtAuthenticationException(JwtAuthenticationException ex) {
        return new ResponseFailure(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(LoginFailedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseFailure handleLoginFailedException(LoginFailedException ex) {
        return new ResponseFailure(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseFailure handleAccessDenied(AccessDeniedException ex) {
        return new ResponseFailure(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseFailure handleAuthorizationDenied(AuthorizationDeniedException ex, WebRequest request) {
        return new ResponseFailure(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseFailure handleInternalAuthenticationServiceException(InternalAuthenticationServiceException ex,
                                                                        WebRequest request) {
        log.error(ex.getMessage());
        return new ResponseFailure(HttpStatus.UNAUTHORIZED, "Login failed!");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseFailure handleAllUncaughtExceptions(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseFailure(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error",
                buildDetails(ex, request, "Internal Server Error"));
    }
}
