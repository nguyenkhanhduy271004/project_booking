package com.booking.booking.service.impl;

import com.booking.booking.common.Gender;
import com.booking.booking.common.UserStatus;
import com.booking.booking.common.UserType;
import com.booking.booking.dto.request.*;
import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.UserPageResponse;
import com.booking.booking.dto.response.UserResponse;
import com.booking.booking.exception.*;
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
import com.booking.booking.service.interfaces.UserService;
import com.booking.booking.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.booking.booking.common.AppConst.SEARCH_SPEC_OPERATOR;

@Service
@Slf4j(topic = "USER-SERVICE")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserMapper userMapper;
    private final UserContext userContext;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final ForgotPasswordRepository forgotPasswordRepository;

    @Override
    public UserPageResponse findAll(String keyword, String sort, int page, int size) {
        Sort.Order order = buildSortOrder(sort);
        Pageable pageable = PageRequest.of(pageIndex(page), size, Sort.by(order));
        Page<User> entityPage = StringUtils.hasLength(keyword)
                ? userRepository.searchByKeyword("%" + keyword.toLowerCase() + "%", pageable)
                : userRepository.findAllByIsDeletedFalse(pageable);
        return mapToUserPageResponse(page, size, entityPage);
    }

    @Override
    public UserPageResponse findAllUsersIsDeleted(String keyword, String sort, int page, int size) {
        Sort.Order order = buildSortOrder(sort);
        Pageable pageable = PageRequest.of(pageIndex(page), size, Sort.by(order));
        Page<User> entityPage = StringUtils.hasLength(keyword)
                ? userRepository.searchByKeywordAndIsDeletedTrue("%" + keyword.toLowerCase() + "%",
                pageable)
                : userRepository.findAllByIsDeletedTrue(pageable);
        return mapToUserPageResponse(page, size, entityPage);
    }

    @Override
    public PageResponse<?> advanceSearchWithSpecification(Pageable pageable, String[] user) {
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
        var spec = builder.build();
        if (spec == null) {
            return convertToPageResponse(new PageImpl<>(List.of(), pageable, 0), pageable);
        }
        Page<User> users = userRepository.findAll(spec, pageable);
        return convertToPageResponse(users, pageable);
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserPageResponse findUserForManager(Pageable pageable) {
        User user = userContext.getCurrentUser();

        if (user.getHotel() == null) {
            throw new BadRequestException("User does not own any hotel");
        }

        Page<User> entityPage = userRepository.findUserForManager(user.getHotel().getId(), pageable);

        return mapToUserPageResponse(pageable.getPageNumber(), pageable.getPageSize(), entityPage);
    }

    @Override
    public UserPageResponse findUserForStaff(Pageable pageable) {
        User user = userContext.getCurrentUser();

        if (user.getHotel() == null) {
            throw new BadRequestException("User does not own any hotel");
        }

        Page<User> entityPage = userRepository.findUserForStaff(user.getHotel().getId(), pageable);

        return mapToUserPageResponse(pageable.getPageNumber(), pageable.getPageSize(), entityPage);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(UserCreationRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already used by another user");
        }


        User user = userRepository.save(userMapper.toUserEntity(req));
        try {
            emailService.sendVerificationEmail(req.getEmail(), req.getUsername());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateRequest req) {
        User existUser = getUserEntity(req.getId());
        existUser.setFirstName(req.getFirstName());
        existUser.setLastName(req.getLastName());
        existUser.setEmail(req.getEmail());
        existUser.setPhone(req.getPhone());
        existUser.setStatus(UserStatus.valueOf(req.getStatus()));
        try {
            existUser.setGender(Gender.valueOf(req.getGender()));
            existUser.setType(UserType.valueOf(req.getType()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid enum value for Gender or UserType");
        }
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
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new PasswordNotMatchException("Password not match, please try again");
        }
        User user = getUserEntity(req.getId());
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is not correct!");
        }
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void deleteById(Long id) {
        User user = getUserEntity(id);
        user.setDeleted(true);
        user.setStatus(UserStatus.INACTIVE);
        user.setDeletedAt(Date.from(Instant.now()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        List<User> users = userRepository.findAllByIdInAndIsDeletedFalse(ids);
        Set<Long> validIds = users.stream().map(User::getId).collect(Collectors.toSet());
        List<Long> invalidIds = ids.stream().filter(id -> !validIds.contains(id)).toList();
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
        String username = redisTemplate.opsForValue().get(secretCode);
        if (username == null) {
            throw new IllegalArgumentException("Mã xác thực không hợp lệ hoặc đã hết hạn.");
        }
        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new ResourceNotFoundException("Username not found with username: " + username));

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
    }

    @Override
    public void restoreByIds(List<Long> ids) {
        List<User> users = userRepository.findAllByIdInAndIsDeletedTrue(ids);
        Set<Long> validIds = users.stream().map(User::getId).collect(Collectors.toSet());
        List<Long> invalidIds = ids.stream().filter(id -> !validIds.contains(id)).toList();
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
            emailService.sendSimpleEmail(user.getEmail(), "Reset password", randomPass);
        } catch (Exception e) {
            log.error("Failed to send reset password email", e);
        }
    }

    @Override
    public void verifyEmail(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username" + username));
        int otp = otpGenerator();
        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .user(user)
                .build();
        try {
            emailService.sendSimpleEmail(user.getEmail(), "OTP for Forgot Password request", String.valueOf(otp));
        } catch (Exception e) {
            log.error("Failed to send reset password email", e);
        }
        forgotPasswordRepository.save(fp);
    }

    @Override
    @Transactional
    public boolean verifyOtp(Integer otp, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Please provide an valid username!"));
        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new BadRequestException("Invalid OTP for username: " + username));
        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByOtpAndUser(otp, user);
            return false;
        }
        forgotPasswordRepository.deleteByOtpAndUser(otp, user);
        return true;
    }

    @Override
    public void resetPassword(String username, ChangePasswordRequest req) {
        if (!Objects.equals(req.getPassword(), req.getRepeatPassword())) {
            throw new PasswordNotMatchException("Please enter the password again!");
        }
        String encodedPassword = passwordEncoder.encode(req.getPassword());
        userRepository.updatePassword(username, encodedPassword);
    }

    @Override
    public void activeAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer otpGenerator() {
        SecureRandom random = new SecureRandom();
        return random.nextInt(900000) + 100000;
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

    private User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserPageResponse mapToUserPageResponse(int page, int size, Page<User> userEntities) {
        List<UserResponse> userList = userEntities.getContent().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
        UserPageResponse response = new UserPageResponse();
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(userEntities.getTotalElements());
        response.setTotalPages(userEntities.getTotalPages());
        response.setUsers(userList);
        return response;
    }


    private Sort.Order buildSortOrder(String sort) {
        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "id");
        if (StringUtils.hasLength(sort)) {
            Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)");
            Matcher matcher = pattern.matcher(sort);
            if (matcher.find()) {
                String columnName = matcher.group(1);
                order = "asc".equalsIgnoreCase(matcher.group(3))
                        ? new Sort.Order(Sort.Direction.ASC, columnName)
                        : new Sort.Order(Sort.Direction.DESC, columnName);
            }
        }
        return order;
    }

    private int pageIndex(int page) {
        return Math.max(0, page - 1);
    }
}
