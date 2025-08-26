package com.booking.booking.repository;

import com.booking.booking.common.UserType;
import com.booking.booking.model.User;
import jakarta.transaction.Transactional;
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

public interface UserRepository extends JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {

  @Query(value = "select u from User u where u.isDeleted = false and u.status='ACTIVE' " +
      "and (lower(u.firstName) like :keyword " +
      "or lower(u.lastName) like :keyword " +
      "or lower(u.username) like :keyword " +
      "or lower(u.phone) like :keyword " +
      "or lower(u.email) like :keyword)")
  Page<User> searchByKeyword(String keyword, Pageable pageable);

  @Query(value = "select u from User u where u.isDeleted = true and u.status='ACTIVE' " +
      "and (lower(u.firstName) like :keyword " +
      "or lower(u.lastName) like :keyword " +
      "or lower(u.username) like :keyword " +
      "or lower(u.phone) like :keyword " +
      "or lower(u.email) like :keyword)")
  Page<User> searchByKeywordAndIsDeletedTrue(String keyword, Pageable pageable);

  Page<User> findAllByIsDeletedFalse(Pageable pageable);

  Page<User> findAllByIsDeletedTrue(Pageable pageable);

  User findByIdAndIsDeletedFalse(Long id);

  User findByUsernameAndIsDeletedFalse(String username);

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String username);

  Optional<User> findByEmailAndIsDeletedFalse(String email);

  List<User> findAllByIdInAndIsDeletedFalse(List<Long> ids);

  List<User> findAllByIdInAndIsDeletedTrue(List<Long> ids);

  @Transactional
  @Modifying
  @Query("update User u set u.password = ?2 where u.email = ?1")
  void updatePassword(String email, String password);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.isDeleted = true, u.status = 'INACTIVE', u.deletedAt = :deletedAt WHERE u.id IN :ids AND u.isDeleted = false")
  int softDeleteByIds(@Param("ids") List<Long> ids, @Param("deletedAt") Date deletedAt);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("Update User u SET u.isDeleted = false, u.status = 'ACTIVE' WHERE u.id IN :ids AND u.isDeleted = true")
  int restoreDeletedByIds(@Param("ids") List<Long> ids);


  List<User> findAllByType(UserType type);

  @Query("SELECT u FROM User u WHERE u.hotel.id = :hotelId AND u.type = 'STAFF' OR u.type = 'GUEST'")
  Page<User> findUserForManager(@Param("hotelId") Long id, Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.hotel.id = :hotelId AND u.type = 'GUEST' ")
  Page<User> findUserForStaff(@Param("hotelId") Long id, Pageable pageable);

}
