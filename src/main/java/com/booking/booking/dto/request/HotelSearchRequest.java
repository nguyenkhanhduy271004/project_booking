package com.booking.booking.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HotelSearchRequest {

    private String keyword;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int numberOfGuests;
    private int numberOfRooms;
}
