package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.controller.response.RoomResponse;
import com.booking.booking.dto.RoomDTO;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dashboard/rooms")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-ROOM-CONTROLLER")
@Tag(name = "Dashboard Room Controller", description = "Quản lý Room trong Dashboard - Có phân quyền")
@PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
public class DashboardRoomController {

  private final RoomService roomService;

  @Operation(summary = "Get All Rooms with Authorization", description = "Lấy danh sách room theo quyền: Admin/System Admin xem tất cả, Manager/Staff xem room thuộc hotel mình quản lý")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getAllRoomsForDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size,
      @RequestParam(defaultValue = "id", required = false) String sort,
      @RequestParam(defaultValue = "false", required = false) boolean deleted) {

    log.info("Dashboard: Getting rooms list - page: {}, size: {}, deleted: {}", page, size,
        deleted);

    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
    Page<RoomResponse> rooms = roomService.getAllRoomsWithAuthorization(pageable, deleted);

    PageResponse<?> response = PageResponse.builder()
        .pageNo(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPage(rooms.getTotalPages())
        .totalElements(rooms.getTotalElements())
        .items(rooms.getContent())
        .build();

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Room list retrieved successfully",
        response);
  }

  @Operation(summary = "Get Room by ID with Authorization", description = "Lấy thông tin room theo ID với phân quyền")
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getRoomByIdForDashboard(@PathVariable("id") @Min(0) Long id) {
    log.info("Dashboard: Getting room by ID: {}", id);

    return roomService.getRoomByIdWithAuthorization(id)
        .map(room -> new ResponseSuccess(HttpStatus.OK, "Dashboard: Room found", room))
        .orElseThrow(() -> new ResourceNotFoundException("Room not found or access denied"));
  }

  @Operation(summary = "Create Room", description = "Tạo room mới - Chỉ SYSTEM_ADMIN và ADMIN")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@authorizationUtils.canCreateHotelRoom()")
  public ResponseSuccess createRoomFromDashboard(
      @RequestPart(value = "room") @Valid RoomDTO room,
      @RequestPart(value = "images", required = false) MultipartFile[] images) {

    log.info("Dashboard: Creating new room for hotel ID: {}", room.getHotelId());

    RoomResponse created = roomService.createRoomWithHotelName(room, images);
    return new ResponseSuccess(HttpStatus.CREATED, "Dashboard: Room created successfully", created);
  }

  @Operation(summary = "Update Room", description = "Cập nhật room - SYSTEM_ADMIN, ADMIN, MANAGER")
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("@authorizationUtils.canUpdateHotelRoom()")
  public ResponseSuccess updateRoomFromDashboard(
      @PathVariable("id") @Min(0) Long id,
      @Valid @RequestPart RoomDTO room,
      @RequestPart(value = "images", required = false) MultipartFile[] images) {

    log.info("Dashboard: Updating room ID: {}", id);

    RoomResponse updated = roomService.updateRoomWithHotelName(id, room, images);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Room updated successfully", updated);
  }

  @Operation(summary = "Soft Delete Room", description = "Xóa mềm room - Chỉ SYSTEM_ADMIN")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("@authorizationUtils.canSoftDelete()")
  public ResponseSuccess deleteRoomFromDashboard(@PathVariable("id") @Min(0) Long id) {
    log.info("Dashboard: Soft deleting room ID: {}", id);

    roomService.softDeleteRoom(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Room deleted successfully");
  }

  @Operation(summary = "Soft Delete Multiple Rooms", description = "Xóa mềm nhiều room - Chỉ SYSTEM_ADMIN")
  @DeleteMapping()
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess deleteRoomsFromDashboard(@RequestBody List<Long> ids) {
    log.info("Dashboard: Soft deleting {} rooms", ids.size());

    roomService.softDeleteRooms(ids);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Rooms deleted successfully");
  }

  @Operation(summary = "Restore Room", description = "Khôi phục room đã xóa - Chỉ SYSTEM_ADMIN")
  @PutMapping("/{id}/restore")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess restoreRoomFromDashboard(@PathVariable("id") @Min(0) Long id) {
    log.info("Dashboard: Restoring room ID: {}", id);

    roomService.restoreRoom(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Room restored successfully");
  }

  @Operation(summary = "Check Room Availability", description = "Kiểm tra tình trạng phòng")
  @GetMapping("/availability")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess checkRoomAvailabilityFromDashboard(
      @RequestParam Long roomId,
      @RequestParam String checkIn,
      @RequestParam String checkOut) {

    log.info("Dashboard: Checking availability for room ID: {} from {} to {}", roomId, checkIn,
        checkOut);

    LocalDate checkInDate = LocalDate.parse(checkIn);
    LocalDate checkOutDate = LocalDate.parse(checkOut);

    boolean isAvailable = roomService.isRoomAvailable(roomId, checkInDate, checkOutDate);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Room availability checked",
        java.util.Map.of("available", isAvailable));
  }

  @Operation(summary = "Get Available Rooms", description = "Lấy danh sách phòng trống")
  @GetMapping("/available")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getAvailableRoomsFromDashboard(
      @RequestParam Long hotelId,
      @RequestParam String checkIn,
      @RequestParam String checkOut) {

    log.info("Dashboard: Getting available rooms for hotel ID: {} from {} to {}", hotelId, checkIn,
        checkOut);

    LocalDate checkInDate = LocalDate.parse(checkIn);
    LocalDate checkOutDate = LocalDate.parse(checkOut);

    List<RoomResponse> availableRooms = roomService.getAvailableRoomsWithHotelName(hotelId,
        checkInDate,
        checkOutDate);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Available rooms retrieved",
        availableRooms);
  }
}
