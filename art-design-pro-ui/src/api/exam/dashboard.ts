import request from '@/utils/http'

export function getAdminDashboard() {
  return request.get<any>({ url: '/api/dashboard/admin' })
}
export function getTeacherDashboard() {
  return request.get<any>({ url: '/api/dashboard/teacher' })
}
export function getStudentDashboard() {
  return request.get<any>({ url: '/api/dashboard/student' })
}
export function getStudentScoreTrend(params: any) {
  return request.get<any>({ url: '/api/dashboard/student-trend', params })
}
