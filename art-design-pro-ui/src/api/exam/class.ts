import request from '@/utils/http'

export function getClassList(params: any) {
  return request.get<any>({ url: '/api/class/list', params })
}
export function getClassDetail(id: number) {
  return request.get<any>({ url: `/api/class/${id}` })
}
export function addClass(data: any) {
  return request.post<any>({ url: '/api/class/add', params: data })
}
export function updateClass(id: number, data: any) {
  return request.put<any>({ url: `/api/class/update/${id}`, params: data })
}
export function deleteClass(id: number) {
  return request.del<any>({ url: `/api/class/${id}` })
}
export function getClassStudents(classId: number) {
  return request.get<any>({ url: `/api/class/students/${classId}` })
}
export function getAllClasses() {
  return request.get<any>({ url: '/api/class/all' })
}
export function getMyClasses() {
  return request.get<any>({ url: '/api/class/my' })
}
