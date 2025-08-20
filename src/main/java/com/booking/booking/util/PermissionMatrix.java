package com.booking.booking.util;

/**
 * Ma trận phân quyền cho hệ thống Booking
 * 
 * LOGIC PHÂN QUYỀN:
 * 
 * 🔴 SYSTEM_ADMIN:
 * - Toàn quyền trên tất cả entity
 * - Quản lý user, role, permission
 * - Xóa vĩnh viễn, khôi phục dữ liệu
 * - Xem thống kê toàn hệ thống
 * 
 * 🟠 ADMIN:
 * - Quản lý hotel, room, booking, voucher, evaluate
 * - Tạo/sửa/xóa mềm hotel và room
 * - Xem tất cả booking và evaluate
 * - Quản lý voucher toàn hệ thống
 * - Không thể quản lý user system
 * 
 * 🟡 MANAGER:
 * - Chỉ quản lý hotel mình được giao (managedByUser)
 * - Quản lý room thuộc hotel mình
 * - Xem booking và evaluate của hotel mình
 * - Tạo voucher cho hotel mình
 * - Quản lý staff trong hotel mình
 * - Không thể xóa vĩnh viễn
 * 
 * 🟢 STAFF:
 * - Chỉ xem dữ liệu hotel mình làm việc
 * - Xử lý booking (check-in, check-out)
 * - Xem evaluate của hotel
 * - Không thể tạo/sửa/xóa
 * - Chỉ có quyền đọc và xử lý booking
 * 
 * 🔵 GUEST:
 * - Xem danh sách hotel, room public
 * - Tạo booking
 * - Tạo evaluate sau khi booking
 * - Xem voucher public
 * - Quản lý thông tin cá nhân
 */
public class PermissionMatrix {

    // Entity permissions
    public static class Entity {
        public static final String USER = "USER";
        public static final String HOTEL = "HOTEL";
        public static final String ROOM = "ROOM";
        public static final String BOOKING = "BOOKING";
        public static final String EVALUATE = "EVALUATE";
        public static final String VOUCHER = "VOUCHER";
        public static final String ROLE = "ROLE";
        public static final String PERMISSION = "PERMISSION";
    }

    // Action permissions
    public static class Action {
        public static final String CREATE = "CREATE";
        public static final String READ = "READ";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String RESTORE = "RESTORE";
        public static final String PERMANENT_DELETE = "PERMANENT_DELETE";
        public static final String MANAGE = "MANAGE";
    }

    // Role definitions
    public static class Role {
        public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
        public static final String ADMIN = "ADMIN";
        public static final String MANAGER = "MANAGER";
        public static final String STAFF = "STAFF";
        public static final String GUEST = "GUEST";
    }

    // Scope definitions
    public static class Scope {
        public static final String ALL = "ALL"; // Tất cả dữ liệu
        public static final String MANAGED = "MANAGED"; // Dữ liệu mình quản lý
        public static final String OWNED = "OWNED"; // Dữ liệu của mình
        public static final String PUBLIC = "PUBLIC"; // Dữ liệu công khai
    }
}
