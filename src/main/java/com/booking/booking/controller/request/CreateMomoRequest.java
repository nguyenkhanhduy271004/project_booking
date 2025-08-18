package com.booking.booking.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateMomoRequest {
  private String partnerCode;
  private String requestType;
  private String ipnUrl;
  private String orderId;
  private long amount;
  private String orderInfo;
  private String requestId;
  private String redirectUrl;
  private String lang;
  private String extraData;
  private String signature;
}
