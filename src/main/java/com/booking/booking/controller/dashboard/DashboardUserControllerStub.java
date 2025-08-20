package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.dto.UserPasswordRequest;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.PermissionMatrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary stub controller for methods that need service implementation
 */
@RestController
@RequestMapping("/api/dashboard/users-stub")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-USER-STUB-CONTROLLER")
@Tag(name = "Dashboard User Stub Controller", description = "Temporary stub methods")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class DashboardUserControllerStub {

  private final AuthorizationUtils authorizationUtils;

  @Operation(summary = "Change User Password - STUB", description = "TODO: Implement in service")
  @PutMapping("/{id}/password")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess changeUserPasswordFromDashboard(
      @PathVariable("id") Long id,
      @Valid @RequestBody UserPasswordRequest request) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.UPDATE, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Changing password for user ID: {} (STUB)", id);

    // TODO: Implement changePassword method in UserService
    return new ResponseSuccess(HttpStatus.OK,
        "Dashboard: User password change not implemented yet");
  }

  @Operation(summary = "Soft Delete User - STUB", description = "TODO: Implement in service")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess deleteUserFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.DELETE, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Soft deleting user ID: {} (STUB)", id);

    // TODO: Implement softDeleteUser method in UserService
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: User soft delete not implemented yet");
  }

  @Operation(summary = "Soft Delete Multiple Users - STUB", description = "TODO: Implement in service")
  @DeleteMapping()
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess deleteUsersFromDashboard(@RequestBody List<Long> ids) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.DELETE, PermissionMatrix.Entity.USER);
    log.info("Dashboard: Soft deleting {} users (STUB)", ids.size());

    // TODO: Implement softDeleteUsers method in UserService
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Users soft delete not implemented yet");
  }

  @Operation(summary = "Restore User - STUB", description = "TODO: Implement in service")
  @PutMapping("/{id}/restore")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess restoreUserFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.RESTORE,
        PermissionMatrix.Entity.USER);
    log.info("Dashboard: Restoring user ID: {} (STUB)", id);

    // TODO: Implement restoreUser method in UserService
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: User restore not implemented yet");
  }

  @Operation(summary = "Restore Multiple Users - STUB", description = "TODO: Implement in service")
  @PutMapping("/restore")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess restoreUsersFromDashboard(@RequestBody List<Long> ids) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.RESTORE,
        PermissionMatrix.Entity.USER);
    log.info("Dashboard: Restoring {} users (STUB)", ids.size());

    // TODO: Implement restoreUsers method in UserService
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Users restore not implemented yet");
  }

  @Operation(summary = "Permanently Delete User - STUB", description = "TODO: Implement in service")
  @DeleteMapping("/{id}/permanent")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess deleteUserPermanentlyFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.PERMANENT_DELETE,
        PermissionMatrix.Entity.USER);
    log.info("Dashboard: Permanently deleting user ID: {} (STUB)", id);

    // TODO: Implement deleteUserPermanently method in UserService
    return new ResponseSuccess(HttpStatus.OK,
        "Dashboard: User permanent delete not implemented yet");
  }

  @Operation(summary = "Permanently Delete Multiple Users - STUB", description = "TODO: Implement in service")
  @DeleteMapping("/permanent")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess deleteUsersPermanentlyFromDashboard(@RequestBody List<Long> ids) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.PERMANENT_DELETE,
        PermissionMatrix.Entity.USER);
    log.info("Dashboard: Permanently deleting {} users (STUB)", ids.size());

    // TODO: Implement deleteUsersPermanently method in UserService
    return new ResponseSuccess(HttpStatus.OK,
        "Dashboard: Users permanent delete not implemented yet");
  }
}
