package com.booking.booking.dto;

import com.booking.booking.dto.response.UserResponse;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.Data;

@Data
public class HotelDTO {

    private Long id;

    private Long managerId;

    @NotBlank(message = "Hotel name is required")
    @Size(max = 100, message = "Hotel name must be at most 100 characters")
    private String name;

    @NotBlank(message = "District is required")
    @Size(max = 50, message = "District must be at most 50 characters")
    private String district;

    @NotBlank(message = "Address detail is required")
    @Size(max = 255, message = "Address detail must be at most 255 characters")
    private String addressDetail;

    @Min(value = 1, message = "Total rooms must be at least 1")
    private int totalRooms;

    @DecimalMin(value = "0.0", inclusive = true, message = "Star rating must be at least 0.0")
    @DecimalMax(value = "5.0", inclusive = true, message = "Star rating must be at most 5.0")
    private double starRating;

    @Size(max = 500, message = "Image URL must be at most 500 characters")
    private String imageUrl;

    private UserResponse managedBy;

    @NotNull(message = "Services must not be null")
    @Size(min = 1, message = "At least one service must be selected")
    private List<@NotBlank(message = "Service name must not be blank") String> services;
}
