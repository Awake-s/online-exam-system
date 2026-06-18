import { AppRouteRecord } from '@/types/router'

/** 首页（独立一级菜单，无子路由叶节点，框架自动用 Layout 包裹） */
export const studentHomeRoute: AppRouteRecord = {
  name: 'StudentHome',
  path: '/student-home',
  component: '/student/home',
  meta: {
    title: 'menus.student.home',
    icon: 'ri:home-smile-2-line',
    roles: ['R_STUDENT'],
    keepAlive: false,
    fixedTab: true
  }
}

/** 在线考试：我的考试 */
export const studentRoutes: AppRouteRecord = {
  name: 'Student',
  path: '/student',
  component: '/index/index',
  meta: {
    title: 'menus.student.title',
    icon: 'ri:draft-line',
    roles: ['R_STUDENT']
  },
  children: [
    {
      path: 'exam',
      name: 'MyExam',
      component: '/student/my-exam',
      meta: {
        title: 'menus.student.exam',
        icon: 'ri:draft-line',
        keepAlive: true
      }
    },
    {
      path: 'exam/do/:examId',
      name: 'ExamPage',
      component: '/student/exam-page',
      meta: {
        title: 'menus.student.examPage',
        icon: 'ri:edit-2-line',
        isHide: true,
        isHideTab: true,
        keepAlive: false,
        isFullPage: true
      }
    }
    // profile / notification 已剥离为共享一级路由 /profile、/notification
    // 见 src/router/modules/shared.ts
  ]
}

/** 成绩中心：我的成绩 + 错题本 */
export const studyCenterRoutes: AppRouteRecord = {
  name: 'StudyCenter',
  path: '/my-study',
  component: '/index/index',
  meta: {
    title: 'menus.student.gradeCenter',
    icon: 'ri:trophy-line',
    roles: ['R_STUDENT']
  },
  children: [
    {
      path: 'score',
      name: 'MyScore',
      component: '/student/my-score',
      meta: {
        title: 'menus.student.score',
        icon: 'ri:medal-line',
        keepAlive: true
      }
    },
    {
      path: 'score/:recordId',
      name: 'ScoreDetail',
      component: '/student/score-detail',
      meta: {
        title: 'menus.student.scoreDetail',
        icon: 'ri:file-chart-line',
        isHide: true,
        keepAlive: false
      }
    },
    {
      path: 'wrong',
      name: 'WrongBook',
      component: '/student/wrong-book',
      meta: {
        title: 'menus.student.wrong',
        icon: 'ri:error-warning-line',
        keepAlive: true
      }
    }
  ]
}
