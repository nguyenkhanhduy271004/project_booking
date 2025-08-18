package com.booking.booking.model;

import com.booking.booking.common.TypeRoom;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "tbl_room")
public class Room extends AbstractEntity<Long> implements Serializable {

  @Enumerated(EnumType.STRING)
  private TypeRoom typeRoom;
  private int capacity;
  private double pricePerNight;
  private boolean available;

  @ElementCollection
  @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "room_id"))
  @Column(name = "image_url")
  @Builder.Default
  private List<String> listImageUrl = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "hotel_id", nullable = false)
  @JsonIgnore
  private Hotel hotel;

  @ManyToMany(mappedBy = "rooms")
  @Builder.Default
  @JsonIgnore
  private List<Booking> bookings = new ArrayList<>();

  @OneToMany(mappedBy = "room")
  private List<Evaluate> evaluates = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "created_by")
  @JsonBackReference(value = "room-created-by")
  private User createdByUser;

  @ManyToOne
  @JoinColumn(name = "updated_by")
  @JsonBackReference(value = "room-updated-by")
  private User updatedByUser;

}
