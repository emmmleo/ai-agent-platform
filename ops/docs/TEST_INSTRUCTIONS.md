# 前后端联调测试指令

## 前置准备

### 1. 确保 Docker MySQL 容器运行
```bash
cd C:\Users\14494\mygithub\my_project\docker-mysql
docker compose ps
# 如果容器未运行，执行：
docker compose up -d
```

### 2. 确保数据库已创建
连接到 MySQL 并创建数据库（如果不存在）：
```sql
CREATE DATABASE IF NOT EXISTS aiagent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 启动后端服务

### 方式一：使用 Maven（推荐）
```bash
cd backend
mvn spring-boot:run
```

### 方式二：使用 IDE
- 在 IDE 中打开 `backend/src/main/java/com/aiagent/Application.java`
- 右键运行 `main` 方法

### 验证后端启动
后端启动成功后，访问以下 URL 验证：
- 健康检查：http://localhost:8082/api/test/health
- GET 测试：http://localhost:8082/api/test/hello?name=Test
- 浏览器应返回 JSON 响应

## 启动前端服务

```bash
cd frontend
npm install  # 如果还没安装依赖
npm run dev
```

前端启动成功后，访问：
- http://localhost:5173

## 测试步骤

### 1. 打开前端页面
在浏览器中打开 http://localhost:5173

### 2. 测试接口
页面提供了三个测试按钮：
- **健康检查**：测试后端服务是否正常运行
- **GET 请求测试**：测试 GET 请求和查询参数
- **POST 请求测试**：测试 POST 请求和请求体

### 3. 查看结果
- 成功：页面会显示绿色的响应结果区域，包含后端返回的 JSON 数据
- 失败：页面会显示红色的错误信息区域

## 测试接口说明

### 健康检查接口
- **URL**: `/api/test/health`
- **方法**: GET
- **说明**: 检查后端服务状态

### GET 请求测试
- **URL**: `/api/test/hello?name=xxx`
- **方法**: GET
- **参数**: name (可选)
- **说明**: 测试 GET 请求和查询参数传递

### POST 请求测试
- **URL**: `/api/test/echo`
- **方法**: POST
- **请求体**: JSON 格式
- **说明**: 测试 POST 请求和请求体传递

## 常见问题排查

### 1. 后端启动失败
- 检查端口 8082 是否被占用
- 检查数据库连接配置是否正确
- 检查 MySQL 容器是否运行
- 查看后端控制台错误信息

### 2. 前端无法连接后端
- 确认后端服务已启动（访问 http://localhost:8082/api/test/health）
- 检查 `vite.config.ts` 中的代理配置
- 检查浏览器控制台的网络请求和错误信息

### 3. CORS 跨域错误
- 确认后端 `CorsConfig.java` 已正确配置
- 检查后端日志是否有 CORS 相关错误

### 4. 数据库连接失败
- 确认 MySQL 容器正在运行
- 检查 `application.yml` 中的数据库配置
- 确认数据库 `aiagent` 已创建
- 验证用户名和密码是否正确

## 快速测试命令

### 使用 curl 测试后端（可选）
```bash
# 健康检查
curl http://localhost:8082/api/test/health

# GET 请求测试
curl "http://localhost:8082/api/test/hello?name=Test"

# POST 请求测试
curl -X POST http://localhost:8082/api/test/echo \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello from curl"}'
```

## 预期结果

### 健康检查响应示例
```json
{
  "code": 200,
  "message": "Backend service is running",
  "data": {
    "status": "healthy",
    "timestamp": "2025-11-19T20:45:00",
    "service": "ai-agent-backend"
  }
}
```

### GET 请求响应示例
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "greeting": "Hello Vue + Spring Boot!",
    "timestamp": "2025-11-19T20:45:00",
    "method": "GET"
  }
}
```

### POST 请求响应示例
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "received": {
      "message": "Hello from Frontend!",
      "timestamp": "2025-11-19T20:45:00"
    },
    "timestamp": "2025-11-19T20:45:00",
    "method": "POST"
  }
}
```

