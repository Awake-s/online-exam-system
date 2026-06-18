import request from '@/utils/http'

export function getSubjectList(params: any) {
  return request.get<any>({ url: '/api/subject/list', params })
}
export function addSubject(data: any) {
  return request.post<any>({ url: '/api/subject/add', params: data })
}
export function updateSubject(id: number, data: any) {
  return request.put<any>({ url: `/api/subject/update/${id}`, params: data })
}
export function deleteSubject(id: number) {
  return request.del<any>({ url: `/api/subject/${id}` })
}
export function getAllSubjects() {
  return request.get<any>({ url: '/api/subject/all' })
}
