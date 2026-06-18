export const questionTypeMap: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
export const difficultyMap: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
export const difficultyTagType: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'danger' }

export function formatDateTime(dt: string | null | undefined): string {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 16)
}
