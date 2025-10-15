package com.booking.booking.controller;

import com.booking.booking.service.EnhancedAIChatService;
import com.booking.booking.service.HotelRAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-chat")
@Slf4j
@RequiredArgsConstructor
public class AIChatController {

    private final EnhancedAIChatService chatService;
    private final VectorStore vectorStore;
    private final HotelRAGService hotelRAGService;

    @GetMapping("/reindex")
    public ResponseEntity<String> reindex() {
        long startTime = System.currentTimeMillis();
        hotelRAGService.indexAllHotels();
        long endTime = System.currentTimeMillis();
        return ResponseEntity.ok("Đã reindex khách sạn vào vector store trong " + (endTime - startTime) + "ms");
    }

    @GetMapping("/warmup")
    public ResponseEntity<Map<String, Object>> warmupCache() {
        long startTime = System.currentTimeMillis();
        
        String[] commonQuestions = {
            "khách sạn ở hà nội",
            "khách sạn ở sài gòn", 
            "khách sạn ở đà nẵng",
            "khách sạn giá rẻ",
            "khách sạn 5 sao"
        };
        
        int totalWarmed = 0;
        for (String question : commonQuestions) {
            try {
                chatService.answerQuestion(question);
                totalWarmed++;
            } catch (Exception e) {
                log.warn("Lỗi khi warm up câu hỏi: {}", question, e);
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        return ResponseEntity.ok(Map.of(
            "message", "Cache đã được warm up",
            "questionsWarmed", totalWarmed,
            "totalTime", (endTime - startTime) + "ms",
            "timestamp", Instant.now().toString()
        ));
    }


    @GetMapping("/vectorstore/debug")
    public ResponseEntity<?> debugVectorStore(@RequestParam String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5).build()
        );
        return ResponseEntity.ok(
                docs.stream()
                        .map(doc -> Map.of(
                                "text", doc.getText(),
                                "metadata", doc.getMetadata()
                        )).toList()
        );
    }


    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody ChatRequest request) {
        try {
            String question = request.getQuestion();
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Câu hỏi không được để trống"));
            }

            long startTime = System.currentTimeMillis();
            List<HotelRAGService.HotelSuggestionDTO> suggestions = chatService.answerQuestion(question);
            long endTime = System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("question", question);
            result.put("results", suggestions);
            result.put("total", suggestions.size());
            result.put("timestamp", Instant.now().toString());
            result.put("processingTime", (endTime - startTime) + "ms");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý câu hỏi", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Có lỗi xảy ra khi xử lý câu hỏi. Vui lòng thử lại."));
        }
    }



    //    @PostMapping("/refresh")
    //    @PreAuthorize("hasRole('ADMIN')")
    //    public ResponseEntity<Map<String, String>> refreshData() {
    //        try {
    //            chatService.refreshAllHotelData();
    //            return ResponseEntity.ok(Map.of(
    //                    "message", "Đã làm mới dữ liệu thành công",
    //                    "timestamp", Instant.now().toString()
    //            ));
    //        } catch (Exception e) {
    //            log.error("Lỗi khi làm mới dữ liệu", e);
    //            return ResponseEntity.internalServerError()
    //                    .body(Map.of("error", "Lỗi khi làm mới dữ liệu"));
    //        }
    //    }

//    @PostMapping("/refresh/{hotelId}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Map<String, String>> refreshHotelData(@PathVariable Long hotelId) {
//        try {
//            chatService.refreshHotelData(hotelId);
//            return ResponseEntity.ok(Map.of(
//                    "message", "Đã làm mới dữ liệu khách sạn ID: " + hotelId,
//                    "timestamp", Instant.now().toString()
//            ));
//        } catch (Exception e) {
//            log.error("Lỗi khi làm mới dữ liệu khách sạn {}", hotelId, e);
//            return ResponseEntity.internalServerError()
//                    .body(Map.of("error", "Lỗi khi làm mới dữ liệu khách sạn"));
//        }
//    }

    public static class ChatRequest {
        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }
}
