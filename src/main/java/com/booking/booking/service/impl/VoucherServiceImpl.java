package com.booking.booking.service.impl;

import com.booking.booking.common.UserType;
import com.booking.booking.dto.request.VoucherCreateRequest;
import com.booking.booking.dto.request.VoucherUpdateRequest;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.User;
import com.booking.booking.model.Voucher;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.VoucherRepository;
import com.booking.booking.service.VoucherService;
import com.booking.booking.util.UserContext;
import java.time.Instant;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "VOUCHER-SERVICE")
public class VoucherServiceImpl implements VoucherService {

  private final VoucherRepository voucherRepository;
  private final HotelRepository hotelRepository;
  private final UserContext userContext;


  @Override
  public void createVoucher(VoucherCreateRequest request) {
    User user = userContext.getCurrentUser();
    Hotel hotel;

    if (UserType.ADMIN.equals(user.getType())) {
      hotel = hotelRepository.findById(request.getHotelId())
          .orElseThrow(() -> new ResourceNotFoundException(
              "Hotel not found with id: " + request.getHotelId()));
    } else {
      hotel = hotelRepository.findByManagerAndIsDeletedFalse(user)
          .orElseThrow(() -> new ResourceNotFoundException("Hotel not found for current user"));

      if (!user.getId().equals(hotel.getManager().getId())) {
        log.warn("User {} is not authorized to create voucher for hotel {}", user.getId(),
            hotel.getId());
        throw new BadRequestException("You are not authorized to create voucher for this hotel");
      }
    }

    if (voucherRepository.existsByVoucherCode(request.getVoucherCode())) {
      throw new BadRequestException("Voucher code already exists");
    }

    Voucher voucher = new Voucher();
    voucher.setHotel(hotel);
    voucher.setStatus(request.getStatus());
    voucher.setQuantity(request.getQuantity());
    voucher.setCreatedAt(Date.from(Instant.now()));
    voucher.setVoucherCode(request.getVoucherCode());
    voucher.setVoucherName(request.getVoucherName());
    voucher.setExpiredDate(request.getExpiredDate());
    voucher.setPercentDiscount(request.getPercentDiscount());
    voucher.setPriceCondition(request.getPriceCondition());

    voucherRepository.save(voucher);
  }

  @Override
  public void updateVoucher(Long id, VoucherUpdateRequest request) {

    Voucher existVoucher = voucherRepository.findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Voucher not found with id: " + id));
    User user = userContext.getCurrentUser();
    Hotel hotel = existVoucher.getHotel();

    if (!UserType.ADMIN.equals(user.getType()) &&
        !user.getId().equals(hotel.getManager().getId())) {
      throw new BadRequestException("You are not authorized to update this voucher");
    }


    if (!existVoucher.getVoucherCode().equals(request.getVoucherCode()) &&
        voucherRepository.existsByVoucherCode(request.getVoucherCode())) {
      throw new BadRequestException("Voucher code already exists");
    }

    existVoucher.setVoucherCode(request.getVoucherCode());
    existVoucher.setVoucherName(request.getVoucherName());
    existVoucher.setQuantity(request.getQuantity());
    existVoucher.setPercentDiscount(request.getPercentDiscount());
    existVoucher.setPriceCondition(request.getPriceCondition());
    existVoucher.setExpiredDate(request.getExpiredDate());
    existVoucher.setStatus(request.getStatus());
    existVoucher.setUpdatedAt(Date.from(Instant.now()));

    voucherRepository.save(existVoucher);
  }

  @Override
  public void deleteVoucher(Long id) {
    Voucher existVoucher = voucherRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
    User user = userContext.getCurrentUser();
    Hotel hotel = existVoucher.getHotel();

    if (!UserType.ADMIN.equals(user.getType()) &&
        !user.getId().equals(hotel.getManager().getId())) {
      throw new BadRequestException("You are not authorized to delete this voucher");
    }
    voucherRepository.deleteById(existVoucher.getId());
  }

  @Override
  public Page<Voucher> getAllVouchers(Pageable pageable) {
    return voucherRepository.findAll(pageable);
  }

  @Override
  public Voucher getVoucherById(Long id) {
    return voucherRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
  }

  @Override
  public Voucher getVoucherByHotelId(Long hotelId) {
    return voucherRepository.findByHotel_Id(hotelId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Voucher not found with hotel id: " + hotelId));
  }

}
