import { defineStore } from 'pinia'
import { ref } from 'vue'
import SockJS from 'sockjs-client'
import { Client, type IMessage } from '@stomp/stompjs'
import { useUserStore } from './user'
import { ROLE } from '@/constants/role'
import { ElNotification } from 'element-plus'

/**
 * 通知 WebSocket Store
 * 功能：
 * 1. WebSocket 连接管理
 * 2. 实时接收通知推送
 * 3. 浏览器原生通知
 * 4. 降级到轮询（WebSocket 失败时）
 */
export const useNotificationWebSocketStore = defineStore('notificationWebSocket', () => {
  const userStore = useUserStore()

  const stompClient = ref<Client | null>(null)
  const connected = ref(false)
  const fallbackToPolling = ref(false)
  let pollingTimer: ReturnType<typeof setInterval> | null = null
  let intentionalDisconnect = false

  // 连接 WebSocket
  const connect = () => {
    if (connected.value) {
      console.log('⚠️ WebSocket 已连接，跳过重复连接')
      return
    }
    // 清理旧的 STOMP 客户端（防止 STOMP 自动重连期间再次调用 connect 产生连接泄漏）
    if (stompClient.value) {
      try { stompClient.value.deactivate() } catch { /* ignore */ }
      stompClient.value = null
    }
    intentionalDisconnect = false

    // 获取 token
    const token = userStore.accessToken?.replace('Bearer ', '')
    if (!token) {
      console.warn('⚠️ 未找到 token，无法连接 WebSocket')
      return
    }

    // 使用相对路径，开发环境由 Vite 代理转发到后端，避免跨域问题
    const wsUrl = `/ws/notification?token=${token}`
    console.log('🔌 正在连接 WebSocket:', wsUrl.replace(token, '***'))

    stompClient.value = new Client({
      webSocketFactory: () => new SockJS(wsUrl) as any,
      reconnectDelay: 5000, // 自动重连间隔 5秒
      heartbeatIncoming: 10000, // 心跳检测 10秒
      heartbeatOutgoing: 10000,

      onConnect: () => {
        connected.value = true
        fallbackToPolling.value = false
        stopPolling() // 停止轮询
        console.log('✅ WebSocket 已连接')

        // 订阅个人通知频道
        stompClient.value?.subscribe(
          `/user/queue/notification`,
          (message: IMessage) => {
            handleNewNotification(JSON.parse(message.body))
          }
        )

        // 如果是学生，订阅班级通知
        const roleId = userStore.info?.roleId
        const classId = userStore.info?.classId

        if (roleId === ROLE.STUDENT && classId) {
          stompClient.value?.subscribe(
            `/topic/class/${classId}/notification`,
            (message: IMessage) => {
              handleNewNotification(JSON.parse(message.body))
            }
          )
          console.log(`📢 已订阅班级 ${classId} 通知`)
        }

        // 如果是管理员，订阅管理员通知
        if (roleId === ROLE.ADMIN) {
          stompClient.value?.subscribe(
            `/topic/admin/notification`,
            (message: IMessage) => {
              handleNewNotification(JSON.parse(message.body))
            }
          )
          console.log('📢 已订阅管理员通知')
        }
      },

      onStompError: (frame) => {
        console.error('❌ WebSocket STOMP 错误:', frame)
        connected.value = false
        fallbackToPolling.value = true
        startPolling() // 降级到轮询
      },

      onWebSocketClose: () => {
        console.warn('⚠️ WebSocket 连接关闭')
        connected.value = false

        // 如果是主动断开（退出登录），不启动轮询
        if (intentionalDisconnect) return

        // 如果不是主动断开，则降级到轮询
        if (!fallbackToPolling.value) {
          fallbackToPolling.value = true
          startPolling()
        }
      },

      onWebSocketError: (event) => {
        console.error('❌ WebSocket 错误:', event)
        connected.value = false
        fallbackToPolling.value = true
        startPolling()
      }
    })

    stompClient.value.activate()
  }

  /**
   * F1: 用户偏好开关（localStorage 持久化）
   * - notify.desktop: 浏览器原生桌面通知（默认 true）
   * - notify.sound:   提示音（默认 true）
   * - notify.inApp:   应用内右上角弹窗（默认 true，强烈不建议关闭）
   */
  const PREF_KEY_DESKTOP = 'notify.desktop'
  const PREF_KEY_SOUND = 'notify.sound'
  const PREF_KEY_IN_APP = 'notify.inApp'

  const isDesktopEnabled = (): boolean => localStorage.getItem(PREF_KEY_DESKTOP) !== 'false'
  const isSoundEnabled = (): boolean => localStorage.getItem(PREF_KEY_SOUND) !== 'false'
  const isInAppEnabled = (): boolean => localStorage.getItem(PREF_KEY_IN_APP) !== 'false'

  /** 用户切换偏好（UI 组件调用） */
  const setDesktopEnabled = (enabled: boolean) => localStorage.setItem(PREF_KEY_DESKTOP, String(enabled))
  const setSoundEnabled = (enabled: boolean) => localStorage.setItem(PREF_KEY_SOUND, String(enabled))
  const setInAppEnabled = (enabled: boolean) => localStorage.setItem(PREF_KEY_IN_APP, String(enabled))

  // 处理新通知
  const handleNewNotification = (notification: any) => {
    console.log('📬 收到新通知:', notification)

    // 触发全局事件，通知组件刷新
    import('@/utils/sys/mittBus').then((mod) => {
      mod.default.emit('refreshNotification')

      // 考试相关通知同步触发 examEvent，让页面数据实时刷新（无需等待轮询）
      // 注：SCORE_UPDATED（教师修正后重新发布成绩）必须与 SCORE_PUBLISHED 同步触发，
      // 否则学生收到通知后我的成绩页不会自动刷新（实测 BUG），与 MarkingServiceImpl 行为对齐
      const examTypes = ['EXAM_PUBLISHED', 'EXAM_UPDATED', 'EXAM_CANCELLED', 'EXAM_SUBMITTED',
        'EXAM_AUTO_SUBMITTED', 'EXAM_ABSENT', 'EXAM_END_SUMMARY', 'SCORE_PUBLISHED', 'SCORE_UPDATED']
      if (notification.type && examTypes.includes(notification.type)) {
        mod.default.emit('examEvent', { type: notification.type, bizId: notification.bizId })
      }
    })

    // F1: 根据优先级智能选择通知行为
    const priority = notification.priority || 2
    // 紧急（priority=1）：即使关闭开关也强制提示，保证关键事件不被错过
    const isUrgent = priority === 1

    // 应用内弹窗（ElNotification 右上角）
    if (isInAppEnabled() || isUrgent) {
      ElNotification({
        title: notification.title || '新通知',
        message: notification.content || '',
        type: isUrgent ? 'warning' : 'info',
        duration: isUrgent ? 8000 : 4000, // 紧急通知显示更久
        position: 'top-right'
      })
    }

    // 浏览器原生通知（需要用户授权 + 偏好开关）
    if ((isDesktopEnabled() || isUrgent)
        && 'Notification' in window
        && Notification.permission === 'granted') {
      new Notification(notification.title || '新通知', {
        body: notification.content || '',
        // 使用项目自带的 favicon.ico 作为通知图标，避免 logo.png 404
        // （Notification.icon 兼容 .ico 格式，Chrome/Firefox/Edge 均支持）
        icon: '/favicon.ico',
        tag: notification.id?.toString() || 'notification',
        requireInteraction: isUrgent // 紧急通知要求用户交互才消失
      })
    }

    // 提示音（可选开关 + 紧急豁免）
    if (isSoundEnabled() || isUrgent) {
      playNotificationSound()
    }
  }

  // 断开连接
  const disconnect = () => {
    intentionalDisconnect = true
    stopPolling()
    if (stompClient.value) {
      stompClient.value.deactivate()
      connected.value = false
      console.log('🔌 WebSocket 已断开')
    }
  }

  // 播放提示音
  const playNotificationSound = () => {
    try {
      const audio = new Audio('/sounds/notification.mp3')
      audio.volume = 0.3
      audio.play().catch(() => {
        // 静默失败（浏览器可能阻止自动播放）
      })
    } catch (e) {
      // 静默失败
    }
  }

  // 降级轮询（WebSocket 失败时）
  const startPolling = () => {
    if (pollingTimer) return
    if (!userStore.accessToken) return

    console.log('🔄 启动轮询模式（每30秒）')

    // 立即执行一次
    void (async () => {
      try {
        if (!userStore.accessToken) return
        const { getUnreadCount } = await import('@/api/exam/notification')
        await getUnreadCount()
        const mittBus = (await import('@/utils/sys/mittBus')).default
        mittBus.emit('refreshNotification')
      } catch {
        // Silent failure to avoid breaking fallback mode.
      }
    })()

    // 每30秒轮询一次
    pollingTimer = setInterval(async () => {
      try {
        const { getUnreadCount } = await import('@/api/exam/notification')
        await getUnreadCount()
        // 触发全局刷新事件，让通知面板同步更新列表和待办
        const mittBus = (await import('@/utils/sys/mittBus')).default
        mittBus.emit('refreshNotification')
      } catch (e) {
        console.error('轮询失败', e)
      }
    }, 30000)
  }

  const stopPolling = () => {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
      console.log('⏹️ 停止轮询')
    }
  }

  // 请求浏览器通知权限
  const requestNotificationPermission = () => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          console.log('✅ 浏览器通知权限已授予')
        } else {
          console.log('⚠️ 用户拒绝了浏览器通知权限')
        }
      })
    }
  }

  return {
    connected,
    fallbackToPolling,
    connect,
    disconnect,
    requestNotificationPermission,
    // F1: 通知偏好开关
    isDesktopEnabled,
    isSoundEnabled,
    isInAppEnabled,
    setDesktopEnabled,
    setSoundEnabled,
    setInAppEnabled
  }
})
