package com.booking.booking.util;

import com.booking.booking.model.User;
import com.booking.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserContext {

  private final UserRepository userRepository;

  public User getCurrentUser() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    if (principal instanceof UserDetails) {
      String username = ((UserDetails) principal).getUsername();
      return userRepository.findByUsernameAndIsDeletedFalse(username);
    }

    return null;
  }
}
