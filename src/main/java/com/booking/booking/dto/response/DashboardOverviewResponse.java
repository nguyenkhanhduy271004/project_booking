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
public class DashboardOverviewResponse implements Serializable {
    private String userRole;
    private boolean canAccessAllData;
    private String message;
    private String currentMonth;
    private String currentYear;
}
