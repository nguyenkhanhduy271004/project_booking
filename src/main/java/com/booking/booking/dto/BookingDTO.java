package com.booking.booking.dto;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDTO {

    private Long id;

    @NotNull(message = "Guest ID is required")
    private Long guestId;

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @NotNull(message = "Room IDs are required")
    private List<Long> roomIds;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    @NotNull(message = "Total price is required")
    @Positive(message = "Total price must be positive")
    private BigDecimal totalPrice;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    private BookingStatus status;

    private String notes;

    private String bookingCode;

    private boolean isDeleted;
}
