package com.booking.booking.service.impl;

import com.booking.booking.dto.request.EvaluateRequest;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.mapper.EvaluateMapper;
import com.booking.booking.model.Evaluate;
import com.booking.booking.model.User;
import com.booking.booking.repository.EvaluateRepository;
import com.booking.booking.repository.RoomRepository;
import com.booking.booking.service.EvaluateService;
import com.booking.booking.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "EVALUATE-SERVICE")
@RequiredArgsConstructor
public class EvaluateServiceImpl implements EvaluateService {

  private final EvaluateRepository evaluateRepository;
  private final RoomRepository roomRepository;
  private final EvaluateMapper evaluateMapper;
  private final UserContext userContext;

  @Override
  public void createEvaluate(EvaluateRequest evaluateRequest) {

    evaluateRepository.save(evaluateMapper.toEvaluate(evaluateRequest));

  }

  @Override
  public void updateEvaluate(Long id, EvaluateRequest evaluateRequest) {
    User user = userContext.getCurrentUser();
    Evaluate existEvaluate = evaluateRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Evaluate not found"));

    if (!user.getId().equals(existEvaluate.getCreatedBy().getId())) {
      throw new BadRequestException("User does not belong to this evaluate");
    }
    existEvaluate.setMessage(evaluateRequest.getMessage());
    existEvaluate.setStarRating(evaluateRequest.getStarRating());

    evaluateRepository.save(existEvaluate);
  }


  @Override
  public Evaluate getEvaluatesByRoomId(Long roomId) {

    return evaluateRepository.findByRoomId(roomId);
  }


  @Override
  public void deleteEvaluate(Long id) {
    User user = userContext.getCurrentUser();

    Evaluate existEvaluate = evaluateRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Evaluate not found"));

    if (!user.getId().equals(existEvaluate.getCreatedBy().getId())) {
      throw new BadRequestException("User does not belong to this evaluate");
    }
    evaluateRepository.deleteById(id);
  }
}
