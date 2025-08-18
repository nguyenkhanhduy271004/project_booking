package com.booking.booking.service;

import com.booking.booking.dto.RoomDTO;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Room;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface RoomService {

  Page<Room> getAllRooms(Pageable pageable);

  Page<Room> getAllRooms(Pageable pageable, boolean deleted);

  Optional<Room> getRoomById(Long id);

  Room createRoom(RoomDTO room, MultipartFile[] imagesRoom);

  Room updateRoom(Long id, RoomDTO updatedRoom, MultipartFile[] images) throws ResourceNotFoundException;

  void deleteRoom(Long id);

  void deleteRooms(List<Long> ids);

  void softDeleteRoom(Long id);

  void softDeleteRooms(List<Long> ids);

  void restoreRoom(Long id);

  void restoreRooms(List<Long> ids);

  void deleteRoomPermanently(Long id);

  void deleteRoomsPermanently(List<Long> ids);

  boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut);

  List<Room> getAvailableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut);

  List<LocalDate> getUnavailableDates(Long roomId, LocalDate from, LocalDate to);

  boolean reserveRooms(List<Long> roomIds, LocalDate checkIn, LocalDate checkOut);

  int getAvailableRoomCount(Long hotelId, LocalDate checkIn, LocalDate checkOut);

  boolean areRoomsAvailableForBooking(List<Long> roomIds, LocalDate checkIn, LocalDate checkOut);
}
