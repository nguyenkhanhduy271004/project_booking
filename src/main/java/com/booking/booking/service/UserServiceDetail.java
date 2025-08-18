package com.booking.booking.service;

import com.booking.booking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public record UserServiceDetail(UserRepository userRepository) {


  public UserDetailsService UserDetailsService() {
    return userRepository::findByUsernameAndIsDeletedFalse;
  }
}
