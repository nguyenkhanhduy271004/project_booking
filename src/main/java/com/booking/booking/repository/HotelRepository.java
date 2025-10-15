package com.booking.booking.repository;

import com.booking.booking.model.Hotel;
import com.booking.booking.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long>,
        JpaSpecificationExecutor<Hotel> {

    @Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.manager m LEFT JOIN FETCH m.roles WHERE h.isDeleted = false")
    Page<Hotel> findAllByIsDeletedFalse(Pageable pageable);

    @Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.manager m LEFT JOIN FETCH m.roles WHERE h.id = :id AND h.isDeleted = false")
    Optional<Hotel> findByIdAndIsDeletedFalse(@Param("id") Long id);

    List<Hotel> findAllByIdInAndIsDeletedFalse(List<Long> ids);

    List<Hotel> findAllByIdInAndIsDeletedTrue(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Hotel h SET h.isDeleted = true, h.deletedAt = :deletedAt WHERE h.id IN :ids AND h.isDeleted = false")
    int softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Date deletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Hotel h SET h.isDeleted = false, h.deletedAt = null WHERE h.id IN :ids AND h.isDeleted = true")
    int restoreByIds(@Param("ids") List<Long> ids);

    Page<Hotel> findAllByIsDeletedTrue(Pageable pageable);

    Optional<Hotel> findByManagerAndIsDeletedFalse(User manager);

    Long countByIsDeletedFalse();

    @Query(value = """
            SELECT h.id, h.name, COUNT(b.id) as bookingCount, 
                   COALESCE(SUM(b.total_price), 0) as totalRevenue,
                   0 as averageRating,
                   ROUND((COUNT(b.id) * 100.0 / NULLIF(h.total_rooms * :months, 0)), 2) as occupancyRate
            FROM tbl_hotel h
            LEFT JOIN tbl_booking b ON b.hotel_id = h.id 
                AND b.status IN ('COMPLETED', 'CONFIRMED')
                AND b.created_at >= :startDate
            WHERE h.is_deleted = false
            GROUP BY h.id, h.name, h.total_rooms
            ORDER BY bookingCount DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object> findTopHotelsByBookingCount(@Param("limit") int limit,
                                             @Param("months") int months,
                                             @Param("startDate") LocalDateTime startDate);

    @Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.rooms")
    List<Hotel> findAllWithRooms();

    @Query("""
                SELECT DISTINCT h FROM Hotel h
                JOIN h.rooms r
                WHERE
                    (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                     OR LOWER(h.district) LIKE LOWER(CONCAT('%', :keyword, '%'))
                     OR LOWER(h.province) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND r.available = true
                    AND r.capacity >= :numberOfGuests
                    AND (r.holdExpiresAt IS NULL OR r.holdExpiresAt < CURRENT_TIMESTAMP)
                    AND NOT EXISTS (
                        SELECT b FROM Booking b
                        JOIN b.rooms br
                        WHERE br.id = r.id
                        AND b.status NOT IN ('CANCELLED', 'FAILED')
                        AND (
                            b.checkInDate < :checkOut AND b.checkOutDate > :checkIn
                        )
                    )
                GROUP BY h
                HAVING COUNT(r.id) >= :numberOfRooms
            """)
    List<Hotel> searchAvailableHotels(
            @Param("keyword") String keyword,
            @Param("numberOfGuests") int numberOfGuests,
            @Param("numberOfRooms") int numberOfRooms,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );


}

