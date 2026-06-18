import request from '@/utils/http'

export function getWrongSubjects() {
  return request.get<any>({ url: '/api/wrong/subjects' })
}
export function getWrongExams(subjectId: number) {
  return request.get<any>({ url: '/api/wrong/exams', params: { subjectId } })
}
export function getWrongTypeCounts(params: any) {
  return request.get<any>({ url: '/api/wrong/type-counts', params })
}
export function getWrongList(params: any) {
  return request.get<any>({ url: '/api/wrong/list', params })
}
export function getWrongDetail(answerId: number) {
  return request.get<any>({ url: `/api/wrong/detail/${answerId}` })
}
export function removeWrong(answerId: number) {
  return request.del<any>({ url: `/api/wrong/${answerId}` })
}
