package com.booking.booking.mapper;

import com.booking.booking.dto.request.BookingRequest;
import com.booking.booking.dto.response.BookingResponse;
import com.booking.booking.model.Booking;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final ModelMapper modelMapper;

    public BookingResponse toBookingResponse(Booking booking) {
        BookingResponse res = modelMapper.map(booking, BookingResponse.class);

        if (booking.getHotel() != null) {
            res.setHotelName(booking.getHotel().getName());
        }

        if (booking.getRooms() != null && !booking.getRooms().isEmpty()) {
            java.util.List<BookingResponse.RoomInfo> roomInfos = booking.getRooms().stream()
                    .map(room -> {
                        BookingResponse.RoomInfo roomInfo = new BookingResponse.RoomInfo();
                        roomInfo.setId(room.getId());
                        roomInfo.setTypeRoom(room.getTypeRoom());
                        roomInfo.setCapacity(room.getCapacity());
                        roomInfo.setPricePerNight(room.getPricePerNight());
                        roomInfo.setAvailable(room.isAvailable());
                        roomInfo
                                .setImageUrls(room.getListImageUrl() != null ? room.getListImageUrl() : new java.util.ArrayList<>());
                        return roomInfo;
                    })
                    .toList();
            res.setRooms(roomInfos);
        }

        return res;
    }


    public void toBooking(Booking booking, BookingRequest bookingRequest,
                          Hotel hotel, java.util.List<Room> rooms) {
        booking.setHotel(hotel);
        booking.setLegacyRoomId(rooms.isEmpty() ? null : rooms.get(0).getId());
        booking.setRooms(new java.util.ArrayList<>(rooms));
        booking.setCheckInDate(bookingRequest.getCheckInDate());
        booking.setCheckOutDate(bookingRequest.getCheckOutDate());
        booking.setTotalPrice(BigDecimal.valueOf(bookingRequest.getTotalPrice()));
        booking.setPaymentType(bookingRequest.getPaymentType());
        booking.setNotes(bookingRequest.getNotes());
        booking.setStatus(bookingRequest.getStatus());
    }


}
