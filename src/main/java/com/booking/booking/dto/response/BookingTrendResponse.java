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
public class BookingTrendResponse implements Serializable {
    private String month;
    private String year;
    private Long bookings;
    private Double revenue;
    private Long newUsers;
}
