package com.booking.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;

public abstract class PaymentDTO {

  @Builder
  @AllArgsConstructor
  public static class VNPayResponse {
    public String code;
    public String message;
    public String paymentUrl;
  }

}
