package com.booking.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HotelWithAvailableRoomsDTO {
    private Long hotelId;
    private String name;
    private String address;
    private String imageUrl;
    private double starRating;
    private List<RoomSummaryDTO> availableRooms;
}
