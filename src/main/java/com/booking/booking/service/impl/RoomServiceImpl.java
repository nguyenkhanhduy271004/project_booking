package com.booking.booking.service.impl;

import com.booking.booking.common.UserType;
import com.booking.booking.dto.RoomDTO;
import com.booking.booking.dto.response.RoomResponse;
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
import com.booking.booking.service.interfaces.RoomService;
import com.booking.booking.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final KafkaTemplate<String, String> kafkaTemplate;


    @Override
    public Page<RoomResponse> getAllRoomsWithHotelName(Pageable pageable, boolean deleted) {
        User user = userContext.getCurrentUser();
        if (user.getType().equals(UserType.MANAGER) || user.getType().equals(UserType.STAFF)) {

            Page<Room> rooms = deleted ? roomRepository.findAllByIsDeletedTrueAndHotel(pageable, user.getHotel())
                    : roomRepository.findAllByIsDeletedFalseAndHotel(pageable, user.getHotel());
            return rooms.map(roomMapper::toRoomResponseDTO);

        }
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
    public RoomResponse createRoom(RoomDTO room, MultipartFile[] imagesRoom) {
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
        roomEntity.setCreatedBy(userContext.getCurrentUser());
        roomEntity.setHotel(existHotel);

        roomRepository.save(roomEntity);
        return roomMapper.toRoomResponseDTO(roomEntity);
    }

    @Override
    public RoomResponse updateRoom(Long id, RoomDTO updatedRoom, MultipartFile[] images, String keepImagesJson) {
        Room room = roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        room.setTypeRoom(updatedRoom.getTypeRoom());
        room.setCapacity(updatedRoom.getCapacity());
        room.setServices(updatedRoom.getServices());
        room.setAvailable(updatedRoom.isAvailable());
        room.setUpdatedBy(userContext.getCurrentUser());
        room.setPricePerNight(updatedRoom.getPricePerNight());

        if (images != null && images.length > 0) {
            List<String> finalImageList = new ArrayList<>();

            if (keepImagesJson != null && !keepImagesJson.trim().isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    List<String> keepImages = objectMapper.readValue(keepImagesJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

                    List<String> currentImages = room.getListImageUrl();
                    if (currentImages != null) {
                        for (String keepImage : keepImages) {
                            if (currentImages.contains(keepImage)) {
                                finalImageList.add(keepImage);
                            }
                        }
                    }

                    log.info("Keeping {} old images", finalImageList.size());
                } catch (Exception e) {
                    log.error("Failed to parse keepImages JSON: {}", keepImagesJson, e);
                }
            }

            List<String> newImageUrls = uploadImages(images);
            finalImageList.addAll(newImageUrls);

            log.info("Final image list: {} old + {} new = {} total",
                    finalImageList.size() - newImageUrls.size(),
                    newImageUrls.size(),
                    finalImageList.size());

            List<String> currentImages = room.getListImageUrl();
            if (currentImages != null) {
                List<String> imagesToDelete = new ArrayList<>();
                for (String currentImage : currentImages) {
                    if (!finalImageList.contains(currentImage)) {
                        imagesToDelete.add(currentImage);
                    }
                }
                if (!imagesToDelete.isEmpty()) {
                    log.info("Deleting {} unused images", imagesToDelete.size());
                    deleteImages(imagesToDelete);
                }
            }

            room.setListImageUrl(finalImageList);
        }

        return roomMapper.toRoomResponseDTO(roomRepository.save(room));
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
                .collect(Collectors.toList());
    }

    private void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        imageUrls.forEach(url -> {
            try {
                String publicId = extractPublicIdFromUrl(url);
                if (publicId != null) {
                    kafkaTemplate.send("delete-image", publicId)
                            .whenComplete((result, ex) -> {
                                if (ex == null) {
                                    log.info("Image deleted successfully: {}", publicId);
                                } else {
                                    log.error("Failed to delete image {}: {}", publicId, ex.getMessage());
                                }
                            });
                }
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
            log.error("Failed to extract public_id from URL: {}", imageUrl, e);
            return null;
        }
    }

    @Override
    public void softDeleteRoom(Long id) {
        Room room = roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

        LocalDate today = LocalDate.now();
        List<Booking> activeBookings = bookingRepository.findConflictingBookings(
                List.of(room.getId()), today, today);

        if (!activeBookings.isEmpty()) {
            throw new BadRequestException("Room with id " + id + " currently has active bookings and cannot be deleted");
        }

        deleteImages(room.getListImageUrl());
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

        LocalDate today = LocalDate.now();
        List<Booking> activeBookings = bookingRepository.findConflictingBookings(ids, today, today);

        if (!activeBookings.isEmpty()) {
            List<Long> busyRoomIds = activeBookings.stream()
                    .flatMap(b -> b.getRooms().stream())
                    .map(Room::getId)
                    .filter(ids::contains)
                    .distinct()
                    .toList();

            throw new BadRequestException(
                    "Some rooms cannot be deleted because they have active bookings: " + busyRoomIds
            );
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

    @Override
    @Transactional
    public void updateStatusRoom(List<Long> ids, Boolean status) {
        List<Room> rooms = roomRepository.findAllById(ids);

        if (rooms.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more room IDs not found");
        }

        rooms.forEach(room -> room.setAvailable(status));

        roomRepository.saveAll(rooms);
    }

    @Transactional
    public void holdRooms(List<Long> roomIds) {
        User user = userContext.getCurrentUser();
        List<Room> rooms = roomRepository.lockRoomsForUpdate(roomIds);

        LocalDateTime now = LocalDateTime.now();

        for (Room room : rooms) {
            if (room.getHeldByUserId() != null &&
                    room.getHoldUntil() != null &&
                    room.getHoldUntil().isAfter(now)) {
                throw new BadRequestException("Phòng " + room.getId() + " đang bị người khác giữ!");
            }

            room.setHeldByUserId(user.getId());
            room.setHoldUntil(now.plusMinutes(10));
        }

        roomRepository.saveAll(rooms);
    }


    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void clearExpiredRoomHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Room> expiredRooms = roomRepository.findRoomsToRelease(now);

        expiredRooms.forEach(room -> {
            room.setAvailable(true);
            room.setHoldExpiresAt(null);
        });

        roomRepository.saveAll(expiredRooms);
    }


}
