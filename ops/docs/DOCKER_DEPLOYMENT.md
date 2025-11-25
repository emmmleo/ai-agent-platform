# Docker 部署说明文档

## 📦 文件结构

```
ai-agent-platform/
├── backend/
│   ├── Dockerfile              # 后端Docker镜像构建文件
│   └── .dockerignore          # Docker构建忽略文件
├── frontend/
│   ├── Dockerfile              # 前端Docker镜像构建文件
│   ├── nginx.conf              # Nginx配置文件
│   └── .dockerignore          # Docker构建忽略文件
├── docker-compose.yml          # 完整的服务编排文件
└── deploy.sh                   # 一键部署脚本
```

## 🔍 详细设计说明

### 1. 后端 Dockerfile (`backend/Dockerfile`)

#### 为什么使用多阶段构建？

**多阶段构建的优势：**
- **减小镜像体积**：最终镜像只包含运行所需的JRE，不包含Maven和源代码
- **安全性**：不包含构建工具和源代码，减少攻击面
- **构建效率**：可以在构建阶段使用完整的构建工具链

#### 阶段1：构建阶段
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
```
- 使用官方Maven镜像，包含Java 21和Maven 3.9
- `AS build` 命名构建阶段，后续可以引用

```dockerfile
WORKDIR /app
COPY pom.xml .
COPY src ./src
```
- 先复制`pom.xml`，利用Docker层缓存机制
- 如果依赖未变化，Maven依赖下载会被缓存，加快构建速度

```dockerfile
RUN mvn clean package -DskipTests
```
- `-DskipTests`：跳过测试以加快构建（生产环境建议启用测试）

#### 阶段2：运行阶段
```dockerfile
FROM eclipse-temurin:21-jre-alpine
```
- 使用Alpine Linux基础镜像，体积小（约150MB）
- 只包含JRE（Java Runtime Environment），不包含JDK
- Java 21支持，与构建阶段版本一致

```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
```
- **安全最佳实践**：不使用root用户运行应用
- 创建专用用户`spring`，降低安全风险

```dockerfile
COPY --from=build /app/target/*.jar app.jar
```
- 从构建阶段复制编译好的JAR文件
- `--from=build`引用第一阶段

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/api/actuator/health || exit 1
```
- 健康检查机制，Docker可以监控容器健康状态
- `start-period=40s`：给应用40秒启动时间
- 注意：需要Spring Boot Actuator依赖（如果项目中没有，可以移除或修改检查方式）

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- 使用ENTRYPOINT确保JAR文件作为主进程运行
- 可以添加JVM参数，如：`-Xmx512m -Xms256m`

#### 镜像大小对比
- **单阶段构建**（包含Maven和JDK）：~800MB
- **多阶段构建**（仅JRE）：~200MB
- **节省空间**：约75%

---

### 2. 前端 Dockerfile (`frontend/Dockerfile`)

#### 为什么前端也需要多阶段构建？

**原因：**
- 构建阶段需要Node.js和所有依赖（包括devDependencies）
- 运行阶段只需要静态文件，用Nginx服务即可
- 最终镜像从~500MB（Node.js）降到~25MB（Nginx Alpine）

#### 阶段1：构建阶段
```dockerfile
FROM node:20-alpine AS build
```
- 使用Node.js 20 Alpine版本，体积较小

```dockerfile
COPY package*.json ./
RUN npm ci
```
- **为什么用`npm ci`而不是`npm install`？**
  - `npm ci`：从`package-lock.json`精确安装，更快更可靠
  - 适合CI/CD环境，确保依赖版本一致
  - 会先删除`node_modules`，确保干净安装

```dockerfile
RUN npm run build
```
- 执行构建命令，生成`dist/`目录（Vite默认输出目录）

#### 阶段2：运行阶段
```dockerfile
FROM nginx:alpine
```
- 使用Nginx Alpine镜像，体积小、性能好
- Nginx是生产环境常用的静态文件服务器

```dockerfile
COPY --from=build /app/dist /usr/share/nginx/html
```
- 复制构建产物到Nginx默认静态文件目录

```dockerfile
COPY nginx.conf /etc/nginx/conf.d/default.conf
```
- 复制自定义Nginx配置

#### 镜像大小对比
- **单阶段构建**（Node.js运行时）：~500MB
- **多阶段构建**（Nginx）：~25MB
- **节省空间**：约95%

---

### 3. Nginx 配置 (`frontend/nginx.conf`)

#### 关键配置说明

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```
- **Vue Router History模式支持**
- 当访问`/agents`等路由时，Nginx会尝试找对应文件，找不到则返回`index.html`
- 让Vue Router在前端处理路由

```nginx
location /api {
    proxy_pass http://backend:8082;
    ...
}
```
- **API代理**：前端请求`/api/*`会被代理到后端服务
- `backend`是docker-compose中定义的服务名
- 在Docker网络中，服务可以通过服务名互相访问

```nginx
gzip on;
```
- **Gzip压缩**：减小传输文件大小，提升加载速度

```nginx
location ~* \.(js|css|png|jpg|...)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```
- **静态资源缓存**：设置长期缓存，提升性能
- 文件名通常包含hash，内容变化时文件名也会变化

---

### 4. Docker Compose (`docker-compose.yml`)

#### 服务编排说明

```yaml
services:
  mysql:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", ...]
```
- **健康检查**：确保MySQL完全启动后再启动依赖服务

```yaml
  backend:
    environment:
      - SPRING_DATASOURCE_JDBC_URL=jdbc:mysql://mysql:3306/...
    depends_on:
      mysql:
        condition: service_healthy
```
- **环境变量覆盖**：通过环境变量覆盖`application.yml`中的配置
- `mysql`是服务名，Docker会自动解析为容器IP
- `depends_on`确保MySQL健康后才启动后端

```yaml
  frontend:
    depends_on:
      - backend
```
- 前端依赖后端，确保后端先启动

```yaml
networks:
  codehubix_network:
    driver: bridge
```
- 所有服务在同一网络中，可以通过服务名互相访问
- 隔离外部网络，提高安全性

```yaml
volumes:
  mysql_data:
  backend_logs:
```
- **数据持久化**：MySQL数据和后端日志保存在Docker卷中
- 容器删除后数据不会丢失

---

### 5. 部署脚本 (`deploy.sh`)

#### 脚本功能

1. **环境检查**
   - 检查Docker和Docker Compose是否安装
   - 检查端口是否被占用

2. **清理旧容器**（可选）
   - `--clean`参数：清理旧容器和数据卷

3. **构建镜像**
   - 构建后端和前端镜像
   - `--no-build`参数：跳过构建，使用已有镜像

4. **启动服务**
   - 按依赖顺序启动服务
   - 等待MySQL和后端就绪

5. **状态显示**
   - 显示服务访问地址
   - 显示常用命令

#### 使用方式

```bash
# 基本部署
./deploy.sh

# 清理后重新部署
./deploy.sh --clean

# 跳过构建（使用已有镜像）
./deploy.sh --no-build

# 查看帮助
./deploy.sh --help
```

---

## 🚀 部署流程

### 方式1：使用部署脚本（推荐）

**Linux/Mac:**
```bash
# 1. 给脚本添加执行权限
chmod +x deploy.sh

# 2. 执行部署
./deploy.sh
```

**Windows:**
```cmd
# 直接执行批处理文件
deploy.bat

# 或使用PowerShell
.\ops\scripts\deploy.bat
```

### 方式2：手动部署

```bash
# 1. 构建镜像
docker-compose build

# 2. 启动服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f
```

### 方式3：仅启动数据库（开发环境）

```bash
cd docker-mysql
docker-compose up -d
```

---

## 🔧 常用命令

```bash
# 查看所有服务状态
docker-compose ps

# 查看日志
docker-compose logs -f              # 所有服务
docker-compose logs -f backend       # 仅后端
docker-compose logs -f frontend      # 仅前端

# 重启服务
docker-compose restart backend
docker-compose restart frontend

# 停止服务
docker-compose stop

# 停止并删除容器
docker-compose down

# 停止并删除容器、数据卷
docker-compose down -v

# 进入容器
docker exec -it codehubix-backend sh
docker exec -it codehubix-mysql mysql -u demo_user -p

# 查看镜像大小
docker images | grep codehubix
```

---

## 📊 镜像大小优化总结

| 组件 | 单阶段构建 | 多阶段构建 | 节省 |
|------|-----------|-----------|------|
| 后端 | ~800MB | ~200MB | 75% |
| 前端 | ~500MB | ~25MB | 95% |
| **总计** | **~1.3GB** | **~225MB** | **83%** |

---

## ⚠️ 注意事项

### 1. 后端健康检查
如果项目中没有Spring Boot Actuator依赖，需要：
- 移除Dockerfile中的HEALTHCHECK
- 或添加Actuator依赖到`pom.xml`

### 2. 数据库连接
- Docker环境中，`localhost`应改为服务名`mysql`
- 通过环境变量覆盖配置，无需修改`application.yml`

### 3. 前端API代理
- 开发环境：Vite代理到`localhost:8082`
- 生产环境：Nginx代理到`backend:8082`（Docker服务名）

### 4. 端口冲突
- 如果80端口被占用，可以修改`docker-compose.yml`中的端口映射
- 例如：`"8080:80"`，然后访问`http://localhost:8080`

### 5. 数据持久化
- MySQL数据保存在`mysql_data`卷中
- 删除容器不会删除数据，删除卷才会：`docker-compose down -v`

---

## 🔒 生产环境建议

1. **安全配置**
   - 修改默认密码
   - 使用环境变量文件（`.env`）管理敏感信息
   - 启用HTTPS

2. **性能优化**
   - 添加JVM参数（内存限制等）
   - 配置Nginx缓存策略
   - 启用数据库连接池优化

3. **监控和日志**
   - 集成日志收集系统（如ELK）
   - 添加应用监控（如Prometheus）
   - 配置告警机制

4. **高可用**
   - 使用Docker Swarm或Kubernetes
   - 配置负载均衡
   - 数据库主从复制

---

## 📝 总结

这些Dockerfile和部署脚本的设计遵循了以下最佳实践：

1. ✅ **多阶段构建**：减小镜像体积
2. ✅ **非root用户**：提高安全性
3. ✅ **健康检查**：确保服务可用性
4. ✅ **数据持久化**：保护重要数据
5. ✅ **服务编排**：自动化部署流程
6. ✅ **环境隔离**：使用Docker网络

通过这些配置，可以实现：
- 🚀 快速部署
- 📦 小体积镜像
- 🔒 安全运行
- 🔄 易于维护

