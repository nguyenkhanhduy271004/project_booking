package com.booking.booking.dto.response;

import com.booking.booking.common.VoucherType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VoucherResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private VoucherType type;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private boolean isActive;

    // Hotel information
    private Long hotelId;
    private String hotelName;

    // Audit fields
    private Date createdAt;
    private Date updatedAt;
    private String createdByUser;
    private String updatedByUser;
}
