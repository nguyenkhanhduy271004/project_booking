package com.booking.booking.service.impl;

import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.UserResponse;
import com.booking.booking.dto.HotelDTO;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.InvalidHotelIdsException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.mapper.HotelMapper;
import com.booking.booking.mapper.UserMapper;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.model.User;
import com.booking.booking.repository.BookingRepository;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.service.CloudinaryService;
import com.booking.booking.service.HotelService;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.UserContext;
import com.google.gson.Gson;
import jakarta.transaction.Transactional;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j(topic = "HOTEL-SERVICE")
public class HotelServiceImpl implements HotelService {

  private final Gson gson;
  private final UserContext userContext;
  private final AuthorizationUtils authorizationUtils;
  private final HotelMapper hotelMapper;
  private final RoomRepository roomRepository;
  private final HotelRepository hotelRepository;
  private final CloudinaryService cloudinaryService;
  private final BookingRepository bookingRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  public Page<Hotel> getAllHotels(Pageable pageable) {
    return hotelRepository.findAllByIsDeletedFalse(pageable);
  }

  @Override
  public Page<Hotel> getAllHotels(Pageable pageable, boolean deleted) {
    return deleted ? hotelRepository.findAllByIsDeletedTrue(pageable)
        : hotelRepository.findAllByIsDeletedFalse(pageable);
  }

  @Override
  public PageResponse<?> advanceSearchWithSpecification(Pageable pageable, String[] hotel) {
    return null;
  }

  @Override
  public HotelDTO getHotelById(Long id) {
    Hotel hotel = hotelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

    UserResponse userResponse = userMapper.toUserResponse(hotel.getManagedByUser());

    HotelDTO hotelDTO = hotelMapper.toDTO(hotel);
    hotelDTO.setManagedBy(userResponse);

    return hotelDTO;
  }

  @Override
  public List<Room> getRoomByHotelId(Long hotelId) {
    hotelRepository.findByIdAndIsDeletedFalse(hotelId)
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

    return roomRepository.findByHotelId(hotelId);
  }

  @Override
  public Hotel createHotel(HotelDTO hotelDTO, MultipartFile imageHotel) {
    Hotel hotel = hotelMapper.toHotel(hotelDTO);
    hotel.setCreatedAt(Date.valueOf(LocalDate.now()));
    hotel.setUpdatedAt(Date.valueOf(LocalDate.now()));

    User managedUser = userContext.getCurrentUser();

    if (managedUser == null) {
      throw new ResourceNotFoundException("User not found with id: " + managedUser.getId());
    }
    hotel.setManagedByUser(managedUser);

    User createdUser = userContext.getCurrentUser();
    hotel.setCreatedByUser(createdUser);

    if (imageHotel != null && !imageHotel.isEmpty()) {
      try {
        Map<String, Object> data = this.cloudinaryService.upload(imageHotel);
        String imageUrl = (String) data.get("secure_url");
        log.info(imageUrl);
        hotel.setImageUrl(imageUrl);
      } catch (Exception e) {
        throw new BadRequestException(
            "Failed to upload image: " + imageHotel.getOriginalFilename());
      }
    }

    return hotelRepository.save(hotel);
  }

  @Override
  public Hotel updateHotel(Long id, HotelDTO updatedHotel,
      MultipartFile imageHotel) {
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
        log.info(imageUrl);
        hotel.setImageUrl(imageUrl);
      } catch (Exception e) {
        throw new BadRequestException(
            "Failed to upload image: " + imageHotel.getOriginalFilename());
      }
    }

    updateFields(hotel, updatedHotel);
    hotel.setUpdatedAt(Date.valueOf(LocalDate.now()));

    User managedUser = userContext.getCurrentUser();

    if (managedUser == null) {
      throw new ResourceNotFoundException("User not found with id: " + managedUser.getId());
    }
    hotel.setManagedByUser(managedUser);

    User updatedUser = userContext.getCurrentUser();
    log.info("updated by user: " + updatedUser.getUsername());
    hotel.setUpdatedByUser(updatedUser);

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

  private void updateFields(Hotel target, HotelDTO source) {
    target.setName(source.getName());
    target.setDistrict(source.getDistrict());
    target.setAddressDetail(source.getAddressDetail());
    target.setTotalRooms(source.getTotalRooms());
  }

  @Override
  public void deleteHotel(Long id) throws ResourceNotFoundException {
    Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

    String oldImageUrl = hotel.getImageUrl();
    if (oldImageUrl != null && !oldImageUrl.isBlank()) {
      try {
        String publicId = extractPublicIdFromUrl(oldImageUrl);
        cloudinaryService.delete(publicId);
      } catch (Exception e) {
        log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
      }
    }

    hotel.setDeleted(true);
    hotel.setDeletedAt(new java.util.Date());
    hotelRepository.save(hotel);
  }

  @Override
  public void deleteHotels(List<Long> ids) throws ResourceNotFoundException {
    hotelRepository.softDeleteByIds(ids, new java.util.Date());
  }

  @Override
  public void softDeleteHotel(Long id) {
    Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));
    hotel.setDeleted(true);
    hotel.setDeletedAt(new java.util.Date());
    hotelRepository.save(hotel);
  }

  @Override
  public void softDeleteHotels(List<Long> ids) {
    List<Hotel> existing = hotelRepository.findAllByIdInAndIsDeletedFalse(ids);
    java.util.Set<Long> valid = existing.stream().map(Hotel::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidHotelIdsException("Some hotel IDs are invalid or already deleted", invalid);
    }
    hotelRepository.softDeleteByIds(ids, new java.util.Date());
  }

  @Override
  public void restoreHotel(Long id) {
    Hotel hotel = hotelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));
    hotel.setDeleted(false);
    hotel.setDeletedAt(null);
    hotelRepository.save(hotel);
  }

  @Override
  public void restoreHotels(List<Long> ids) {
    List<Hotel> existing = hotelRepository.findAllByIdInAndIsDeletedTrue(ids);
    java.util.Set<Long> valid = existing.stream().map(Hotel::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidHotelIdsException("Some hotel IDs are invalid or not deleted", invalid);
    }
    hotelRepository.restoreByIds(ids);
  }

  @Override
  public void deleteHotelPermanently(Long id) throws ResourceNotFoundException {
    Hotel hotel = hotelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

    roomRepository.findByHotelId(id).forEach(room -> {
      bookingRepository.deleteByRoomIds(java.util.List.of(room.getId()));
    });
    bookingRepository.deleteByHotelIds(java.util.List.of(id));

    String oldImageUrl = hotel.getImageUrl();
    if (oldImageUrl != null && !oldImageUrl.isBlank()) {
      try {
        String publicId = extractPublicIdFromUrl(oldImageUrl);

        // cloudinaryService.delete(publicId);
        kafkaTemplate.send("delete-image", publicId);
      } catch (Exception e) {
        log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
      }
    }

    roomRepository.findByHotelId(id).forEach(room -> {
      java.util.List<String> imageUrls = room.getListImageUrl();
      if (imageUrls != null) {
        for (String url : imageUrls) {
          try {
            String publicId = extractPublicIdFromUrl(url);
            kafkaTemplate.send("delete-image", publicId);
          } catch (Exception e) {
            log.warn("Failed to delete image: {}", url, e);
          }
        }
      }
    });

    roomRepository.deleteAll(roomRepository.findByHotelId(id));
    hotelRepository.delete(hotel);
  }

  @Override
  public void deleteHotelsPermanently(List<Long> ids) throws ResourceNotFoundException {
    List<Hotel> list = hotelRepository.findAllById(ids);
    java.util.Set<Long> valid = list.stream().map(Hotel::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidHotelIdsException("Some hotel IDs are invalid", invalid);
    }
    ids.forEach(id -> {
      Hotel hotel = hotelRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
      roomRepository.findByHotelId(id).forEach(room -> {
        bookingRepository.deleteByRoomIds(java.util.List.of(room.getId()));
      });
      bookingRepository.deleteByHotelIds(java.util.List.of(id));

      String imageUrl = hotel.getImageUrl();
      if (imageUrl != null && !imageUrl.isBlank()) {
        try {
          String publicId = extractPublicIdFromUrl(imageUrl);
          kafkaTemplate.send("delete-image", publicId);
        } catch (Exception e) {
          log.warn("Failed to delete hotel image from Cloudinary: {}", imageUrl, e);
        }
      }

      roomRepository.findByHotelId(id).forEach(room -> {
        java.util.List<String> imageUrls = room.getListImageUrl();
        if (imageUrls != null) {
          for (String url : imageUrls) {
            try {
              String publicId = extractPublicIdFromUrl(url);
              kafkaTemplate.send("delete-image", publicId);
            } catch (Exception e) {
              log.warn("Failed to delete image: {}", url, e);
            }
          }
        }
      });

      roomRepository.deleteAll(roomRepository.findByHotelId(id));
      hotelRepository.delete(hotel);
    });
  }

  /**
   * Lấy danh sách hotel với phân quyền - System Admin & Admin: Xem tất cả - Manager: Chỉ xem hotel
   * mình quản lý - Staff: Chỉ xem hotel mình làm việc - Guest: Không xem được
   */
  @Override
  public Page<Hotel> getAllHotelsWithAuthorization(Pageable pageable, boolean deleted) {
    User currentUser = authorizationUtils.getCurrentUser();

    if (authorizationUtils.canAccessAllData()) {
      // System Admin and Admin can see all hotels
      return getAllHotels(pageable, deleted);
    } else if (authorizationUtils.isManager()) {
      // Manager can only see hotels they manage
      return deleted
          ? hotelRepository.findAllByManagedByUserAndIsDeletedTrue(currentUser, pageable)
          : hotelRepository.findAllByManagedByUserAndIsDeletedFalse(currentUser, pageable);
    } else if (authorizationUtils.isStaff()) {
      // Staff can only see hotels they work at (same as manager for now)
      return deleted
          ? hotelRepository.findAllByManagedByUserAndIsDeletedTrue(currentUser, pageable)
          : hotelRepository.findAllByManagedByUserAndIsDeletedFalse(currentUser, pageable);
    } else {
      // Guest or other roles - return empty page
      return Page.empty(pageable);
    }
  }

  /**
   * Lấy hotel theo ID với phân quyền - System Admin & Admin: Xem tất cả - Manager & Staff: Chỉ xem
   * hotel mình quản lý/làm việc - Guest: Không xem được
   */
  @Override
  public Optional<Hotel> getHotelByIdWithAuthorization(Long id) {
//    User currentUser = authorizationUtils.getCurrentUser();
//
//    if (authorizationUtils.canAccessAllData()) {
//      // System Admin and Admin can see all hotels
//      return getHotelById(id);
//    } else if (authorizationUtils.isManager() || authorizationUtils.isStaff()) {
//      // Manager and Staff can only see hotels they manage/work at
//      return hotelRepository.findByIdAndManagedByUserAndIsDeletedFalse(id, currentUser);
//    } else {
//      // Guest or other roles - return empty
//      return Optional.empty();
//    }
    return null;
  }

}
