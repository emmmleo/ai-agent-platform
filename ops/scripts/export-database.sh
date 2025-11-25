#!/bin/bash
# ============================================
# CodeHubix 数据库导出脚本 (Linux/Mac)
# ============================================

echo "============================================"
echo "CodeHubix 数据库导出脚本"
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
    echo "[错误] MySQL 容器未运行，请先启动容器"
    exit 1
fi

# 创建导出目录
mkdir -p database-backup

# 生成文件名（带时间戳）
filename="database-backup/demo_db_backup_$(date +%Y%m%d_%H%M%S).sql"

echo "[信息] 正在导出数据库..."
echo "[目标文件] $filename"
echo ""

# 导出数据库（包含结构、数据、存储过程/事件，强制UTF-8编码）
docker exec codehubix-mysql mysqldump \
    --default-character-set=utf8mb4 \
    --set-charset \
    --single-transaction \
    --routines \
    --events \
    -u root -proot123456 demo_db > "$filename"

if [ $? -eq 0 ]; then
    echo ""
    echo "[成功] 数据库导出完成！"
    echo "[文件位置] $filename"
    echo ""
    echo "[提示] 可以将此文件复制到新电脑，使用 import-database.sh 导入"
else
    echo ""
    echo "[错误] 数据库导出失败，请检查错误信息"
    exit 1
fi

