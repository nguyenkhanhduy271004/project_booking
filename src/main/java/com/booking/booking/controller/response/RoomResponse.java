package com.booking.booking.controller.response;

import com.booking.booking.common.TypeRoom;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {

    private Long id;

    private TypeRoom typeRoom;

    private int capacity;

    private double pricePerNight;

    private boolean available;

    private List<String> listImageUrl;

    private List<String> services;

    // Hotel information
    private Long hotelId;
    private String hotelName;

    // Audit fields - hiển thị tên đầy đủ của user
    private Date createdAt;
    private Date updatedAt;
    private String createdByUser;
    private String updatedByUser;
}