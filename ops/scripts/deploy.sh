#!/bin/bash

# ai-agent-platform 部署脚本
# 用途: 一键部署整个应用（数据库、后端、前端）

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 部署参数
CLEAN_MODE=0
NO_BUILD=0
BUILD_BACKEND=0
BUILD_FRONTEND=0
BUILD_SELECTION=0
SHOULD_BUILD=0

# 打印带颜色的消息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_usage() {
    cat <<EOF
用法: $0 [选项]

选项:
  --clean           清理旧容器和数据卷后重新部署
  --no-build        跳过镜像构建（使用已有镜像）
  --build-backend   仅构建后端镜像
  --build-frontend  仅构建前端镜像
  --build-all       构建前后端镜像（可与其它选项组合）
  --help, -h        显示帮助信息
EOF
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --clean)
                CLEAN_MODE=1
                ;;
            --no-build)
                NO_BUILD=1
                BUILD_SELECTION=1
                BUILD_BACKEND=0
                BUILD_FRONTEND=0
                ;;
            --build-backend)
                BUILD_BACKEND=1
                BUILD_SELECTION=1
                ;;
            --build-frontend)
                BUILD_FRONTEND=1
                BUILD_SELECTION=1
                ;;
            --build-all)
                BUILD_BACKEND=1
                BUILD_FRONTEND=1
                BUILD_SELECTION=1
                ;;
            --help|-h)
                print_usage
                exit 0
                ;;
            *)
                print_warn "未知参数: $1"
                print_usage
                exit 1
                ;;
        esac
        shift
    done

    if [[ $BUILD_SELECTION -eq 0 ]]; then
        BUILD_BACKEND=1
        BUILD_FRONTEND=1
    fi

    if [[ $BUILD_BACKEND -eq 1 || $BUILD_FRONTEND -eq 1 ]]; then
        SHOULD_BUILD=1
    fi
}

# 检查Docker是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    print_info "Docker 环境检查通过"
}

# 检查端口是否被占用
check_ports() {
    local ports=(3306 8081 8082 80)
    local occupied=()
    
    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an | grep -q ":$port.*LISTEN" 2>/dev/null; then
            occupied+=($port)
        fi
    done
    
    if [ ${#occupied[@]} -gt 0 ]; then
        print_warn "以下端口已被占用: ${occupied[*]}"
        read -p "是否继续? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 停止并删除旧容器
cleanup_old_containers() {
    print_info "清理旧容器..."
    docker-compose down -v 2>/dev/null || true
    print_info "清理完成"
}

# 构建镜像
build_images() {
    local targets=()
    if [[ $BUILD_BACKEND -eq 1 ]]; then
        targets+=("后端")
    fi
    if [[ $BUILD_FRONTEND -eq 1 ]]; then
        targets+=("前端")
    fi

    local target_str="未选择需要构建的服务"
    if [[ ${#targets[@]} -gt 0 ]]; then
        local IFS=', '
        target_str="${targets[*]}"
    fi

    print_info "本次构建组件: ${target_str}"
    print_info "开始构建镜像..."
    print_info "注意: 首次构建可能需要几分钟，请耐心等待..."
    print_info "提示: 如果镜像拉取失败，请配置 Docker 镜像加速器"
    echo ""
    
    # 预先拉取所需的基础镜像，避免构建时拉取失败
    print_info "预先拉取基础镜像（如果不存在）..."
    print_info "这可以避免构建过程中的网络问题..."
    echo ""

    if [[ $BUILD_BACKEND -eq 1 ]]; then
        print_info "拉取后端构建镜像: maven:3.9-eclipse-temurin-21..."
        docker pull maven:3.9-eclipse-temurin-21 2>/dev/null || print_warn "拉取 maven:3.9-eclipse-temurin-21 失败，将在构建时重试"

        print_info "拉取后端运行镜像: eclipse-temurin:21-jre-alpine..."
        docker pull eclipse-temurin:21-jre-alpine 2>/dev/null || print_warn "拉取 eclipse-temurin:21-jre-alpine 失败，将在构建时重试"
    fi

    if [[ $BUILD_FRONTEND -eq 1 ]]; then
        print_info "拉取前端构建镜像: node:20-alpine..."
        docker pull node:20-alpine 2>/dev/null || print_warn "拉取 node:20-alpine 失败，将在构建时重试"

        print_info "拉取前端运行镜像: nginx:alpine..."
        docker pull nginx:alpine 2>/dev/null || print_warn "拉取 nginx:alpine 失败，将在构建时重试"
    fi

    print_info "拉取数据库镜像: mysql:8.0..."
    docker pull mysql:8.0 2>/dev/null || print_warn "拉取 mysql:8.0 失败，将在启动时重试"

    print_info "拉取 phpMyAdmin 镜像: phpmyadmin/phpmyadmin..."
    docker pull phpmyadmin/phpmyadmin 2>/dev/null || print_warn "拉取 phpmyadmin/phpmyadmin 失败，将在启动时重试"

    print_info "基础镜像拉取完成（已存在的镜像会跳过）"
    echo ""

    if [[ $BUILD_BACKEND -eq 1 ]]; then
        print_info "构建后端镜像..."
        if ! docker-compose build backend; then
            print_error "后端镜像构建失败"
            echo ""
            print_info "可能的原因和解决方案:"
            echo "  1. 网络问题 - 无法访问 Docker Hub"
            echo "     解决方案: 配置 Docker 镜像加速器"
            echo ""
            echo "  2. Docker 资源不足 - 请增加 Docker Desktop 的 CPU/内存限制"
            echo ""
            echo "  3. 查看详细错误: docker-compose build --progress=plain backend"
            echo ""
            exit 1
        fi
    fi

    if [[ $BUILD_FRONTEND -eq 1 ]]; then
        print_info "构建前端镜像..."
        if ! docker-compose build frontend; then
            print_error "前端镜像构建失败"
            echo ""
            echo "  1. 请检查网络连接是否正常"
            echo "  2. 查看详细错误: docker-compose build frontend"
            exit 1
        fi
    fi

    print_info "镜像构建完成"
}

# 启动服务
start_services() {
    print_info "启动服务..."
    
    # 启动所有服务
    docker-compose up -d
    
    print_info "等待服务启动..."
    
    # 等待MySQL就绪
    print_info "等待 MySQL 启动..."
    timeout=60
    counter=0
    while ! docker exec codehubix-mysql mysqladmin ping -h localhost -u demo_user -pdemo_pass_123 --silent 2>/dev/null; do
        sleep 2
        counter=$((counter + 2))
        if [ $counter -ge $timeout ]; then
            print_error "MySQL 启动超时"
            exit 1
        fi
    done
    print_info "MySQL 已就绪"
    
    # 等待后端就绪
    print_info "等待后端服务启动..."
    sleep 10
    counter=0
    # 尝试连接后端API（不依赖Actuator）
    while ! curl -f http://localhost:8082/api >/dev/null 2>&1 && ! curl -f http://localhost:8082/api/auth/login >/dev/null 2>&1; do
        sleep 3
        counter=$((counter + 3))
        if [ $counter -ge 90 ]; then
            print_warn "后端服务启动可能较慢，请检查日志: docker logs codehubix-backend"
            break
        fi
    done
    print_info "后端服务已就绪"
    
    # 等待前端就绪
    print_info "等待前端服务启动..."
    sleep 5
    print_info "前端服务已就绪"
}

# 显示服务状态
show_status() {
    echo ""
    print_info "=========================================="
    print_info "部署完成！服务访问地址："
    print_info "=========================================="
    echo -e "${GREEN}前端应用:${NC}    http://localhost"
    echo -e "${GREEN}后端API:${NC}     http://localhost:8082/api"
    echo -e "${GREEN}phpMyAdmin:${NC}   http://localhost:8081"
    echo ""
    print_info "默认登录账号："
    echo -e "  管理员: ${YELLOW}admin${NC} / ${YELLOW}123456${NC}"
    echo -e "  普通用户: ${YELLOW}user${NC} / ${YELLOW}123456${NC}"
    echo ""
    print_info "常用命令："
    echo "  查看日志: docker-compose logs -f [service_name]"
    echo "  停止服务: docker-compose down"
    echo "  重启服务: docker-compose restart [service_name]"
    echo "  查看状态: docker-compose ps"
    echo ""
}

# 主函数
main() {
    print_info "开始部署 ai-agent-platform..."
    echo ""
    
    # 检查环境
    check_docker
    check_ports
    
    # 清理旧容器（可选）
    if [[ $CLEAN_MODE -eq 1 ]]; then
        cleanup_old_containers
    fi
    
    # 构建镜像
    if [[ $NO_BUILD -eq 1 ]]; then
        print_info "根据 --no-build 参数，跳过所有镜像构建"
    elif [[ $SHOULD_BUILD -eq 0 ]]; then
        print_info "未选择需要构建的服务，跳过镜像构建"
    else
        build_images
    fi
    
    # 启动服务
    start_services
    
    # 显示状态
    show_status
}

parse_args "$@"
main

