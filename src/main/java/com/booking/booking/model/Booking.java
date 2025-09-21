package com.booking.booking.model;

import com.booking.booking.common.BookingStatus;
import com.booking.booking.common.PaymentType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "tbl_booking")
public class Booking extends AbstractEntity<Long> implements Serializable {

    @Column(nullable = false, unique = true)
    private String bookingCode;

    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonBackReference(value = "hotel-bookings")
    private Hotel hotel;

    @Column(length = 500)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "guest_id")
    private User guest;

    @Column(name = "room_id")
    private Long legacyRoomId;

    @Column(nullable = false)
    @Future
    private LocalDate checkInDate;

    @Column(nullable = false)
    @Future
    private LocalDate checkOutDate;

    @Column(nullable = false)
    @DecimalMin("0.0")
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @ManyToMany
    @JoinTable(name = "tbl_booking_room", joinColumns = @JoinColumn(name = "booking_id"), inverseJoinColumns = @JoinColumn(name = "room_id"))
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();


    @AssertTrue(message = "Check-out date must be after check-in date")
    private boolean isValidDateRange() {
        return checkOutDate != null && checkInDate != null && checkOutDate.isAfter(checkInDate);
    }
}
