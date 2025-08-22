package com.booking.booking.mapper;

import com.booking.booking.common.UserType;
import com.booking.booking.dto.HotelDTO;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.User;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.util.UserContext;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelMapper {

  private final ModelMapper modelMapper;
  private final UserContext userContext;
  private final UserRepository userRepository;
  private final HotelRepository hotelRepository;

  public HotelDTO toDTO(Hotel hotel) {
    return modelMapper.map(hotel, HotelDTO.class);
  }

  public Hotel toHotel(HotelDTO hotelDTO) {
    Hotel newHotel = modelMapper.map(hotelDTO, Hotel.class);
    newHotel.setServices(hotelDTO.getServices());

    User currentUser = userContext.getCurrentUser();
    if (currentUser == null) {
      throw new ResourceNotFoundException("User not found");
    }

    User manager = userRepository.findByIdAndIsDeletedFalse(hotelDTO.getManagerId());

    if (manager == null || !manager.getType().equals(UserType.MANAGER)) {
      throw new ResourceNotFoundException("User is not manager");
    }

    Optional<Hotel> existingHotel = hotelRepository.findByManagedByUserAndIsDeletedFalse(manager);

    if (existingHotel.isPresent()) {
      throw new ResourceNotFoundException("One manager only can manage one hotel");
    }

    newHotel.setManagedByUser(manager);

    newHotel.setCreatedByUser(currentUser);

    Date now = Date.valueOf(LocalDate.now());
    newHotel.setCreatedAt(now);
    newHotel.setUpdatedAt(now);

    return newHotel;
  }

  public void updateHotelFromDTO(Hotel hotel, HotelDTO hotelDTO) {
    hotel.setName(hotelDTO.getName());
    hotel.setDistrict(hotelDTO.getDistrict());
    hotel.setAddressDetail(hotelDTO.getAddressDetail());
    hotel.setTotalRooms(hotelDTO.getTotalRooms());
    hotel.setStarRating(hotelDTO.getStarRating());
    hotel.setServices(hotelDTO.getServices());
    hotel.setUpdatedAt(Date.valueOf(LocalDate.now()));

    User currentUser = userContext.getCurrentUser();
    if (currentUser == null) {
      throw new ResourceNotFoundException("User not found");
    }

    if(hotelDTO.getManagerId() != null) {
      User manager = userRepository.findByIdAndIsDeletedFalse(hotelDTO.getManagerId());
      if (manager == null || !manager.getType().equals(UserType.MANAGER)) {
        throw new ResourceNotFoundException("User is not manager");
      }

      Optional<Hotel> existingHotel = hotelRepository.findByManagedByUserAndIsDeletedFalse(manager);
      if (existingHotel.isPresent() && !existingHotel.get().getId().equals(hotel.getId())) {
        throw new ResourceNotFoundException("One manager only can manage one hotel");
      }

      hotel.setManagedByUser(manager);
    }
    hotel.setUpdatedByUser(currentUser);
  }

}
