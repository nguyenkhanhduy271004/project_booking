package com.booking.booking.controller.dashboard;

import com.booking.booking.controller.response.ResponseSuccess;
import com.booking.booking.mapper.EvaluateMapper;
import com.booking.booking.service.EvaluateService;
import com.booking.booking.util.AuthorizationUtils;
import com.booking.booking.util.PermissionMatrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/evaluates")
@RequiredArgsConstructor
@Slf4j(topic = "DASHBOARD-EVALUATE-CONTROLLER")
@Tag(name = "Dashboard Evaluate Controller", description = "Quản lý Đánh giá trong Dashboard - SYSTEM_ADMIN, ADMIN, MANAGER")
@PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'ADMIN', 'MANAGER')")
public class DashboardEvaluateController {

  private final EvaluateService evaluateService;
  private final EvaluateMapper evaluateMapper;
  private final AuthorizationUtils authorizationUtils;

  @Operation(summary = "Get All Evaluates with Authorization", description = "Lấy danh sách đánh giá theo quyền: Admin/System Admin xem tất cả, Manager xem đánh giá của hotel mình")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ResponseSuccess getAllEvaluatesForDashboard(
      @RequestParam(defaultValue = "0", required = false) int page,
      @RequestParam(defaultValue = "10", required = false) int size,
      @RequestParam(defaultValue = "id", required = false) String sort,
      @RequestParam(defaultValue = "false", required = false) boolean deleted) {

    authorizationUtils.logPermissions(PermissionMatrix.Action.READ,
        PermissionMatrix.Entity.EVALUATE);
    log.info("Dashboard: Getting evaluates list - page: {}, size: {}, deleted: {}", page, size,
        deleted);

    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

    // TODO: EvaluateService needs pagination methods - using placeholder for now
    return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED,
        "Dashboard: Evaluate listing not implemented - need pagination methods in EvaluateService",
        null);
  }

  // TODO: Most EvaluateService methods need to be implemented
  /*
   * @Operation(summary = "Get Evaluate by ID with Authorization", description =
   * "Lấy thông tin đánh giá theo ID với phân quyền")
   *
   * @GetMapping("/{id}")
   *
   * @ResponseStatus(HttpStatus.OK)
   * public ResponseSuccess getEvaluateByIdForDashboard(@PathVariable("id") Long
   * id) {
   * // TODO: Implement getEvaluateById in EvaluateService
   * return new ResponseSuccess(HttpStatus.NOT_IMPLEMENTED,
   * "Method not implemented yet", null);
   * }
   */

  // TODO: All other methods need EvaluateService implementation
  /*
   * All methods below need proper EvaluateService implementation:
   * - getEvaluatesByHotel
   * - getEvaluatesByRoom
   * - replyToEvaluate
   * - approveEvaluate
   * - rejectEvaluate
   * - hideEvaluate
   * - showEvaluate
   * - getEvaluateStatistics
   * - softDeleteEvaluate
   * - softDeleteEvaluates
   * - restoreEvaluate
   */

}
