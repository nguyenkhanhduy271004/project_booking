package com.booking.booking.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    private String district;
    private String addressDetail;
    private int totalRooms;
    private double starRating;
    private String imageUrl;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonBackReference(value = "hotel-created-by")
    private User createdByUser;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    @JsonBackReference(value = "hotel-updated-by")
    private User updatedByUser;

    @ManyToOne
    @JoinColumn(name = "managed_by")
    @JsonBackReference(value = "hotel-managed-by")
    private User managedByUser;

    @OneToMany(mappedBy = "hotel")
    private Set<Voucher> vouchers = new HashSet<>();

}
