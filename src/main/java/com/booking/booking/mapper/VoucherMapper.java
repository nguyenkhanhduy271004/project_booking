package com.booking.booking.mapper;

import com.booking.booking.controller.response.VoucherResponse;
import com.booking.booking.dto.VoucherDTO;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Voucher;
import com.booking.booking.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoucherMapper {

    private final ModelMapper modelMapper;
    private final HotelRepository hotelRepository;

    public VoucherResponse toVoucherResponse(Voucher voucher) {
        VoucherResponse response = modelMapper.map(voucher, VoucherResponse.class);
        if (voucher.getHotel() != null) {
            response.setHotelId(voucher.getHotel().getId());
            response.setHotelName(voucher.getHotel().getName());
        }

        return response;
    }

    public Voucher toVoucher(VoucherDTO voucherDTO) {
        Voucher voucher = modelMapper.map(voucherDTO, Voucher.class);
        if (voucherDTO.getHotelId() != null) {
            Hotel hotel = hotelRepository.findById(voucherDTO.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + voucherDTO.getHotelId()));
            voucher.setHotel(hotel);
        }

        return voucher;
    }

    public void updateVoucher(Voucher existingVoucher, VoucherDTO voucherDTO) {
        if (voucherDTO.getCode() != null) {
            existingVoucher.setVoucherCode(voucherDTO.getCode());
        }
        if (voucherDTO.getName() != null) {
            existingVoucher.setVoucherName(voucherDTO.getName());
        }

        if (voucherDTO.getHotelId() != null) {
            Hotel hotel = hotelRepository.findById(voucherDTO.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + voucherDTO.getHotelId()));
            existingVoucher.setHotel(hotel);
        } else {
            existingVoucher.setHotel(null); // System-wide voucher
        }
    }
}
