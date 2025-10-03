package com.booking.booking.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "tbl_evaluate")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evaluate extends AbstractEntity<Long> implements Serializable {

    private String message;
    private Float starRating;

    @ManyToOne
    @JoinColumn(name = "room_id")
    @JsonBackReference
    private Room room;

}
