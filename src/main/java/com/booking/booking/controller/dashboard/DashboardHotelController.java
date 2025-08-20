package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.dto.HotelDTO;
import com.booking.booking.mapper.HotelMapper;
import com.booking.booking.model.Hotel;
import com.booking.booking.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/dashboard/hotels")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-HOTEL-CONTROLLER")
@Tag(name = "Dashboard Hotel Controller", description = "Quản lý Hotel trong Dashboard - Có phân quyền")
@PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
public class DashboardHotelController {

  private final HotelService hotelService;
  private final HotelMapper hotelMapper;

  @Operation(summary = "Get All Hotels with Authorization", description = "Lấy danh sách hotel theo quyền: Admin/System Admin xem tất cả, Manager/Staff xem hotel mình quản lý")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getAllHotelsForDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size,
      @RequestParam(defaultValue = "id", required = false) String sort,
      @RequestParam(defaultValue = "false", required = false) boolean deleted) {

    log.info("Dashboard: Getting hotels list - page: {}, size: {}, deleted: {}", page, size,
        deleted);

    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
    Page<HotelDTO> hotelPage = hotelService.getAllHotelsWithAuthorization(pageable, deleted)
        .map(hotelMapper::toDTO);

    PageResponse<?> response = PageResponse.builder()
        .pageNo(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPage(hotelPage.getTotalPages())
        .totalElements(hotelPage.getTotalElements())
        .items(hotelPage.getContent())
        .build();

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel list retrieved successfully",
        response);
  }

  @Operation(summary = "Search Hotels", description = "Tìm kiếm hotel - SYSTEM_ADMIN, ADMIN, MANAGER")
  @GetMapping("/search")
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
  public ResponseSuccess searchHotelsFromDashboard(
      Pageable pageable,
      @RequestParam(required = false) String[] hotel) {

    log.info("Dashboard: Searching hotels with criteria");

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel search completed",
        hotelService.advanceSearchWithSpecification(pageable, hotel));
  }

  @Operation(summary = "Get Hotel by ID with Authorization", description = "Lấy thông tin hotel theo ID với phân quyền")
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getHotelByIdForDashboard(@PathVariable("id") Long id) {
    log.info("Dashboard: Getting hotel by ID: {}", id);

    return hotelService.getHotelByIdWithAuthorization(id)
        .map(hotelMapper::toDTO)
        .map(dto -> new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel found", dto))
        .orElseThrow(() -> new RuntimeException("Hotel not found or access denied"));
  }

  @Operation(summary = "Get Hotel Rooms", description = "Lấy danh sách phòng của hotel")
  @GetMapping("/{id}/rooms")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getHotelRoomsFromDashboard(@PathVariable("id") Long id) {
    log.info("Dashboard: Getting rooms for hotel ID: {}", id);

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel rooms retrieved successfully",
        hotelService.getRoomByHotelId(id));
  }

  @Operation(summary = "Create Hotel", description = "Tạo hotel mới - Chỉ SYSTEM_ADMIN và ADMIN")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@authorizationUtils.canCreateHotelRoom()")
  public ResponseSuccess createHotelFromDashboard(
      @RequestPart(value = "hotel") @Valid HotelDTO hotelDTO,
      @RequestPart(value = "image", required = false) MultipartFile imageHotel) {

    log.info("Dashboard: Creating new hotel: {}", hotelDTO.getName());

    Hotel created = hotelService.createHotel(hotelDTO, imageHotel);
    return new ResponseSuccess(HttpStatus.CREATED, "Dashboard: Hotel created successfully",
        hotelMapper.toDTO(created));
  }

  @Operation(summary = "Update Hotel", description = "Cập nhật hotel - SYSTEM_ADMIN, ADMIN, MANAGER")
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
  public ResponseSuccess updateHotelFromDashboard(
      @PathVariable("id") Long id,
      @RequestPart(value = "hotel") @Valid HotelDTO hotelDTO,
      @RequestPart(value = "image", required = false) MultipartFile imageHotel) {

    log.info("Dashboard: Updating hotel ID: {}", id);

    Hotel updated = hotelService.updateHotel(id, hotelDTO, imageHotel);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel updated successfully",
        hotelMapper.toDTO(updated));
  }

  @Operation(summary = "Soft Delete Hotel", description = "Xóa mềm hotel - Chỉ SYSTEM_ADMIN")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess deleteHotelFromDashboard(@PathVariable("id") Long id) {
    log.info("Dashboard: Soft deleting hotel ID: {}", id);

    hotelService.softDeleteHotel(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel deleted successfully");
  }

  @Operation(summary = "Soft Delete Multiple Hotels", description = "Xóa mềm nhiều hotel - Chỉ SYSTEM_ADMIN")
  @DeleteMapping
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess deleteHotelsFromDashboard(@RequestBody List<Long> ids) {
    log.info("Dashboard: Soft deleting {} hotels", ids.size());

    hotelService.softDeleteHotels(ids);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotels deleted successfully");
  }

  @Operation(summary = "Restore Hotel", description = "Khôi phục hotel đã xóa - Chỉ SYSTEM_ADMIN")
  @PutMapping("/{id}/restore")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess restoreHotelFromDashboard(@PathVariable("id") Long id) {
    log.info("Dashboard: Restoring hotel ID: {}", id);

    hotelService.restoreHotel(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel restored successfully");
  }

  @Operation(summary = "Restore Multiple Hotels", description = "Khôi phục nhiều hotel đã xóa - Chỉ SYSTEM_ADMIN")
  @PutMapping("/restore")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess restoreHotelsFromDashboard(@RequestBody List<Long> ids) {
    log.info("Dashboard: Restoring {} hotels", ids.size());

    hotelService.restoreHotels(ids);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotels restored successfully");
  }

  @Operation(summary = "Permanently Delete Hotel", description = "Xóa vĩnh viễn hotel - Chỉ SYSTEM_ADMIN")
  @DeleteMapping("/{id}/permanent")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess deleteHotelPermanentlyFromDashboard(@PathVariable("id") Long id) {
    log.info("Dashboard: Permanently deleting hotel ID: {}", id);

    hotelService.deleteHotelPermanently(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotel permanently deleted successfully");
  }

  @Operation(summary = "Permanently Delete Multiple Hotels", description = "Xóa vĩnh viễn nhiều hotel - Chỉ SYSTEM_ADMIN")
  @DeleteMapping("/permanent")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN')")
  public ResponseSuccess deleteHotelsPermanentlyFromDashboard(@RequestBody List<Long> ids) {
    log.info("Dashboard: Permanently deleting {} hotels", ids.size());

    hotelService.deleteHotelsPermanently(ids);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Hotels permanently deleted successfully");
  }
}
