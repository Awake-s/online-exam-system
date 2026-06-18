#!/bin/bash
# ============================================================
# 在线考试系统 — 线上更新脚本（零停机友好）
#
# 流程：
#   本地打包 → scp 上传 /home/ubuntu/staging/ → 跑此脚本更新
#
# 功能：
#   1. 校验 staging 目录下的待部署文件
#   2. 备份当前生产版本（带时间戳）
#   3. 替换新版本
#   4. 前端更新无需重启（Nginx 直接服务新文件）
#   5. 后端更新 systemd restart + 等待健康检查
#   6. 健康检查失败自动回滚
#
# 约定：
#   JAR  文件：/home/ubuntu/staging/exam-system-1.0.0.jar
#   前端文件：/home/ubuntu/staging/dist/
#
# 用法：
#   sudo bash /opt/exam-system/update.sh backend     # 只更新后端
#   sudo bash /opt/exam-system/update.sh frontend    # 只更新前端
#   sudo bash /opt/exam-system/update.sh all         # 全部更新
#   sudo bash /opt/exam-system/update.sh rollback    # 回滚到上一版本
# ============================================================

set -euo pipefail

# ==================== 配置 ====================
APP_DIR="/opt/exam-system"
STAGING_DIR="/home/ubuntu/staging"
BACKUP_DIR="${APP_DIR}/backups/releases"
JAR_NAME="exam-system-1.0.0.jar"
FRONTEND_DIR="${APP_DIR}/frontend/dist"
# 上线门禁用 readiness 组（readinessState+db），不含 Redis 这类可降级依赖；
# 根端点 /actuator/health 仍含 Redis，留给监控/人工排障用。
HEALTH_URL="http://127.0.0.1:8081/actuator/health/readiness"
HEALTH_TIMEOUT=60         # 启动等待超时（秒）
DATE=$(date +%Y%m%d_%H%M%S)

# ==================== 日志函数 ====================
log()  { echo "[$(date '+%H:%M:%S')] $*"; }
err()  { echo "[$(date '+%H:%M:%S')] ❌ $*" >&2; }
ok()   { echo "[$(date '+%H:%M:%S')] ✅ $*"; }
warn() { echo "[$(date '+%H:%M:%S')] ⚠️  $*"; }

# ==================== 参数解析 ====================
ACTION="${1:-}"

case "${ACTION}" in
    backend|frontend|all|rollback) ;;
    "")
        err "缺少参数"
        echo "用法：$0 backend|frontend|all|rollback"
        exit 1
        ;;
    *)
        err "未知操作：${ACTION}"
        exit 1
        ;;
esac

# ==================== 前置检查 ====================
[[ $EUID -eq 0 ]] || { err "必须以 root/sudo 身份运行"; exit 1; }
mkdir -p "${BACKUP_DIR}"/{jar,frontend}

# ==================== 子函数：后端健康检查 ====================
health_check() {
    local waited=0
    log "🩺 健康检查（最长等待 ${HEALTH_TIMEOUT}s）..."
    while [[ ${waited} -lt ${HEALTH_TIMEOUT} ]]; do
        if curl -sf --max-time 3 "${HEALTH_URL}" 2>/dev/null | grep -q '"status":"UP"'; then
            ok "后端健康检查通过"
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
        printf "."
    done
    echo ""
    err "健康检查超时（${HEALTH_TIMEOUT}s 内未返回 UP）"
    return 1
}

# ==================== 子函数：后端更新 ====================
update_backend() {
    local NEW_JAR="${STAGING_DIR}/${JAR_NAME}"

    if [[ ! -f "${NEW_JAR}" ]]; then
        err "未找到 ${NEW_JAR}"
        err "请先在本地打包并 scp 上传："
        echo "     mvn package -DskipTests"
        echo "     scp target/${JAR_NAME} ubuntu@SERVER:${STAGING_DIR}/"
        return 1
    fi

    # 校验 JAR 完整性
    if ! unzip -t "${NEW_JAR}" >/dev/null 2>&1; then
        err "${NEW_JAR} 损坏（zip 完整性校验失败）"
        return 1
    fi

    NEW_SIZE=$(du -h "${NEW_JAR}" | awk '{print $1}')
    log "📦 新 JAR：${NEW_JAR} (${NEW_SIZE})"

    # 1. 备份当前版本
    local CURRENT_JAR="${APP_DIR}/${JAR_NAME}"
    if [[ -f "${CURRENT_JAR}" ]]; then
        local BACKUP_JAR="${BACKUP_DIR}/jar/${JAR_NAME}.${DATE}.bak"
        cp -p "${CURRENT_JAR}" "${BACKUP_JAR}"
        ok "当前版本已备份：${BACKUP_JAR}"
    else
        warn "当前无 JAR 文件（首次部署）"
    fi

    # 2. 替换
    log "🔄 替换 JAR..."
    install -o exam -g exam -m 640 "${NEW_JAR}" "${CURRENT_JAR}"

    # 3. 重启服务
    log "🔃 重启 exam-system..."
    systemctl restart exam-system

    # 4. 健康检查
    if health_check; then
        ok "后端更新成功！"
        # 清理 staging 里的 JAR（避免混淆下次更新）
        rm -f "${NEW_JAR}"
        log "🧹 已清理 staging 里的 JAR"
        # 只保留最近 5 个备份
        ls -t "${BACKUP_DIR}/jar/" | tail -n +6 | xargs -r -I {} rm -- "${BACKUP_DIR}/jar/{}"
        return 0
    else
        # 失败，自动回滚
        err "健康检查失败，自动回滚到上一版本..."
        local LATEST_BACKUP
        LATEST_BACKUP=$(ls -t "${BACKUP_DIR}/jar/"*.bak 2>/dev/null | head -1)
        if [[ -n "${LATEST_BACKUP}" && -f "${LATEST_BACKUP}" ]]; then
            install -o exam -g exam -m 640 "${LATEST_BACKUP}" "${CURRENT_JAR}"
            systemctl restart exam-system
            warn "已回滚到 ${LATEST_BACKUP}"
            if health_check; then
                err "回滚版本运行正常。请排查新版 JAR 问题。"
            else
                err "回滚后仍不健康！需人工介入"
                err "查日志：sudo tail -100 /opt/exam-system/logs/stdout.log"
            fi
        else
            err "无可用备份，回滚失败！需人工恢复"
        fi
        return 1
    fi
}

# ==================== 子函数：前端更新 ====================
update_frontend() {
    local NEW_DIST="${STAGING_DIR}/dist"

    if [[ ! -d "${NEW_DIST}" ]]; then
        err "未找到 ${NEW_DIST}"
        err "请先在本地打包并 scp 上传："
        echo "     npm run build"
        echo "     scp -r dist ubuntu@SERVER:${STAGING_DIR}/"
        return 1
    fi

    if [[ ! -f "${NEW_DIST}/index.html" ]]; then
        err "${NEW_DIST}/index.html 不存在（前端构建可能失败）"
        return 1
    fi

    local FILE_COUNT
    FILE_COUNT=$(find "${NEW_DIST}" -type f | wc -l)
    local TOTAL_SIZE
    TOTAL_SIZE=$(du -sh "${NEW_DIST}" | awk '{print $1}')
    log "📦 新前端：${FILE_COUNT} 个文件 (${TOTAL_SIZE})"

    # 1. 备份当前版本
    if [[ -d "${FRONTEND_DIR}" ]] && [[ -f "${FRONTEND_DIR}/index.html" ]]; then
        local BACKUP_FE="${BACKUP_DIR}/frontend/dist.${DATE}"
        cp -a "${FRONTEND_DIR}" "${BACKUP_FE}"
        ok "当前前端已备份：${BACKUP_FE}"
    fi

    # 2. 原子替换（先写 .new，再 mv）
    log "🔄 替换前端静态文件..."
    rm -rf "${FRONTEND_DIR}.new"
    cp -a "${NEW_DIST}" "${FRONTEND_DIR}.new"
    chown -R exam:exam "${FRONTEND_DIR}.new"
    chmod -R u=rwX,g=rX,o=rX "${FRONTEND_DIR}.new"

    if [[ -d "${FRONTEND_DIR}" ]]; then
        rm -rf "${FRONTEND_DIR}.old"
        mv "${FRONTEND_DIR}" "${FRONTEND_DIR}.old"
    fi
    mv "${FRONTEND_DIR}.new" "${FRONTEND_DIR}"
    rm -rf "${FRONTEND_DIR}.old" 2>/dev/null || true

    # 3. Nginx reload（不中断连接，加载新文件）
    log "🔃 Nginx reload..."
    if nginx -t >/dev/null 2>&1; then
        systemctl reload nginx
        ok "Nginx 已 reload"
    else
        warn "Nginx 配置测试失败，跳过 reload（前端文件已更新）"
    fi

    # 4. 验证
    if curl -sfo /dev/null http://127.0.0.1/; then
        ok "前端更新成功！"
        rm -rf "${NEW_DIST}"
        log "🧹 已清理 staging 里的 dist"
        # 只保留最近 3 个前端备份
        ls -dt "${BACKUP_DIR}/frontend/dist."* 2>/dev/null | tail -n +4 | xargs -r rm -rf
        return 0
    else
        err "前端验证失败（http://127.0.0.1/ 不可访问）"
        return 1
    fi
}

# ==================== 子函数：回滚 ====================
rollback() {
    warn "⏪ 回滚模式"

    # 回滚后端
    local LATEST_JAR_BACKUP
    LATEST_JAR_BACKUP=$(ls -t "${BACKUP_DIR}/jar/"*.bak 2>/dev/null | head -1)
    if [[ -n "${LATEST_JAR_BACKUP}" && -f "${LATEST_JAR_BACKUP}" ]]; then
        log "后端回滚到：${LATEST_JAR_BACKUP}"
        install -o exam -g exam -m 640 "${LATEST_JAR_BACKUP}" "${APP_DIR}/${JAR_NAME}"
        systemctl restart exam-system
        health_check || warn "后端回滚后健康检查失败"
    else
        warn "无后端备份可回滚"
    fi

    # 回滚前端
    local LATEST_FE_BACKUP
    LATEST_FE_BACKUP=$(ls -dt "${BACKUP_DIR}/frontend/dist."* 2>/dev/null | head -1)
    if [[ -n "${LATEST_FE_BACKUP}" && -d "${LATEST_FE_BACKUP}" ]]; then
        log "前端回滚到：${LATEST_FE_BACKUP}"
        rm -rf "${FRONTEND_DIR}.rollback"
        cp -a "${LATEST_FE_BACKUP}" "${FRONTEND_DIR}.rollback"
        chown -R exam:exam "${FRONTEND_DIR}.rollback"
        chmod -R u=rwX,g=rX,o=rX "${FRONTEND_DIR}.rollback"
        [[ -d "${FRONTEND_DIR}" ]] && mv "${FRONTEND_DIR}" "${FRONTEND_DIR}.tmp_$$"
        mv "${FRONTEND_DIR}.rollback" "${FRONTEND_DIR}"
        rm -rf "${FRONTEND_DIR}.tmp_$$" 2>/dev/null || true
        systemctl reload nginx
    else
        warn "无前端备份可回滚"
    fi

    ok "回滚完成"
}

# ==================== 主流程 ====================
echo ""
echo "=========================================="
echo "   在线考试系统 — 线上更新（${ACTION}）"
echo "=========================================="
echo ""

case "${ACTION}" in
    backend)
        update_backend
        ;;
    frontend)
        update_frontend
        ;;
    all)
        update_frontend && update_backend
        ;;
    rollback)
        rollback
        ;;
esac

echo ""
echo "=========================================="
log "📋 当前版本状态："
if [[ -f "${APP_DIR}/${JAR_NAME}" ]]; then
    JAR_TIME=$(stat -c '%y' "${APP_DIR}/${JAR_NAME}" | cut -d'.' -f1)
    JAR_SIZE=$(du -h "${APP_DIR}/${JAR_NAME}" | awk '{print $1}')
    echo "   后端 JAR：${JAR_SIZE}，更新时间：${JAR_TIME}"
fi
if [[ -f "${FRONTEND_DIR}/index.html" ]]; then
    FE_TIME=$(stat -c '%y' "${FRONTEND_DIR}/index.html" | cut -d'.' -f1)
    FE_FILES=$(find "${FRONTEND_DIR}" -type f | wc -l)
    echo "   前端文件：${FE_FILES} 个，更新时间：${FE_TIME}"
fi
echo "   服务状态：$(systemctl is-active exam-system)"
echo "=========================================="

exit 0
