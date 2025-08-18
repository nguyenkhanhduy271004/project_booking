package com.booking.booking.exception;

import java.util.List;

public class InvalidUserIdsException extends RuntimeException {
  private final List<Long> invalidIds;

  public InvalidUserIdsException(String message, List<Long> invalidIds) {
    super(message);
    this.invalidIds = invalidIds;
  }

  public List<Long> getInvalidIds() {
    return invalidIds;
  }
}
