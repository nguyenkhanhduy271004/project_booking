package com.booking.booking.mapper;

import com.booking.booking.dto.request.VoucherCreateRequest;
import com.booking.booking.dto.request.VoucherUpdateRequest;
import com.booking.booking.model.Voucher;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class VoucherMapper {

    private final ModelMapper modelMapper;

    public void updateVoucher(Voucher existVoucher, VoucherUpdateRequest request) {
        modelMapper.map(request, existVoucher);
        existVoucher.setUpdatedAt(Date.from(Instant.now()));
    }

    public Voucher toVoucher(VoucherCreateRequest request) {

        Voucher voucher = modelMapper.map(request, Voucher.class);
        voucher.setCreatedAt(Date.from(Instant.now()));

        return voucher;
    }

}
