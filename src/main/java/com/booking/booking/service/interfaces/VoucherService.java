package com.booking.booking.service.interfaces;

import com.booking.booking.dto.request.VoucherCreateRequest;
import com.booking.booking.dto.request.VoucherUpdateRequest;
import com.booking.booking.model.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VoucherService {

  void createVoucher(VoucherCreateRequest request);

  void updateVoucher(Long id, VoucherUpdateRequest request);

  void deleteVoucher(Long id);

  Page<Voucher> getAllVouchers(Pageable pageable);

  Voucher getVoucherById(Long id);

  Voucher getVoucherByHotelId(Long hotelId);
}
