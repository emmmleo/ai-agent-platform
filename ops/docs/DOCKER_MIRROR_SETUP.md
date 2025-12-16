# Docker 镜像加速配置指南

## 🔍 问题诊断

如果你遇到以下错误：
```
ERROR [internal] load metadata for docker.io/library/eclipse-temurin:21-jre-alpine
ERROR [internal] load metadata for docker.io/library/maven:3.9-eclipse-temurin-21
```

这通常是因为：
1. **网络问题**：无法访问 Docker Hub（国内用户常见）
2. **镜像拉取超时**：网络速度慢导致超时
3. **Docker Hub限流**：未登录用户有拉取限制

## 🚀 解决方案

### 方案1：配置Docker镜像加速器（推荐）

#### Windows Docker Desktop

1. **打开Docker Desktop设置**
   - 右键点击系统托盘中的Docker图标
   - 选择 "Settings" 或 "设置"

2. **进入Docker Engine配置**
   - 点击左侧菜单 "Docker Engine"
   - 在JSON配置中添加镜像加速器

3. **添加镜像加速器配置**

   在JSON配置中添加以下内容：

   ```json
   {
     "registry-mirrors": [
       "https://docker.mirrors.ustc.edu.cn",
       "https://hub-mirror.c.163.com",
       "https://mirror.baidubce.com"
     ]
   }
   ```

   完整配置示例：
   ```json
   {
     "builder": {
       "gc": {
         "defaultKeepStorage": "20GB",
         "enabled": true
       }
     },
     "experimental": false,
     "registry-mirrors": [
       "https://docker.mirrors.ustc.edu.cn",
       "https://hub-mirror.c.163.com",
       "https://mirror.baidubce.com"
     ]
   }
   ```

4. **应用并重启**
   - 点击 "Apply & Restart"
   - 等待Docker重启完成

5. **验证配置**
   ```cmd
   docker info | findstr "Registry Mirrors"
   ```

#### 国内常用镜像加速器

| 镜像加速器 | 地址 | 说明 |
|-----------|------|------|
| 中科大镜像 | https://docker.mirrors.ustc.edu.cn | 推荐 |
| 网易镜像 | https://hub-mirror.c.163.com | 稳定 |
| 百度云镜像 | https://mirror.baidubce.com | 速度快 |
| 阿里云镜像 | 需要登录获取 | 个人专属 |

#### 阿里云镜像加速器（推荐）

1. **登录阿里云**
   - 访问：https://cr.console.aliyun.com/
   - 登录你的阿里云账号

2. **获取专属加速地址**
   - 进入 "容器镜像服务" → "镜像加速器"
   - 复制你的专属加速地址（格式：`https://xxxxx.mirror.aliyuncs.com`）

3. **添加到Docker配置**
   - 将获取的地址添加到 `registry-mirrors` 数组中

### 方案2：手动拉取镜像

如果配置镜像加速器后仍有问题，可以手动拉取镜像：

```cmd
# 拉取Maven镜像
docker pull maven:3.9-eclipse-temurin-21

# 拉取Java运行环境镜像
docker pull eclipse-temurin:21-jre-alpine

# 拉取Node.js镜像
docker pull node:20-alpine

# 拉取Nginx镜像
docker pull nginx:alpine

# 拉取MySQL镜像
docker pull mysql:8.0
```

### 方案3：使用国内镜像源构建

如果上述方法都不行，可以修改Dockerfile使用国内镜像：

#### 修改 backend/Dockerfile

```dockerfile
# 使用国内镜像源（如果需要）
# FROM registry.cn-hangzhou.aliyuncs.com/acs/maven:3.9-eclipse-temurin-21 AS build
FROM maven:3.9-eclipse-temurin-21 AS build

# ... 其他内容保持不变 ...

# 使用国内镜像源（如果需要）
# FROM registry.cn-hangzhou.aliyuncs.com/acs/eclipse-temurin:21-jre-alpine
FROM eclipse-temurin:21-jre-alpine
```

**注意**：不推荐修改Dockerfile，优先使用镜像加速器方案。

## 🔧 其他网络问题解决

### 检查网络连接

```cmd
# 测试Docker Hub连接
ping registry-1.docker.io

# 测试DNS解析
nslookup registry-1.docker.io
```

### 配置代理（如果需要）

如果你使用代理，需要在Docker Desktop中配置：

1. **打开Docker Desktop设置**
2. **进入 "Resources" → "Proxies"**
3. **配置代理服务器**
   - Manual proxy configuration
   - 输入代理地址和端口

### 增加超时时间

如果网络较慢，可以增加Docker的超时时间（需要修改Docker daemon配置）。

## ✅ 验证配置是否生效

### 方法1：查看Docker信息

```cmd
docker info
```

查找 "Registry Mirrors" 部分，应该显示你配置的镜像地址。

### 方法2：测试拉取镜像

```cmd
# 拉取一个小镜像测试
docker pull hello-world

# 如果成功，说明配置生效
```

### 方法3：查看拉取日志

```cmd
# 构建时查看详细日志
docker-compose build --progress=plain backend
```

## 🎯 快速配置脚本

创建一个快速配置脚本 `setup-docker-mirror.bat`：

```batch
@echo off
echo 配置Docker镜像加速器...
echo.
echo 请按照以下步骤操作：
echo 1. 打开Docker Desktop
echo 2. 进入 Settings -^> Docker Engine
echo 3. 在JSON配置中添加以下内容：
echo.
echo {
echo   "registry-mirrors": [
echo     "https://docker.mirrors.ustc.edu.cn",
echo     "https://hub-mirror.c.163.com",
echo     "https://mirror.baidubce.com"
echo   ]
echo }
echo.
echo 4. 点击 Apply ^& Restart
echo.
pause
```

## 📝 常见问题

### Q1: 配置后仍然很慢？

**A:** 
- 尝试更换其他镜像加速器
- 检查网络连接
- 考虑使用VPN或代理

### Q2: 如何知道使用了哪个镜像？

**A:** 
```cmd
docker info | findstr "Registry Mirrors"
```

### Q3: 可以配置多个镜像吗？

**A:** 可以，Docker会按顺序尝试，第一个失败会尝试下一个。

### Q4: 配置后需要重启Docker吗？

**A:** 是的，配置后必须重启Docker Desktop才能生效。

## 🚀 配置完成后

配置完成后，重新运行部署脚本：

```cmd
ops\scripts\deploy.bat
```

应该可以正常拉取镜像了！

---

**提示**：如果仍然有问题，可以：
1. 查看详细错误日志：`docker-compose build --progress=plain backend`
2. 尝试手动拉取镜像：`docker pull maven:3.9-eclipse-temurin-21`
3. 检查Docker Desktop日志

