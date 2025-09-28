package com.booking.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EnhancedAIChatService {

    private final HotelRAGService ragService;

    public EnhancedAIChatService(HotelRAGService ragService) {
        this.ragService = ragService;
    }

    @Cacheable(value = "hotelSearch", key = "#question.toLowerCase().trim()", unless = "#result == null || #result.isEmpty()")
    public List<HotelRAGService.HotelSuggestionDTO> answerQuestion(String question) {
        long startTime = System.currentTimeMillis();

        if (question == null || question.trim().isEmpty()) {
            return greetingMessage();
        }

        // Normalize question
        String normalizedQuestion = question.toLowerCase().trim();
        log.info("Đang xử lý câu hỏi: {}", normalizedQuestion);

        List<HotelRAGService.HotelSuggestionDTO> result = ragService.searchAndAnswer(normalizedQuestion);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Hoàn thành xử lý câu hỏi trong {}ms", totalTime);

        if (result == null || result.isEmpty()) {
            return fallbackMessage();
        }

        return result;
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

    private List<HotelRAGService.HotelSuggestionDTO> greetingMessage() {
        return List.of(
                HotelRAGService.HotelSuggestionDTO.builder()
                        .name("Xin chào! Tôi là trợ lý tìm kiếm khách sạn.")
                        .district("")
                        .province("")
                        .minPrice(0)
                        .star(0)
                        .url("")
                        .build()
        );
    }

    private List<HotelRAGService.HotelSuggestionDTO> fallbackMessage() {
        return List.of(
                HotelRAGService.HotelSuggestionDTO.builder()
                        .name("Xin lỗi, tôi không thể tìm thấy khách sạn phù hợp với câu hỏi của bạn.")
                        .district("")
                        .province("")
                        .minPrice(0)
                        .star(0)
                        .url("")
                        .build()
        );
    }
}
