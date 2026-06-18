import { AppRouteRecord } from '@/types/router'
import { adminHomeRoute, adminSystemRoutes, adminAcademicRoutes } from './admin'
import { teacherHomeRoute, teacherRoutes, examCenterRoutes } from './teacher'
import { studentHomeRoute, studentRoutes, studyCenterRoutes } from './student'
import { notificationRoute, profileRoute } from './shared'

/**
 * 导出所有模块化路由
 *
 * 路由分层：
 * - 业务菜单：adminXxx / teacherXxx / studentXxx（按角色隔离，各自一个业务父菜单组）
 * - 账号级快捷页（共享）：notificationRoute / profileRoute
 *   顶层无 children 结构 → RouteTransformer 自动识别为一级路由 → 面包屑只显示当前页
 */
export const routeModules: AppRouteRecord[] = [
  adminHomeRoute,
  adminSystemRoutes,
  adminAcademicRoutes,
  teacherHomeRoute,
  teacherRoutes,
  examCenterRoutes,
  studentHomeRoute,
  studentRoutes,
  studyCenterRoutes,
  notificationRoute,
  profileRoute
]
