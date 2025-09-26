package com.booking.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tbl_hotel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Hotel extends AbstractEntity<Long> implements Serializable {

    @Column(nullable = false)
    private String name;

    private int totalRooms;
    private String imageUrl;
    private String district;
    private String province;
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

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("staff-hotel")
    @Builder.Default
    private List<User> staffs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "hotel_services", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "service")
    @Builder.Default
    private List<String> services = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by")
    @JsonIgnore
    private User manager;
}
