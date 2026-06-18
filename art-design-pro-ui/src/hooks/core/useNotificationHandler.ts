/**
 * useNotificationHandler - 通知点击处理组合式函数（E1 架构升级版）
 *
 * 提供统一的通知点击跳转逻辑，供下拉面板（art-notification）和
 * 通知中心页（views/notification）共用，避免逻辑重复与分叉。
 *
 * E1 重大改进：
 * 1. 优先使用后端生成的 payload.actionUrl 跳转（从 60 行硬编码收敛至 1 行 router.push）
 * 2. 无 actionUrl 时，回退到基于 roleCode/bizType/type 的旧硬编码逻辑（渐进式迁移兼容）
 * 3. 新增通知类型无需改前端，只需后端 NotificationActionUrlResolver 注册
 *
 * 设计原则：
 * 1. 基于 roleCode 字符串判断角色，而非数字 roleId（更稳定）
 * 2. 点击时先标记已读，再关闭面板，最后路由跳转
 * 3. 未知 bizType/type 组合静默忽略，不抛错不弹窗
 *
 * @module hooks/core/useNotificationHandler
 */
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { markAsRead } from '@/api/exam/notification'

export interface NotificationPayload {
  senderId?: number
  senderName?: string
  senderAvatar?: string
  actionUrl?: string
  extras?: Record<string, unknown>
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  bizType: string
  bizId: number | null
  isRead: number
  createTime: string
  /** E1 新增：优先级 1=紧急 / 2=普通 / 3=次要 */
  priority?: number
  /** E1 新增：扩展载荷（发送者、action_url、extras） */
  payload?: NotificationPayload
}

/**
 * actionUrl 安全白名单校验（深度防御 - OWASP A1 防 XSS / A10 防开放重定向）。
 *
 * 严格限制为"站内相对路径"：
 *   - 必须以 `/` 开头
 *   - 禁止 `//` 开头（protocol-relative URL 可能被劫持）
 *   - 禁止包含 `:` 之前的 scheme（javascript: / data: / http: / https: 等）
 *   - 长度不超过 500（防 DoS）
 *
 * 当前后端 NotificationActionUrlResolver 生成的 URL 全部满足约束，
 * 但此处兜底防御，避免未来任何调用点误引入用户输入导致存储型 XSS。
 */
const isSafeActionUrl = (url: unknown): url is string => {
  if (typeof url !== 'string') return false
  if (url.length === 0 || url.length > 500) return false
  // 必须以 / 开头但不能以 // 开头（排除 protocol-relative）
  if (!url.startsWith('/') || url.startsWith('//')) return false
  // 排除任何包含 scheme 的 URL（如 `/redirect?to=javascript:...` 下游仍由 vue-router 处理，
  // 此处仅防 scheme 型；开放重定向由路由本身确保不含跨站跳转）

  if (/^\s*(javascript|data|vbscript|file):/i.test(url)) return false
  return true
}

export function useNotificationHandler() {
  const router = useRouter()
  const userStore = useUserStore()

  /**
   * 处理通知点击
   * @param item 通知项
   * @param closeCallback 关闭面板回调（通知中心页不需要传）
   */
  const handleNotificationClick = async (
    item: NotificationItem,
    closeCallback?: () => void
  ): Promise<void> => {
    // 1. 标记已读
    if (item.isRead === 0) {
      try {
        await markAsRead(item.id)
        item.isRead = 1
      } catch {
        // 静默失败，不影响后续跳转
      }
    }

    // 2. 关闭面板（下拉面板场景）
    closeCallback?.()

    // 3. E1 优先：使用后端生成的 actionUrl（从 60 行 if-else 收敛至 1 行）
    //    深度防御：白名单校验后才 push，拒绝任何非站内相对路径
    const actionUrl = item.payload?.actionUrl
    if (isSafeActionUrl(actionUrl)) {
      router.push(actionUrl)
      // 针对成绩/账号创建场景保留友好提示（可选，保留原有体验）
      if (item.type === 'SCORE_PUBLISHED' || item.type === 'SCORE_UPDATED') {
        ElMessage.success(
          item.type === 'SCORE_UPDATED' ? '成绩已更新，请查看最新详情' : '成绩已公布，请查看详情'
        )
      } else if (item.type === 'ACCOUNT_CREATED') {
        ElMessage.success('欢迎使用在线考试系统！')
      }
      return
    }

    // 4. 回退：旧硬编码逻辑（兼容升级前的历史通知记录，渐进式迁移）
    //    actionUrl 校验失败也会走到此分支（防御性保底跳转）
    handleLegacyNotificationClick(item)
  }

  /**
   * 旧版硬编码跳转逻辑（向后兼容）。
   * 当通知没有后端生成的 actionUrl 时，回退到此处理（例如历史通知记录）。
   */
  const handleLegacyNotificationClick = (item: NotificationItem): void => {
    const roleCode = userStore.info?.roleCode

    if (item.bizType === 'exam') {
      if (roleCode === 'STUDENT') {
        if (item.type === 'EXAM_PUBLISHED' || item.type === 'EXAM_UPDATED') {
          router.push('/student/exam')
          ElMessage.info('请在考试列表中查看考试详情')
        } else if (item.type === 'EXAM_CANCELLED') {
          router.push('/student-home')
        }
      } else if (roleCode === 'TEACHER') {
        if (item.type === 'EXAM_SUBMITTED' && item.bizId) {
          router.push(`/exam-center/marking?examId=${item.bizId}`)
        } else {
          router.push('/exam-center/exam')
        }
      } else if (roleCode === 'ADMIN' && item.type === 'EXAM_CREATED') {
        router.push('/admin-home')
      }
      return
    }

    if (item.bizType === 'score') {
      // SCORE_PUBLISHED：首次发布；SCORE_UPDATED：教师修正后重新发布
      // 后端对两种类型都生成 actionUrl=/my-study/score，但 fallback 必须同样支持，
      // 以兼容历史遗留无 actionUrl 的旧通知（升级前的 sys_notification 记录）。
      if (roleCode === 'STUDENT' && (item.type === 'SCORE_PUBLISHED' || item.type === 'SCORE_UPDATED')) {
        router.push('/my-study/score')
        ElMessage.success(
          item.type === 'SCORE_UPDATED' ? '成绩已更新，请查看最新详情' : '成绩已公布，请查看详情'
        )
      }
      return
    }

    if (item.bizType === 'user') {
      if (roleCode === 'ADMIN') {
        router.push('/admin/user')
      } else if (item.type === 'ACCOUNT_CREATED') {
        // profile 已统一为共享一级路由 /profile，无需再按角色分支
        router.push('/profile')
        ElMessage.success('欢迎使用在线考试系统！')
      }
    }
  }

  /**
   * 通知中心页路径（统一一级路由，三角色共享）
   *
   * @see src/router/modules/shared.ts notificationRoute
   */
  const getNotificationCenterPath = (): string => '/notification'

  return {
    handleNotificationClick,
    getNotificationCenterPath
  }
}
