package com.booking.booking.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class InvalidHotelIdsException extends RuntimeException {
    private final List<Long> invalidIds;

    public InvalidHotelIdsException(String message, List<Long> invalidIds) {
        super(message);
        this.invalidIds = invalidIds;
    }
}
