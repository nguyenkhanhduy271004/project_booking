package com.booking.booking.service.interfaces;

import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.UserResponse;
import com.booking.booking.dto.HotelDTO;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface HotelService {

    Page<Hotel> getAllHotels(Pageable pageable);

    Page<Hotel> getAllHotels(Pageable pageable, boolean deleted);

    PageResponse<?> advanceSearchWithSpecification(Pageable pageable, String[] hotel);

    HotelDTO getHotelById(Long id) throws ResourceNotFoundException;

    List<Room> getRoomByHotelId(Long hotelId) throws ResourceNotFoundException;

    Hotel createHotel(HotelDTO hotelDTO, MultipartFile imageHotel);

    Hotel updateHotel(Long id, HotelDTO updatedHotel, MultipartFile imageHotel) throws ResourceNotFoundException;

    void deleteHotel(Long id) throws ResourceNotFoundException;

    void deleteHotels(List<Long> ids) throws ResourceNotFoundException;

    void softDeleteHotel(Long id);

    void softDeleteHotels(List<Long> ids);

    void restoreHotel(Long id);

    void restoreHotels(List<Long> ids);

    void deleteHotelPermanently(Long id);

    void deleteHotelsPermanently(List<Long> ids);

    Page<Hotel> getAllHotelsWithAuthorization(Pageable pageable, boolean deleted);

    Optional<Hotel> getHotelByIdWithAuthorization(Long id);

    List<UserResponse> getListManagersForCreateOrUpdateHotel();
}
