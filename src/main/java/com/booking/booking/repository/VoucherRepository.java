package com.booking.booking.repository;

import com.booking.booking.model.Voucher;
import java.util.Optional;
import javax.swing.text.html.Option;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

  Optional<Voucher> findById(Long voucherId);
  Optional<Voucher> findByHotel_Id(Long voucherId);

}
