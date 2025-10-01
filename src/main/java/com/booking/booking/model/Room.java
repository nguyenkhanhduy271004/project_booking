package com.booking.booking.model;

import com.booking.booking.common.TypeRoom;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "tbl_room")
public class Room extends AbstractEntity<Long> implements Serializable {

    private int capacity;
    @Enumerated(EnumType.STRING)
    private TypeRoom typeRoom;
    private boolean available;
    private double pricePerNight;
    private LocalDateTime holdExpiresAt;

    @Column(name = "held_by_user_id")
    private Long heldByUserId;

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

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Evaluate> evaluates = new ArrayList<>();


    @ElementCollection
    @CollectionTable(name = "room_services", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "service")
    @Builder.Default
    private List<String> services = new ArrayList<>();

}
