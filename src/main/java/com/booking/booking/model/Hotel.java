package com.booking.booking.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "tbl_hotel")
public class Hotel extends AbstractEntity<Long> implements Serializable {

    @Column(name = "name", nullable = false)
    private String name;
    private int totalRooms;
    private String imageUrl;
    private String district;
    private double starRating;
    private String addressDetail;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "hotel")
    @JsonIgnore
    @Builder.Default
    private Set<Voucher> vouchers = new HashSet<>();

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "hotel")
    private List<User> staffs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "hotel_services", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "service")
    @Builder.Default
    private List<String> services = new ArrayList<>();

    

    @ManyToOne
    @JoinColumn(name = "managed_by")
    @JsonBackReference(value = "hotel-managed-by")
    private User managedByUser;

}
