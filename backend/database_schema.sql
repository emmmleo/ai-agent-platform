-- ============================================
-- CodeHubix 数据库表结构创建脚本
-- 数据库名: demo_db
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_0900_ai_ci
-- ============================================

-- 使用数据库
USE demo_db;

-- ============================================
-- 1. 用户表 (users)
-- ============================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `role` VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER' COMMENT '角色：ROLE_USER, ROLE_ADMIN',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ============================================
-- 2. 菜单表 (menus)
-- ============================================
CREATE TABLE IF NOT EXISTS `menus` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id` BIGINT NULL COMMENT '父菜单ID',
    `title` VARCHAR(100) NOT NULL COMMENT '菜单标题',
    `path` VARCHAR(200) NOT NULL COMMENT '菜单路径',
    `order_num` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `allowed_roles` VARCHAR(200) NULL COMMENT '允许的角色（逗号分隔）',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';

-- ============================================
-- 3. 智能体表 (agent)
-- ============================================
CREATE TABLE IF NOT EXISTS `agent` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
    `user_id` BIGINT NOT NULL COMMENT '创建者ID',
    `name` VARCHAR(200) NOT NULL COMMENT '智能体名称',
    `description` TEXT NULL COMMENT '描述',
    `system_prompt` TEXT NULL COMMENT '系统提示词',
    `user_prompt_template` TEXT NULL COMMENT '用户提示词模板',
    `model_config` TEXT NULL COMMENT '模型配置（JSON字符串）',
    `workflow_id` BIGINT NULL COMMENT '关联工作流ID',
    `knowledge_base_ids` TEXT NULL COMMENT '关联知识库ID列表（JSON字符串）',
    `plugin_ids` TEXT NULL COMMENT '关联插件ID列表（JSON字符串）',
    `status` VARCHAR(50) NOT NULL DEFAULT 'draft' COMMENT '状态：draft, published',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体表';

-- ============================================
-- 4. 知识库表 (knowledge_base)
-- ============================================
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `user_id` BIGINT NOT NULL COMMENT '创建者ID',
    `name` VARCHAR(200) NOT NULL COMMENT '知识库名称',
    `description` TEXT NULL COMMENT '知识库描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库表';

-- ============================================
-- 5. 知识库文档表 (knowledge_document)
-- ============================================
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `user_id` BIGINT NOT NULL COMMENT '上传者ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_type` VARCHAR(50) NULL COMMENT '文件类型：txt/md',
    `file_size` BIGINT NULL COMMENT '文件大小（字节）',
    `content` LONGTEXT NULL COMMENT '文档内容',
    `status` VARCHAR(50) NOT NULL DEFAULT 'processing' COMMENT '处理状态：processing/processed/failed',
    `chunk_count` INT NULL DEFAULT 0 COMMENT '分块数量',
    `vectorized` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已向量化：0-否，1-是',
    `error_message` TEXT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_base_id` (`knowledge_base_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_vectorized` (`vectorized`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档表';

-- ============================================
-- 6. 工作流表 (workflow)
-- ============================================
CREATE TABLE IF NOT EXISTS `workflow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
    `user_id` BIGINT NOT NULL COMMENT '创建者ID',
    `name` VARCHAR(200) NOT NULL COMMENT '工作流名称',
    `description` TEXT NULL COMMENT '工作流描述',
    `definition` LONGTEXT NULL COMMENT '工作流定义（JSON字符串）',
    `status` VARCHAR(50) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流表';

-- ============================================
-- 7. 工作流执行记录表 (workflow_execution)
-- ============================================
CREATE TABLE IF NOT EXISTS `workflow_execution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
    `workflow_id` BIGINT NOT NULL COMMENT '工作流ID',
    `user_id` BIGINT NOT NULL COMMENT '执行者ID',
    `status` VARCHAR(50) NOT NULL DEFAULT 'pending' COMMENT '执行状态：pending/running/completed/failed',
    `input_params` TEXT NULL COMMENT '输入参数（JSON字符串）',
    `output_result` LONGTEXT NULL COMMENT '输出结果（JSON字符串）',
    `error_message` TEXT NULL COMMENT '错误信息',
    `started_at` DATETIME NULL COMMENT '开始时间',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流执行记录表';

-- ============================================
-- 8. 插件表 (plugin)
-- ============================================
CREATE TABLE IF NOT EXISTS `plugin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '插件ID',
    `user_id` BIGINT NOT NULL COMMENT '创建者ID',
    `name` VARCHAR(200) NOT NULL COMMENT '插件名称',
    `description` TEXT NULL COMMENT '插件描述',
    `openapi_spec` LONGTEXT NULL COMMENT 'OpenAPI规范（JSON字符串）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='插件表';

-- ============================================
-- 9. 智能体会话上下文表 (agent_conversation_context)
-- ============================================
CREATE TABLE IF NOT EXISTS `agent_conversation_context` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `agent_id` BIGINT NOT NULL COMMENT '智能体ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `messages` LONGTEXT NOT NULL COMMENT '上下文消息（JSON）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_user` (`agent_id`, `user_id`),
    KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体会话上下文表';

-- ============================================
-- 外键约束（可选，根据实际需求决定是否添加）
-- ============================================
-- 注意：如果添加外键约束，需要确保数据完整性
-- 以下外键约束可以根据实际需求启用或禁用

-- ALTER TABLE `agent` ADD CONSTRAINT `fk_agent_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `agent` ADD CONSTRAINT `fk_agent_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`id`) ON DELETE SET NULL;

-- ALTER TABLE `knowledge_base` ADD CONSTRAINT `fk_knowledge_base_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- ALTER TABLE `knowledge_document` ADD CONSTRAINT `fk_knowledge_document_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `knowledge_document` ADD CONSTRAINT `fk_knowledge_document_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- ALTER TABLE `workflow` ADD CONSTRAINT `fk_workflow_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- ALTER TABLE `workflow_execution` ADD CONSTRAINT `fk_workflow_execution_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflow` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `workflow_execution` ADD CONSTRAINT `fk_workflow_execution_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- ALTER TABLE `plugin` ADD CONSTRAINT `fk_plugin_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- ALTER TABLE `menus` ADD CONSTRAINT `fk_menus_parent` FOREIGN KEY (`parent_id`) REFERENCES `menus` (`id`) ON DELETE SET NULL;

-- ============================================
-- 脚本执行完成
-- ============================================
-- 执行此脚本后，应用启动时会通过 DataInitializer 自动创建默认用户和菜单数据
-- 默认用户：
--   - admin / 123456 (ROLE_ADMIN)
--   - user / 123456 (ROLE_USER)
-- ============================================

