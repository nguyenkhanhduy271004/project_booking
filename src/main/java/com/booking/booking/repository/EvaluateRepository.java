package com.booking.booking.repository;

import com.booking.booking.model.Evaluate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluateRepository extends JpaRepository<Evaluate, Long> {

    List<Evaluate> findByRoomId(Long roomId);

    @Query("SELECT e FROM Evaluate e WHERE e.hotel.id = :hotelId")
    Page<Evaluate> findEvaluatesByHotelId(@Param("hotelId") Long hotelId, Pageable pageable);

    Page<Evaluate> findAll(Pageable pageable);
}
