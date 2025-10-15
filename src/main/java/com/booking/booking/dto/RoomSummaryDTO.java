package com.booking.booking.dto;

import com.booking.booking.common.TypeRoom;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoomSummaryDTO {
    private Long roomId;
    private TypeRoom typeRoom;
    private double pricePerNight;
    private int capacity;
    private List<String> imageUrls;
}
