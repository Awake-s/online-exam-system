import request from '@/utils/http'

export function getExamList(params: any) {
  return request.get<any>({ url: '/api/exam/list', params })
}
export function publishExam(data: any) {
  return request.post<any>({ url: '/api/exam/add', params: data })
}
export function updateExam(id: number, data: any) {
  return request.put<any>({ url: `/api/exam/update/${id}`, params: data })
}
export function deleteExam(id: number) {
  return request.del<any>({ url: `/api/exam/${id}` })
}
export function getExamRecords(examId: number) {
  return request.get<any>({ url: `/api/exam/records/${examId}` })
}
