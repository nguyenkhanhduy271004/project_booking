-- Insert sample data for dashboard testing
-- This script adds sample hotels, rooms, users, and bookings for dashboard visualization

-- Insert sample hotels
INSERT INTO tbl_hotel (name, address_detail, total_rooms, managed_by, is_deleted, created_at, updated_at) VALUES
('Grand Hotel Saigon', '123 Nguyen Hue, District 1, HCMC', 100, 1, false, NOW(), NOW()),
('Melia Hanoi Hotel', '44B Ly Thuong Kiet, Hoan Kiem, Hanoi', 150, 1, false, NOW(), NOW()),
('Furama Resort Da Nang', '68 Ho Xuan Huong, Da Nang', 200, 1, false, NOW(), NOW()),
('Sheraton Nha Trang', '26-28 Tran Phu, Nha Trang', 120, 1, false, NOW(), NOW()),
('InterContinental Phu Quoc', 'Bai Trong, Phu Quoc Island', 180, 1, false, NOW(), NOW());

-- Insert sample rooms for each hotel
INSERT INTO tbl_room (hotel_id, room_number, type_room, price_per_night, capacity, available, is_deleted, created_at, updated_at) VALUES
-- Grand Hotel Saigon rooms
(1, '101', 'STANDARD', 1500000, 2, true, false, NOW(), NOW()),
(1, '102', 'STANDARD', 1500000, 2, true, false, NOW(), NOW()),
(1, '201', 'DELUXE', 2500000, 2, true, false, NOW(), NOW()),
(1, '301', 'SUITE', 3500000, 4, true, false, NOW(), NOW()),
(1, '401', 'CONFERENCE', 5000000, 20, true, false, NOW(), NOW()),

-- Melia Hanoi Hotel rooms
(2, '101', 'STANDARD', 1800000, 2, true, false, NOW(), NOW()),
(2, '102', 'STANDARD', 1800000, 2, true, false, NOW(), NOW()),
(2, '201', 'DELUXE', 2800000, 2, true, false, NOW(), NOW()),
(2, '301', 'SUITE', 3800000, 4, true, false, NOW(), NOW()),
(2, '401', 'CONFERENCE', 6000000, 25, true, false, NOW(), NOW()),

-- Furama Resort Da Nang rooms
(3, '101', 'STANDARD', 1200000, 2, true, false, NOW(), NOW()),
(3, '102', 'STANDARD', 1200000, 2, true, false, NOW(), NOW()),
(3, '201', 'DELUXE', 2200000, 2, true, false, NOW(), NOW()),
(3, '301', 'SUITE', 3200000, 4, true, false, NOW(), NOW()),
(3, '401', 'CONFERENCE', 4500000, 30, true, false, NOW(), NOW()),

-- Sheraton Nha Trang rooms
(4, '101', 'STANDARD', 1600000, 2, true, false, NOW(), NOW()),
(4, '102', 'STANDARD', 1600000, 2, true, false, NOW(), NOW()),
(4, '201', 'DELUXE', 2600000, 2, true, false, NOW(), NOW()),
(4, '301', 'SUITE', 3600000, 4, true, false, NOW(), NOW()),
(4, '401', 'CONFERENCE', 5500000, 22, true, false, NOW(), NOW()),

-- InterContinental Phu Quoc rooms
(5, '101', 'STANDARD', 2000000, 2, true, false, NOW(), NOW()),
(5, '102', 'STANDARD', 2000000, 2, true, false, NOW(), NOW()),
(5, '201', 'DELUXE', 3000000, 2, true, false, NOW(), NOW()),
(5, '301', 'SUITE', 4000000, 4, true, false, NOW(), NOW()),
(5, '401', 'CONFERENCE', 7000000, 35, true, false, NOW(), NOW());

-- Insert sample users (guests)
INSERT INTO tbl_user (username, password, email, phone, first_name, last_name, gender, birthday, type, status, address, is_deleted, created_at, updated_at) VALUES
('guest1', '$2a$10$example1', 'guest1@example.com', '0123456789', 'Nguyen', 'Van A', 'MALE', '1990-01-15', 'GUEST', 'ACTIVE', 'Ho Chi Minh City', false, NOW() - INTERVAL 30 DAY, NOW()),
('guest2', '$2a$10$example2', 'guest2@example.com', '0123456790', 'Tran', 'Thi B', 'FEMALE', '1985-05-20', 'GUEST', 'ACTIVE', 'Ha Noi', false, NOW() - INTERVAL 25 DAY, NOW()),
('guest3', '$2a$10$example3', 'guest3@example.com', '0123456791', 'Le', 'Van C', 'MALE', '1992-08-10', 'GUEST', 'ACTIVE', 'Da Nang', false, NOW() - INTERVAL 20 DAY, NOW()),
('guest4', '$2a$10$example4', 'guest4@example.com', '0123456792', 'Pham', 'Thi D', 'FEMALE', '1988-12-03', 'GUEST', 'ACTIVE', 'Nha Trang', false, NOW() - INTERVAL 15 DAY, NOW()),
('guest5', '$2a$10$example5', 'guest5@example.com', '0123456793', 'Hoang', 'Van E', 'MALE', '1995-03-25', 'GUEST', 'ACTIVE', 'Phu Quoc', false, NOW() - INTERVAL 10 DAY, NOW()),
('guest6', '$2a$10$example6', 'guest6@example.com', '0123456794', 'Vu', 'Thi F', 'FEMALE', '1991-07-18', 'GUEST', 'ACTIVE', 'Can Tho', false, NOW() - INTERVAL 5 DAY, NOW());

-- Insert sample bookings with different statuses and dates
INSERT INTO tbl_booking (guest_id, booking_code, check_in_date, check_out_date, total_price, status, payment_type, is_deleted, created_at, updated_at) VALUES
-- Completed bookings from last month
(2, 'BK001', DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_SUB(CURDATE(), INTERVAL 23 DAY), 3000000, 'COMPLETED', 'CREDIT_CARD', false, DATE_SUB(NOW(), INTERVAL 26 DAY), NOW()),
(3, 'BK002', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 18 DAY), 4400000, 'COMPLETED', 'BANK_TRANSFER', false, DATE_SUB(NOW(), INTERVAL 21 DAY), NOW()),
(4, 'BK003', DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 13 DAY), 3600000, 'COMPLETED', 'CREDIT_CARD', false, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(5, 'BK004', DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 8 DAY), 4000000, 'COMPLETED', 'CASH', false, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),

-- Confirmed bookings (active)
(2, 'BK005', CURDATE() + INTERVAL 1 DAY, CURDATE() + INTERVAL 3 DAY, 3200000, 'CONFIRMED', 'CREDIT_CARD', false, NOW() - INTERVAL 5 DAY, NOW()),
(3, 'BK006', CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 4 DAY, 4800000, 'CONFIRMED', 'BANK_TRANSFER', false, NOW() - INTERVAL 3 DAY, NOW()),
(4, 'BK007', CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 5 DAY, 4000000, 'CONFIRMED', 'CREDIT_CARD', false, NOW() - INTERVAL 2 DAY, NOW()),

-- Pending bookings
(5, 'BK008', CURDATE() + INTERVAL 7 DAY, CURDATE() + INTERVAL 9 DAY, 6000000, 'PENDING', 'CREDIT_CARD', false, NOW() - INTERVAL 1 DAY, NOW()),

-- Cancelled bookings
(6, 'BK009', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2400000, 'CANCELLED', 'CREDIT_CARD', false, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW());

-- Insert booking_rooms relationships
INSERT INTO tbl_booking_room (booking_id, room_id) VALUES
(1, 1), (1, 2),
(2, 6), (2, 7),
(3, 11), (3, 12),
(4, 16), (4, 17),
(5, 3), (5, 8),
(6, 13), (6, 18),
(7, 4), (7, 9),
(8, 14), (8, 19),
(9, 5), (9, 10);

-- Note: Evaluation table doesn't exist yet, so we skip evaluation data
-- This will be added when evaluation feature is implemented
