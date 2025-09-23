package com.booking.booking.service;

import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.repository.HotelRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HotelRAGService {

    private final HotelRepository hotelRepository;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            Bạn là một trợ lý đặt phòng khách sạn thông minh tại TP.HCM, Việt Nam.
            
            Hướng dẫn:
            - Luôn trả lời bằng tiếng Việt
            - Gợi ý khách sạn phù hợp dựa trên yêu cầu của khách (vị trí, ngân sách, rating, số người)
            - Hiển thị giá bằng VND với định dạng phân cách hàng nghìn
            - Nếu không có khách sạn nào phù hợp hoàn toàn, gợi ý lựa chọn gần nhất
            - Chỉ sử dụng thông tin từ dữ liệu được cung cấp
            - Luôn thân thiện và hữu ích
            """;

    public HotelRAGService(HotelRepository hotelRepository,
                           VectorStore vectorStore,
                           ChatClient.Builder chatClientBuilder) {
        this.hotelRepository = hotelRepository;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @PostConstruct
    public void initializeVectorStore() {
        log.info("Bắt đầu khởi tạo vector store...");
        CompletableFuture.runAsync(() -> {
            try {
                indexAllHotels();
                log.info("Hoàn thành khởi tạo vector store");
            } catch (Exception e) {
                log.error("Lỗi khi khởi tạo vector store", e);
            }
        });
    }

    public void indexAllHotels() {
        List<Hotel> hotels = hotelRepository.findAll();
        log.info("Đang index {} khách sạn...", hotels.size());

        List<Document> documents = new ArrayList<>();

        for (Hotel hotel : hotels) {
            try {
                documents.add(createHotelOverviewDocument(hotel));

                for (Room room : hotel.getRooms()) {
                    documents.add(createRoomDocument(hotel, room));
                }

                documents.add(createLocationDocument(hotel));

            } catch (Exception e) {
                log.warn("Lỗi khi tạo document cho hotel {}: {}", hotel.getId(), e.getMessage());
            }
        }

        if (!documents.isEmpty()) {
            try {
                clearVectorStore();

                addDocumentsInBatches(documents);

                log.info("Đã index thành công {} documents", documents.size());
            } catch (Exception e) {
                log.error("Lỗi khi thêm documents vào vector store", e);
            }
        }
    }

    private void clearVectorStore() {
        try {
            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")
                            .topK(10000)
                            .build()
            );

            if (!existingDocs.isEmpty()) {
                List<String> docIds = existingDocs.stream()
                        .map(doc -> doc.getMetadata().get("id"))
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .collect(Collectors.toList());

                if (!docIds.isEmpty()) {
                    vectorStore.delete(docIds);
                    log.info("Đã xóa {} documents cũ", docIds.size());
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xóa dữ liệu cũ: {}", e.getMessage());
        }
    }

    private void addDocumentsInBatches(List<Document> documents) {
        int batchSize = 20;
        for (int i = 0; i < documents.size(); i += batchSize) {
            try {
                int endIndex = Math.min(i + batchSize, documents.size());
                List<Document> batch = documents.subList(i, endIndex);

                vectorStore.add(batch);

                log.info("Đã index batch {}/{} ({} documents)",
                        (i / batchSize + 1),
                        (documents.size() + batchSize - 1) / batchSize,
                        batch.size());

                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Lỗi khi index batch từ {} đến {}", i, Math.min(i + batchSize, documents.size()), e);
            }
        }
    }

    private Document createHotelOverviewDocument(Hotel hotel) {
        String content = String.format("""
                        🏨 KHÁCH SẠN: %s
                        📍 Địa chỉ: %s, Quận %s, TP.HCM
                        ⭐ Xếp hạng: %s sao
                        🛏️ Tổng số phòng: %s phòng
                        💰 Khoảng giá: %s - %s VND/đêm
                        🏷️ Loại phòng có sẵn: %s
                        ✅ Tình trạng: %s
                        """,
                safeString(hotel.getName()),
                safeString(hotel.getAddressDetail()),
                safeString(hotel.getDistrict()),
                safeString(hotel.getStarRating()),
                String.valueOf(hotel.getRooms().size()),
                formatPrice(getMinPrice(hotel)),
                formatPrice(getMaxPrice(hotel)),
                getRoomTypes(hotel),
                hasAvailableRooms(hotel) ? "Còn phòng trống" : "Hết phòng"
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", "hotel_" + hotel.getId());
        metadata.put("type", "hotel_overview");
        metadata.put("hotelId", hotel.getId());
        metadata.put("hotelName", hotel.getName());
        metadata.put("district", hotel.getDistrict());
        metadata.put("starRating", hotel.getStarRating());
        metadata.put("minPrice", getMinPrice(hotel));
        metadata.put("maxPrice", getMaxPrice(hotel));
        metadata.put("hasAvailableRooms", hasAvailableRooms(hotel));


        return new Document(content, metadata);
    }


    private Document createRoomDocument(Hotel hotel, Room room) {
        String content = String.format("""
                        🛏️ PHÒNG: %s
                        🔢 Sức chứa: %s người
                        💰 Giá: %s VND/đêm
                        ✅ Trạng thái: %s
                        🏨 Khách sạn: %s (Quận %s)
                        """,
                safeString(room.getTypeRoom()),
                safeString(room.getCapacity()),
                formatPrice(room.getPricePerNight()),
                room.isAvailable() ? "Có sẵn" : "Đã đặt",
                safeString(hotel.getName()),
                safeString(hotel.getDistrict())
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", "hotel_" + hotel.getId());
        metadata.put("type", "hotel_overview");
        metadata.put("hotelId", hotel.getId());
        metadata.put("hotelName", hotel.getName());
        metadata.put("district", hotel.getDistrict());
        metadata.put("starRating", hotel.getStarRating());
        metadata.put("minPrice", getMinPrice(hotel));
        metadata.put("maxPrice", getMaxPrice(hotel));
        metadata.put("hasAvailableRooms", hasAvailableRooms(hotel));


        return new Document(content, metadata);
    }


    private Document createLocationDocument(Hotel hotel) {
        String content = String.format("""
                        📍 ĐỊA ĐIỂM KHÁCH SẠN
                        Khách sạn: %s
                        Địa chỉ: %s, Quận %s, TP.HCM
                        ⭐ Xếp hạng: %s sao
                        """,
                safeString(hotel.getName()),
                safeString(hotel.getAddressDetail()),
                safeString(hotel.getDistrict()),
                safeString(hotel.getStarRating())
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", "hotel_" + hotel.getId());
        metadata.put("type", "hotel_overview");
        metadata.put("hotelId", hotel.getId());
        metadata.put("hotelName", hotel.getName());
        metadata.put("district", hotel.getDistrict());
        metadata.put("starRating", hotel.getStarRating());
        metadata.put("minPrice", getMinPrice(hotel));
        metadata.put("maxPrice", getMaxPrice(hotel));
        metadata.put("hasAvailableRooms", hasAvailableRooms(hotel));


        return new Document(content, metadata);
    }

    private String safeString(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }


    public String searchAndAnswer(String question) {
        try {
            log.debug("Đang xử lý câu hỏi: {}", question);

            // Truy vấn vector store
            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(6)
                            .similarityThreshold(0.5)
                            .build()
            );

            if (relevantDocs == null || relevantDocs.isEmpty()) {
                log.info("Không tìm thấy document phù hợp");
                return generateFallbackMessage(question);
            }

            // Tạo phần context từ các document text (chỉ text không lỗi)
            String context = relevantDocs.stream()
                    .map(doc -> {
                        try {
                            return doc.getText();
                        } catch (Exception e) {
                            log.warn("Lỗi khi lấy text từ document", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n" + "=".repeat(50) + "\n"));

            log.info("Tìm thấy {} documents liên quan", relevantDocs.size());

            // Tạo prompt đầy đủ
            String prompt = String.format("""
                Dựa vào thông tin khách sạn sau:

                %s

                Câu hỏi của khách: %s

                → Hãy đưa ra gợi ý phù hợp nhất. Nếu có nhiều lựa chọn, hãy nêu ưu nhược điểm của từng khách sạn và giúp khách dễ so sánh.
                Nếu không tìm thấy phù hợp, hãy lịch sự từ chối và gợi ý thêm các khu vực lân cận.
                """, context, question);

            // Gửi prompt đến AI
            String response = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Đã tạo câu trả lời thành công");
            return response;

        } catch (Exception e) {
            log.error("Lỗi khi xử lý câu hỏi", e);
            return "❌ Xin lỗi, đã xảy ra lỗi khi tìm kiếm khách sạn. Bạn vui lòng thử lại sau hoặc cung cấp thêm thông tin.";
        }
    }


    private String generateFallbackMessage(String question) {
        String lower = question.toLowerCase();
        String district = null;

        if (lower.contains("thủ đức")) district = "Thủ Đức";
        else if (lower.contains("bình thạnh")) district = "Bình Thạnh";
        else if (lower.contains("quận 1")) district = "Quận 1";
        else if (lower.contains("quận 3")) district = "Quận 3";
        else if (lower.contains("quận 7")) district = "Quận 7";

        return String.format("""
                        Hiện tại, tôi chưa tìm thấy khách sạn nào đúng với khu vực%s trong hệ thống.
                        
                        🔍 Tuy nhiên, bạn có thể giúp tôi hiểu rõ hơn bằng cách cung cấp thêm các thông tin sau:
                        • Khu vực cụ thể hoặc lân cận (VD: Thủ Đức, Quận 9, Bình Thạnh...)
                        • Ngân sách dự kiến mỗi đêm (VD: dưới 500.000 VND, 1 - 2 triệu VND...)
                        • Số lượng người ở (VD: 2 người lớn, 1 trẻ em)
                        • Tiêu chuẩn mong muốn (VD: khách sạn 3 sao, 4 sao...)
                        
                        📌 Nếu bạn linh hoạt về khu vực, tôi có thể gợi ý một vài khách sạn gần%s ngay bây giờ. Bạn có muốn xem không?
                        """,
                district != null ? " \"" + district + "\"" : "",
                district != null ? " " + district : " khu vực bạn chọn"
        );
    }

    @CacheEvict(value = "hotelSearch", allEntries = true)
    public void updateHotelInIndex(Long hotelId) {
        try {
            log.info("Đang cập nhật index cho khách sạn ID: {}", hotelId);

            // Xóa documents cũ của hotel này
            deleteHotelDocuments(hotelId);

            // Thêm lại documents mới
            Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
            if (hotel != null) {
                List<Document> newDocs = new ArrayList<>();
                newDocs.add(createHotelOverviewDocument(hotel));
                newDocs.add(createLocationDocument(hotel));

                for (Room room : hotel.getRooms()) {
                    newDocs.add(createRoomDocument(hotel, room));
                }

                vectorStore.add(newDocs);
                log.info("Đã cập nhật {} documents cho khách sạn {}", newDocs.size(), hotel.getName());
            }

        } catch (Exception e) {
            log.error("Lỗi khi cập nhật index cho khách sạn {}", hotelId, e);
        }
    }

    private void deleteHotelDocuments(Long hotelId) {
        try {
            // Tìm tất cả documents của hotel này
            List<String> patterns = List.of(
                    "hotel_" + hotelId,
                    "location_" + hotelId
            );

            // Tìm tất cả room documents của hotel này
            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("hotelId:" + hotelId)
                            .topK(1000)
                            .build()
            );

            List<String> docIdsToDelete = existingDocs.stream()
                    .map(doc -> doc.getMetadata().get("id"))
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            docIdsToDelete.addAll(patterns);

            if (!docIdsToDelete.isEmpty()) {
                vectorStore.delete(docIdsToDelete);
                log.debug("Đã xóa {} documents cũ của hotel {}", docIdsToDelete.size(), hotelId);
            }

        } catch (Exception e) {
            log.warn("Không thể xóa documents cũ của hotel {}: {}", hotelId, e.getMessage());
        }
    }

    // Helper methods
    private double getMinPrice(Hotel hotel) {
        return hotel.getRooms().stream()
                .mapToDouble(Room::getPricePerNight)
                .min()
                .orElse(0);
    }

    private double getMaxPrice(Hotel hotel) {
        return hotel.getRooms().stream()
                .mapToDouble(Room::getPricePerNight)
                .max()
                .orElse(0);
    }

    private String getRoomTypes(Hotel hotel) {
        return hotel.getRooms().stream()
                .map(room -> room.getTypeRoom().toString())
                .distinct()
                .collect(Collectors.joining(", "));
    }


    private boolean hasAvailableRooms(Hotel hotel) {
        return hotel.getRooms().stream()
                .anyMatch(Room::isAvailable);
    }

    private String formatPrice(double price) {
        return String.format("%,.0f", price);
    }

    private String getPriceSegment(double price) {
        if (price < 500000) return "Tiết kiệm";
        if (price < 1000000) return "Tầm trung";
        if (price < 2000000) return "Cao cấp";
        return "Sang trọng";
    }

    private String getDistrictDescription(String district) {
        Map<String, String> descriptions = Map.of(
                "1", "Trung tâm thành phố, gần các địa điểm du lịch chính",
                "3", "Khu vực sầm uất, nhiều nhà hàng và cafe",
                "7", "Khu vực hiện đại, gần khu Phú Mỹ Hưng",
                "Bình Thạnh", "Khu vực phát triển, giao thông thuận tiện",
                "Thủ Đức", "Khu đô thị mới, hiện đại"
        );
        return descriptions.getOrDefault(district, "Khu vực thuận tiện");
    }
}

