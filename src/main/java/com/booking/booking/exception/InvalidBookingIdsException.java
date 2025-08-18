package com.booking.booking.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class InvalidBookingIdsException extends RuntimeException {
    private final List<Long> invalidIds;

    public InvalidBookingIdsException(String message, List<Long> invalidIds) {
        super(message);
        this.invalidIds = invalidIds;
    }
}
