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
public class DashboardStatisticsResponse implements Serializable {
    private Long totalUsers;
    private Long totalHotels;
    private Long totalRooms;
    private Long totalBookings;
    private Long activeBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Double totalRevenue;
    private Double monthlyRevenue;
    private Long newUsersThisMonth;
    private Long newBookingsThisMonth;
    private String scope;
}
