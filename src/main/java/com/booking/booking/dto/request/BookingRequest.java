package com.booking.booking.dto.request;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.PaymentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BookingRequest {

    private Long guestId;

    private Long voucherId;

    private Double totalPrice;

    private BookingStatus status;

    @NotNull(message = "Hotel ID must not be null")
    private Long hotelId;

    @NotEmpty(message = "Room IDs must not be empty")
    @Size(max = 10, message = "Cannot book more than 10 rooms at once")
    private List<Long> roomIds;

    @NotNull(message = "Check-in date must not be null")
    @Future(message = "Check-in date must be in the future")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date must not be null")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOutDate;

    @NotNull(message = "Payment type must not be null")
    private PaymentType paymentType;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

}
