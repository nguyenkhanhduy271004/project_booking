package com.booking.booking.service.interfaces;


import com.booking.booking.dto.request.EvaluateRequest;
import com.booking.booking.dto.response.EvaluateResponse;
import com.booking.booking.model.Evaluate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EvaluateService {

  void createEvaluate(EvaluateRequest evaluateRequest);

  void updateEvaluate(Long id, EvaluateRequest evaluateRequest);

  List<EvaluateResponse> getEvaluatesByRoomId(Long roomId);

  Page<EvaluateResponse> getAllEvaluates(Pageable pageable);

  void deleteEvaluate(Long id);

}
