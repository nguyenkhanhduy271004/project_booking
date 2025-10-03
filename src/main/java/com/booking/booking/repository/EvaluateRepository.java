package com.booking.booking.repository;

import com.booking.booking.model.Evaluate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluateRepository extends JpaRepository<Evaluate, Long> {

  List<Evaluate> findByRoomId(Long roomId);
}
