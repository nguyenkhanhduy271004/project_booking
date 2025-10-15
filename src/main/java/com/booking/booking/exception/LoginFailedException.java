package com.booking.booking.exception;

public class LoginFailedException extends RuntimeException {
    public LoginFailedException(String message) {
        super(message);
    }
}
