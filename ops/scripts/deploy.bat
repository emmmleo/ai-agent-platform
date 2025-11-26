@echo off
chcp 65001 >nul 2>&1
REM CodeHubix Windows 部署脚本
REM 用途: 一键部署整个应用（数据库、后端、前端）

setlocal enabledelayedexpansion

echo ==========================================
echo CodeHubix 部署脚本 (Windows)
echo ==========================================
echo.

REM 检查Docker是否安装
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker 未安装，请先安装 Docker Desktop
    exit /b 1
)

where docker-compose >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose 未安装，请先安装 Docker Compose
    exit /b 1
)

echo [INFO] Docker 环境检查通过
echo.

REM 检查参数
set CLEAN_MODE=0
set NO_BUILD=0

if "%1"=="--clean" set CLEAN_MODE=1
if "%1"=="--no-build" set NO_BUILD=1
if "%1"=="--help" goto :help
if "%1"=="-h" goto :help

REM 清理旧容器
if %CLEAN_MODE%==1 (
    echo [INFO] 清理旧容器...
    docker-compose down -v 2>nul
    echo [INFO] 清理完成
    echo.
)

REM 构建镜像
if %NO_BUILD%==0 (
    echo [INFO] 开始构建镜像...
    echo [INFO] 注意: 首次构建可能需要几分钟，请耐心等待...
    echo [INFO] 提示: 如果镜像拉取失败，请配置Docker镜像加速器
    echo [INFO] 运行 setup-docker-mirror.bat 查看配置方法
    echo.
    
    REM 预先拉取所有所需的基础镜像，避免构建时拉取失败
    echo [INFO] 预先拉取基础镜像（如果不存在）...
    echo [INFO] 这可以避免构建过程中的网络问题...
    echo.
    
    echo [INFO] 拉取后端构建镜像: maven:3.9-eclipse-temurin-21...
    docker pull maven:3.9-eclipse-temurin-21 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] 拉取 maven:3.9-eclipse-temurin-21 失败，将在构建时重试
    )
    
    echo [INFO] 拉取后端运行镜像: eclipse-temurin:21-jre-alpine...
    docker pull eclipse-temurin:21-jre-alpine 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] 拉取 eclipse-temurin:21-jre-alpine 失败，将在构建时重试
    )
    
    echo [INFO] 拉取前端构建镜像: node:20-alpine...
    docker pull node:20-alpine 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] 拉取 node:20-alpine 失败，将在构建时重试
    )
    
    echo [INFO] 拉取前端运行镜像: nginx:alpine...
    docker pull nginx:alpine 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] 拉取 nginx:alpine 失败，将在构建时重试
    )
    
    echo [INFO] 拉取数据库镜像: mysql:8.0...
    docker pull mysql:8.0 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] 拉取 mysql:8.0 失败，将在启动时重试
    )
    
    echo [INFO] 拉取 phpMyAdmin 镜像: phpmyadmin/phpmyadmin...
    docker pull phpmyadmin/phpmyadmin 2>nul
    if %errorlevel% neq 0 (
        echo [WARN] 拉取 phpmyadmin/phpmyadmin 失败，将在启动时重试
    )
    
    echo [INFO] 基础镜像拉取完成（已存在的镜像会跳过）
    echo.
    
    echo [INFO] 构建后端镜像...
    docker-compose build backend
    if %errorlevel% neq 0 (
        echo [ERROR] 后端镜像构建失败
        echo.
        echo [INFO] 可能的原因和解决方案:
        echo   1. 网络问题 - 无法访问Docker Hub
        echo      解决方案: 配置Docker镜像加速器
        echo      运行: setup-docker-mirror.bat 查看配置方法
        echo.
        echo   2. Docker Desktop资源不足
        echo      解决方案: 增加Docker Desktop的内存和CPU分配
        echo.
        echo   3. 查看详细错误信息:
        echo      docker-compose build --progress=plain backend
        echo.
        echo   4. 尝试手动拉取镜像:
        echo      docker pull maven:3.9-eclipse-temurin-21
        echo      docker pull eclipse-temurin:21-jre-alpine
        echo.
        exit /b 1
    )
    
    echo [INFO] 构建前端镜像...
    docker-compose build frontend
    if %errorlevel% neq 0 (
        echo [ERROR] 前端镜像构建失败
        echo [INFO] 请检查:
        echo   1. 网络连接是否正常
        echo   2. 查看详细错误: docker-compose build frontend
        exit /b 1
    )
    
    echo [INFO] 镜像构建完成
    echo.
)

REM 启动服务
echo [INFO] 启动服务...
docker-compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] 服务启动失败
    echo [INFO] 请检查以下内容:
    echo   1. Docker Desktop 是否正在运行
    echo   2. 端口是否被占用: netstat -ano ^| findstr ":3306 :8081 :8082 :80"
    echo   3. 查看详细错误: docker-compose logs
    exit /b 1
)

echo [INFO] 等待服务启动...
echo.

REM 等待MySQL就绪
echo [INFO] 等待 MySQL 启动...
set TIMEOUT=60
set COUNTER=0
:wait_mysql
timeout /t 2 /nobreak >nul

REM 检查容器是否存在
docker ps --filter "name=codehubix-mysql" --format "{{.Names}}" | findstr /C:"codehubix-mysql" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] MySQL 容器未找到，请检查 docker-compose up 是否成功
    exit /b 1
)

docker exec codehubix-mysql mysqladmin ping -h localhost -u demo_user -pdemo_pass_123 --silent 2>nul
if %errorlevel% equ 0 (
    echo [INFO] MySQL 已就绪
    goto :mysql_ready
)
set /a COUNTER+=2
if %COUNTER% geq %TIMEOUT% (
    echo [ERROR] MySQL 启动超时，请检查日志: docker logs codehubix-mysql
    echo [INFO] 提示: 可以手动检查容器状态: docker-compose ps
    exit /b 1
)
goto :wait_mysql

:mysql_ready
echo.

REM 等待后端就绪
echo [INFO] 等待后端服务启动...
timeout /t 10 /nobreak >nul
set COUNTER=0
:wait_backend
timeout /t 3 /nobreak >nul

REM 检查curl是否存在，如果不存在使用PowerShell
where curl >nul 2>&1
if %errorlevel% equ 0 (
    curl -f http://localhost:8082/api >nul 2>&1
    if %errorlevel% equ 0 goto :backend_ready
    curl -f http://localhost:8082/api/auth/login >nul 2>&1
    if %errorlevel% equ 0 goto :backend_ready
) else (
    REM 使用PowerShell检查后端是否就绪
    powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8082/api' -Method Get -TimeoutSec 2 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1
    if %errorlevel% equ 0 goto :backend_ready
    powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8082/api/auth/login' -Method Get -TimeoutSec 2 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1
    if %errorlevel% equ 0 goto :backend_ready
)

set /a COUNTER+=3
if %COUNTER% geq 90 (
    echo [WARN] 后端服务启动可能较慢，请检查日志: docker logs codehubix-backend
    goto :backend_ready
)
goto :wait_backend

:backend_ready
echo [INFO] 后端服务已就绪
echo.

REM 等待前端就绪
echo [INFO] 等待前端服务启动...
timeout /t 5 /nobreak >nul
echo [INFO] 前端服务已就绪
echo.

REM 显示状态
echo ==========================================
echo 部署完成！服务访问地址：
echo ==========================================
echo 前端应用:    http://localhost
echo 后端API:     http://localhost:8082/api
echo phpMyAdmin:  http://localhost:8081
echo.
echo 默认登录账号：
echo   管理员: admin / 123456
echo   普通用户: user / 123456
echo.
echo 常用命令：
echo   查看日志: docker-compose logs -f [service_name]
echo   停止服务: docker-compose down
echo   重启服务: docker-compose restart [service_name]
echo   查看状态: docker-compose ps
echo.
goto :end

:help
echo 用法: deploy.bat [选项]
echo.
echo 选项:
echo   --clean      清理旧容器和数据卷后重新部署
echo   --no-build   跳过镜像构建（使用已有镜像）
echo   --help, -h   显示帮助信息
goto :end

:end
endlocal

