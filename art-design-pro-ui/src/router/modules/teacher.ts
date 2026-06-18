import { AppRouteRecord } from '@/types/router'

/** 首页（独立一级菜单，无子路由叶节点，框架自动用 Layout 包裹） */
export const teacherHomeRoute: AppRouteRecord = {
  name: 'TeacherHome',
  path: '/teacher-home',
  component: '/teacher/home',
  meta: {
    title: 'menus.teacher.home',
    icon: 'ri:home-smile-2-line',
    roles: ['R_TEACHER'],
    keepAlive: false,
    fixedTab: true
  }
}

/** 题库中心：题库管理 + 试卷管理 + 试卷模板 */
export const teacherRoutes: AppRouteRecord = {
  name: 'Teacher',
  path: '/teacher',
  component: '/index/index',
  meta: {
    title: 'menus.teacher.title',
    icon: 'ri:bank-line',
    roles: ['R_TEACHER']
  },
  children: [
    {
      path: 'question',
      name: 'QuestionManage',
      component: '/teacher/question-manage',
      meta: {
        title: 'menus.teacher.question',
        icon: 'ri:file-text-line',
        keepAlive: true
      }
    },
    {
      path: 'paper',
      name: 'PaperManage',
      component: '/teacher/paper-manage',
      meta: {
        title: 'menus.teacher.paper',
        icon: 'ri:article-line',
        keepAlive: true
      }
    },
    {
      path: 'paper/template',
      name: 'PaperTemplate',
      component: '/teacher/paper-template',
      meta: {
        title: 'menus.teacher.paperTemplate',
        icon: 'ri:file-copy-2-line',
        keepAlive: true
      }
    },
    {
      path: 'paper/edit/:id?',
      name: 'PaperEdit',
      component: '/teacher/paper-edit',
      meta: {
        title: 'menus.teacher.paperEdit',
        icon: 'ri:edit-line',
        isHide: true,
        keepAlive: false
      }
    }
    // profile / notification 已剥离为共享一级路由 /profile、/notification
    // 见 src/router/modules/shared.ts
  ]
}

/** 考试中心：考试管理 + 阅卷管理 + 成绩管理 */
export const examCenterRoutes: AppRouteRecord = {
  name: 'ExamCenter',
  path: '/exam-center',
  component: '/index/index',
  meta: {
    title: 'menus.teacher.examCenter',
    icon: 'ri:task-line',
    roles: ['R_TEACHER']
  },
  children: [
    {
      path: 'exam',
      name: 'ExamManage',
      component: '/teacher/exam-manage',
      meta: {
        title: 'menus.teacher.exam',
        icon: 'ri:calendar-event-line',
        keepAlive: true
      }
    },
    {
      path: 'marking',
      name: 'MarkingList',
      component: '/teacher/marking-list',
      meta: {
        title: 'menus.teacher.marking',
        icon: 'ri:draft-line',
        keepAlive: true
      }
    },
    {
      path: 'marking/:recordId',
      name: 'MarkingDetail',
      component: '/teacher/marking-detail',
      meta: {
        title: 'menus.teacher.markingDetail',
        icon: 'ri:pencil-line',
        isHide: true,
        keepAlive: false
      }
    },
    {
      path: 'score',
      name: 'ScoreManage',
      component: '/teacher/score-manage',
      meta: {
        title: 'menus.teacher.score',
        icon: 'ri:bar-chart-box-line',
        keepAlive: true
      }
    },
    {
      path: 'score/analysis/:examId',
      name: 'ScoreAnalysis',
      component: '/teacher/score-analysis',
      meta: {
        title: 'menus.teacher.scoreAnalysis',
        icon: 'ri:line-chart-line',
        isHide: true,
        keepAlive: false
      }
    }
  ]
}
