package com.booking.booking.controller.request;

import com.booking.booking.common.VoucherStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUpdateRequest {

    @NotBlank(message = "Voucher code is required")
    private String voucherCode;

    @NotBlank(message = "Voucher name is required")
    private String voucherName;

    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity must be positive or zero")
    private Long quantity;

    @NotNull(message = "Percent discount is required")
    @Min(value = 0, message = "Percent discount must be at least 0")
    @Max(value = 100, message = "Percent discount must be at most 100")
    private Integer percentDiscount;

    @NotNull(message = "Price condition is required")
    @PositiveOrZero(message = "Price condition must be positive or zero")
    private BigDecimal priceCondition;

    @NotNull(message = "Expired date is required")
    private LocalDate expiredDate;

    @NotNull(message = "Status is required")
    private VoucherStatus status;

    @NotNull(message = "Hotel ID is required")
    private Long hotelId;
}
