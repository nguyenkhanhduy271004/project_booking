package com.booking.booking.service;

import com.booking.booking.controller.request.BookingRequest;
import com.booking.booking.controller.response.BookingResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

  java.util.List<BookingResponse> createBooking(BookingRequest request);

  Page<BookingResponse> getAllBookings(Pageable pageable);

  Page<BookingResponse> getAllBookings(Pageable pageable, boolean deleted);

  BookingResponse getBookingById(Long id);

  BookingResponse updateBooking(Long id, BookingRequest request);

  void deleteBooking(Long id);

  void deleteBookings(List<Long> ids);

  void restoreBooking(Long id);

  void restoreBookings(List<Long> ids);

  void deleteBookingPermanently(Long id);

  void cancelBooking(Long id);

  void deleteBookingsPermanently(List<Long> ids);

  List<BookingResponse> getAllBookingsByGuestId();

  Page<BookingResponse> getMyBookings(Pageable pageable);

  BookingResponse getMyBookingById(Long id);
}
