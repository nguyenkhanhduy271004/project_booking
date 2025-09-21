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

  public EvaluateResponse toEvaluateResponse(Evaluate evaluate) {
    EvaluateResponse response = modelMapper.map(evaluate, EvaluateResponse.class);

    if (evaluate.getRoom() != null) {
      response.setRoomId(evaluate.getRoom().getId());
      response.setRoomType(evaluate.getRoom().getTypeRoom());

      if (evaluate.getRoom().getHotel() != null) {
        response.setHotelId(evaluate.getRoom().getHotel().getId());
        response.setHotelName(evaluate.getRoom().getHotel().getName());
      }
    }

    if (evaluate.getCreatedBy() != null) {
      response.setUserId(evaluate.getCreatedBy().getId());
      response.setUserName(evaluate.getCreatedBy().getFirstName() + " " + evaluate.getCreatedBy().getLastName());
    }

    return response;
  }

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
