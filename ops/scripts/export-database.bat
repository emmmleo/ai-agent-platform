@echo off
REM ============================================
REM CodeHubix 数据库导出脚本 (Windows)
REM ============================================

echo ============================================
echo CodeHubix 数据库导出脚本
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
    echo [错误] MySQL 容器未运行，请先启动容器
    pause
    exit /b 1
)

REM 创建导出目录
if not exist "database-backup" mkdir database-backup

REM 生成文件名（带时间戳）
REM 使用 PowerShell 获取标准格式的时间戳（更可靠）
for /f "delims=" %%i in ('powershell -Command "Get-Date -Format 'yyyyMMdd_HHmmss'"') do set timestamp=%%i
set filename=database-backup\demo_db_backup_%timestamp%.sql

echo [信息] 正在导出数据库...
echo [目标文件] %filename%
echo.

REM 导出数据库（包含结构、数据、存储过程/事件，强制UTF-8编码）
docker exec codehubix-mysql mysqldump ^
    --default-character-set=utf8mb4 ^
    --set-charset ^
    --single-transaction ^
    --routines ^
    --events ^
    -u root -proot123456 demo_db > "%filename%"

if %errorlevel% equ 0 (
    echo.
    echo [成功] 数据库导出完成！
    echo [文件位置] %filename%
    echo.
    echo [提示] 可以将此文件复制到新电脑，使用 import-database.bat 导入
) else (
    echo.
    echo [错误] 数据库导出失败，请检查错误信息
)

echo.
pause

