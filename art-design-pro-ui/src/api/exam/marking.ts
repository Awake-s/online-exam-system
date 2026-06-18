import request from '@/utils/http'

export function getPendingList(examId: number) {
  return request.get<any>({ url: `/api/marking/list/${examId}` })
}
export function getMarkingDetail(recordId: number) {
  return request.get<any>({ url: `/api/marking/detail/${recordId}` })
}
export function markScores(data: any) {
  return request.post<any>({ url: '/api/marking/score', params: data })
}
export function publishScores(examId: number) {
  return request.post<any>({ url: `/api/marking/publish/${examId}` })
}
