package com.booking.booking.listener;

import com.booking.booking.service.EnhancedAIChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HotelDataEventListener {

    private final EnhancedAIChatService chatService;

    public HotelDataEventListener(EnhancedAIChatService chatService) {
        this.chatService = chatService;
    }

    @EventListener
    @Async
    public void handleHotelCreated(HotelCreatedEvent event) {
        log.info("Khách sạn mới được tạo, đang cập nhật search index: {}", event.getHotelId());
        chatService.refreshHotelData(event.getHotelId());
    }

    @EventListener
    @Async
    public void handleHotelUpdated(HotelUpdatedEvent event) {
        log.info("Khách sạn được cập nhật, đang refresh search index: {}", event.getHotelId());
        chatService.refreshHotelData(event.getHotelId());
    }

    @EventListener
    @Async
    public void handleRoomUpdated(RoomUpdatedEvent event) {
        log.info("Phòng được cập nhật, đang refresh search index cho khách sạn: {}", event.getHotelId());
        chatService.refreshHotelData(event.getHotelId());
    }
}