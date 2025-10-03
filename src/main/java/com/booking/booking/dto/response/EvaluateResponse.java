package com.booking.booking.dto.response;

import com.booking.booking.common.TypeRoom;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvaluateResponse {

    private String message;
    private float starRating;
    private String reviewer;
}