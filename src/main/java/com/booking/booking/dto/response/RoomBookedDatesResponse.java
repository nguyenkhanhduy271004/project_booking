package com.booking.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomBookedDatesResponse {
    private Long roomId;
    private Set<LocalDate> bookedDates;
}
