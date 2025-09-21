package com.booking.booking.service.impl;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.UserType;
import com.booking.booking.common.VoucherStatus;
import com.booking.booking.dto.request.BookingRequest;
import com.booking.booking.dto.response.BookingResponse;
import com.booking.booking.dto.BookingNotificationDTO;
import com.booking.booking.exception.AccessDeniedException;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.InvalidBookingIdsException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.mapper.BookingMapper;
import com.booking.booking.model.Booking;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.model.User;
import com.booking.booking.model.Voucher;
import com.booking.booking.repository.BookingRepository;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.repository.VoucherRepository;
import com.booking.booking.service.BookingService;
import com.booking.booking.util.UserContext;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "Booking-Service")
public class BookingServiceImpl implements BookingService {

  private final SimpMessagingTemplate messagingTemplate;
  private final BookingRepository bookingRepository;
  private final VoucherRepository voucherRepository;
  private final HotelRepository hotelRepository;
  private final RoomRepository roomRepository;
  private final UserRepository userRepository;
  private final BookingMapper bookingMapper;
  private final UserContext userContext;

  @Value("${booking.expiry.minutes:15}")
  private int expiryMinutes;

  public List<BookingResponse> createBooking(BookingRequest request) {
    User currentUser = userContext.getCurrentUser();
    Voucher voucher = voucherRepository.findById(request.getVoucherId()).orElse(null);
    validateVoucherCollect(voucher, request.getHotelId());

    validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

    List<Room> rooms = validateAndFetchRooms(request.getRoomIds(), request.getHotelId());
    validateRoomAvailability(rooms, request.getRoomIds(), request.getCheckInDate(),
        request.getCheckOutDate());

    long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

    BigDecimal total = rooms.stream()
        .map(room -> BigDecimal.valueOf(room.getPricePerNight()))
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .multiply(BigDecimal.valueOf(nights));

    if (total.compareTo(voucher.getPriceCondition()) < 0) {
      throw new BadRequestException("Voucher not eligible!");
    }

    BigDecimal discount = total.multiply(BigDecimal.valueOf(voucher.getPercentDiscount()))
        .divide(BigDecimal.valueOf(100));
    BigDecimal finalTotal = total.subtract(discount);

    User guest = getBookingGuest(currentUser, request.getGuestId());

    String bookingCode = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    Booking booking = Booking.builder()
        .bookingCode(bookingCode)
        .hotel(rooms.get(0).getHotel())
        .legacyRoomId(rooms.get(0).getId())
        .rooms(new ArrayList<>(rooms))
        .checkInDate(request.getCheckInDate())
        .checkOutDate(request.getCheckOutDate())
        .totalPrice(finalTotal)
        .status(BookingStatus.PENDING)
        .paymentType(request.getPaymentType())
        .notes(request.getNotes())
        .guest(guest)
        .build();

    Booking saved = bookingRepository.save(booking);
    BookingResponse response = bookingMapper.toBookingResponse(saved);

    voucher.setQuantity(voucher.getQuantity() - 1);
    voucherRepository.save(voucher);

    BookingNotificationDTO dto = new BookingNotificationDTO(saved);
    Long hotelId = saved.getHotel().getId();

    if (currentUser.getType() == UserType.ADMIN || currentUser.getType() == UserType.SYSTEM_ADMIN) {
      messagingTemplate.convertAndSend("/topic/booking/global", dto);
    }

    messagingTemplate.convertAndSend("/topic/booking/hotel/" + hotelId, dto);

    return List.of(response);
  }

  private void validateVoucherCollect(Voucher voucher, Long hotelId) {
    List<String> errors = new ArrayList<>();
    if (voucher == null) {
      errors.add("Voucher does not exist or code is invalid");
    }
    if (voucher != null) {
      if (voucher.getStatus() != VoucherStatus.ACTIVE) {
        errors.add("Voucher is not active. Current status: " + voucher.getStatus());
      }
      if (voucher.getQuantity() <= 0) {
        errors.add("Voucher has no remaining quantity");
      }
      if (voucher.getExpiredDate().isBefore(LocalDate.now())) {
        errors.add("Voucher expired on " + voucher.getExpiredDate());
      }
      if (!voucher.getHotel().getId().equals(hotelId)) {
        errors.add("Voucher does not apply to hotel with id " + hotelId);
      }
    }
    if (!errors.isEmpty()) {
      throw new BadRequestException(String.join("; ", errors));
    }
  }


  private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
      throw new BadRequestException("Check-out date must be after check-in date");
    }

    long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    if (nights <= 0) {
      throw new BadRequestException("Booking must be at least 1 night");
    }
    if (nights > 30) {
      throw new BadRequestException("Booking period cannot exceed 30 days");
    }
  }

  private List<Room> validateAndFetchRooms(List<Long> roomIds, Long hotelId) {
    if (roomIds == null || roomIds.isEmpty()) {
      throw new BadRequestException("Room IDs must not be empty");
    }

    List<Room> rooms = roomRepository.findAllById(roomIds);
    if (rooms.size() != roomIds.size()) {
      Set<Long> foundIds = rooms.stream().map(Room::getId).collect(Collectors.toSet());
      List<Long> invalid = roomIds.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new BadRequestException("Some room IDs are invalid: " + invalid);
    }

    for (Room room : rooms) {
      if (!room.getHotel().getId().equals(hotelId)) {
        throw new BadRequestException(
            "Room id " + room.getId() + " does not belong to hotel id " + hotelId);
      }
    }

    return rooms;
  }


  private void validateRoomAvailability(List<Room> rooms, List<Long> roomIds, LocalDate checkIn,
      LocalDate checkOut) {
    for (Room room : rooms) {
      if (!room.isAvailable()) {
        throw new BadRequestException("Room " + room.getId() + " is not available for booking");
      }
    }

    List<Booking> conflicting = bookingRepository.findConflictingBookings(roomIds, checkIn,
        checkOut);
    if (!conflicting.isEmpty()) {
      List<Long> conflictRoomIds = conflicting.stream()
          .flatMap(b -> b.getRooms().stream())
          .map(Room::getId)
          .distinct()
          .toList();
      throw new BadRequestException(
          "Some rooms are not available for the selected dates: " + conflictRoomIds);
    }
  }

  private User getBookingGuest(User currentUser, Long guestId) {
    if (currentUser.getType() == UserType.GUEST) {
      return currentUser;
    }

    if (guestId == null) {
      throw new BadRequestException("Guest ID must not be null for non-GUEST users");
    }

    User guest = userRepository.findById(guestId)
        .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));

//    if (guest.getType() != UserType.GUEST) {
//      throw new BadRequestException("Specified user is not a guest. User type: " + guest.getType());
//    }

    return guest;
  }


  public Page<BookingResponse> getAllBookings(Pageable pageable) {
    return bookingRepository.findAllByIsDeletedFalse(pageable)
        .map(bookingMapper::toBookingResponse);
  }

  @Override
  public Page<BookingResponse> getAllBookings(Pageable pageable, boolean deleted) {
    Page<Booking> page = deleted
        ? bookingRepository.findAllByIsDeletedTrue(pageable)
        : bookingRepository.findAllByIsDeletedFalse(pageable);
    return page.map(bookingMapper::toBookingResponse);
  }

  public BookingResponse getBookingById(Long id) {
    Booking booking = bookingRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    return bookingMapper.toBookingResponse(booking);
  }

  public BookingResponse updateBooking(Long id, BookingRequest request) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

    Hotel hotel = hotelRepository.findById(request.getHotelId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Hotel not found with id: " + request.getHotelId()));

    if (request.getRoomIds() == null || request.getRoomIds().isEmpty()) {
      throw new BadRequestException("Room IDs must not be empty");
    }

    List<Room> rooms = roomRepository.findAllById(request.getRoomIds());
    if (rooms.size() != request.getRoomIds().size()) {
      throw new BadRequestException("Some room IDs are invalid");
    }

    for (Room room : rooms) {
      if (!hotel.getRooms().contains(room)) {
        throw new ResourceNotFoundException(String.format(
            "Hotel with id %s does not contain room with id %s",
            request.getHotelId(), room.getId()));
      }
    }

    bookingMapper.toBooking(booking, request, hotel, rooms);

    Booking updated = bookingRepository.save(booking);
    return bookingMapper.toBookingResponse(updated);
  }

  public void deleteBooking(Long id) {
    Booking booking = bookingRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    booking.setDeleted(true);
    booking.setDeletedAt(new Date());
    bookingRepository.save(booking);
  }

  @Override
  @Transactional
  public void deleteBookings(List<Long> ids) {
    List<Booking> existing = bookingRepository.findAllByIdInAndIsDeletedFalse(ids);
    java.util.Set<Long> valid = existing.stream().map(Booking::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidBookingIdsException("Some booking IDs are invalid or already deleted",
          invalid);
    }
    bookingRepository.softDeleteByIds(ids, new Date());
  }

  @Override
  public void restoreBooking(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    booking.setDeleted(false);
    booking.setDeletedAt(null);
    bookingRepository.save(booking);
  }

  @Override
  @Transactional
  public void restoreBookings(List<Long> ids) {
    List<Booking> existing = bookingRepository.findAllByIdInAndIsDeletedTrue(ids);
    java.util.Set<Long> valid = existing.stream().map(Booking::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidBookingIdsException("Some booking IDs are invalid or not deleted", invalid);
    }
    bookingRepository.restoreByIds(ids);
  }

  @Override
  public void deleteBookingPermanently(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    bookingRepository.delete(booking);
  }

  @Override
  public void cancelBooking(Long id) {
    Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

    User user = userContext.getCurrentUser();

    if (!booking.getGuest().getId().equals(user.getId())) {
      throw new AccessDeniedException("You are not allowed to cancel a booking that is not yours");
    }

    booking.setStatus(BookingStatus.CANCELLED);
    bookingRepository.save(booking);
  }

  @Override
  @Transactional
  public void deleteBookingsPermanently(List<Long> ids) {
    List<Booking> list = bookingRepository.findAllById(ids);
    java.util.Set<Long> valid = list.stream().map(Booking::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<Long> invalid = ids.stream().filter(id -> !valid.contains(id)).toList();
    if (!invalid.isEmpty()) {
      throw new InvalidBookingIdsException("Some booking IDs are invalid", invalid);
    }
    bookingRepository.deleteAll(list);
  }

  @Override
  public List<BookingResponse> getAllBookingsByGuestId() {
    User user = userContext.getCurrentUser();

    List<Booking> bookingList = bookingRepository.findBookingEntitiesByGuestId(user.getId());

    List<BookingResponse> result = new ArrayList<>();
    for (Booking booking : bookingList) {
      BookingResponse response = bookingMapper.toBookingResponse(booking);

      result.add(response);
    }

    return result;
  }

  @Override
  public Page<BookingResponse> getMyBookings(Pageable pageable) {
    User currentUser = userContext.getCurrentUser();
    return bookingRepository.findByGuestIdAndIsDeletedFalse(currentUser.getId(), pageable)
        .map(bookingMapper::toBookingResponse);
  }

  @Override
  public BookingResponse getMyBookingById(Long id) {
    User currentUser = userContext.getCurrentUser();
    Booking booking = bookingRepository.findByIdAndGuestIdAndIsDeletedFalse(id,
            currentUser.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found or access denied"));
    return bookingMapper.toBookingResponse(booking);
  }

  @Override
  public Page<BookingResponse> getHistoryBookingByRoomId(Long roomId, LocalDate from, LocalDate to, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Booking> result = bookingRepository.findHistoryByRoomId(roomId, from, to, pageable);
    return result.map(bookingMapper::toBookingResponse);
  }

  @Scheduled(fixedRate = 300000)
  @Transactional
  public void expirePendingBookings() {
    LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(expiryMinutes);
    Date expiredDate = Date.from(expiredBefore.atZone(ZoneId.systemDefault()).toInstant());

    List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(expiredDate);

    if (!expiredBookings.isEmpty()) {
      log.info("Found {} expired pending bookings", expiredBookings.size());

      for (Booking booking : expiredBookings) {
        booking.setStatus(BookingStatus.EXPIRED);
        log.info("Expired booking: {} for guest: {}", booking.getBookingCode(),
            booking.getGuest().getUsername());
      }

      bookingRepository.saveAll(expiredBookings);
      log.info("Successfully expired {} bookings", expiredBookings.size());
    }
  }
}
