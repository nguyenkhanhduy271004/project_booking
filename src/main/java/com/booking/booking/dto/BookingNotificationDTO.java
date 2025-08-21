package com.booking.booking.dto;

import com.booking.booking.model.Booking;
import com.booking.booking.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingNotificationDTO {

  private String bookingCode;
  private String guestName;
  private Long hotelId;
  private List<Long> roomIds;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private BigDecimal totalPrice;

  public BookingNotificationDTO(Booking booking) {
    this.bookingCode = booking.getBookingCode();
    this.guestName = booking.getGuest().getFirstName() + " " + booking.getGuest().getLastName();
    this.hotelId = booking.getHotel().getId();
    this.roomIds = booking.getRooms().stream().map(Room::getId).collect(Collectors.toList());
    this.checkInDate = booking.getCheckInDate();
    this.checkOutDate = booking.getCheckOutDate();
    this.totalPrice = booking.getTotalPrice();
  }
}
