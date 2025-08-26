package com.booking.booking.model;

import com.booking.booking.common.Gender;
import com.booking.booking.common.UserStatus;
import com.booking.booking.common.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@Entity
@Table(name = "tbl_user")
@Slf4j(topic = "UserEntity")
public class User extends AbstractEntity<Long> implements UserDetails, Serializable {

  @Column(name = "first_name", length = 255)
  private String firstName;

  @Column(name = "last_name", length = 255)
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender")
  private Gender gender;

  @Column(name = "day_of_birth")
  @Temporal(TemporalType.DATE)
  private Date birthDay;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "phone", length = 15)
  private String phone;

  @Column(name = "username", unique = true, nullable = false, length = 255)
  private String username;

  @Column(name = "password", length = 255)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", length = 255)
  private UserType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 255)
  private UserStatus status;

  @Column(name = "secret_code", length = 255)
  private String secretCode;

  @JsonIgnore
  @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private Set<UserHasRole> roles = new HashSet<>();

  @JsonIgnore
  @OneToMany(mappedBy = "user")
  private Set<GroupHasUser> groups = new HashSet<>();

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private ForgotPassword forgotPassword;

  @OneToMany(mappedBy = "createdBy")
  private List<Evaluate> evaluates = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hotel_id")
  private Hotel hotel;

  public Collection<? extends GrantedAuthority> getAuthorities() {

    List<Role> roleList = roles.stream().map(UserHasRole::getRole).toList();

    List<String> roleNames = roleList.stream().map(Role::getName).toList();
    log.info("User roles: {}", roleNames);

    return roleNames.stream().map(SimpleGrantedAuthority::new).toList();
  }

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return UserStatus.ACTIVE.equals(status);
  }
}
