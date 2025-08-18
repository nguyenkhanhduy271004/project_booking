package com.booking.booking.mapper;


import com.booking.booking.dto.RoomDTO;
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
}
