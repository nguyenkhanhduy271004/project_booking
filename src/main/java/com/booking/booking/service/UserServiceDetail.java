package com.booking.booking.service;

import com.booking.booking.model.User;
import com.booking.booking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public record UserServiceDetail(UserRepository userRepository) {


  public UserDetailsService UserDetailsService() {
    return username -> {
      System.out.println("Loading user with username: " + username);
      User user = userRepository.findByUsernameAndIsDeletedFalseWithRoles(username);
      System.out.println("Found user: " + (user != null ? user.getUsername() : "null"));
      if (user == null) {
        throw new UsernameNotFoundException("User not found with username: " + username);
      }
      return user;
    };
  }
}
