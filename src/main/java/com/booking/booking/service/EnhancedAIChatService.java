package com.booking.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnhancedAIChatService {

    private final HotelRAGService ragService;

    public EnhancedAIChatService(HotelRAGService ragService) {
        this.ragService = ragService;
    }

    @Cacheable(value = "hotelSearch", key = "#question", unless = "#result == null || #result.contains('lỗi')")
    public String answerQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return """
                Xin chào! Tôi là trợ lý tìm kiếm khách sạn tại TP.HCM. 
                Tôi có thể giúp bạn tìm khách sạn phù hợp dựa trên:
                - Vị trí mong muốn
                - Ngân sách 
                - Số người ở
                - Tiêu chuẩn khách sạn
                
                Bạn cần tìm khách sạn như thế nào?
                """;
        }

        log.info("Đang xử lý câu hỏi: {}", question);
        return ragService.searchAndAnswer(question);
    }

    @CacheEvict(value = "hotelSearch", allEntries = true)
    public void refreshAllHotelData() {
        log.info("Đang làm mới toàn bộ dữ liệu khách sạn...");
        ragService.indexAllHotels();
    }

    public void refreshHotelData(Long hotelId) {
        log.info("Đang làm mới dữ liệu khách sạn ID: {}", hotelId);
        ragService.updateHotelInIndex(hotelId);
    }
}