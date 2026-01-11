# 故障排查指南

## 🔧 常见问题解决

### 问题1：脚本执行时出现乱码

**症状：**
```
[INFO] 部署脚本 (Windows)
==========================================
乱码文字...
```

**原因：**
Windows命令行默认使用GBK编码，而脚本文件可能是UTF-8编码。

**解决方案：**
1. ✅ **已修复**：脚本已自动设置UTF-8编码（`chcp 65001`）
2. 如果仍有问题，手动设置：
   ```cmd
   chcp 65001
   ops\scripts\deploy.bat
   ```
3. 或者使用PowerShell执行：
   ```powershell
   [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
   .\ops\scripts\deploy.bat
   ```

---

### 问题2：deploy.bat 执行失败

#### 2.1 Docker未安装或未运行

**错误信息：**
```
[ERROR] Docker 未安装，请先安装 Docker Desktop
```

**解决方案：**
1. 下载安装 Docker Desktop：https://www.docker.com/products/docker-desktop
2. 安装后启动 Docker Desktop
3. 等待Docker完全启动（系统托盘图标不再闪烁）
4. 验证安装：
   ```cmd
   docker --version
   docker-compose --version
   ```

#### 2.2 端口被占用

**错误信息：**
```
Error: bind: address already in use
ERROR: for mysql  Cannot start service mysql: driver failed programming external connectivity
```

**解决方案：**
1. 检查端口占用：
   ```cmd
   netstat -ano | findstr ":3306"
   netstat -ano | findstr ":8081"
   netstat -ano | findstr ":8082"
   netstat -ano | findstr ":80"
   ```

2. 停止占用端口的程序，或修改 `docker-compose.yml` 中的端口映射：
   ```yaml
   ports:
     - "3307:3306"  # 改为3307
     - "8083:8082"  # 改为8083
   ```

#### 2.3 镜像构建失败

**错误信息：**
```
ERROR: failed to build backend
ERROR: failed to build frontend
```

**解决方案：**

1. **检查网络连接**
   - 确保能访问Docker Hub
   - 如果在国内，可能需要配置镜像加速器

2. **清理Docker缓存**
   ```cmd
   docker system prune -a
   ```

3. **重新构建（不使用缓存）**
   ```cmd
   docker-compose build --no-cache backend
   docker-compose build --no-cache frontend
   ```

4. **查看详细错误**
   ```cmd
   docker-compose build backend 2>&1 | more
   ```

5. **检查磁盘空间**
   ```cmd
   docker system df
   ```

#### 2.4 服务启动失败

**错误信息：**
```
[ERROR] 服务启动失败
```

**解决方案：**

1. **检查Docker Desktop状态**
   - 确保Docker Desktop正在运行
   - 检查系统资源（CPU、内存）是否充足

2. **查看详细日志**
   ```cmd
   docker-compose logs
   docker-compose logs mysql
   docker-compose logs backend
   ```

3. **检查容器状态**
   ```cmd
   docker-compose ps
   docker ps -a
   ```

4. **尝试单独启动服务**
   ```cmd
   docker-compose up mysql -d
   docker-compose up backend -d
   docker-compose up frontend -d
   ```

#### 2.5 MySQL启动超时

**错误信息：**
```
[ERROR] MySQL 启动超时
```

**解决方案：**

1. **检查MySQL容器日志**
   ```cmd
   docker logs codehubix-mysql
   ```

2. **手动检查MySQL状态**
   ```cmd
   docker exec codehubix-mysql mysqladmin ping -h localhost -u demo_user -pdemo_pass_123
   ```

3. **增加等待时间**
   - 编辑 `ops\scripts\deploy.bat`，将 `TIMEOUT=60` 改为更大的值

4. **检查数据卷**
   ```cmd
   docker volume ls
   docker volume inspect codehubix_mysql_data
   ```

#### 2.6 后端服务无法连接

**错误信息：**
```
[WARN] 后端服务启动可能较慢
```

**解决方案：**

1. **检查后端日志**
   ```cmd
   docker logs codehubix-backend
   docker-compose logs -f backend
   ```

2. **检查后端是否能访问数据库**
   ```cmd
   docker exec codehubix-backend ping mysql
   ```

3. **手动测试后端API**
   ```cmd
   # 使用PowerShell
   powershell -Command "Invoke-WebRequest -Uri 'http://localhost:8082/api' -Method Get"
   
   # 或使用curl（如果已安装）
   curl http://localhost:8082/api
   ```

4. **检查后端容器状态**
   ```cmd
   docker ps | findstr backend
   docker inspect codehubix-backend
   ```

---

### 问题3：curl命令不存在

**症状：**
```
'curl' 不是内部或外部命令
```

**解决方案：**

1. ✅ **已修复**：脚本已自动检测并使用PowerShell的 `Invoke-WebRequest`
2. 如果仍有问题，可以安装curl：
   - Windows 10 1803+ 自带curl
   - 或下载：https://curl.se/windows/

---

### 问题4：PowerShell执行策略限制

**症状：**
```
无法加载文件，因为在此系统上禁止运行脚本
```

**解决方案：**

1. **以管理员身份运行PowerShell**
2. **设置执行策略**：
   ```powershell
   Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

---

### 问题5：Docker Desktop资源不足

**症状：**
```
容器启动失败
内存不足
```

**解决方案：**

1. **增加Docker Desktop资源分配**
   - 打开 Docker Desktop
   - Settings → Resources
   - 增加 CPU 和 Memory 分配

2. **关闭其他占用资源的程序**

3. **检查Docker资源使用**
   ```cmd
   docker stats
   ```

---

## 🔍 诊断步骤

### 步骤1：检查环境

```cmd
# 检查Docker
docker --version
docker-compose --version

# 检查Docker是否运行
docker ps

# 检查端口占用
netstat -ano | findstr ":3306 :8081 :8082 :80"
```

### 步骤2：检查容器状态

```cmd
# 查看所有容器
docker-compose ps

# 查看所有容器（包括停止的）
docker ps -a

# 查看特定容器日志
docker logs codehubix-mysql
docker logs codehubix-backend
docker logs codehubix-frontend
```

### 步骤3：检查网络

```cmd
# 检查Docker网络
docker network ls
docker network inspect codehubix_codehubix_network

# 测试容器间连通性
docker exec codehubix-backend ping mysql
```

### 步骤4：检查数据卷

```cmd
# 查看所有数据卷
docker volume ls

# 检查MySQL数据卷
docker volume inspect codehubix_mysql_data
```

---

## 🛠️ 重置和清理

### 完全重置（删除所有容器和数据）

```cmd
# 停止并删除所有容器
docker-compose down -v

# 删除所有相关镜像
docker rmi codehubix-backend codehubix-frontend

# 清理未使用的资源
docker system prune -a
```

### 仅重置数据库

```cmd
# 停止MySQL容器
docker-compose stop mysql

# 删除MySQL数据卷
docker volume rm codehubix_mysql_data

# 重新启动
docker-compose up mysql -d
```

---

## 📞 获取帮助

### 查看日志

```cmd
# 所有服务日志
docker-compose logs -f

# 特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql

# 最近100行日志
docker-compose logs --tail=100 backend
```

### 进入容器调试

```cmd
# 进入后端容器
docker exec -it codehubix-backend sh

# 进入MySQL容器
docker exec -it codehubix-mysql bash

# 连接MySQL
docker exec -it codehubix-mysql mysql -u demo_user -p
# 密码: demo_pass_123
```

### 检查配置文件

```cmd
# 验证docker-compose.yml语法
docker-compose config

# 查看服务配置
docker-compose config --services
```

---

## ✅ 成功启动检查清单

部署成功后，应该满足以下条件：

- [ ] 所有容器状态为 "Up"
  ```cmd
  docker-compose ps
  ```

- [ ] 可以访问前端
  - 浏览器打开：http://localhost

- [ ] 可以访问后端API
  ```cmd
  powershell -Command "Invoke-WebRequest -Uri 'http://localhost:8082/api'"
  ```

- [ ] 可以访问phpMyAdmin
  - 浏览器打开：http://localhost:8081

- [ ] 可以登录系统
  - 用户名：admin
  - 密码：123456

---

## 💡 提示

1. **首次启动较慢**：需要下载Docker镜像，可能需要5-10分钟
2. **构建镜像较慢**：首次构建需要编译代码，可能需要3-5分钟
3. **MySQL启动需要时间**：MySQL初始化可能需要30-60秒
4. **查看实时日志**：使用 `docker-compose logs -f` 查看实时日志

---

如果以上方法都无法解决问题，请提供以下信息：

1. 错误信息截图
2. `docker-compose ps` 的输出
3. `docker-compose logs` 的相关错误日志
4. Windows版本和Docker Desktop版本

