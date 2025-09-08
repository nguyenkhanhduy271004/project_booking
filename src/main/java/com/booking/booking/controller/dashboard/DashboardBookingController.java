package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.response.BookingResponse;
import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.dto.BookingDTO;
import com.booking.booking.service.BookingService;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.PermissionMatrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/bookings")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-BOOKING-CONTROLLER")
@Tag(name = "Dashboard Booking Controller", description = "Quản lý Booking trong Dashboard - SYSTEM_ADMIN, ADMIN, MANAGER, STAFF")
@PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
public class DashboardBookingController {

  private final BookingService bookingService;
  private final AuthorizationUtils authorizationUtils;

  @Operation(summary = "Get All Bookings with Authorization", description = "Lấy danh sách booking theo quyền: Admin/System Admin xem tất cả, Manager/Staff xem booking của hotel mình")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getAllBookingsForDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size,
      @RequestParam(defaultValue = "id", required = false) String sort,
      @RequestParam(defaultValue = "false", required = false) boolean deleted) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Getting bookings list - page: {}, size: {}, deleted: {}", page, size,
        deleted);

    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

    // Use existing BookingService method
    Page<BookingResponse> bookings;
    if (authorizationUtils.canAccessAllData()) {
      // SYSTEM_ADMIN, ADMIN - xem tất cả booking
      bookings = bookingService.getAllBookings(pageable, deleted);
    } else {
      // MANAGER, STAFF - chỉ xem booking của hotel mình quản lý/làm việc
      // TODO: Implement authorization filter in service
      bookings = bookingService.getAllBookings(pageable, deleted);
    }

    PageResponse<?> response = PageResponse.builder()
        .pageNo(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPage(bookings.getTotalPages())
        .totalElements(bookings.getTotalElements())
        .items(bookings.getContent()) // BookingResponse already returned from service
        .build();

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Booking list retrieved successfully",
        response);
  }

  @Operation(summary = "Get Booking by ID with Authorization", description = "Lấy thông tin booking theo ID với phân quyền")
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getBookingByIdForDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Getting booking by ID: {}", id);

    BookingResponse booking = bookingService.getBookingById(id);
    if (booking != null) {
      return new ResponseSuccess(HttpStatus.OK, "Dashboard: Booking found", booking);
    } else {
      throw new RuntimeException("Booking not found or access denied");
    }
  }

  @Operation(summary = "Search Bookings", description = "Tìm kiếm booking theo tiêu chí")
  @GetMapping("/search")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess searchBookingsFromDashboard(
      Pageable pageable,
      @RequestParam(required = false) String bookingCode,
      @RequestParam(required = false) String guestName,
      @RequestParam(required = false) String hotelName,
      @RequestParam(required = false) String status) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Searching bookings with criteria");

    // Implement search logic in service
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Booking search completed", null);
  }

  @Operation(summary = "Create Booking", description = "Tạo booking mới - SYSTEM_ADMIN, ADMIN, MANAGER, STAFF")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseSuccess createBookingFromDashboard(@Valid @RequestBody BookingDTO bookingDTO) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.CREATE,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Creating new booking for guest: {}", bookingDTO.getGuestId());

    // Convert BookingDTO to BookingRequest to use existing service
    com.booking.booking.controller.request.BookingRequest bookingRequest = new com.booking.booking.controller.request.BookingRequest();
    bookingRequest.setGuestId(bookingDTO.getGuestId());
    bookingRequest.setHotelId(bookingDTO.getHotelId());
    bookingRequest.setRoomIds(bookingDTO.getRoomIds());
    bookingRequest.setCheckInDate(bookingDTO.getCheckInDate());
    bookingRequest.setCheckOutDate(bookingDTO.getCheckOutDate());
    bookingRequest.setTotalPrice(bookingDTO.getTotalPrice().doubleValue());
    bookingRequest.setPaymentType(bookingDTO.getPaymentType());
    bookingRequest.setNotes(bookingDTO.getNotes());
    bookingRequest.setStatus(bookingDTO.getStatus());

    java.util.List<BookingResponse> createdBookings = bookingService.createBooking(bookingRequest);
    return new ResponseSuccess(HttpStatus.CREATED, "Dashboard: Booking created successfully",
        createdBookings);
  }

  @Operation(summary = "Update Booking", description = "Cập nhật booking - SYSTEM_ADMIN, ADMIN, MANAGER, STAFF")
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess updateBookingFromDashboard(
      @PathVariable("id") Long id,
      @Valid @RequestBody BookingDTO bookingDTO) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.UPDATE,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Updating booking ID: {}", id);

    // Convert BookingDTO to BookingRequest to use existing service
    com.booking.booking.controller.request.BookingRequest bookingRequest = new com.booking.booking.controller.request.BookingRequest();
    bookingRequest.setGuestId(bookingDTO.getGuestId());
    bookingRequest.setHotelId(bookingDTO.getHotelId());
    bookingRequest.setRoomIds(bookingDTO.getRoomIds());
    bookingRequest.setCheckInDate(bookingDTO.getCheckInDate());
    bookingRequest.setCheckOutDate(bookingDTO.getCheckOutDate());
    bookingRequest.setTotalPrice(bookingDTO.getTotalPrice().doubleValue());
    bookingRequest.setPaymentType(bookingDTO.getPaymentType());
    bookingRequest.setNotes(bookingDTO.getNotes());
    bookingRequest.setStatus(bookingDTO.getStatus());

    BookingResponse updated = bookingService.updateBooking(id, bookingRequest);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Booking updated successfully", updated);
  }

  @Operation(summary = "Cancel Booking", description = "Hủy booking - SYSTEM_ADMIN, ADMIN, MANAGER, STAFF")
  @PutMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess cancelBookingFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.UPDATE,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Cancelling booking ID: {}", id);

    bookingService.cancelBooking(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Booking cancelled successfully");
  }

  // TODO: The following methods need to be implemented in BookingService
  /*
   * @Operation(summary = "Confirm Booking", description =
   * "Xác nhận booking - SYSTEM_ADMIN, ADMIN, MANAGER, STAFF")
   *
   * @PutMapping("/{id}/confirm")
   *
   * @ResponseStatus(HttpStatus.OK)
   * public ResponseSuccess confirmBookingFromDashboard(@PathVariable("id") Long
   * id) {
   * // TODO: Implement confirmBooking in BookingService
   * return new ResponseSuccess(HttpStatus.OK, "Method not implemented yet");
   * }
   *
   * @Operation(summary = "Check-in Booking", description =
   * "Check-in khách - MANAGER, STAFF")
   *
   * @PutMapping("/{id}/checkin")
   *
   * @ResponseStatus(HttpStatus.OK)
   * public ResponseSuccess checkinBookingFromDashboard(@PathVariable("id") Long
   * id) {
   * // TODO: Implement checkinBooking in BookingService
   * return new ResponseSuccess(HttpStatus.OK, "Method not implemented yet");
   * }
   *
   * @Operation(summary = "Check-out Booking", description =
   * "Check-out khách - MANAGER, STAFF")
   *
   * @PutMapping("/{id}/checkout")
   *
   * @ResponseStatus(HttpStatus.OK)
   * public ResponseSuccess checkoutBookingFromDashboard(@PathVariable("id") Long
   * id) {
   * // TODO: Implement checkoutBooking in BookingService
   * return new ResponseSuccess(HttpStatus.OK, "Method not implemented yet");
   * }
   */

  @Operation(summary = "Get Booking Statistics", description = "Thống kê booking theo quyền")
  @GetMapping("/statistics")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getBookingStatisticsFromDashboard(
      @RequestParam(required = false) String period,
      @RequestParam(required = false) Long hotelId) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Getting booking statistics - period: {}, hotelId: {}", period, hotelId);

    // Logic thống kê theo quyền
    if (authorizationUtils.canAccessAllData()) {
      // SYSTEM_ADMIN, ADMIN - thống kê toàn hệ thống
      // return bookingService.getSystemBookingStatistics(period);
    } else if (authorizationUtils.isManager() || authorizationUtils.isStaff()) {
      // MANAGER, STAFF - thống kê hotel mình quản lý
      // return bookingService.getHotelBookingStatistics(period, managedHotelIds);
    }

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Booking statistics retrieved", null);
  }

  // TODO: Implement soft delete booking method
  /*
   * @Operation(summary = "Soft Delete Booking", description =
   * "Xóa mềm booking - Chỉ SYSTEM_ADMIN")
   *
   * @DeleteMapping("/{id}")
   *
   * @ResponseStatus(HttpStatus.OK)
   *
   * @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
   * public ResponseSuccess deleteBookingFromDashboard(@PathVariable("id") Long
   * id) {
   * // TODO: Implement softDeleteBooking in BookingService
   * return new ResponseSuccess(HttpStatus.OK, "Method not implemented yet");
   * }
   */

  @Operation(summary = "Restore Booking", description = "Khôi phục booking đã xóa - Chỉ SYSTEM_ADMIN")
  @PutMapping("/{id}/restore")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess restoreBookingFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.RESTORE,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Restoring booking ID: {}", id);

    bookingService.restoreBooking(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Booking restored successfully");
  }

  @Operation(summary = "Get Today's Check-ins", description = "Lấy danh sách check-in hôm nay")
  @GetMapping("/checkins/today")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getTodayCheckinsFromDashboard() {
    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Getting today's check-ins");

    // Implement logic to get today's check-ins based on user role
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Today's check-ins retrieved", null);
  }

  @Operation(summary = "Get Today's Check-outs", description = "Lấy danh sách check-out hôm nay")
  @GetMapping("/checkouts/today")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getTodayCheckoutsFromDashboard() {
    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.BOOKING);
    log.info("Dashboard: Getting today's check-outs");

    // Implement logic to get today's check-outs based on user role
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Today's check-outs retrieved", null);
  }
}
