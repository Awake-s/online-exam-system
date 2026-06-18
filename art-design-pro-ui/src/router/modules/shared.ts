import { AppRouteRecord } from '@/types/router'

/**
 * 跨角色共享的账号级一级路由
 *
 * 设计依据（art-design-pro 官方规则）：
 * 1. RouteTransformer.isFirstLevelRoute 自动识别：`depth === 0 && 无 children`
 *    → 自动 Layout 包裹 + 自动标记 meta.isFirstLevel = true
 * 2. art-breadcrumb 读取 matched[0].meta.isFirstLevel，为 true 时面包屑只显示当前页
 * 3. MenuProcessor.filterMenuByRoles 按 meta.roles 声明式过滤，不依赖路径前缀
 *
 * 为什么剥离到顶层（不嵌套在业务菜单下）：
 * - 多角色分治场景下，同一账号级页面在不同角色下会挂到不同的业务父菜单
 *   （admin→"系统管理" / teacher→"题库中心" / student→"在线考试"），导致
 *   面包屑语义分裂、内容与导航矛盾、点击父项跳到无关业务的 UX 伤害
 * - 顶层一级路由后，三角色统一面包屑"通知中心"/"个人中心"单项，零歧义
 *
 * 参考文档：https://www.artd.pro/docs/zh/guide/essentials/route.html
 */

/**
 * 通知中心 - 跨角色共享
 *
 * - URL：`/notification`（REST 风格，语义清晰）
 * - 入口：Header 铃铛图标 → 下拉面板 → 查看全部
 * - 权限：三角色共享，声明式通过 meta.roles 过滤
 * - 菜单：isHide: true 不在侧边栏显示
 * - 缓存：keepAlive: true 保留列表滚动位置和筛选状态
 */
export const notificationRoute: AppRouteRecord = {
  name: 'Notification',
  path: '/notification',
  component: '/notification/index',
  meta: {
    title: 'menus.notification.center',
    icon: 'ri:notification-3-line',
    roles: ['R_ADMIN', 'R_TEACHER', 'R_STUDENT'],
    isHide: true,
    keepAlive: true
  }
}

/**
 * 个人中心 - 跨角色共享
 *
 * - URL：`/profile`（REST 风格，语义清晰）
 * - 入口：Header 用户头像 → 下拉菜单 → 个人中心
 * - 权限：三角色共享，声明式通过 meta.roles 过滤
 * - 菜单：isHide: true 不在侧边栏显示
 * - 标签页：isHideTab: true 不在工作标签页显示（对标原生 UserCenter 设计规范）
 * - 缓存：keepAlive: true 保留表单编辑状态，防止误关标签丢失输入
 */
export const profileRoute: AppRouteRecord = {
  name: 'Profile',
  path: '/profile',
  component: '/profile/index',
  meta: {
    title: 'menus.profile.title',
    icon: 'ri:user-line',
    roles: ['R_ADMIN', 'R_TEACHER', 'R_STUDENT'],
    isHide: true,
    isHideTab: true,
    keepAlive: true
  }
}
