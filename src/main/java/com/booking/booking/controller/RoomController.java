package com.booking.booking.controller;

import com.booking.booking.dto.RoomDTO;
import com.booking.booking.dto.response.BookingResponse;
import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.dto.response.RoomResponse;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.service.BookingService;
import com.booking.booking.service.RoomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j(topic = "ROOM-CONTROLLER")
@Tag(name = "Room Controller")
public class RoomController {

    private final RoomService roomService;
    private final BookingService bookingService;


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getAllRooms(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "id", required = false) String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<RoomResponse> rooms = roomService.getAllRoomsWithHotelName(pageable,
                false);

        PageResponse<?> response = PageResponse.builder()
                .pageNo(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalPage(rooms.getTotalPages())
                .totalElements(rooms.getTotalElements())
                .items(rooms.getContent())
                .build();
        return new ResponseSuccess(HttpStatus.OK, "Fetched room list successfully", response);
    }


    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getRoomById(@PathVariable("id") @Min(0) Long id) {
        return roomService.getRoomByIdWithHotelName(id)
                .map(room -> new ResponseSuccess(HttpStatus.OK, "Room found", room))
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseSuccess createRoom(
            @RequestPart(value = "room") @Valid RoomDTO room,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        RoomResponse created = roomService.createRoomWithHotelName(room, images);
        return new ResponseSuccess(HttpStatus.CREATED, "Room created successfully", created);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseSuccess updateRoom(
            @PathVariable("id") @Min(0) Long id,
            @Valid @RequestPart RoomDTO room,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        RoomResponse updated = roomService.updateRoomWithHotelName(id, room, images);
        return new ResponseSuccess(HttpStatus.OK, "Room updated successfully", updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteRoom(@PathVariable("id") @Min(0) Long id) {
        roomService.softDeleteRoom(id);
        return new ResponseSuccess(HttpStatus.OK, "Room deleted successfully");
    }

    @DeleteMapping("/ids")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteRooms(@RequestBody List<Long> ids) {
        roomService.softDeleteRooms(ids);
        return new ResponseSuccess(HttpStatus.OK, "Rooms deleted successfully");
    }

    @PutMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess restoreRoom(@PathVariable("id") @Min(0) Long id) {
        roomService.restoreRoom(id);
        return new ResponseSuccess(HttpStatus.OK, "Room restored successfully");
    }

    @PutMapping("/restore")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess restoreRooms(@RequestBody List<Long> ids) {
        roomService.restoreRooms(ids);
        return new ResponseSuccess(HttpStatus.OK, "Rooms restored successfully");
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteRoomPermanently(@PathVariable("id") @Min(0) Long id) {
        roomService.deleteRoomPermanently(id);
        return new ResponseSuccess(HttpStatus.OK, "Room permanently deleted successfully");
    }

    @DeleteMapping("/permanent")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteRoomsPermanently(@RequestBody List<Long> ids) {
        roomService.deleteRoomsPermanently(ids);
        return new ResponseSuccess(HttpStatus.OK, "Rooms permanently deleted successfully");
    }

    @GetMapping("/availability")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess checkRoomAvailability(
            @RequestParam Long roomId,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);

        boolean isAvailable = roomService.isRoomAvailable(roomId, checkInDate, checkOutDate);
        return new ResponseSuccess(HttpStatus.OK, "Room availability checked",
                java.util.Map.of("available", isAvailable));
    }


    @GetMapping("/available")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getAvailableRooms(
            @RequestParam Long hotelId,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);

        List<RoomResponse> availableRooms = roomService.getAvailableRoomsWithHotelName(hotelId,
                checkInDate, checkOutDate);
        return new ResponseSuccess(HttpStatus.OK, "Available rooms retrieved", availableRooms);
    }

    @GetMapping("/{id}/unavailable-dates")
    @ResponseStatus(HttpStatus.OK)
    public ResponseSuccess getUnavailableDates(
            @PathVariable Long id,
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        List<LocalDate> unavailableDates = roomService.getUnavailableDates(id, fromDate, toDate);
        return new ResponseSuccess(HttpStatus.OK, "Unavailable dates retrieved", unavailableDates);
    }

    @GetMapping("/{roomId}/bookings")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER','STAFF')")
    public ResponseSuccess getRoomBookings(
            @PathVariable Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Page<BookingResponse> bookings = bookingService.getHistoryBookingByRoomId(roomId, from, to,
                page, size);

        PageResponse<?> response = PageResponse.builder()
                .pageNo(page)
                .pageSize(size)
                .totalPage(bookings.getTotalPages())
                .totalElements(bookings.getTotalElements())
                .items(bookings.getContent())
                .build();
        return new ResponseSuccess(HttpStatus.OK, "Get history booking of room successfully",
                response);
    }
}
