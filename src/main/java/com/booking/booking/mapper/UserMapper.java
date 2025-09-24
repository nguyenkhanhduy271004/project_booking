package com.booking.booking.mapper;

import com.booking.booking.common.Gender;
import com.booking.booking.common.UserStatus;
import com.booking.booking.common.UserType;
import com.booking.booking.dto.request.RegisterRequest;
import com.booking.booking.dto.request.UserCreationRequest;
import com.booking.booking.dto.response.UserResponse;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.model.Hotel;
import com.booking.booking.model.Role;
import com.booking.booking.model.User;
import com.booking.booking.model.UserHasRole;
import com.booking.booking.repository.HotelRepository;
import com.booking.booking.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HotelRepository hotelRepository;

    public LocalDate convertToLocalDate(java.util.Date date, String zoneId) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.of(zoneId)).toLocalDate();
    }

    public UserResponse toUserResponse(User user) {

        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        if (user.getBirthDay() != null) {
            userResponse.setBirthday(convertToLocalDate(user.getBirthDay(), "Asia/Ho_Chi_Minh"));
        }
        userResponse.setFullName(user.getFirstName() + " " + user.getLastName());
        return userResponse;

    }

    public User toUserEntity(RegisterRequest registerRequest) {

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setGender(Gender.valueOf(registerRequest.getGender()));
        user.setPhone(registerRequest.getPhoneNumber());
        user.setType(UserType.GUEST);
        user.setStatus(UserStatus.INACTIVE);

        Role role = roleRepository.findByName("GUEST");
        if (role == null) {
            throw new BadRequestException("Invalid role: GUEST");
        }

        UserHasRole userHasRole = new UserHasRole(user, role);
        user.setRoles(new HashSet<>());
        user.getRoles().add(userHasRole);

        return user;
    }

    public User toUserEntity(UserCreationRequest req) {
        User newUser = new User();
        newUser.setFirstName(req.getFirstName());
        newUser.setLastName(req.getLastName());
        newUser.setGender(Gender.valueOf(req.getGender()));
        newUser.setBirthDay(parseDayOfBirth(req.getBirthday()));
        newUser.setEmail(req.getEmail());
        newUser.setPhone(req.getPhone());
        newUser.setUsername(req.getUsername());
        newUser.setType(UserType.valueOf(req.getType()));
        newUser.setStatus(UserStatus.INACTIVE);
        newUser.setPassword(passwordEncoder.encode(req.getPassword()));

        if (req.getHotelId() != null) {
            Hotel hotel = hotelRepository.findById(req.getHotelId()).orElseThrow(() -> new ResourceNotFoundException("Hotel not found"));

            newUser.setHotel(hotel);
        }

        Role role = roleRepository.findByName(req.getType());
        if (role == null) {
            throw new BadRequestException("Invalid role: " + req.getType());
        }

        UserHasRole userHasRole = new UserHasRole(newUser, role);
        newUser.setRoles(new HashSet<>());
        newUser.getRoles().add(userHasRole);

        return newUser;
    }


    private Date parseDayOfBirth(String birthDay) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(birthDay, formatter);

        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
