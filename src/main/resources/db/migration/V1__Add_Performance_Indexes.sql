-- Performance indexes for booking system

-- Booking table indexes
CREATE INDEX IF NOT EXISTS idx_booking_guest_status ON tbl_booking(guest_id, status);
CREATE INDEX IF NOT EXISTS idx_booking_dates ON tbl_booking(check_in_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_booking_hotel_dates ON tbl_booking(hotel_id, check_in_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_booking_code ON tbl_booking(booking_code);
CREATE INDEX IF NOT EXISTS idx_booking_created_at ON tbl_booking(created_at);
CREATE INDEX IF NOT EXISTS idx_booking_deleted ON tbl_booking(is_deleted);

-- Room table indexes
CREATE INDEX IF NOT EXISTS idx_room_hotel_available ON tbl_room(hotel_id, available);
CREATE INDEX IF NOT EXISTS idx_room_deleted ON tbl_room(is_deleted);

-- Hotel table indexes  
CREATE INDEX IF NOT EXISTS idx_hotel_deleted ON tbl_hotel(is_deleted);

-- User table indexes
CREATE INDEX IF NOT EXISTS idx_user_username ON tbl_user(username);
CREATE INDEX IF NOT EXISTS idx_user_email ON tbl_user(email);
CREATE INDEX IF NOT EXISTS idx_user_status ON tbl_user(status);
CREATE INDEX IF NOT EXISTS idx_user_deleted ON tbl_user(is_deleted);

-- Booking room junction table indexes
CREATE INDEX IF NOT EXISTS idx_booking_room_booking_id ON tbl_booking_room(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_room_room_id ON tbl_booking_room(room_id);
