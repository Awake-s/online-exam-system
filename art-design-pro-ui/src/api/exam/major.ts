import request from '@/utils/http'

export function getMajorList(params: any) {
  return request.get<any>({ url: '/api/major/list', params })
}
export function addMajor(data: any) {
  return request.post<any>({ url: '/api/major/add', params: data })
}
export function updateMajor(id: number, data: any) {
  return request.put<any>({ url: `/api/major/update/${id}`, params: data })
}
export function deleteMajor(id: number) {
  return request.del<any>({ url: `/api/major/${id}` })
}
export function getAllMajors() {
  return request.get<any>({ url: '/api/major/all' })
}
