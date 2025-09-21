package com.booking.booking.controller;

import com.booking.booking.dto.request.VoucherCreateRequest;
import com.booking.booking.dto.request.VoucherUpdateRequest;
import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.model.Voucher;
import com.booking.booking.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@Slf4j(topic = "VOUCHER-CONTROLLER")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseSuccess createVoucher(@Valid @RequestBody VoucherCreateRequest request) {

        voucherService.createVoucher(request);

        return new ResponseSuccess(HttpStatus.CREATED, "Create a new voucher successfully!");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseSuccess updateVoucher(@PathVariable Long id, @Valid @RequestBody VoucherUpdateRequest request) {
        voucherService.updateVoucher(id, request);

        return new ResponseSuccess(HttpStatus.OK, "Update a voucher successfully!");
    }

    @GetMapping("/{id}")
    public ResponseSuccess getVoucherById(@PathVariable Long id) {

        return new ResponseSuccess(HttpStatus.OK, "Get a voucher successfully!",
                voucherService.getVoucherById(id));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseSuccess getVoucherByIdHotel(@PathVariable Long hotelId) {

        return new ResponseSuccess(HttpStatus.OK, "Get a voucher by hotel successfully!",
                voucherService.getVoucherByHotelId(hotelId));
    }

    @GetMapping
    public ResponseSuccess getAllVouchers(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "id", required = false) String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

        Page<Voucher> pages = voucherService.getAllVouchers(pageable);

        PageResponse<?> pageResponse = PageResponse.builder()
                .pageNo(page)
                .pageSize(size)
                .totalPage(pages.getTotalPages())
                .totalElements(pages.getTotalElements())
                .items(pages.getContent())
                .build();

        return new ResponseSuccess(HttpStatus.OK, "Get all vouchers successfully!", pageResponse);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseSuccess deleteVoucher(@PathVariable long id) {
        voucherService.deleteVoucher(id);

        return new ResponseSuccess(HttpStatus.OK, "Delete a voucher successfully!");
    }
}
