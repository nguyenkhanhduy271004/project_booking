package com.booking.booking.mapper;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.dto.request.BookingRequest;
import com.booking.booking.dto.response.BookingResponse;
import com.booking.booking.dto.BookingDTO;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Booking;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.model.User;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.repository.UserRepository;
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
  private final UserRepository userRepository;

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

  public Booking toBooking(BookingDTO bookingDTO) {
    Hotel hotel = hotelRepository.findById(bookingDTO.getHotelId())
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + bookingDTO.getHotelId()));

    User guest = userRepository.findById(bookingDTO.getGuestId())
        .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + bookingDTO.getGuestId()));

    // Get all rooms for multi-room booking
    if (bookingDTO.getRoomIds() == null || bookingDTO.getRoomIds().isEmpty()) {
      throw new ResourceNotFoundException("Room IDs cannot be null or empty");
    }

    java.util.List<Room> rooms = roomRepository.findAllById(bookingDTO.getRoomIds());
    if (rooms.size() != bookingDTO.getRoomIds().size()) {
      throw new ResourceNotFoundException("Some room IDs not found");
    }

    // Validate all rooms belong to the hotel
    for (Room room : rooms) {
      if (!hotel.getRooms().contains(room)) {
        throw new ResourceNotFoundException(
            String.format("Hotel with id %s does not contain room with id %s", bookingDTO.getHotelId(),
                room.getId()));
      }
    }

    Booking booking = new Booking();
    booking.setHotel(hotel);
    booking.setGuest(guest);
    booking.setLegacyRoomId(rooms.isEmpty() ? null : rooms.get(0).getId());
    booking.setRooms(new java.util.ArrayList<>(rooms));
    booking.setCheckInDate(bookingDTO.getCheckInDate());
    booking.setCheckOutDate(bookingDTO.getCheckOutDate());
    booking.setTotalPrice(bookingDTO.getTotalPrice());
    booking.setPaymentType(bookingDTO.getPaymentType());
    booking.setStatus(bookingDTO.getStatus() != null ? bookingDTO.getStatus() : BookingStatus.PENDING);
    booking.setNotes(bookingDTO.getNotes());

    if (bookingDTO.getBookingCode() != null) {
      booking.setBookingCode(bookingDTO.getBookingCode());
    } else {
      booking.setBookingCode("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    return booking;
  }

  public void updateBooking(Booking existingBooking, BookingDTO bookingDTO) {
    if (bookingDTO.getHotelId() != null) {
      Hotel hotel = hotelRepository.findById(bookingDTO.getHotelId())
          .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + bookingDTO.getHotelId()));
      existingBooking.setHotel(hotel);
    }

    if (bookingDTO.getGuestId() != null) {
      User guest = userRepository.findById(bookingDTO.getGuestId())
          .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + bookingDTO.getGuestId()));
      existingBooking.setGuest(guest);
    }

    if (bookingDTO.getRoomIds() != null && !bookingDTO.getRoomIds().isEmpty()) {
      java.util.List<Room> rooms = roomRepository.findAllById(bookingDTO.getRoomIds());
      existingBooking.setRooms(new java.util.ArrayList<>(rooms));
      existingBooking.setLegacyRoomId(rooms.isEmpty() ? null : rooms.get(0).getId());
    }

    if (bookingDTO.getCheckInDate() != null) {
      existingBooking.setCheckInDate(bookingDTO.getCheckInDate());
    }

    if (bookingDTO.getCheckOutDate() != null) {
      existingBooking.setCheckOutDate(bookingDTO.getCheckOutDate());
    }

    if (bookingDTO.getTotalPrice() != null) {
      existingBooking.setTotalPrice(bookingDTO.getTotalPrice());
    }

    if (bookingDTO.getPaymentType() != null) {
      existingBooking.setPaymentType(bookingDTO.getPaymentType());
    }

    if (bookingDTO.getStatus() != null) {
      existingBooking.setStatus(bookingDTO.getStatus());
    }

    if (bookingDTO.getNotes() != null) {
      existingBooking.setNotes(bookingDTO.getNotes());
    }
  }

}
