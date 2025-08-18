package com.booking.booking.controller.response;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.PaymentType;
import com.booking.booking.common.TypeRoom;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class BookingResponse {

  private Long id;
  private String bookingCode;
  private Long hotelId;
  private String hotelName;
  private List<RoomInfo> rooms;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private Double totalPrice;
  private BookingStatus status;
  private PaymentType paymentType;
  private Date createdAt;
  private Date updatedAt;
  private String notes;

  @Data
  public static class RoomInfo {
    private Long id;
    private TypeRoom typeRoom;
    private int capacity;
    private double pricePerNight;
    private boolean available;
    private List<String> imageUrls;
  }
}
