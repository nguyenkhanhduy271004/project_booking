package com.booking.booking.controller.response;

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

    private Long id;
    private Integer rating;
    private String comment;
    private String reply;
    private boolean isApproved;
    private boolean isHidden;

    // Room information
    private Long roomId;
    private TypeRoom roomType;

    // Hotel information
    private Long hotelId;
    private String hotelName;

    // User information
    private Long userId;
    private String userName;

    // Audit fields
    private Date createdAt;
    private Date updatedAt;
    private String createdByUser;
    private String updatedByUser;
}