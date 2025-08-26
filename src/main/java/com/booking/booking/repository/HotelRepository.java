package com.booking.booking.repository;

import com.booking.booking.model.Hotel;
import com.booking.booking.model.User;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelRepository extends JpaRepository<Hotel, Long>,
    JpaSpecificationExecutor<Hotel> {

  Page<Hotel> findAllByIsDeletedFalse(Pageable pageable);

  Optional<Hotel> findByIdAndIsDeletedFalse(Long id);

  List<Hotel> findAllByIdInAndIsDeletedFalse(List<Long> ids);

  List<Hotel> findAllByIdInAndIsDeletedTrue(List<Long> ids);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Hotel h SET h.isDeleted = true, h.deletedAt = :deletedAt WHERE h.id IN :ids AND h.isDeleted = false")
  int softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Date deletedAt);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Hotel h SET h.isDeleted = false, h.deletedAt = null WHERE h.id IN :ids AND h.isDeleted = true")
  int restoreByIds(@Param("ids") List<Long> ids);

  Page<Hotel> findAllByIsDeletedTrue(Pageable pageable);

  Page<Hotel> findAllByManagedByUserAndIsDeletedFalse(User managedByUser, Pageable pageable);

  Page<Hotel> findAllByManagedByUserAndIsDeletedTrue(User managedByUser, Pageable pageable);

  Optional<Hotel> findByManagedByUserAndIsDeletedFalse(User managedByUser);

}
