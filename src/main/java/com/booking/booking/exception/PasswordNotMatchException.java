package com.booking.booking.exception;

public class PasswordNotMatchException extends RuntimeException {
  public PasswordNotMatchException(String message) {
    super(message);
  }
}
