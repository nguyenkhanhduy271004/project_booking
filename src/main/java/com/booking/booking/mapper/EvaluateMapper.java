package com.booking.booking.mapper;

import com.booking.booking.dto.request.EvaluateRequest;
import com.booking.booking.dto.response.EvaluateResponse;
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

    public EvaluateResponse toEvaluateResponse(Evaluate evaluate) {
        EvaluateResponse evaluateResponse = modelMapper.map(evaluate, EvaluateResponse.class);

        if (evaluate.getCreatedBy() != null) {
            String firstName = evaluate.getCreatedBy().getFirstName() != null ? evaluate.getCreatedBy().getFirstName() : "";
            String lastName = evaluate.getCreatedBy().getLastName() != null ? evaluate.getCreatedBy().getLastName() : "";
            evaluateResponse.setReviewer((firstName + " " + lastName).trim());
        } else {
            evaluateResponse.setReviewer("Unknown User");
        }

        return evaluateResponse;
    }

}
