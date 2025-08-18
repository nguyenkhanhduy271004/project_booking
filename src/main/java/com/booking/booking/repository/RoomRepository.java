package com.booking.booking.repository;

import com.booking.booking.model.Room;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
        List<Room> findByHotelId(Long hotelId);

        long countByHotelId(Long hotelId);

        org.springframework.data.domain.Page<Room> findAllByIsDeletedFalse(
                        org.springframework.data.domain.Pageable pageable);

        Optional<Room> findByIdAndIsDeletedFalse(Long id);

        List<Room> findAllByIdInAndIsDeletedFalse(List<Long> ids);

        List<Room> findAllByIdInAndIsDeletedTrue(List<Long> ids);

        @Modifying(clearAutomatically = true)
        @Query("UPDATE Room r SET r.isDeleted = true, r.deletedAt = :deletedAt WHERE r.id IN :ids AND r.isDeleted = false")
        int softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Date deletedAt);

        @Modifying(clearAutomatically = true)
        @Query("UPDATE Room r SET r.isDeleted = false, r.deletedAt = null WHERE r.id IN :ids AND r.isDeleted = true")
        int restoreByIds(@Param("ids") List<Long> ids);

        org.springframework.data.domain.Page<Room> findAllByIsDeletedTrue(
                        org.springframework.data.domain.Pageable pageable);

        List<Room> findByHotelIdAndIsDeletedFalseAndAvailableTrue(Long hotelId);
}
