-- Fix booking status column length to accommodate new enum values

-- Increase status column length to handle longer enum values like 'PAYMENT_PENDING', 'CHECKED_OUT'
ALTER TABLE tbl_booking MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Optional: Add constraint to ensure only valid enum values
-- ALTER TABLE tbl_booking ADD CONSTRAINT chk_booking_status 
-- CHECK (status IN ('PENDING', 'PAYMENT_PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'EXPIRED', 'COMPLETED'));
