package com.booking.booking.dto.request;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.PaymentType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class BookingRequest {

  @NotNull(message = "Hotel ID must not be null")
  private Long hotelId;

  @NotEmpty(message = "Room IDs must not be empty")
  @Size(max = 10, message = "Cannot book more than 10 rooms at once")
  private java.util.List<Long> roomIds;

  @NotNull(message = "Check-in date must not be null")
  @Future(message = "Check-in date must be in the future")
  private LocalDate checkInDate;

  @NotNull(message = "Check-out date must not be null")
  @Future(message = "Check-out date must be in the future")
  private LocalDate checkOutDate;

  private Double totalPrice;

  @NotNull(message = "Payment type must not be null")
  private PaymentType paymentType;

  private BookingStatus status;

  @Size(max = 500, message = "Notes cannot exceed 500 characters")
  private String notes;

  private Long guestId;

  private Long voucherId;

  @AssertTrue(message = "Check-out date must be after check-in date")
  public boolean isValidDateRange() {
    return checkOutDate != null && checkInDate != null && checkOutDate.isAfter(checkInDate);
  }

  @AssertTrue(message = "Booking period cannot exceed 30 days")
  public boolean isValidBookingPeriod() {
    return checkOutDate != null && checkInDate != null &&
        ChronoUnit.DAYS.between(checkInDate, checkOutDate) <= 30;
  }

  @AssertTrue(message = "Check-in date must be at least tomorrow")
  public boolean isValidCheckInDate() {
    return checkInDate != null && checkInDate.isAfter(LocalDate.now());
  }
}
