import request from '@/utils/http'

export function getNotificationList(params: {
  page: number
  size: number
  type?: string
  isRead?: 0 | 1
}) {
  return request.get<any>({ url: '/api/notification/list', params })
}

export function getUnreadCount() {
  return request.get<any>({ url: '/api/notification/unread-count' })
}

export function markAsRead(id: number) {
  return request.put<any>({ url: `/api/notification/read/${id}` })
}

export function markAllAsRead() {
  return request.put<any>({ url: '/api/notification/read-all' })
}

/** 删除单条通知 */
export function deleteNotification(id: number) {
  return request.del<any>({ url: `/api/notification/${id}` })
}

/** 批量删除通知（单次最多 100 条） */
export function batchDeleteNotifications(ids: number[]) {
  return request.del<any>({ url: '/api/notification/batch', data: { ids } })
}

/** 批量标记已读（单次最多 100 条） */
export function batchMarkAsRead(ids: number[]) {
  return request.put<any>({ url: '/api/notification/batch-read', data: { ids } })
}

export function getPendingItems() {
  return request.get<any>({ url: '/api/notification/pending' })
}
