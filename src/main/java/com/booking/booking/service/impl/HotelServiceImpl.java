package com.booking.booking.service.impl;

import com.booking.booking.common.UserType;
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
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.repository.VoucherRepository;
import com.booking.booking.service.CloudinaryService;
import com.booking.booking.service.HotelService;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.UserContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

  private final UserMapper userMapper;
  private final UserContext userContext;
  private final HotelMapper hotelMapper;
  private final RoomRepository roomRepository;
  private final HotelRepository hotelRepository;
  private final CloudinaryService cloudinaryService;
  private final AuthorizationUtils authorizationUtils;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final UserRepository userRepository;
  private final VoucherRepository voucherRepository;

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
  public void deleteHotel(Long id) {
    Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

    String oldImageUrl = hotel.getImageUrl();
    if (oldImageUrl != null && !oldImageUrl.isBlank()) {
      try {
        String publicId = extractPublicIdFromUrl(oldImageUrl);
        if (publicId != null && !publicId.isBlank()) {
          cloudinaryService.delete(publicId);
        } else {
          log.warn("Image publicId extraction failed for URL: {}", oldImageUrl);
        }
      } catch (Exception e) {
        log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
      }
    }

    hotel.setDeleted(true);
    hotel.setDeletedAt(new java.util.Date());

    User currentUser = userContext.getCurrentUser();
    if (currentUser != null) {
      hotel.setUpdatedByUser(currentUser);
    }

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
    Set<Long> valid = existing.stream().map(Hotel::getId)
        .collect(Collectors.toSet());
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

    // Delete hotel image from Cloudinary
    deleteImage(hotel.getImageUrl());

    // Delete room images from Cloudinary
    roomRepository.findByHotelId(id).forEach(room -> {
      java.util.List<String> imageUrls = room.getListImageUrl();
      if (imageUrls != null) {
        imageUrls.forEach(this::deleteImage);
      }
    });

    // Delete vouchers first to avoid foreign key constraint violation
    voucherRepository.deleteAllByHotelId(id);

    // Delete the hotel - cascade will automatically delete:
    // - All rooms (via CascadeType.ALL)
    // - All bookings (via CascadeType.ALL)
    // - All evaluates (via Room cascade to Evaluate)
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

    // Delete all vouchers for all hotels first to avoid foreign key constraint
    // violations
    voucherRepository.deleteAllByHotelIds(ids);

    hotels.forEach(hotel -> {
      // Delete hotel image from Cloudinary
      deleteImage(hotel.getImageUrl());

      // Delete room images from Cloudinary
      List<Room> rooms = roomRepository.findByHotelId(hotel.getId());
      rooms.forEach(room -> {
        List<String> imageUrls = room.getListImageUrl();
        if (imageUrls != null) {
          imageUrls.forEach(this::deleteImage);
        }
      });

      // Delete the hotel - cascade will automatically delete all related entities
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

  /**
   * Lấy danh sách hotel với phân quyền - System Admin & Admin: Xem tất cả -
   * Manager: Chỉ xem hotel
   * mình quản lý - Staff: Chỉ xem hotel mình làm việc - Guest: Không xem được
   */
  @Override
  public Page<Hotel> getAllHotelsWithAuthorization(Pageable pageable, boolean deleted) {
    User currentUser = authorizationUtils.getCurrentUser();

    if (authorizationUtils.canAccessAllData()) {
      return getAllHotels(pageable, deleted);
    } else if (authorizationUtils.isManager()) {
      return deleted
          ? hotelRepository.findAllByManagedByUserAndIsDeletedTrue(currentUser, pageable)
          : hotelRepository.findAllByManagedByUserAndIsDeletedFalse(currentUser, pageable);
    } else if (authorizationUtils.isStaff()) {
      return deleted
          ? hotelRepository.findAllByManagedByUserAndIsDeletedTrue(currentUser, pageable)
          : hotelRepository.findAllByManagedByUserAndIsDeletedFalse(currentUser, pageable);
    } else {
      return Page.empty(pageable);
    }
  }

  /**
   * Lấy hotel theo ID với phân quyền - System Admin & Admin: Xem tất cả - Manager
   * & Staff: Chỉ xem
   * hotel mình quản lý/làm việc - Guest: Không xem được
   */
  @Override
  public Optional<Hotel> getHotelByIdWithAuthorization(Long id) {
    // User currentUser = authorizationUtils.getCurrentUser();
    //
    // if (authorizationUtils.canAccessAllData()) {
    // // System Admin and Admin can see all hotels
    // return getHotelById(id);
    // } else if (authorizationUtils.isManager() || authorizationUtils.isStaff()) {
    // // Manager and Staff can only see hotels they manage/work at
    // return hotelRepository.findByIdAndManagedByUserAndIsDeletedFalse(id,
    // currentUser);
    // } else {
    // // Guest or other roles - return empty
    // return Optional.empty();
    // }
    return null;
  }

  @Override
  public List<UserResponse> getListManagersForCreateOrUpdateHotel() {
    return userRepository.findAllByType(UserType.MANAGER).stream()
        .map(userMapper::toUserResponse)
        .toList();
  }

}
