package com.booking.booking.repository;

import com.booking.booking.model.Voucher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

  Optional<Voucher> findById(Long voucherId);

  Optional<Voucher> findByHotel_Id(Long voucherId);

  boolean existsByVoucherCode(String voucherCode);

  @Modifying
  @Query("DELETE FROM Voucher v WHERE v.hotel.id = :hotelId")
  void deleteAllByHotelId(@Param("hotelId") Long hotelId);

  @Modifying
  @Query("DELETE FROM Voucher v WHERE v.hotel.id IN :hotelIds")
  void deleteAllByHotelIds(@Param("hotelIds") List<Long> hotelIds);

}
