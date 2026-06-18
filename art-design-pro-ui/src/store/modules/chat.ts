import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getChatContacts, getChatConversations, getChatMessages, sendChatMessage, sendChatMessagesBatch, markConversationRead, getChatUnreadCount, getUserOnlineStatus, batchGetOnlineStatus, getIncrementalMessages, recallChatMessage, adminDeleteChatMessage, hideChatConversation, unhideChatConversation, pinChatConversation, unpinChatConversation, muteChatConversation, unmuteChatConversation } from '@/api/exam/chat'
import type { BatchSendItem } from '@/api/exam/chat'
import { connectWebSocket, disconnectWebSocket } from '@/utils/websocket'
import { useUserStore } from '@/store/modules/user'

export const useChatStore = defineStore('chat', () => {
  const contacts = ref<any[]>([])
  const conversations = ref<any[]>([])
  /**
   * L3-M1-4：已归档会话独立列表（对应后端 user?_hidden=1）。
   * <p>
   * 业务语义：与 Telegram Archive 模式一致 —— 归档 ≠ 删除：
   * - 主列表 {@link conversations} 默认展示，走 loadConversations()（archived=false）
   * - 已归档列表 {@link archivedConversations} 按需加载，走 loadArchivedConversations()（archived=true）
   * - 收到新消息时，后端自动解除归档，WS 推送 CONVERSATION_UNHIDDEN 让前端把会话从归档移回主列表
   */
  const archivedConversations = ref<any[]>([])
  const currentConversationId = ref<number | null>(null)
  const currentMessages = ref<any[]>([])
  const unreadTotal = ref(0)
  const wsConnected = ref(false)
  const onlineStatusMap = ref<Record<string, boolean>>({})

  /**
   * L3-M0-6：当前用户看到的 "XX 正在输入..." 状态表。
   * <p>
   * key = 发送方 userId（Number），value = 用户还会被视为"正在输入"的到期时间戳（ms）。
   * <p>
   * 业界参考：
   * <ul>
   *   <li>WhatsApp/Telegram — typing 状态即时展示，设 TTL 是因为 STOP 事件可能丢包</li>
   *   <li>心跳间隔 3s + TTL 6s = 能容忍 1 次 START 丢包，不会出现中断闪烁</li>
   * </ul>
   */
  const typingUsers = ref<Record<number, number>>({})
  let _typingGcTimer: ReturnType<typeof setInterval> | null = null
  // I2：跟踪本地见过的最大 messageId，用于 WS 重连时向后端增量拉取漏消息。
  // 注意：只统计后端返回的真实数字 id，忽略 sendMessage 乐观 UI 的 tmp_xxx 临时 id。
  const maxSeenMessageId = ref(0)

  const currentConversation = computed(() =>
    conversations.value.find(c => c.id === currentConversationId.value)
  )

  async function loadContacts() {
    try {
      contacts.value = await getChatContacts()
      // 从联系人数据中直接提取在线状态（后端已嵌入 online 字段）
      let hasOnlineField = false
      for (const c of contacts.value) {
        if (c.id !== undefined && c.online !== undefined) {
          onlineStatusMap.value[String(c.id)] = !!c.online
          hasOnlineField = true
        }
      }
      // 如果后端未返回 online 字段，标记需要额外查询
      if (!hasOnlineField) {
        console.log('[loadContacts] 后端未返回 online 字段，将通过 syncAllOnlineStatus 补充')
      }
    } catch { /* ignore */ }
  }

  async function loadConversations() {
    try { conversations.value = await getChatConversations(false) } catch { /* ignore */ }
  }

  /**
   * L3-M1-4：按需加载"已归档"会话列表（对应 Telegram Archive 视图）。
   * 只在用户展开归档区域时调用，避免每次打开聊天面板都多一次 HTTP。
   */
  async function loadArchivedConversations() {
    try { archivedConversations.value = await getChatConversations(true) } catch { /* ignore */ }
  }

  async function loadMessages(conversationId: number, page = 1, size = 50) {
    try {
      const res = await getChatMessages(conversationId, { page, size })
      // 消息按时间倒序返回，需要反转为正序显示
      // L3-M0-3：把后端返回的撤回元数据（deleted/deletedAt/deletedBy）统一映射，
      // 与 WS MESSAGE_DELETED 事件路径保持相同数据结构，UI 渲染逻辑单一来源
      currentMessages.value = (res.records || []).reverse().map((m: any) => ({
        ...m,
        // 后端撤回消息的 content 脱敏为 null，前端据 deleted 标记渲染占位符，不依赖 content
        deleted: !!m.deleted,
      }))
      // I2：跟踪最大 messageId，便于重连补拉
      for (const m of currentMessages.value) bumpMaxSeenId(m)
    } catch { /* ignore */ }
  }

  /**
   * I2 辅助：更新 maxSeenMessageId。
   * 只接受后端返回的真实数字 id（忽略乐观 UI 的 tmp_xxx 字符串 id）。
   */
  function bumpMaxSeenId(msg: any) {
    const id = msg?.id
    if (typeof id === 'number' && id > maxSeenMessageId.value) {
      maxSeenMessageId.value = id
    }
  }

  /**
   * I2 辅助：按 conversationId 节流调用 markConversationRead。
   * <p>
   * 批量补拉场景下多条消息触发同一会话的 markAsRead 调用，节流为单次 REST 请求；
   * 常规单条推送场景下每次都立即发起，用户体验不变（1s 内重复推送才会被合并）。
   */
  const _markAsReadTimers: Record<string, ReturnType<typeof setTimeout>> = {}
  function throttledMarkAsRead(conversationId: number) {
    const key = String(conversationId)
    if (_markAsReadTimers[key]) return // 已有待执行的调用，直接合并
    _markAsReadTimers[key] = setTimeout(() => {
      delete _markAsReadTimers[key]
      markConversationRead(conversationId).catch(() => { /* ignore */ })
    }, 200) // 200ms 合并窗口，用户感知无延迟
  }

  async function openConversation(conversationId: number) {
    currentConversationId.value = conversationId
    await loadMessages(conversationId)
    await markConversationRead(conversationId)
    // 更新会话列表中的未读数
    const conv = conversations.value.find(c => c.id === conversationId)
    if (conv) conv.unreadCount = 0
    await refreshUnreadCount()
  }

  /**
   * 生成 UUID v4 作为客户端消息幂等键
   * 优先用 crypto.randomUUID（现代浏览器原生支持），降级到手写版本
   */
  function generateClientMsgId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID()
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0
      const v = c === 'x' ? r : (r & 0x3) | 0x8
      return v.toString(16)
    })
  }

  /**
   * 发送消息（乐观 UI + 幂等重试）
   * 状态机：sending → sent → delivered / failed
   */
  async function sendMessage(receiverId: number, content: string) {
    const userStore = useUserStore()
    const myId = userStore.getUserInfo?.userId
    const tempId = `tmp_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    const clientMsgId = generateClientMsgId()

    // 1️⃣ 乐观插入占位消息到本地 UI
    const optimisticMsg: any = {
      id: tempId,
      tempId,
      clientMsgId,
      senderId: myId,
      receiverId,
      content,
      isMe: true,
      isRead: 0,
      createTime: new Date().toISOString(),
      status: 'sending',
    }
    currentMessages.value.push(optimisticMsg)

    try {
      // 2️⃣ REST 发送（持久化 + 后端 WebSocket 推送）
      const realMsg = await sendChatMessage({ receiverId, content, clientMsgId })
      // 3️⃣ 用真实消息替换占位（保留 tempId 便于未来操作）
      const idx = currentMessages.value.findIndex((m: any) => m.id === tempId)
      if (idx >= 0) {
        currentMessages.value[idx] = {
          ...realMsg,
          isMe: true,
          tempId,
          clientMsgId,
          status: 'sent',
        }
      }
      bumpMaxSeenId(realMsg)
      return realMsg
    } catch (e) {
      console.error('发送消息失败', e)
      // 4️⃣ 失败：标红占位消息允许用户重发
      const msg = currentMessages.value.find((m: any) => m.id === tempId)
      if (msg) msg.status = 'failed'
      throw e
    }
  }

  /**
   * L3-M3-1：原子批量发送（对齐 Slack chat.postMessage batch / Telegram sendMediaGroup / Discord 单消息多附件）。
   * <p>
   * 状态机流：
   * <ul>
   *   <li>所有 items 乐观插入占位（status='sending'），用户立刻看到 N 个气泡</li>
   *   <li>一次 REST 调用批量发送（整批 1 个速率限制令牌）</li>
   *   <li>按 clientMsgId 精确匹配后端 per-item 结果：
   *     <ul>
   *       <li><code>sent</code> / <code>already_sent</code>：用真实消息替换占位（含 id/createTime/senderName 等），status='sent'</li>
   *       <li><code>failed</code>：保留占位，status='failed'，气泡 ❗ 图标点击触发 resendMessage（单条路径 + 同一 clientMsgId 保证幂等）</li>
   *     </ul>
   *   </li>
   *   <li>整体请求失败（批次级限流 / 网络中断）：所有占位统一标 failed</li>
   * </ul>
   * <p>
   * 关键约束：每个 item 的 clientMsgId 独立生成，保证即使"部分已入库 + 用户重发"也不会产生重复消息。
   *
   * @param contents 待发送的消息内容数组（图片/文件/文本统一编码后传入）
   * @returns 批次结果统计：sentCount / failedCount / rateLimited / failedTempIds（用于 UI 提示）
   */
  async function sendMessagesBatch(receiverId: number, contents: string[]): Promise<{
    sentCount: number
    failedCount: number
    rateLimited: boolean
    failedTempIds: string[]
  }> {
    const userStore = useUserStore()
    const myId = userStore.getUserInfo?.userId

    if (!contents.length) {
      return { sentCount: 0, failedCount: 0, rateLimited: false, failedTempIds: [] }
    }

    // 1️⃣ 构造每项 clientMsgId / tempId 映射，乐观插入占位
    const tempIdByClient: Record<string, string> = {}
    const items: BatchSendItem[] = contents.map((content) => {
      const tempId = `tmp_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
      const clientMsgId = generateClientMsgId()
      tempIdByClient[clientMsgId] = tempId
      currentMessages.value.push({
        id: tempId,
        tempId,
        clientMsgId,
        senderId: myId,
        receiverId,
        content,
        isMe: true,
        isRead: 0,
        createTime: new Date().toISOString(),
        status: 'sending',
      })
      return { clientMsgId, content }
    })

    // 2️⃣ 批量 REST 调用
    try {
      const resp = await sendChatMessagesBatch({ receiverId, items })
      const results = resp?.results || []

      let sentCount = 0
      let failedCount = 0
      const failedTempIds: string[] = []

      // 3️⃣ 按 clientMsgId 匹配升级 UI 占位
      for (const r of results) {
        const tempId = tempIdByClient[r.clientMsgId]
        if (!tempId) continue
        const idx = currentMessages.value.findIndex((m: any) => m.tempId === tempId)
        if (idx < 0) continue

        if (r.status === 'sent' || r.status === 'already_sent') {
          currentMessages.value[idx] = {
            ...r.message,
            isMe: true,
            tempId,
            clientMsgId: r.clientMsgId,
            status: 'sent',
          }
          if (r.message) bumpMaxSeenId(r.message)
          sentCount++
        } else {
          // failed：保留占位 + 标记 failed（气泡 ❗ 触发 resendMessage 单条重发）
          currentMessages.value[idx].status = 'failed'
          failedCount++
          failedTempIds.push(tempId)
        }
      }

      return { sentCount, failedCount, rateLimited: false, failedTempIds }
    } catch (e: any) {
      // 整批失败（批次级限流 / 网络中断）：所有占位标 failed
      const msg = String(e?.response?.data?.message || e?.message || '')
      const rateLimited = /频繁|rate.?limit|429/i.test(msg)
      const failedTempIds: string[] = []
      for (const clientMsgId in tempIdByClient) {
        const tempId = tempIdByClient[clientMsgId]
        const m = currentMessages.value.find((x: any) => x.tempId === tempId)
        if (m) {
          m.status = 'failed'
          failedTempIds.push(tempId)
        }
      }
      console.error('批量发送消息失败', e)
      return {
        sentCount: 0,
        failedCount: items.length,
        rateLimited,
        failedTempIds,
      }
    }
  }

  /**
   * 重发失败的消息（复用原 clientMsgId 保证幂等）
   */
  async function resendMessage(tempId: string) {
    const msg = currentMessages.value.find((m: any) => m.tempId === tempId)
    if (!msg || msg.status !== 'failed') return
    msg.status = 'sending'
    try {
      const realMsg = await sendChatMessage({
        receiverId: msg.receiverId,
        content: msg.content,
        clientMsgId: msg.clientMsgId,
      })
      const idx = currentMessages.value.findIndex((m: any) => m.tempId === tempId)
      if (idx >= 0) {
        currentMessages.value[idx] = {
          ...realMsg,
          isMe: true,
          tempId,
          clientMsgId: msg.clientMsgId,
          status: 'sent',
        }
      }
      bumpMaxSeenId(realMsg)
    } catch (e) {
      const m = currentMessages.value.find((x: any) => x.tempId === tempId)
      if (m) m.status = 'failed'
      throw e
    }
  }

  function handleIncomingMessage(msg: any) {
    // 补充 isMe 字段（WebSocket 推送的消息不含此字段）
    const userStore = useUserStore()
    const myId = userStore.getUserInfo?.userId
    if (myId && msg.senderId !== undefined) {
      msg.isMe = String(msg.senderId) === String(myId)
    }
    // I2：更新本地"见过的最大 messageId"，用于下次 WS 重连时的增量拉取
    bumpMaxSeenId(msg)

    // L3-M0-2：WS 重连补拉会带回断连期间被撤回的消息（deletedAt 非空）。
    // 本地已渲染 → 按 MESSAGE_DELETED 处理（显示占位）；本地没有 → 忽略（用户从未见过，撤回对他无意义）。
    if (msg.deletedAt) {
      const existsLocally = currentMessages.value.find((m: any) => m.id === msg.id)
      if (existsLocally) {
        markMessageDeletedLocal(msg.id, msg.conversationId, msg.deletedBy)
      }
      return
    }

    // ⭐ 自回推场景（L3-M1-3 修复）：自己发的消息一律在此分支处理完毕，绝不往下走。
    //
    // 【历史 bug】原实现用 m.id === msg.id 去重，但乐观占位的 id 是前端临时 tmp_xxx、
    // WS 自回推的 msg.id 是后端真实数字 — 两者永不相等。当 WS 比 REST 先到时，去重
    // 失败 → 进入下方 isCurrentConv 分支 → 再插入一条 → UI 出现两条相同消息（图 1）。
    //
    // 【修复思路】改用 clientMsgId 作为稳定标识（前端生成，贯穿"乐观占位 → REST 响应 → WS 推送"
    // 三个阶段）。业界同构方案：Zulip local_id / Baileys messageKey.id / Signal clientMsgId。
    //
    // 【强约束】isMe 分支无论是否命中乐观占位，都必须 return，杜绝双路径插入。
    if (msg.isMe) {
      // 优先按 clientMsgId 匹配（稳定），次选 id（兼容服务端不带 clientMsgId 的历史/补拉数据）
      const existingIdx = currentMessages.value.findIndex((m: any) => {
        if (msg.clientMsgId && m.clientMsgId) {
          return m.clientMsgId === msg.clientMsgId
        }
        return m.id === msg.id
      })
      if (existingIdx >= 0) {
        // 升级：把乐观占位的 id（tmp_xxx）替换为真实 id + 更新状态为 delivered
        // 必须保留原占位的 tempId，用于失败重发路径（resendMessage 按 tempId 查找）
        const existing = currentMessages.value[existingIdx]
        currentMessages.value[existingIdx] = {
          ...existing,
          ...msg,
          isMe: true,
          tempId: existing.tempId,
          clientMsgId: existing.clientMsgId || msg.clientMsgId,
          status: 'delivered',
        }
        patchConversationLocal(msg, /* incrementUnread */ false)
        return
      }
      // 本地完全没占位（罕见：刷新后重连补拉的自消息）→ 插入一次，但**仍然必须 return**
      currentMessages.value.push({ ...msg, isMe: true, status: 'delivered' })
      patchConversationLocal(msg, /* incrementUnread */ false)
      return
    }

    // 判断是否属于当前打开的聊天（兼容新聊天 conversationId 尚为 null 的情况）
    const isCurrentConv = currentConversationId.value
      ? msg.conversationId === currentConversationId.value
      : false
    // 新聊天场景：conversationId 为 null，但对方的 senderId 或 receiverId 匹配当前聊天对象
    const isNewChatMatch = !currentConversationId.value && myId && (
      String(msg.senderId) === String(myId) || String(msg.receiverId) === String(myId)
    ) && msg.conversationId

    if (isCurrentConv || isNewChatMatch) {
      // 去重（REST 和 WS 可能都推送了）
      if (!currentMessages.value.find((m: any) => m.id === msg.id)) {
        currentMessages.value.push({ ...msg, status: 'delivered' })
      }
      // 新聊天首次收到消息时，绑定 conversationId
      if (!currentConversationId.value && msg.conversationId) {
        currentConversationId.value = msg.conversationId
      }
      // 自动标记已读（会触发后端 markAsRead → 后端再推送 read-receipt 给对方）
      // I2：批量补拉场景下多条消息落入同一会话，节流合并为单次调用，避免 N 次 REST 请求
      if (currentConversationId.value) {
        throttledMarkAsRead(currentConversationId.value)
      }
    }

    // L3-M1-4：收到新消息时，如果该会话在归档列表里，后端已自动把 hidden 重置为 0，
    // 前端需要把会话从 archivedConversations 搬回 conversations（Telegram 归档规则的一部分）
    if (msg.conversationId) {
      const archIdx = archivedConversations.value.findIndex((c: any) => c.id === msg.conversationId)
      if (archIdx >= 0) {
        const restored = archivedConversations.value.splice(archIdx, 1)[0]
        if (!conversations.value.some((c: any) => c.id === restored.id)) {
          conversations.value.unshift(restored)
        }
      }
    }

    // M6：增量更新会话列表与未读计数
    // 当前会话正在打开 → 不增未读（会立刻被 markAsRead 清零），否则 +1
    const shouldIncrementUnread = !msg.isMe && !isCurrentConv
    const patched = patchConversationLocal(msg, shouldIncrementUnread)
    if (!patched) {
      // 本地找不到该会话 → 真的是新会话，才做一次全量拉取（对新会话不可避免，但仅这一次）
      loadConversations()
    }
    if (shouldIncrementUnread) {
      unreadTotal.value = unreadTotal.value + 1
    }
  }

  /**
   * M6：增量更新本地会话列表里的 lastMessage/lastMessageTime/unreadCount，
   * 避免每条消息都触发 loadConversations() + getUnreadCount() 两次全量 API。
   *
   * @param msg WS 推送的消息
   * @param incrementUnread 是否对该会话未读数 +1（当且仅当消息是对方发的且会话未打开）
   * @returns 本地是否找到该会话（找不到则调用方应触发一次全量拉取）
   */
  function patchConversationLocal(msg: any, incrementUnread: boolean): boolean {
    if (!msg?.conversationId) return false
    const conv = conversations.value.find((c: any) => c.id === msg.conversationId)
    if (!conv) return false
    // 生成本地预览（与后端 truncateByCodePoint 一致的 code point 安全截取）
    const preview = codePointSubstring(String(msg.content ?? ''), 100)
    conv.lastMessage = preview
    conv.lastMessageTime = msg.createTime || new Date().toISOString()
    // L3-M0-5：同步发送者 ID，让 formatLastMessage 能正确渲染"你/对方xxx"
    if (msg.senderId != null) {
      conv.lastMessageSenderId = msg.senderId
    }
    if (incrementUnread) {
      conv.unreadCount = (Number(conv.unreadCount) || 0) + 1
    }
    // 将最新会话移到相应位置（与后端 pinned DESC, lastMessageTime DESC 排序保持一致）
    // L3-M0-7：尊重置顶约束 —— 非置顶会话不能越过置顶组插到列表最顶
    const idx = conversations.value.indexOf(conv)
    if (idx > 0) {
      conversations.value.splice(idx, 1)
      if (conv.pinned) {
        // 置顶会话 → 插到置顶组的最顶端（即列表最顶）
        conversations.value.unshift(conv)
      } else {
        // 非置顶会话 → 插到第一个非置顶会话的位置（即所有置顶之后）
        const firstNonPinnedIdx = conversations.value.findIndex((c: any) => !c.pinned)
        if (firstNonPinnedIdx < 0) {
          // 列表里全是置顶 → 追加到尾
          conversations.value.push(conv)
        } else {
          conversations.value.splice(firstNonPinnedIdx, 0, conv)
        }
      }
    }
    return true
  }

  /**
   * M7 辅助：根据 code point 截取字符串（不切断 emoji 代理对）
   */
  function codePointSubstring(s: string, maxCodePoints: number): string {
    if (!s) return ''
    const arr = Array.from(s) // Array.from 按 code point 迭代，比 string.split('') 安全
    if (arr.length <= maxCodePoints) return s
    return arr.slice(0, maxCodePoints).join('')
  }

  /**
   * M7 / L3-M1-1：统一处理后端回执事件（通过 /user/queue/read-receipts 投递）。
   * <p>
   * 两种类型：
   * - READ_RECEIPT  对方读了我的消息 → 将我的消息状态升级为 'read'
   * - SELF_READ     我自己在另一个 Tab 读了会话 → 当前 Tab 同步清零该会话的未读计数 + 刷新全局铃铛
   *
   * payload: { type, conversationId, readerId, readAt, count }
   */
  function handleReadReceipt(receipt: any) {
    if (!receipt) return
    const userStore = useUserStore()
    const myId = userStore.getUserInfo?.userId
    if (!myId) return

    if (receipt.type === 'READ_RECEIPT') {
      // 对方读了我的消息。防御：回执发起者必须不是自己
      if (String(receipt.readerId) === String(myId)) return
      // 仅更新当前打开会话内已渲染的消息（其他会话无活跃渲染可省略）
      if (currentConversationId.value !== receipt.conversationId) return
      for (const m of currentMessages.value) {
        if (m.isMe && (m.status === 'sent' || m.status === 'delivered' || m.status == null)) {
          m.status = 'read'
          m.isRead = 1
        }
      }
    } else if (receipt.type === 'SELF_READ') {
      // L3-M1-1：自己在另一个 Tab 读了该会话，当前 Tab 同步清零未读（多标签页一致性）
      if (String(receipt.readerId) !== String(myId)) return
      const conv = conversations.value.find((c: any) => c.id === receipt.conversationId)
      if (conv) conv.unreadCount = 0
      // 同步刷新全局铃铛数（从 DB 读准确值，避免本地累加误差）
      refreshUnreadCount()
    }
  }

  // ========== L3：消息撤回 / 强删 / 会话隐藏 ==========

  /**
   * L3：处理后端 MESSAGE_DELETED 事件（被对方撤回或被管理员强删）。
   * 当前会话内渲染的消息将被标记为 deleted，UI 渲染为"已撤回"占位。
   * L3-M0-1：载荷带回 newLastMessage / newLastMessageTime 时，同步更新会话列表预览，
   * 避免左侧列表继续残留已撤回的文本。
   */
  function handleMessageEvent(event: any) {
    if (!event) return
    if (event.type === 'MESSAGE_DELETED') {
      // L3-M0-4：透传 deletedAt 供前端"重新编辑"按钮的窗口计算
      markMessageDeletedLocal(event.messageId, event.conversationId, event.deletedBy, event.deletedAt)
      // 会话列表 lastMessage 预览同步（来自后端 refreshConversationLastMessage）
      if (event.conversationId && 'newLastMessage' in event) {
        const conv = conversations.value.find((c: any) => c.id === event.conversationId)
        if (conv) {
          conv.lastMessage = event.newLastMessage ?? ''
          conv.lastMessageTime = event.newLastMessageTime ?? conv.lastMessageTime
          // L3-M0-5：同步 sender_id，前端 formatLastMessage 据此渲染"你/对方撤回"
          if ('newLastMessageSenderId' in event) {
            conv.lastMessageSenderId = event.newLastMessageSenderId
          }
        }
      }
    } else if (event.type === 'CONVERSATION_HIDDEN') {
      // M2 / L3-M1-4：自己在另一 Tab 主动归档了会话，当前 Tab 同步更新
      // 幂等：当前 Tab 若刚刚乐观处理过（hideConversationLocal），两个数组里的状态已正确，跳过即可
      if (!event.conversationId) return
      const idx = conversations.value.findIndex((c: any) => c.id === event.conversationId)
      if (idx >= 0) {
        // 从主列表搬到归档列表（本地有完整对象，不需要走 HTTP 拉取）
        const removed = conversations.value.splice(idx, 1)[0]
        const alreadyInArch = archivedConversations.value.some((c: any) => c.id === removed.id)
        if (!alreadyInArch) {
          const insertAt = archivedConversations.value.findIndex(
            (c: any) => new Date(c.lastMessageTime || 0).getTime() < new Date(removed.lastMessageTime || 0).getTime()
          )
          if (insertAt < 0) archivedConversations.value.push(removed)
          else archivedConversations.value.splice(insertAt, 0, removed)
        }
      }
      // 若当前正在打开该会话，清除当前状态（后端已把未读列为排除，铃铛也要刷新）
      if (currentConversationId.value === event.conversationId) {
        currentConversationId.value = null
        currentMessages.value = []
      }
      refreshUnreadCount()
    } else if (event.type === 'CONVERSATION_UNHIDDEN') {
      // L3-M1-4：自己在另一 Tab 主动取消归档，或收到新消息时后端自动解除了归档
      // 从归档列表搬回主列表；若本地归档里没有（冷启动场景），就触发一次主列表刷新
      if (!event.conversationId) return
      const archIdx = archivedConversations.value.findIndex((c: any) => c.id === event.conversationId)
      if (archIdx >= 0) {
        const removed = archivedConversations.value.splice(archIdx, 1)[0]
        const alreadyInMain = conversations.value.some((c: any) => c.id === removed.id)
        if (!alreadyInMain) {
          const insertAt = conversations.value.findIndex(
            (c: any) => new Date(c.lastMessageTime || 0).getTime() < new Date(removed.lastMessageTime || 0).getTime()
          )
          if (insertAt < 0) conversations.value.push(removed)
          else conversations.value.splice(insertAt, 0, removed)
        }
      } else {
        // 本地归档列表里没缓存 → 走一次 loadConversations 补齐主列表（含这条会话的完整数据）
        loadConversations()
      }
      refreshUnreadCount()
    }
  }

  /**
   * L3 辅助：把本地消息数组里的指定 id 标记为已撤回。
   * 不从数组移除，保留位置占位（业界 WhatsApp/Telegram 的通行做法）。
   * L3-M0-4：新增 deletedAt 字段（来自 WS 事件 payload），用于"重新编辑"按钮的 2min 窗口计算。
   * 同时把 content 脱敏为 null —— 对齐后端 getMessages 的做法，防止前端 devtools 残留原文。
   */
  function markMessageDeletedLocal(messageId: number, conversationId: number, deletedBy?: number, deletedAt?: string) {
    if (!messageId || !conversationId) return
    // 仅更新当前会话内已渲染消息；其他会话用户下次打开时从 DB 拉取会自然过滤掉
    if (currentConversationId.value !== conversationId) return
    const idx = currentMessages.value.findIndex((m: any) => m.id === messageId)
    if (idx < 0) return
    currentMessages.value[idx] = {
      ...currentMessages.value[idx],
      deleted: true,
      deletedBy,
      deletedAt: deletedAt || new Date().toISOString(),  // WS 有带 deletedAt 则用之，否则本地兜底
      content: null,  // 脱敏：前端数组不再保留撤回前原文
    }
  }

  /**
   * L3：发送者撤回自己的消息（2 分钟内）。
   * 乐观 UI：先本地标记为 deleted，失败则回滚；成功后后端也会推 WS 事件，handleMessageEvent 幂等处理。
   */
  async function recallMyMessage(messageId: number) {
    const idx = currentMessages.value.findIndex((m: any) => m.id === messageId)
    const original = idx >= 0 ? { ...currentMessages.value[idx] } : null
    // 乐观标记
    if (idx >= 0) {
      currentMessages.value[idx] = { ...currentMessages.value[idx], deleted: true }
    }
    try {
      await recallChatMessage(messageId)
    } catch (e) {
      // 回滚
      if (idx >= 0 && original) currentMessages.value[idx] = original
      throw e
    }
  }

  /**
   * L3：管理员强删任意消息。
   * 与 recallMyMessage 同理，先乐观标记，失败则回滚。
   */
  async function adminDeleteMessage(messageId: number) {
    const idx = currentMessages.value.findIndex((m: any) => m.id === messageId)
    const original = idx >= 0 ? { ...currentMessages.value[idx] } : null
    if (idx >= 0) {
      currentMessages.value[idx] = { ...currentMessages.value[idx], deleted: true }
    }
    try {
      await adminDeleteChatMessage(messageId)
    } catch (e) {
      if (idx >= 0 && original) currentMessages.value[idx] = original
      throw e
    }
  }

  /**
   * L3：当前用户归档会话（从主列表移到"已归档"视图）。
   * <p>
   * L3-M1-4 升级：乐观 UI 不只是"从主列表移除"，还需要把该会话**插入归档列表**，
   * 保证本地状态机与后端数据一致（后端是 hidden=1，前端有独立 archivedConversations 数组）。
   * 这是把"隐藏=删除感"改造成"归档=可找回"的关键实现点。
   */
  async function hideConversationLocal(conversationId: number) {
    const idx = conversations.value.findIndex((c: any) => c.id === conversationId)
    const removed = idx >= 0 ? conversations.value.splice(idx, 1)[0] : null
    // 如果正在打开该会话，清除当前会话
    if (currentConversationId.value === conversationId) {
      currentConversationId.value = null
      currentMessages.value = []
    }
    // 同步到归档列表（按 lastMessageTime 插入到正确位置，保持有序）
    if (removed) {
      const insertAt = archivedConversations.value.findIndex(
        (c: any) => new Date(c.lastMessageTime || 0).getTime() < new Date(removed.lastMessageTime || 0).getTime()
      )
      if (insertAt < 0) {
        archivedConversations.value.push(removed)
      } else {
        archivedConversations.value.splice(insertAt, 0, removed)
      }
    }
    try {
      await hideChatConversation(conversationId)
    } catch (e) {
      // 回滚：同时撤销主列表移除 + 归档列表添加
      if (removed) {
        conversations.value.splice(idx, 0, removed)
        const archIdx = archivedConversations.value.findIndex((c: any) => c.id === conversationId)
        if (archIdx >= 0) archivedConversations.value.splice(archIdx, 1)
      }
      throw e
    }
  }

  /**
   * L3-M1-4：取消归档（把会话从归档列表恢复到主列表）。
   * <p>
   * 乐观 UI：立即从 archivedConversations 移除并插入 conversations；失败则回滚。
   * 插入位置按 lastMessageTime 排序，与主列表的后端排序规则对齐。
   */
  async function unhideConversationLocal(conversationId: number) {
    const archIdx = archivedConversations.value.findIndex((c: any) => c.id === conversationId)
    const removed = archIdx >= 0 ? archivedConversations.value.splice(archIdx, 1)[0] : null
    if (removed) {
      const insertAt = conversations.value.findIndex(
        (c: any) => new Date(c.lastMessageTime || 0).getTime() < new Date(removed.lastMessageTime || 0).getTime()
      )
      if (insertAt < 0) {
        conversations.value.push(removed)
      } else {
        conversations.value.splice(insertAt, 0, removed)
      }
    }
    try {
      await unhideChatConversation(conversationId)
      // 后端真相源：解除归档后全局未读可能恢复（此前被 hidden 过滤掉了）
      refreshUnreadCount()
    } catch (e) {
      if (removed) {
        archivedConversations.value.splice(archIdx, 0, removed)
        const mainIdx = conversations.value.findIndex((c: any) => c.id === conversationId)
        if (mainIdx >= 0) conversations.value.splice(mainIdx, 1)
      }
      throw e
    }
  }

  /**
   * L3-M0-7 辅助：将 conversations 数组按"置顶优先 + lastMessageTime DESC"稳定排序。
   * <p>
   * 在 pin/unpin 成功后调用，保证前端列表排序与后端 getConversations 响应一致。
   * 使用原地 sort，响应性靠 ref 的 .value 整体替换触发（Vue 对数组内部项索引变化会脏检查）。
   */
  function _sortConversationsByPinned() {
    conversations.value.sort((a: any, b: any) => {
      const ap = !!a.pinned
      const bp = !!b.pinned
      if (ap !== bp) return ap ? -1 : 1
      // 同组内按 lastMessageTime DESC
      const at = new Date(a.lastMessageTime || 0).getTime()
      const bt = new Date(b.lastMessageTime || 0).getTime()
      return bt - at
    })
  }

  /**
   * L3-M0-7：会话置顶 / 取消置顶（乐观 UI + 自动重排序）。
   * <p>
   * 业界参考：微信 / WhatsApp / Telegram Pin Chat。
   * 上限 5 个由后端强制校验，超限时后端抛业务异常，前端 catch 后自动回滚并把错误抛出。
   *
   * @param conversationId 目标会话 ID
   * @param pinned         true=置顶，false=取消置顶
   */
  async function setConversationPinnedLocal(conversationId: number, pinned: boolean) {
    const conv = conversations.value.find((c: any) => c.id === conversationId)
    if (!conv) return
    const prevPinned = !!conv.pinned
    if (prevPinned === pinned) return  // 幂等：已是目标状态，无需操作

    // 乐观 UI：立即更新本地状态并重排
    conv.pinned = pinned
    _sortConversationsByPinned()

    try {
      if (pinned) await pinChatConversation(conversationId)
      else await unpinChatConversation(conversationId)
    } catch (e) {
      // 回滚
      conv.pinned = prevPinned
      _sortConversationsByPinned()
      throw e
    }
  }

  /**
   * L3-M0-7：会话免打扰 / 取消免打扰（乐观 UI）。
   * <p>
   * 业界参考：WhatsApp Mute / 微信消息免打扰。语义：不影响消息收取和未读计数，
   * 但前端桌面通知/声音逻辑会据此字段过滤（见 handleIncomingMessage 里的 mute 判定）。
   */
  async function setConversationMutedLocal(conversationId: number, muted: boolean) {
    const conv = conversations.value.find((c: any) => c.id === conversationId)
    if (!conv) return
    const prev = !!conv.muted
    if (prev === muted) return

    conv.muted = muted
    try {
      if (muted) await muteChatConversation(conversationId)
      else await unmuteChatConversation(conversationId)
    } catch (e) {
      conv.muted = prev
      throw e
    }
  }

  async function refreshUnreadCount() {
    try {
      const res = await getChatUnreadCount()
      unreadTotal.value = res.total || 0
    } catch { /* ignore */ }
  }

  function handleStatusChange(status: any) {
    if (status.userId !== undefined) {
      onlineStatusMap.value[String(status.userId)] = !!status.online
    }
  }

  async function checkUserOnline(userId: number) {
    try {
      const res = await getUserOnlineStatus(userId)
      onlineStatusMap.value[String(userId)] = !!res.online
    } catch { /* ignore */ }
  }

  // syncAllOnlineStatus in-flight 锁：避免 30s 轮询期间慢请求引发并发竞争
  // 命名风格对齐项目内已有的私有状态变量（如 _typingGcTimer）
  let _syncOnlineStatusInFlight: Promise<void> | null = null

  /**
   * 同步所有联系人/会话对端的在线状态（v2.x 真实数据规模适配版）。
   * <p>
   * 关键工程实践（应对 v2.x 引入 24 教师 + 490 学生 = 514 用户后的边界场景）：
   * <ol>
   *   <li><b>分批 ≤ 200</b>：与后端 {@code ChatController#batchGetOnlineStatus} 的反慢速枚举安全限制对齐
   *       （后端注释明确说明该限制用于"防止超大 userIds 列表做慢速枚举攻击"）。</li>
   *   <li><b>Promise.allSettled 而非 all</b>：单批失败不连累其他批的成功结果。这是关键 — 旧实现用
   *       Promise.all 在任一批失败时会抛弃所有成功结果并触发 514 并发降级，构成"网络抖动 → 雪崩"风险。
   *       MDN 官方推荐 allSettled 用于"独立失败可容忍"的并发场景。</li>
   *   <li><b>仅失败批走降级</b>：旧实现失败后 Promise.all(ids.map) 会触发 N 个独立 HTTP 请求（admin 场景 514 个），
   *       打满浏览器 HTTP/1.1 6 并发上限并消耗后端 Tomcat 线程池。新实现仅对失败批的 ids 降级。</li>
   *   <li><b>in-flight 单飞锁</b>：上层 30s 轮询调用本函数，若上一次因网络慢未完成，下一次直接复用 in-flight
   *       promise（参考 SWR / React Query 的 dedupe 模式），杜绝并发竞争。</li>
   * </ol>
   */
  async function syncAllOnlineStatus(): Promise<void> {
    // 工业级 dedupe：重入直接复用 in-flight promise（避免 30s 轮询期间慢请求引发并发竞争）
    if (_syncOnlineStatusInFlight) return _syncOnlineStatusInFlight
    _syncOnlineStatusInFlight = (async () => {
      const ids = [...new Set([
        ...contacts.value.map((c: any) => c.id).filter(Boolean),
        ...conversations.value.map((c: any) => c.otherUserId).filter(Boolean),
      ])]
      if (ids.length === 0) return
      // 策略 1：批量接口分批并发查询（每批 ≤ 200，allSettled 容错）
      const BATCH_SIZE = 200
      const batches: number[][] = []
      for (let i = 0; i < ids.length; i += BATCH_SIZE) {
        batches.push(ids.slice(i, i + BATCH_SIZE))
      }
      const settled = await Promise.allSettled(batches.map((b) => batchGetOnlineStatus(b)))
      let anyFulfilled = false
      const failedBatches: number[][] = []
      settled.forEach((r, idx) => {
        if (r.status === 'fulfilled' && r.value && typeof r.value === 'object') {
          anyFulfilled = true
          for (const [k, v] of Object.entries(r.value)) {
            onlineStatusMap.value[k] = !!v
          }
        } else if (r.status === 'rejected') {
          failedBatches.push(batches[idx])
        }
      })
      // 全部成功 → 早退
      if (failedBatches.length === 0) return
      // 策略 2：降级到逐个查询。仅对失败批的 ids 降级（而非全量 ids），杜绝雪崩
      // 极端 case：所有批都失败 + 无任何 fulfilled → 降级到全量逐个，行为兜底
      const fallbackIds = anyFulfilled ? failedBatches.flat() : ids
      await Promise.all(fallbackIds.map((id: number) => checkUserOnline(id)))
    })()
    try {
      await _syncOnlineStatusInFlight
    } finally {
      _syncOnlineStatusInFlight = null
    }
  }

  function isUserOnline(userId: number | string): boolean {
    return !!onlineStatusMap.value[String(userId)]
  }

  /**
   * I2：WS 重连后调用，补拉断连期间漏掉的消息。
   * <p>
   * 实现要点：
   * 1. 以 `maxSeenMessageId` 作为游标，只拉新消息，避免全量传输。
   * 2. 若本地从未见过任何消息（maxSeenMessageId === 0），则跳过 —— 由 loadConversations 触发冷启动，
   *    增量接口只服务 "已有上下文的重连" 场景。
   * 3. 分页循环：若单批返回 == limit，说明可能还有更多，继续拉直到 < limit 或达到总条数上限（2000）兜底。
   * 4. 单条消息走 handleIncomingMessage 复用去重 / 会话归档 / 未读计数 / UI 插入逻辑。
   */
  async function pullIncrementalMessages() {
    if (maxSeenMessageId.value <= 0) {
      console.log('[chat] 重连：本地无历史消息，跳过增量拉取（由常规加载接口负责）')
      return
    }
    const BATCH_LIMIT = 200
    const MAX_TOTAL = 2000 // 安全兜底，防止意外死循环
    let total = 0
    try {
      while (total < MAX_TOTAL) {
        const batch = await getIncrementalMessages({
          sinceMessageId: maxSeenMessageId.value,
          limit: BATCH_LIMIT,
        })
        if (!Array.isArray(batch) || batch.length === 0) break
        console.log(`[chat] 重连补拉 ${batch.length} 条漏消息（since=${maxSeenMessageId.value}）`)
        for (const m of batch) {
          handleIncomingMessage(m) // 内部有去重，幂等安全
        }
        total += batch.length
        // 未满批说明已拉完，退出；否则循环再拉一批
        if (batch.length < BATCH_LIMIT) break
      }
      if (total >= MAX_TOTAL) {
        console.warn(`[chat] 重连补拉达到安全上限 ${MAX_TOTAL} 条，建议用户手动刷新以获取更早内容`)
      }
    } catch (e) {
      console.error('[chat] 重连补拉失败，下次重连会自动重试', e)
    }
  }

  /**
   * L3-M0-6：处理后端推送的 typing 事件（对方正在输入...）。
   * <p>
   * 业界参考：WhatsApp/Telegram/Signal。事件丞辅元 {@code {type, senderId, timestamp}}。
   * <ul>
   *   <li>TYPING_START → 记录 senderId 的过期时间戳（基于本地 now，避免服务器/客户时钟偏差）</li>
   *   <li>TYPING_STOP → 立即清除</li>
   * </ul>
   */
  function handleTypingEvent(event: any) {
    if (!event || typeof event.senderId !== 'number') return
    if (event.type === 'TYPING_START') {
      // TTL 6s（ChatConstants.TYPING_CLIENT_TTL_MS），与后端心跳间隔 3s 配套
      typingUsers.value[event.senderId] = Date.now() + 6000
    } else if (event.type === 'TYPING_STOP') {
      delete typingUsers.value[event.senderId]
    }
  }

  /**
   * L3-M0-6 辅助：清除过期的 typing 状态（12 Hz 扫描，足以兼顾流畅度和性能）。
   * <p>
   * 必要性：即使后端 TYPING_STOP 丢包，本地也能在 6s 后自动隐藏提示，避免 UI 悬停。
   */
  function _gcTypingUsers() {
    const now = Date.now()
    let changed = false
    for (const uid in typingUsers.value) {
      if (typingUsers.value[uid] <= now) {
        delete typingUsers.value[Number(uid)]
        changed = true
      }
    }
    if (changed) {
      // 触发 Vue 响应 — ref<Record> 的 delete 应是保常触发，这里注释说明不需额外方式
    }
  }

  /**
   * L3-M0-6：查询某 userId 当前是否在 "正在输入..." 状态。
   * <p>
   * 给 UI 组件用的 selector，同步检查过期时间戳避免脱离每 500ms GC 扫描的 race condition。
   */
  function isUserTyping(userId: number): boolean {
    const exp = typingUsers.value[userId]
    return typeof exp === 'number' && exp > Date.now()
  }

  function initWebSocket() {
    connectWebSocket(
      (msg: any) => { handleIncomingMessage(msg) },
      (status: any) => { handleStatusChange(status) },
      // 重连回调：先补拉漏消息（关键业务数据），再同步在线状态（体验增强）
      async () => {
        await pullIncrementalMessages()
        syncAllOnlineStatus()
      },
      (receipt: any) => { handleReadReceipt(receipt) },
      (event: any) => { handleMessageEvent(event) },
      (event: any) => { handleTypingEvent(event) }
    )
    wsConnected.value = true
    // 启动 typing TTL 垃圾回收（幂等：如已存在先清）
    if (_typingGcTimer) clearInterval(_typingGcTimer)
    _typingGcTimer = setInterval(_gcTypingUsers, 500)
  }

  function destroyWebSocket() {
    disconnectWebSocket()
    wsConnected.value = false
    if (_typingGcTimer) {
      clearInterval(_typingGcTimer)
      _typingGcTimer = null
    }
    typingUsers.value = {}
  }

  function reset() {
    contacts.value = []
    conversations.value = []
    archivedConversations.value = []
    currentConversationId.value = null
    currentMessages.value = []
    unreadTotal.value = 0
    maxSeenMessageId.value = 0
    typingUsers.value = {}
    destroyWebSocket()
  }

  return {
    contacts, conversations, archivedConversations,
    currentConversationId, currentMessages, unreadTotal, wsConnected,
    onlineStatusMap, maxSeenMessageId,
    currentConversation,
    loadContacts, loadConversations, loadArchivedConversations, loadMessages,
    openConversation, sendMessage, sendMessagesBatch, resendMessage,
    handleIncomingMessage, handleReadReceipt, handleMessageEvent, refreshUnreadCount,
    pullIncrementalMessages,
    // L3
    recallMyMessage, adminDeleteMessage, hideConversationLocal, unhideConversationLocal,
    checkUserOnline, syncAllOnlineStatus, isUserOnline,
    initWebSocket, destroyWebSocket, reset,
    // L3-M0-6：Typing Indicator
    typingUsers, handleTypingEvent, isUserTyping,
    // L3-M0-7：会话置顶 + 免打扰
    setConversationPinnedLocal, setConversationMutedLocal
  }
})
