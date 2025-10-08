package com.booking.booking.service.interfaces;

import com.booking.booking.dto.request.BookingRequest;
import com.booking.booking.dto.response.BookingResponse;
import com.booking.booking.dto.response.RoomBookedDatesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    List<BookingResponse> createBooking(BookingRequest request);

    Page<BookingResponse> getAllBookings(Pageable pageable);

    Page<BookingResponse> getAllBookings(Pageable pageable, boolean deleted);

    BookingResponse getBookingById(Long id);

    BookingResponse getBookingByBookingCode(String bookingCode);

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

    Page<BookingResponse> getHistoryBookingByRoomId(Long roomId, LocalDate from, LocalDate to, int page, int size);

    void updateStatusBooking(Long id, String Status);

    RoomBookedDatesResponse getBookedDates(Long roomId);


}
