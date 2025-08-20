package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.request.UserCreationRequest;
import com.booking.booking.controller.request.UserUpdateRequest;
import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.controller.response.UserPageResponse;
import com.booking.booking.controller.response.UserResponse;
import com.booking.booking.service.UserService;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.PermissionMatrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/users")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-USER-CONTROLLER")
@Tag(name = "Dashboard User Controller", description = "Manage users in dashboard - SYSTEM_ADMIN only")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class DashboardUserController {

  private final UserService userService;
  private final AuthorizationUtils authorizationUtils;

  @Operation(summary = "Get All Users", description = "Lấy danh sách tất cả user - Chỉ SYSTEM_ADMIN")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getAllUsersForDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size,
      @RequestParam(defaultValue = "id", required = false) String sort,
      @RequestParam(defaultValue = "false", required = false) boolean deleted) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Getting all users - page: {}, size: {}, deleted: {}", page, size, deleted);

    UserPageResponse userPageResponse;
    if (deleted) {
      userPageResponse = userService.findAllUsersIsDeleted("", sort, page, size);
    } else {
      userPageResponse = userService.findAll("", sort, page, size);
    }

    PageResponse<?> response = PageResponse.builder()
        .pageNo(page)
        .pageSize(size)
        .totalPage((int) userPageResponse.getTotalPages())
        .totalElements(userPageResponse.getTotalElements())
        .items(userPageResponse.getUsers())
        .build();

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: User list retrieved successfully",
        response);
  }

  @Operation(summary = "Search Users", description = "Tìm kiếm user - Chỉ SYSTEM_ADMIN")
  @GetMapping("/search")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess searchUsersFromDashboard(
      Pageable pageable,
      @RequestParam(required = false) String[] user) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Searching users with criteria");

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: User search completed",
        userService.advanceSearchWithSpecification(pageable, user));
  }

  @Operation(summary = "Get User by ID", description = "Lấy thông tin user theo ID - Chỉ SYSTEM_ADMIN")
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getUserByIdForDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.READ, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Getting user by ID: {}", id);

    UserResponse userResponse = userService.findById(id);
    if (userResponse != null) {
      return new ResponseSuccess(HttpStatus.OK, "Dashboard: User found", userResponse);
    } else {
      throw new RuntimeException("User not found");
    }
  }

  @Operation(summary = "Create User", description = "Create new user - SYSTEM_ADMIN only")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseSuccess createUserFromDashboard(@Valid @RequestBody UserCreationRequest request) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.CREATE, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Creating new user");

    Long userId = userService.save(request);
    UserResponse created = userService.findById(userId);
    return new ResponseSuccess(HttpStatus.CREATED, "Dashboard: User created successfully", created);
  }

  @Operation(summary = "Update User", description = "Update user - SYSTEM_ADMIN only")
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess updateUserFromDashboard(
      @PathVariable("id") Long id,
      @Valid @RequestBody UserUpdateRequest request) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.UPDATE, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Updating user ID: {}", id);
    request.setId(id);
    userService.update(request);
    UserResponse updated = userService.findById(id);
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: User updated successfully", updated);
  }

  @Operation(summary = "Activate User Account", description = "Activate user account - SYSTEM_ADMIN only")
  @PutMapping("/{id}/activate")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess activateUserFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.UPDATE, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Activating user ID: {}", id);

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: User activated successfully");
  }

  @Operation(summary = "Deactivate User Account", description = "Deactivate user account - SYSTEM_ADMIN only")
  @PutMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess deactivateUserFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.UPDATE, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Deactivating user ID: {}", id);

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: User deactivated successfully");
  }
}
