/**
 * 快速入口配置
 *
 * ## 权限管理原则（单一权威源）
 *
 * 路由条目（含 `routeName`）：权限由路由表的 `meta.roles` 自动推导，
 * fastEnter 配置**无需重复声明 roles**，避免双重维护风险。
 *
 * - `MenuProcessor.filterMenuByRoles` 已在菜单加载阶段按角色过滤
 * - 未授权的路由**根本不会注册到 router 实例**
 * - `useFastEnter` 通过 `router.hasRoute(routeName)` 判定可见性
 * - 新增/修改路由的权限，只需修改路由的 `meta.roles`，此处自动同步
 *
 * 外链条目（仅含 `link`）：若需按角色控制可见性，可手动配 `roles` 字段。
 *
 * @see src/hooks/core/useFastEnter.ts matchVisibility
 * @see src/router/core/MenuProcessor.ts filterMenuByRoles
 */
import type { FastEnterConfig } from '@/types/config'

const fastEnterConfig: FastEnterConfig = {
  // 显示条件（屏幕宽度）
  minWidth: 1200,
  // 应用列表（权限由路由表 meta.roles 自动推导）
  applications: [
    // ── 教师模块 ──
    {
      name: '题库管理',
      description: '管理考试题目',
      icon: 'ri:questionnaire-line',
      iconColor: '#377dff',
      enabled: true,
      order: 1,
      routeName: 'QuestionManage'
    },
    {
      name: '试卷管理',
      description: '创建与编辑试卷',
      icon: 'ri:file-list-3-line',
      iconColor: '#ff6b6b',
      enabled: true,
      order: 2,
      routeName: 'PaperManage'
    },
    {
      name: '试卷模板',
      description: '管理试卷模板',
      icon: 'ri:file-copy-2-line',
      iconColor: '#FF9F43',
      enabled: true,
      order: 3,
      routeName: 'PaperTemplate'
    },
    {
      name: '考试管理',
      description: '发布与管理考试',
      icon: 'ri:calendar-check-line',
      iconColor: '#ffb100',
      enabled: true,
      order: 4,
      routeName: 'ExamManage'
    },
    {
      name: '阅卷管理',
      description: '批改主观题',
      icon: 'ri:pencil-ruler-2-line',
      iconColor: '#13DEB9',
      enabled: true,
      order: 5,
      routeName: 'MarkingList'
    },
    {
      name: '成绩管理',
      description: '查看与分析成绩',
      icon: 'ri:bar-chart-box-line',
      iconColor: '#7A7FFF',
      enabled: true,
      order: 6,
      routeName: 'ScoreManage'
    },
    // ── 学生模块 ──
    {
      name: '我的考试',
      description: '查看可参加的考试',
      icon: 'ri:file-edit-line',
      iconColor: '#38C0FC',
      enabled: true,
      order: 1,
      routeName: 'MyExam'
    },
    {
      name: '我的成绩',
      description: '查看历次考试成绩',
      icon: 'ri:trophy-line',
      iconColor: '#FF9F43',
      enabled: true,
      order: 2,
      routeName: 'MyScore'
    },
    {
      name: '错题本',
      description: '复习错题巩固知识',
      icon: 'ri:bookmark-line',
      iconColor: '#FB7299',
      enabled: true,
      order: 3,
      routeName: 'WrongBook'
    },
    // ── 管理员模块 ──
    {
      name: '用户管理',
      description: '管理系统用户',
      icon: 'ri:user-settings-line',
      iconColor: '#377dff',
      enabled: true,
      order: 1,
      routeName: 'UserManage'
    },
    {
      name: '专业管理',
      description: '管理专业信息',
      icon: 'ri:graduation-cap-line',
      iconColor: '#9c27b0',
      enabled: true,
      order: 2,
      routeName: 'MajorManage'
    },
    {
      name: '班级管理',
      description: '管理班级信息',
      icon: 'ri:team-line',
      iconColor: '#13DEB9',
      enabled: true,
      order: 3,
      routeName: 'ClassManage'
    },
    {
      name: '科目管理',
      description: '管理科目信息',
      icon: 'ri:book-2-line',
      iconColor: '#ffb100',
      enabled: true,
      order: 4,
      routeName: 'SubjectManage'
    }
  ],
  // 快速链接（权限由路由表 meta.roles 自动推导）
  quickLinks: [
    {
      name: '首页',
      enabled: true,
      order: 1,
      routeName: 'TeacherHome'
    },
    {
      name: '首页',
      enabled: true,
      order: 1,
      routeName: 'StudentHome'
    },
    {
      name: '首页',
      enabled: true,
      order: 1,
      routeName: 'AdminHome'
    },
    {
      name: '个人中心',
      enabled: true,
      order: 2,
      routeName: 'Profile'
    }
  ]
}

export default Object.freeze(fastEnterConfig)
