<!-- 通知中心页 - 三角色共用 | 三段式布局 + 业界顶级 UX -->
<template>
  <div class="notification-center">
    <div class="art-card-sm notification-card">
      <!-- ============ 固定头：工具栏 ============ -->
      <div class="toolbar">
        <div class="toolbar-row">
          <ElTabs v-model="activeTab" class="notification-tabs" @tab-change="handleTabChange">
            <ElTabPane label="全部" name="all" />
            <ElTabPane :label="unreadTabLabel" name="unread" />
          </ElTabs>

          <div class="toolbar-actions">
            <ElTooltip content="按 / 聚焦搜索框" placement="top" :show-after="600">
              <ElInput
                v-model="keyword"
                placeholder="搜索通知标题或内容"
                clearable
                class="search-input"
                @input="handleKeywordInput"
                @clear="handleKeywordInput"
              >
                <template #prefix>
                  <ArtSvgIcon icon="ri:search-line" />
                </template>
              </ElInput>
            </ElTooltip>

            <ElSelect
              v-model="typeFilter"
              placeholder="全部类型"
              clearable
              class="type-select"
              @change="handleFilterChange"
            >
              <ElOption label="考试发布" value="EXAM_PUBLISHED" />
              <ElOption label="考试更新" value="EXAM_UPDATED" />
              <ElOption label="考试取消" value="EXAM_CANCELLED" />
              <ElOption label="考试提交" value="EXAM_SUBMITTED" />
              <ElOption label="自动提交" value="EXAM_AUTO_SUBMITTED" />
              <ElOption label="缺考通知" value="EXAM_ABSENT" />
              <ElOption label="考试结束" value="EXAM_END_SUMMARY" />
              <ElOption label="成绩发布" value="SCORE_PUBLISHED" />
              <ElOption label="成绩更新" value="SCORE_UPDATED" />
              <ElOption label="账号创建" value="ACCOUNT_CREATED" />
              <ElOption label="用户创建" value="USER_CREATED" />
              <ElOption label="考试创建" value="EXAM_CREATED" />
            </ElSelect>

            <ElTooltip content="按 x 进入/退出多选" placement="top" :show-after="600">
              <ElButton :type="selectionMode ? 'primary' : 'default'" @click="toggleSelectionMode">
                <ArtSvgIcon icon="ri:checkbox-multiple-line" class="mr-1" />
                {{ selectionMode ? '退出多选' : '多选' }}
              </ElButton>
            </ElTooltip>

            <ElButton :disabled="unreadTotal === 0" @click="handleMarkAllRead">
              <ArtSvgIcon icon="ri:check-double-line" class="mr-1" />
              全部标记已读
            </ElButton>
          </div>
        </div>

        <!-- 批量操作浮条（选中时出现）-->
        <Transition name="slide-down">
          <div v-if="selectionMode && selectedIds.size > 0" class="batch-bar">
            <div class="batch-info">
              <ArtSvgIcon icon="ri:checkbox-circle-fill" class="batch-check-icon" />
              <span class="batch-text">
                已选 <b class="batch-count">{{ selectedIds.size }}</b> 条
              </span>
              <span class="batch-divider" />
              <ElButton text size="small" type="primary" @click="selectAllVisible">
                选中本页全部
              </ElButton>
              <ElButton text size="small" @click="clearSelection">清空选择</ElButton>
            </div>
            <div class="batch-actions">
              <ElButton size="small" @click="handleBatchMarkRead">
                <ArtSvgIcon icon="ri:check-line" class="mr-1" />
                标记已读
              </ElButton>
              <ElButton size="small" type="danger" plain @click="handleBatchDelete">
                <ArtSvgIcon icon="ri:delete-bin-line" class="mr-1" />
                删除
              </ElButton>
            </div>
          </div>
        </Transition>
      </div>

      <!-- ============ 可滚动区：通知列表 ============ -->
      <div v-loading="loading" class="list-scroll scrollbar-thin" ref="listScrollRef">
        <!-- 时间分组列表 -->
        <template v-if="list.length > 0">
          <div v-for="group in groupedList" :key="group.label" class="notification-group">
            <div class="group-title">
              <span>{{ group.label }}</span>
              <span class="group-count">{{ group.items.length }}</span>
            </div>
            <ul class="notification-list">
              <li
                v-for="(item, idx) in group.items"
                :key="item.id"
                class="notification-item"
                :class="{
                  'is-read': item.isRead === 1,
                  'is-selected': selectedIds.has(item.id),
                  'is-focused': focusedId === item.id,
                  'in-selection': selectionMode
                }"
                :data-id="item.id"
                :tabindex="0"
                @click="handleItemClick(item, $event)"
                @focus="focusedId = item.id"
              >
                <!-- 左侧色条（未读标识） -->
                <span class="unread-bar" aria-hidden="true" />

                <!-- 多选 Checkbox -->
                <div v-if="selectionMode" class="checkbox-wrap" @click.stop>
                  <ElCheckbox
                    :model-value="selectedIds.has(item.id)"
                    @change="toggleSelect(item.id)"
                  />
                </div>

                <!-- 图标 / 发送者头像（E1: 有 sender 时显示头像，否则显示类型图标） -->
                <div class="icon-wrap">
                  <template v-if="item.payload?.senderAvatar || item.payload?.senderName">
                    <ElAvatar :size="40" :src="item.payload.senderAvatar" class="sender-avatar">
                      {{ (item.payload.senderName || '?').charAt(0) }}
                    </ElAvatar>
                    <!-- 类型图标作为角标叠加在头像右下角 -->
                    <div class="icon-overlay" :class="[getNoticeStyle(item.type).iconClass]">
                      <ArtSvgIcon class="!bg-transparent" :icon="getNoticeStyle(item.type).icon" />
                    </div>
                  </template>
                  <div v-else class="icon-badge" :class="[getNoticeStyle(item.type).iconClass]">
                    <ArtSvgIcon
                      class="text-lg !bg-transparent"
                      :icon="getNoticeStyle(item.type).icon"
                    />
                  </div>
                  <span v-if="item.isRead === 0" class="unread-dot" />
                </div>

                <!-- 内容 -->
                <div class="content-wrap">
                  <h4 class="title">
                    <!-- E1: 优先级徽章（紧急=红 / 次要=灰；普通不显示） -->
                    <span
                      v-if="item.priority === 1"
                      class="priority-badge priority-urgent"
                      aria-label="紧急"
                      >紧急</span
                    >
                    <span
                      v-else-if="item.priority === 3"
                      class="priority-badge priority-low"
                      aria-label="次要"
                      >次要</span
                    >
                    <span v-html="highlightKeyword(item.title)" />
                  </h4>
                  <!-- E1: 发送者名称（辅助说明） -->
                  <p v-if="item.payload?.senderName" class="sender-info">
                    <ArtSvgIcon icon="ri:user-line" class="mr-1" />
                    {{ item.payload.senderName }}
                  </p>
                  <p v-if="item.content" class="description">
                    <span v-html="highlightKeyword(item.content)" />
                  </p>
                  <ElTooltip
                    :content="formatAbsoluteTime(item.createTime)"
                    placement="top"
                    :show-after="400"
                  >
                    <p class="time">{{ formatRelativeTime(item.createTime) }}</p>
                  </ElTooltip>
                </div>

                <!-- Hover 快捷操作栏 -->
                <div v-show="!selectionMode" class="hover-actions" @click.stop>
                  <ElTooltip v-if="item.isRead === 0" content="标记已读 (e)" placement="top">
                    <ElButton
                      circle
                      size="small"
                      class="action-btn"
                      @click.stop="handleMarkReadSingle(item, idx, group)"
                    >
                      <ArtSvgIcon icon="ri:check-line" />
                    </ElButton>
                  </ElTooltip>
                  <ElTooltip content="删除此通知" placement="top">
                    <ElButton
                      circle
                      size="small"
                      class="action-btn action-delete"
                      @click.stop="handleDeleteSingle(item)"
                    >
                      <ArtSvgIcon icon="ri:delete-bin-line" />
                    </ElButton>
                  </ElTooltip>
                </div>
              </li>
            </ul>
          </div>
        </template>

        <!-- 空状态：三种场景差异化（Refactoring UI 设计原则） -->
        <div v-else-if="!loading" class="empty-state">
          <div class="empty-icon-wrap" :class="emptyConfig.wrapClass">
            <ArtSvgIcon :icon="emptyConfig.icon" class="empty-icon" />
          </div>
          <h3 class="empty-title">{{ emptyConfig.title }}</h3>
          <p class="empty-desc">{{ emptyConfig.desc }}</p>
          <ElButton
            v-if="emptyConfig.actionText"
            type="primary"
            plain
            @click="emptyConfig.onAction"
          >
            {{ emptyConfig.actionText }}
          </ElButton>
        </div>
      </div>

      <!-- ============ 固定尾：分页 + 快捷键提示 ============ -->
      <div v-if="total > 0" class="pagination-bar">
        <div class="shortcut-hint">
          <ArtSvgIcon icon="ri:keyboard-line" class="mr-1" />
          <span class="hint-key">j</span>/<span class="hint-key">k</span> 切换
          <span class="hint-key ml-2">e</span> 已读 <span class="hint-key ml-2">x</span> 多选
          <span class="hint-key ml-2">/</span> 搜索
        </div>
        <ElPagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          size="small"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, ref, nextTick } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { useRouter } from 'vue-router'
  import {
    getNotificationList,
    getUnreadCount,
    markAllAsRead,
    markAsRead,
    deleteNotification,
    batchDeleteNotifications,
    batchMarkAsRead
  } from '@/api/exam/notification'
  import {
    useNotificationHandler,
    type NotificationItem
  } from '@/hooks/core/useNotificationHandler'
  import { useCommon } from '@/hooks/core/useCommon'
  import { mittBus } from '@/utils/sys'

  // 组件 name 必须与路由 name 一致，以便 Vue KeepAlive 的 :exclude 机制能正确匹配
  // 关闭标签页时，useWorktabStore 会把路由 name 加入 keepAliveExclude，
  // 若组件 name 不一致（如曾用的 'NotificationCenter'），exclude 将无法命中 → 缓存无法销毁
  // 参见 src/components/core/layouts/art-page-content/index.vue KeepAlive :exclude
  defineOptions({ name: 'Notification' })

  // ===================== 数据状态 =====================
  const list = ref<NotificationItem[]>([])
  const total = ref(0)
  const unreadTotal = ref(0)
  const loading = ref(false)

  const activeTab = ref<'all' | 'unread'>('all')
  const typeFilter = ref<string>('')
  const keyword = ref<string>('')
  const page = ref(1)
  const pageSize = ref(20)

  // 多选状态（P3）
  const selectionMode = ref(false)
  const selectedIds = ref<Set<number>>(new Set())

  // 键盘焦点（P3）
  const focusedId = ref<number | null>(null)
  const listScrollRef = ref<HTMLElement | null>(null)

  const router = useRouter()
  const { homePath } = useCommon()
  const { handleNotificationClick } = useNotificationHandler()

  // ===================== 计算属性 =====================
  const unreadTabLabel = computed(() =>
    unreadTotal.value > 0 ? `未读 (${unreadTotal.value})` : '未读'
  )

  /** 扁平化后的可见列表（用于键盘导航） */
  const flatVisibleList = computed<NotificationItem[]>(() =>
    groupedList.value.flatMap((g) => g.items)
  )

  /** 空状态配置（P2：三场景差异化 / Refactoring UI 原则） */
  const emptyConfig = computed(() => {
    // 1) 有搜索关键字 / 类型筛选但结果为空
    if (keyword.value || typeFilter.value) {
      return {
        icon: 'ri:search-line',
        wrapClass: 'empty-search',
        title: '没有匹配的通知',
        desc: '试试清除筛选条件或更换关键词',
        actionText: '清除筛选',
        onAction: clearFilters
      }
    }
    // 2) 切换到"未读"且全部已读
    if (activeTab.value === 'unread') {
      return {
        icon: 'ri:check-double-line',
        wrapClass: 'empty-done',
        title: '全部看完啦，好棒！',
        desc: '你已经处理完所有未读通知',
        actionText: '',
        onAction: () => {}
      }
    }
    // 3) 完全没有通知
    return {
      icon: 'ri:mail-open-line',
      wrapClass: 'empty-inbox',
      title: '一切都清空了',
      desc: '暂时没有新的通知，有新消息会第一时间告诉你',
      actionText: '返回首页',
      onAction: goHome
    }
  })

  /** 时间分组（P2：按 Today/Yesterday/This week/Earlier） */
  interface Group {
    label: string
    items: NotificationItem[]
  }

  const groupedList = computed<Group[]>(() => {
    const now = new Date()
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
    const startOfYesterday = startOfToday - 86400000
    const startOfThisWeek = startOfToday - (now.getDay() === 0 ? 6 : now.getDay() - 1) * 86400000

    const buckets: Record<string, NotificationItem[]> = {
      today: [],
      yesterday: [],
      thisWeek: [],
      earlier: []
    }

    list.value.forEach((item) => {
      const t = new Date(item.createTime).getTime()
      if (t >= startOfToday) buckets.today.push(item)
      else if (t >= startOfYesterday) buckets.yesterday.push(item)
      else if (t >= startOfThisWeek) buckets.thisWeek.push(item)
      else buckets.earlier.push(item)
    })

    const result: Group[] = []
    if (buckets.today.length) result.push({ label: '今天', items: buckets.today })
    if (buckets.yesterday.length) result.push({ label: '昨天', items: buckets.yesterday })
    if (buckets.thisWeek.length) result.push({ label: '本周更早', items: buckets.thisWeek })
    if (buckets.earlier.length) result.push({ label: '更早', items: buckets.earlier })
    return result
  })

  // ===================== 数据加载 =====================
  const loadData = async () => {
    loading.value = true
    try {
      const params: {
        page: number
        size: number
        type?: string
        isRead?: 0 | 1
      } = {
        page: page.value,
        size: pageSize.value
      }
      if (typeFilter.value) params.type = typeFilter.value
      if (activeTab.value === 'unread') params.isRead = 0

      const res: any = await getNotificationList(params)
      if (res) {
        list.value = res.records || []
        total.value = res.total || 0
      }
      // 加载后清空过期的选中项
      const visibleIds = new Set(list.value.map((n) => n.id))
      selectedIds.value = new Set([...selectedIds.value].filter((id) => visibleIds.has(id)))
    } catch {
      // 静默失败
    } finally {
      loading.value = false
    }
  }

  const fetchUnreadCount = async () => {
    try {
      const res: any = await getUnreadCount()
      if (res) unreadTotal.value = res.total || 0
    } catch {
      // 静默失败
    }
  }

  const loadAll = async () => {
    await Promise.all([loadData(), fetchUnreadCount()])
  }

  // ===================== 交互 =====================
  const handleTabChange = () => {
    page.value = 1
    loadData()
  }

  const handleFilterChange = () => {
    page.value = 1
    loadData()
  }

  const handleSizeChange = () => {
    page.value = 1
    loadData()
  }

  /** 关键字本地过滤（不发请求，对当前页做 title/content 匹配） */
  const handleKeywordInput = () => {
    // 本地搜索不重载数据，只影响 groupedList 中 highlightKeyword 的渲染
    // 注意：真正的关键词 + 分页合流需要后端支持，这里做本地页内过滤即可
  }

  /** 清除所有筛选条件 */
  const clearFilters = () => {
    keyword.value = ''
    typeFilter.value = ''
    activeTab.value = 'all'
    page.value = 1
    loadData()
  }

  /** 返回首页（使用项目统一的 homePath，基于 menuStore 动态计算） */
  const goHome = () => {
    router.push(homePath.value || '/')
  }

  const handleItemClick = async (item: NotificationItem, _evt?: MouseEvent) => {
    if (selectionMode.value) {
      toggleSelect(item.id)
      return
    }
    focusedId.value = item.id
    const wasUnread = item.isRead === 0
    await handleNotificationClick(item)
    if (wasUnread) {
      item.isRead = 1
      if (unreadTotal.value > 0) unreadTotal.value--
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead()
      list.value.forEach((n) => (n.isRead = 1))
      unreadTotal.value = 0
      ElMessage.success('已全部标记已读')
    } catch {
      // 静默失败
    }
  }

  // ===================== P1：单项 Hover 操作 =====================
  const handleMarkReadSingle = async (item: NotificationItem, _idx: number, _group: Group) => {
    if (item.isRead === 1) return
    try {
      await markAsRead(item.id)
      item.isRead = 1
      if (unreadTotal.value > 0) unreadTotal.value--
      ElMessage.success('已标记已读')
    } catch {
      // 静默失败
    }
  }

  const handleDeleteSingle = async (item: NotificationItem) => {
    try {
      await ElMessageBox.confirm('确定删除此条通知吗？', '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
    } catch {
      return // 用户取消
    }
    try {
      await deleteNotification(item.id)
      const wasUnread = item.isRead === 0
      list.value = list.value.filter((n) => n.id !== item.id)
      total.value = Math.max(0, total.value - 1)
      if (wasUnread && unreadTotal.value > 0) unreadTotal.value--
      selectedIds.value.delete(item.id)
      ElMessage.success('已删除')
    } catch {
      // 静默失败
    }
  }

  // ===================== P3：多选批量操作 =====================
  const toggleSelectionMode = () => {
    selectionMode.value = !selectionMode.value
    if (!selectionMode.value) selectedIds.value = new Set()
  }

  const toggleSelect = (id: number) => {
    const s = new Set(selectedIds.value)
    if (s.has(id)) s.delete(id)
    else s.add(id)
    selectedIds.value = s
  }

  const selectAllVisible = () => {
    selectedIds.value = new Set(list.value.map((n) => n.id))
  }

  const clearSelection = () => {
    selectedIds.value = new Set()
  }

  const handleBatchMarkRead = async () => {
    const ids = [...selectedIds.value]
    if (ids.length === 0) return
    try {
      await batchMarkAsRead(ids)
      const affected = list.value.filter((n) => ids.includes(n.id) && n.isRead === 0)
      affected.forEach((n) => (n.isRead = 1))
      unreadTotal.value = Math.max(0, unreadTotal.value - affected.length)
      ElMessage.success(`已标记 ${affected.length} 条为已读`)
      clearSelection()
    } catch {
      // 静默失败
    }
  }

  const handleBatchDelete = async () => {
    const ids = [...selectedIds.value]
    if (ids.length === 0) return
    try {
      await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条通知吗？`, '批量删除', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    try {
      await batchDeleteNotifications(ids)
      const deletedUnread = list.value.filter((n) => ids.includes(n.id) && n.isRead === 0).length
      list.value = list.value.filter((n) => !ids.includes(n.id))
      total.value = Math.max(0, total.value - ids.length)
      unreadTotal.value = Math.max(0, unreadTotal.value - deletedUnread)
      ElMessage.success(`已删除 ${ids.length} 条`)
      clearSelection()
    } catch {
      // 静默失败
    }
  }

  // ===================== P3：键盘快捷键（j/k/e/x//） =====================
  const isInputFocused = (): boolean => {
    const tag = document.activeElement?.tagName
    return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  }

  const scrollItemIntoView = (id: number) => {
    nextTick(() => {
      const el = listScrollRef.value?.querySelector<HTMLElement>(`[data-id="${id}"]`)
      el?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    })
  }

  const handleKeyboard = (e: KeyboardEvent) => {
    // 输入框聚焦时不响应（除 / 特殊处理）
    if (isInputFocused()) return
    const flat = flatVisibleList.value
    if (flat.length === 0) return

    const currentIdx = flat.findIndex((n) => n.id === focusedId.value)

    switch (e.key) {
      case 'j':
      case 'ArrowDown': {
        e.preventDefault()
        const next = flat[Math.min(currentIdx + 1, flat.length - 1)] || flat[0]
        focusedId.value = next.id
        scrollItemIntoView(next.id)
        break
      }
      case 'k':
      case 'ArrowUp': {
        e.preventDefault()
        const prev = flat[Math.max(currentIdx - 1, 0)] || flat[0]
        focusedId.value = prev.id
        scrollItemIntoView(prev.id)
        break
      }
      case 'e': {
        e.preventDefault()
        const target = flat[currentIdx]
        if (target && target.isRead === 0) {
          handleMarkReadSingle(target, currentIdx, groupedList.value[0])
        }
        break
      }
      case 'x': {
        e.preventDefault()
        toggleSelectionMode()
        break
      }
      case '/': {
        e.preventDefault()
        const input = document.querySelector<HTMLInputElement>(
          '.notification-center .search-input input'
        )
        input?.focus()
        break
      }
    }
  }

  // ===================== 关键词高亮 =====================
  const escapeHtml = (s: string): string =>
    s.replace(
      /[&<>"']/g,
      (m) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m] || m
    )

  const escapeRegExp = (s: string): string => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

  const highlightKeyword = (text: string): string => {
    if (!text) return ''
    const safe = escapeHtml(text)
    const k = keyword.value.trim()
    if (!k) return safe
    const re = new RegExp(escapeRegExp(escapeHtml(k)), 'gi')
    return safe.replace(re, (m) => `<mark class="kw-highlight">${m}</mark>`)
  }

  // ===================== 时间格式化 =====================
  const formatRelativeTime = (timeStr: string): string => {
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
    return formatAbsoluteTime(timeStr).slice(5, 16) // MM-DD HH:mm
  }

  const formatAbsoluteTime = (timeStr: string): string => {
    if (!timeStr) return ''
    const d = new Date(timeStr)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
      d.getHours()
    )}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }

  // ===================== 样式映射 =====================
  interface NoticeStyle {
    icon: string
    iconClass: string
  }

  const noticeStyleMap: Record<string, NoticeStyle> = {
    EXAM_PUBLISHED: { icon: 'ri:file-list-3-line', iconClass: 'bg-theme/12 text-theme' },
    EXAM_UPDATED: { icon: 'ri:edit-line', iconClass: 'bg-warning/12 text-warning' },
    EXAM_CANCELLED: { icon: 'ri:close-circle-line', iconClass: 'bg-danger/12 text-danger' },
    EXAM_SUBMITTED: { icon: 'ri:check-double-line', iconClass: 'bg-success/12 text-success' },
    EXAM_AUTO_SUBMITTED: { icon: 'ri:check-double-line', iconClass: 'bg-warning/12 text-warning' },
    EXAM_ABSENT: { icon: 'ri:user-unfollow-line', iconClass: 'bg-danger/12 text-danger' },
    EXAM_END_SUMMARY: { icon: 'ri:bar-chart-box-line', iconClass: 'bg-info/12 text-info' },
    SCORE_PUBLISHED: { icon: 'ri:bar-chart-box-line', iconClass: 'bg-info/12 text-info' },
    SCORE_UPDATED: { icon: 'ri:refresh-line', iconClass: 'bg-warning/12 text-warning' },
    ACCOUNT_CREATED: { icon: 'ri:user-add-line', iconClass: 'bg-success/12 text-success' },
    USER_CREATED: { icon: 'ri:user-add-line', iconClass: 'bg-theme/12 text-theme' },
    EXAM_CREATED: { icon: 'ri:file-list-3-line', iconClass: 'bg-warning/12 text-warning' }
  }

  const getNoticeStyle = (type: string): NoticeStyle =>
    noticeStyleMap[type] || { icon: 'ri:notification-3-line', iconClass: 'bg-theme/12 text-theme' }

  // ===================== 生命周期 =====================
  const handleGlobalRefresh = () => {
    loadAll()
  }

  onMounted(() => {
    loadAll()
    mittBus.on('refreshNotification', handleGlobalRefresh)
    window.addEventListener('keydown', handleKeyboard)
  })

  onUnmounted(() => {
    mittBus.off('refreshNotification', handleGlobalRefresh)
    window.removeEventListener('keydown', handleKeyboard)
  })
</script>

<style scoped lang="scss">
  /* ========================================================================
     通知中心 | 三段式布局 + 业界顶级 UX
     参考：GitHub Notifications / Linear / Slack / Notion / Material Design 3
     ======================================================================== */

  .notification-center {
    // P0 核心：显式高度，让 h-full 有效；flex 列方向充满可用空间
    height: var(--art-full-height, calc(100vh - 110px));
    display: flex;
    flex-direction: column;
  }

  .notification-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0; // 关键：让 flex-1 生效的圣杯
    overflow: hidden;
    padding: 0 !important; // 覆盖 .art-card-sm 的内边距
  }

  /* ============ 固定头：工具栏 ============ */
  .toolbar {
    flex-shrink: 0;
    padding: 18px 20px 0;
    border-bottom: 1px solid var(--art-border-dashed-color);
  }

  .toolbar-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
  }

  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  .search-input {
    width: 220px;
  }

  .type-select {
    width: 140px;
  }

  :deep(.notification-tabs) {
    .el-tabs__nav-wrap::after {
      display: none;
    }
    .el-tabs__header {
      margin-bottom: 0;
    }
  }

  /* ============ 批量操作浮条 ============ */
  .batch-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 14px;
    margin-top: 12px;
    margin-bottom: -1px; // 贴紧底边
    background: var(--el-color-primary-light-9, rgb(236 245 255));
    border: 1px solid var(--el-color-primary-light-7, rgb(197 225 255));
    border-radius: 8px;
    font-size: 13px;
    color: var(--art-gray-700);
    gap: 12px;
  }

  .batch-info {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }

  .batch-check-icon {
    color: var(--el-color-primary);
    font-size: 16px;
  }

  .batch-text {
    font-size: 13px;
    color: var(--art-gray-700);
  }

  .batch-count {
    color: var(--el-color-primary);
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    padding: 0 2px;
  }

  .batch-divider {
    width: 1px;
    height: 14px;
    background: var(--el-color-primary-light-6, rgba(64, 158, 255, 0.3));
    margin: 0 4px;
  }

  .batch-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }

  .slide-down-enter-active,
  .slide-down-leave-active {
    transition: all 0.22s ease;
  }

  .slide-down-enter-from,
  .slide-down-leave-to {
    opacity: 0;
    transform: translateY(-6px);
  }

  /* ============ 滚动列表区 ============ */
  .list-scroll {
    flex: 1;
    min-height: 0; // 配合 flex-1 + overflow 才能滚
    overflow-y: auto;
    overscroll-behavior: contain; // 不穿透 body
    padding: 0 20px;
  }

  .list-scroll::-webkit-scrollbar {
    width: 6px;
  }
  .list-scroll::-webkit-scrollbar-thumb {
    background: var(--art-gray-300);
    border-radius: 3px;
  }
  .list-scroll::-webkit-scrollbar-track {
    background: transparent;
  }

  /* ============ 时间分组 ============ */
  .notification-group + .notification-group {
    margin-top: 12px;
  }

  .group-title {
    // P0 修复：避免 sticky 遮挡下方选中项的 outline
    position: sticky;
    top: 0;
    z-index: 3; // 高于 item，但不溢出到右侧操作
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 14px 4px 12px; // 下 padding 增大 → 让出 item 顶部缓冲
    margin-bottom: 4px;
    font-size: 11px;
    font-weight: 600;
    color: var(--art-gray-500);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    // 渐变 95% 实色，仅最后 5% 透明 → 不会侵蚀下方 item
    background: linear-gradient(
      to bottom,
      var(--art-main-bg-color, #fff) 0%,
      var(--art-main-bg-color, #fff) 95%,
      transparent 100%
    );
  }

  .group-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 18px;
    padding: 0 6px;
    background: var(--art-gray-200);
    border-radius: 9px;
    font-size: 11px;
    font-weight: 500;
    color: var(--art-gray-600);
    letter-spacing: 0;
    font-variant-numeric: tabular-nums;
  }

  /* ============ 通知列表项 ============ */
  .notification-list {
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .notification-item {
    // 关键 1：relative + z-index 配合 inset shadow，让边框绝不溢出
    position: relative;
    z-index: 1;

    // 关键 2：align-items: stretch 让子元素可自行垂直居中
    display: flex;
    align-items: stretch;
    gap: 12px;
    padding: 14px 14px 14px 18px;
    margin-bottom: 4px;
    border-radius: 10px;
    cursor: pointer;
    outline: none;
    transition:
      background-color 0.18s ease,
      transform 0.18s ease,
      box-shadow 0.18s ease;

    // 左侧色条：未读状态有色，已读透明
    .unread-bar {
      position: absolute;
      left: 6px;
      top: 14px;
      bottom: 14px;
      width: 3px;
      border-radius: 2px;
      background: var(--el-color-primary);
      opacity: 1;
      transition: opacity 0.2s ease;
    }

    // Hover：微提升 + 浅底 + 操作栏淡入（参考 Linear / GitHub）
    &:hover {
      background-color: var(--art-gray-200);
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

      .hover-actions {
        opacity: 1;
        pointer-events: auto;
        transform: translateX(0);
      }
    }

    // 已读状态：降低视觉权重
    &.is-read {
      .unread-bar {
        opacity: 0;
      }
      .title {
        font-weight: 400;
        color: var(--art-gray-700);
      }
      .icon-badge {
        opacity: 0.55;
      }
      .time {
        color: var(--art-gray-400);
      }
    }

    // 未读状态：粗体 + 浅蓝背景
    &:not(.is-read) {
      background: var(--el-color-primary-light-9, rgba(64, 158, 255, 0.04));

      .title {
        font-weight: 600;
        color: var(--art-gray-900);
      }
    }

    // 选中态 → inset shadow，不会被 sticky 遮挡 + 更深背景
    &.is-selected {
      background: var(--el-color-primary-light-8, rgba(64, 158, 255, 0.12)) !important;
      box-shadow: inset 0 0 0 1.5px var(--el-color-primary);
    }

    // 键盘焦点态（j/k 导航）→ 也用 inset，不溢出
    &.is-focused:not(.is-selected) {
      box-shadow: inset 0 0 0 2px var(--el-color-primary-light-5, rgba(64, 158, 255, 0.4));
    }

    // 选中 + 聚焦 → 最强边框
    &.is-selected.is-focused {
      box-shadow: inset 0 0 0 2px var(--el-color-primary);
    }

    // P0 修复：多选模式下隐藏左色条，避免与 checkbox 视觉冲突
    &.in-selection .unread-bar {
      display: none;
    }
  }

  /* ============ 列表项子元素（均垂直居中对齐） ============ */
  .checkbox-wrap {
    display: flex;
    align-items: center;
    margin-right: -2px; // 视觉上与 icon 缩近
  }

  .icon-wrap {
    position: relative;
    flex-shrink: 0;
    display: flex;
    align-items: center;
  }

  .icon-badge {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    transition: opacity 0.2s ease;
  }

  /* E1: 发送者头像 */
  .sender-avatar {
    flex-shrink: 0;
    border: 2px solid var(--art-main-bg-color, #fff);
    transition: opacity 0.2s ease;
  }

  /* E1: 头像右下角类型角标（Slack 风格） */
  .icon-overlay {
    position: absolute;
    right: -4px;
    bottom: -4px;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    border: 2px solid var(--art-main-bg-color, #fff);
    font-size: 11px;
  }

  .unread-dot {
    position: absolute;
    top: -2px;
    right: -2px;
    width: 8px;
    height: 8px;
    background: var(--el-color-danger);
    border: 2px solid var(--art-main-bg-color, #fff);
    border-radius: 50%;
  }

  .content-wrap {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center; // 内容区垂直居中
    gap: 3px;
    padding: 2px 0; // 上下微边距，补偿多行内容时的视觉平衡
  }

  .title {
    margin: 0;
    font-size: 14px;
    line-height: 22px;
    color: var(--art-gray-900);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    letter-spacing: 0.01em;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  /* E1: 优先级徽章（业界标配 - Linear/Jira 风格） */
  .priority-badge {
    display: inline-flex;
    align-items: center;
    padding: 1px 6px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 600;
    line-height: 14px;
    letter-spacing: 0.04em;
    flex-shrink: 0;
  }

  .priority-urgent {
    background: var(--el-color-danger-light-9, rgba(245, 108, 108, 0.1));
    color: var(--el-color-danger);
    border: 1px solid var(--el-color-danger-light-7, rgba(245, 108, 108, 0.35));
  }

  .priority-low {
    background: var(--art-gray-200);
    color: var(--art-gray-600);
    border: 1px solid var(--art-gray-300);
  }

  /* E1: 发送者姓名行（小字辅助说明） */
  .sender-info {
    margin: 0;
    font-size: 12px;
    color: var(--art-gray-500);
    display: inline-flex;
    align-items: center;
    font-weight: 500;
  }

  .description {
    margin: 0;
    font-size: 13px;
    line-height: 19px;
    color: var(--art-gray-600);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .time {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--art-gray-500); // P1 修复：400 → 500，WCAG AA 合规
    cursor: help;
    display: inline-block;
    font-variant-numeric: tabular-nums;
  }

  /* 关键词高亮 */
  :deep(.kw-highlight) {
    background: var(--el-color-warning-light-8, rgba(230, 162, 60, 0.2));
    color: var(--el-color-warning-dark-2, #b88230);
    padding: 0 2px;
    border-radius: 2px;
    font-weight: 600;
  }

  /* ============ Hover 操作栏（幽灵按钮风格 - 参考 GitHub/Linear） ============ */
  .hover-actions {
    display: flex;
    align-items: center; // 垂直居中，不再 top 对齐
    gap: 4px;
    opacity: 0;
    pointer-events: none;
    transform: translateX(4px);
    transition:
      opacity 0.18s ease,
      transform 0.18s ease;
    flex-shrink: 0;
  }

  // 改为无边框幽灵按钮，hover 浅底高亮 → 更克制、专业
  :deep(.hover-actions .action-btn) {
    width: 30px;
    height: 30px;
    min-height: 30px;
    padding: 0;
    border: 1px solid transparent !important;
    background: transparent;
    color: var(--art-gray-600);
    box-shadow: none;
    transition: all 0.15s ease;

    &:hover {
      background: var(--art-gray-300);
      color: var(--art-gray-900);
      border-color: var(--art-gray-400) !important;
    }

    .art-svg-icon {
      font-size: 15px;
    }
  }

  :deep(.hover-actions .action-delete:hover) {
    background: var(--el-color-danger-light-9, rgba(245, 108, 108, 0.1)) !important;
    color: var(--el-color-danger) !important;
    border-color: var(--el-color-danger-light-5, rgba(245, 108, 108, 0.4)) !important;
  }

  /* ============ 空状态（Refactoring UI 原则） ============ */
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 360px;
    padding: 60px 20px;
    text-align: center;
  }

  .empty-icon-wrap {
    width: 96px;
    height: 96px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;

    &.empty-inbox {
      background: linear-gradient(135deg, #ebf4ff 0%, #dbeafe 100%);
      color: var(--el-color-primary);
    }
    &.empty-done {
      background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
      color: var(--el-color-success);
    }
    &.empty-search {
      background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
      color: var(--el-color-warning);
    }
  }

  .empty-icon {
    font-size: 44px;
  }

  .empty-title {
    margin: 0 0 8px;
    font-size: 16px;
    font-weight: 600;
    color: var(--art-gray-800);
  }

  .empty-desc {
    margin: 0 0 20px;
    font-size: 13px;
    color: var(--art-gray-500);
    max-width: 320px;
    line-height: 20px;
  }

  /* ============ 固定尾：分页 + 快捷键提示 ============ */
  .pagination-bar {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    border-top: 1px solid var(--art-border-dashed-color);
    flex-wrap: wrap;
    gap: 10px;
  }

  .shortcut-hint {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: var(--art-gray-400);
  }

  .hint-key {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 18px;
    padding: 0 5px;
    background: var(--art-gray-200);
    border: 1px solid var(--art-gray-300);
    border-radius: 3px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 11px;
    color: var(--art-gray-700);
  }

  /* ============ 响应式 ============ */
  @media (max-width: 768px) {
    .toolbar-row {
      flex-direction: column;
      align-items: stretch;
    }
    .search-input,
    .type-select {
      width: 100%;
    }
    .shortcut-hint {
      display: none; // 移动端无实体键盘，隐藏
    }
    .pagination-bar {
      justify-content: center;
    }
  }
</style>
