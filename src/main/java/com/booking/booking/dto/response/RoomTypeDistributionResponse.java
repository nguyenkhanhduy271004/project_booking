package com.booking.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeDistributionResponse implements Serializable {
    private String roomType;
    private Long count;
    private Double percentage;
    private String color;
}
