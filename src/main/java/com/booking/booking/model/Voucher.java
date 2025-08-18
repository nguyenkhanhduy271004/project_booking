package com.booking.booking.model;

import com.booking.booking.common.VoucherStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_voucher")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voucher extends AbstractEntity<Long> implements Serializable {

  @NotBlank
  @Column(name = "voucher_code", nullable = false, length = 64)
  private String voucherCode;

  @NotBlank
  @Column(name = "voucher_name", nullable = false, length = 255)
  private String voucherName;

  @NotNull
  @PositiveOrZero
  @Column(nullable = false)
  private Long quantity;

  @NotNull
  @Min(0)
  @Max(100)
  @Column(name = "percent_discount", nullable = false)
  private Integer percentDiscount;

  @NotNull
  @PositiveOrZero
  @Column(name = "min_order_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal priceCondition;

  @NotNull
  @Column(name = "expired_date", nullable = false)
  private LocalDate expiredDate;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private VoucherStatus status;

  @ManyToOne
  @JoinColumn(name = "hotel_id", nullable = false)
  @JsonIgnore
  private Hotel hotel;
}
