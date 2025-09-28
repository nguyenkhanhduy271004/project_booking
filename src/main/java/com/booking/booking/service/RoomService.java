package com.booking.booking.service;

import com.booking.booking.dto.RoomDTO;
import com.booking.booking.dto.response.RoomResponse;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomService {


    Page<RoomResponse> getAllRoomsWithHotelName(Pageable pageable, boolean deleted);

    Optional<RoomResponse> getRoomByIdWithHotelName(Long id);

    RoomResponse createRoom(RoomDTO room, MultipartFile[] imagesRoom);


    RoomResponse updateRoom(Long id, RoomDTO updatedRoom, MultipartFile[] images, String keepImagesJson) throws ResourceNotFoundException;


    void softDeleteRoom(Long id);

    void softDeleteRooms(List<Long> ids);

    void restoreRoom(Long id);

    void restoreRooms(List<Long> ids);

    void deleteRoomPermanently(Long id);

    void deleteRoomsPermanently(List<Long> ids);

    boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut);

    List<Room> getAvailableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut);

    List<RoomResponse> getAvailableRoomsWithHotelName(Long hotelId, LocalDate checkIn, LocalDate checkOut);

    List<LocalDate> getUnavailableDates(Long roomId, LocalDate from, LocalDate to);

    void updateStatusRoom(List<Long> ids, Boolean status);
}
