-- 创建 users 表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) DEFAULT NULL,
    organization VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(30) DEFAULT NULL,
    bio TEXT DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入管理员初始账号
-- 密码 123456 的 BCrypt 加密值
INSERT INTO users (username, password, role, created_at) 
VALUES ('admin', '$2a$10$X5wFuQ7qKeXRfFGcEiLNE.4n5p/YlV8xHLNJ/FKEJxF9Xj7nV2gp2', 'admin', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE username=username;

