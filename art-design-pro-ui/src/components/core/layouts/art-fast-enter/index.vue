<!-- 顶部快速入口面板 -->
<template>
  <ElPopover
    ref="popoverRef"
    :width="700"
    :offset="0"
    :show-arrow="false"
    trigger="hover"
    placement="bottom-start"
    popper-class="fast-enter-popover"
    :popper-style="{
      border: '1px solid var(--default-border)',
      borderRadius: 'calc(var(--custom-radius) / 2 + 4px)'
    }"
  >
    <template #reference>
      <div class="flex-c gap-2">
        <slot />
      </div>
    </template>

    <div class="grid grid-cols-[2fr_0.8fr]">
      <div>
        <div class="grid grid-cols-2 gap-1.5">
          <!-- 应用列表 -->
          <div
            v-for="application in enabledApplications"
            :key="application.name"
            class="mr-3 c-p flex-c gap-3 rounded-lg p-2 hover:bg-g-200/70 dark:hover:bg-g-200/90 hover:[&_.app-icon]:!bg-transparent"
            @click="handleApplicationClick(application)"
          >
            <div class="app-icon size-12 flex-cc rounded-lg bg-g-200/80 dark:bg-g-300/30">
              <ArtSvgIcon
                class="text-xl"
                :icon="application.icon"
                :style="{ color: application.iconColor }"
              />
            </div>
            <div>
              <h3 class="m-0 text-sm font-medium text-g-800">{{ application.name }}</h3>
              <p class="mt-1 text-xs text-g-600">{{ application.description }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="border-l-d pl-6 pt-2">
        <h3 class="mb-2.5 text-base font-medium text-g-800">快速链接</h3>
        <ul>
          <li
            v-for="quickLink in enabledQuickLinks"
            :key="quickLink.name"
            class="c-p py-2 hover:[&_span]:text-theme"
            @click="handleQuickLinkClick(quickLink)"
          >
            <span class="text-g-600 no-underline">{{ quickLink.name }}</span>
          </li>
        </ul>
      </div>
    </div>
  </ElPopover>
</template>

<script setup lang="ts">
  import { ElMessage } from 'element-plus'
  import { useFastEnter } from '@/hooks/core/useFastEnter'
  import type { FastEnterApplication, FastEnterQuickLink } from '@/types/config'

  defineOptions({ name: 'ArtFastEnter' })

  const router = useRouter()
  const popoverRef = ref()

  // 使用快速入口配置
  const { enabledApplications, enabledQuickLinks } = useFastEnter()

  /**
   * 处理导航跳转
   *
   * 决策顺序：
   * 1. 显式外链（link 字段且以 http/https 开头）→ 新标签页打开（含 Tabnabbing 安全加固）
   * 2. 路由跳转（routeName 字段）→ 先校验路由注册状态，失败给用户友好反馈
   *
   * @param routeName 路由名称
   * @param link 外部链接
   */
  const handleNavigate = (routeName?: string, link?: string): void => {
    // 1. 显式外链：用 link 字段而非 routeName 前缀判断（杜绝误判）
    //    安全加固：noopener + noreferrer 防止 Reverse Tabnabbing 攻击
    if (link && /^https?:\/\//i.test(link)) {
      window.open(link, '_blank', 'noopener,noreferrer')
      popoverRef.value?.hide()
      return
    }

    // 2. 路由跳转：注册校验（防止动态路由未注册时的静默失败）
    if (routeName) {
      if (!router.hasRoute(routeName)) {
        if (import.meta.env.DEV) {
          console.warn(`[FastEnter] 路由 '${routeName}' 未注册，无法跳转`)
        }
        ElMessage.warning('该功能暂不可用')
        popoverRef.value?.hide()
        return
      }

      router.push({ name: routeName })
      popoverRef.value?.hide()
      return
    }

    // 3. 配置无效（既无 routeName 也无 link）
    console.warn('[FastEnter] 导航配置无效：缺少 routeName 或 link')
  }

  /**
   * 处理应用项点击
   * @param application 应用配置对象
   */
  const handleApplicationClick = (application: FastEnterApplication): void => {
    handleNavigate(application.routeName, application.link)
  }

  /**
   * 处理快速链接点击
   * @param quickLink 快速链接配置对象
   */
  const handleQuickLinkClick = (quickLink: FastEnterQuickLink): void => {
    handleNavigate(quickLink.routeName, quickLink.link)
  }
</script>
