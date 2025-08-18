package com.booking.booking.controller;

import com.booking.booking.controller.request.BookingRequest;
import com.booking.booking.controller.response.BookingResponse;
import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  @Operation(summary = "Create Booking", description = "API to create a new booking")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF', 'GUEST')")
  public ResponseSuccess createBooking(@Valid @RequestBody BookingRequest request) {
    List<BookingResponse> response = bookingService.createBooking(request);
    return new ResponseSuccess(HttpStatus.CREATED, "Booking created successfully", response);
  }

  @Operation(summary = "Get All Bookings", description = "API to retrieve all bookings with pagination")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
  public ResponseSuccess getAllBookings(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id", required = false) String sort,
      @RequestParam(defaultValue = "false") boolean deleted) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

    Page<BookingResponse> bookings = bookingService.getAllBookings(pageable, deleted);
    PageResponse<?> result = PageResponse.builder()
        .pageNo(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPage(bookings.getTotalPages())
        .totalElements(bookings.getTotalElements())
        .items(bookings.getContent())
        .build();
    return new ResponseSuccess(HttpStatus.OK, "Fetched booking list successfully", result);
  }

  @Operation(summary = "Get Booking By Id", description = "API to retrieve booking details by ID")
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
  public ResponseSuccess getBookingById(@PathVariable Long id) {
    BookingResponse response = bookingService.getBookingById(id);
    return new ResponseSuccess(HttpStatus.OK, "Booking retrieved successfully", response);
  }

  @Operation(summary = "Update Booking", description = "API to update an existing booking by ID")
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
  public ResponseSuccess updateBooking(@PathVariable Long id,
      @Valid @RequestBody BookingRequest request) {
    BookingResponse response = bookingService.updateBooking(id, request);
    return new ResponseSuccess(HttpStatus.OK, "Booking updated successfully", response);
  }

  @Operation(summary = "Delete Booking", description = "API to delete a booking by ID")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
  public ResponseSuccess deleteBooking(@PathVariable Long id) {
    bookingService.deleteBooking(id);
    return new ResponseSuccess(HttpStatus.NO_CONTENT, "Booking deleted successfully");
  }

  @Operation(summary = "Delete Bookings", description = "API to soft delete multiple bookings by IDs")
  @DeleteMapping("/ids")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
  public ResponseSuccess deleteBookings(@RequestBody List<Long> ids) {
    bookingService.deleteBookings(ids);
    return new ResponseSuccess(HttpStatus.NO_CONTENT, "Bookings deleted successfully");
  }

  @Operation(summary = "Restore Booking", description = "API to restore a soft-deleted booking by ID")
  @PutMapping("/{id}/restore")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
  public ResponseSuccess restoreBooking(@PathVariable Long id) {
    bookingService.restoreBooking(id);
    return new ResponseSuccess(HttpStatus.OK, "Booking restored successfully");
  }

  @Operation(summary = "Restore Bookings", description = "API to restore multiple soft-deleted bookings by IDs")
  @PutMapping("/restore")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF')")
  public ResponseSuccess restoreBookings(@RequestBody List<Long> ids) {
    bookingService.restoreBookings(ids);
    return new ResponseSuccess(HttpStatus.OK, "Bookings restored successfully");
  }

  @Operation(summary = "Delete Booking Permanently", description = "API to permanently delete a booking by ID")
  @DeleteMapping("/{id}/permanent")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
  public ResponseSuccess deleteBookingPermanently(@PathVariable Long id) {
    bookingService.deleteBookingPermanently(id);
    return new ResponseSuccess(HttpStatus.NO_CONTENT, "Booking permanently deleted");
  }

  @Operation(summary = "Delete Bookings Permanently", description = "API to permanently delete multiple bookings by IDs")
  @DeleteMapping("/permanent")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
  public ResponseSuccess deleteBookingsPermanently(@RequestBody List<Long> ids) {
    bookingService.deleteBookingsPermanently(ids);
    return new ResponseSuccess(HttpStatus.NO_CONTENT, "Bookings permanently deleted");
  }

  @Operation(summary = "Get My Bookings", description = "API for guests to retrieve their own bookings")
  @GetMapping("/my-bookings")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAuthority('GUEST')")
  public ResponseSuccess getMyBookings(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id", required = false) String sort) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
    Page<BookingResponse> bookings = bookingService.getMyBookings(pageable);

    PageResponse<?> result = PageResponse.builder()
        .pageNo(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPage(bookings.getTotalPages())
        .totalElements(bookings.getTotalElements())
        .items(bookings.getContent())
        .build();
    return new ResponseSuccess(HttpStatus.OK, "My bookings retrieved successfully", result);
  }

  @Operation(summary = "Get My Booking By Id", description = "API for guests to retrieve their own booking details")
  @GetMapping("/my-bookings/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAuthority('GUEST')")
  public ResponseSuccess getMyBookingById(@PathVariable Long id) {
    BookingResponse response = bookingService.getMyBookingById(id);
    return new ResponseSuccess(HttpStatus.OK, "Booking retrieved successfully", response);
  }

  @Operation(summary = "Delete Booking By Guest", description = "API to permanently delete a booking by ID")
  @PutMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'STAFF','GUEST')")
  public ResponseSuccess cancelBooking(@PathVariable Long id) {
    bookingService.cancelBooking(id);
    return new ResponseSuccess(HttpStatus.NO_CONTENT, "Booking permanently deleted");
  }

}
