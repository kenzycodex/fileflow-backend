-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    profile_image_path VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_login TIMESTAMP,
    last_username_change TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    storage_quota BIGINT NOT NULL,
    storage_used BIGINT NOT NULL DEFAULT 0,
    role VARCHAR(20) NOT NULL
);

-- Create user_settings table
CREATE TABLE user_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    theme_preference VARCHAR(20) NOT NULL,
    notification_email BOOLEAN NOT NULL DEFAULT TRUE,
    notification_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    default_view VARCHAR(20) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create folders table
CREATE TABLE folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_name VARCHAR(255) NOT NULL,
    parent_folder_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_accessed TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES folders(id) ON DELETE CASCADE
);

-- Create files table
CREATE TABLE files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    storage_path VARCHAR(1000),
    file_size BIGINT,
    file_type VARCHAR(100),
    mime_type VARCHAR(255),
    parent_folder_id BIGINT,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_accessed TIMESTAMP,
    checksum VARCHAR(64),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Create file_versions table
CREATE TABLE file_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    storage_path VARCHAR(1000),
    version_number INT NOT NULL,
    file_size BIGINT,
    created_at TIMESTAMP,
    created_by BIGINT,
    comment VARCHAR(255),
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create shares table
CREATE TABLE shares (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    recipient_id BIGINT,
    recipient_email VARCHAR(100),
    share_link VARCHAR(255) NOT NULL,
    permissions VARCHAR(20) NOT NULL,
    expiry_date TIMESTAMP,
    is_password_protected BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(100),
    created_at TIMESTAMP,
    view_count INT NOT NULL DEFAULT 0,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create activities table
CREATE TABLE activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    item_id BIGINT,
    item_type VARCHAR(20),
    description VARCHAR(255),
    ip_address VARCHAR(50),
    created_at TIMESTAMP,
    device_info VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create devices table
CREATE TABLE devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    last_sync_date TIMESTAMP,
    push_token VARCHAR(255),
    created_at TIMESTAMP,
    last_active TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create sync_queue table
CREATE TABLE sync_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id BIGINT,
    action_type VARCHAR(50) NOT NULL,
    item_id BIGINT,
    item_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP,
    processed_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    data_payload TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Create storage_chunks table
CREATE TABLE storage_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    upload_id VARCHAR(100) NOT NULL,
    chunk_number INT NOT NULL,
    chunk_size BIGINT NOT NULL,
    total_chunks INT NOT NULL,
    total_size BIGINT NOT NULL,
    storage_path VARCHAR(1000),
    parent_folder_id BIGINT,
    original_filename VARCHAR(255),
    mime_type VARCHAR(255),
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Create file_access_logs table
CREATE TABLE file_access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    access_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(255),
    access_time TIMESTAMP,
    device_info VARCHAR(255),
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create quota_extensions table
CREATE TABLE quota_extensions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    additional_space BIGINT NOT NULL,
    reason VARCHAR(255),
    granted_by BIGINT,
    expiry_date TIMESTAMP,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indices for performance
CREATE INDEX idx_folders_user_id ON folders(user_id);
CREATE INDEX idx_folders_parent_folder_id ON folders(parent_folder_id);
CREATE INDEX idx_files_user_id ON files(user_id);
CREATE INDEX idx_files_parent_folder_id ON files(parent_folder_id);
CREATE INDEX idx_shares_owner_id ON shares(owner_id);
CREATE INDEX idx_shares_recipient_id ON shares(recipient_id);
CREATE INDEX idx_activities_user_id ON activities(user_id);