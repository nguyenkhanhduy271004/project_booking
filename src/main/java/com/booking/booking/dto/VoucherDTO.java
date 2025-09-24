package com.booking.booking.dto;

import com.booking.booking.common.VoucherType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VoucherDTO {

    private Long id;

    @NotBlank(message = "Voucher code is required")
    private String code;

    @NotBlank(message = "Voucher name is required")
    private String name;

    private String description;

    @NotNull(message = "Voucher type is required")
    private VoucherType type;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @Positive(message = "Usage limit must be positive")
    private Integer usageLimit;

    private Integer usedCount;

    private Long hotelId;

    private boolean isActive;

    private boolean isDeleted;
}
