package com.booking.booking.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class InvalidRoomIdsException extends RuntimeException {
    private final List<Long> invalidIds;

    public InvalidRoomIdsException(String message, List<Long> invalidIds) {
        super(message);
        this.invalidIds = invalidIds;
    }
}
