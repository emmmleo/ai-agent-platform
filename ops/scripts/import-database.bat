@echo off
REM ============================================
REM CodeHubix 数据库导入脚本 (Windows)
REM ============================================

echo ============================================
echo CodeHubix 数据库导入脚本
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

REM 检查备份文件
if not exist "database-backup" (
    echo [错误] database-backup 目录不存在
    echo [提示] 请将备份文件放在 database-backup 目录下
    pause
    exit /b 1
)

echo [信息] 可用的备份文件：
dir /b database-backup\*.sql 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到备份文件
    pause
    exit /b 1
)

echo.
set /p backup_file="请输入要导入的备份文件名（或直接回车使用最新的）: "

if "%backup_file%"=="" (
    REM 使用最新的备份文件
    for /f "delims=" %%i in ('dir /b /o-d database-backup\*.sql 2^>nul') do (
        set backup_file=%%i
        goto :found
    )
    echo [错误] 未找到备份文件
    pause
    exit /b 1
    :found
)

if not exist "database-backup\%backup_file%" (
    echo [错误] 文件不存在: database-backup\%backup_file%
    pause
    exit /b 1
)

echo.
echo [警告] 导入数据将覆盖现有数据库！
echo [备份文件] %backup_file%
set /p confirm="确认导入？(y/N): "
if /i not "%confirm%"=="y" (
    echo [取消] 操作已取消
    pause
    exit /b 0
)

echo.
echo [信息] 正在导入数据库...
echo.

REM 导入数据库
docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db < "database-backup\%backup_file%"

if %errorlevel% equ 0 (
    echo.
    echo [成功] 数据库导入完成！
    echo.
    echo [提示] 请重启后端服务
    echo [命令] docker-compose restart backend
) else (
    echo.
    echo [错误] 数据库导入失败，请检查错误信息
)

echo.
pause

