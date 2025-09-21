package com.booking.booking.controller;

import com.booking.booking.dto.request.*;
import com.booking.booking.dto.response.PageResponse;
import com.booking.booking.dto.response.ResponseSuccess;
import com.booking.booking.dto.response.UserPageResponse;
import com.booking.booking.mapper.UserMapper;
import com.booking.booking.model.User;
import com.booking.booking.service.UserService;
import com.booking.booking.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Controller")
@Slf4j(topic = "USER-CONTROLLER")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final UserContext userContext;

    @Operation(summary = "Get user list", description = "API retrieve users (deleted or not)")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "id") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "false") boolean deleted) {

        log.info("Get user list (deleted: {})", deleted);

        UserPageResponse userPageResponse;
        if (deleted) {
            userPageResponse = userService.findAllUsersIsDeleted(keyword, sort, page, size);
        } else {
            userPageResponse = userService.findAll(keyword, sort, page, size);
        }

        PageResponse<?> response = PageResponse.builder()
                .pageNo(userPageResponse.getPageNumber())
                .pageSize(userPageResponse.getPageSize())
                .totalPage(userPageResponse.getTotalPages())
                .totalElements(userPageResponse.getTotalElements())
                .items(userPageResponse.getUsers())
                .build();

        String message =
                deleted ? "Fetched deleted user list successfully" : "Fetched user list successfully";
        return new ResponseSuccess(HttpStatus.OK, message, response);
    }

    @Operation(summary = "Search user by keyword", description = "API retrieve user from database")
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess searchUser(Pageable pageable,
                                      @RequestParam(required = false) String[] user) {
        log.info("Search user");

        return new ResponseSuccess(HttpStatus.OK, "Search users successfully",
                userService.advanceSearchWithSpecification(pageable, user));
    }

    @GetMapping("/manager")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseSuccess getListUserForManager(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        UserPageResponse userPageResponse = userService.findUserForManager(pageable);

        PageResponse<?> response = PageResponse.builder()
                .pageNo(userPageResponse.getPageNumber())
                .pageSize(userPageResponse.getPageSize())
                .totalPage(userPageResponse.getTotalPages())
                .totalElements(userPageResponse.getTotalElements())
                .items(userPageResponse.getUsers())
                .build();
        return new ResponseSuccess(HttpStatus.OK, "Get list user for manager", response);
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAuthority('STAFF')")
    public ResponseSuccess getListUserForStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        UserPageResponse userPageResponse = userService.findUserForStaff(pageable);

        PageResponse<?> response = PageResponse.builder()
                .pageNo(userPageResponse.getPageNumber())
                .pageSize(userPageResponse.getPageSize())
                .totalPage(userPageResponse.getTotalPages())
                .totalElements(userPageResponse.getTotalElements())
                .items(userPageResponse.getUsers())
                .build();
        return new ResponseSuccess(HttpStatus.OK, "Get list user for manager", response);
    }

    @Operation(summary = "Get user detail", description = "API retrieve user detail by ID from database")
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess getUserDetail(@PathVariable @Min(1) Long userId) {
        log.info("Get user detail by ID: {}", userId);

        return new ResponseSuccess(HttpStatus.OK, "Get user detail successfully",
                userService.findById(userId));
    }

    @Operation(summary = "Create User", description = "API add new user to database")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess createUser(@Valid @RequestBody UserCreationRequest request) {
        log.info("Create User: {}", request);

        return new ResponseSuccess(HttpStatus.CREATED, "User created successfully",
                userService.save(request));
    }

    @Operation(summary = "Update User", description = "API update user to database")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER', 'STAFF', 'GUEST')")
    public ResponseSuccess updateUser(@Valid @RequestBody UserUpdateRequest request) {
        log.info("Updating user: {}", request);

        userService.update(request);

        return new ResponseSuccess(HttpStatus.OK, "User updated successfully",
                userService.findById(request.getId()));
    }

    @GetMapping("/info")
    public ResponseSuccess getUserInfo() {
        User user = userContext.getCurrentUser();

        return new ResponseSuccess(HttpStatus.OK, "Get user info successfully",
                userMapper.toUserResponse(user));
    }

    @Operation(summary = "Change Password", description = "API change password for user to database")
    @PostMapping("/change-password")
    public ResponseSuccess changePassword(@Valid @RequestBody UserPasswordRequest request) {
        log.info("Changing password for user: {}", request);

        User user = userContext.getCurrentUser();
        request.setId(user.getId());
        userService.changePassword(request);

        return new ResponseSuccess(HttpStatus.OK, "Password changed successfully");
    }

    @GetMapping("/confirm-email")
    public void confirmEmail(@RequestParam String secretCode, HttpServletResponse response)
            throws IOException {
        log.info("Confirm email: {}", secretCode);

        try {

            userService.confirmEmail(secretCode);

        } catch (Exception e) {
            log.error("Confirm email was failed!, errorMessage={}" + e.getMessage());
        } finally {
            response.sendRedirect("http://localhost:3000");
        }
    }

    @PostMapping("/active-account/{email}")
    public ResponseSuccess activeAccount(@PathVariable String email) {
        log.info("Active account: {}", email);

        userService.activeAccount(email);
        return new ResponseSuccess(HttpStatus.OK, "Active account successfully");
    }

    @PostMapping("/verify-mail/{email}")
    public ResponseSuccess verifyEmail(@PathVariable String email) {
        log.info("Verify email: {}", email);

        userService.verifyEmail(email);

        return new ResponseSuccess(HttpStatus.OK, "Email verified successfully");
    }

    @PostMapping("/verify-otp")
    public ResponseSuccess verifyOtp(@RequestBody VerifyOtpRequest req) {

        log.info("Verify otp: {}", req.getOtp());

        boolean isTrue = userService.verifyOtp(req.getOtp(), req.getEmail());

        if (!isTrue) {
            return new ResponseSuccess(HttpStatus.EXPECTATION_FAILED, "OTP is expired!");
        }

        return new ResponseSuccess(HttpStatus.OK, "OTP verified successfully");
    }

    @PostMapping("/reset-password/{email}")
    public ResponseSuccess resetPassword(@PathVariable String email,
                                         @Valid @RequestBody ChangePasswordRequest req) {

        log.info("Change password: {}", req);

        userService.resetPassword(email, req);

        return new ResponseSuccess(HttpStatus.OK, "Password changed successfully");
    }

//  @PostMapping("/forgot-password")
//  public ResponseSuccess resetPassword(@Valid @RequestBody ForgetPasswordRequest req) {
//
//    userService.resetPassword(req);
//
//    return new ResponseSuccess(HttpStatus.OK, "Password reset successfully");
//  }

    @Operation(summary = "Delete user", description = "API activate user from database")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteUser(@PathVariable @Min(1) Long userId) {
        log.info("Deleting user: {}", userId);

        userService.deleteById(userId);

        return new ResponseSuccess(HttpStatus.OK, "User deleted successfully");
    }

    @Operation(summary = "Delete users", description = "API activate user from database")
    @DeleteMapping("/ids")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteUsers(@RequestBody List<Long> userIds) {
        log.info("Deleting users: {}", userIds);

        userService.deleteByIds(userIds);

        return new ResponseSuccess(HttpStatus.OK, "Users deleted successfully");
    }

    @DeleteMapping("/{userId}/permanent")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteUserPermanently(@PathVariable @Min(1) Long userId) {
        log.info("Deleting user permanently: {}", userId);
        userService.deletePermanentlyById(userId);
        return new ResponseSuccess(HttpStatus.OK, "User permanently deleted successfully");
    }

    @DeleteMapping("/permanent")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess deleteUsersPermanently(@RequestBody List<Long> userIds) {
        log.info("Deleting users permanently: {}", userIds);
        userService.deletePermanentlyByIds(userIds);
        return new ResponseSuccess(HttpStatus.OK, "Users permanently deleted successfully");
    }

    @Operation(summary = "Restore user", description = "API activate user from database")
    @PutMapping("/{userId}/restore")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess restoreUser(@PathVariable @Min(1) Long userId) {
        log.info("Deleting user: {}", userId);

        userService.restoreById(userId);

        return new ResponseSuccess(HttpStatus.OK, "User restore successfully");
    }

    @Operation(summary = "Restore users", description = "API activate user from database")
    @PutMapping("/restore")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseSuccess restoreUsers(@RequestBody List<Long> userIds) {
        log.info("Restore users: {}", userIds);

        userService.restoreByIds(userIds);

        return new ResponseSuccess(HttpStatus.OK, "Users restore successfully");
    }

    @GetMapping("/info-oauth2")
    public ResponseSuccess getUserInfoOauth2(OAuth2AuthenticationToken auth) {
        if (auth == null) {
            return new ResponseSuccess(HttpStatus.UNAUTHORIZED, "No authentication token provided");
        }

        String email = auth.getPrincipal().getAttribute("email");
        String picture = auth.getPrincipal().getAttribute("picture");
        if (picture == null) {
            picture = "";
        }

        Map<String, Object> map = Map.of("email", email, "picture", picture);

        return new ResponseSuccess(HttpStatus.OK, "Get user info successfully", map);
    }
}
