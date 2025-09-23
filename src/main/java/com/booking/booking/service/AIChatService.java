package com.booking.booking.service;

import com.booking.booking.model.Hotel;
import com.booking.booking.model.Room;
import com.booking.booking.repository.HotelRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIChatService {

    private final HotelRepository hotelRepository;
    private final ChatClient chatClient;

    public AIChatService(HotelRepository hotelRepository, ChatClient.Builder chatClient) {
        this.hotelRepository = hotelRepository;
        this.chatClient = chatClient
                .defaultSystem("You are a hotel booking assistant. Based on the user's question, help them find a suitable hotel room. Only answer based on the given context.")
                .build();
    }

    public String buildHotelContext(List<Hotel> hotels) {
        StringBuilder contextBuilder = new StringBuilder();

        for (Hotel hotel : hotels) {
            contextBuilder.append("🏨 Hotel: ").append(hotel.getName()).append("\n");
            contextBuilder.append("📍 Address: ").append(hotel.getAddressDetail()).append(", District: ").append(hotel.getDistrict()).append("\n");
            contextBuilder.append("⭐ Rating: ").append(hotel.getStarRating()).append(" stars").append("\n");

            List<Room> rooms = hotel.getRooms();
            if (rooms.isEmpty()) {
                contextBuilder.append("⚠️ No rooms listed.\n");
            } else {
                contextBuilder.append("🛏️ Rooms:\n");
                for (Room room : rooms) {
                    contextBuilder.append(String.format(
                            "   - Type: %s | Price: %,d VND | Capacity: %d | Available: %s\n",
                            room.getTypeRoom(),
                            (long) room.getPricePerNight(),
                            room.getCapacity(),
                            room.isAvailable() ? "✅" : "❌"
                    ));
                }
            }

            contextBuilder.append("---\n");
        }

        return contextBuilder.toString();
    }


    public String answerQuestion(String question) {

        String context = buildHotelContext(hotelRepository.findAll());

        String aiResponse = chatClient.prompt()
                .system("You are a hotel booking assistant. Based on the context, suggest the best options for the user.")
                .user("Context:\n" + context + "\n\nUser question: " + question)
                .call()
                .content();

        return aiResponse;
    }


}
