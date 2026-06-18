import request from '@/utils/http'

export function getUserList(params: any) {
  return request.get<any>({ url: '/api/user/list', params })
}
export function addUser(data: any) {
  return request.post<any>({ url: '/api/user/add', params: data })
}
export function updateUser(data: any) {
  return request.put<any>({ url: '/api/user/update', params: data })
}
export function deleteUser(id: number) {
  return request.del<any>({ url: `/api/user/${id}` })
}
export function updateUserStatus(id: number, data: any) {
  return request.put<any>({ url: `/api/user/status/${id}`, params: data })
}
export function resetPassword(id: number) {
  return request.put<any>({ url: `/api/user/reset-password/${id}` })
}
