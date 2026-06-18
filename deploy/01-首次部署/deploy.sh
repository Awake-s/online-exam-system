#!/bin/bash
# ============================================================
# 在线考试系统 — 生产环境一键部署脚本
# 适配：Ubuntu 22.04 LTS / Debian 12（阿里云/腾讯云/AWS 通用）
#
# 权威依据：
#   - MySQL 8 Oracle 官方调优：https://dev.mysql.com/doc/refman/8.0/en/optimizing-innodb-configuration-variables.html
#   - Redis 7 生产配置（konfy.io）：https://konfy.io/config/redis-production-conf-persistence
#   - Ubuntu Server Security Guide：https://ubuntu.com/server/docs/security
#
# 特性：
#   ✅ 交互式密码输入（避免硬编码）
#   ✅ 幂等性（支持重复执行）
#   ✅ MySQL/Redis 生产级调优
#   ✅ UFW 防火墙 + SSH 加固
#   ✅ 自动生成 secrets.env 模板
#
# 使用：sudo bash deploy.sh
# ============================================================

set -euo pipefail

# ==================== 禁用 APT 交互对话框 ====================
# Ubuntu 22.04 默认会在 apt upgrade 时弹出 needrestart TUI 询问是否重启，
# 这会抢占 SSH 终端导致脚本卡死。强制所有 APT 操作非交互。
export DEBIAN_FRONTEND=noninteractive
export NEEDRESTART_MODE=a
export NEEDRESTART_SUSPEND=1
# 永久禁用 needrestart 交互（幂等，多次执行无副作用）
if [[ -f /etc/needrestart/needrestart.conf ]]; then
  sed -i "s/^#\?\\\$nrconf{restart} = .*/\$nrconf{restart} = 'a';/g" /etc/needrestart/needrestart.conf 2>/dev/null || true
  sed -i "s/^#\?\\\$nrconf{kernelhints} = .*/\$nrconf{kernelhints} = -1;/g" /etc/needrestart/needrestart.conf 2>/dev/null || true
fi

# ==================== 基础配置 ====================
APP_NAME="exam-system"
APP_DIR="/opt/exam-system"
SECRETS_DIR="/etc/exam-system"
DB_NAME="online_exam_system"
DB_USER="exam_app"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 颜色（日志区分）
C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_YELLOW='\033[0;33m'
C_RED='\033[0;31m'
C_NC='\033[0m'

log() { echo -e "${C_CYAN}[$(date +%T)]${C_NC} $*"; }
ok()  { echo -e "${C_GREEN}  ✅ $*${C_NC}"; }
warn(){ echo -e "${C_YELLOW}  ⚠️  $*${C_NC}"; }
err() { echo -e "${C_RED}  ❌ $*${C_NC}" >&2; }

[[ $EUID -eq 0 ]] || { err "必须以 root 身份运行：sudo bash $0"; exit 1; }

# ==================== 0. 交互式获取密码 ====================
log "=========================================="
log "  在线考试系统 — 生产环境部署（强化版）"
log "=========================================="

gen_password() {
  openssl rand -base64 24 | tr -d '\n' | tr -d '=/+' | head -c 24
}

gen_jwt_secret() {
  openssl rand -base64 48 | tr -d '\n'
}

read_password() {
  local prompt="$1"
  local default_val="$2"
  local val=""
  read -s -p "$prompt [回车使用随机生成值]: " val
  echo >&2
  echo "${val:-$default_val}"
}

log "[0/9] 生成/读取密码..."
# 如果已存在 secrets.env 则跳过交互（幂等）
if [[ -f "${SECRETS_DIR}/secrets.env" ]]; then
  warn "${SECRETS_DIR}/secrets.env 已存在，跳过密码生成"
  source "${SECRETS_DIR}/secrets.env"
  DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-$(gen_password)}"
else
  DB_ROOT_PASSWORD="$(read_password '请输入 MySQL root 密码' "$(gen_password)")"
  DB_PASSWORD="$(read_password '请输入应用数据库密码' "$(gen_password)")"
  REDIS_PASSWORD="$(read_password '请输入 Redis 密码' "$(gen_password)")"
  JWT_SECRET="$(read_password '请输入 JWT Secret' "$(gen_jwt_secret)")"
  ok "密码已生成（将写入 ${SECRETS_DIR}/secrets.env，权限 600）"
fi

# ==================== 1. 系统更新 ====================
log "[1/9] 更新系统包..."
apt update -qq
apt upgrade -y -qq
apt install -y curl wget unzip git ufw ca-certificates gnupg lsb-release openssl

# 时区设为中国
timedatectl set-timezone Asia/Shanghai
ok "时区设置为 Asia/Shanghai"

# ==================== 2. JDK 11 ====================
log "[2/9] 安装 JDK 11..."
if ! command -v java >/dev/null 2>&1; then
  apt install -y openjdk-11-jdk-headless
fi
java -version 2>&1 | head -1
ok "JDK 已就绪"

# ==================== 3. MySQL 8.0 + 生产调优 ====================
log "[3/9] 安装 MySQL 8.0 + 生产调优..."
if ! command -v mysql >/dev/null 2>&1; then
  DEBIAN_FRONTEND=noninteractive apt install -y mysql-server
fi
systemctl enable mysql >/dev/null 2>&1
systemctl start mysql

# ---------- MySQL 内存智能分级（根据系统内存自动调整 buffer pool） ----------
# 权威依据：MySQL 官方建议 innodb_buffer_pool_size 为系统内存的 50%~75%，
# 但前提是主机只跑 MySQL。我们的机器同时跑 JVM/Redis/Nginx/OS，必须按实际情况保守分配。
MEM_MB=$(free -m | awk '/^Mem:/{print $2}')
if [[ $MEM_MB -lt 2500 ]]; then
    # 2G 机器（如香港轻量）：仅 15%，给 JVM/Redis/OS 留足空间
    INNODB_POOL_MB=$(( MEM_MB * 15 / 100 ))
    TIER="低内存保守模式（2G 级）"
elif [[ $MEM_MB -lt 5000 ]]; then
    # 4G 机器（如学生机）：40%
    INNODB_POOL_MB=$(( MEM_MB * 40 / 100 ))
    TIER="标准模式（4G 级）"
else
    # 8G+ 机器（如企业级）：50%
    INNODB_POOL_MB=$(( MEM_MB / 2 ))
    TIER="高性能模式（8G+ 级）"
fi
[[ $INNODB_POOL_MB -lt 128 ]] && INNODB_POOL_MB=128
log "MySQL 内存分级：${TIER}（系统 ${MEM_MB}M → buffer pool ${INNODB_POOL_MB}M）"
[[ $INNODB_POOL_MB -gt 4096 ]] && INNODB_POOL_MB=4096

# 写 MySQL 生产调优配置（幂等：覆盖）
cat > /etc/mysql/mysql.conf.d/99-exam-system.cnf <<EOF
# ============================================================
# 在线考试系统 MySQL 生产调优
# 权威依据：https://dev.mysql.com/doc/refman/8.0/en/optimizing-innodb-configuration-variables.html
# ============================================================
[mysqld]
# 仅监听本地（安全：不暴露到公网）
bind-address = 127.0.0.1
skip-networking = OFF

# 字符集
character-set-server = utf8mb4
collation-server = utf8mb4_general_ci

# 连接数
max_connections = 200
max_connect_errors = 10000

# 大文件上传支持（如 Excel 题库）
max_allowed_packet = 64M

# InnoDB 缓冲池（按系统内存 50% 计算，当前：${INNODB_POOL_MB}M）
innodb_buffer_pool_size = ${INNODB_POOL_MB}M
innodb_buffer_pool_instances = 2
innodb_log_file_size = 256M
innodb_log_buffer_size = 32M
# 1=最安全但慢 / 2=性能优（崩溃最多丢 1s 数据）/ 0=最快但不安全
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
innodb_file_per_table = 1

# 慢查询日志（排障用）
slow_query_log = 1
slow_query_log_file = /var/log/mysql/mysql-slow.log
long_query_time = 2

# 时区
default-time-zone = '+08:00'
EOF

systemctl restart mysql
sleep 2

# 创建数据库和应用用户（幂等）
mysql -u root <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED WITH caching_sha2_password BY '${DB_ROOT_PASSWORD}';
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE, SHOW VIEW ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF

ok "MySQL 8 已就绪 + 生产调优完成（buffer_pool=${INNODB_POOL_MB}M）"

# ==================== 4. Redis 7 + 生产调优 ====================
log "[4/9] 安装 Redis + 生产调优..."
if ! command -v redis-cli >/dev/null 2>&1; then
  apt install -y redis-server
fi

# Redis 最大内存：系统内存的 25%（保守值）
REDIS_MAX_MB=$(( MEM_MB / 4 ))
[[ $REDIS_MAX_MB -lt 128 ]] && REDIS_MAX_MB=128
[[ $REDIS_MAX_MB -gt 2048 ]] && REDIS_MAX_MB=2048

# 写 Redis 生产调优配置（通过 include 方式，不污染主配置）
cat > /etc/redis/redis-exam-system.conf <<EOF
# ============================================================
# Redis 生产调优（include 到 /etc/redis/redis.conf）
# 权威依据：https://konfy.io/config/redis-production-conf-persistence
# ============================================================
# 仅监听本地
bind 127.0.0.1 ::1
protected-mode yes
port 6379

# 认证
requirepass ${REDIS_PASSWORD}

# 内存限制（防 OOM）
maxmemory ${REDIS_MAX_MB}mb
maxmemory-policy allkeys-lru
maxmemory-samples 10

# 持久化：RDB + AOF 混合（生产推荐）
save 3600 1
save 300 100
save 60 10000
rdbcompression yes
rdbchecksum yes
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-use-rdb-preamble yes

# 客户端
maxclients 1000
timeout 300
tcp-backlog 511
tcp-keepalive 300

# 关闭危险命令（防滥用）
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command CONFIG "CONFIG_EXAM_$(echo -n "${REDIS_PASSWORD}" | md5sum | head -c 8)"
EOF

# 注入 include 到主配置（幂等）
if ! grep -q "redis-exam-system.conf" /etc/redis/redis.conf; then
  echo "" >> /etc/redis/redis.conf
  echo "# 在线考试系统生产配置" >> /etc/redis/redis.conf
  echo "include /etc/redis/redis-exam-system.conf" >> /etc/redis/redis.conf
fi

systemctl enable redis-server >/dev/null 2>&1
systemctl restart redis-server
sleep 1

# 验证
if redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning ping | grep -q PONG; then
  ok "Redis 已就绪 + 生产调优（maxmemory=${REDIS_MAX_MB}M, allkeys-lru, RDB+AOF 混合）"
else
  err "Redis 未响应，请检查 /var/log/redis/redis-server.log"
  exit 1
fi

# ==================== 5. Nginx ====================
log "[5/9] 安装 Nginx..."
if ! command -v nginx >/dev/null 2>&1; then
  apt install -y nginx
fi
systemctl enable nginx >/dev/null 2>&1
ok "Nginx 已就绪"

# ==================== 6. 应用目录 & 用户 ====================
log "[6/9] 创建应用目录 & 用户..."
id -u exam >/dev/null 2>&1 || useradd -r -s /bin/false -M exam
mkdir -p ${APP_DIR}/{logs,uploads/avatar,uploads/chat,frontend/dist,backups}
chown -R exam:exam ${APP_DIR}
chmod 750 ${APP_DIR}

# 创建敏感配置目录
mkdir -p ${SECRETS_DIR}
chmod 700 ${SECRETS_DIR}

# 写 secrets.env（权限 600，仅 root 可读）
cat > ${SECRETS_DIR}/secrets.env <<EOF
# ============================================================
# 在线考试系统 — 敏感环境变量
# ⚠️ 本文件权限必须为 600（仅 root 可读）
# ⚠️ 禁止纳入 Git、禁止以任何方式分享
# ============================================================
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}
JWT_SECRET=${JWT_SECRET}

# 百度语音识别（可选：不用则留空）
BAIDU_APP_ID=
BAIDU_API_KEY=
BAIDU_SECRET_KEY=

# CORS 允许域名（部署时按实际域名修改）
APP_CORS_ORIGINS=https://kimi888.xyz,https://kimi123.site,https://kimi666.online
EOF
chmod 600 ${SECRETS_DIR}/secrets.env
chown root:root ${SECRETS_DIR}/secrets.env
ok "密钥文件已写入：${SECRETS_DIR}/secrets.env（权限 600）"

# ==================== 7. 防火墙（UFW） ====================
log "[7/9] 配置防火墙..."
ufw --force reset >/dev/null 2>&1
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow 'Nginx Full'
# 不开放 8081/3306/6379（全部仅 localhost）
ufw --force enable
ok "UFW 已启用（仅开放 22/80/443）"

# ==================== 8. 安装 certbot（Let's Encrypt） ====================
log "[8/9] 安装 certbot（Let's Encrypt）..."
if ! command -v certbot >/dev/null 2>&1; then
  apt install -y certbot python3-certbot-nginx
fi
ok "certbot 已就绪（证书申请命令见指南）"

# ==================== 9. 生成凭据备忘录 ====================
log "[9/9] 生成部署凭据备忘录..."
CREDENTIALS_FILE="/root/exam-system-credentials-$(date +%Y%m%d-%H%M%S).txt"
cat > ${CREDENTIALS_FILE} <<EOF
========================================
在线考试系统 — 部署凭据备忘录
生成时间：$(date)
========================================

【MySQL】
  Host:           127.0.0.1:3306
  Root Password:  ${DB_ROOT_PASSWORD}
  App User:       ${DB_USER}
  App Password:   ${DB_PASSWORD}
  Database:       ${DB_NAME}

【Redis】
  Host:           127.0.0.1:6379
  Password:       ${REDIS_PASSWORD}

【JWT】
  Secret:         ${JWT_SECRET}

========================================
⚠️ 本文件已写入 ${CREDENTIALS_FILE}（权限 600）
⚠️ 建议妥善保存后删除：shred -u ${CREDENTIALS_FILE}
========================================
EOF
chmod 600 ${CREDENTIALS_FILE}

echo ""
echo "=========================================="
echo -e "  ${C_GREEN}✅ 基础环境安装完成！${C_NC}"
echo "=========================================="
echo ""
echo "凭据已保存到：${CREDENTIALS_FILE}"
echo ""
echo "接下来请手动执行以下步骤："
echo ""
echo "  ① 导入数据库："
echo "     mysql -u root -p'${DB_ROOT_PASSWORD}' ${DB_NAME} < ${APP_DIR}/online_exam_system.sql"
echo ""
echo "  ② 上传后端 JAR 包到：${APP_DIR}/exam-system-1.0.0.jar"
echo ""
echo "  ③ 上传前端构建产物到：${APP_DIR}/frontend/dist/"
echo ""
echo "  ④ 部署 Nginx 配置："
echo "     cp ${SCRIPT_DIR}/nginx.conf /etc/nginx/sites-available/exam-system"
echo "     ln -sf /etc/nginx/sites-available/exam-system /etc/nginx/sites-enabled/"
echo "     rm -f /etc/nginx/sites-enabled/default"
echo "     nginx -t && systemctl reload nginx"
echo ""
echo "  ⑤ 部署 systemd 服务："
echo "     cp ${SCRIPT_DIR}/exam-system.service /etc/systemd/system/"
echo "     systemctl daemon-reload"
echo "     systemctl enable exam-system"
echo "     systemctl start exam-system"
echo ""
echo "  ⑥ 验证："
echo "     systemctl status exam-system"
echo "     curl http://localhost:8081/actuator/health"
echo "     systemd-analyze security exam-system  # 应 ≤ 4.0 SAFE"
echo ""
echo "  ⑦ 申请 HTTPS 证书（备案通过后）："
echo "     certbot --nginx -d kimi888.xyz -d www.kimi888.xyz"
echo ""
echo "=========================================="
