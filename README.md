# ai-agent-platform

基于 AI 的智能体管理平台

## 🚀 快速开始

### 一键部署

**Windows:**
```cmd
deploy.bat
```

**Linux/Mac:**
```bash
chmod +x deploy.sh
./deploy.sh
```

### 访问地址

- **前端应用**: http://localhost
- **后端 API**: http://localhost:8082/api
- **phpMyAdmin**: http://localhost:8081

### 默认账号

- 管理员: `admin` / `123456`
- 普通用户: `user` / `123456`

## 📚 文档

- [启动指南](START_GUIDE.md) - 详细的启动和部署说明（推荐新用户阅读）
- [项目概览](PROJECT_OVERVIEW.md) - 项目架构和技术栈说明
- [故障排查](TROUBLESHOOTING.md) - 常见问题和解决方案
- [运维文档](ops/README.md) - 运维脚本和技术文档

## 🏗️ 项目结构

```
ai-agent-platform/
├── backend/          # 后端服务（Spring Boot）
├── frontend/         # 前端应用（Vue 3）
├── ops/              # 运维脚本和文档
│   ├── scripts/      # 部署和数据库脚本
│   └── docs/         # 技术文档
├── docker-compose.yml # Docker 编排配置
└── README.md         # 本文件
```

## 🔧 技术栈

- **后端**: Spring Boot 3.5.6 + Java 21 + MyBatis + MySQL
- **前端**: Vue 3 + TypeScript + Vite
- **部署**: Docker + Docker Compose

## 📝 更多信息

- 详细文档请查看 [START_GUIDE.md](START_GUIDE.md)
- 如果需要配置代理，请查看 [PROXY_CONFIG.md](PROXY_CONFIG.md)

