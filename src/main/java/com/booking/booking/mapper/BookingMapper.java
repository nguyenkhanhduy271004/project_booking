package com.booking.booking.mapper;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.controller.request.BookingRequest;
import com.booking.booking.controller.response.BookingResponse;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Booking;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingMapper {

  private final ModelMapper modelMapper;
  private final HotelRepository hotelRepository;
  private final RoomRepository roomRepository;

  public BookingResponse toBookingResponse(Booking booking) {
    BookingResponse res = modelMapper.map(booking, BookingResponse.class);

    // Set hotel name
    if (booking.getHotel() != null) {
      res.setHotelName(booking.getHotel().getName());
    }

    // Map room details for website display
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

  public Booking toBooking(BookingRequest bookingRequest) {

    Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + bookingRequest.getHotelId()));

    // Get all rooms for multi-room booking
    if (bookingRequest.getRoomIds() == null || bookingRequest.getRoomIds().isEmpty()) {
      throw new ResourceNotFoundException("Room IDs cannot be null or empty");
    }

    java.util.List<Room> rooms = roomRepository.findAllById(bookingRequest.getRoomIds());
    if (rooms.size() != bookingRequest.getRoomIds().size()) {
      throw new ResourceNotFoundException("Some room IDs not found");
    }

    // Validate all rooms belong to the hotel
    for (Room room : rooms) {
      if (!hotel.getRooms().contains(room)) {
        throw new ResourceNotFoundException(
            String.format("Hotel with id %s does not contain room with id %s", bookingRequest.getHotelId(),
                room.getId()));
      }
    }

    Booking booking = new Booking();
    booking.setHotel(hotel);
    booking.setLegacyRoomId(rooms.isEmpty() ? null : rooms.get(0).getId()); // Set first room for legacy compatibility
    booking.setRooms(new java.util.ArrayList<>(rooms));
    booking.setCheckInDate(bookingRequest.getCheckInDate());
    booking.setCheckOutDate(bookingRequest.getCheckOutDate());
    booking.setTotalPrice(BigDecimal.valueOf(bookingRequest.getTotalPrice()));
    booking.setPaymentType(bookingRequest.getPaymentType());
    booking.setStatus(BookingStatus.PENDING);
    booking.setNotes(bookingRequest.getNotes());
    booking.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

    return booking;
  }

  public void toBooking(Booking booking, BookingRequest bookingRequest,
      Hotel hotel, java.util.List<Room> rooms) {
    booking.setHotel(hotel);
    booking.setLegacyRoomId(rooms.isEmpty() ? null : rooms.get(0).getId()); // Set first room for legacy compatibility
    booking.setRooms(new java.util.ArrayList<>(rooms));
    booking.setCheckInDate(bookingRequest.getCheckInDate());
    booking.setCheckOutDate(bookingRequest.getCheckOutDate());
    booking.setTotalPrice(BigDecimal.valueOf(bookingRequest.getTotalPrice()));
    booking.setPaymentType(bookingRequest.getPaymentType());
    booking.setNotes(bookingRequest.getNotes());
    booking.setStatus(bookingRequest.getStatus());
  }

}
