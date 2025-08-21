package com.booking.booking.service.impl;

import com.booking.booking.controller.response.RoomResponse;
import com.booking.booking.dto.RoomDTO;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.InvalidRoomIdsException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.mapper.RoomMapper;
import com.booking.booking.model.Booking;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.model.User;
import com.booking.booking.repository.BookingRepository;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.service.CloudinaryService;
import com.booking.booking.service.RoomService;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.UserContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
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
@Slf4j(topic = "ROOM-SERVICE")
public class RoomServiceImpl implements RoomService {

  private final RoomMapper roomMapper;
  private final UserContext userContext;
  private final AuthorizationUtils authorizationUtils;
  private final RoomRepository roomRepository;
  private final BookingRepository bookingRepository;
  private final HotelRepository hotelRepository;
  private final CloudinaryService cloudinaryService;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public Page<Room> getAllRooms(
      Pageable pageable) {
    return roomRepository.findAllByIsDeletedFalse(pageable);
  }

  @Override
  public Page<Room> getAllRooms(Pageable pageable, boolean deleted) {
    return deleted ? roomRepository.findAllByIsDeletedTrue(pageable)
        : roomRepository.findAllByIsDeletedFalse(pageable);
  }

  @Override
  public Optional<Room> getRoomById(Long id) {
    return roomRepository.findByIdAndIsDeletedFalse(id);
  }

  @Override
  public Page<RoomResponse> getAllRoomsWithHotelName(Pageable pageable, boolean deleted) {
    Page<Room> rooms = deleted ? roomRepository.findAllByIsDeletedTrue(pageable)
        : roomRepository.findAllByIsDeletedFalse(pageable);
    return rooms.map(roomMapper::toRoomResponseDTO);
  }

  @Override
  public Optional<RoomResponse> getRoomByIdWithHotelName(Long id) {
    return roomRepository.findByIdAndIsDeletedFalse(id)
        .map(roomMapper::toRoomResponseDTO);
  }

  @Override
  public Room createRoom(RoomDTO room, MultipartFile[] imagesRoom) {

    Hotel existHotel = hotelRepository.findById(room.getHotelId()).orElseThrow(
        () -> new ResourceNotFoundException("Hotel not found with id: " + room.getHotelId()));

    long currentRoomCount = roomRepository.countByHotelId(existHotel.getId());
    if (currentRoomCount >= existHotel.getTotalRooms()) {
      throw new BadRequestException("Number of rooms exceeds total allowed rooms for the hotel");
    }

    List<String> listImageUrl = new ArrayList<>();
    if (imagesRoom != null && imagesRoom.length > 0) {
      for (MultipartFile image : imagesRoom) {
        try {
          Map<String, Object> data = this.cloudinaryService.upload(image);
          String imageUrl = (String) data.get("secure_url");
          listImageUrl.add(imageUrl);
        } catch (Exception e) {
          throw new BadRequestException("Failed to upload image: " + image.getOriginalFilename());
        }
      }
      room.setListImageUrl(listImageUrl);
    }

    Room roomEntity = roomMapper.toRoom(room);
    roomEntity.setServices(room.getServices());
    roomEntity.setCreatedByUser(userContext.getCurrentUser());

    return roomRepository.save(roomEntity);
  }

  @Override
  public RoomResponse createRoomWithHotelName(RoomDTO room, MultipartFile[] imagesRoom) {
    Room createdRoom = createRoom(room, imagesRoom);
    return roomMapper.toRoomResponseDTO(createdRoom);
  }

  @Override
  public Room updateRoom(Long id, RoomDTO updatedRoom, MultipartFile[] images) {

    Room room = roomRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    room.setTypeRoom(updatedRoom.getTypeRoom());
    room.setCapacity(updatedRoom.getCapacity());
    room.setServices(updatedRoom.getServices());
    room.setAvailable(updatedRoom.isAvailable());
    room.setUpdatedByUser(userContext.getCurrentUser());
    room.setPricePerNight(updatedRoom.getPricePerNight());

    if (images != null && images.length > 0) {
      try {
        List<String> oldImageUrls = room.getListImageUrl();
        if (oldImageUrls != null) {
          for (String url : oldImageUrls) {
            String publicId = extractPublicIdFromUrl(url);
            kafkaTemplate.send("delete-image", publicId);
          }
        }

        List<String> newUrls = new ArrayList<>();
        for (MultipartFile image : images) {
          Map<String, Object> uploadResult = cloudinaryService.upload(image);
          String imageUrl = (String) uploadResult.get("secure_url");
          newUrls.add(imageUrl);
        }
        room.setListImageUrl(newUrls);
      } catch (Exception e) {
        throw new BadRequestException("Failed to process room images");
      }
    }

    return roomRepository.save(room);
  }

  @Override
  public RoomResponse updateRoomWithHotelName(Long id, RoomDTO updatedRoom, MultipartFile[] images)
      throws ResourceNotFoundException {
    Room updatedRoomEntity = updateRoom(id, updatedRoom, images);
    return roomMapper.toRoomResponseDTO(updatedRoomEntity);
  }

  private String extractPublicIdFromUrl(String imageUrl) {
    try {
      String publicIdWithFolder = imageUrl.substring(imageUrl.indexOf("/upload/") + 8);
      return publicIdWithFolder.substring(0, publicIdWithFolder.lastIndexOf('.'));
    } catch (Exception e) {
      throw new RuntimeException("Failed to extract publicId from image URL: " + imageUrl, e);
    }
  }

  @Override
  public void deleteRoom(Long id) {
    Room room = roomRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

    List<String> imageUrls = room.getListImageUrl();
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

    room.setDeleted(true);
    room.setDeletedAt(new java.util.Date());
    roomRepository.save(room);
  }

  @Override
  public void deleteRooms(List<Long> ids) {
    roomRepository.softDeleteByIds(ids, new java.util.Date());
  }

  @Override
  public void softDeleteRoom(Long id) {
    Room room = roomRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    room.setDeleted(true);
    room.setDeletedAt(new java.util.Date());
    roomRepository.save(room);
  }

  @Override
  public void softDeleteRooms(List<Long> ids) {
    List<Room> existing = roomRepository.findAllByIdInAndIsDeletedFalse(ids);
    java.util.Set<Long> valid = existing.stream().map(Room::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidRoomIdsException("Some room IDs are invalid or already deleted", invalid);
    }
    roomRepository.softDeleteByIds(ids, new java.util.Date());
  }

  @Override
  public void restoreRoom(Long id) {
    Room room = roomRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    room.setDeleted(false);
    room.setDeletedAt(null);
    roomRepository.save(room);
  }

  @Override
  public void restoreRooms(List<Long> ids) {
    List<Room> existing = roomRepository.findAllByIdInAndIsDeletedTrue(ids);
    java.util.Set<Long> valid = existing.stream().map(Room::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidRoomIdsException("Some room IDs are invalid or not deleted", invalid);
    }
    roomRepository.restoreByIds(ids);
  }

  @Override
  public void deleteRoomPermanently(Long id) {
    Room room = roomRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

    List<String> imageUrls = room.getListImageUrl();
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

    roomRepository.delete(room);
  }

  @Override
  public void deleteRoomsPermanently(List<Long> ids) {
    List<Room> list = roomRepository.findAllById(ids);
    java.util.Set<Long> valid = list.stream().map(Room::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidRoomIdsException("Some room IDs are invalid", invalid);
    }
    ids.forEach(id -> {
      Room room = roomRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

      List<String> imageUrls = room.getListImageUrl();
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

      roomRepository.delete(room);
    });
  }

  public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
    Room room = roomRepository.findById(roomId).orElse(null);
    if (room == null || !room.isAvailable()) {
      return false;
    }

    List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
        List.of(roomId), checkIn, checkOut);

    return conflictingBookings.isEmpty();
  }

  public List<Room> getAvailableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
    List<Room> allRooms = roomRepository.findByHotelIdAndIsDeletedFalseAndAvailableTrue(hotelId);

    if (allRooms.isEmpty()) {
      return new ArrayList<>();
    }

    List<Long> roomIds = allRooms.stream().map(Room::getId).collect(Collectors.toList());
    List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
        roomIds, checkIn, checkOut);

    Set<Long> unavailableRoomIds = conflictingBookings.stream()
        .flatMap(booking -> booking.getRooms().stream())
        .map(Room::getId)
        .collect(Collectors.toSet());

    return allRooms.stream()
        .filter(room -> !unavailableRoomIds.contains(room.getId()))
        .collect(Collectors.toList());
  }

  @Override
  public List<RoomResponse> getAvailableRoomsWithHotelName(Long hotelId, LocalDate checkIn,
      LocalDate checkOut) {
    List<Room> availableRooms = getAvailableRooms(hotelId, checkIn, checkOut);
    return availableRooms.stream()
        .map(roomMapper::toRoomResponseDTO)
        .collect(Collectors.toList());
  }

  public List<LocalDate> getUnavailableDates(Long roomId, LocalDate from, LocalDate to) {
    List<Booking> bookings = bookingRepository.findBookingsByRoomIdAndDateRange(
        roomId, from, to);

    List<LocalDate> unavailableDates = new ArrayList<>();

    for (Booking booking : bookings) {
      LocalDate current = booking.getCheckInDate();
      while (!current.isAfter(booking.getCheckOutDate().minusDays(1))) {
        if (!current.isBefore(from) && !current.isAfter(to)) {
          unavailableDates.add(current);
        }
        current = current.plusDays(1);
      }
    }

    return unavailableDates;
  }

  @Transactional
  public boolean reserveRooms(List<Long> roomIds, LocalDate checkIn, LocalDate checkOut) {
    for (Long roomId : roomIds) {
      if (!isRoomAvailable(roomId, checkIn, checkOut)) {
        log.warn("Room {} is not available for dates {} to {}", roomId, checkIn, checkOut);
        return false;
      }
    }
    return true;
  }

  public int getAvailableRoomCount(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
    return getAvailableRooms(hotelId, checkIn, checkOut).size();
  }

  public boolean areRoomsAvailableForBooking(List<Long> roomIds, LocalDate checkIn,
      LocalDate checkOut) {
    for (Long roomId : roomIds) {
      if (!isRoomAvailable(roomId, checkIn, checkOut)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Lấy danh sách room với phân quyền - System Admin & Admin: Xem tất cả - Manager: Chỉ xem room
   * thuộc hotel mình quản lý - Staff: Chỉ xem room thuộc hotel mình làm việc - Guest: Không xem
   * được
   */
  @Override
  public Page<RoomResponse> getAllRoomsWithAuthorization(Pageable pageable, boolean deleted) {
    User currentUser = authorizationUtils.getCurrentUser();

    if (authorizationUtils.canAccessAllData()) {
      // System Admin and Admin can see all rooms
      return getAllRoomsWithHotelName(pageable, deleted);
    } else if (authorizationUtils.isManager()) {
      // Manager can only see rooms from hotels they manage
      Page<Room> rooms = deleted
          ? roomRepository.findAllByHotelManagedByUserAndIsDeletedTrue(currentUser, pageable)
          : roomRepository.findAllByHotelManagedByUserAndIsDeletedFalse(currentUser, pageable);
      return rooms.map(roomMapper::toRoomResponseDTO);
    } else if (authorizationUtils.isStaff()) {
      // Staff can only see rooms from hotels they work at
      // For now, using the same logic as manager since we don't have staff-hotel
      // relationship
      Page<Room> rooms = deleted
          ? roomRepository.findAllByHotelManagedByUserAndIsDeletedTrue(currentUser, pageable)
          : roomRepository.findAllByHotelManagedByUserAndIsDeletedFalse(currentUser, pageable);
      return rooms.map(roomMapper::toRoomResponseDTO);
    } else {
      // Guest or other roles - return empty page
      return Page.empty(pageable);
    }
  }

  /**
   * Lấy room theo ID với phân quyền - System Admin & Admin: Xem tất cả - Manager & Staff: Chỉ xem
   * room thuộc hotel mình quản lý/làm việc - Guest: Không xem được
   */
  @Override
  public Optional<RoomResponse> getRoomByIdWithAuthorization(Long id) {
    User currentUser = authorizationUtils.getCurrentUser();

    if (authorizationUtils.canAccessAllData()) {
      // System Admin and Admin can see all rooms
      return getRoomByIdWithHotelName(id);
    } else if (authorizationUtils.isManager() || authorizationUtils.isStaff()) {
      // Manager and Staff can only see rooms from hotels they manage/work at
      Optional<Room> room = roomRepository.findByIdAndHotelManagedByUserAndIsDeletedFalse(id,
          currentUser);
      return room.map(roomMapper::toRoomResponseDTO);
    } else {
      // Guest or other roles - return empty
      return Optional.empty();
    }
  }

}
