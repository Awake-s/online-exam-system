import request from '@/utils/http'

export function getProfileInfo() {
  return request.get<any>({ url: '/api/profile/info' })
}
export function updateProfile(data: any) {
  return request.put<any>({ url: '/api/profile/update', params: data })
}
export function changePassword(data: any) {
  return request.put<any>({ url: '/api/profile/password', params: data })
}
export function uploadAvatar(formData: FormData) {
  return request.post<any>({
    url: '/api/profile/avatar',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    showErrorMessage: false
  } as any)
}
