package com.booking.booking.service.impl;

import com.booking.booking.controller.response.RoomResponse;
import com.booking.booking.dto.RoomDTO;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.ForBiddenException;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private final RoomRepository roomRepository;
  private final HotelRepository hotelRepository;
  private final BookingRepository bookingRepository;
  private final CloudinaryService cloudinaryService;
  private final AuthorizationUtils authorizationUtils;
  private final KafkaTemplate<String, String> kafkaTemplate;


  @Override
  public Page<Room> getAllRooms(Pageable pageable) {
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
        () -> new ResourceNotFoundException("Hotel not found with id: " + room.getHotelId())
    );

    long currentRoomCount = roomRepository.countByHotelId(existHotel.getId());
    if (currentRoomCount >= existHotel.getTotalRooms()) {
      throw new BadRequestException("Number of rooms exceeds total allowed rooms for the hotel");
    }

    List<String> listImageUrl = uploadImages(imagesRoom);
    room.setListImageUrl(listImageUrl);

    Room roomEntity = roomMapper.toRoom(room);
    roomEntity.setServices(room.getServices());
    roomEntity.setCreatedByUser(userContext.getCurrentUser());

    return roomRepository.save(roomEntity);
  }

  @Override
  public RoomResponse createRoomWithHotelName(RoomDTO room, MultipartFile[] imagesRoom) {
    return roomMapper.toRoomResponseDTO(createRoom(room, imagesRoom));
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
      deleteImages(room.getListImageUrl());
      room.setListImageUrl(uploadImages(images));
    }

    return roomRepository.save(room);
  }

  @Override
  public RoomResponse updateRoomWithHotelName(Long id, RoomDTO updatedRoom,
      MultipartFile[] images) {
    return roomMapper.toRoomResponseDTO(updateRoom(id, updatedRoom, images));
  }


  private List<String> uploadImages(MultipartFile[] images) {
    if (images == null || images.length == 0) {
      return Collections.emptyList();
    }

    return Stream.of(images)
        .parallel()
        .map(image -> {
          try {
            Map<String, Object> data = cloudinaryService.upload(image);
            return (String) data.get("secure_url");
          } catch (Exception e) {
            throw new BadRequestException("Failed to upload image: " + image.getOriginalFilename());
          }
        })
        .toList();
  }

  private void deleteImages(List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }

    imageUrls.forEach(url -> {
      try {
        String publicId = extractPublicIdFromUrl(url);

        kafkaTemplate.send("delete-image", publicId)
            .whenComplete((result, ex) -> {
              if (ex == null) {
                log.info("Image deleted successfully: {}", publicId);
              } else {
                log.error("Failed to delete image {}: {}", publicId, ex.getMessage());
              }
            });

      } catch (Exception e) {
        log.warn("Failed to process image deletion: {}", url, e);
      }
    });
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
    deleteImages(room.getListImageUrl());
    room.setDeleted(true);
    room.setDeletedAt(new Date());
    roomRepository.save(room);
  }

  @Override
  public void deleteRooms(List<Long> ids) {
    roomRepository.softDeleteByIds(ids, new Date());
  }

  @Override
  public void softDeleteRoom(Long id) {
    Room room = roomRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    room.setDeleted(true);
    room.setDeletedAt(new Date());
    roomRepository.save(room);
  }

  @Override
  public void softDeleteRooms(List<Long> ids) {
    List<Room> existing = roomRepository.findAllByIdInAndIsDeletedFalse(ids);
    Set<Long> valid = existing.stream().map(Room::getId).collect(Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidRoomIdsException("Some room IDs are invalid or already deleted", invalid);
    }
    roomRepository.softDeleteByIds(ids, new Date());
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
    Set<Long> valid = existing.stream().map(Room::getId).collect(Collectors.toSet());
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
    deleteImages(room.getListImageUrl());
    roomRepository.delete(room);
  }

  @Override
  public void deleteRoomsPermanently(List<Long> ids) {
    List<Room> list = roomRepository.findAllById(ids);
    Set<Long> valid = list.stream().map(Room::getId).collect(Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidRoomIdsException("Some room IDs are invalid", invalid);
    }
    list.forEach(room -> {
      deleteImages(room.getListImageUrl());
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
    return roomRepository.findAvailableRooms(hotelId, checkIn, checkOut);
  }

  @Override
  public List<RoomResponse> getAvailableRoomsWithHotelName(Long hotelId, LocalDate checkIn,
      LocalDate checkOut) {
    return getAvailableRooms(hotelId, checkIn, checkOut)
        .stream()
        .map(roomMapper::toRoomResponseDTO)
        .toList();
  }

  @Override
  public List<LocalDate> getUnavailableDates(Long roomId, LocalDate from, LocalDate to) {
    return List.of();
  }


  @Transactional
  public boolean reserveRooms(List<Long> roomIds, LocalDate checkIn, LocalDate checkOut) {
    List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(roomIds, checkIn,
        checkOut);
    if (!conflictingBookings.isEmpty()) {
      log.warn("Some rooms are not available for {} to {}", checkIn, checkOut);
      return false;
    }
    return true;
  }

  public int getAvailableRoomCount(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
    return getAvailableRooms(hotelId, checkIn, checkOut).size();
  }

  public boolean areRoomsAvailableForBooking(List<Long> roomIds, LocalDate checkIn,
      LocalDate checkOut) {
    return roomIds.stream().allMatch(id -> isRoomAvailable(id, checkIn, checkOut));
  }


  @Override
  public Page<RoomResponse> getAllRoomsWithAuthorization(Pageable pageable, boolean deleted) {
    User currentUser = authorizationUtils.getCurrentUser();

    if (authorizationUtils.canAccessAllData()) {
      return getAllRoomsWithHotelName(pageable, deleted);
    } else if (authorizationUtils.isManager() || authorizationUtils.isStaff()) {
      Page<Room> rooms = deleted
          ? roomRepository.findAllByHotelManagedByUserAndIsDeletedTrue(currentUser, pageable)
          : roomRepository.findAllByHotelManagedByUserAndIsDeletedFalse(currentUser, pageable);
      return rooms.map(roomMapper::toRoomResponseDTO);
    } else {
      throw new ForBiddenException("You do not have permission to access these rooms");
    }
  }

  @Override
  public Optional<RoomResponse> getRoomByIdWithAuthorization(Long id) {
    User currentUser = authorizationUtils.getCurrentUser();

    if (authorizationUtils.canAccessAllData()) {
      return getRoomByIdWithHotelName(id);
    } else if (authorizationUtils.isManager() || authorizationUtils.isStaff()) {
      return roomRepository.findByIdAndHotelManagedByUserAndIsDeletedFalse(id, currentUser)
          .map(roomMapper::toRoomResponseDTO);
    } else {
      throw new ForBiddenException("You do not have permission to access this room");
    }
  }
}
