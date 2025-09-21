//package com.booking.booking.service;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import com.booking.booking.common.UserStatus;
//import com.booking.booking.controller.response.UserResponse;
//import com.booking.booking.exception.ResourceNotFoundException;
//import com.booking.booking.mapper.UserMapper;
//import com.booking.booking.model.User;
//import com.booking.booking.repository.ForgotPasswordRepository;
//import com.booking.booking.repository.RoleRepository;
//import com.booking.booking.repository.UserRepository;
//import com.booking.booking.service.impl.UserServiceImpl;
//import com.booking.booking.util.UserContext;
//import java.util.Optional;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@ExtendWith(MockitoExtension.class)
//public class UserServiceTest {
//
//  private UserService userService;
//
//  private @Mock UserMapper userMapper;
//  private @Mock EmailService emailService;
//  private @Mock UserRepository userRepository;
//  private @Mock RoleRepository roleRepository;
//  private @Mock PasswordEncoder passwordEncoder;
//  private @Mock RedisTemplate<String, String> redisTemplate;
//  private @Mock ForgotPasswordRepository forgotPasswordRepository;
//  private @Mock UserContext userContext;
//
//  private static User user;
//
//  @BeforeAll
//  static void beforeAll() {
//    user = new User();
//    user.setFirstName("Nguyen");
//    user.setLastName("Duy");
//    user.setEmail("nguyenduy@gmail.com");
//    user.setPassword("password");
//    user.setUsername("nguyenduy123");
//  }
//
//  @BeforeEach
//  void beforeEach() {
//    userService = new UserServiceImpl(userMapper, emailService,
//        userRepository, roleRepository,
//        passwordEncoder, redisTemplate, forgotPasswordRepository, userContext);
//  }
//
//  @Test
//  void testFindUserByUsername_Success() {
//    when(userRepository.findByUsernameAndIsDeletedFalse("nguyenduy123")).thenReturn(user);
//
//    UserResponse mapped = new UserResponse();
//    mapped.setUsername("nguyenduy123");
//
//    when(userMapper.toUserResponse(user)).thenReturn(mapped);
//
//    UserResponse result = userService.findByUsername("nguyenduy123");
//    Assertions.assertNotNull(result);
//    assertEquals(mapped.getUsername(), result.getUsername());
//
//  }
//
//  @Test
//  void testFindUserByUsername_Failure() {
//    when(userRepository.findByUsernameAndIsDeletedFalse("nguyenduy123")).thenReturn(null);
//
//    ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.findByUsername("nguyenduy123"));
//    assertEquals("User not found with username: nguyenduy123", thrown.getMessage());
//  }
//
//  @Test
//  void testFindUserByEmail_Success() {
//    when(userRepository.findByEmail("nguyenduy@gmail.com")).thenReturn(Optional.of(user));
//
//    UserResponse mapped = new UserResponse();
//    mapped.setUsername("nguyenduy@gmail.com");
//    when(userMapper.toUserResponse(user)).thenReturn(mapped);
//
//    UserResponse result = userService.findByEmail("nguyenduy@gmail.com");
//    Assertions.assertNotNull(result);
//    assertEquals(mapped.getUsername(), result.getUsername());
//  }
//
//  @Test
//  void testFindUserByEmail_Failure() {
//    when(userRepository.findByEmail("nguyenduy@gmail.com")).thenReturn(Optional.empty());
//
//    ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.findByEmail("nguyenduy@gmail.com"));
//    assertEquals("User not found with email: nguyenduy@gmail.com", thrown.getMessage());
//  }
//
//  @Test
//  void testFindUserById_Success() {
//    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//
//    UserResponse mapped = new UserResponse();
//    mapped.setEmail("john@doe.com");
//
//    when(userMapper.toUserResponse(user)).thenReturn(mapped);
//
//    UserResponse result = userService.findById(1L);
//    Assertions.assertNotNull(result);
//    assertEquals("john@doe.com", result.getEmail());
//  }
//
//  @Test
//  void testFindUserById_Failure() {
//    ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.findById(1L));
//    Assertions.assertEquals("User not found with id: 1", thrown.getMessage());
//  }
//
//  @Test
//  void testDeleteById_Success() {
//    user.setId(1L);
//    user.setDeleted(false);
//    user.setStatus(UserStatus.ACTIVE);
//    user.setDeletedAt(null);
//
//    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//
//    userService.deleteById(1L);
//
//    Assertions.assertTrue(user.isDeleted());
//    Assertions.assertEquals(UserStatus.INACTIVE, user.getStatus());
//    Assertions.assertNotNull(user.getDeletedAt());
//
//    verify(userRepository).save(user);
//  }
//
//  @Test
//  void testDeleteById_Failure() {
//    when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//    ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> userService.deleteById(1L));
//    Assertions.assertEquals("User not found with id: 1", thrown.getMessage());
//  }
//
//  @Test
//  void testDeletePermanentlyById_Success() {
//    Long id = 1L;
//    User mockUser = new User();
//    mockUser.setId(id);
//
//    when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));
//
//    userService.deletePermanentlyById(id);
//
//    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//    verify(userRepository).delete(userCaptor.capture());
//
//    User deletedUser = userCaptor.getValue();
//    assertEquals(id, deletedUser.getId());
//  }
//
//
//
//}
