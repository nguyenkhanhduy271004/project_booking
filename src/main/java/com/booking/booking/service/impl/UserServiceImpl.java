package com.booking.booking.service.impl;

import static com.booking.booking.util.AppConst.SEARCH_SPEC_OPERATOR;

import com.booking.booking.common.Gender;
import com.booking.booking.common.UserStatus;
import com.booking.booking.common.UserType;
import com.booking.booking.controller.request.ChangePasswordRequest;
import com.booking.booking.controller.request.ForgetPasswordRequest;
import com.booking.booking.controller.request.UserCreationRequest;
import com.booking.booking.controller.request.UserPasswordRequest;
import com.booking.booking.controller.request.UserUpdateRequest;
import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.UserPageResponse;
import com.booking.booking.controller.response.UserResponse;
import com.booking.booking.exception.BadRequestException;
import com.booking.booking.exception.DuplicateResourceException;
import com.booking.booking.exception.InvalidUserIdsException;
import com.booking.booking.exception.PasswordNotMatchException;
import com.booking.booking.exception.ResourceNotFoundException;
import com.booking.booking.mapper.UserMapper;
import com.booking.booking.model.ForgotPassword;
import com.booking.booking.model.Role;
import com.booking.booking.model.User;
import com.booking.booking.model.UserHasRole;
import com.booking.booking.repository.ForgotPasswordRepository;
import com.booking.booking.repository.RoleRepository;
import com.booking.booking.repository.UserRepository;
import com.booking.booking.repository.specification.UserSpecificationsBuilder;
import com.booking.booking.service.EmailService;
import com.booking.booking.service.UserService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j(topic = "USER-SERVICE")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final EmailService emailService;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ForgotPasswordRepository forgotPasswordRepository;

  @Override
  public UserPageResponse findAll(String keyword, String sort, int page, int size) {
    log.info("findAll start");

    Sort.Order order = new Sort.Order(Sort.Direction.ASC, "id");
    if (StringUtils.hasLength(sort)) {
      Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)");
      Matcher matcher = pattern.matcher(sort);
      if (matcher.find()) {
        String columnName = matcher.group(1);
        if (matcher.group(3).equalsIgnoreCase("asc")) {
          order = new Sort.Order(Sort.Direction.ASC, columnName);
        } else {
          order = new Sort.Order(Sort.Direction.DESC, columnName);
        }
      }
    }

    int pageNo = 0;
    if (page > 0) {
      pageNo = page - 1;
    }

    // Paging
    Pageable pageable = PageRequest.of(pageNo, size, Sort.by(order));

    Page<User> entityPage;

    if (StringUtils.hasLength(keyword)) {
      keyword = "%" + keyword.toLowerCase() + "%";
      entityPage = userRepository.searchByKeyword(keyword, pageable);
    } else {
      entityPage = userRepository.findAllByIsDeletedFalse(pageable);
    }

    return getUserPageResponse(page, size, entityPage);
  }

  @Override
  public PageResponse<?> advanceSearchWithSpecification(Pageable pageable, String[] user) {

    log.info("getUsersBySpecifications");

    UserSpecificationsBuilder builder = new UserSpecificationsBuilder();

    builder.with("isDeleted", ":", false, null, null);

    if (user != null) {
      Pattern pattern = Pattern.compile(SEARCH_SPEC_OPERATOR);
      for (String s : user) {
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
          builder.with(matcher.group(1), matcher.group(2), matcher.group(4), matcher.group(3),
              matcher.group(5));
        }
      }
    }

    Page<User> users = userRepository.findAll(Objects.requireNonNull(builder.build()),
        pageable);
    return convertToPageResponse(users, pageable);
  }

  @Override
  public UserPageResponse findAllUsersIsDeleted(String keyword, String sort, int page, int size) {
    log.info("findAll start");

    Sort.Order order = new Sort.Order(Sort.Direction.ASC, "id");
    if (StringUtils.hasLength(sort)) {
      Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)");
      Matcher matcher = pattern.matcher(sort);
      if (matcher.find()) {
        String columnName = matcher.group(1);
        if (matcher.group(3).equalsIgnoreCase("asc")) {
          order = new Sort.Order(Sort.Direction.ASC, columnName);
        } else {
          order = new Sort.Order(Sort.Direction.DESC, columnName);
        }
      }
    }

    int pageNo = 0;
    if (page > 0) {
      pageNo = page - 1;
    }

    // Paging
    Pageable pageable = PageRequest.of(pageNo, size, Sort.by(order));

    Page<User> entityPage;

    if (StringUtils.hasLength(keyword)) {
      keyword = "%" + keyword.toLowerCase() + "%";
      entityPage = userRepository.searchByKeywordAndIsDeletedTrue(keyword, pageable);
    } else {
      entityPage = userRepository.findAllByIsDeletedTrue(pageable);
    }

    return getUserPageResponse(page, size, entityPage);
  }

  private PageResponse<?> convertToPageResponse(Page<User> users, Pageable pageable) {
    List<UserResponse> response = users.stream().map(user -> UserResponse.builder()
        .id(user.getId())
        .fullName(user.getFirstName() + " " + user.getLastName())
        .email(user.getEmail())
        .phone(user.getPhone())
        .build()).toList();
    return PageResponse.builder()
        .pageNo(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPage(users.getTotalPages())
        .totalElements(users.getTotalElements())
        .items(response)
        .build();
  }

  @Override
  public UserResponse findById(Long id) {
    log.info("Find user by id: {}", id);

    User user = getUserEntity(id);

    return userMapper.toUserResponse(user);
  }

  @Override
  public UserResponse findByUsername(String username) {
    User user = userRepository.findByUsernameAndIsDeletedFalse(username);

    if (user == null) {
      throw new ResourceNotFoundException("User not found with username: " + username);
    }

    return userMapper.toUserResponse(user);
  }

  @Override
  public UserResponse findByEmail(String email) {

    User user = userRepository.findByUsernameAndIsDeletedFalse(email);

    if (user == null) {
      throw new ResourceNotFoundException("User not found with email: " + email);
    }

    return userMapper.toUserResponse(user);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long save(UserCreationRequest req) {

    if (userRepository.findByUsername(req.getUsername()).isPresent()) {
      throw new DuplicateResourceException("Username already exists");
    }

    log.info("Saving user: {}", req);

    User user = userRepository.save(userMapper.toUserEntity(req));
    log.info("Saved user: {}", user);

    try {
      emailService.emailVerification(req.getEmail(), req.getUsername());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return user.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void update(UserUpdateRequest req) {
    log.info("Updating user: {}", req);

    User existUser = getUserEntity(req.getId());

    existUser.setFirstName(req.getFirstName());
    existUser.setLastName(req.getLastName());
    existUser.setEmail(req.getEmail());
    existUser.setPhone(req.getPhone());
    existUser.setGender(Gender.valueOf(req.getGender()));
    existUser.setType(UserType.valueOf(req.getType()));

    Role role = roleRepository.findByName(req.getType());
    if (role == null) {
      throw new BadRequestException("Invalid role: " + req.getType());
    }

    Set<UserHasRole> roles = new HashSet<>();
    roles.add(new UserHasRole(existUser, role));
    existUser.setRoles(roles);

    userRepository.save(existUser);
  }


  @Override
  public void changePassword(UserPasswordRequest req) {
    log.info("Changing password for user: {}", req);

    if (!req.getPassword().equals(req.getConfirmPassword())) {
      throw new PasswordNotMatchException("Password not match, please try again");
    }
    // Get user by id
    User user = getUserEntity(req.getId());

    if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
      throw new BadRequestException("Old password is not correct!");
    }

    if (req.getPassword().equals(req.getConfirmPassword())) {
      user.setPassword(passwordEncoder.encode(req.getPassword()));
    }

    userRepository.save(user);
    log.info("Changed password for user: {}", user);
  }

  @Override
  public void deleteById(Long id) {
    log.info("Deleting user: {}", id);
    getUserEntity(id);

    // Get user by id
    User user = getUserEntity(id);
    user.setDeleted(true);
    user.setStatus(UserStatus.INACTIVE);
    user.setDeletedAt(Date.from(Instant.now()));

    userRepository.save(user);
    log.info("Deleted user id: {}", id);
  }

  @Override
  @Transactional
  public void deleteByIds(List<Long> ids) {
    List<User> users = userRepository.findAllByIdInAndIsDeletedFalse(ids);

    Set<Long> validIds = users.stream()
        .map(User::getId)
        .collect(Collectors.toSet());

    List<Long> invalidIds = ids.stream()
        .filter(id -> !validIds.contains(id))
        .toList();

    if (!invalidIds.isEmpty()) {
      throw new InvalidUserIdsException("Some user IDs are invalid or already deleted", invalidIds);
    }

    Date now = Date.from(Instant.now());
    userRepository.softDeleteByIds(ids, now);
  }

  @Override
  public void deletePermanentlyById(Long id) {
    User user = getUserEntity(id);
    userRepository.delete(user);
  }

  @Override
  public void deletePermanentlyByIds(List<Long> ids) {
    List<User> users = userRepository.findAllById(ids);
    userRepository.deleteAll(users);
  }

  @Override
  public void confirmEmail(String secretCode) {
    log.info(secretCode);
    String username = (String) redisTemplate.opsForValue().get(secretCode);

    if (username == null) {
      throw new IllegalArgumentException("Mã xác thực không hợp lệ hoặc đã hết hạn.");
    }

    User user = userRepository.findByUsernameAndIsDeletedFalse(username);

    if (user == null) {
      throw new ResourceNotFoundException("User not found with username" + username);
    }
    user.setStatus(UserStatus.ACTIVE);
    userRepository.save(user);

    redisTemplate.delete(secretCode);
  }

  @Override
  public void restoreById(Long id) {
    User user = getUserEntity(id);
    user.setDeleted(false);
    user.setStatus(UserStatus.ACTIVE);
    user.setDeletedAt(null);
    userRepository.save(user);
    log.info("Restore user id: {}", id);
  }

  @Override
  public void restoreByIds(List<Long> ids) {
    List<User> users = userRepository.findAllByIdInAndIsDeletedTrue(ids);

    Set<Long> validIds = users.stream()
        .map(User::getId)
        .collect(Collectors.toSet());

    List<Long> invalidIds = ids.stream()
        .filter(id -> !validIds.contains(id))
        .toList();

    if (!invalidIds.isEmpty()) {
      throw new InvalidUserIdsException("Some user IDs are invalid or not deleted", invalidIds);
    }

    userRepository.restoreDeletedByIds(ids);
  }

  @Override
  @Transactional
  public void resetPassword(ForgetPasswordRequest req) {

    User user = userRepository.findByUsernameAndIsDeletedFalse(req.getUsername());

    if (user == null) {
      throw new ResourceNotFoundException("User not found with username" + req.getUsername());
    }

    String randomPass = generateStrongPassword(12);

    user.setPassword(passwordEncoder.encode(randomPass));

    userRepository.save(user);

    try {
      emailService.send(user.getEmail(), "Reset password", randomPass);
    } catch (Exception e) {
      log.error("Failed to send reset password email", e);
    }

  }

  @Override
  public void verifyEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email" + email));

    int otp = otpGenerator();

    ForgotPassword fp = ForgotPassword.builder()
        .otp(otp)
        .expirationTime(new Date(System.currentTimeMillis() + 70 * 1000))
        .user(user)
        .build();

    try {
      emailService.send(user.getEmail(), "OTP for Forgot Password request", String.valueOf(otp));
    } catch (Exception e) {
      log.error("Failed to send reset password email", e);
    }

    forgotPasswordRepository.save(fp);
  }

  @Override
  @Transactional
  public boolean verifyOtp(Integer otp, String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Please provide an valid email!"));

    ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
        .orElseThrow(() -> new BadRequestException("Invalid OTP for email: " + email));

    if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
      forgotPasswordRepository.deleteByOtpAndUser(otp, user);
      return false;
    }

    forgotPasswordRepository.deleteByOtpAndUser(otp, user);
    return true;
  }

  @Override
  public void resetPassword(String email, ChangePasswordRequest req) {
    if (!Objects.equals(req.getPassword(), req.getRepeatPassword())) {
      throw new PasswordNotMatchException("Please enter the password again!");
    }

    String encodedPassword = passwordEncoder.encode(req.getPassword());

    userRepository.updatePassword(email, encodedPassword);
  }

  @Override
  public void activeAccount(String email) {

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    try {
      emailService.emailVerification(user.getEmail(), user.getUsername());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Integer otpGenerator() {
    Random random = new Random();
    return random.nextInt(100_000, 999_999);
  }

  public String generateStrongPassword(int length) {
    String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String lower = "abcdefghijklmnopqrstuvwxyz";
    String digits = "0123456789";
    String special = "!@#$%^&*()-_=+<>?";
    String all = upper + lower + digits + special;
    SecureRandom random = new SecureRandom();

    StringBuilder password = new StringBuilder();

    password.append(upper.charAt(random.nextInt(upper.length())));
    password.append(lower.charAt(random.nextInt(lower.length())));
    password.append(digits.charAt(random.nextInt(digits.length())));
    password.append(special.charAt(random.nextInt(special.length())));

    for (int i = 4; i < length; i++) {
      password.append(all.charAt(random.nextInt(all.length())));
    }

    List<Character> chars = password.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    Collections.shuffle(chars, random);

    return chars.stream().map(String::valueOf).collect(Collectors.joining());
  }

  /**
   * Get user by id
   *
   * @param id
   * @return
   */
  private User getUserEntity(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
  }

  /**
   * Convert EserEntities to UserResponse
   *
   * @param page
   * @param size
   * @param userEntities
   * @return
   */
  private UserPageResponse getUserPageResponse(int page, int size,
      Page<User> userEntities) {
    log.info("Convert User Entity Page");

    List<UserResponse> userList = userEntities.getContent().stream()
        .map(user -> userMapper.toUserResponse(user))
        .collect(Collectors.toList());

    UserPageResponse response = new UserPageResponse();
    response.setPageNumber(page);
    response.setPageSize(size);
    response.setTotalElements(userEntities.getTotalElements());
    response.setTotalPages(userEntities.getTotalPages());
    response.setUsers(userList);

    return response;
  }

}
