package com.booking.booking.mapper;

import com.booking.booking.dto.HotelDTO;
import com.booking.booking.model.Hotel;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelMapper {

    private final ModelMapper modelMapper;

    public HotelDTO toDTO(Hotel hotel) {
        return modelMapper.map(hotel, HotelDTO.class);
    }

    public Hotel toHotel(HotelDTO hotelDTO) {
        return modelMapper.map(hotelDTO, Hotel.class);
    }
}
