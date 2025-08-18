-- Migration script to remove legacy room_id column from tbl_booking
-- Run this AFTER confirming the new many-to-many relationship works correctly

-- Step 1: Create the junction table if it doesn't exist
CREATE TABLE IF NOT EXISTS tbl_booking_room (
    booking_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    PRIMARY KEY (booking_id, room_id),
    FOREIGN KEY (booking_id) REFERENCES tbl_booking(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES tbl_room(id) ON DELETE CASCADE
);

-- Step 2: Migrate existing data from room_id column to junction table
-- (This assumes existing bookings have room_id populated)
INSERT IGNORE INTO tbl_booking_room (booking_id, room_id)
SELECT id, room_id 
FROM tbl_booking 
WHERE room_id IS NOT NULL;

-- Step 3: Remove the foreign key constraint on room_id (if exists)
-- ALTER TABLE tbl_booking DROP FOREIGN KEY constraint_name_here;

-- Step 4: Drop the room_id column (CAREFUL - this is irreversible!)
-- ALTER TABLE tbl_booking DROP COLUMN room_id;

-- Note: Uncomment Step 3 and 4 only after thorough testing
-- and replace 'constraint_name_here' with the actual constraint name
