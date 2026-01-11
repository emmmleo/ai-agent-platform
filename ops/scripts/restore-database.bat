@echo off
REM ============================================
REM CodeHubix 数据库恢复脚本 (Windows)
REM ============================================

echo ============================================
echo CodeHubix 数据库恢复脚本
echo ============================================
echo.

REM 检查 Docker 是否运行
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)

REM 检查 MySQL 容器是否存在
docker ps -a | findstr "codehubix-mysql" >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] MySQL 容器不存在，请先运行 docker-compose up -d
    pause
    exit /b 1
)

REM 检查 MySQL 容器是否运行
docker ps | findstr "codehubix-mysql" >nul 2>&1
if %errorlevel% neq 0 (
    echo [信息] MySQL 容器未运行，正在启动...
    docker-compose up -d mysql
    echo [信息] 等待 MySQL 启动...
    timeout /t 10 /nobreak >nul
)

echo [信息] 正在执行数据库恢复...
echo.

REM 执行 SQL 脚本
docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db < backend\database_schema.sql

if %errorlevel% equ 0 (
    echo.
    echo [成功] 数据库表结构已恢复！
    echo.
    echo [信息] 正在验证表是否创建成功...
    docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db -e "SHOW TABLES;"
    echo.
    echo [完成] 数据库恢复完成！
    echo [提示] 请重启后端服务以初始化默认数据
    echo [命令] docker-compose restart backend
) else (
    echo.
    echo [错误] 数据库恢复失败，请检查错误信息
)

echo.
pause

