package com.booking.booking.repository;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.model.Booking;
import com.booking.booking.model.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {

    List<Booking> findBookingEntitiesByGuestId(Long guestId);

    Page<Booking> findAllByIsDeletedFalse(Pageable pageable);

    Page<Booking> findAllByIsDeletedFalseAndHotel(Pageable pageable, Hotel hotel);

    Optional<Booking> findByIdAndIsDeletedFalse(Long id);

    Page<Booking> findAllByIsDeletedTrue(Pageable pageable);

    List<Booking> findAllByIdInAndIsDeletedFalse(List<Long> ids);

    List<Booking> findAllByIdInAndIsDeletedTrue(List<Long> ids);

    @Query("SELECT DISTINCT b FROM Booking b JOIN b.rooms r WHERE r.id IN :roomIds AND b.isDeleted = false AND b.status NOT IN ('CANCELLED', 'COMPLETED', 'EXPIRED') AND ((b.checkInDate <= :checkOut AND b.checkOutDate > :checkIn))")
    List<Booking> findConflictingBookings(@Param("roomIds") List<Long> roomIds,
                                          @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);

    Optional<Booking> findByBookingCode(String bookingCode);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.createdAt < :expiredBefore")
    List<Booking> findExpiredPendingBookings(@Param("expiredBefore") Date expiredBefore);

    Page<Booking> findByGuestIdAndIsDeletedFalse(Long guestId, Pageable pageable);

    Optional<Booking> findByIdAndGuestIdAndIsDeletedFalse(Long id, Long guestId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.isDeleted = true, b.deletedAt = :deletedAt WHERE b.id IN :ids AND b.isDeleted = false")
    int softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Date deletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.isDeleted = false, b.deletedAt = null WHERE b.id IN :ids AND b.isDeleted = true")
    int restoreByIds(@Param("ids") List<Long> ids);

    @Query("""
                select distinct b
                from Booking b
                join b.rooms r
                where r.id = :roomId
                  and (:from is null or b.checkInDate >= :from)
                  and (:to is null or b.checkOutDate <= :to)
                order by b.checkInDate desc, b.id desc
            """)
    Page<Booking> findHistoryByRoomId(
            @Param("roomId") Long roomId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    Long countByStatus(BookingStatus status);

    Long countByCreatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status IN :statuses")
    Double sumTotalPriceByStatusIn(@Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status IN :statuses AND b.createdAt BETWEEN :startDate AND :endDate")
    Double sumTotalPriceByStatusInAndCreatedAtBetween(@Param("statuses") List<BookingStatus> statuses,
                                                      @Param("startDate") java.time.LocalDateTime startDate,
                                                      @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT COALESCE(AVG(b.totalPrice), 0) FROM Booking b WHERE b.status IN (com.booking.booking.common.BookingStatus.COMPLETED, com.booking.booking.common.BookingStatus.CONFIRMED)")
    Double findAverageOrderValue();

    @Query("SELECT b FROM Booking b JOIN b.rooms r " +
            "WHERE r.id = :roomId " +
            "AND b.status NOT IN ('CANCELLED', 'FAILED')")
    List<Booking> findBookingsByRoomId(@Param("roomId") Long roomId);

    List<Booking> findByStatusAndPaymentExpiredAtBefore(BookingStatus status, Instant instant);
}
