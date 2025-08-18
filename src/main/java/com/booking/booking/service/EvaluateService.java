package com.booking.booking.service;


import com.booking.booking.controller.request.EvaluateRequest;
import com.booking.booking.model.Evaluate;

public interface EvaluateService {

  void createEvaluate(EvaluateRequest evaluateRequest);

  void updateEvaluate(Long id, EvaluateRequest evaluateRequest);

  Evaluate getEvaluatesByRoomId(Long roomId);

  void deleteEvaluate(Long id);

}
