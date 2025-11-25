#!/bin/bash
# ============================================
# CodeHubix 数据库导入脚本 (Linux/Mac)
# ============================================

echo "============================================"
echo "CodeHubix 数据库导入脚本"
echo "============================================"
echo ""

# 检查 Docker 是否运行
if ! docker ps >/dev/null 2>&1; then
    echo "[错误] Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查 MySQL 容器是否存在
if ! docker ps -a | grep -q "codehubix-mysql"; then
    echo "[错误] MySQL 容器不存在，请先运行 docker-compose up -d"
    exit 1
fi

# 检查 MySQL 容器是否运行
if ! docker ps | grep -q "codehubix-mysql"; then
    echo "[信息] MySQL 容器未运行，正在启动..."
    docker-compose up -d mysql
    echo "[信息] 等待 MySQL 启动..."
    sleep 10
fi

# 检查备份文件
if [ ! -d "database-backup" ]; then
    echo "[错误] database-backup 目录不存在"
    echo "[提示] 请将备份文件放在 database-backup 目录下"
    exit 1
fi

echo "[信息] 可用的备份文件："
ls -1 database-backup/*.sql 2>/dev/null
if [ $? -ne 0 ]; then
    echo "[错误] 未找到备份文件"
    exit 1
fi

echo ""
read -p "请输入要导入的备份文件名（或直接回车使用最新的）: " backup_file

if [ -z "$backup_file" ]; then
    # 使用最新的备份文件
    backup_file=$(ls -t database-backup/*.sql | head -1)
    backup_file=$(basename "$backup_file")
fi

if [ ! -f "database-backup/$backup_file" ]; then
    echo "[错误] 文件不存在: database-backup/$backup_file"
    exit 1
fi

echo ""
echo "[警告] 导入数据将覆盖现有数据库！"
echo "[备份文件] $backup_file"
read -p "确认导入？(y/N): " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "[取消] 操作已取消"
    exit 0
fi

echo ""
echo "[信息] 正在导入数据库..."
echo ""

# 导入数据库
docker exec -i codehubix-mysql mysql -u demo_user -pdemo_pass_123 demo_db < "database-backup/$backup_file"

if [ $? -eq 0 ]; then
    echo ""
    echo "[成功] 数据库导入完成！"
    echo ""
    echo "[提示] 请重启后端服务"
    echo "[命令] docker-compose restart backend"
else
    echo ""
    echo "[错误] 数据库导入失败，请检查错误信息"
    exit 1
fi

