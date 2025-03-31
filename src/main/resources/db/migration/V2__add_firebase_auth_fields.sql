-- Add Firebase auth columns to users table
ALTER TABLE users
    ADD COLUMN firebase_uid VARCHAR(255) NULL,
    ADD COLUMN auth_provider ENUM('LOCAL', 'GOOGLE', 'GITHUB', 'MICROSOFT', 'APPLE') NOT NULL DEFAULT 'LOCAL',
    ADD INDEX idx_firebase_uid (firebase_uid);

-- Add reset password token columns if not already present
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS reset_password_token_expiry DATETIME NULL,
    ADD INDEX IF NOT EXISTS idx_reset_password_token (reset_password_token);