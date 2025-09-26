package com.booking.booking.service;

import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.repository.HotelRepository;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
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
            Bạn là một trợ lý đặt phòng khách sạn thông minh tại Việt Nam.
            
            Hướng dẫn:
            - Luôn trả lời bằng tiếng Việt
            - Gợi ý khách sạn phù hợp dựa trên yêu cầu của khách (vị trí, ngân sách, rating, số người)
            - Hiển thị giá bằng VND với định dạng phân cách hàng nghìn
            - Nếu không có khách sạn nào phù hợp hoàn toàn, gợi ý lựa chọn gần nhất trong khu vực lân cận
            - Chỉ sử dụng thông tin từ dữ liệu được cung cấp
            - Luôn thân thiện và hữu ích
            """;

    public HotelRAGService(HotelRepository hotelRepository,
                           VectorStore vectorStore,
                           ChatClient.Builder chatClientBuilder) {
        this.hotelRepository = hotelRepository;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @PostConstruct
    public void initializeVectorStore() {
        CompletableFuture.runAsync(() -> {
            try {
                indexAllHotels();
            } catch (Exception e) {
                log.error("Lỗi khi khởi tạo vector store", e);
            }
        });
    }

    public void indexAllHotels() {
        long startTime = System.currentTimeMillis();
        List<Hotel> hotels = hotelRepository.findAll();

        // Pre-allocate với estimated capacity
        List<Document> documents = new ArrayList<>(hotels.size() * 3); // Estimate 3 docs per hotel

        log.info("Bắt đầu tạo documents cho {} khách sạn", hotels.size());

        for (Hotel hotel : hotels) {
            try {
                // Chỉ tạo hotel overview document để tăng tốc
                // Room documents có thể tạo sau nếu cần
                documents.add(createHotelOverviewDocument(hotel));

                // Chỉ tạo location document nếu thực sự cần thiết
                if (hotel.getDistrict() != null && !hotel.getDistrict().trim().isEmpty()) {
                    documents.add(createLocationDocument(hotel));
                }

                // Tạo room documents cho các phòng available (giới hạn số lượng)
                int roomCount = 0;
                for (Room room : hotel.getRooms()) {
                    if (room.isAvailable() && roomCount < 3) { // Giới hạn 3 phòng/hotel để giảm documents
                        documents.add(createRoomDocument(hotel, room));
                        roomCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi khi tạo document cho hotel {}: {}", hotel.getId(), e.getMessage());
            }
        }

        long docCreationTime = System.currentTimeMillis();
        log.info("Tạo {} documents trong {}ms", documents.size(), docCreationTime - startTime);

        if (!documents.isEmpty()) {
            try {
                clearVectorStore();
                addDocumentsInBatches(documents);

                long totalTime = System.currentTimeMillis() - startTime;
                log.info("Hoàn thành indexing {} documents trong {}ms", documents.size(), totalTime);
            } catch (Exception e) {
                log.error("Lỗi khi thêm documents vào vector store", e);
            }
        }
    }

    private void clearVectorStore() {
        try {
            List<Document> existingDocs = vectorStore.similaritySearch(SearchRequest.builder().query("*").topK(10000).build());
            if (!existingDocs.isEmpty()) {
                List<String> docIds = existingDocs.stream()
                        .map(doc -> doc.getMetadata().get("id"))
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                if (!docIds.isEmpty()) {
                    vectorStore.delete(docIds);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xóa dữ liệu cũ: {}", e.getMessage());
        }
    }

    private void addDocumentsInBatches(List<Document> documents) {
        int batchSize = 50;
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = null;
            try {
                int endIndex = Math.min(i + batchSize, documents.size());
                batch = documents.subList(i, endIndex);
                vectorStore.add(batch);
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("Lỗi khi index batch từ {} đến {}", i, Math.min(i + batchSize, documents.size()), e);

                if (batch != null) {
                    try {
                        int smallerBatchSize = Math.min(10, batch.size());
                        for (int j = 0; j < batch.size(); j += smallerBatchSize) {
                            int smallEndIndex = Math.min(j + smallerBatchSize, batch.size());
                            List<Document> smallBatch = batch.subList(j, smallEndIndex);
                            vectorStore.add(smallBatch);
                            Thread.sleep(100);
                        }
                    } catch (Exception retryException) {
                        log.error("Lỗi khi retry index batch", retryException);
                    }
                } else {
                    log.warn("Không thể retry vì batch null tại vị trí {}", i);
                }
            }
        }
    }


    private Document createHotelOverviewDocument(Hotel hotel) {
        String content = String.format("""
                        🏨 KHÁCH SẠN: %s
                        📍 Địa chỉ: %s, %s, %s
                        ⭐ Xếp hạng: %s sao
                        🛏️ Tổng số phòng: %s phòng
                        💰 Khoảng giá: %s - %s VND/đêm
                        🏷️ Loại phòng có sẵn: %s
                        ✅ Tình trạng: %s
                        """,
                safeString(hotel.getName()),
                safeString(hotel.getAddressDetail()),
                safeString(hotel.getDistrict()),
                safeString(hotel.getProvince()),
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
        metadata.put("province", inferProvince(hotel.getDistrict(), hotel.getProvince()));
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
                        🏨 Khách sạn: %s (%s, %s)
                        """,
                safeString(room.getTypeRoom()),
                safeString(room.getCapacity()),
                formatPrice(room.getPricePerNight()),
                room.isAvailable() ? "Có sẵn" : "Đã đặt",
                safeString(hotel.getName()),
                safeString(hotel.getDistrict()),
                safeString(hotel.getProvince())
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", "room_" + room.getId());
        metadata.put("type", "room");
        metadata.put("hotelId", hotel.getId());
        metadata.put("hotelName", hotel.getName());
        metadata.put("district", hotel.getDistrict());
        metadata.put("province", hotel.getProvince());

        return new Document(content, metadata);
    }

    private Document createLocationDocument(Hotel hotel) {
        String content = String.format("""
                        📍 ĐỊA ĐIỂM KHÁCH SẠN
                        Khách sạn: %s
                        Địa chỉ: %s, %s, %s
                        ⭐ Xếp hạng: %s sao
                        """,
                safeString(hotel.getName()),
                safeString(hotel.getAddressDetail()),
                safeString(hotel.getDistrict()),
                safeString(hotel.getProvince()),
                safeString(hotel.getStarRating())
        );

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", UUID.randomUUID().toString());
        metadata.put("type", "location");
        metadata.put("hotelId", hotel.getId());
        metadata.put("hotelName", hotel.getName());
        metadata.put("district", hotel.getDistrict());
        metadata.put("province", hotel.getProvince());

        return new Document(content, metadata);
    }

    public List<HotelSuggestionDTO> searchAndAnswer(String question) {
        long startTime = System.currentTimeMillis();

        try {
            // Giảm topK và tăng similarity threshold để tìm kiếm nhanh hơn
            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(6)  // Giảm từ 10 xuống 6
                            .similarityThreshold(0.6)  // Giảm từ 0.7 xuống 0.6 để có kết quả nhanh hơn
                            .build()
            );

            long searchTime = System.currentTimeMillis();
            log.debug("Vector search took: {}ms", searchTime - startTime);

            if (relevantDocs == null || relevantDocs.isEmpty()) {
                log.info("Không tìm thấy document phù hợp với câu hỏi: {}", question);
                return Collections.emptyList();
            }

            // Sử dụng HashSet cho performance tốt hơn
            Set<Long> addedHotelIds = new HashSet<>();
            List<HotelSuggestionDTO> suggestions = new ArrayList<>(5); // Pre-allocate capacity

            // Trích xuất từ khóa địa điểm từ câu hỏi (cache result)
            String locationKeywords = extractLocationKeywords(question);
            boolean hasLocationFilter = !locationKeywords.isEmpty();

            for (Document doc : relevantDocs) {
                // Early exit if we have enough results
                if (suggestions.size() >= 5) {
                    break;
                }

                Map<String, Object> meta = doc.getMetadata();

                // Skip non-hotel documents
                if (!"hotel_overview".equals(meta.get("type"))) continue;

                Long hotelId = Long.parseLong(String.valueOf(meta.get("hotelId")));

                // Kiểm tra trùng lặp
                if (addedHotelIds.contains(hotelId)) {
                    continue;
                }

                String name = String.valueOf(meta.get("hotelName"));
                String district = Optional.ofNullable(meta.get("district"))
                        .map(String::valueOf)
                        .orElse("Không rõ");
                String province = Optional.ofNullable(meta.get("province"))
                        .map(String::valueOf)
                        .filter(p -> !"null".equalsIgnoreCase(p))
                        .orElse("Không rõ");
                // Kiểm tra relevance về vị trí nếu có từ khóa địa điểm
                if (hasLocationFilter && !isLocationRelevant(locationKeywords, district, province)) {
                    log.debug("Khách sạn {} không phù hợp với vị trí yêu cầu: {}", name, locationKeywords);
                    continue;
                }

                // Parse numbers safely with defaults
                double minPrice = parseDoubleOrDefault(meta.get("minPrice"), 0.0);
                int star = parseIntOrDefault(meta.get("starRating"), 0);

                HotelSuggestionDTO dto = HotelSuggestionDTO.builder()
                        .id(hotelId)
                        .name(name)
                        .district(district)
                        .province(province)
                        .minPrice(minPrice)
                        .star(star)
                        .url("http://localhost:5173/hotel/" + hotelId)
                        .build();

                suggestions.add(dto);
                addedHotelIds.add(hotelId);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Tìm thấy {} khách sạn phù hợp cho câu hỏi: {} ({}ms)",
                    suggestions.size(), question, totalTime);
            return suggestions;

        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            log.error("Lỗi khi xử lý câu hỏi: {} ({}ms)", question, errorTime, e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse double safely with default value
     */
    private double parseDoubleOrDefault(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parse int safely with default value
     */
    private int parseIntOrDefault(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return (int) Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Trích xuất từ khóa địa điểm từ câu hỏi
     */
    private String extractLocationKeywords(String question) {
        if (question == null) return "";

        String lowerQuestion = question.toLowerCase();
        List<String> locationKeywords = new ArrayList<>();

        // Các từ khóa địa điểm phổ biến ở Việt Nam
        String[] vietnameseProvinces = {
                "hà nội", "hồ chí minh", "đà nẵng", "hải phòng", "cần thơ", "quảng ninh", "khánh hòa",
                "thừa thiên huế", "lâm đồng", "bình định", "phú quốc", "vũng tàu", "nha trang",
                "hạ long", "sapa", "hội an", "huế", "đà lạt", "phú yên", "bình thuận", "quảng nam",
                "quảng trị", "nghệ an", "thanh hóa", "nam định", "hải dương", "bắc ninh", "vĩnh phúc",
                "thái nguyên", "lạng sơn", "cao bằng", "hà giang", "lai châu", "sơn la", "điện biên",
                "yên bái", "tuyên quang", "phú thọ", "vĩnh yên", "bắc giang", "quảng bình"
        };

        for (String province : vietnameseProvinces) {
            if (lowerQuestion.contains(province)) {
                locationKeywords.add(province);
            }
        }

        return String.join(" ", locationKeywords);
    }

    /**
     * Kiểm tra xem khách sạn có phù hợp với vị trí yêu cầu không
     */
    private boolean isLocationRelevant(String locationKeywords, String district, String province) {
        if (locationKeywords == null || locationKeywords.trim().isEmpty()) {
            return true; // Nếu không có từ khóa địa điểm cụ thể, chấp nhận tất cả
        }

        String lowerKeywords = locationKeywords.toLowerCase();
        String lowerDistrict = district != null ? district.toLowerCase() : "";
        String lowerProvince = province != null ? province.toLowerCase() : "";

        // Kiểm tra xem district hoặc province có chứa từ khóa không
        for (String keyword : lowerKeywords.split("\\s+")) {
            if (lowerDistrict.contains(keyword) || lowerProvince.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Suy luận tỉnh/thành phố từ district nếu province null hoặc rỗng
     */
    private String inferProvince(String district, String province) {
        // Nếu province đã có và không phải null/empty thì trả về
        if (province != null && !province.trim().isEmpty() && !"null".equals(province)) {
            return province;
        }

        // Nếu district null hoặc rỗng thì trả về empty
        if (district == null || district.trim().isEmpty()) {
            return "";
        }

        String lowerDistrict = district.toLowerCase();

        // Map các quận/huyện phổ biến với tỉnh/thành phố
        if (lowerDistrict.contains("quận 1") || lowerDistrict.contains("quận 2") ||
                lowerDistrict.contains("quận 3") || lowerDistrict.contains("quận 4") ||
                lowerDistrict.contains("quận 5") || lowerDistrict.contains("quận 6") ||
                lowerDistrict.contains("quận 7") || lowerDistrict.contains("quận 8") ||
                lowerDistrict.contains("quận 9") || lowerDistrict.contains("quận 10") ||
                lowerDistrict.contains("quận 11") || lowerDistrict.contains("quận 12") ||
                lowerDistrict.contains("thủ đức") || lowerDistrict.contains("bình thạnh") ||
                lowerDistrict.contains("gò vấp") || lowerDistrict.contains("phú nhuận") ||
                lowerDistrict.contains("tân bình") || lowerDistrict.contains("tân phú") ||
                lowerDistrict.contains("bình tân") || lowerDistrict.contains("hóc môn") ||
                lowerDistrict.contains("củ chi") || lowerDistrict.contains("nhà bè") ||
                lowerDistrict.contains("cần giờ") || lowerDistrict.contains("tp.hcm") ||
                lowerDistrict.contains("hồ chí minh") || lowerDistrict.contains("sài gòn")) {
            return "Thành Phố Hồ Chí Minh";
        }

        if (lowerDistrict.contains("ba đình") || lowerDistrict.contains("hoàn kiếm") ||
                lowerDistrict.contains("hai bà trưng") || lowerDistrict.contains("đống đa") ||
                lowerDistrict.contains("tây hồ") || lowerDistrict.contains("cầu giấy") ||
                lowerDistrict.contains("thanh xuân") || lowerDistrict.contains("hoàng mai") ||
                lowerDistrict.contains("long biên") || lowerDistrict.contains("nam từ liêm") ||
                lowerDistrict.contains("bắc từ liêm") || lowerDistrict.contains("hà nội")) {
            return "Hà Nội";
        }

        if (lowerDistrict.contains("hải châu") || lowerDistrict.contains("thanh khê") ||
                lowerDistrict.contains("sơn trà") || lowerDistrict.contains("ngũ hành sơn") ||
                lowerDistrict.contains("liên chiểu") || lowerDistrict.contains("cẩm lệ") ||
                lowerDistrict.contains("đà nẵng")) {
            return "Đà Nẵng";
        }

        if (lowerDistrict.contains("nha trang") || lowerDistrict.contains("cam ranh") ||
                lowerDistrict.contains("khánh hòa")) {
            return "Khánh Hòa";
        }

        if (lowerDistrict.contains("quảng trị")) {
            return "Quảng Trị";
        }

        if (lowerDistrict.contains("huế") || lowerDistrict.contains("thừa thiên")) {
            return "Thừa Thiên Huế";
        }

        if (lowerDistrict.contains("đà lạt") || lowerDistrict.contains("lâm đồng")) {
            return "Lâm Đồng";
        }

        if (lowerDistrict.contains("vũng tàu") || lowerDistrict.contains("bà rịa")) {
            return "Bà Rịa - Vũng Tàu";
        }

        if (lowerDistrict.contains("phú quốc") || lowerDistrict.contains("kiên giang")) {
            return "Kiên Giang";
        }

        // Nếu không match được thì trả về district làm province
        return district;
    }

    @Builder
    @Data
    public static class HotelSuggestionDTO {
        private Long id;
        private String name;
        private String district;
        private String province;
        private double minPrice;
        private int star;
        private String url;
    }

    @CacheEvict(value = "hotelSearch", allEntries = true)
    public void updateHotelInIndex(Long hotelId) {
        try {
            deleteHotelDocuments(hotelId);
            Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
            if (hotel != null) {
                List<Document> newDocs = new ArrayList<>();
                newDocs.add(createHotelOverviewDocument(hotel));
                newDocs.add(createLocationDocument(hotel));
                for (Room room : hotel.getRooms()) {
                    if (room.isAvailable()) {
                        newDocs.add(createRoomDocument(hotel, room));
                    }
                }
                vectorStore.add(newDocs);
            }
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật index cho khách sạn {}", hotelId, e);
        }
    }

    private void deleteHotelDocuments(Long hotelId) {
        try {
            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder().query("*").topK(1000).build()
            );

            List<String> docIdsToDelete = existingDocs.stream()
                    .filter(doc -> hotelId.equals(doc.getMetadata().get("hotelId")))
                    .map(doc -> doc.getMetadata().get("id"))
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            if (!docIdsToDelete.isEmpty()) {
                vectorStore.delete(docIdsToDelete);
            }
        } catch (Exception e) {
            log.warn("Không thể xóa documents cũ của hotel {}: {}", hotelId, e.getMessage());
        }
    }



    private double getMinPrice(Hotel hotel) {
        return hotel.getRooms().stream().mapToDouble(Room::getPricePerNight).min().orElse(0);
    }

    private double getMaxPrice(Hotel hotel) {
        return hotel.getRooms().stream().mapToDouble(Room::getPricePerNight).max().orElse(0);
    }

    private String getRoomTypes(Hotel hotel) {
        return hotel.getRooms().stream().map(room -> room.getTypeRoom().toString()).distinct().collect(Collectors.joining(", "));
    }

    private boolean hasAvailableRooms(Hotel hotel) {
        return hotel.getRooms().stream().anyMatch(Room::isAvailable);
    }

    private String formatPrice(double price) {
        return String.format("%,.0f", price);
    }

    private String safeString(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }
}