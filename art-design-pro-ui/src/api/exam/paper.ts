import request from '@/utils/http'

export function getPaperList(params: any) {
  return request.get<any>({ url: '/api/paper/list', params })
}
export function getPaperDetail(id: number) {
  return request.get<any>({ url: `/api/paper/${id}` })
}
export function createPaper(data: any) {
  return request.post<any>({ url: '/api/paper/add', params: data })
}
export function updatePaper(id: number, data: any) {
  return request.put<any>({ url: `/api/paper/update/${id}`, params: data })
}
export function randomPaper(data: any) {
  return request.post<any>({ url: '/api/paper/random', params: data })
}
export function deletePaper(id: number) {
  return request.del<any>({ url: `/api/paper/${id}` })
}
export function togglePublishPaper(id: number) {
  return request.put<any>({ url: `/api/paper/togglePublish/${id}` })
}
