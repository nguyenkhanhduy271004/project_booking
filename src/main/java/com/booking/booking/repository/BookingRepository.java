package com.booking.booking.repository;

import com.booking.booking.model.Booking;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>,
                JpaSpecificationExecutor<Booking> {

        List<Booking> findBookingEntitiesByGuestId(Long guestId);

        // Queries for soft-delete awareness
        org.springframework.data.domain.Page<Booking> findAllByIsDeletedFalse(
                        org.springframework.data.domain.Pageable pageable);

        Optional<Booking> findByIdAndIsDeletedFalse(Long id);

        org.springframework.data.domain.Page<Booking> findAllByIsDeletedTrue(
                        org.springframework.data.domain.Pageable pageable);

        List<Booking> findAllByIdInAndIsDeletedFalse(List<Long> ids);

        List<Booking> findAllByIdInAndIsDeletedTrue(List<Long> ids);

        @Query("SELECT DISTINCT b FROM Booking b JOIN b.rooms r WHERE r.id IN :roomIds AND b.isDeleted = false AND b.status NOT IN ('CANCELLED', 'COMPLETED')")
        List<Booking> findActiveBookingsByRoomIds(@Param("roomIds") List<Long> roomIds);

        @Query("SELECT DISTINCT b FROM Booking b JOIN b.rooms r WHERE r.id IN :roomIds AND b.isDeleted = false AND b.status NOT IN ('CANCELLED', 'COMPLETED', 'EXPIRED') AND ((b.checkInDate <= :checkOut AND b.checkOutDate > :checkIn))")
        List<Booking> findConflictingBookings(@Param("roomIds") List<Long> roomIds,
                        @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);

        Optional<Booking> findByBookingCode(String bookingCode);

        @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.createdAt < :expiredBefore")
        List<Booking> findExpiredPendingBookings(@Param("expiredBefore") Date expiredBefore);

        org.springframework.data.domain.Page<Booking> findByGuestIdAndIsDeletedFalse(Long guestId,
                        org.springframework.data.domain.Pageable pageable);

        Optional<Booking> findByIdAndGuestIdAndIsDeletedFalse(Long id, Long guestId);

        @Query("SELECT DISTINCT b FROM Booking b JOIN b.rooms r WHERE r.id = :roomId AND b.isDeleted = false AND b.status NOT IN ('CANCELLED', 'COMPLETED', 'EXPIRED') AND ((b.checkInDate <= :toDate AND b.checkOutDate > :fromDate))")
        List<Booking> findBookingsByRoomIdAndDateRange(@Param("roomId") Long roomId,
                        @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

        @Modifying(clearAutomatically = true)
        @Query("UPDATE Booking b SET b.isDeleted = true, b.deletedAt = :deletedAt WHERE b.id IN :ids AND b.isDeleted = false")
        int softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Date deletedAt);

        @Modifying(clearAutomatically = true)
        @Query("UPDATE Booking b SET b.isDeleted = false, b.deletedAt = null WHERE b.id IN :ids AND b.isDeleted = true")
        int restoreByIds(@Param("ids") List<Long> ids);

        @org.springframework.transaction.annotation.Transactional
        @Modifying(clearAutomatically = true)
        @Query("DELETE FROM Booking b WHERE EXISTS (SELECT r FROM b.rooms r WHERE r.id IN :roomIds)")
        int deleteByRoomIds(@Param("roomIds") List<Long> roomIds);

        @org.springframework.transaction.annotation.Transactional
        @Modifying(clearAutomatically = true)
        @Query("DELETE FROM Booking b WHERE b.hotel.id IN :hotelIds")
        int deleteByHotelIds(@Param("hotelIds") List<Long> hotelIds);
}
