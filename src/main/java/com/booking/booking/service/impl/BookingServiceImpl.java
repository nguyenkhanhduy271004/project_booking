package com.booking.booking.service.impl;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.UserType;
import com.booking.booking.common.VoucherStatus;
import com.booking.booking.dto.BookingNotificationDTO;
import com.booking.booking.dto.request.BookingRequest;
import com.booking.booking.dto.response.BookingResponse;
import com.booking.booking.exception.*;
import com.booking.booking.mapper.BookingMapper;
import com.booking.booking.model.*;
import com.booking.booking.repository.*;
import com.booking.booking.service.interfaces.BookingService;
import com.booking.booking.util.BookingUtil;
import com.booking.booking.util.UserContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    private final BookingUtil bookingUtil;

    @Value("${booking.expiry.minutes:15}")
    private int expiryMinutes;


    @Transactional
    public List<BookingResponse> createBooking(BookingRequest request) {
        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        List<Room> rooms = validateAndFetchRooms(request.getRoomIds(), request.getHotelId());
        validateRoomAvailability(rooms, request.getRoomIds(), request.getCheckInDate(), request.getCheckOutDate());

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            throw new BadRequestException("Checkout date must be after checkin date");
        }

        BigDecimal total = calculateTotalPrice(rooms, nights);

        Voucher voucher = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getVoucherId() != null) {
            voucher = getValidVoucher(request.getVoucherId(), request.getHotelId(), total);
            discount = calculateDiscount(total, voucher);
        }

        BigDecimal finalTotal = total.subtract(discount);

        User currentUser = userContext.getCurrentUser();
        User guest = getBookingGuest(currentUser, request.getGuestId());


        String bookingCode = generateBookingCode(rooms.get(0).getHotel().getName());

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

        if (voucher != null) {
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);
        }

        int updated = roomRepository.markRoomsUnavailableByIds(request.getRoomIds());
        if (updated != request.getRoomIds().size()) {
            throw new BadRequestException("Set unavailable rooms is failed");
        }

        sendBookingNotification(saved, currentUser);

        return List.of(bookingMapper.toBookingResponse(saved));
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "XX";
        StringBuilder sb = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            sb.append(Character.toUpperCase(word.charAt(0)));
        }
        return sb.toString();
    }


    private String generateBookingCode(String hotelName) {
        String initials = getInitials(hotelName);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BK-" + initials + "-" + randomPart;
    }


    private BigDecimal calculateTotalPrice(List<Room> rooms, long nights) {
        return BigDecimal.valueOf(
                rooms.stream().mapToDouble(Room::getPricePerNight).sum()
        ).multiply(BigDecimal.valueOf(nights));
    }

    private Voucher getValidVoucher(Long voucherId, Long hotelId, BigDecimal total) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BadRequestException("Voucher not found"));

        validateVoucherCollect(voucher, hotelId);

        if (voucher.getPriceCondition() != null && total.compareTo(voucher.getPriceCondition()) < 0) {
            throw new BadRequestException("Voucher not eligible (order total below condition)");
        }

        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            throw new BadRequestException("Voucher out of stock");
        }

        return voucher;
    }

    private BigDecimal calculateDiscount(BigDecimal total, Voucher voucher) {
        return total.multiply(BigDecimal.valueOf(voucher.getPercentDiscount()))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }


    private void sendBookingNotification(Booking booking, User currentUser) {
        BookingNotificationDTO dto = new BookingNotificationDTO(booking);
        Long hotelId = booking.getHotel().getId();

        if (currentUser.getType() == UserType.ADMIN || currentUser.getType() == UserType.SYSTEM_ADMIN) {
            messagingTemplate.convertAndSend("/topic/booking/global", dto);
        }
        messagingTemplate.convertAndSend("/topic/booking/hotel/" + hotelId, dto);
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

        User user = userContext.getCurrentUser();

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        for (Room room : rooms) {
            if (!room.isAvailable() && !room.getHeldByUserId().equals(user.getId())) {
                throw new BadRequestException("Room is held by another user.");
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

        return userRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));
    }


    public Page<BookingResponse> getAllBookings(Pageable pageable) {

        User user = userContext.getCurrentUser();

        if (user.getType().equals(UserType.MANAGER) || user.getType().equals(UserType.STAFF)) {


            return bookingRepository.findAllByIsDeletedFalseAndHotel(pageable, user.getHotel())
                    .map(bookingMapper::toBookingResponse);

        }

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

        User user = userContext.getCurrentUser();

        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (user.getType() == UserType.GUEST) {
            if (booking.getGuest() == null || !booking.getGuest().getId().equals(user.getId())) {
                throw new ForBiddenException("You are not allowed to access this booking with id: " + id);
            }
        }

        return bookingMapper.toBookingResponse(booking);
    }


    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + request.getHotelId()));

        List<Room> rooms = roomRepository.findAllById(request.getRoomIds());
        if (rooms.size() != request.getRoomIds().size()) {
            throw new BadRequestException("Some room IDs are invalid");
        }

        for (Room room : rooms) {
            if (!room.getHotel().getId().equals(hotel.getId())) {
                throw new BadRequestException(String.format(
                        "Room with id %s does not belong to hotel with id %s",
                        room.getId(), hotel.getId()));
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
        Set<Long> valid = existing.stream().map(Booking::getId)
                .collect(Collectors.toSet());
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
        Set<Long> valid = list.stream().map(Booking::getId)
                .collect(Collectors.toSet());
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

    @Override
    public void updateStatusBooking(Long id, String Status) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        booking.setStatus(BookingStatus.valueOf(Status.toUpperCase()));
        bookingUtil.handleBookingWithStatus(booking, BookingStatus.valueOf(Status.toUpperCase()));
        bookingRepository.save(booking);
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
