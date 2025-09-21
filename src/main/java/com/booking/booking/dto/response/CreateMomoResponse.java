package com.booking.booking.dto.response;

import lombok.Data;

@Data
public class CreateMomoResponse {
  private String partnerCode;
  private String requestId;
  private String orderId;
  private Long amount;
  private Long responseTime;
  private String message;
  private Integer resultCode;
  private String payUrl;
  private String deeplink;
  private String qrCodeUrl;
  private String deeplinkMiniApp;

}
