package com.booking.booking.config;

import com.booking.booking.service.HotelRAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class VectorStoreInitializer {

    private final HotelRAGService hotelRAGService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeVectorStore() {
        log.info("Ứng dụng đã sẵn sàng, bắt đầu khởi tạo vector store...");
        
        try {
            hotelRAGService.indexAllHotels();
            log.info("Khởi tạo vector store thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo vector store", e);
            // Không throw exception để không làm crash ứng dụng
        }
    }
}
