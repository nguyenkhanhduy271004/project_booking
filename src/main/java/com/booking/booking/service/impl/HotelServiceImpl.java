package com.booking.booking.service.impl;

import com.booking.booking.common.UserType;
import com.booking.booking.dto.HotelDTO;
import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.UserResponse;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.InvalidHotelIdsException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.mapper.HotelMapper;
import com.booking.booking.mapper.UserMapper;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.model.User;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.repository.VoucherRepository;
import com.booking.booking.service.CloudinaryService;
import com.booking.booking.service.interfaces.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "HOTEL-SERVICE")
public class HotelServiceImpl implements HotelService {

    private final UserMapper userMapper;
    private final HotelMapper hotelMapper;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final CloudinaryService cloudinaryService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<Hotel> getAllHotels(Pageable pageable, boolean deleted) {
        return deleted ? hotelRepository.findAllByIsDeletedTrue(pageable)
                : hotelRepository.findAllByIsDeletedFalse(pageable);
    }

    @Override
    public PageResponse<?> advanceSearchWithSpecification(Pageable pageable, String[] hotel) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDTO getHotelById(Long id) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        User manager = hotel.getManager();

        HotelDTO hotelDTO = hotelMapper.toDTO(hotel);

        if (manager != null) {
            UserResponse userResponse = userMapper.toUserResponse(manager);
            hotelDTO.setManagedBy(userResponse);
        } else {
            hotelDTO.setManagedBy(null);
        }

        return hotelDTO;
    }


    @Override
    public List<Room> getRoomByHotelId(Long hotelId) {
        hotelRepository.findByIdAndIsDeletedFalse(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

        return roomRepository.findByHotelIdAndIsDeletedFalse(hotelId);
    }

    @Override
    @Transactional
    public Hotel createHotel(HotelDTO hotelDTO, MultipartFile imageHotel) {
        Hotel hotel = hotelMapper.toHotel(hotelDTO);

        if (imageHotel != null && !imageHotel.isEmpty()) {
            try {
                Map<String, Object> data = this.cloudinaryService.upload(imageHotel);
                String imageUrl = (String) data.get("secure_url");
                log.info("Uploaded image URL: {}", imageUrl);
                hotel.setImageUrl(imageUrl);
            } catch (Exception e) {
                log.error("Error uploading hotel image", e);
                throw new BadRequestException(
                        "Failed to upload image: " + imageHotel.getOriginalFilename());
            }
        }

        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel updateHotel(Long id, HotelDTO updatedHotel, MultipartFile imageHotel) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        if (imageHotel != null && !imageHotel.isEmpty()) {
            try {
                String oldImageUrl = hotel.getImageUrl();
                if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                    String publicId = extractPublicIdFromUrl(oldImageUrl);
                    cloudinaryService.delete(publicId);
                }

                Map<String, Object> data = cloudinaryService.upload(imageHotel);
                String imageUrl = (String) data.get("secure_url");
                log.info("Uploaded new hotel image URL: {}", imageUrl);
                hotel.setImageUrl(imageUrl);
            } catch (Exception e) {
                log.error("Error uploading image", e);
                throw new BadRequestException(
                        "Failed to upload image: " + imageHotel.getOriginalFilename());
            }
        }

        hotelMapper.updateHotelFromDTO(hotel, updatedHotel);

        return hotelRepository.save(hotel);
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            int folderIndex = imageUrl.indexOf("/upload/") + "/upload/".length();
            String folderPathWithFile = imageUrl.substring(folderIndex);
            String publicIdWithExt = folderPathWithFile.replaceAll("^v\\d+/", "");
            return publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf('.'));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract publicId from image URL: " + imageUrl, e);
        }
    }


    @Override
    public void softDeleteHotel(Long id) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));
        hotel.setDeleted(true);
        hotel.setDeletedAt(new Date());
        hotelRepository.save(hotel);
    }

    @Override
    public void softDeleteHotels(List<Long> ids) {
        List<Hotel> existing = hotelRepository.findAllByIdInAndIsDeletedFalse(ids);
        Set<Long> valid = existing.stream().map(Hotel::getId)
                .collect(Collectors.toSet());
        List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
        if (!invalid.isEmpty()) {
            throw new InvalidHotelIdsException("Some hotel IDs are invalid or already deleted", invalid);
        }
        hotelRepository.softDeleteByIds(ids, new Date());
    }

    @Override
    public void restoreHotel(Long id) {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));
        hotel.setDeleted(false);
        hotelRepository.save(hotel);
    }

    @Override
    public void restoreHotels(List<Long> ids) {
        List<Hotel> existing = hotelRepository.findAllByIdInAndIsDeletedTrue(ids);
        Set<Long> valid = existing.stream().map(Hotel::getId)
                .collect(Collectors.toSet());
        List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
        if (!invalid.isEmpty()) {
            throw new InvalidHotelIdsException("Some hotel IDs are invalid or not deleted", invalid);
        }
        hotelRepository.restoreByIds(ids);
    }

    @Override
    public void deleteHotelPermanently(Long id) throws ResourceNotFoundException {
        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

        deleteImage(hotel.getImageUrl());

        roomRepository.findByHotelIdAndIsDeletedFalse(id).forEach(room -> {
            List<String> imageUrls = room.getListImageUrl();
            if (imageUrls != null) {
                imageUrls.forEach(this::deleteImage);
            }
        });

        voucherRepository.deleteAllByHotelId(id);

        hotelRepository.delete(hotel);
    }

    @Transactional
    public void deleteHotelsPermanently(List<Long> ids) {
        List<Hotel> hotels = hotelRepository.findAllById(ids);
        Set<Long> foundIds = hotels.stream().map(Hotel::getId).collect(Collectors.toSet());

        List<Long> invalidIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new InvalidHotelIdsException("Some hotel IDs are invalid", invalidIds);
        }


        voucherRepository.deleteAllByHotelIds(ids);

        hotels.forEach(hotel -> {
            deleteImage(hotel.getImageUrl());

            List<Room> rooms = roomRepository.findByHotelIdAndIsDeletedFalse(hotel.getId());
            rooms.forEach(room -> {
                List<String> imageUrls = room.getListImageUrl();
                if (imageUrls != null) {
                    imageUrls.forEach(this::deleteImage);
                }
            });

            hotelRepository.delete(hotel);
        });
    }

    private void deleteImage(String url) {
        if (url != null && !url.isBlank()) {
            try {
                String publicId = extractPublicIdFromUrl(url);
                kafkaTemplate.send("delete-image", publicId);
            } catch (Exception e) {
                log.warn("Failed to delete image: {}", url, e);
            }
        }
    }


    @Override
    public List<UserResponse> getListManagersForCreateOrUpdateHotel() {
        return userRepository.findAllByType(UserType.MANAGER).stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

}
