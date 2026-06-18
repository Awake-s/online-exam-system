import request from '@/utils/http'

export function getMyScores(params: any) {
  return request.get<any>({ url: '/api/score/my-scores', params })
}
export function getClassScores(examId: number) {
  return request.get<any>({ url: `/api/score/class/${examId}` })
}
export function getScoreAnalysis(examId: number) {
  return request.get<any>({ url: `/api/score/analysis/${examId}` })
}
export function exportScores(examId: number) {
  return request.download(`/api/score/export/${examId}`)
}
