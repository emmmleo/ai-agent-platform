# 数据库恢复指南

## 概述

此文档用于在 Docker 数据库文件被删除后，重新创建所有数据库表结构。

## 数据库信息

- **数据库名**: `demo_db`
- **用户名**: `demo_user`
- **密码**: `demo_pass_123`
- **端口**: `3306`
- **字符集**: `utf8mb4`
- **排序规则**: `utf8mb4_0900_ai_ci`

## 恢复步骤

### 方法一：使用 MySQL 客户端执行 SQL 脚本

1. **确保 Docker 容器正在运行**
   ```bash
   docker-compose ps
   ```

2. **进入 MySQL 容器**
   ```bash
   docker exec -it codehubix-mysql bash
   ```

3. **登录 MySQL**
   ```bash
   mysql -u demo_user -pdemo_pass_123 demo_db
   ```

4. **执行 SQL 脚本**
   ```bash
   source /path/to/database_schema.sql
   ```
   或者直接复制 SQL 内容粘贴执行。

### 方法二：从宿主机执行 SQL 脚本

1. **从宿主机执行 SQL 脚本**
   ```bash
   docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db < backend/database_schema.sql
   ```

### 方法三：使用 phpMyAdmin

1. **访问 phpMyAdmin**
   - 打开浏览器访问: `http://localhost:8081`
   - 用户名: `demo_user`
   - 密码: `demo_pass_123`

2. **选择数据库**
   - 在左侧选择 `demo_db` 数据库

3. **导入 SQL 脚本**
   - 点击 "导入" 标签
   - 选择 `backend/database_schema.sql` 文件
   - 点击 "执行"

## 需要创建的表

根据项目实体类分析，需要创建以下 8 个表：

1. **users** - 用户表
   - 存储用户账号信息（用户名、密码哈希、角色等）

2. **menus** - 菜单表
   - 存储系统菜单配置信息

3. **agent** - 智能体表
   - 存储智能体配置信息

4. **knowledge_base** - 知识库表
   - 存储知识库基本信息

5. **knowledge_document** - 知识库文档表
   - 存储知识库中的文档内容

6. **workflow** - 工作流表
   - 存储工作流定义信息

7. **workflow_execution** - 工作流执行记录表
   - 存储工作流执行历史记录

8. **plugin** - 插件表
   - 存储插件配置信息

## 自动初始化数据

执行 SQL 脚本创建表后，当 Spring Boot 应用启动时，`DataInitializer` 会自动创建以下默认数据：

### 默认用户
- **管理员**: `admin` / `123456` (角色: ROLE_ADMIN)
- **普通用户**: `user` / `123456` (角色: ROLE_USER)

### 默认菜单
应用会自动创建以下菜单项：
- 仪表盘 (/)
- 个人信息 (/profile)
- 智能体管理 (/agents)
- 知识库管理 (/knowledge-bases)
- 工作流管理 (/workflows)
- 插件管理 (/plugins)
- 用户管理 (/users) - 仅管理员可见

## 验证恢复

### 1. 检查表是否创建成功

```sql
USE demo_db;
SHOW TABLES;
```

应该看到 8 个表：
- users
- menus
- agent
- knowledge_base
- knowledge_document
- workflow
- workflow_execution
- plugin

### 2. 检查表结构

```sql
DESCRIBE users;
DESCRIBE menus;
-- ... 其他表
```

### 3. 启动应用验证

```bash
docker-compose restart backend
```

查看日志确认：
- 表创建成功
- 默认用户和菜单初始化成功
- 应用正常启动

## 注意事项

1. **外键约束**: SQL 脚本中的外键约束已被注释，如需启用数据完整性检查，可以取消注释。

2. **数据备份**: 建议在恢复前备份现有数据（如果有）。

3. **字符集**: 确保数据库使用 `utf8mb4` 字符集以支持中文和 emoji。

4. **索引**: 所有表都已创建必要的索引以优化查询性能。

## 故障排查

### 问题：表已存在错误

如果遇到 "Table already exists" 错误，可以：

1. **删除现有表**（谨慎操作，会丢失数据）
   ```sql
   DROP TABLE IF EXISTS plugin;
   DROP TABLE IF EXISTS workflow_execution;
   DROP TABLE IF EXISTS workflow;
   DROP TABLE IF EXISTS knowledge_document;
   DROP TABLE IF EXISTS knowledge_base;
   DROP TABLE IF EXISTS agent;
   DROP TABLE IF EXISTS menus;
   DROP TABLE IF EXISTS users;
   ```

2. **或者使用 CREATE TABLE IF NOT EXISTS**（脚本已使用此语法）

### 问题：权限不足

确保使用正确的用户和密码：
- 用户: `demo_user`
- 密码: `demo_pass_123`

### 问题：字符集问题

如果遇到中文乱码，检查数据库字符集：
```sql
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';
```

## 联系支持

如果遇到其他问题，请检查：
1. Docker 容器日志: `docker logs codehubix-mysql`
2. 后端应用日志: `docker logs codehubix-backend`
3. 数据库连接配置: `backend/src/main/resources/application.yml`

