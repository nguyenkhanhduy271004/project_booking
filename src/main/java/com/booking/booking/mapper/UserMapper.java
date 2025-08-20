package com.booking.booking.mapper;

import com.booking.booking.common.Gender;
import com.booking.booking.common.UserStatus;
import com.booking.booking.common.UserType;
import com.booking.booking.controller.request.RegisterRequest;
import com.booking.booking.controller.request.UserCreationRequest;
import com.booking.booking.controller.request.UserUpdateRequest;
import com.booking.booking.controller.response.UserResponse;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.model.Role;
import com.booking.booking.model.User;
import com.booking.booking.model.UserHasRole;
import com.booking.booking.repository.RoleRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

  private final ModelMapper modelMapper;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

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
    newUser.setStatus(UserStatus.NONE);
    newUser.setPassword(passwordEncoder.encode(req.getPassword()));

    Role role = roleRepository.findByName(req.getType());
    if (role == null) {
      throw new BadRequestException("Invalid role: " + req.getType());
    }

    UserHasRole userHasRole = new UserHasRole(newUser, role);
    newUser.setRoles(new HashSet<>());
    newUser.getRoles().add(userHasRole);

    return newUser;
  }

  public User toUserEntity(UserUpdateRequest req, User existingUser) {
    if (req.getFirstName() != null) {
      existingUser.setFirstName(req.getFirstName());
    }
    if (req.getLastName() != null) {
      existingUser.setLastName(req.getLastName());
    }
    if (req.getEmail() != null) {
      existingUser.setEmail(req.getEmail());
    }
    if (req.getPhone() != null) {
      existingUser.setPhone(req.getPhone());
    }
    if (req.getType() != null) {
      existingUser.setType(UserType.valueOf(req.getType()));
    }
    if (req.getGender() != null) {
      existingUser.setGender(Gender.valueOf(req.getGender()));
    }
    if (req.getBirthday() != null) {
      existingUser.setBirthDay(parseDayOfBirth(req.getBirthday()));
    }

    return existingUser;
  }

  public User toUserEntity(com.booking.booking.dto.UserDTO userDTO) {
    User user = new User();
    user.setUsername(userDTO.getUsername());
    user.setFirstName(userDTO.getFirstName());
    user.setLastName(userDTO.getLastName());
    user.setEmail(userDTO.getEmail());
    user.setPhone(userDTO.getPhone());

    if (userDTO.getGender() != null) {
      user.setGender(com.booking.booking.common.Gender.valueOf(userDTO.getGender()));
    }

    if (userDTO.getBirthday() != null) {
      user.setBirthDay(parseDayOfBirth(userDTO.getBirthday()));
    }

    if (userDTO.getType() != null) {
      user.setType(com.booking.booking.common.UserType.valueOf(userDTO.getType()));
    }

    if (userDTO.getPassword() != null) {
      user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
    }

    user.setStatus(com.booking.booking.common.UserStatus.NONE);

    // Set role based on type
    if (userDTO.getType() != null) {
      Role role = roleRepository.findByName(userDTO.getType());
      if (role == null) {
        throw new BadRequestException("Invalid role: " + userDTO.getType());
      }

      com.booking.booking.model.UserHasRole userHasRole = new com.booking.booking.model.UserHasRole(user, role);
      user.setRoles(new HashSet<>());
      user.getRoles().add(userHasRole);
    }

    return user;
  }

  public void updateUserFromDTO(User existingUser, com.booking.booking.dto.UserDTO userDTO) {
    if (userDTO.getFirstName() != null) {
      existingUser.setFirstName(userDTO.getFirstName());
    }
    if (userDTO.getLastName() != null) {
      existingUser.setLastName(userDTO.getLastName());
    }
    if (userDTO.getEmail() != null) {
      existingUser.setEmail(userDTO.getEmail());
    }
    if (userDTO.getPhone() != null) {
      existingUser.setPhone(userDTO.getPhone());
    }
    if (userDTO.getGender() != null) {
      existingUser.setGender(com.booking.booking.common.Gender.valueOf(userDTO.getGender()));
    }
    if (userDTO.getBirthday() != null) {
      existingUser.setBirthDay(parseDayOfBirth(userDTO.getBirthday()));
    }
    if (userDTO.getType() != null) {
      existingUser.setType(com.booking.booking.common.UserType.valueOf(userDTO.getType()));

      // Update role
      Role role = roleRepository.findByName(userDTO.getType());
      if (role != null) {
        existingUser.getRoles().clear();
        com.booking.booking.model.UserHasRole userHasRole = new com.booking.booking.model.UserHasRole(existingUser,
            role);
        existingUser.getRoles().add(userHasRole);
      }
    }
  }

  private Date parseDayOfBirth(String birthDay) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate localDate = LocalDate.parse(birthDay, formatter);

    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
