import request from '@/utils/http'

export function getQuestionList(params: any) {
  return request.get<any>({ url: '/api/question/list', params })
}
export function getQuestionDetail(id: number) {
  return request.get<any>({ url: `/api/question/${id}` })
}
export function addQuestion(data: any) {
  return request.post<any>({ url: '/api/question/add', params: data })
}
export function updateQuestion(id: number, data: any) {
  return request.put<any>({ url: `/api/question/update/${id}`, params: data })
}
export function deleteQuestion(id: number) {
  return request.del<any>({ url: `/api/question/${id}` })
}
export function batchDeleteQuestions(ids: number[]) {
  return request.del<any>({ url: '/api/question/batch', data: ids })
}
export function importQuestions(formData: FormData) {
  return request.post<any>({
    url: '/api/question/import',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
