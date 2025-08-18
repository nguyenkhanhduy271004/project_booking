package com.booking.booking.exception;

public class UsernameIsExistException extends RuntimeException {
    public UsernameIsExistException(String message) {
        super(message);
    }
}
