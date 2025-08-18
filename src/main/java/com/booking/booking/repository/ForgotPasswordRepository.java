package com.booking.booking.repository;

import com.booking.booking.model.ForgotPassword;
import com.booking.booking.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, Long> {

  @Query("select fp from ForgotPassword fp where fp.otp = ?1 and fp.user = ?2")
  Optional<ForgotPassword> findByOtpAndUser(Integer otp, User user);

  @Modifying
  @Query("delete from ForgotPassword fp where fp.otp = :otp and fp.user = :user")
  void deleteByOtpAndUser(@Param("otp") Integer otp, @Param("user") User user);
}
