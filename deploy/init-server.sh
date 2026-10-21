#!/bin/bash
# 服务器一键初始化脚本。在华为云服务器上执行：
#   curl -fsSL https://raw.githubusercontent.com/<your-repo>/master/deploy/init-server.sh | bash
# 或者先上传再执行：bash deploy/init-server.sh
#
# 支持：Ubuntu 22.04 / CentOS 8+
# 执行完成后：
#   1. Docker 和 Docker Compose 已安装
#   2. 防火墙已开放 8080 端口
#   3. 项目已部署并启动
#   4. 访问 http://<服务器IP>:8080/admin/ 进入后台

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ---------- 1. 检测系统 ----------
log_info "检测操作系统..."
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
    VERSION=$VERSION_ID
else
    log_error "无法检测操作系统"
    exit 1
fi
log_info "系统: $OS $VERSION"

# ---------- 2. 安装 Docker ----------
if command -v docker &> /dev/null; then
    log_info "Docker 已安装: $(docker --version)"
else
    log_info "安装 Docker..."
    if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        apt-get update
        apt-get install -y ca-certificates curl gnupg
        install -m 0755 -d /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/$OS/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
        chmod a+r /etc/apt/keyrings/docker.gpg
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/$OS $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
        apt-get update
        apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ]; then
        yum install -y yum-utils
        yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
        yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
        systemctl enable docker
        systemctl start docker
    else
        log_error "不支持的系统: $OS"
        exit 1
    fi
    log_info "Docker 安装完成: $(docker --version)"
fi

# 配置 Docker 镜像加速（华为云）
log_info "配置 Docker 镜像加速..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "50m",
    "max-file": "3"
  }
}
EOF
systemctl daemon-reload
systemctl restart docker

# ---------- 3. 配置防火墙 ----------
log_info "配置防火墙..."
if command -v ufw &> /dev/null; then
    ufw allow 8080/tcp
    ufw allow 22/tcp
    ufw --force enable
    log_info "UFW 已开放 8080 端口"
elif command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=8080/tcp
    firewall-cmd --reload
    log_info "firewalld 已开放 8080 端口"
else
    log_warn "未检测到防火墙，请手动在华为云安全组中开放 8080 端口"
fi

# ---------- 4. 配置 Swap（如果还没有） ----------
if [ ! -f /swapfile ]; then
    log_info "配置 2G Swap..."
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    # 调优 swap 使用策略：尽量用物理内存
    sysctl vm.swappiness=10
    echo 'vm.swappiness=10' >> /etc/sysctl.conf
    log_info "Swap 配置完成"
else
    log_info "Swap 已存在"
fi

# ---------- 5. 生成 .env ----------
log_info "生成环境变量文件..."
DB_ROOT_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 24)
cat > .env <<EOF
DB_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
EOF
log_info "数据库密码已生成（保存在 .env 中）"

# ---------- 6. 启动服务 ----------
log_info "构建并启动服务..."
docker compose -f docker-compose.prod.yml up -d --build

# ---------- 7. 等待健康检查 ----------
log_info "等待服务启动..."
for i in $(seq 1 60); do
    if curl -sf http://localhost:8080/admin-api/health > /dev/null 2>&1; then
        log_info "服务已启动！"
        break
    fi
    if [ $i -eq 60 ]; then
        log_error "服务启动超时，查看日志：docker compose -f docker-compose.prod.yml logs app"
        exit 1
    fi
    sleep 5
done

# ---------- 8. 输出信息 ----------
SERVER_IP=$(curl -sf http://ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')
echo ""
echo "=========================================="
echo -e "${GREEN}  部署完成！${NC}"
echo "=========================================="
echo ""
echo "  后台管理：http://${SERVER_IP}:8080/admin/"
echo "  健康检查：http://${SERVER_IP}:8080/admin-api/health"
echo ""
echo "  数据库密码：查看 .env 文件"
echo ""
echo "  常用命令："
echo "    查看日志：docker compose -f docker-compose.prod.yml logs -f app"
echo "    重启服务：docker compose -f docker-compose.prod.yml restart"
echo "    停止服务：docker compose -f docker-compose.prod.yml down"
echo "    更新代码：git pull && docker compose -f docker-compose.prod.yml up -d --build"
echo ""
echo "=========================================="
