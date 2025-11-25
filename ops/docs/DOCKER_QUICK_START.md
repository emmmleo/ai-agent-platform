# Docker 快速开始

## 🚀 一键部署

### Linux/Mac
```bash
chmod +x deploy.sh
./deploy.sh
```

### Windows
```cmd
ops\scripts\deploy.bat
```

## 📦 为什么这样设计？

### 后端 Dockerfile 设计要点

1. **多阶段构建**
   - 阶段1：使用Maven镜像构建JAR（包含完整构建工具）
   - 阶段2：使用JRE镜像运行（只包含运行时）
   - **结果**：镜像从800MB降到200MB

2. **Alpine Linux**
   - 使用`eclipse-temurin:21-jre-alpine`
   - Alpine是最小的Linux发行版
   - **结果**：进一步减小镜像体积

3. **非root用户**
   - 创建专用用户`spring`运行应用
   - **原因**：安全最佳实践，降低攻击风险

4. **层缓存优化**
   - 先复制`pom.xml`，再复制源代码
   - **原因**：依赖变化频率低，可以复用缓存层

### 前端 Dockerfile 设计要点

1. **多阶段构建**
   - 阶段1：使用Node.js镜像构建（包含npm和所有依赖）
   - 阶段2：使用Nginx镜像服务静态文件
   - **结果**：镜像从500MB降到25MB

2. **npm ci vs npm install**
   - 使用`npm ci`而不是`npm install`
   - **原因**：
     - 更快（跳过依赖解析）
     - 更可靠（严格按照package-lock.json）
     - 适合CI/CD环境

3. **Nginx配置**
   - 支持Vue Router的History模式
   - API请求代理到后端
   - 静态资源缓存优化

### Docker Compose 设计要点

1. **服务依赖**
   - 使用`depends_on`和`healthcheck`确保启动顺序
   - MySQL健康后才启动后端
   - 后端启动后才启动前端

2. **网络隔离**
   - 所有服务在同一Docker网络中
   - 通过服务名互相访问（如`mysql`、`backend`）
   - 隔离外部网络，提高安全性

3. **数据持久化**
   - MySQL数据保存在Docker卷中
   - 后端日志也保存在卷中
   - 容器删除后数据不丢失

4. **环境变量**
   - 通过环境变量覆盖配置
   - 无需修改源代码中的配置文件
   - 适合不同环境部署

## 📊 镜像大小对比

| 组件 | 单阶段 | 多阶段 | 节省 |
|------|--------|--------|------|
| 后端 | 800MB | 200MB | 75% |
| 前端 | 500MB | 25MB | 95% |
| **总计** | **1.3GB** | **225MB** | **83%** |

## 🔧 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend

# 重启服务
docker-compose restart backend

# 停止服务
docker-compose down

# 完全清理（包括数据卷）
docker-compose down -v
```

## 🌐 访问地址

- **前端**: http://localhost
- **后端API**: http://localhost:8082/api
- **phpMyAdmin**: http://localhost:8081

## 🔑 默认账号

- 管理员: `admin` / `123456`
- 普通用户: `user` / `123456`

## ⚠️ 注意事项

1. **端口占用**：确保3306、8081、8082、80端口未被占用
2. **Docker版本**：需要Docker 20.10+和Docker Compose 2.0+
3. **内存要求**：建议至少2GB可用内存
4. **首次启动**：可能需要几分钟下载镜像和构建

## 📚 详细文档

更多详细信息请查看 [DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md)

