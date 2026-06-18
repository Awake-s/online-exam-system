import request from '@/utils/http'

export function getChatContacts() {
  return request.get<any>({ url: '/api/chat/contacts' })
}
/**
 * 获取会话列表。
 * @param archived 归档过滤开关。默认 false=主列表（未归档），true="已归档"视图
 *                 与后端 L3-M1-4 保持一致，后端以 user?_hidden=1 为归档标识
 */
export function getChatConversations(archived = false) {
  return request.get<any>({ url: '/api/chat/conversations', params: { archived } })
}
export function getChatMessages(conversationId: number, params: any) {
  return request.get<any>({ url: `/api/chat/conversations/${conversationId}/messages`, params })
}
export function sendChatMessage(data: { receiverId: number; content: string; clientMsgId?: string }) {
  return request.post<any>({ url: '/api/chat/messages', params: data })
}

/**
 * L3-M3-1：原子批量发送（对齐 Slack chat.postMessage batch / Telegram sendMediaGroup / Discord 单消息多附件）。
 * <p>
 * 关键语义：
 * - 批次级限流：整批 1 permit（后端 ChatConstants.RATE_LIMIT_STUDENT_PER_SECOND），根治"批量发 5 文件第 3 条起被限"
 * - Partial Success：返回 results 数组，每项 {clientMsgId, status: 'sent'|'already_sent'|'failed', message?, error?}
 * - per-item 幂等：每项独立 clientMsgId；重复批次自动命中已有消息返回 already_sent
 * - 上限：单批 ≤ 20 条（后端 ChatConstants.MAX_BATCH_SEND_SIZE）
 */
export interface BatchSendItem { clientMsgId: string; content: string }
export interface BatchSendItemResult {
  clientMsgId: string
  status: 'sent' | 'already_sent' | 'failed'
  message?: any
  error?: string
}
export interface BatchSendResponse { results: BatchSendItemResult[] }
export function sendChatMessagesBatch(data: { receiverId: number; items: BatchSendItem[] }) {
  return request.post<BatchSendResponse>({ url: '/api/chat/messages:batch', params: data })
}
export function markConversationRead(conversationId: number) {
  return request.put<any>({ url: `/api/chat/conversations/${conversationId}/read` })
}
export function getChatUnreadCount() {
  return request.get<any>({ url: '/api/chat/unread-count' })
}
/**
 * I2：WS 重连后补拉漏消息。
 * @param params.sinceMessageId 本地已有的最大 message.id，null/undefined 表示从头拉（首次连接通常不应调用此接口）
 * @param params.limit 单次最多 500 条，默认 200
 */
export function getIncrementalMessages(params: { sinceMessageId?: number; limit?: number }) {
  return request.get<any[]>({ url: '/api/chat/messages/incremental', params })
}
/**
 * L3：发送者撤回自己发出的消息（2 分钟内有效）。
 * 成功后后端通过 WS 推送 MESSAGE_DELETED 给会话双方。
 */
export function recallChatMessage(messageId: number) {
  return request.post<any>({ url: `/api/chat/messages/${messageId}/recall` })
}
/**
 * L3-M0-4：获取撤回消息的"重新编辑"草稿（对齐微信/QQ 2 分钟内可编辑）。
 * <p>
 * 返回 { content: string | null }：
 * - content !== null：草稿有效，前端回填到输入框
 * - content === null：过期（超 2min）或非文字消息，前端应隐藏"重新编辑"按钮
 * <p>
 * 后端权限：仅原发送者可访问；非发送者会得到业务错误。
 */
export function getRecallDraft(messageId: number) {
  return request.get<{ content: string | null }>({ url: `/api/chat/messages/${messageId}/recall-draft` })
}
/**
 * L3：管理员强制删除任意消息（无时限，服务端会写审计日志）。
 */
export function adminDeleteChatMessage(messageId: number) {
  return request.post<any>({ url: `/api/chat/messages/${messageId}/admin-delete` })
}
/**
 * L3：当前用户归档会话（即"从主列表隐藏"，对方不受影响）。
 * L3-M1-4：语义调整为"归档"而非"删除"—— 对应 Telegram Archive 模式：
 * 会话仍可通过"已归档"视图找回；收到新消息时自动解除归档。
 */
export function hideChatConversation(conversationId: number) {
  return request.del<any>({ url: `/api/chat/conversations/${conversationId}` })
}

/**
 * L3-M1-4：取消归档，把会话从"已归档"视图恢复到主列表（对称 {@link hideChatConversation}）。
 */
export function unhideChatConversation(conversationId: number) {
  return request.put<any>({ url: `/api/chat/conversations/${conversationId}/unhide` })
}

/**
 * L3-M0-7：会话置顶（对齐微信 / WhatsApp / Telegram Pin Chat）。
 * <p>
 * HTTP 语义：PUT 幂等；已置顶时再次调用无害。
 * 约束：每用户最多置顶 5 个会话（后端 ChatConstants.MAX_PINNED_CONVERSATIONS），
 * 超过时后端抛业务异常，前端应捕获并提示用户。
 */
export function pinChatConversation(conversationId: number) {
  return request.put<any>({ url: `/api/chat/conversations/${conversationId}/pin` })
}

/**
 * L3-M0-7：会话取消置顶（对称 {@link pinChatConversation}）。
 */
export function unpinChatConversation(conversationId: number) {
  return request.put<any>({ url: `/api/chat/conversations/${conversationId}/unpin` })
}

/**
 * L3-M0-7：会话免打扰（对齐 WhatsApp Mute / 微信消息免打扰）。
 * <p>
 * 语义：消息正常收取 + 未读数正常显示，但前端不触发桌面通知 / 声音提醒。
 */
export function muteChatConversation(conversationId: number) {
  return request.put<any>({ url: `/api/chat/conversations/${conversationId}/mute` })
}

/**
 * L3-M0-7：会话取消免打扰（对称 {@link muteChatConversation}）。
 */
export function unmuteChatConversation(conversationId: number) {
  return request.put<any>({ url: `/api/chat/conversations/${conversationId}/unmute` })
}
export function uploadChatImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any>({ url: '/api/upload/image', params: formData })
}
export function uploadChatFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any>({ url: '/api/upload/file', params: formData })
}
export function getUserOnlineStatus(userId: number) {
  return request.get<any>({ url: `/api/chat/online-status/${userId}` })
}
export function batchGetOnlineStatus(userIds: number[]) {
  return request.post<any>({ url: '/api/chat/online-status/batch', params: userIds })
}
export function speechRecognize(audioBlob: Blob) {
  const formData = new FormData()
  formData.append('audio', audioBlob, 'recording.wav')
  return request.post<any>({ url: '/api/speech/recognize', params: formData })
}
