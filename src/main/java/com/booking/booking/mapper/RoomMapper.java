package com.booking.booking.mapper;

import com.booking.booking.dto.RoomDTO;
import com.booking.booking.controller.response.RoomResponse;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomMapper {

  private final HotelRepository hotelRepository;
  private final ModelMapper modelMapper;

  public Room toRoom(RoomDTO roomDTO) {

    Hotel hotel = hotelRepository.findById(roomDTO.getHotelId())
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

    Room room = modelMapper.map(roomDTO, Room.class);
    room.setId(null);
    room.setHotel(hotel);
    return room;
  }

  public RoomResponse toRoomResponseDTO(Room room) {
    return RoomResponse.builder()
        .id(room.getId())
        .typeRoom(room.getTypeRoom())
        .capacity(room.getCapacity())
        .pricePerNight(room.getPricePerNight())
        .available(room.isAvailable())
        .listImageUrl(room.getListImageUrl())
        .hotelId(room.getHotel() != null ? room.getHotel().getId() : null)
        .hotelName(room.getHotel() != null ? room.getHotel().getName() : null)
        .createdAt(room.getCreatedAt())
        .updatedAt(room.getUpdatedAt())
        .createdByUser(room.getCreatedByUser() != null ? room.getCreatedByUser().getUsername() : null)
        .updatedByUser(room.getUpdatedByUser() != null ? room.getUpdatedByUser().getUsername() : null)
        .build();
  }
}
