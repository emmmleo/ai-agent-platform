# ai-agent-platform项目启动指南

## 🚀 快速启动（推荐：Docker方式）

### 前置条件

1. **安装 Docker Desktop for Windows**
   - 下载地址：https://www.docker.com/products/docker-desktop
   - 安装后确保Docker Desktop正在运行（系统托盘有Docker图标）

2. **验证Docker安装**
   ```cmd
   docker --version
   docker-compose --version
   ```
3. **保证docker可以拉取镜像**

   - (1)设置国内镜像加速（docker-setting-docker engine）
   - (2)使用IP代理(clash),需要设置docker-setting-resources-proxirs:http和https,均设置为http://你的代理内网IP：端口，例如：http://10.7.47.36:7890
```
registry-mirrors": [
    "https://registry.docker-cn.com",
    "https://docker.mirrors.ustc.edu.cn/",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"]
```
   

  
  

### 方式一：使用部署脚本（最简单）

#### 步骤1：打开命令行
- 按 `Win + R`，输入 `cmd`，回车
- 或者在项目根目录右键，选择"在终端中打开"

#### 步骤2：执行部署脚本(docker能直接拉取镜像)
```cmd
cd 你的目录\ai-agent-platform\ops\scripts
```
```
.\deploy.bat
```
######  对于步骤2，如果docker不能直接拉取镜像，比如使用windows+wsl2+docker，设置国内加速器失败，导致构建失败可以参考我的解决办法
##### 1、cmd执行：ipconfig
##### 2、识别到代理ip地址，查看你的代理端口，比如clash的7890
##### 3、将http://your ip//your port填入到docekr-setting-reaources-proxies
##### 4、重启docker
##### 5、参考[PROXY_CONFIG.md](PROXY_CONFIG.md)，设置环境变量
##### 6、执行
```
.\deploy.bat
```




#### 步骤3：等待部署完成
脚本会自动：
1. ✅ 检查Docker环境
2. ✅ 构建后端和前端镜像（首次需要几分钟）
3. ✅ 启动MySQL数据库
4. ✅ **自动创建数据库表结构**（首次启动时）
5. ✅ 启动后端服务（自动初始化默认用户和菜单）
6. ✅ 启动前端服务
7. ✅ 等待所有服务就绪

**💡 提示**：在新电脑上首次运行 `docker compose up -d` 时，MySQL 会自动执行初始化脚本创建所有表结构，无需手动操作！

#### 步骤4：访问应用
部署成功后，访问以下地址：

- **前端应用**: http://localhost
- **后端API**: http://localhost:8082/api
- **phpMyAdmin**: http://localhost:8081

**默认登录账号：**
- 管理员: `admin` / `123456`
- 普通用户: `user` / `123456`

**📝 数据库说明：**
- 首次启动时，MySQL 会自动创建所有表结构（8个表）
- 后端启动时会自动创建默认用户和菜单数据
- 如果数据库表已存在，不会重复创建（安全）

---

### 方式二：手动使用Docker Compose

如果不想使用脚本，可以手动执行：

```cmd
# 1. 构建镜像（首次运行需要）
docker-compose build

# 2. 启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看日志
docker-compose logs -f
```

---

## 🔧 传统方式启动（开发环境）

如果你不想使用Docker，可以分别启动各个服务：

### 前置条件

1. **Java 21** - 后端需要
2. **Maven** - 后端构建工具
3. **Node.js 20+** - 前端需要
4. **MySQL 8.0** - 数据库

### 步骤1：启动数据库

#### 选项A：使用Docker启动MySQL（推荐）
```cmd
cd docker-mysql
docker-compose up -d
```

#### 选项B：使用本地MySQL
- 确保MySQL服务正在运行
- 创建数据库：`demo_db`
- 创建用户：`demo_user` / `demo_pass_123`

### 步骤2：启动后端

```cmd
cd backend

# 方式1：使用Maven运行
mvn spring-boot:run

# 方式2：先编译再运行
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

后端启动在：http://localhost:8082/api

### 步骤3：启动前端

```cmd
cd frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

前端启动在：http://localhost:5173

---

## 📋 启动脚本参数说明

### ops\scripts\ops\scripts\deploy.bat 参数

```cmd
# 基本部署
ops\scripts\deploy.bat

# 清理后重新部署（删除旧容器和数据）
ops\scripts\deploy.bat --clean

# 跳过构建（使用已有镜像，加快启动）
ops\scripts\deploy.bat --no-build

# 查看帮助
ops\scripts\deploy.bat --help
```

---

## ✅ 验证服务是否启动成功

### 1. 检查Docker容器状态

```cmd
docker-compose ps
```

应该看到4个服务都在运行：
- `codehubix-mysql` - MySQL数据库
- `codehubix-phpmyadmin` - phpMyAdmin
- `codehubix-backend` - 后端服务
- `codehubix-frontend` - 前端服务

### 2. 检查服务日志

```cmd
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

### 3. 测试服务连接

```cmd
# 测试后端API
curl http://localhost:8082/api

# 测试前端
curl http://localhost

# 测试phpMyAdmin
curl http://localhost:8081
```

或在浏览器中直接访问上述地址。

---

## 🛠️ 常用管理命令

### 查看服务状态
```cmd
docker-compose ps
```

### 查看日志
```cmd
# 实时查看所有日志
docker-compose logs -f

# 查看后端日志
docker-compose logs -f backend

# 查看最近100行日志
docker-compose logs --tail=100 backend
```

### 重启服务
```cmd
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend
docker-compose restart frontend
```

### 停止服务
```cmd
# 停止服务（保留容器）
docker-compose stop

# 停止并删除容器
docker-compose down

# 停止并删除容器和数据卷（⚠️ 会删除数据库数据）
docker-compose down -v
```

### 进入容器
```cmd
# 进入后端容器
docker exec -it codehubix-backend sh

# 进入MySQL容器
docker exec -it codehubix-mysql mysql -u demo_user -p
# 密码：demo_pass_123
```

---

## ⚠️ 常见问题解决

### 问题1：端口被占用

**错误信息：**
```
Error: bind: address already in use
```

**解决方法：**
1. 检查端口占用：
   ```cmd
   netstat -ano | findstr :3306
   netstat -ano | findstr :8082
   netstat -ano | findstr :80
   ```

2. 停止占用端口的程序，或修改`docker-compose.yml`中的端口映射

### 问题2：Docker未运行

**错误信息：**
```
Cannot connect to the Docker daemon
```

**解决方法：**
1. 确保Docker Desktop正在运行
2. 检查系统托盘是否有Docker图标
3. 重启Docker Desktop

### 问题3：构建失败

**错误信息：**
```
ERROR: failed to build backend
```

**解决方法：**
1. 检查网络连接（需要下载Docker镜像）
2. 清理Docker缓存：
   ```cmd
   docker system prune -a
   ```
3. 重新构建：
   ```cmd
   docker-compose build --no-cache backend
   ```

### 问题4：后端无法连接数据库

**错误信息：**
```
Communications link failure
```

**解决方法：**
1. 确保MySQL容器已启动：
   ```cmd
   docker-compose ps
   ```
2. 检查MySQL日志：
   ```cmd
   docker-compose logs mysql
   ```
3. 等待MySQL完全启动（可能需要30-60秒）

### 问题5：前端无法访问后端

**解决方法：**
1. 检查后端是否运行：
   ```cmd
   curl http://localhost:8082/api
   ```
2. 检查前端Nginx配置中的代理设置
3. 查看前端日志：
   ```cmd
   docker-compose logs frontend
   ```

### 问题6：Windows上curl命令不存在

**解决方法：**
1. 使用PowerShell的`Invoke-WebRequest`：
   ```powershell
   Invoke-WebRequest http://localhost:8082/api
   ```
2. 或在浏览器中直接访问地址

---

## 📊 服务启动顺序

Docker Compose会自动按依赖关系启动服务：

```
1. MySQL (健康检查通过)
   ↓
2. phpMyAdmin (等待MySQL)
   ↓
3. 后端 (等待MySQL健康)
   ↓
4. 前端 (等待后端)
```

---

## 🔍 检查清单

启动前检查：
- [ ] Docker Desktop已安装并运行
- [ ] 端口3306、8081、8082、80未被占用
- [ ] 有足够的磁盘空间（至少2GB）
- [ ] 网络连接正常（需要下载镜像）

启动后检查：
- [ ] 所有容器状态为"Up"
- [ ] 后端日志无错误
- [ ] 可以访问 http://localhost
- [ ] 可以访问 http://localhost:8082/api
- [ ] 可以登录系统

---

## 🎯 快速参考

### 首次启动
```cmd
ops\scripts\deploy.bat
```

### 重新部署
```cmd
ops\scripts\deploy.bat --clean
```

### 查看日志
```cmd
docker-compose logs -f
```

### 停止服务
```cmd
docker-compose down
```

### 访问地址
- 前端: http://localhost
- 后端: http://localhost:8082/api
- 数据库管理: http://localhost:8081

---

## 📚 更多信息

- 详细部署说明：查看 `DOCKER_DEPLOYMENT.md`
- 项目概览：查看 `PROJECT_OVERVIEW.md`
- 快速开始：查看 `DOCKER_QUICK_START.md`

---

**祝您使用愉快！** 🎉

