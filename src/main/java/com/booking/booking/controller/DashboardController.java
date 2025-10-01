package com.booking.booking.controller;

import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard Controller")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard overview", description = "API retrieve dashboard overview data")
    @GetMapping("/overview")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
    public ResponseSuccess getOverview() {
        log.info("Get dashboard overview");
        return new ResponseSuccess(HttpStatus.OK, "Dashboard overview retrieved successfully", 
                dashboardService.getOverview());
    }

    @Operation(summary = "Get dashboard statistics", description = "API retrieve dashboard statistics")
    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
    public ResponseSuccess getStatistics() {
        log.info("Get dashboard statistics");
        return new ResponseSuccess(HttpStatus.OK, "Dashboard statistics retrieved successfully", 
                dashboardService.getStatistics());
    }

    @Operation(summary = "Get booking trends", description = "API retrieve booking trends data")
    @GetMapping("/trends")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
    public ResponseSuccess getBookingTrends(@RequestParam(defaultValue = "6") int months) {
        log.info("Get booking trends for {} months", months);
        return new ResponseSuccess(HttpStatus.OK, "Booking trends retrieved successfully", 
                dashboardService.getBookingTrends(months));
    }

    @Operation(summary = "Get top hotels", description = "API retrieve top performing hotels")
    @GetMapping("/top-hotels")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
    public ResponseSuccess getTopHotels(@RequestParam(defaultValue = "5") int limit) {
        log.info("Get top {} hotels", limit);
        return new ResponseSuccess(HttpStatus.OK, "Top hotels retrieved successfully", 
                dashboardService.getTopHotels(limit));
    }

    @Operation(summary = "Get room type distribution", description = "API retrieve room type distribution")
    @GetMapping("/room-distribution")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
    public ResponseSuccess getRoomTypeDistribution() {
        log.info("Get room type distribution");
        return new ResponseSuccess(HttpStatus.OK, "Room type distribution retrieved successfully", 
                dashboardService.getRoomTypeDistribution());
    }

    @Operation(summary = "Get revenue statistics", description = "API retrieve revenue statistics")
    @GetMapping("/revenue")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
    public ResponseSuccess getRevenueStatistics(@RequestParam(defaultValue = "12") int months) {
        log.info("Get revenue statistics for {} months", months);
        return new ResponseSuccess(HttpStatus.OK, "Revenue statistics retrieved successfully", 
                dashboardService.getRevenueStatistics(months));
    }
}
