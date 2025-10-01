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
public class TopHotelResponse implements Serializable {
    private Long hotelId;
    private String hotelName;
    private Long totalBookings;
    private Double totalRevenue;
    private Double averageRating;
    private Integer occupancyRate;
}
