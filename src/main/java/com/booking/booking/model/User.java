package com.booking.booking.model;

import com.booking.booking.common.Gender;
import com.booking.booking.common.UserStatus;
import com.booking.booking.common.UserType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.*;

@Entity
@Table(name = "tbl_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Slf4j(topic = "UserEntity")
public class User extends AbstractEntity<Long> implements UserDetails, Serializable {

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Temporal(TemporalType.DATE)
    private Date birthDay;

    @Column(length = 255)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(unique = true, nullable = false, length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserType type;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(length = 255)
    private String secretCode;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<UserHasRole> roles = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private Set<GroupHasUser> groups = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private ForgotPassword forgotPassword;

    @OneToMany(mappedBy = "createdBy")
    @JsonIgnore
    private List<Evaluate> evaluates = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    @JsonBackReference("staff-hotel")
    private Hotel hotel;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(UserHasRole::getRole)
                .map(Role::getName)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserStatus.ACTIVE.equals(status);
    }
}

