//package com.booking.booking.repository;
//
//import com.booking.booking.model.User;
//import org.springframework.beans.factory.annotation.Autowired;
//
//@DataJpaTest
//public class UserRepositoryTest {
//
//  @Autowired
//  private UserRepository userRepository;
//
//  @Test
//  public void UserRepository_SaveAll_ReturnSavedUser() {
//    User user = User.builder().username("nguyenduy123").password("password").build();
//
//    User savedUser = userRepository.save(user);
//
//    Assertions.assertNotNull(savedUser);
//  }
//}
