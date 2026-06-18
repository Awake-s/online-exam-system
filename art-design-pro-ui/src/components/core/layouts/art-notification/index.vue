<!-- 通知组件 -->
<template>
  <div
    class="art-notification-panel art-card-sm !shadow-xl"
    :style="{
      transform: show ? 'scaleY(1)' : 'scaleY(0.9)',
      opacity: show ? 1 : 0
    }"
    v-show="visible"
    @click.stop
  >
    <div class="flex-cb px-3.5 mt-3.5">
      <span class="text-base font-medium text-g-800">{{ $t('notice.title') }}</span>
      <!-- 「标为已读」按钮仅在通知 tab 显示：待办无"已读"语义，显示此按钮会误导用户 -->
      <span
        v-if="currentTabKey === 'notice'"
        class="text-xs px-2 py-1 c-p select-none rounded-md transition-all duration-200"
        :class="
          unreadTotal > 0 ? 'text-theme bg-theme/8 hover:bg-theme/15' : 'text-g-400 hover:bg-g-100'
        "
        @click="handleMarkAllRead"
      >
        {{ $t('notice.btnRead') }}
      </span>
    </div>

    <!--
      Tab 栏 - 保持始终渲染：
      · 管理员仅有「通知」一个 tab，仍正常显示并带未读数 badge，单 tab 也有交互反馈价值
      · 学生/教师显示「通知」+「待办」两个 tab
      · 始终渲染保持下方列表区 h-[calc(100%-95px)] 的布局前提不变，零回归
    -->
    <ul class="box-border flex items-end w-full h-12.5 px-3.5 border-b-d">
      <li
        v-for="(item, index) in barList"
        :key="item.key"
        class="h-12 leading-12 mr-5 overflow-hidden text-[13px] c-p select-none"
        :class="barActiveIndex === index ? 'bar-active font-medium' : 'text-g-500 hover:text-g-700'"
        @click="changeBar(index)"
      >
        {{ item.name }}
        <span
          v-if="item.num > 0"
          class="inline-flex items-center justify-center min-w-4 h-4 px-1 text-[10px] rounded-full font-medium leading-none ml-1"
          :class="barActiveIndex === index ? 'bg-theme/12 text-theme' : 'bg-g-200/80 text-g-500'"
          >{{ item.num }}</span
        >
      </li>
    </ul>

    <div class="w-full h-[calc(100%-95px)]">
      <div class="h-[calc(100%-60px)] overflow-y-scroll scrollbar-thin">
        <!-- 通知 -->
        <ul v-show="currentTabKey === 'notice'">
          <li
            v-for="item in noticeList"
            :key="item.id"
            class="notice-item box-border flex-c px-3.5 py-3.5 c-p last:border-b-0 hover:bg-g-200/60"
            :class="{ 'read-item': item.isRead === 1, 'urgent-item': item.priority === 1 }"
            @click="handleReadNotice(item)"
          >
            <div class="relative">
              <!-- E1: 有 sender 时显示头像，否则显示类型图标 -->
              <template v-if="item.payload?.senderAvatar || item.payload?.senderName">
                <ElAvatar :size="36" :src="item.payload.senderAvatar">
                  {{ (item.payload.senderName || '?').charAt(0) }}
                </ElAvatar>
                <!-- 右下角类型角标 -->
                <div
                  class="absolute -bottom-1 -right-1 size-5 rounded-full border-2 border-white flex-cc text-[10px]"
                  :class="[getNoticeStyle(item.type).iconClass]"
                >
                  <ArtSvgIcon class="!bg-transparent" :icon="getNoticeStyle(item.type).icon" />
                </div>
              </template>
              <div
                v-else
                class="size-9 leading-9 text-center rounded-lg flex-cc"
                :class="[getNoticeStyle(item.type).iconClass]"
              >
                <ArtSvgIcon
                  class="text-lg !bg-transparent"
                  :icon="getNoticeStyle(item.type).icon"
                />
              </div>
              <span
                v-if="item.isRead === 0"
                class="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-danger unread-dot"
              />
            </div>
            <div class="w-[calc(100%-45px)] ml-3.5">
              <h4
                class="text-sm leading-5.5 text-g-900 truncate flex items-center gap-1.5"
                :class="item.isRead === 0 ? 'font-medium' : 'font-normal'"
              >
                <!-- E1: 优先级徽章（紧急红、次要灰；普通不显示） -->
                <span
                  v-if="item.priority === 1"
                  class="shrink-0 px-1.5 py-0.5 text-[10px] font-semibold rounded leading-none bg-danger/10 text-danger border border-danger/30"
                  >紧急</span
                >
                <span
                  v-else-if="item.priority === 3"
                  class="shrink-0 px-1.5 py-0.5 text-[10px] font-medium rounded leading-none bg-g-200 text-g-600 border border-g-300"
                  >次要</span
                >
                <span class="truncate">{{ item.title }}</span>
              </h4>
              <!-- E1: 发送者姓名（小字辅助） -->
              <p
                v-if="item.payload?.senderName"
                class="mt-0.5 text-[11px] text-g-500 flex items-center gap-0.5"
              >
                <ArtSvgIcon icon="ri:user-line" class="!bg-transparent text-[11px]" />
                {{ item.payload.senderName }}
              </p>
              <p v-if="item.content" class="mt-0.5 text-xs text-g-500 truncate">{{
                item.content
              }}</p>
              <p class="mt-1.5 text-xs text-g-500">{{ formatTime(item.createTime) }}</p>
            </div>
          </li>
        </ul>

        <!-- 待办 -->
        <ul v-show="currentTabKey === 'pending'">
          <li
            v-for="(item, index) in pendingList"
            :key="index"
            class="box-border flex items-start gap-3 px-4 py-4 c-p last:border-b-0 hover:bg-g-200/60 transition-colors duration-150"
            @click="handlePendingClick(item)"
          >
            <div
              class="size-10 rounded-xl flex-cc shrink-0"
              :class="[getNoticeStyle(item.type).iconClass]"
            >
              <ArtSvgIcon class="text-xl !bg-transparent" :icon="getNoticeStyle(item.type).icon" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="text-sm font-medium leading-5.5 text-g-900">{{ item.title }}</h4>
              <p v-if="item.content" class="mt-1.5 text-xs text-g-500 leading-4.5">{{
                item.content
              }}</p>
              <span
                class="inline-flex items-center gap-1 mt-2 text-[11px] px-2 py-0.5 rounded-full font-medium leading-4"
                :class="[getPendingBadgeClass(item.type)]"
                ><ArtSvgIcon
                  class="text-xs !bg-transparent"
                  :icon="getPendingBadgeIcon(item.type)"
                />{{ getPendingLabel(item.type) }}</span
              >
            </div>
          </li>
        </ul>

        <!-- 空状态 -->
        <div
          v-show="currentTabIsEmpty"
          class="relative top-25 h-full text-g-500 text-center !bg-transparent"
        >
          <ArtSvgIcon icon="system-uicons:inbox" class="text-5xl" />
          <p class="mt-3.5 text-xs !bg-transparent"
            >{{ $t('notice.text[0]') }}{{ barList[barActiveIndex]?.name || '' }}</p
          >
        </div>
      </div>

      <div class="relative box-border w-full px-3.5">
        <ElButton class="w-full mt-3" @click="handleViewAll" v-ripple>
          {{ viewAllBtnText }}
        </ElButton>
      </div>
    </div>

    <div class="h-25"></div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch, onMounted, onUnmounted, type ComputedRef } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import {
    getNotificationList,
    getUnreadCount,
    markAllAsRead,
    getPendingItems
  } from '@/api/exam/notification'
  import { mittBus } from '@/utils/sys'
  import { useUserStore } from '@/store/modules/user'
  import { useNotificationWebSocketStore } from '@/store/modules/notificationWebSocket'
  import { useNotificationHandler } from '@/hooks/core/useNotificationHandler'

  defineOptions({ name: 'ArtNotification' })

  interface NoticeItem {
    id: number
    title: string
    content: string
    type: string
    bizType: string
    bizId: number | null
    isRead: number
    createTime: string
    /** E1 新增：优先级 1=紧急 / 2=普通 / 3=次要 */
    priority?: number
    /** E1 新增：扩展载荷（发送者、action_url 等） */
    payload?: {
      senderId?: number
      senderName?: string
      senderAvatar?: string
      actionUrl?: string
      extras?: Record<string, unknown>
    }
  }

  interface PendingItem {
    type: string
    title: string
    content: string
    bizType: string
    bizId: number | null
  }

  interface BarItem {
    name: ComputedRef<string>
    num: number
    key: 'notice' | 'pending' // 语义 key，避免角色切换时仅依赖 index 导致漂移
  }

  interface NoticeStyle {
    icon: string
    iconClass: string
  }

  const { t } = useI18n()
  const router = useRouter()
  const userStore = useUserStore()
  const wsStore = useNotificationWebSocketStore() // D4: 监听 WS 连接状态，WS 连通时关闭本地 15s 轮询
  const { handleNotificationClick, getNotificationCenterPath } = useNotificationHandler()

  const props = defineProps<{
    value: boolean
  }>()

  const emit = defineEmits<{
    'update:value': [value: boolean]
  }>()

  const show = ref(false)
  const visible = ref(false)
  const barActiveIndex = ref(0)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  // ===================== 数据 =====================
  const noticeList = ref<NoticeItem[]>([])
  const pendingList = ref<PendingItem[]>([])
  const unreadTotal = ref(0)

  /**
   * Tab 列表 - 根据角色动态生成
   * 核心业务判断：管理员无待办业务（业务分析结论，详见 NotificationServiceImpl.getPendingItems 注释）
   * 因此仅学生/教师展示「待办」tab，管理员仅保留「通知」tab
   */
  const barList = computed<BarItem[]>(() => {
    const bars: BarItem[] = [
      {
        name: computed(() => t('notice.bar[0]')),
        num: unreadTotal.value,
        key: 'notice'
      }
    ]
    const roleCode = userStore.info?.roleCode
    // 管理员无待办，不展示此 tab
    if (roleCode !== 'ADMIN') {
      bars.push({
        name: computed(() => t('notice.bar[2]')),
        num: pendingList.value.length,
        key: 'pending'
      })
    }
    return bars
  })

  /** 当前 tab 的语义 key（防索引越界的兜底） */
  const currentTabKey = computed<'notice' | 'pending'>(() => {
    return barList.value[barActiveIndex.value]?.key ?? 'notice'
  })

  // ===================== API 调用 =====================
  const fetchNotifications = async () => {
    try {
      const res = await getNotificationList({ page: 1, size: 20 })
      if (res) {
        noticeList.value = res.records || []
      }
    } catch (e) {
      // 接口未就绪时静默失败
    }
  }

  const fetchUnreadCount = async () => {
    try {
      const res = await getUnreadCount()
      if (res) {
        unreadTotal.value = res.total || 0
      }
    } catch (e) {
      // 静默失败
    }
  }

  const fetchPendingItems = async () => {
    try {
      const res = await getPendingItems()
      if (res) {
        pendingList.value = res || []
      }
    } catch (e) {
      // 静默失败
    }
  }

  const loadAllData = async () => {
    await Promise.all([fetchNotifications(), fetchUnreadCount(), fetchPendingItems()])
  }

  // ===================== 样式管理 =====================
  const noticeStyleMap: Record<string, NoticeStyle> = {
    EXAM_PUBLISHED: {
      icon: 'ri:file-list-3-line',
      iconClass: 'bg-theme/12 text-theme'
    },
    EXAM_UPDATED: {
      icon: 'ri:edit-line',
      iconClass: 'bg-warning/12 text-warning'
    },
    EXAM_CANCELLED: {
      icon: 'ri:close-circle-line',
      iconClass: 'bg-danger/12 text-danger'
    },
    EXAM_SUBMITTED: {
      icon: 'ri:check-double-line',
      iconClass: 'bg-success/12 text-success'
    },
    EXAM_AUTO_SUBMITTED: {
      icon: 'ri:check-double-line',
      iconClass: 'bg-warning/12 text-warning'
    },
    EXAM_ABSENT: {
      icon: 'ri:user-unfollow-line',
      iconClass: 'bg-danger/12 text-danger'
    },
    EXAM_END_SUMMARY: {
      icon: 'ri:bar-chart-box-line',
      iconClass: 'bg-info/12 text-info'
    },
    SCORE_PUBLISHED: {
      icon: 'ri:bar-chart-box-line',
      iconClass: 'bg-info/12 text-info'
    },
    SCORE_UPDATED: {
      icon: 'ri:refresh-line',
      iconClass: 'bg-warning/12 text-warning'
    },
    ACCOUNT_CREATED: {
      icon: 'ri:user-add-line',
      iconClass: 'bg-success/12 text-success'
    },
    USER_CREATED: {
      icon: 'ri:user-add-line',
      iconClass: 'bg-theme/12 text-theme'
    },
    EXAM_CREATED: {
      icon: 'ri:file-list-3-line',
      iconClass: 'bg-warning/12 text-warning'
    },
    EXAM_PENDING: {
      icon: 'ri:time-line',
      iconClass: 'bg-warning/12 text-warning'
    },
    EXAM_IN_PROGRESS: {
      icon: 'ri:timer-flash-line',
      iconClass: 'bg-danger/12 text-danger'
    },
    NEED_MARKING: {
      icon: 'ri:quill-pen-line',
      iconClass: 'bg-info/12 text-info'
    }
  }

  const getNoticeStyle = (type: string): NoticeStyle => {
    return (
      noticeStyleMap[type] || {
        icon: 'ri:notification-3-line',
        iconClass: 'bg-theme/12 text-theme'
      }
    )
  }

  const pendingLabelMap: Record<string, string> = {
    EXAM_PENDING: '待考',
    EXAM_IN_PROGRESS: '进行中',
    NEED_MARKING: '待批'
  }

  const getPendingLabel = (type: string): string => pendingLabelMap[type] || '待办'

  const pendingBadgeClassMap: Record<string, string> = {
    EXAM_PENDING: 'bg-amber-50 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400',
    EXAM_IN_PROGRESS: 'bg-red-50 text-red-500 dark:bg-red-900/30 dark:text-red-400',
    NEED_MARKING: 'bg-blue-50 text-blue-500 dark:bg-blue-900/30 dark:text-blue-400'
  }

  const pendingBadgeIconMap: Record<string, string> = {
    EXAM_PENDING: 'ri:time-line',
    EXAM_IN_PROGRESS: 'ri:timer-flash-line',
    NEED_MARKING: 'ri:quill-pen-line'
  }

  const getPendingBadgeClass = (type: string): string =>
    pendingBadgeClassMap[type] || 'bg-gray-50 text-gray-500'
  const getPendingBadgeIcon = (type: string): string => pendingBadgeIconMap[type] || 'ri:todo-line'

  // ===================== 时间格式化 =====================
  const formatTime = (timeStr: string): string => {
    if (!timeStr) return ''
    const d = new Date(timeStr)
    const now = new Date()
    const diffMs = now.getTime() - d.getTime()
    const diffMin = Math.floor(diffMs / 60000)
    if (diffMin < 1) return '刚刚'
    if (diffMin < 60) return `${diffMin} 分钟前`
    const diffHour = Math.floor(diffMin / 60)
    if (diffHour < 24) return `${diffHour} 小时前`
    const diffDay = Math.floor(diffHour / 24)
    if (diffDay < 7) return `${diffDay} 天前`
    return timeStr.replace('T', ' ').substring(0, 16)
  }

  // ===================== 动画管理 =====================
  const showNotice = (open: boolean) => {
    if (open) {
      visible.value = true
      loadAllData()
      setTimeout(() => {
        show.value = true
      }, 5)
    } else {
      show.value = false
      setTimeout(() => {
        visible.value = false
      }, 350)
    }
  }

  // ===================== 交互逻辑 =====================
  const changeBar = (index: number) => {
    // 防越界：index 超出 barList 范围时回退到 0
    if (index >= 0 && index < barList.value.length) {
      barActiveIndex.value = index
    }
  }

  const currentTabIsEmpty = computed(() => {
    // 用语义 key 取数据，避免索引漂移
    if (currentTabKey.value === 'notice') return noticeList.value.length === 0
    return pendingList.value.length === 0
  })

  const handleReadNotice = async (item: NoticeItem) => {
    const wasUnread = item.isRead === 0
    await handleNotificationClick(item, () => emit('update:value', false))
    if (wasUnread && unreadTotal.value > 0) unreadTotal.value--
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead()
      noticeList.value.forEach((n) => (n.isRead = 1))
      unreadTotal.value = 0
    } catch (e) {
      // 静默失败
    }
  }

  const handlePendingClick = (item: PendingItem) => {
    // 关闭通知面板
    emit('update:value', false)

    // 根据待办类型跳转
    if (item.type === 'NEED_MARKING') {
      // 教师待批改 -> 批改列表页面
      router.push(`/exam-center/marking?examId=${item.bizId}`)
    } else if (item.type === 'EXAM_PENDING' || item.type === 'EXAM_IN_PROGRESS') {
      // 学生待参加考试或考试进行中 -> 跳转到考试列表页
      router.push('/student/exam')
      if (item.type === 'EXAM_IN_PROGRESS') {
        ElMessage.info('请在考试列表中继续答题')
      } else {
        ElMessage.info('请在考试列表中查看考试详情')
      }
    }
  }

  /**
   * 「查看全部/立即处理」按钮跳转逻辑
   * - 通知 tab：跳至角色对应的通知中心（聚合查看历史消息）
   * - 待办 tab：跳至角色对应的工作页，用户可立即处理
   *   · 学生 → 我的考试列表（待考/进行中均在此页筛选）
   *   · 教师 → 阅卷管理列表（待批改试卷聚合）
   *   · 管理员不会出现此 tab（barList 已过滤），此处保留兜底无副作用
   */
  const handleViewAll = () => {
    emit('update:value', false)

    // 待办 tab 路由跳转
    if (currentTabKey.value === 'pending') {
      const roleCode = userStore.info?.roleCode
      if (roleCode === 'TEACHER') {
        router.push('/exam-center/marking')
      } else if (roleCode === 'STUDENT') {
        router.push('/student/exam')
      }
      // 管理员兜底：由于 barList 已过滤，正常流程走不到此分支，
      // 极端防御情况（如数据异常）保持面板关闭即可，不做无意义跳转
      return
    }

    // 通知 tab：跳至角色对应的通知中心页
    router.push(getNotificationCenterPath())
  }

  /** 按钮文案跟随 tab 语义精准化 */
  const viewAllBtnText = computed(() => {
    if (currentTabKey.value === 'pending') return '立即处理'
    return t('notice.viewAll')
  })

  // ===================== 生命周期 =====================
  /**
   * D4: 按需轮询策略
   * - WS 连通时：无需本地轮询（WS 推送 + mittBus.refreshNotification 已同步）
   * - WS 未连通：启用 15s 兜底轮询；此时 notificationWebSocket 的 30s 降级轮询也会工作
   *
   * 两层保障但不冗余：WS 一旦连通立刻关闭本地轮询，避免同一信息多次拉取浪费资源
   */
  const startLocalPollingIfNeeded = () => {
    if (wsStore.connected) {
      stopLocalPolling()
      return
    }
    if (!pollTimer) {
      pollTimer = setInterval(fetchUnreadCount, 15000)
    }
  }

  const stopLocalPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  onMounted(() => {
    loadAllData()
    startLocalPollingIfNeeded()
    mittBus.on('refreshNotification', loadAllData)
  })

  onUnmounted(() => {
    stopLocalPolling()
    mittBus.off('refreshNotification', loadAllData)
  })

  /** 监听 WS 连接状态，动态切换轮询策略 */
  watch(
    () => wsStore.connected,
    (connected) => {
      if (connected) {
        stopLocalPolling()
      } else {
        startLocalPollingIfNeeded()
      }
    }
  )

  watch(
    () => props.value,
    (newValue) => {
      showNotice(newValue)
    }
  )

  /**
   * 防御：角色切换（登出登入）时重置 tab 索引到 0，避免管理员账号下索引 1 指向不存在的 tab
   * 同时让 barList.length 变化时索引越界自动校正
   */
  watch(
    () => [userStore.info?.roleCode, barList.value.length] as [string | undefined, number],
    ([, len]) => {
      if (barActiveIndex.value >= len) {
        barActiveIndex.value = 0
      }
    }
  )

  defineExpose({ unreadTotal })
</script>

<style scoped>
  @reference '@styles/core/tailwind.css';

  .art-notification-panel {
    @apply absolute 
    top-14.5 
    right-5 
    w-90 
    h-125 
    max-h-[calc(100vh-80px)]
    overflow-hidden 
    transition-all 
    duration-300
    origin-top 
    will-change-[top,left] 
    max-[640px]:top-[65px]
    max-[640px]:right-0
    max-[640px]:w-full 
    max-[640px]:h-[80vh];
  }

  .bar-active {
    color: var(--theme-color) !important;
    border-bottom: 2px solid var(--theme-color);
  }

  .notice-item.read-item {
    opacity: 0.5;
  }

  .notice-item.read-item:hover {
    opacity: 0.7;
  }

  /* E1: 紧急通知左侧红色强调条 */
  .notice-item.urgent-item {
    position: relative;
    background: rgba(var(--el-color-danger-rgb, 245, 108, 108), 0.04);
  }

  .notice-item.urgent-item::before {
    content: '';
    position: absolute;
    left: 0;
    top: 8px;
    bottom: 8px;
    width: 3px;
    background: var(--el-color-danger);
    border-radius: 0 2px 2px 0;
  }

  .unread-dot {
    animation: dot-pulse 2.5s ease-in-out infinite;
  }

  @keyframes dot-pulse {
    0%,
    100% {
      opacity: 1;
      transform: scale(1);
    }
    50% {
      opacity: 0.4;
      transform: scale(1.4);
    }
  }

  .scrollbar-thin::-webkit-scrollbar {
    width: 5px !important;
  }

  .dark .scrollbar-thin::-webkit-scrollbar-track {
    background-color: var(--default-box-color);
  }

  .dark .scrollbar-thin::-webkit-scrollbar-thumb {
    background-color: #222 !important;
  }
</style>
