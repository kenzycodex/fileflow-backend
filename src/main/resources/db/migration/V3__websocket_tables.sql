-- V3__websocket_tables.sql
-- Add WebSocket related tables for real-time notifications and tracking

-- Table for tracking WebSocket sessions
CREATE TABLE websocket_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'User associated with this session',
    session_id VARCHAR(100) NOT NULL COMMENT 'WebSocket session identifier',
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the session was established',
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last activity timestamp',
    ip_address VARCHAR(50) COMMENT 'IP address of the client',
    user_agent VARCHAR(500) COMMENT 'User agent of the client',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether the session is currently active',
    disconnected_at TIMESTAMP NULL COMMENT 'When the session was terminated',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_session_id (session_id),
    INDEX idx_websocket_user_id (user_id),
    INDEX idx_websocket_is_active (is_active)
) COMMENT = 'Tracks active WebSocket sessions for users';

-- Table for WebSocket subscriptions (what users are listening to)
CREATE TABLE websocket_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'User who subscribed',
    item_id BIGINT NOT NULL COMMENT 'File or folder ID the user is subscribed to',
    item_type ENUM('FILE', 'FOLDER') NOT NULL COMMENT 'Type of item',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the subscription was created',
    updated_at TIMESTAMP NULL COMMENT 'Last time the subscription was updated',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether the subscription is active',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_subscription (user_id, item_id, item_type),
    INDEX idx_subscription_user (user_id),
    INDEX idx_subscription_item (item_id, item_type)
) COMMENT = 'Tracks what files and folders users are subscribed to for real-time updates';

-- Table for queued WebSocket notifications
CREATE TABLE websocket_notifications_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'User to notify',
    notification_type VARCHAR(50) NOT NULL COMMENT 'Type of notification (FILE_UPLOADED, FOLDER_UPDATED, etc.)',
    payload TEXT NOT NULL COMMENT 'JSON payload to send to the client',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the notification was created',
    sent_at TIMESTAMP NULL COMMENT 'When the notification was sent',
    is_sent BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether the notification has been sent',
    retry_count INT NOT NULL DEFAULT 0 COMMENT 'Number of retry attempts',
    last_retry TIMESTAMP NULL COMMENT 'Last retry timestamp',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notification_user (user_id),
    INDEX idx_notification_status (is_sent),
    INDEX idx_notification_created (created_at)
) COMMENT = 'Queue for WebSocket notifications to ensure delivery';

-- Table for WebSocket activity metrics
CREATE TABLE websocket_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_date DATE NOT NULL COMMENT 'Date of the metrics',
    hour_of_day TINYINT NOT NULL COMMENT 'Hour of the day (0-23)',
    active_connections INT NOT NULL DEFAULT 0 COMMENT 'Number of active connections during this period',
    messages_sent INT NOT NULL DEFAULT 0 COMMENT 'Number of messages sent during this period',
    messages_received INT NOT NULL DEFAULT 0 COMMENT 'Number of messages received during this period',
    errors_count INT NOT NULL DEFAULT 0 COMMENT 'Number of errors during this period',
    average_message_size INT COMMENT 'Average message size in bytes',

    UNIQUE KEY unique_metric_time (event_date, hour_of_day),
    INDEX idx_metrics_date (event_date)
) COMMENT = 'WebSocket usage metrics for monitoring and capacity planning';