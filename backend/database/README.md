# 数据库初始化说明

## 目录结构

```
backend/database/
├── init/              # MySQL 自动初始化脚本目录
│   └── 01-init-tables.sql  # 数据库表结构创建脚本
├── database_schema.sql     # 手动恢复脚本（用于已有数据库）
└── README.md          # 本说明文件
```

## 自动初始化（推荐）

### 工作原理

MySQL 容器在**首次启动**时（数据目录为空），会自动执行 `/docker-entrypoint-initdb.d/` 目录下的所有 `.sql` 文件（按文件名排序）。

### 使用场景

✅ **新电脑首次部署** - 自动创建所有表结构  
✅ **删除 volume 后重新部署** - 自动初始化  
✅ **开发环境重置** - 清理数据后自动恢复表结构  

### 配置说明

在 `docker-compose.yml` 中已配置：

```yaml
volumes:
  - mysql_data:/var/lib/mysql
  - ./backend/database/init:/docker-entrypoint-initdb.d
```

### 注意事项

⚠️ **重要**：MySQL 只在**首次初始化**时执行这些脚本。如果 volume 已经存在数据，**不会**再次执行。

如果需要重新初始化：
1. 停止容器：`docker-compose down`
2. 删除 volume：`docker volume rm codehubix_mysql_data`
3. 重新启动：`docker-compose up -d`

## 手动恢复（已有数据库）

如果数据库已经存在但表结构丢失，使用手动恢复脚本：

### Windows
```bash
ops\scripts\restore-database.bat
```

### Linux/Mac
```bash
./restore-database.sh
```

或直接执行 SQL：
```bash
docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db < backend/database_schema.sql
```

## 默认数据初始化

表结构创建后，Spring Boot 应用启动时会通过 `DataInitializer` 自动创建：

### 默认用户
- **管理员**: `admin` / `123456` (ROLE_ADMIN)
- **普通用户**: `user` / `123456` (ROLE_USER)

### 默认菜单
- 仪表盘 (/)
 - 个人信息 (/account/profile)
- 智能体管理 (/agents)
- 知识库管理 (/knowledge-bases)
- 工作流管理 (/workflows)
- 插件管理 (/plugins)
- 用户管理 (/users) - 仅管理员

## 验证初始化

### 检查表是否创建
```bash
docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db -e "SHOW TABLES;"
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

### 检查默认数据
```bash
# 检查默认用户
docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db -e "SELECT username, role FROM users;"

# 检查默认菜单
docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db -e "SELECT title, path FROM menus;"
```

## 常见问题

### Q: 为什么我的表没有被创建？

**A:** 可能的原因：
1. Volume 已经存在数据（MySQL 不会重复执行初始化脚本）
2. 脚本执行失败（查看容器日志：`docker logs codehubix-mysql`）
3. 文件路径不正确

**解决方案**：
- 删除 volume 重新初始化（见上方说明）
- 或使用手动恢复脚本

### Q: 如何添加新的初始化脚本？

**A:** 
1. 在 `backend/database/init/` 目录下创建新的 `.sql` 文件
2. 文件名使用数字前缀控制执行顺序，如：`02-init-data.sql`
3. 重启容器（如果是首次初始化）

### Q: 初始化脚本执行顺序？

**A:** MySQL 按**文件名排序**执行，建议使用数字前缀：
- `01-init-tables.sql` - 创建表结构
- `02-init-data.sql` - 插入初始数据（如果有）

## 相关文件

- `backend/database_schema.sql` - 手动恢复脚本
- `ops/scripts/restore-database.bat` - 恢复脚本
- `backend/src/main/java/com/aiagent/config/DataInitializer.java` - 默认数据初始化
