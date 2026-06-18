/**
 * L3-M0-8 + CHAT-325：聊天会话草稿持久化（纯前端 localStorage）。
 * <p>
 * <b>架构演进（CHAT-325 - 零会话草稿根治）</b>：
 * <ul>
 *   <li><b>旧架构</b>：key = {@code chat_draft_{userId}_{convId}}，value = plain string</li>
 *   <li><b>新架构</b>：key = {@code chat_draft_{userId}_p{peerUserId}}，value = JSON {@link DraftRecord}</li>
 * </ul>
 *
 * <b>为什么从 convId-keyed 迁移到 peer-keyed？</b>
 * <p>
 * {@code conversationId} 是<b>派生标识</b>（首条消息发送后后端才生成），
 * 导致"未发过消息的会话"无法持久化草稿 —— 这是 Signal 自 2018 年至今未修复的著名 bug
 * （<a href="https://github.com/signalapp/Signal-Android/issues/7612">Signal #7612</a>）。
 * <p>
 * {@code peerUserId} 是<b>稳定标识</b>，用户选择联系人的瞬间即存在，永不变更、
 * 跨会话状态（有/无历史）一致、跨登出登录保留。因此草稿业务主键必须是 peerUserId。
 *
 * <b>业界对标（2024-2025 权威调研）</b>：
 * <ul>
 *   <li><b>Telegram</b>: peer-based DraftMessage（core.telegram.org/api/drafts）</li>
 *   <li><b>WhatsApp</b>: chat_jid-based local drafts</li>
 *   <li><b>微信 PC</b>: {my_wxid}_{peer_wxid}-based local drafts</li>
 *   <li><b>Slack</b>: virtual channel/dm_id-based + cloud-synced</li>
 * </ul>
 *
 * 本项目选择 peer-keyed + 本地存储（对齐 WhatsApp/微信 PC 级别），
 * 跨设备云同步为未来可选增强（需后端 API + DB 字段）。
 *
 * <b>核心设计</b>：
 * <ul>
 *   <li>按 {userId, peerUserId} 独立存储，跨账号天然隔离</li>
 *   <li>JSON 富结构：content + peerUserName + peerUserAvatar + updatedAt，
 *       让"未建立会话的对话"也能渲染 ghost 会话卡片（对齐 WhatsApp/微信 PC）</li>
 *   <li>debounce 500ms 写入，减少 localStorage 抖动</li>
 *   <li>切会话/发送成功时立即清除（不等 debounce）</li>
 *   <li>单会话 10KB 截断，防 localStorage 5MB 总量溢出</li>
 *   <li>向前兼容：legacy convId-keyed 草稿在首次 loadConversations 后通过
 *       {@link migrateLegacyDraftsToPeerKeyed} 一次性迁移</li>
 * </ul>
 */

/**
 * 草稿存储结构（peer-keyed 新格式 JSON value）。
 * <p>
 * 只需 content 即可完成"输入框回填"最小业务；peerUserName/peerUserAvatar 用于
 * ghost 会话卡片渲染（当该 peer 尚未建立服务端会话时）；updatedAt 用于列表排序。
 */
export interface DraftRecord {
  /** 草稿正文（已 trim，非空） */
  content: string
  /** 对方用户 ID（稳定标识 — 草稿业务主键） */
  peerUserId: number
  /** 对方显示名（用于 ghost card；缺失时 UI 层 fallback "联系人"） */
  peerUserName?: string
  /** 对方头像 URL（用于 ghost card） */
  peerUserAvatar?: string
  /** 最后更新毫秒时间戳（用于会话列表按时间倒排） */
  updatedAt: number
}

/** 单会话草稿最大字符数（UTF-16 下 10KB ≈ 5000 char） */
const MAX_DRAFT_LENGTH = 5000

// ======================================================================
//  Peer-keyed API（主 API，新代码应使用）
// ======================================================================

/** Peer-keyed localStorage key 构造器。注意带 {@code _p} 前缀以区分 legacy 格式 */
function buildPeerKey(userId: number | string, peerUserId: number | string): string {
  return `chat_draft_${userId}_p${peerUserId}`
}

/** Peer-keyed debounce 定时器（按 key 独立） */
const _pendingPeerTimers: Record<string, ReturnType<typeof setTimeout>> = {}

/**
 * 异步保存 peer-keyed 草稿（debounce 500ms）。
 * <p>
 * 输入框 @input 高频调用时使用 — 最后一次调用内容在 500ms 内写入。
 */
export function saveDraftByPeerDebounced(
  userId: number | string,
  peerUserId: number | string,
  content: string,
  meta?: { peerUserName?: string; peerUserAvatar?: string },
  delayMs = 500,
): void {
  if (!userId || !peerUserId) return
  const key = buildPeerKey(userId, peerUserId)
  if (_pendingPeerTimers[key]) clearTimeout(_pendingPeerTimers[key])
  _pendingPeerTimers[key] = setTimeout(() => {
    saveDraftByPeer(userId, peerUserId, content, meta)
    delete _pendingPeerTimers[key]
  }, delayMs)
}

/**
 * 立即写入 peer-keyed 草稿（同步）。
 * <p>
 * 用于切会话、关闭窗口、pagehide 等"必须立即落盘"的场景，绕过 debounce。
 * <p>
 * 空串输入会清除草稿条目（对齐 WhatsApp 清空后自动删除草稿的行为）。
 */
export function saveDraftByPeer(
  userId: number | string,
  peerUserId: number | string,
  content: string,
  meta?: { peerUserName?: string; peerUserAvatar?: string },
): void {
  if (!userId || !peerUserId) return
  const key = buildPeerKey(userId, peerUserId)
  const trimmed = (content ?? '').trim()
  try {
    if (trimmed.length === 0) {
      if (_pendingPeerTimers[key]) {
        clearTimeout(_pendingPeerTimers[key])
        delete _pendingPeerTimers[key]
      }
      localStorage.removeItem(key)
      return
    }
    const truncated = trimmed.length > MAX_DRAFT_LENGTH
      ? trimmed.slice(0, MAX_DRAFT_LENGTH)
      : trimmed
    const record: DraftRecord = {
      content: truncated,
      peerUserId: Number(peerUserId),
      peerUserName: meta?.peerUserName,
      peerUserAvatar: meta?.peerUserAvatar,
      updatedAt: Date.now(),
    }
    localStorage.setItem(key, JSON.stringify(record))
  } catch (e) {
    // QuotaExceededError 降级：静默失败，不打断聊天主流程（对齐 WhatsApp/Telegram Web）
    console.warn('[chatDraft] 保存草稿失败（localStorage 可能已满）:', e)
  }
}

/**
 * 读取 peer-keyed 草稿。找不到或解析失败返回 {@code null}。
 */
export function loadDraftByPeer(
  userId: number | string,
  peerUserId: number | string,
): DraftRecord | null {
  if (!userId || !peerUserId) return null
  const key = buildPeerKey(userId, peerUserId)
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const record = JSON.parse(raw) as DraftRecord
    // 防御性校验：格式损坏直接视为无草稿（不崩溃）
    if (typeof record?.content !== 'string') return null
    return record
  } catch {
    return null
  }
}

/**
 * 清除 peer-keyed 草稿（发送成功 / 用户清空输入框等场景）。
 * <p>
 * 同时取消 pending debounce 定时器，避免"清除后又被 debounce 写回"。
 */
export function clearDraftByPeer(
  userId: number | string,
  peerUserId: number | string,
): void {
  if (!userId || !peerUserId) return
  const key = buildPeerKey(userId, peerUserId)
  if (_pendingPeerTimers[key]) {
    clearTimeout(_pendingPeerTimers[key])
    delete _pendingPeerTimers[key]
  }
  try {
    localStorage.removeItem(key)
  } catch {
    /* ignore */
  }
}

// ======================================================================
//  Legacy convId-keyed API（已废弃，仅保留兼容旧版存量草稿的读取）
//  新代码禁止使用，一律改用上方 *ByPeer API
// ======================================================================

/** Legacy convId-keyed localStorage key 构造器（无 _p 前缀） */
function buildLegacyKey(userId: number | string, conversationId: number | string): string {
  return `chat_draft_${userId}_${conversationId}`
}

/** Legacy debounce 定时器（仅迁移过渡期使用） */
const _pendingLegacyTimers: Record<string, ReturnType<typeof setTimeout>> = {}

/** @deprecated CHAT-325: 使用 {@link saveDraftByPeerDebounced} 代替 */
export function saveDraftDebounced(
  userId: number | string,
  conversationId: number | string,
  content: string,
  delayMs = 500,
): void {
  if (!userId || !conversationId) return
  const key = buildLegacyKey(userId, conversationId)
  if (_pendingLegacyTimers[key]) clearTimeout(_pendingLegacyTimers[key])
  _pendingLegacyTimers[key] = setTimeout(() => {
    saveDraftImmediate(userId, conversationId, content)
    delete _pendingLegacyTimers[key]
  }, delayMs)
}

/** @deprecated CHAT-325: 使用 {@link saveDraftByPeer} 代替 */
export function saveDraftImmediate(
  userId: number | string,
  conversationId: number | string,
  content: string,
): void {
  if (!userId || !conversationId) return
  const key = buildLegacyKey(userId, conversationId)
  const trimmed = (content ?? '').trim()
  try {
    if (trimmed.length === 0) {
      localStorage.removeItem(key)
      return
    }
    const truncated = trimmed.length > MAX_DRAFT_LENGTH
      ? trimmed.slice(0, MAX_DRAFT_LENGTH)
      : trimmed
    localStorage.setItem(key, truncated)
  } catch (e) {
    console.warn('[chatDraft] 保存草稿失败 (legacy):', e)
  }
}

/** @deprecated CHAT-325: 使用 {@link loadDraftByPeer} 代替 */
export function loadDraft(
  userId: number | string,
  conversationId: number | string,
): string {
  if (!userId || !conversationId) return ''
  const key = buildLegacyKey(userId, conversationId)
  try {
    return localStorage.getItem(key) ?? ''
  } catch {
    return ''
  }
}

/** @deprecated CHAT-325: 使用 {@link clearDraftByPeer} 代替 */
export function clearDraft(
  userId: number | string,
  conversationId: number | string,
): void {
  if (!userId || !conversationId) return
  const key = buildLegacyKey(userId, conversationId)
  if (_pendingLegacyTimers[key]) {
    clearTimeout(_pendingLegacyTimers[key])
    delete _pendingLegacyTimers[key]
  }
  try {
    localStorage.removeItem(key)
  } catch {
    /* ignore */
  }
}

// ======================================================================
//  批量读取 / 迁移
// ======================================================================

/**
 * 批量读取指定用户的所有 <b>peer-keyed</b> 草稿（新格式）。
 * <p>
 * 用途：会话列表渲染"[草稿]"前缀 + ghost 会话卡片合成。
 * <p>
 * 性能：localStorage 全表扫描 O(n)；n 为 localStorage 条目总数（通常 < 100），
 * 一次调用成本 < 1ms。
 *
 * @returns Map&lt;peerUserId, DraftRecord&gt;
 */
export function loadAllDraftsOfUser(userId: number | string): Map<number, DraftRecord> {
  const result = new Map<number, DraftRecord>()
  if (!userId) return result
  const prefix = `chat_draft_${userId}_p`
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (!key || !key.startsWith(prefix)) continue
      const peerIdStr = key.slice(prefix.length)
      const peerUserId = Number(peerIdStr)
      if (!Number.isFinite(peerUserId)) continue
      const raw = localStorage.getItem(key)
      if (!raw) continue
      try {
        const record = JSON.parse(raw) as DraftRecord
        if (record?.content && record.content.trim().length > 0) {
          result.set(peerUserId, record)
        }
      } catch {
        // 格式损坏条目忽略（下次 save 时会覆盖修复）
      }
    }
  } catch (e) {
    console.warn('[chatDraft] 批量读取草稿失败:', e)
  }
  return result
}

/**
 * 一次性迁移 legacy convId-keyed 草稿到 peer-keyed 格式。
 * <p>
 * <b>调用时机</b>：首次 loadConversations 完成后（需要 convId → peerUserId 映射）。
 * <b>幂等</b>：可重复调用；已迁移过的会话不会重复处理。
 * <p>
 * <b>算法</b>：
 * <ol>
 *   <li>扫描 localStorage 所有 {@code chat_draft_{userId}_{plainNumber}} 格式 key
 *       （排除 {@code chat_draft_{userId}_p{number}} 新格式）</li>
 *   <li>对每个 legacy key，通过 convIdToPeer 映射找到 peerUserId + 对方信息</li>
 *   <li>若映射存在 + 新 key 未占用（或新 key 空内容）→ 写入新 key，删除旧 key</li>
 *   <li>若映射不存在（会话已被服务端删除 / 不在首屏列表）→ 保留 legacy key 不处理</li>
 *   <li>若新 key 已有非空内容（用户在新格式下又新写过）→ 丢弃 legacy（新的胜出）</li>
 * </ol>
 *
 * @param userId 当前用户 ID
 * @param convIdToPeer  convId → {peerUserId, peerUserName, peerUserAvatar} 映射
 * @returns 本次实际迁移的条目数
 */
export function migrateLegacyDraftsToPeerKeyed(
  userId: number | string,
  convIdToPeer: Map<number, { peerUserId: number; peerUserName?: string; peerUserAvatar?: string }>,
): number {
  if (!userId) return 0
  const userPrefix = `chat_draft_${userId}_`
  const peerPrefix = `chat_draft_${userId}_p`
  let migrated = 0
  try {
    // 先收集后处理：避免迭代中修改 localStorage 导致 index 错位
    const toMigrate: Array<{ oldKey: string; convId: number; content: string }> = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (!key) continue
      if (!key.startsWith(userPrefix)) continue
      if (key.startsWith(peerPrefix)) continue // 跳过新格式
      const convIdStr = key.slice(userPrefix.length)
      const convId = Number(convIdStr)
      if (!Number.isFinite(convId)) continue
      const value = localStorage.getItem(key)
      if (!value) continue
      toMigrate.push({ oldKey: key, convId, content: value })
    }

    for (const { oldKey, convId, content } of toMigrate) {
      const peer = convIdToPeer.get(convId)
      if (!peer) continue // 孤儿 legacy，保留不处理（后续若 conv 加载进来可再次迁移）
      const newKey = buildPeerKey(userId, peer.peerUserId)
      const existing = localStorage.getItem(newKey)
      if (existing) {
        try {
          const rec = JSON.parse(existing) as DraftRecord
          if (rec?.content && rec.content.trim().length > 0) {
            // 新格式已有非空草稿（更晚写入）→ 新的胜出，直接丢弃 legacy
            localStorage.removeItem(oldKey)
            continue
          }
        } catch {
          /* 新 key 格式损坏，直接覆盖 */
        }
      }
      const record: DraftRecord = {
        content,
        peerUserId: peer.peerUserId,
        peerUserName: peer.peerUserName,
        peerUserAvatar: peer.peerUserAvatar,
        updatedAt: Date.now(),
      }
      try {
        localStorage.setItem(newKey, JSON.stringify(record))
        localStorage.removeItem(oldKey)
        migrated++
      } catch (e) {
        // 配额失败：保留旧 key，下次重试（绝不丢数据）
        console.warn('[chatDraft] 迁移写入失败，保留 legacy key:', e)
      }
    }
  } catch (e) {
    console.warn('[chatDraft] 迁移 legacy 草稿失败:', e)
  }
  return migrated
}

/**
 * 清除当前用户所有草稿（peer-keyed + legacy 全部清除）。
 * <p>
 * <b>注意：此函数当前不在登出流程中被调用</b>（CHAT-308 bug-fix 后解耦）。
 * <p>
 * 本项目采用「本地保留派」草稿架构（对齐微信 PC / Discord / Slack）：
 * 登出仅清 token + 用户信息，草稿留在 localStorage 里让同账号重登可恢复；
 * 跨账号安全由 {@code chat_draft_${userId}_} 命名空间隔离天然保证。
 * <p>
 * 此函数作为<b>备用 API 保留</b>，供未来可能的"用户主动清除所有草稿"
 * 功能调用（例如个人设置页的"清空聊天草稿"按钮，类 Discord 隐私设置）。
 */
export function clearAllDraftsOfUser(userId: number | string): void {
  if (!userId) return
  const prefix = `chat_draft_${userId}_`
  try {
    const keysToDelete: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith(prefix)) keysToDelete.push(key)
    }
    keysToDelete.forEach(k => localStorage.removeItem(k))
  } catch (e) {
    console.warn('[chatDraft] 清除所有草稿失败:', e)
  }
}
