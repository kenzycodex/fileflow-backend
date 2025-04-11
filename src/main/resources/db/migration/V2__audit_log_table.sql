-- V2__audit_log_table.sql
-- Add audit_logs table for comprehensive security event tracking

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL COMMENT 'Type of event (LOGIN, LOGOUT, PASSWORD_RESET, etc.)',
    event_status ENUM('SUCCESS', 'FAILURE', 'WARNING', 'INFO') NOT NULL COMMENT 'Status of the event',
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Time when the event occurred',
    ip_address VARCHAR(50) COMMENT 'IP address from which the event originated',
    user_agent VARCHAR(500) COMMENT 'User agent used for the event',
    user_id BIGINT COMMENT 'User ID associated with the event (can be null for anonymous events)',
    username VARCHAR(100) COMMENT 'Username associated with the event',
    email VARCHAR(100) COMMENT 'Email associated with the event',
    details VARCHAR(1000) COMMENT 'Additional details about the event',
    session_id VARCHAR(100) COMMENT 'Session ID for tracking events within a session',

    -- Add indexes for efficient querying
    INDEX idx_audit_event_time (event_time),
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_ip_address (ip_address),
    INDEX idx_audit_email (email)
);

-- Add comment to the table
ALTER TABLE audit_logs COMMENT = 'Comprehensive audit log for security and user activity tracking';

-- Optional: Add a foreign key if strict referential integrity is needed
-- ALTER TABLE audit_logs
--    ADD CONSTRAINT fk_audit_logs_user
--    FOREIGN KEY (user_id) REFERENCES users(id)
--    ON DELETE SET NULL;

-- Consider this optional since we often want to keep audit records even if a user is deleted
-- The absence of a foreign key constraint allows keeping audit records for deleted users