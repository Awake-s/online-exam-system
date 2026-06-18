/**
 * useFastEnter - 快速入口管理
 *
 * 管理顶部栏的快速入口功能，提供应用列表和快速链接的配置和过滤。
 * 支持动态启用/禁用、自定义排序、响应式宽度控制等功能。
 *
 * ## 主要功能
 *
 * 1. 应用列表管理 - 获取启用的应用列表，自动按排序权重排序
 * 2. 快速链接管理 - 获取启用的快速链接，支持自定义排序
 * 3. 响应式配置 - 所有配置自动响应变化，无需手动更新
 * 4. 宽度控制 - 提供最小显示宽度配置，支持响应式布局
 *
 * ## 可见性判定（单一权威源原则）
 *
 * - 路由条目（有 `routeName`）：以路由表的 `meta.roles` 为唯一权威源，
 *   fastEnter 配置无需重复声明 roles，避免双重维护风险。
 * - 外链条目（仅有 `link`）：以 fastEnter 配置中的 `roles` 字段为准，
 *   未配 roles 则默认所有角色可见。
 * - 路由未注册：自动隐藏该条目，开发模式下发出 warning 便于排查。
 *
 * @module useFastEnter
 * @author Art Design Pro Team
 */

import { computed } from 'vue'
import { useRouter } from 'vue-router'
import appConfig from '@/config'
import { useUserStore } from '@/store/modules/user'
import type { FastEnterApplication, FastEnterBaseItem, FastEnterQuickLink } from '@/types/config'

export function useFastEnter() {
  // 获取快速入口配置
  const fastEnterConfig = computed(() => appConfig.fastEnter)
  const userStore = useUserStore()
  const router = useRouter()

  /**
   * 判定单个条目对当前用户是否可见
   *
   * 决策流程：
   * 1. 外链条目（无 routeName）→ 按 fastEnter 手工 roles 判断
   * 2. 路由条目（有 routeName）→ 从路由表读取 meta.roles 为单一权威源
   *    - 路由未注册 → 隐藏并发出 DEV warning（配置漂移保护）
   *    - 路由无 meta.roles → 所有角色可见
   *    - 路由有 meta.roles → 按交集判断
   */
  const matchVisibility = (item: FastEnterBaseItem): boolean => {
    const userRoles = userStore.info?.roles || []

    // 外链条目：以 fastEnter 配置中的 roles 字段为准
    if (!item.routeName) {
      if (!item.roles || item.roles.length === 0) return true
      return item.roles.some((r) => userRoles.includes(r))
    }

    // 路由条目：先校验路由是否已注册（防止动态路由时序问题）
    if (!router.hasRoute(item.routeName)) {
      if (import.meta.env.DEV) {
        console.warn(`[FastEnter] 路由 '${item.routeName}' 未注册，对应的快速入口已自动隐藏`)
      }
      return false
    }

    // 从路由表读取 meta.roles（单一权威源，消除双重维护）
    const targetRoute = router.getRoutes().find((r) => r.name === item.routeName)
    const metaRoles = targetRoute?.meta?.roles as string[] | undefined

    // 路由无权限限制 → 所有角色可见
    if (!metaRoles || metaRoles.length === 0) return true

    // 按 meta.roles 与用户角色取交集
    return metaRoles.some((r) => userRoles.includes(r))
  }

  // 获取启用的应用列表（可见性过滤 + 排序权重排序）
  const enabledApplications = computed<FastEnterApplication[]>(() => {
    if (!fastEnterConfig.value?.applications) return []

    return fastEnterConfig.value.applications
      .filter((app) => app.enabled !== false && matchVisibility(app))
      .sort((a, b) => (a.order || 0) - (b.order || 0))
  })

  // 获取启用的快速链接（可见性过滤 + 排序权重排序）
  const enabledQuickLinks = computed<FastEnterQuickLink[]>(() => {
    if (!fastEnterConfig.value?.quickLinks) return []

    return fastEnterConfig.value.quickLinks
      .filter((link) => link.enabled !== false && matchVisibility(link))
      .sort((a, b) => (a.order || 0) - (b.order || 0))
  })

  // 获取最小显示宽度
  const minWidth = computed(() => {
    return fastEnterConfig.value?.minWidth || 1200
  })

  return {
    fastEnterConfig,
    enabledApplications,
    enabledQuickLinks,
    minWidth
  }
}
