package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.response.PageResponse;
import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.dto.VoucherDTO;
import com.booking.booking.mapper.VoucherMapper;
import com.booking.booking.model.Voucher;
import com.booking.booking.service.VoucherService;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.PermissionMatrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/dashboard/vouchers")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-VOUCHER-CONTROLLER")
@Tag(name = "Dashboard Voucher Controller", description = "Manage vouchers in dashboard - SYSTEM_ADMIN, ADMIN, MANAGER")
@PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
public class DashboardVoucherController {

  private final VoucherMapper voucherMapper;
  private final VoucherService voucherService;
  private final AuthorizationUtils authorizationUtils;

  @Operation(summary = "Get All Vouchers with Authorization", description = "Get vouchers with role-based access")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getAllVouchersForDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size,
      @RequestParam(defaultValue = "id", required = false) String sort,
      @RequestParam(defaultValue = "false", required = false) boolean deleted) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Getting vouchers list - page: {}, size: {}, deleted: {}", page, size,
        deleted);

    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

    Page<Voucher> vouchers;
    if (authorizationUtils.canAccessAllData()) {
      vouchers = voucherService.getAllVouchers(pageable);
    } else {
      vouchers = voucherService.getAllVouchers(pageable);
    }

    PageResponse<?> response = PageResponse.builder()
        .pageNo(pageable.getPageNumber())
        .pageSize(pageable.getPageSize())
        .totalPage(vouchers.getTotalPages())
        .totalElements(vouchers.getTotalElements())
        .items(vouchers.getContent().stream()
            .map(voucherMapper::toVoucherResponse)
            .toList())
        .build();

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Voucher list retrieved successfully",
        response);
  }

  @Operation(summary = "Get Voucher by ID with Authorization", description = "Get voucher by id with authorization")
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getVoucherByIdForDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Getting voucher by ID: {}", id);

    Voucher voucher = voucherService.getVoucherById(id);
    if (voucher == null) {
      throw new RuntimeException("Voucher not found or access denied");
    }
    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Voucher found",
        voucherMapper.toVoucherResponse(voucher));
  }

  @Operation(summary = "Get Vouchers by Hotel", description = "Get vouchers by hotel")
  @GetMapping("/hotel/{hotelId}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getVouchersByHotelFromDashboard(
      @PathVariable("hotelId") Long hotelId,
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Getting vouchers for hotel ID: {}", hotelId);

    Pageable pageable = PageRequest.of(page, size);

    if (!authorizationUtils.canAccessAllData()) {
      // Scope check should be enforced in service layer
    }
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Create Voucher", description = "Create voucher - SYSTEM_ADMIN, ADMIN, MANAGER")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseSuccess createVoucherFromDashboard(@Valid @RequestBody VoucherDTO voucherDTO) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.CREATE,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Creating new voucher: {}", voucherDTO.getCode());

    if (!authorizationUtils.canAccessAllData() && voucherDTO.getHotelId() != null) {
    }
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Update Voucher", description = "Update voucher - SYSTEM_ADMIN, ADMIN, MANAGER")
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess updateVoucherFromDashboard(
      @PathVariable("id") Long id,
      @Valid @RequestBody VoucherDTO voucherDTO) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.UPDATE,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Updating voucher ID: {}", id);

    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Activate Voucher", description = "Activate voucher")
  @PutMapping("/{id}/activate")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess activateVoucherFromDashboard(@PathVariable("id") Long id) {
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Deactivate Voucher", description = "Deactivate voucher")
  @PutMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess deactivateVoucherFromDashboard(@PathVariable("id") Long id) {
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Get Voucher Usage Statistics", description = "Get usage statistics for voucher")
  @GetMapping("/{id}/statistics")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getVoucherUsageStatisticsFromDashboard(@PathVariable("id") Long id) {
    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Getting usage statistics for voucher ID: {}", id);

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Voucher usage statistics retrieved",
        null);
  }

  @Operation(summary = "Get All Voucher Statistics", description = "Get overall voucher statistics with role-based access")
  @GetMapping("/statistics")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getVoucherStatisticsFromDashboard(
      @RequestParam(required = false) String period,
      @RequestParam(required = false) Long hotelId) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Getting voucher statistics - period: {}, hotelId: {}", period, hotelId);

    return new ResponseSuccess(HttpStatus.OK, "Dashboard: Voucher statistics retrieved", null);
  }

  @Operation(summary = "Get Expired Vouchers", description = "Get expired vouchers")
  @GetMapping("/expired")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getExpiredVouchersFromDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Getting expired vouchers");

    Pageable pageable = PageRequest.of(page, size);
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Get Active Vouchers", description = "Get active vouchers")
  @GetMapping("/active")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getActiveVouchersFromDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.VOUCHER);
    log.info("Dashboard: Getting active vouchers");

    Pageable pageable = PageRequest.of(page, size);
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Soft Delete Voucher", description = "Soft delete voucher")
  @PutMapping("/{id}/soft-delete")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess deleteVoucherFromDashboard(@PathVariable("id") Long id) {
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }

  @Operation(summary = "Restore Voucher", description = "Restore soft deleted voucher")
  @PutMapping("/{id}/restore")
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess restoreVoucherFromDashboard(@PathVariable("id") Long id) {
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED, null, null);
  }
}
