#!/bin/bash
# ============================================================
# 在线考试系统 — 生产数据备份脚本
#
# 功能：
#   - MySQL 全量备份（gzip 压缩）
#   - Redis AOF/RDB 备份
#   - 用户上传文件打包
#   - 30 天保留，超时自动清理
#   - 备份完整性校验
#
# 部署：
#   sudo cp backup.sh /opt/exam-system/backup.sh
#   sudo chmod +x /opt/exam-system/backup.sh
#   sudo crontab -e
#   # 添加：每天凌晨 3 点执行
#   0 3 * * * /opt/exam-system/backup.sh >> /opt/exam-system/logs/backup.log 2>&1
#
# 手动执行：sudo bash /opt/exam-system/backup.sh
# ============================================================

set -euo pipefail

# ==================== 配置 ====================
APP_DIR="/opt/exam-system"
BACKUP_DIR="${APP_DIR}/backups"
SECRETS_FILE="/etc/exam-system/secrets.env"
DB_NAME="online_exam_system"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)

# ==================== 日志函数 ====================
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }
err() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ $*" >&2; }

# ==================== 前置检查 ====================
[[ $EUID -eq 0 ]] || { err "必须以 root 身份运行"; exit 1; }
[[ -f "${SECRETS_FILE}" ]] || { err "未找到 ${SECRETS_FILE}"; exit 1; }

# 不 source 整个文件，只提取需要的变量（避免含特殊字符如 & 的 URL 类变量导致 shell 解析失败）
DB_USER=$(grep -E '^DB_USER=' "${SECRETS_FILE}" | head -1 | cut -d= -f2-)
DB_PASSWORD=$(grep -E '^DB_PASSWORD=' "${SECRETS_FILE}" | head -1 | cut -d= -f2-)
REDIS_PASSWORD=$(grep -E '^REDIS_PASSWORD=' "${SECRETS_FILE}" | head -1 | cut -d= -f2-)

[[ -n "${DB_PASSWORD}" ]] || { err "DB_PASSWORD 未设置"; exit 1; }
[[ -n "${REDIS_PASSWORD}" ]] || { err "REDIS_PASSWORD 未设置"; exit 1; }

mkdir -p "${BACKUP_DIR}"/{mysql,redis,uploads}

log "=========================================="
log "  开始备份（${DATE}）"
log "=========================================="

# ==================== 1. MySQL 备份 ====================
log "[1/4] MySQL 全量备份..."
MYSQL_BAK="${BACKUP_DIR}/mysql/db_${DATE}.sql.gz"
mysqldump \
    --user="${DB_USER:-exam_app}" \
    --password="${DB_PASSWORD}" \
    --single-transaction \
    --quick \
    --lock-tables=false \
    --routines \
    --triggers \
    --events \
    --default-character-set=utf8mb4 \
    --set-gtid-purged=OFF \
    "${DB_NAME}" 2>/dev/null | gzip -9 > "${MYSQL_BAK}"

# 验证备份完整性
if [[ -s "${MYSQL_BAK}" ]] && gzip -t "${MYSQL_BAK}" 2>/dev/null; then
    MYSQL_SIZE=$(du -h "${MYSQL_BAK}" | awk '{print $1}')
    log "  ✅ MySQL 备份成功：${MYSQL_BAK} (${MYSQL_SIZE})"
else
    err "MySQL 备份失败或文件损坏"
    rm -f "${MYSQL_BAK}"
    exit 1
fi

# ==================== 2. Redis 备份 ====================
log "[2/4] Redis 备份..."
# 触发 BGSAVE（后台异步 RDB 快照）
redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning BGSAVE >/dev/null

# 等待 BGSAVE 完成
LAST_SAVE=$(redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning LASTSAVE)
sleep 2
for i in {1..30}; do
    NEW_SAVE=$(redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning LASTSAVE)
    if [[ "${NEW_SAVE}" != "${LAST_SAVE}" ]]; then
        break
    fi
    sleep 1
done

# 复制 RDB + AOF
# 优先用 CONFIG GET 动态获取；失败则探测常见路径；都不行则警告但不退出
REDIS_DATA_DIR=$(redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning CONFIG GET dir 2>/dev/null | tail -1)
if [[ -z "${REDIS_DATA_DIR}" || ! -d "${REDIS_DATA_DIR}" ]]; then
    for candidate in /var/lib/redis /var/lib/redis-server /var/lib/redis/data; do
        if [[ -d "${candidate}" ]]; then
            REDIS_DATA_DIR="${candidate}"
            log "  （CONFIG GET 未返回有效路径，fallback 到 ${REDIS_DATA_DIR}）"
            break
        fi
    done
fi

REDIS_BAK="${BACKUP_DIR}/redis/redis_${DATE}.tar.gz"
if [[ -n "${REDIS_DATA_DIR}" && -d "${REDIS_DATA_DIR}" ]]; then
    # 用 sudo 可能遇到权限问题的路径也能读（脚本本身已以 root 运行）
    tar -czf "${REDIS_BAK}" -C "${REDIS_DATA_DIR}" . 2>/dev/null || {
        err "Redis 数据打包失败（${REDIS_DATA_DIR}）"
        rm -f "${REDIS_BAK}"
    }
    if [[ -s "${REDIS_BAK}" ]]; then
        REDIS_SIZE=$(du -h "${REDIS_BAK}" | awk '{print $1}')
        log "  ✅ Redis 备份成功：${REDIS_BAK} (${REDIS_SIZE})"
    fi
else
    log "  ⚠️ 未找到 Redis 数据目录，跳过 Redis 备份（非致命，Redis 数据可重建）"
fi

# ==================== 3. 上传文件备份 ====================
log "[3/4] 用户上传文件备份..."
if [[ -d "${APP_DIR}/uploads" ]]; then
    UPLOADS_BAK="${BACKUP_DIR}/uploads/uploads_${DATE}.tar.gz"
    tar -czf "${UPLOADS_BAK}" -C "${APP_DIR}" uploads 2>/dev/null
    UPLOADS_SIZE=$(du -h "${UPLOADS_BAK}" | awk '{print $1}')
    log "  ✅ 上传文件备份成功：${UPLOADS_BAK} (${UPLOADS_SIZE})"
else
    log "  ⚠️ uploads 目录不存在，跳过"
fi

# ==================== 4. 清理过期备份 ====================
log "[4/4] 清理 ${RETENTION_DAYS} 天前的备份..."
BEFORE=$(find "${BACKUP_DIR}" -type f \( -name "*.sql.gz" -o -name "*.tar.gz" \) | wc -l)
find "${BACKUP_DIR}" -type f -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
find "${BACKUP_DIR}" -type f -name "*.tar.gz" -mtime +${RETENTION_DAYS} -delete
AFTER=$(find "${BACKUP_DIR}" -type f \( -name "*.sql.gz" -o -name "*.tar.gz" \) | wc -l)
DELETED=$(( BEFORE - AFTER ))
log "  ✅ 清理完成（删除 ${DELETED} 个过期文件，保留 ${AFTER} 个）"

# ==================== 总结 ====================
TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" | awk '{print $1}')
FREE_SPACE=$(df -h "${BACKUP_DIR}" | awk 'NR==2 {print $4}')

log "=========================================="
log "  ✅ 备份全部完成"
log "  备份目录：${BACKUP_DIR}"
log "  目录占用：${TOTAL_SIZE}"
log "  磁盘剩余：${FREE_SPACE}"
log "=========================================="

# 可选：推送到异地（腾讯云 COS / 阿里云 OSS）
# 取消下面注释并安装对应 CLI 工具后生效
# coscmd upload -r "${BACKUP_DIR}/mysql/" /exam-system-backup/mysql/
# ossutil cp -r "${BACKUP_DIR}/mysql/" oss://your-bucket/exam-system-backup/mysql/

exit 0
