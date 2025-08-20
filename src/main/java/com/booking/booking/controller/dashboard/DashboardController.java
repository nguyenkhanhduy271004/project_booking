package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.service.HotelService;
import com.booking.booking.service.RoomService;
import com.booking.booking.util.AuthorizationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-CONTROLLER")
@Tag(name = "Dashboard Controller", description = "Dashboard API cho quản lý hệ thống")
@PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
public class DashboardController {

  private final HotelService hotelService;
  private final RoomService roomService;
  private final AuthorizationUtils authorizationUtils;

  @Operation(summary = "Dashboard Overview", description = "Lấy thông tin tổng quan dashboard")
  @GetMapping("/overview")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getDashboardOverview() {
    log.info("Getting dashboard overview for user role: {}", getCurrentUserRole());

    Map<String, Object> overview = new HashMap<>();

    // Thông tin cơ bản cho tất cả role
    overview.put("userRole", getCurrentUserRole());
    overview.put("canAccessAllData", authorizationUtils.canAccessAllData());

    // Thống kê sẽ được implement ở các method riêng
    if (authorizationUtils.canAccessAllData()) {
      overview.put("message", "Welcome to System Dashboard - Full Access");
    } else if (authorizationUtils.isManager()) {
      overview.put("message", "Welcome to Manager Dashboard");
    } else if (authorizationUtils.isStaff()) {
      overview.put("message", "Welcome to Staff Dashboard");
    }

    return new ResponseSuccess(HttpStatus.OK, "Dashboard overview retrieved successfully",
        overview);
  }

  @Operation(summary = "Dashboard Statistics", description = "Lấy thống kê dashboard theo quyền")
  @GetMapping("/statistics")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getDashboardStatistics() {
    log.info("Getting dashboard statistics for user role: {}", getCurrentUserRole());

    Map<String, Object> statistics = new HashMap<>();

    try {
      if (authorizationUtils.canAccessAllData()) {
        // System Admin & Admin - thống kê toàn hệ thống
        statistics.put("totalHotels", "All hotels count");
        statistics.put("totalRooms", "All rooms count");
        statistics.put("totalBookings", "All bookings count");
        statistics.put("scope", "SYSTEM_WIDE");
      } else if (authorizationUtils.isManager()) {
        // Manager - thống kê hotel mình quản lý
        statistics.put("managedHotels", "Hotels managed by current user");
        statistics.put("managedRooms", "Rooms in managed hotels");
        statistics.put("hotelBookings", "Bookings in managed hotels");
        statistics.put("scope", "MANAGER_HOTELS");
      } else if (authorizationUtils.isStaff()) {
        // Staff - thống kê hotel mình làm việc
        statistics.put("workingHotels", "Hotels where staff works");
        statistics.put("workingRooms", "Rooms in working hotels");
        statistics.put("hotelBookings", "Bookings in working hotels");
        statistics.put("scope", "STAFF_HOTELS");
      }

      return new ResponseSuccess(HttpStatus.OK, "Dashboard statistics retrieved successfully",
          statistics);

    } catch (Exception e) {
      log.error("Error getting dashboard statistics: {}", e.getMessage());
      return new ResponseSuccess(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving statistics",
          null);
    }
  }

  /**
   * Lấy role hiện tại của user
   */
  private String getCurrentUserRole() {
      if (authorizationUtils.isSystemAdmin()) {
          return "SYSTEM_ADMIN";
      }
      if (authorizationUtils.isAdmin()) {
          return "ADMIN";
      }
      if (authorizationUtils.isManager()) {
          return "MANAGER";
      }
      if (authorizationUtils.isStaff()) {
          return "STAFF";
      }
    return "UNKNOWN";
  }
}
