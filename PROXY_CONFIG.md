# Docker 构建代理配置说明

## 📋 概述

默认情况下，Dockerfile 和 docker-compose.yml **不包含任何代理配置**，可以在任何网络环境下使用。

如果您的网络环境需要代理才能访问 Docker Hub（例如企业内网或需要科学上网），可以按照以下方式配置。

## 🔧 配置方法
#### 下面的ip与端口换成你的代理ip和端口，比如例子：http://10.7.47.36:7890

### 方法一：使用环境变量（推荐）

**Windows (PowerShell):**
```powershell
$env:HTTP_PROXY="http://10.7.47.36:7890"
$env:HTTPS_PROXY="http://10.7.47.36:7890"
docker compose build
```

**Windows (CMD):**
```cmd
set HTTP_PROXY=http://10.7.47.36:7890
set HTTPS_PROXY=http://10.7.47.36:7890
docker compose build
```

**Linux/Mac:**
```bash
export HTTP_PROXY=http://10.7.47.36:7890
export HTTPS_PROXY=http://10.7.47.36:7890
docker compose build
```

### 方法二：修改 docker-compose.yml

1. **编辑 `docker-compose.yml`**

   找到 `backend` 和 `frontend` 服务的 `build` 部分，取消注释并配置：

   ```yaml
   backend:
     build:
       context: ./backend
       dockerfile: Dockerfile
       args:
         HTTP_PROXY: http://10.7.47.36:7890  # 修改为您的代理地址
         HTTPS_PROXY: http://10.7.47.36:7890
   ```

2. **运行构建**
   ```bash
   docker compose build
   ```

### 方法三：使用 .env 文件

1. **创建 `.env` 文件**（项目根目录）

   ```env
   HTTP_PROXY=http://10.7.47.36:7890
   HTTPS_PROXY=http://10.7.47.36:7890
   ```

2. **修改 `docker-compose.yml`**

   ```yaml
   backend:
     build:
       context: ./backend
       dockerfile: Dockerfile
       args:
         HTTP_PROXY: ${HTTP_PROXY:-}
         HTTPS_PROXY: ${HTTPS_PROXY:-}
   ```

3. **运行构建**
   ```bash
   docker compose build
   ```

## ⚠️ 注意事项

1. **代理地址格式**: `http://host:port` 或 `https://host:port`
2. **本地代理**: 如果使用本地代理（如 Clash、V2Ray），通常地址是 `http://127.0.0.1:7890`
3. **企业代理**: 请咨询网络管理员获取正确的代理地址
4. **不需要代理**: 如果您的网络可以直接访问 Docker Hub，**无需任何配置**，直接使用即可

## 🔍 验证代理是否生效

构建时查看日志，如果看到从代理服务器下载镜像，说明代理配置成功。

## 📚 相关文档

- [Docker 镜像加速配置](ops/docs/DOCKER_MIRROR_SETUP.md) - 如果代理不可用，可以配置镜像加速器
- [故障排查](TROUBLESHOOTING.md) - 其他网络相关问题

