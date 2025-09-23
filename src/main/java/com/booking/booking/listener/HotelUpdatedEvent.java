package com.booking.booking.listener;

import org.springframework.context.ApplicationEvent;

public class HotelUpdatedEvent extends ApplicationEvent {

    private final Long hotelId;

    public HotelUpdatedEvent(Object source, Long hotelId) {
        super(source);
        this.hotelId = hotelId;
    }

    public Long getHotelId() {
        return hotelId;
    }
}
