#!/bin/bash
# ============================================================
# 在线考试系统 — 测试数据清理脚本
#
# 场景：
#   上线前用管理员/教师/学生账号进行过功能测试，数据库里留下了
#   考试记录、聊天消息、通知等测试数据。正式上线前需清空这些数据，
#   但要保留基础数据（用户、角色、班级、科目、题库等）。
#
# 清理分级（层层递进）：
#   --basic   清理: 通知 + 聊天会话/消息  （最轻，始终安全）
#   --full    清理: basic + 考试/考试记录/答题记录 + 错题
#   --reset   清理: full + 试卷 + 试卷模板（回到"只有题库"的状态）
#
# 始终保留（永远不清）：
#   - sys_user / sys_role                （用户和角色）
#   - edu_major / edu_class / edu_subject （教学基础）
#   - subject_major                       （科目专业关联）
#   - teacher_class / teacher_subject     （教师班级/科目）
#   - exam_question                       （题库）
#
# 安全措施：
#   1. 执行前自动 mysqldump 全库备份到 /opt/exam-system/backups/cleanup/
#   2. 交互式显示将要清理的表 + 当前行数，等待 yes/no 确认
#   3. 使用 --yes 跳过确认（自动化场景）
#   4. 使用 --dry-run 仅预览不执行
#
# 用法：
#   sudo bash /opt/exam-system/cleanup-test-data.sh --basic            # 交互式
#   sudo bash /opt/exam-system/cleanup-test-data.sh --full --dry-run   # 只预览
#   sudo bash /opt/exam-system/cleanup-test-data.sh --reset --yes      # 跳过确认（危险）
# ============================================================

set -euo pipefail

# ==================== 参数解析 ====================
LEVEL=""         # basic / full / reset
DRY_RUN=0
YES=0

for arg in "$@"; do
    case "${arg}" in
        --basic)   LEVEL="basic" ;;
        --full)    LEVEL="full" ;;
        --reset)   LEVEL="reset" ;;
        --dry-run) DRY_RUN=1 ;;
        --yes|-y)  YES=1 ;;
        -h|--help)
            grep -E '^#' "$0" | sed 's/^# \{0,1\}//' | head -50
            exit 0
            ;;
        *)
            echo "❌ 未知参数：${arg}"
            echo "用法：$0 --basic|--full|--reset [--dry-run] [--yes]"
            exit 1
            ;;
    esac
done

if [[ -z "${LEVEL}" ]]; then
    echo "❌ 必须指定清理级别：--basic / --full / --reset"
    echo "运行 $0 --help 查看帮助"
    exit 1
fi

# ==================== 配置 ====================
APP_DIR="/opt/exam-system"
BACKUP_DIR="${APP_DIR}/backups/cleanup"
SECRETS_FILE="/etc/exam-system/secrets.env"
DB_NAME="online_exam_system"
DATE=$(date +%Y%m%d_%H%M%S)

# ==================== 日志函数 ====================
log() { echo "[$(date '+%H:%M:%S')] $*"; }
err() { echo "[$(date '+%H:%M:%S')] ❌ $*" >&2; }
ok()  { echo "[$(date '+%H:%M:%S')] ✅ $*"; }

# ==================== 前置检查 ====================
[[ $EUID -eq 0 ]] || { err "必须以 root/sudo 身份运行"; exit 1; }
[[ -f "${SECRETS_FILE}" ]] || { err "未找到 ${SECRETS_FILE}"; exit 1; }

# 只 grep 需要的变量，避免 source 整个文件（含特殊字符 & 的 URL）
DB_USER=$(grep -E '^DB_USER=' "${SECRETS_FILE}" | head -1 | cut -d= -f2-)
DB_PASSWORD=$(grep -E '^DB_PASSWORD=' "${SECRETS_FILE}" | head -1 | cut -d= -f2-)

[[ -n "${DB_PASSWORD}" ]] || { err "DB_PASSWORD 未设置"; exit 1; }

# MySQL 命令别名（内部使用 root 可能权限更足，但应用账号也够）
MYSQL_CMD="mysql -u ${DB_USER:-exam_app} -p${DB_PASSWORD} ${DB_NAME}"
MYSQLDUMP_CMD="mysqldump -u ${DB_USER:-exam_app} -p${DB_PASSWORD} ${DB_NAME}"

# ==================== 定义各级别要清理的表 ====================
# 顺序很重要：先清子表（有外键引用的）再清父表
BASIC_TABLES=(
    "sys_notification"           # 系统通知
    "chat_message"               # 聊天消息
    "chat_conversation"          # 聊天会话
)

FULL_TABLES=(
    "${BASIC_TABLES[@]}"
    "exam_answer"                # 答题记录（子表）
    "exam_record"                # 考试记录（子表）
    "exam_exam"                  # 考试
)

RESET_TABLES=(
    "${FULL_TABLES[@]}"
    "exam_paper_question"        # 试卷题目关联（子表）
    "exam_paper"                 # 试卷
    "exam_template_rule"         # 模板规则（子表）
    "exam_paper_template"        # 试卷模板
)

# 根据级别选表
case "${LEVEL}" in
    basic)  TARGET_TABLES=("${BASIC_TABLES[@]}") ;;
    full)   TARGET_TABLES=("${FULL_TABLES[@]}") ;;
    reset)  TARGET_TABLES=("${RESET_TABLES[@]}") ;;
esac

# ==================== 显示即将执行的操作 ====================
echo ""
echo "=========================================="
echo "   在线考试系统 — 测试数据清理"
echo "=========================================="
echo "  数据库：${DB_NAME}"
echo "  级别：  ${LEVEL}"
echo "  模式：  $([ ${DRY_RUN} -eq 1 ] && echo '🔍 预览（不会实际执行）' || echo '⚠️  真实执行')"
echo "=========================================="
echo ""
log "📊 扫描将要清理的表和当前行数..."
echo ""

printf "  %-30s %10s\n" "表名" "当前行数"
printf "  %-30s %10s\n" "------------------------------" "----------"

TOTAL_ROWS=0
for tbl in "${TARGET_TABLES[@]}"; do
    COUNT=$(${MYSQL_CMD} -N -B -e "SELECT COUNT(*) FROM ${tbl}" 2>/dev/null || echo "0")
    COUNT=${COUNT:-0}
    printf "  %-30s %10s\n" "${tbl}" "${COUNT}"
    TOTAL_ROWS=$((TOTAL_ROWS + COUNT))
done

printf "  %-30s %10s\n" "------------------------------" "----------"
printf "  %-30s %10s\n" "合计将删除" "${TOTAL_ROWS}"
echo ""

# ==================== 受保护表（永远不动） ====================
echo "🛡️  始终保留（不会被清理）："
echo "    sys_user, sys_role, edu_major, edu_class, edu_subject,"
echo "    subject_major, teacher_class, teacher_subject, exam_question"
echo ""

# ==================== Dry-Run 模式 ====================
if [[ ${DRY_RUN} -eq 1 ]]; then
    log "🔍 Dry-Run 模式，以上是将要执行的操作，未进行任何实际更改。"
    exit 0
fi

# ==================== 二次确认 ====================
if [[ ${TOTAL_ROWS} -eq 0 ]]; then
    ok "目标表已全部为空，无需清理。"
    exit 0
fi

if [[ ${YES} -ne 1 ]]; then
    echo "⚠️  以上 ${#TARGET_TABLES[@]} 个表共 ${TOTAL_ROWS} 行数据将被永久删除。"
    echo "    （删除前会自动全库备份到 ${BACKUP_DIR}）"
    echo ""
    read -p "确认继续？输入 yes 执行，其他取消：" CONFIRM
    if [[ "${CONFIRM}" != "yes" ]]; then
        log "已取消。"
        exit 0
    fi
fi

# ==================== 1. 全库备份（安全网） ====================
mkdir -p "${BACKUP_DIR}"
BACKUP_FILE="${BACKUP_DIR}/before_cleanup_${LEVEL}_${DATE}.sql.gz"

log "💾 清理前全库备份 → ${BACKUP_FILE}"
${MYSQLDUMP_CMD} \
    --single-transaction \
    --quick \
    --lock-tables=false \
    --default-character-set=utf8mb4 \
    --set-gtid-purged=OFF \
    2>/dev/null | gzip -9 > "${BACKUP_FILE}"

if [[ -s "${BACKUP_FILE}" ]] && gzip -t "${BACKUP_FILE}" 2>/dev/null; then
    BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | awk '{print $1}')
    ok "备份完成：${BACKUP_FILE} (${BACKUP_SIZE})"
    ok "⚠️ 如误操作可通过以下命令恢复："
    echo "     gunzip < ${BACKUP_FILE} | mysql -u root -p ${DB_NAME}"
    echo ""
else
    err "备份失败！为安全起见，已中止清理。"
    rm -f "${BACKUP_FILE}"
    exit 1
fi

# ==================== 2. 执行清理 ====================
log "🧹 开始清理（按外键依赖顺序）..."
echo ""

# 关闭外键检查，避免顺序问题导致失败
SQL="SET FOREIGN_KEY_CHECKS = 0;"
for tbl in "${TARGET_TABLES[@]}"; do
    SQL="${SQL} TRUNCATE TABLE ${tbl};"
done
SQL="${SQL} SET FOREIGN_KEY_CHECKS = 1;"

if ${MYSQL_CMD} -e "${SQL}" 2>/dev/null; then
    for tbl in "${TARGET_TABLES[@]}"; do
        ok "  TRUNCATE ${tbl}"
    done
else
    err "清理过程中出错（可能是 exam_app 账号无 TRUNCATE 权限）"
    err "如需尝试用 root 账号执行，请手动跑："
    echo ""
    echo "  mysql -u root -p ${DB_NAME} <<EOF"
    echo "  ${SQL}"
    echo "  EOF"
    echo ""
    exit 1
fi

# ==================== 3. 清理 Redis 相关缓存（可选） ====================
REDIS_PASSWORD=$(grep -E '^REDIS_PASSWORD=' "${SECRETS_FILE}" | head -1 | cut -d= -f2-)
if [[ -n "${REDIS_PASSWORD}" ]]; then
    log "🧹 清理 Redis 相关业务缓存..."
    # 只清业务 Key（exam: 前缀），不动其他项目的 Key
    KEYS_BEFORE=$(redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning --scan --pattern 'exam:*' 2>/dev/null | wc -l)
    redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning --scan --pattern 'exam:*' 2>/dev/null \
        | xargs -r -n 100 redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DEL >/dev/null 2>&1 || true
    KEYS_AFTER=$(redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning --scan --pattern 'exam:*' 2>/dev/null | wc -l)
    ok "  Redis exam:* Keys 清理：${KEYS_BEFORE} → ${KEYS_AFTER}"
else
    log "⚠️ 未找到 REDIS_PASSWORD，跳过 Redis 清理"
fi

# ==================== 4. 验证 ====================
echo ""
log "🔍 清理后验证："
printf "  %-30s %10s\n" "表名" "当前行数"
printf "  %-30s %10s\n" "------------------------------" "----------"
for tbl in "${TARGET_TABLES[@]}"; do
    COUNT=$(${MYSQL_CMD} -N -B -e "SELECT COUNT(*) FROM ${tbl}" 2>/dev/null || echo "?")
    printf "  %-30s %10s\n" "${tbl}" "${COUNT}"
done
echo ""

# ==================== 总结 ====================
echo "=========================================="
ok "   测试数据清理完成"
echo "=========================================="
echo "   级别：    ${LEVEL}"
echo "   清理表数：${#TARGET_TABLES[@]}"
echo "   已删行数：${TOTAL_ROWS}"
echo "   备份位置：${BACKUP_FILE}"
echo "=========================================="
echo ""
log "💡 建议后续动作："
echo "    1. 重启后端让业务缓存彻底重置（可选）："
echo "         sudo systemctl restart exam-system"
echo "    2. 让所有已登录用户重新登录（JWT 仍有效，除非清了 JWT 黑名单）"
echo ""

exit 0
