package com.booking.booking.service;

import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.repository.HotelRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelRAGService {

    private final VectorStore vectorStore;

    private final HotelRepository hotelRepository;

    @Value("${frontend.url}")
    private String frontendUrl;


    public List<HotelSuggestionDTO> searchAndAnswer(String question) {
        long startTime = System.currentTimeMillis();

        try {
            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(10)
                            .build()
            );

            if (relevantDocs == null || relevantDocs.isEmpty()) {
                log.info("Không tìm thấy document phù hợp với câu hỏi: {}", question);
                return Collections.emptyList();
            }

            log.debug("Tìm thấy {} documents từ vector search cho câu hỏi: '{}'", relevantDocs.size(), question);

            Set<Long> addedHotelIds = new HashSet<>();
            List<HotelSuggestionDTO> suggestions = new ArrayList<>(5);

            String locationKeywords = extractLocationKeywords(question);
            boolean hasLocationFilter = !locationKeywords.isEmpty();
            log.debug("Keyword địa điểm rút trích: '{}'", locationKeywords);

            for (Document doc : relevantDocs) {
                if (suggestions.size() >= 5) break;
                Map<String, Object> meta = doc.getMetadata();

                log.debug("Đang xử lý document: type={}, hotelId={}, hotelName={}",
                        meta.get("type"), meta.get("hotelId"), meta.get("hotelName"));

                if (!"hotel_overview".equals(meta.get("type"))) {
                    log.debug("Bỏ qua document vì type không phải hotel_overview: {}", meta.get("type"));
                    continue;
                }
                boolean hasRooms = hasAvailableRooms(meta);
                log.debug("Khách sạn '{}': hasAvailableRooms = {}", meta.get("hotelName"), hasRooms);
                if (!hasRooms) {
                    log.debug("Bỏ qua khách sạn '{}' vì không có phòng trống", meta.get("hotelName"));
                    continue;
                }

                Long hotelId = parseLongOrDefault(meta.get("hotelId"), -1L);
                if (hotelId == -1L) {
                    log.debug("Bỏ qua document vì hotelId không hợp lệ: {}", meta.get("hotelId"));
                    continue;
                }
                if (addedHotelIds.contains(hotelId)) {
                    log.debug("Bỏ qua khách sạn '{}' vì đã được thêm vào danh sách", meta.get("hotelName"));
                    continue;
                }

                String name = safeString(meta.get("hotelName"));
                String district = normalizeLocation(safeString(meta.get("district")));
                String province = normalizeLocation(safeString(meta.get("province")));

                if (hasLocationFilter && !isLocationRelevant(locationKeywords, district, province)) {
                    log.debug("Loại khách sạn '{}' vì không khớp location với '{}': [province='{}', district='{}']",
                            name, locationKeywords, province, district);
                    continue;
                } else if (hasLocationFilter) {
                    log.debug("Khách sạn '{}' khớp với location filter '{}': [province='{}', district='{}']",
                            name, locationKeywords, province, district);
                }

                double minPrice = parseDoubleOrDefault(meta.get("minPrice"), 0.0);
                int star = parseIntOrDefault(meta.get("starRating"), 0);
                if (minPrice <= 0) {
                    log.debug("Bỏ qua khách sạn '{}' vì minPrice <= 0: {}", name, minPrice);
                    continue;
                }

                log.debug("Khách sạn '{}' đã vượt qua tất cả filter, thêm vào kết quả", name);

                HotelSuggestionDTO dto = HotelSuggestionDTO.builder()
                        .id(hotelId)
                        .name(name)
                        .district(district)
                        .province(province)
                        .minPrice(minPrice)
                        .star(star)
                        .url("frontendUrl" + "/" + hotelId)
                        .build();

                suggestions.add(dto);
                addedHotelIds.add(hotelId);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Tìm thấy {} khách sạn phù hợp cho câu hỏi: '{}' ({}ms)", suggestions.size(), question, totalTime);
            return suggestions;

        } catch (Exception e) {
            log.error("Lỗi khi xử lý câu hỏi: {}", question, e);
            return Collections.emptyList();
        }
    }

    private boolean isLocationRelevant(String keyword, String district, String province) {
        String normKeyword = normalizeLocation(keyword);
        String normDistrict = normalizeLocation(district);
        String normProvince = normalizeLocation(province);

        log.debug("Kiểm tra location relevance: keyword='{}', district='{}', province='{}'",
                normKeyword, normDistrict, normProvince);

        // Kiểm tra exact match với province (ưu tiên cao nhất)
        if (normKeyword.equals(normProvince)) {
            log.debug("Khớp location: exact match với province");
            return true;
        }

        // Kiểm tra exact match với district
        if (normKeyword.equals(normDistrict)) {
            log.debug("Khớp location: exact match với district");
            return true;
        }

        // Kiểm tra province chứa keyword (ví dụ: "khánh hòa" chứa "khánh")
        if (normProvince.contains(normKeyword) && normKeyword.length() >= 3) {
            log.debug("Khớp location: province chứa keyword");
            return true;
        }

        // Kiểm tra district chứa keyword
        if (normDistrict.contains(normKeyword) && normKeyword.length() >= 3) {
            log.debug("Khớp location: district chứa keyword");
            return true;
        }

        // Kiểm tra các biến thể tên địa điểm
        boolean variantMatch = isLocationVariant(normKeyword, normDistrict) || isLocationVariant(normKeyword, normProvince);
        if (variantMatch) {
            log.debug("Khớp location: variant matching");
        } else {
            log.debug("Không khớp location với bất kỳ điều kiện nào");
        }
        return variantMatch;
    }

    private String normalizeLocation(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private boolean isLocationVariant(String keyword, String location) {
        if (keyword.isEmpty() || location.isEmpty()) return false;

        // Loại bỏ các từ phổ biến
        String cleanKeyword = keyword.replaceAll("\\b(tỉnh|thành phố|tp|quận|huyện|thị xã|thành thị)\\b", "").trim();
        String cleanLocation = location.replaceAll("\\b(tỉnh|thành phố|tp|quận|huyện|thị xã|thành thị)\\b", "").trim();

        // Kiểm tra sau khi loại bỏ từ phổ biến
        return cleanKeyword.contains(cleanLocation) || cleanLocation.contains(cleanKeyword);
    }

    private String safeString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private boolean hasAvailableRooms(Map<String, Object> meta) {
        Object raw = meta.get("hasAvailableRooms");
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof String) return Boolean.parseBoolean((String) raw);
        return false;
    }

    private int parseIntOrDefault(Object value, int defaultVal) {
        try {
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private double parseDoubleOrDefault(Object value, double defaultVal) {
        try {
            if (value instanceof Number) return ((Number) value).doubleValue();
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private Long parseLongOrDefault(Object value, long defaultVal) {
        try {
            if (value instanceof Number) return ((Number) value).longValue();
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String extractLocationKeywords(String question) {
        question = question.toLowerCase().trim();

        // Danh sách các địa điểm và biến thể của chúng
        Map<String, List<String>> locationVariants = new HashMap<>();
        locationVariants.put("hà nội", List.of("hà nội", "hanoi", "thủ đô"));
        locationVariants.put("tp. hồ chí minh", List.of("tp. hồ chí minh", "hồ chí minh", "sài gòn", "saigon", "tphcm"));
        locationVariants.put("đà nẵng", List.of("đà nẵng", "da nang", "danang"));
        locationVariants.put("huế", List.of("huế", "hue", "thừa thiên huế"));
        locationVariants.put("quảng trị", List.of("quảng trị", "quang tri"));
        locationVariants.put("khánh hòa", List.of("khánh hòa", "khanh hoa", "nha trang"));
        locationVariants.put("cần thơ", List.of("cần thơ", "can tho"));
        locationVariants.put("tây ninh", List.of("tây ninh", "tay ninh"));
        locationVariants.put("hội an", List.of("hội an", "hoi an"));
        locationVariants.put("đắk lắk", List.of("đắk lắk", "dak lak", "buôn ma thuột"));
        locationVariants.put("lâm đồng", List.of("lâm đồng", "lam dong", "đà lạt", "da lat", "bảo lộc"));
        locationVariants.put("bình dương", List.of("bình dương", "binh duong", "thủ dầu một"));
        locationVariants.put("đồng nai", List.of("đồng nai", "dong nai", "biên hòa"));
        locationVariants.put("bà rịa vũng tàu", List.of("bà rịa vũng tàu", "vũng tàu", "vung tau"));
        locationVariants.put("bình định", List.of("bình định", "binh dinh", "quy nhon"));
        locationVariants.put("bình thuận", List.of("bình thuận", "binh thuan", "phan thiết"));
        locationVariants.put("quảng bình", List.of("quảng bình", "quang binh", "đồng hới"));
        locationVariants.put("quảng nam", List.of("quảng nam", "quang nam", "tam kỳ"));
        locationVariants.put("quảng ngãi", List.of("quảng ngãi", "quang ngai"));
        locationVariants.put("quảng ninh", List.of("quảng ninh", "quang ninh", "hạ long", "ha long"));
        locationVariants.put("phú yên", List.of("phú yên", "phu yen", "tuy hòa"));
        locationVariants.put("ninh thuận", List.of("ninh thuận", "ninh thuan", "phan rang"));
        locationVariants.put("lào cai", List.of("lào cai", "lao cai", "sapa"));
        locationVariants.put("nghệ an", List.of("nghệ an", "nghe an", "vinh"));
        locationVariants.put("thanh hóa", List.of("thanh hóa", "thanh hoa"));
        locationVariants.put("hải phòng", List.of("hải phòng", "hai phong"));
        locationVariants.put("kiên giang", List.of("kiên giang", "kien giang", "rạch giá"));
        locationVariants.put("an giang", List.of("an giang", "long xuyên"));
        locationVariants.put("tiền giang", List.of("tiền giang", "tien giang", "mỹ tho"));

        // Tìm địa điểm khớp với câu hỏi
        for (Map.Entry<String, List<String>> entry : locationVariants.entrySet()) {
            String mainLocation = entry.getKey();
            List<String> variants = entry.getValue();

            for (String variant : variants) {
                if (question.contains(variant)) {
                    log.debug("Tìm thấy địa điểm '{}' từ variant '{}'", mainLocation, variant);
                    return mainLocation;
                }
            }
        }

        log.debug("Không tìm thấy địa điểm nào trong câu hỏi: '{}'", question);
        return "";
    }

    public void indexAllHotels() {
        log.info("Bắt đầu indexing tất cả khách sạn vào vector store...");
        long startTime = System.currentTimeMillis();

        try {
            List<Hotel> hotels = hotelRepository.findAllWithRooms();
            log.info("Tìm thấy {} khách sạn để index", hotels.size());

            List<Document> documents = new ArrayList<>();

            for (Hotel hotel : hotels) {
                if (hotel.isDeleted()) continue;

                // Tạo document cho hotel overview
                Document hotelDoc = createHotelDocument(hotel);
                if (hotelDoc != null) {
                    documents.add(hotelDoc);
                }

                // Tạo documents cho từng phòng
                if (hotel.getRooms() != null) {
                    for (Room room : hotel.getRooms()) {
                        if (!room.isDeleted() && room.isAvailable()) {
                            Document roomDoc = createRoomDocument(room, hotel);
                            if (roomDoc != null) {
                                documents.add(roomDoc);
                            }
                        }
                    }
                }
            }

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Đã index {} documents vào vector store", documents.size());
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Hoàn thành indexing trong {}ms", totalTime);

        } catch (Exception e) {
            log.error("Lỗi khi indexing khách sạn", e);
            throw new RuntimeException("Lỗi khi indexing khách sạn", e);
        }
    }

    private Document createHotelDocument(Hotel hotel) {
        try {
            // Tính giá phòng min/max
            double minPrice = 0;
            double maxPrice = 0;
            boolean hasAvailableRooms = false;

            if (hotel.getRooms() != null && !hotel.getRooms().isEmpty()) {
                log.debug("Khách sạn '{}' có {} phòng", hotel.getName(), hotel.getRooms().size());

                long availableRooms = hotel.getRooms().stream()
                        .filter(room -> !room.isDeleted() && room.isAvailable())
                        .count();
                log.debug("Trong đó {} phòng available và không bị deleted", availableRooms);

                List<Double> prices = hotel.getRooms().stream()
                        .filter(room -> !room.isDeleted() && room.isAvailable())
                        .map(Room::getPricePerNight)
                        .filter(Objects::nonNull)
                        .toList();

                log.debug("Có {} phòng có giá hợp lệ", prices.size());

                if (!prices.isEmpty()) {
                    minPrice = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                    maxPrice = prices.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    hasAvailableRooms = true;
                    log.debug("Khách sạn '{}' có phòng trống với giá từ {} đến {}", hotel.getName(), minPrice, maxPrice);
                } else {
                    log.debug("Khách sạn '{}' không có phòng nào có giá hợp lệ", hotel.getName());
                }
            } else {
                log.debug("Khách sạn '{}' không có phòng nào", hotel.getName());
            }

            // Tạo text mô tả khách sạn
            String text = String.format(
                    "📍 KHÁCH SẠN TẠI %s, %s: %s\n" +
                            "🏨 Tên: %s\n" +
                            "📍 Địa chỉ: %s, %s, %s\n" +
                            "⭐ Xếp hạng: %.1f sao\n" +
                            "🛏️ Tổng số phòng: %d phòng\n" +
                            "💰 Khoảng giá: %,.0f VND - %,.0f VND VND/đêm\n" +
                            "🏷️ Loại phòng có sẵn: %s\n" +
                            "✅ Tình trạng: %s\n",
                    hotel.getDistrict(), hotel.getProvince(), hotel.getName(),
                    hotel.getName(),
                    hotel.getAddressDetail(), hotel.getDistrict(), hotel.getProvince(),
                    hotel.getStarRating(),
                    hotel.getTotalRooms(),
                    minPrice, maxPrice,
                    getRoomTypesString(hotel),
                    hasAvailableRooms ? "Còn phòng trống" : "Hết phòng"
            );

            // Tạo metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "hotel_overview");
            metadata.put("hotelId", hotel.getId());
            metadata.put("hotelName", hotel.getName());
            metadata.put("district", hotel.getDistrict());
            metadata.put("province", hotel.getProvince());
            metadata.put("starRating", hotel.getStarRating());
            metadata.put("minPrice", minPrice);
            metadata.put("maxPrice", maxPrice);
            metadata.put("hasAvailableRooms", hasAvailableRooms);
            metadata.put("id", "hotel_" + hotel.getId());

            return new Document(text, metadata);

        } catch (Exception e) {
            log.error("Lỗi khi tạo document cho khách sạn {}", hotel.getId(), e);
            return null;
        }
    }

    private Document createRoomDocument(Room room, Hotel hotel) {
        try {
            String text = String.format(
                    "🛏️ PHÒNG: %s\n" +
                            "🔢 Sức chứa: %d người\n" +
                            "💰 Giá: %,.0f VND/đêm\n" +
                            "✅ Trạng thái: %s\n" +
                            "🏨 Khách sạn: %s (%s, %s)\n",
                    room.getTypeRoom() != null ? room.getTypeRoom().name() : "STANDARD",
                    room.getCapacity(),
                    room.getPricePerNight(),
                    room.isAvailable() ? "Có sẵn" : "Không có sẵn",
                    hotel.getName(), hotel.getDistrict(), hotel.getProvince()
            );

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "room");
            metadata.put("hotelId", hotel.getId());
            metadata.put("hotelName", hotel.getName());
            metadata.put("district", hotel.getDistrict());
            metadata.put("province", hotel.getProvince());
            metadata.put("id", "room_" + room.getId());

            return new Document(text, metadata);

        } catch (Exception e) {
            log.error("Lỗi khi tạo document cho phòng {}", room.getId(), e);
            return null;
        }
    }

    private String getRoomTypesString(Hotel hotel) {
        if (hotel.getRooms() == null || hotel.getRooms().isEmpty()) {
            return "";
        }

        return hotel.getRooms().stream()
                .filter(room -> !room.isDeleted() && room.isAvailable())
                .map(room -> room.getTypeRoom() != null ? room.getTypeRoom().name() : "STANDARD")
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
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
}
