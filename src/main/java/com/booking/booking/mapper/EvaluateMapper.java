package com.booking.booking.mapper;

import com.booking.booking.dto.request.EvaluateRequest;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Evaluate;
import com.booking.booking.model.Room;
import com.booking.booking.model.User;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluateMapper {

    private final ModelMapper modelMapper;
    private final UserContext userContext;
    private final RoomRepository roomRepository;


    public Evaluate toEvaluate(EvaluateRequest req) {
        User user = userContext.getCurrentUser();
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        Evaluate newEvaluate = modelMapper.map(req, Evaluate.class);

        newEvaluate.setId(null);
        newEvaluate.setRoom(room);
        newEvaluate.setCreatedBy(user);
        return newEvaluate;
    }
}
