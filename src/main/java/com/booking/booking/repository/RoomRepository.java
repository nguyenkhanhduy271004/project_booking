package com.booking.booking.repository;

import com.booking.booking.model.Room;
import com.booking.booking.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    @Modifying
    @Transactional
    @Query("UPDATE Room r SET r.available = false WHERE r.id IN :ids")
    int markRoomsUnavailableByIds(List<Long> ids);

    List<Room> findByHotelIdAndIsDeletedFalse(Long hotelId);
    
    List<Room> findByHotelId(Long hotelId);

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

    @Query("SELECT r FROM Room r WHERE r.hotel.manager = :managedByUser AND r.isDeleted = false")
    Page<Room> findAllByHotelManagedByUserAndIsDeletedFalse(
            @Param("managedByUser") User managedByUser,
            Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.hotel.manager = :managedByUser AND r.isDeleted = true")
    Page<Room> findAllByHotelManagedByUserAndIsDeletedTrue(
            @Param("managedByUser") User managedByUser,
            Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.hotel.manager = :managedByUser AND r.id = :id AND r.isDeleted = false")
    Optional<Room> findByIdAndHotelManagedByUserAndIsDeletedFalse(
            @Param("id") Long id,
            @Param("managedByUser") User managedByUser);

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

}
