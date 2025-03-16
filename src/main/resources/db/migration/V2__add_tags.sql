-- Create tags table
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_tag (user_id, name)
);

-- Create file_tags table
CREATE TABLE file_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE KEY unique_file_tag (file_id, tag_id)
);

-- Create indices for performance
CREATE INDEX idx_tags_user_id ON tags(user_id);
CREATE INDEX idx_file_tags_file_id ON file_tags(file_id);
CREATE INDEX idx_file_tags_tag_id ON file_tags(tag_id);