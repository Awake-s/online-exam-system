import request from '@/utils/http'

export function getTemplateList(params?: any) {
  return request.get<any>({ url: '/api/template/list', params })
}
export function getTemplateDetail(id: number) {
  return request.get<any>({ url: `/api/template/${id}` })
}
export function createTemplate(data: any) {
  return request.post<any>({ url: '/api/template/add', params: data })
}
export function updateTemplate(id: number, data: any) {
  return request.put<any>({ url: `/api/template/update/${id}`, params: data })
}
export function deleteTemplate(id: number) {
  return request.del<any>({ url: `/api/template/${id}` })
}
