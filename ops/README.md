# 运维脚本和文档目录

本目录包含项目的运维脚本和技术文档。

## 📁 目录结构

```
ops/
├── scripts/          # 运维脚本
│   ├── deploy.bat           # Windows 部署脚本
│   ├── deploy.sh            # Linux/Mac 部署脚本
│   ├── export-database.bat  # Windows 数据库导出脚本
│   ├── export-database.sh   # Linux/Mac 数据库导出脚本
│   ├── import-database.bat  # Windows 数据库导入脚本
│   ├── import-database.sh   # Linux/Mac 数据库导入脚本
│   └── restore-database.bat # Windows 数据库恢复脚本
│
└── docs/             # 技术文档
    ├── DATABASE_BACKUP_GUIDE.md    # 数据库备份/导入详细说明
    ├── DATABASE_RECOVERY.md        # 数据库恢复指南
    ├── DOCKER_DEPLOYMENT.md        # Docker 部署详细文档
    ├── DOCKER_MIRROR_SETUP.md      # Docker 镜像加速配置
    ├── DOCKER_QUICK_START.md       # Docker 快速开始
    └── TEST_INSTRUCTIONS.md        # 测试说明（历史参考）
```

## 🚀 快速使用

### 部署项目

**Windows:**
```cmd
ops\scripts\deploy.bat
```

**Linux/Mac:**
```bash
chmod +x ops/scripts/deploy.sh
./ops/scripts/deploy.sh
```

### 数据库操作

**导出数据库:**
```cmd
# Windows
ops\scripts\export-database.bat

# Linux/Mac
./ops/scripts/export-database.sh
```

**导入数据库:**
```cmd
# Windows
ops\scripts\import-database.bat

# Linux/Mac
./ops/scripts/import-database.sh
```

**恢复数据库表结构:**
```cmd
# Windows
ops\scripts\restore-database.bat
```

## 📚 文档说明

### 核心文档（根目录）
- `README.md` - 项目主文档
- `START_GUIDE.md` - 启动指南（推荐新用户阅读）
- `TROUBLESHOOTING.md` - 故障排查指南
- `PROJECT_OVERVIEW.md` - 项目概览

### 技术文档（本目录）
- `DATABASE_BACKUP_GUIDE.md` - 数据库备份/导入实现流程详解
- `DATABASE_RECOVERY.md` - 数据库恢复详细步骤
- `DOCKER_DEPLOYMENT.md` - Docker 部署完整文档
- `DOCKER_MIRROR_SETUP.md` - Docker 镜像加速配置指南
- `DOCKER_QUICK_START.md` - Docker 快速开始指南
- `TEST_INSTRUCTIONS.md` - 测试说明（历史参考，可能已过时）

## 🔗 相关链接

- 项目主文档: [../README.md](../README.md)
- 启动指南: [../START_GUIDE.md](../START_GUIDE.md)
- 故障排查: [../TROUBLESHOOTING.md](../TROUBLESHOOTING.md)

