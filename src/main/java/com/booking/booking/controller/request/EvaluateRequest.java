package com.booking.booking.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateRequest {

  private String message;
  private Float starRating;
  private Long roomId;

}
