package com.booking.booking.repository;

import com.booking.booking.common.TypeRoom;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    @Modifying
    @Transactional
    @Query("UPDATE Room r SET r.available = false WHERE r.id IN :ids")
    int markRoomsUnavailableByIds(List<Long> ids);

    List<Room> findByHotelIdAndIsDeletedFalse(Long hotelId);

    long countByHotelId(Long hotelId);

    Page<Room> findAllByIsDeletedFalse(Pageable pageable);

    Optional<Room> findByIdAndIsDeletedFalse(Long id);

    List<Room> findAllByIdInAndIsDeletedFalse(List<Long> ids);

    List<Room> findAllByIdInAndIsDeletedTrue(List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Room r SET r.isDeleted = true, r.deletedAt = :deletedAt WHERE r.id IN :ids AND r.isDeleted = false")
    int softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Date deletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Room r SET r.isDeleted = false, r.deletedAt = null WHERE r.id IN :ids AND r.isDeleted = true")
    int restoreByIds(@Param("ids") List<Long> ids);

    Page<Room> findAllByIsDeletedTrue(Pageable pageable);

    Page<Room> findAllByIsDeletedTrueAndHotel(Pageable pageable, Hotel hotel);

    Page<Room> findAllByIsDeletedFalseAndHotel(Pageable pageable, Hotel hotel);


    @Query("""
                SELECT r FROM Room r 
                WHERE r.hotel.id = :hotelId 
                AND r.isDeleted = false 
                AND r.available = true 
                AND r.id NOT IN (
                    SELECT br.id FROM Booking b 
                    JOIN b.rooms br 
                    WHERE b.checkInDate < :checkOut 
                    AND b.checkOutDate > :checkIn
                )
            """)
    List<Room> findAvailableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut);

    @Query("""
                SELECT r FROM Room r
                WHERE r.available = false
                  AND r.holdExpiresAt < :now
                  AND NOT EXISTS (
                    SELECT b FROM Booking b
                    JOIN b.rooms br
                    WHERE br = r
                      AND b.status IN ('PAYING', 'CONFIRMED', 'CHECKIN')
                  )
            """)
    List<Room> findRoomsToRelease(@Param("now") LocalDateTime now);

    Long countByIsDeletedFalse();

    Long countByTypeRoomAndIsDeletedFalse(TypeRoom typeRoom);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id IN :roomIds")
    List<Room> lockRoomsForUpdate(@Param("roomIds") List<Long> roomIds);

}
