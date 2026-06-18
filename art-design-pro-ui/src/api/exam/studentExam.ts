import request from '@/utils/http'

export function getMyExams() {
  return request.get<any>({ url: '/api/student/exam/my-exams' })
}
export function startExam(examId: number) {
  return request.get<any>({ url: `/api/student/exam/start/${examId}` })
}
export function submitExam(data: any) {
  return request.post<any>({ url: '/api/student/exam/submit', params: data })
}
export function getExamResult(recordId: number) {
  return request.get<any>({ url: `/api/student/exam/result/${recordId}` })
}
export function autoSaveAnswers(data: any) {
  return request.post<any>({ url: '/api/student/exam/auto-save', params: data })
}
export function recordSwitchScreen(recordId: number) {
  return request.post<any>({ url: `/api/student/exam/switch-screen/${recordId}` })
}
