<template>
  <div>
    <!-- 统计概览 -->
    <ElRow :gutter="20">
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-blue-500">全部考试</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="exams.length" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-blue-50 dark:bg-blue-900/20"><ArtSvgIcon icon="ri:file-list-3-line" class="text-xl text-blue-500" /></div>
        </div>
      </ElCol>
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-amber-500">待参加</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="pendingCount" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-amber-50 dark:bg-amber-900/20"><ArtSvgIcon icon="ri:time-line" class="text-xl text-amber-500" /></div>
        </div>
      </ElCol>
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-green-500">已完成</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="completedCount" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-green-50 dark:bg-green-900/20"><ArtSvgIcon icon="ri:checkbox-circle-line" class="text-xl text-green-500" /></div>
        </div>
      </ElCol>
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-red-500">缺考</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="expiredCount" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-red-50 dark:bg-red-900/20"><ArtSvgIcon icon="ri:error-warning-line" class="text-xl text-red-500" /></div>
        </div>
      </ElCol>
    </ElRow>

    <!-- 筛选标签 + 考试卡片 -->
    <div class="art-card mb-5 max-sm:mb-4">
      <div class="flex items-center justify-between px-5 border-b border-g-300">
        <div class="flex gap-0">
          <button v-for="tab in tabs" :key="tab.key"
            class="relative px-5 py-3.5 text-sm font-medium transition-colors"
            :class="currentFilter === tab.key ? 'text-blue-500' : 'text-gray-500 hover:text-gray-700'"
            @click="currentFilter = tab.key">
            {{ tab.label }}
            <span class="ml-1.5 px-1.5 py-0.5 text-[11px] rounded-full"
              :class="currentFilter === tab.key ? 'bg-blue-50 text-blue-500' : 'bg-gray-100 text-gray-400'">
              {{ tab.count }}
            </span>
            <div v-if="currentFilter === tab.key" class="absolute bottom-0 left-0 w-full h-0.5 bg-blue-500 rounded-t"></div>
          </button>
        </div>
        <div class="flex items-center gap-1 p-0.5 rounded-lg bg-gray-100/80 dark:bg-gray-800/60">
          <button class="view-toggle-btn" :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'" title="卡片视图">
            <ArtSvgIcon icon="ri:grid-fill" class="text-sm" />
          </button>
          <button class="view-toggle-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'" title="列表视图">
            <ArtSvgIcon icon="ri:list-check" class="text-sm" />
          </button>
        </div>
      </div>

      <div class="p-5">
        <el-empty v-if="!filteredExams.length" description="暂无相关考试安排" :image-size="100" />

        <!-- 卡片视图 -->
        <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          <div v-for="exam in filteredExams" :key="exam.id"
            class="art-card-sm overflow-hidden hover:shadow-lg transition-all duration-300 hover:-translate-y-1 flex flex-col">
            <div class="p-5 flex-1 space-y-3">
              <div class="flex items-center justify-between">
                <el-tag :type="statusTagType(exam)" effect="light" round size="small">{{ exam.statusName }}</el-tag>
                <span class="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full bg-blue-50 text-blue-500 dark:bg-blue-900/20 dark:text-blue-400"><ArtSvgIcon icon="ri:timer-line" class="text-xs" />{{ getExamDuration(exam) }}</span>
              </div>
              <h3 class="text-base font-semibold text-gray-800 dark:text-gray-200 line-clamp-2 leading-snug">{{ exam.examName }}</h3>
              <div class="space-y-2.5 text-sm">
                <div class="flex items-center justify-between"><span class="flex items-center gap-1.5 text-gray-400"><ArtSvgIcon icon="ri:book-2-line" class="text-sm" />科目</span><span class="text-gray-700 dark:text-gray-300">{{ exam.subjectName }}</span></div>
                <div class="flex items-center justify-between"><span class="flex items-center gap-1.5 text-gray-400"><ArtSvgIcon icon="ri:award-line" class="text-sm" />满分</span><span class="text-gray-700 dark:text-gray-300 font-semibold">{{ exam.totalScore }} 分</span></div>
                <div class="flex items-center justify-between"><span class="flex items-center gap-1.5 text-gray-400"><ArtSvgIcon icon="ri:calendar-schedule-line" class="text-sm" />时间</span>
                  <span class="text-gray-500 text-xs text-right leading-relaxed">{{ formatTime(exam.startTime) }}<br/>至 {{ formatTime(exam.endTime) }}</span>
                </div>
              </div>
            </div>
            <div class="px-5 py-3.5 border-t border-g-300 flex justify-center bg-gray-50/50 dark:bg-gray-800/30">
              <el-button v-if="exam.status === 1 && (exam.recordStatus == null)" type="primary" class="w-full" round @click="router.push(`/student/exam/do/${exam.id}`)">开始考试</el-button>
              <el-button v-else-if="exam.status === 1 && exam.recordStatus === 1" type="warning" class="w-full" round @click="router.push(`/student/exam/do/${exam.id}`)">继续考试</el-button>
              <div v-else-if="exam.status === 2 && exam.recordStatus === 1" class="flex items-center gap-1.5 text-warning font-semibold text-sm"><ArtSvgIcon icon="ri:error-warning-line" class="text-base" />未提交（已超时）</div>
              <div v-else-if="exam.recordStatus === 4 && exam.scorePublished" class="flex items-center justify-between w-full">
                <div class="flex items-center gap-1.5 text-danger font-semibold text-sm"><ArtSvgIcon icon="ri:error-warning-line" class="text-base" />缺考 - 得分：0</div>
                <el-button size="small" round @click="router.push(`/my-study/score/${exam.recordId}`)">查看详情</el-button>
              </div>
              <div v-else-if="exam.recordStatus === 4" class="flex items-center gap-1.5 text-danger font-semibold text-sm"><ArtSvgIcon icon="ri:error-warning-line" class="text-base" />缺考 - 成绩待发布</div>
              <div v-else-if="exam.recordStatus === 2 || (exam.recordStatus === 3 && !exam.scorePublished)" class="flex items-center gap-1.5 text-amber-500 font-semibold text-sm"><ArtSvgIcon icon="ri:time-line" class="text-base" />已交卷，待老师批阅</div>
              <div v-else-if="exam.recordStatus === 3 && exam.scorePublished" class="flex items-center justify-between w-full">
                <div class="flex items-center gap-1.5 text-success font-semibold text-sm"><ArtSvgIcon icon="ri:checkbox-circle-line" class="text-base" />得分：{{ exam.score ?? '-' }}</div>
                <el-button size="small" round @click="router.push(`/my-study/score/${exam.recordId}`)">查看成绩</el-button>
              </div>
              <div v-else-if="exam.status === 2 && exam.recordStatus == null" class="flex items-center gap-1.5 text-danger font-medium text-sm"><ArtSvgIcon icon="ri:error-warning-line" class="text-base" />考试已过期</div>
              <div v-else-if="exam.status === 0" class="flex items-center gap-1.5 text-gray-400 font-medium text-sm"><ArtSvgIcon icon="ri:hourglass-line" class="text-base" />等待开考</div>
            </div>
          </div>
        </div>

        <!-- 列表视图 -->
        <div v-else class="space-y-3">
          <div v-for="exam in filteredExams" :key="exam.id" class="art-card-sm exam-list-item">
            <!-- 左侧：图标 + 考试信息 -->
            <div class="flex items-center gap-3.5 flex-1 min-w-0">
              <div class="exam-list-icon" :class="listIconClass(exam)">
                <ArtSvgIcon :icon="listIconName(exam)" class="text-base" />
              </div>
              <div class="min-w-0 flex-1">
                <h3 class="text-[13px] font-semibold text-gray-800 dark:text-gray-200 truncate">{{ exam.examName }}</h3>
                <div class="flex items-center gap-3 mt-1.5 text-xs text-gray-400">
                  <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:book-2-line" class="text-[11px]" />{{ exam.subjectName }}</span>
                  <span class="hidden sm:flex items-center gap-1"><ArtSvgIcon icon="ri:timer-line" class="text-[11px]" />{{ getExamDuration(exam) }}</span>
                  <span class="hidden sm:flex items-center gap-1"><ArtSvgIcon icon="ri:award-line" class="text-[11px]" />{{ exam.totalScore }}分</span>
                  <span class="hidden md:flex items-center gap-1"><ArtSvgIcon icon="ri:calendar-schedule-line" class="text-[11px]" />{{ formatTime(exam.startTime) }} ~ {{ formatTime(exam.endTime) }}</span>
                </div>
              </div>
            </div>
            <!-- 右侧：状态 + 操作 -->
            <div class="flex items-center gap-2.5 flex-shrink-0 ml-4">
              <el-tag :type="statusTagType(exam)" effect="light" round size="small">{{ exam.statusName }}</el-tag>
              <el-button v-if="exam.status === 1 && (exam.recordStatus == null)" type="primary" round size="small" @click="router.push(`/student/exam/do/${exam.id}`)">开始考试</el-button>
              <el-button v-else-if="exam.status === 1 && exam.recordStatus === 1" type="warning" round size="small" @click="router.push(`/student/exam/do/${exam.id}`)">继续考试</el-button>
              <span v-else-if="exam.status === 2 && exam.recordStatus === 1" class="text-warning text-xs font-semibold whitespace-nowrap">未提交</span>
              <template v-else-if="exam.recordStatus === 4">
                <span class="text-danger text-xs font-semibold whitespace-nowrap">缺考</span>
                <el-button v-if="exam.scorePublished" size="small" round @click="router.push(`/my-study/score/${exam.recordId}`)">详情</el-button>
              </template>
              <span v-else-if="exam.recordStatus === 2 || (exam.recordStatus === 3 && !exam.scorePublished)" class="text-amber-500 text-xs font-semibold whitespace-nowrap">待批阅</span>
              <template v-else-if="exam.recordStatus === 3 && exam.scorePublished">
                <span class="text-success text-sm font-bold whitespace-nowrap">{{ exam.score ?? '-' }}分</span>
                <el-button size="small" round @click="router.push(`/my-study/score/${exam.recordId}`)">查看</el-button>
              </template>
              <span v-else-if="exam.status === 2 && exam.recordStatus == null" class="text-danger text-xs font-medium whitespace-nowrap">已过期</span>
              <span v-else-if="exam.status === 0" class="text-gray-400 text-xs font-medium whitespace-nowrap">等待开考</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated, onBeforeUnmount, onDeactivated } from 'vue'
import { useRouter } from 'vue-router'
import { getMyExams } from '@/api/exam/studentExam'
import { formatDateTime as formatTime } from '@/utils/exam-format'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'MyExam' })

const router = useRouter()
const exams = ref<any[]>([])
const currentFilter = ref('all')
const viewMode = ref<'card' | 'list'>('card')

const pendingCount = computed(() => exams.value.filter(e => (e.status === 0 || e.status === 1) && (!e.recordStatus || e.recordStatus < 2)).length)
const completedCount = computed(() => exams.value.filter(e => e.recordStatus >= 2 && e.recordStatus !== 4).length)
const expiredCount = computed(() => exams.value.filter(e => e.recordStatus === 4).length)

const tabs = computed(() => [
  { key: 'all', label: '全部考试', count: exams.value.length },
  { key: 'pending', label: '待参加', count: pendingCount.value },
  { key: 'completed', label: '已完成', count: completedCount.value }
])

const filteredExams = computed(() => {
  if (currentFilter.value === 'pending') return exams.value.filter(e => (e.status === 0 || e.status === 1) && (!e.recordStatus || e.recordStatus < 2))
  if (currentFilter.value === 'completed') return exams.value.filter(e => e.recordStatus >= 2 && e.recordStatus !== 4)
  return exams.value
})

function getExamDuration(exam: any): string {
  if (!exam.startTime || !exam.endTime) return exam.duration ? `${exam.duration}分钟` : '--'
  const ms = new Date(exam.endTime).getTime() - new Date(exam.startTime).getTime()
  const totalMin = Math.round(ms / 60000)
  if (totalMin >= 60) {
    const h = Math.floor(totalMin / 60)
    const m = totalMin % 60
    return m > 0 ? `${h}小时${m}分钟` : `${h}小时`
  }
  return `${totalMin}分钟`
}

function listIconClass(exam: any) {
  if (exam.status === 1 && (!exam.recordStatus || exam.recordStatus < 2)) return 'icon-primary'
  if (exam.recordStatus === 3) return 'icon-success'
  if (exam.recordStatus === 2) return 'icon-warning'
  if (exam.recordStatus === 4) return 'icon-danger'
  if (exam.status === 0) return 'icon-info'
  return 'icon-danger'
}

function listIconName(exam: any) {
  if (exam.status === 1 && (!exam.recordStatus || exam.recordStatus < 2)) return 'ri:edit-circle-line'
  if (exam.recordStatus === 3) return 'ri:checkbox-circle-line'
  if (exam.recordStatus === 2) return 'ri:time-line'
  if (exam.recordStatus === 4) return 'ri:error-warning-line'
  if (exam.status === 0) return 'ri:hourglass-line'
  return 'ri:file-list-3-line'
}

function statusTagType(exam: any) {
  if (exam.recordStatus === 2) return 'warning'
  if (exam.recordStatus === 3) return 'success'
  if (exam.recordStatus === 4) return 'danger'
  if (exam.status === 1) return 'primary'
  if (exam.status === 0) return 'info'
  return 'danger'
}

let pollTimer: ReturnType<typeof setInterval> | null = null
let statusTimer: ReturnType<typeof setInterval> | null = null

async function refreshExams() {
  exams.value = await getMyExams()
}

// 本地定时器：每秒重新计算考试状态（未开始/进行中/已结束），无需等待后端轮询
function recalcStatus() {
  const now = new Date()
  const statusNames = ['未开始', '进行中', '已结束']
  for (const exam of exams.value) {
    const start = new Date(exam.startTime)
    const end = new Date(exam.endTime)
    let s = 0
    if (now >= start && now <= end) s = 1
    else if (now > end) s = 2
    exam.status = s
    exam.statusName = statusNames[s]
  }
}
function startStatusTimer() { stopStatusTimer(); statusTimer = setInterval(recalcStatus, 1000) }
function stopStatusTimer() { if (statusTimer) { clearInterval(statusTimer); statusTimer = null } }

function startPolling() {
  stopPolling()
  pollTimer = setInterval(refreshExams, 15000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

// WebSocket 考试事件监听：教师发布成绩时即时刷新考试卡片
function onExamEvent(_event: any) {
  refreshExams()
}

onMounted(async () => {
  await refreshExams()
  startPolling()
  startStatusTimer()
  mittBus.on('examEvent', onExamEvent)
})
onActivated(async () => {
  await refreshExams()
  startPolling()
  startStatusTimer()
})
onDeactivated(() => { stopPolling(); stopStatusTimer() })
onBeforeUnmount(() => { stopPolling(); stopStatusTimer(); mittBus.off('examEvent', onExamEvent) })
</script>

<style scoped>
/* 视图切换按钮 */
.view-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 28px;
  border-radius: 6px;
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  background: transparent;
}
.view-toggle-btn:hover { color: var(--el-text-color-regular); }
.view-toggle-btn.active {
  background: var(--default-box-color, #fff);
  color: var(--el-color-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* 列表视图行 */
.exam-list-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  transition: all 0.25s ease;
}
.exam-list-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

/* 列表图标 */
.exam-list-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-primary { background: rgba(64, 158, 255, 0.1); color: var(--el-color-primary); }
.icon-success { background: rgba(103, 194, 58, 0.1); color: var(--el-color-success); }
.icon-warning { background: rgba(230, 162, 60, 0.1); color: var(--el-color-warning); }
.icon-danger { background: rgba(245, 108, 108, 0.1); color: var(--el-color-danger); }
.icon-info { background: rgba(144, 147, 153, 0.1); color: var(--el-color-info); }
</style>
