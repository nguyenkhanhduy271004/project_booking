package com.booking.booking.repository;

import com.booking.booking.model.Evaluate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluateRepository extends JpaRepository<Evaluate, Long> {

  Evaluate findByRoomId(Long roomId);
}
