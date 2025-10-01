package com.booking.booking.util;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.model.Booking;
import com.booking.booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingUtil {

    private final RoomRepository roomRepository;

    public void handleBookingWithStatus(Booking booking, BookingStatus bookingStatus) {
        switch (bookingStatus) {
            case CHECKIN, PAYING, CONFIRMED -> {
                booking.getRooms().forEach(room -> room.setAvailable(false));
                roomRepository.saveAll(booking.getRooms());
            }
            case  CANCELLED, EXPIRED, CHECKOUT, COMPLETED -> {
                booking.getRooms().forEach(room -> room.setAvailable(true));
                roomRepository.saveAll(booking.getRooms());
            }
            default -> {
            }
        }
    }
}
