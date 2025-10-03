package com.booking.booking.service.impl;

import com.booking.booking.common.UserType;
import com.booking.booking.dto.request.VoucherCreateRequest;
import com.booking.booking.dto.request.VoucherUpdateRequest;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.mapper.VoucherMapper;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.User;
import com.booking.booking.model.Voucher;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.VoucherRepository;
import com.booking.booking.service.interfaces.VoucherService;
import com.booking.booking.util.UserContext;
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
    private final VoucherMapper voucherMapper;
    private final UserContext userContext;


    @Override
    public void createVoucher(VoucherCreateRequest request) {
        User user = userContext.getCurrentUser();

        Hotel hotel;
        if (UserType.ADMIN.equals(user.getType())) {
            hotel = hotelRepository.findById(request.getHotelId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Hotel not found with id: " + request.getHotelId()));
        } else if (UserType.MANAGER.equals(user.getType())) {
            hotel = hotelRepository.findByManagerAndIsDeletedFalse(user)
                    .orElseThrow(() -> new ResourceNotFoundException("Hotel not found for current user"));
        } else {
            throw new BadRequestException("Only ADMIN or MANAGER can create vouchers");
        }

        if (voucherRepository.existsByVoucherCode(request.getVoucherCode())) {
            throw new BadRequestException("Voucher code already exists");
        }

        Voucher voucher = voucherMapper.toVoucher(request);
        voucher.setHotel(hotel);
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

        voucherMapper.updateVoucher(existVoucher, request);

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
        User user = userContext.getCurrentUser();

        if (user.getType().equals(UserType.MANAGER) || user.getType().equals(UserType.STAFF)) {

            return voucherRepository.findAllByIsDeletedFalseAndHotel(pageable, user.getHotel());

        }
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
