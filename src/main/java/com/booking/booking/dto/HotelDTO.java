package com.booking.booking.dto;

import lombok.Data;

@Data
public class HotelDTO {
    private Long id;
    private String name;
    private String district;
    private String addressDetail;
    private int totalRooms;
    private double starRating;
    private String imageUrl;
    private Long managerId;
}
