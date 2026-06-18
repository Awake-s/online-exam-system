import { AppRouteRecord } from '@/types/router'

/** 首页（独立一级菜单，无子路由叶节点，框架自动用 Layout 包裹） */
export const adminHomeRoute: AppRouteRecord = {
  name: 'AdminHome',
  path: '/admin-home',
  component: '/admin/home',
  meta: {
    title: 'menus.admin.home',
    icon: 'ri:home-smile-2-line',
    roles: ['R_ADMIN'],
    keepAlive: false,
    fixedTab: true
  }
}

/** 系统管理：用户管理 */
export const adminSystemRoutes: AppRouteRecord = {
  name: 'Admin',
  path: '/admin',
  component: '/index/index',
  meta: {
    title: 'menus.admin.title',
    icon: 'ri:settings-3-line',
    roles: ['R_ADMIN']
  },
  children: [
    {
      path: 'user',
      name: 'UserManage',
      component: '/admin/user-manage',
      meta: {
        title: 'menus.admin.user',
        icon: 'ri:user-settings-line',
        keepAlive: true
      }
    }
    // profile / notification 已剥离为共享一级路由 /profile、/notification
    // 见 src/router/modules/shared.ts
  ]
}

/** 教务管理：班级管理 + 科目管理 */
export const adminAcademicRoutes: AppRouteRecord = {
  name: 'Academic',
  path: '/academic',
  component: '/index/index',
  meta: {
    title: 'menus.admin.academic',
    icon: 'ri:building-2-line',
    roles: ['R_ADMIN']
  },
  children: [
    {
      path: 'major',
      name: 'MajorManage',
      component: '/admin/major-manage',
      meta: {
        title: 'menus.admin.major',
        icon: 'ri:graduation-cap-line',
        keepAlive: true
      }
    },
    {
      path: 'class',
      name: 'ClassManage',
      component: '/admin/class-manage',
      meta: {
        title: 'menus.admin.class',
        icon: 'ri:team-line',
        keepAlive: true
      }
    },
    {
      path: 'subject',
      name: 'SubjectManage',
      component: '/admin/subject-manage',
      meta: {
        title: 'menus.admin.subject',
        icon: 'ri:book-2-line',
        keepAlive: true
      }
    }
  ]
}
