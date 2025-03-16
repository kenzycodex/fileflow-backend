-- Create notification_preferences table
CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_notification_type (user_id, notification_type)
);

-- Create notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    related_item_id BIGINT,
    related_item_type VARCHAR(20),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    expiry_date TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create comments table
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment_text VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    parent_comment_id BIGINT,
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- Create indices for performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);
CREATE INDEX idx_comments_file_id ON comments(file_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_comment_id ON comments(parent_comment_id);