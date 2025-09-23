package com.booking.booking.listener;

import org.springframework.context.ApplicationEvent;

public class HotelCreatedEvent extends ApplicationEvent {

    private final Long hotelId;

    public HotelCreatedEvent(Object source, Long hotelId) {
        super(source);
        this.hotelId = hotelId;
    }

    public Long getHotelId() {
        return hotelId;
    }
}

