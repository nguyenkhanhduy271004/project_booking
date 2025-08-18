package com.booking.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_group")
public class Group extends AbstractEntity<Integer> {

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @OneToOne
  @JoinColumn(name = "role_id")
  private Role role;
}
