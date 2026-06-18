// 使用 sockjs-client 主入口以匹配 @types/sockjs-client 的类型声明（TS7016 修复）
// 原 `sockjs-client/dist/sockjs` 子路径无类型声明文件，主入口 API 等价且有完整类型
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '@/store/modules/user'
import { mittBus } from '@/utils/sys'
import { ElMessage } from 'element-plus'

let stompClient: Client | null = null
let reconnectAttempts = 0
const MAX_RECONNECT = 10
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let onMessageCallback: ((msg: any) => void) | null = null
let onStatusCallback: ((status: any) => void) | null = null
let onReconnectCallback: (() => void) | null = null
let onReadReceiptCallback: ((receipt: any) => void) | null = null
let onMessageEventCallback: ((event: any) => void) | null = null
// L3-M0-6：Typing Indicator 入站事件回调
let onTypingEventCallback: ((event: any) => void) | null = null
let _isFirstConnect = true
// M2-离线恢复相关状态标志
// _isManuallyDisconnected：区分"用户主动退出"与"网络被动断开"，主动退出时不自动恢复
// _isConnecting：防止 visibilitychange + online 事件并发触发多个 doConnect 导致 Client 泄漏
let _isManuallyDisconnected = false
let _isConnecting = false

/**
 * M2-离线恢复：页面恢复可见或网络恢复时触发的自动重连处理。
 * <p>
 * 触发条件：
 * <ul>
 *   <li>用户切回浏览器 Tab（visibilitychange → visible）</li>
 *   <li>浏览器检测到网络恢复（online 事件）</li>
 * </ul>
 * 执行前置：非主动断开 + 当前未连接 + 未正在连接 + 网络可用。
 * 作用：重置重连计数器后立即发起一次连接（不走指数退避），解决"休眠 >3min 后重连上限已耗尽"的用户体验问题。
 */
function onVisibilityOrOnline() {
  if (_isManuallyDisconnected) return
  if (stompClient?.connected) return
  if (_isConnecting) return
  // visibilitychange 事件时：只在 visible 状态下尝试（隐藏时立即重连意义不大）
  if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return
  // online 事件通常 navigator.onLine === true，visibilitychange 时需要手动兜底判断
  if (typeof navigator !== 'undefined' && !navigator.onLine) return
  console.log('[WebSocket] 页面可见/网络恢复，重置重连计数后立即重连')
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = 0
  doConnect()
}

export function connectWebSocket(
  onMessage: (msg: any) => void,
  onStatusChange?: (status: any) => void,
  onReconnect?: () => void,
  onReadReceipt?: (receipt: any) => void,
  onMessageEvent?: (event: any) => void,
  onTypingEvent?: (event: any) => void
) {
  onMessageCallback = onMessage
  onStatusCallback = onStatusChange || null
  onReconnectCallback = onReconnect || null
  onReadReceiptCallback = onReadReceipt || null
  onMessageEventCallback = onMessageEvent || null
  onTypingEventCallback = onTypingEvent || null
  _isFirstConnect = true
  _isManuallyDisconnected = false
  // 清理旧连接，防止多次调用产生连接泄漏
  if (stompClient) {
    try {
      stompClient.deactivate()
    } catch {
      /* ignore */
    }
    stompClient = null
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = 0 // 重置重连计数器，确保重新登录后能正常重连
  // M2：注册浏览器生命周期事件监听（幂等：重复 addEventListener 相同 fn 会被浏览器去重）
  if (typeof document !== 'undefined') {
    document.removeEventListener('visibilitychange', onVisibilityOrOnline)
    document.addEventListener('visibilitychange', onVisibilityOrOnline)
  }
  if (typeof window !== 'undefined') {
    window.removeEventListener('online', onVisibilityOrOnline)
    window.addEventListener('online', onVisibilityOrOnline)
  }
  doConnect()
}

function doConnect() {
  // M2-离线恢复：防止 visibilitychange + online 事件并发触发重复建 Client
  if (_isConnecting) return
  const userStore = useUserStore()
  const token = userStore.accessToken?.replace('Bearer ', '') || ''
  if (!token) return

  const userId = userStore.getUserInfo?.userId
  if (!userId) return

  _isConnecting = true
  try {
    stompClient = new Client({
      webSocketFactory: () => new SockJS(`/ws?token=${token}`) as any,
      debug: () => {}, // 禁用调试日志
      reconnectDelay: 0, // 禁用自动重连，使用自定义重连逻辑
      heartbeatIncoming: 10000, // 服务端→客户端心跳检测 10秒
      heartbeatOutgoing: 10000, // 客户端→服务端心跳检测 10秒
      onConnect: () => {
        _isConnecting = false
        console.log('[WebSocket] 连接成功')
        const isReconnect = !_isFirstConnect
        _isFirstConnect = false
        reconnectAttempts = 0
        if (isReconnect && onReconnectCallback) {
          console.log('[WebSocket] 重连成功，触发状态重查')
          onReconnectCallback()
        }

        // 订阅私聊消息（使用标准 /user/queue/messages，Spring 根据 Principal 自动路由）
        stompClient?.subscribe('/user/queue/messages', (message: any) => {
          if (message.body && onMessageCallback) {
            try {
              const data = JSON.parse(message.body)
              console.log('[WebSocket] 收到消息推送:', data)
              onMessageCallback(data)
            } catch (e) {
              console.error('[WebSocket] 消息解析失败', e)
            }
          }
        })

        // 订阅在线状态变化（定向推送，后端按业务可见性规则筛选）
        // 修复 M4：从 /topic/online-status（全局广播）改为 /user/queue/online-status（per-user 定向）
        // 安全收益：防止用户枚举 + 行为追踪（如学生无法监控教师上下线时间戳）
        stompClient?.subscribe('/user/queue/online-status', (message: any) => {
          if (message.body && onStatusCallback) {
            try {
              const data = JSON.parse(message.body)
              onStatusCallback(data)
            } catch (e) {
              console.error('[WebSocket] 在线状态解析失败', e)
            }
          }
        })

        // 订阅已读回执（M7）：对方 markAsRead 后，后端推送 { conversationId, readerId, readAt }
        // 前端据此将本地"已发送"消息升级为"已读"状态
        stompClient?.subscribe('/user/queue/read-receipts', (message: any) => {
          if (message.body && onReadReceiptCallback) {
            try {
              const receipt = JSON.parse(message.body)
              onReadReceiptCallback(receipt)
            } catch (e) {
              console.error('[WebSocket] 已读回执解析失败', e)
            }
          }
        })

        // 订阅消息事件（L3）：撤回/强删等非"新消息"类事件
        // payload 示例：{ type: 'MESSAGE_DELETED', messageId, conversationId, deletedBy, deletedAt }
        stompClient?.subscribe('/user/queue/message-events', (message: any) => {
          if (message.body && onMessageEventCallback) {
            try {
              const event = JSON.parse(message.body)
              onMessageEventCallback(event)
            } catch (e) {
              console.error('[WebSocket] 消息事件解析失败', e)
            }
          }
        })

        // 订阅聊天错误推送（后端业务异常会推到这里）
        stompClient?.subscribe('/user/queue/errors', (message: any) => {
          if (message.body) {
            try {
              const err = JSON.parse(message.body)
              if (err?.type === 'CHAT_ERROR' && err.message) {
                ElMessage.error(err.message)
                console.warn('[WebSocket] 聊天错误:', err)
              }
            } catch (e) {
              console.error('[WebSocket] 错误消息解析失败', e)
            }
          }
        })

        // L3-M0-6：订阅 typing 事件（对方正在输入...）
        // 业界参考：WhatsApp/Telegram/Signal 的 typing indicator —— 轻量 disposable 事件，
        // 不走消息持久化；前端需要对接收超时做兜底清除，避免丢包导致 UI 永远悬停
        stompClient?.subscribe('/user/queue/typing-events', (message: any) => {
          if (message.body && onTypingEventCallback) {
            try {
              const event = JSON.parse(message.body)
              onTypingEventCallback(event)
            } catch (e) {
              console.error('[WebSocket] typing 事件解析失败', e)
            }
          }
        })

        // 订阅考试事件（交卷、批改等），通过 mittBus 广播给页面组件
        stompClient?.subscribe('/user/queue/exam-events', (message: any) => {
          if (message.body) {
            try {
              const data = JSON.parse(message.body)
              console.log('[WebSocket] 收到考试事件:', data)
              mittBus.emit('examEvent', data)
            } catch (e) {
              console.error('[WebSocket] 考试事件解析失败', e)
            }
          }
        })
      },
      onStompError: (error: any) => {
        _isConnecting = false
        console.error('[WebSocket] 连接失败', error)
        attemptReconnect()
      },
      onWebSocketClose: () => {
        _isConnecting = false
        console.warn('[WebSocket] 连接关闭')
        attemptReconnect()
      }
    })

    stompClient.activate()
  } catch (e) {
    _isConnecting = false
    console.error('[WebSocket] 初始化失败', e)
    attemptReconnect()
  }
}

function attemptReconnect() {
  if (reconnectAttempts >= MAX_RECONNECT) {
    console.error('[WebSocket] 达到最大重连次数，停止重连')
    return
  }
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000)
  reconnectAttempts++
  console.log(`[WebSocket] ${delay}ms 后第 ${reconnectAttempts} 次重连...`)
  reconnectTimer = setTimeout(() => doConnect(), delay)
}

export function disconnectWebSocket() {
  // M2：标记主动断开 + 注销浏览器生命周期监听器，彻底阻止自动恢复逻辑
  _isManuallyDisconnected = true
  if (typeof document !== 'undefined') {
    document.removeEventListener('visibilitychange', onVisibilityOrOnline)
  }
  if (typeof window !== 'undefined') {
    window.removeEventListener('online', onVisibilityOrOnline)
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = MAX_RECONNECT // 阻止自动重连
  if (stompClient) {
    try {
      stompClient.deactivate()
      console.log('[WebSocket] 已断开连接')
    } catch (e) {
      /* ignore */
    }
    stompClient = null
  }
  _isConnecting = false
}

export function isWsConnected(): boolean {
  return stompClient?.connected || false
}

/**
 * L3-M0-6：发送 typing 事件给对方（STOMP 入站端点 {@code /app/chat/typing}）。
 * <p>
 * 业界参考：WhatsApp 的事件驱动 typing indicator —— 轻量 disposable 事件，
 * 丢包可接受（接收端有 TTL 兜底自动清除）。
 * <p>
 * <b>前端调用约定</b>：
 * <ul>
 *   <li>用户输入时每 {@link ChatConstants#TYPING_HEARTBEAT_MS} ms 重发一次 TYPING_START</li>
 *   <li>用户停止输入 {@link ChatConstants#TYPING_AUTO_STOP_MS} ms 后发 TYPING_STOP</li>
 *   <li>关闭会话 / 发送成功后立即发 TYPING_STOP（避免对方界面残留）</li>
 * </ul>
 *
 * @param receiverId 对方 userId
 * @param type       'TYPING_START' 或 'TYPING_STOP'
 * @returns 是否发送成功（WS 未连接时返回 false，调用方无需处理）
 */
export function sendTypingEvent(receiverId: number, type: 'TYPING_START' | 'TYPING_STOP'): boolean {
  if (!stompClient?.connected) return false
  try {
    stompClient.publish({
      destination: '/app/chat/typing',
      body: JSON.stringify({ receiverId, type })
    })
    return true
  } catch (e) {
    // typing 事件允许丢失，静默记录 debug 日志即可
    console.debug('[WebSocket] 发送 typing 事件失败', e)
    return false
  }
}
