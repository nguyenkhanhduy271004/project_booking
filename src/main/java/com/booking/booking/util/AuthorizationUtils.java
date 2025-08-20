package com.booking.booking.util;

import com.booking.booking.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationUtils {

    private final UserContext userContext;

    /**
     * Lấy thông tin user hiện tại
     */
    public User getCurrentUser() {
        return userContext.getCurrentUser();
    }

    /**
     * Kiểm tra user có role cụ thể
     */
    public boolean hasRole(String role) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = currentUser.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    // ==================== ROLE CHECKS ====================

    public boolean isSystemAdmin() {
        return hasRole(PermissionMatrix.Role.SYSTEM_ADMIN);
    }

    public boolean isAdmin() {
        return hasRole(PermissionMatrix.Role.ADMIN);
    }

    public boolean isManager() {
        return hasRole(PermissionMatrix.Role.MANAGER);
    }

    public boolean isStaff() {
        return hasRole(PermissionMatrix.Role.STAFF);
    }

    public boolean isGuest() {
        return hasRole(PermissionMatrix.Role.GUEST);
    }

    // ==================== PERMISSION LEVELS ====================

    /**
     * Có quyền truy cập tất cả dữ liệu (SYSTEM_ADMIN, ADMIN)
     */
    public boolean canAccessAllData() {
        return isSystemAdmin() || isAdmin();
    }

    /**
     * Có quyền quản lý dữ liệu (SYSTEM_ADMIN, ADMIN, MANAGER)
     */
    public boolean canManageData() {
        return isSystemAdmin() || isAdmin() || isManager();
    }

    /**
     * Có quyền xem dữ liệu (tất cả role trừ guest)
     */
    public boolean canViewData() {
        return isSystemAdmin() || isAdmin() || isManager() || isStaff();
    }

    // ==================== SPECIFIC PERMISSIONS ====================

    /**
     * Có quyền quản lý User (chỉ SYSTEM_ADMIN)
     */
    public boolean canManageUsers() {
        return isSystemAdmin();
    }

    /**
     * Có quyền tạo Hotel/Room (SYSTEM_ADMIN, ADMIN)
     */
    public boolean canCreateHotelRoom() {
        return isSystemAdmin() || isAdmin();
    }

    /**
     * Có quyền cập nhật Hotel/Room (SYSTEM_ADMIN, ADMIN, MANAGER)
     */
    public boolean canUpdateHotelRoom() {
        return isSystemAdmin() || isAdmin() || isManager();
    }

    /**
     * Có quyền xóa mềm (chỉ SYSTEM_ADMIN)
     */
    public boolean canSoftDelete() {
        return isSystemAdmin();
    }

    /**
     * Có quyền xóa vĩnh viễn (chỉ SYSTEM_ADMIN)
     */
    public boolean canPermanentDelete() {
        return isSystemAdmin();
    }

    /**
     * Có quyền khôi phục (chỉ SYSTEM_ADMIN)
     */
    public boolean canRestore() {
        return isSystemAdmin();
    }

    /**
     * Có quyền quản lý Booking (SYSTEM_ADMIN, ADMIN, MANAGER, STAFF)
     */
    public boolean canManageBookings() {
        return isSystemAdmin() || isAdmin() || isManager() || isStaff();
    }

    /**
     * Có quyền tạo Voucher (SYSTEM_ADMIN, ADMIN, MANAGER)
     */
    public boolean canCreateVoucher() {
        return isSystemAdmin() || isAdmin() || isManager();
    }

    /**
     * Có quyền quản lý Evaluate (SYSTEM_ADMIN, ADMIN, MANAGER)
     */
    public boolean canManageEvaluates() {
        return isSystemAdmin() || isAdmin() || isManager();
    }

    // ==================== SCOPE CHECKS ====================

    /**
     * Kiểm tra có phải manager của hotel
     */
    public boolean isManagerOfHotel(Long hotelId) {
        if (!isManager())
            return false;

        User currentUser = getCurrentUser();
        if (currentUser == null)
            return false;

        // Logic này sẽ được implement trong service layer
        // với query kiểm tra hotel.managedByUser = currentUser
        return true;
    }

    /**
     * Kiểm tra có phải staff của hotel
     */
    public boolean isStaffOfHotel(Long hotelId) {
        if (!isStaff())
            return false;

        User currentUser = getCurrentUser();
        if (currentUser == null)
            return false;

        // Logic này sẽ được implement trong service layer
        return true;
    }

    /**
     * Lấy scope truy cập dữ liệu
     */
    public String getDataScope() {
        if (canAccessAllData()) {
            return PermissionMatrix.Scope.ALL;
        } else if (isManager() || isStaff()) {
            return PermissionMatrix.Scope.MANAGED;
        } else if (isGuest()) {
            return PermissionMatrix.Scope.PUBLIC;
        }
        return PermissionMatrix.Scope.OWNED;
    }

    /**
     * Log thông tin phân quyền cho debug
     */
    public void logPermissions(String action, String entity) {
        User user = getCurrentUser();
        if (user != null) {
            log.info("Permission Check - User: {}, Role: {}, Action: {}, Entity: {}, Scope: {}",
                    user.getUsername(), getCurrentRole(), action, entity, getDataScope());
        }
    }

    /**
     * Lấy role hiện tại
     */
    public String getCurrentRole() {
        if (isSystemAdmin())
            return PermissionMatrix.Role.SYSTEM_ADMIN;
        if (isAdmin())
            return PermissionMatrix.Role.ADMIN;
        if (isManager())
            return PermissionMatrix.Role.MANAGER;
        if (isStaff())
            return PermissionMatrix.Role.STAFF;
        if (isGuest())
            return PermissionMatrix.Role.GUEST;
        return "UNKNOWN";
    }
}