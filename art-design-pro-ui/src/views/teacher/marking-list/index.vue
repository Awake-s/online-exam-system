<template>
  <div class="marking-list-page">
    <!-- 顶部统计指标 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-5 max-sm:mb-4">
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-amber-50 dark:bg-amber-900/20">
          <ArtSvgIcon icon="ri:file-warning-line" class="text-xl text-amber-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">待批改</div>
          <div class="text-2xl font-bold text-amber-500 leading-tight mt-0.5">{{ totalPending }}</div>
        </div>
      </div>
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-green-50 dark:bg-green-900/20">
          <ArtSvgIcon icon="ri:checkbox-circle-line" class="text-xl text-green-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">已批改</div>
          <div class="text-2xl font-bold text-green-500 leading-tight mt-0.5">{{ totalGraded }}</div>
        </div>
      </div>
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-blue-50 dark:bg-blue-900/20">
          <ArtSvgIcon icon="ri:pie-chart-line" class="text-xl text-blue-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">批改进度</div>
          <div class="flex items-center gap-2 mt-1">
            <div class="w-20 h-2 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all duration-500" :class="progressPercent === 100 ? 'bg-green-500' : 'bg-blue-500'" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="text-sm font-bold" :class="progressPercent === 100 ? 'text-green-500' : 'text-blue-500'">{{ progressPercent }}%</span>
          </div>
        </div>
      </div>
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-red-50 dark:bg-red-900/20">
          <ArtSvgIcon icon="ri:user-unfollow-line" class="text-xl text-red-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">缺考</div>
          <div class="text-2xl font-bold text-red-500 leading-tight mt-0.5">{{ totalAbsent }}</div>
        </div>
      </div>
    </div>

    <!-- 考试卡片（按试卷分组） -->
    <div v-if="groupedExams.length" class="space-y-3 mb-5 max-sm:mb-4">
      <div v-for="group in groupedExams" :key="group.name"
        class="rounded-xl border overflow-hidden"
        :class="group.allDone ? 'border-green-200 dark:border-green-800/30' : 'border-gray-200 dark:border-gray-700'">
        <!-- 分组标题栏 -->
        <div class="flex items-center justify-between px-4 py-2.5"
          :class="group.allDone ? 'bg-green-50/60 dark:bg-green-900/10' : 'bg-gray-50/80 dark:bg-gray-800/40'">
          <div class="flex items-center gap-2 min-w-0">
            <ArtSvgIcon :icon="group.allDone ? 'ri:checkbox-circle-fill' : 'ri:file-text-line'"
              :class="group.allDone ? 'text-green-500' : 'text-blue-500'" class="text-base shrink-0" />
            <span class="text-sm font-semibold text-gray-800 dark:text-gray-200 truncate">{{ group.name }}</span>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <span v-if="group.totalPending > 0"
              class="px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-100 dark:bg-amber-900/25 text-amber-600 dark:text-amber-400">
              {{ group.totalPending }} 待批改
            </span>
            <el-tag v-else-if="group.allPublished" type="success" size="small" effect="plain" round>已全部发布</el-tag>
            <span v-else
              class="px-2 py-0.5 rounded-full text-[11px] font-medium bg-green-100 dark:bg-green-900/25 text-green-600 dark:text-green-400">
              全部完成
            </span>
            <span class="text-[11px] text-gray-400">{{ group.exams.length }}个班级</span>
          </div>
        </div>
        <!-- 班级卡片（横向排列） -->
        <div class="flex gap-3 p-3 overflow-x-auto exam-card-scroll">
          <div v-for="exam in group.exams" :key="exam.id"
            class="w-[240px] min-w-[240px] rounded-lg border cursor-pointer transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5 overflow-hidden shadow-sm"
            :class="selectedExamIds.has(exam.id) ? 'ring-1 ring-blue-400 dark:ring-blue-500 border-blue-400 dark:border-blue-500 bg-blue-50/40 dark:bg-blue-900/15 shadow-md' : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800'"
            @click="selectExam(exam.id)">
            <div class="relative px-3.5 pt-3 pb-2.5">
              <!-- 多选复选框（右上角） -->
              <div class="absolute top-2 right-2" @click.stop>
                <el-checkbox
                  :model-value="selectedExamIds.has(exam.id)"
                  @change="toggleSelect(exam.id)"
                  size="small"
                />
              </div>
              <div class="flex items-center justify-between mb-2 pr-7">
                <span class="text-[13px] font-semibold text-gray-800 dark:text-gray-200 truncate">{{ exam.className }}</span>
                <div v-if="exam.pendingCount > 0" class="flex items-baseline gap-0.5 shrink-0 ml-2">
                  <span class="text-base font-bold text-amber-500 leading-none">{{ exam.pendingCount }}</span>
                  <span class="text-[10px] text-gray-400">待批改</span>
                </div>
                <span v-else class="inline-flex items-center gap-0.5 text-[11px] font-medium text-green-600 shrink-0 ml-2">
                  <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-xs" />已完成
                </span>
              </div>
              <div class="flex items-center gap-2">
                <div class="flex-1 h-1.5 bg-gray-100 dark:bg-gray-600 rounded-full overflow-hidden">
                  <div class="h-full rounded-full transition-all duration-500"
                    :class="exam.pendingCount === 0 ? 'bg-green-500' : 'bg-blue-500'"
                    :style="{ width: exam.totalCount ? ((exam.totalCount - exam.pendingCount) / exam.totalCount * 100) + '%' : '0%' }"></div>
                </div>
                <span class="text-[11px] text-gray-400 shrink-0">{{ exam.totalCount - exam.pendingCount }}/{{ exam.totalCount }}</span>
              </div>
            </div>
            <!-- 底部状态栏（所有卡片统一显示） -->
            <div class="flex items-center justify-between px-3.5 py-1.5 border-t" :class="
              exam.pendingCount === 0 && exam.totalCount > 0
                ? exam.scorePublished
                  ? 'border-blue-100 dark:border-blue-900/30 bg-blue-50/60 dark:bg-blue-900/10'
                  : isExamOngoing(exam)
                    ? 'border-gray-100 dark:border-gray-700 bg-gray-50/60 dark:bg-gray-800/30'
                    : 'border-green-100 dark:border-green-900/30 bg-green-50/60 dark:bg-green-900/10'
                : exam.totalCount > 0 && exam.pendingCount < exam.totalCount
                  ? 'border-blue-100 dark:border-blue-900/30 bg-blue-50/30 dark:bg-blue-900/5'
                  : 'border-gray-100 dark:border-gray-700 bg-gray-50/40 dark:bg-gray-800/20'
            " @click.stop>
              <template v-if="exam.pendingCount === 0 && exam.totalCount > 0 && exam.scorePublished">
                <span class="text-[11px] text-blue-600 dark:text-blue-400 font-medium flex items-center gap-1">
                  <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-[10px]" />已发布
                </span>
                <el-button type="warning" size="small" round
                  :loading="publishingExamIds.has(exam.id)"
                  :disabled="isExamOngoing(exam) || publishingExamIds.has(exam.id)"
                  @click="handlePublishById(exam.id)">
                  <ArtSvgIcon v-if="!publishingExamIds.has(exam.id)" icon="ri:refresh-line" class="mr-0.5 text-xs" />
                  {{ publishingExamIds.has(exam.id) ? '发布中' : '重发' }}
                </el-button>
              </template>
              <template v-else-if="exam.pendingCount === 0 && exam.totalCount > 0 && isExamOngoing(exam)">
                <span class="text-[11px] text-gray-400 font-medium flex items-center gap-1">
                  <ArtSvgIcon icon="ri:time-line" class="text-[10px]" />考试进行中，结束后可发布
                </span>
              </template>
              <template v-else-if="exam.pendingCount === 0 && exam.totalCount > 0">
                <span class="text-[11px] text-green-600 dark:text-green-400 font-medium">可发布</span>
                <el-button type="success" size="small" round
                  :loading="publishingExamIds.has(exam.id)"
                  :disabled="publishingExamIds.has(exam.id)"
                  @click="handlePublishById(exam.id)">
                  <ArtSvgIcon v-if="!publishingExamIds.has(exam.id)" icon="ri:send-plane-line" class="mr-0.5 text-xs" />
                  {{ publishingExamIds.has(exam.id) ? '发布中' : '发布' }}
                </el-button>
              </template>
              <template v-else-if="exam.totalCount > 0 && exam.pendingCount < exam.totalCount">
                <span class="text-[11px] text-blue-500 dark:text-blue-400 font-medium flex items-center gap-1">
                  <ArtSvgIcon icon="ri:edit-circle-line" class="text-[10px]" />批改中 {{ exam.totalCount - exam.pendingCount }}/{{ exam.totalCount }}
                </span>
                <el-button type="primary" size="small" round @click="goToMarking(exam.id)">
                  <ArtSvgIcon icon="ri:edit-line" class="mr-0.5 text-xs" />继续批改
                </el-button>
              </template>
              <template v-else-if="exam.totalCount > 0">
                <span class="text-[11px] text-gray-400 font-medium flex items-center gap-1">
                  <ArtSvgIcon icon="ri:file-list-3-line" class="text-[10px]" />待批改 {{ exam.totalCount }} 份
                </span>
                <el-button type="primary" size="small" round @click="goToMarking(exam.id)">
                  <ArtSvgIcon icon="ri:edit-line" class="mr-0.5 text-xs" />批改
                </el-button>
              </template>
              <template v-else>
                <span class="text-[11px] text-gray-400 font-medium flex items-center gap-1">
                  <ArtSvgIcon icon="ri:time-line" class="text-[10px]" />暂无记录
                </span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量发布浮动操作栏 -->
    <Transition name="el-fade-in">
      <div v-if="selectedExamIds.size > 0" class="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 px-5 py-3 rounded-2xl shadow-2xl border border-green-200 dark:border-green-800 bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm">
        <span class="text-sm text-gray-600 dark:text-gray-300">
          已选 <b class="text-blue-600 dark:text-blue-400">{{ selectedExamIds.size }}</b> 个班级
        </span>
        <el-button v-if="pendingSelectedCount > 0" type="primary" round @click="goToMarking()">
          <ArtSvgIcon icon="ri:edit-line" class="mr-1 text-sm" />
          批改（{{ pendingSelectedTotal }}）
        </el-button>
        <el-button v-if="newPublishCount > 0" type="success" round
          :loading="batchPublishing" :disabled="batchPublishing"
          @click="handleBatchPublish('new')">
          <ArtSvgIcon v-if="!batchPublishing" icon="ri:send-plane-line" class="mr-1 text-sm" />
          {{ batchPublishing ? '发布中...' : `发布成绩（${newPublishCount}）` }}
        </el-button>
        <el-button v-if="rePublishCount > 0" type="warning" round
          :loading="batchPublishing" :disabled="batchPublishing"
          @click="handleBatchPublish('re')">
          <ArtSvgIcon v-if="!batchPublishing" icon="ri:refresh-line" class="mr-1 text-sm" />
          {{ batchPublishing ? '发布中...' : `重新发布（${rePublishCount}）` }}
        </el-button>
        <el-button round size="small" @click="selectedExamIds = new Set()">
          取消选择
        </el-button>
      </div>
    </Transition>

    <!-- 批改列表 -->
    <ElCard ref="markingTableRef" class="art-table-card" shadow="never" style="margin-top: 0; margin-bottom: 20px">
      <template #header>
        <div class="flex-cb">
          <div class="flex items-center gap-3">
            <!-- <h4 class="m-0">{{ selectedExamLabel || '批改记录' }}</h4> -->
            <el-radio-group v-model="listFilter" size="small">
              <el-radio-button value="all">全部 {{ scopedRecords.length }}</el-radio-button>
              <el-radio-button value="pending">待批改 {{ scopedRecords.filter(r => r.statusName === '待批改').length }}</el-radio-button>
              <el-radio-button value="graded">已批改 {{ scopedRecords.filter(r => r.statusName === '已批改').length }}</el-radio-button>
              <el-radio-button value="absent">缺考 {{ scopedRecords.filter(r => r.statusName === '缺考').length }}</el-radio-button>
            </el-radio-group>
          </div>
          <div class="flex gap-2">
            <el-button v-if="selectedExamIds.size > 0" plain size="small" @click="selectedExamIds = new Set()">
              <ArtSvgIcon icon="ri:list-check" class="mr-1" />查看全部
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-if="!displayList.length" :description="listFilter === 'pending' ? '暂无待批改试卷' : listFilter === 'graded' ? '暂无已批改试卷' : listFilter === 'absent' ? '暂无缺考学生' : '暂无批改记录'" :image-size="100" />
      <el-table v-else :data="displayList" stripe style="width: 100%">
        <el-table-column label="学生" min-width="160">
          <template #default="{ row }">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-full flex-cc bg-blue-50 dark:bg-blue-900/20 text-xs font-bold text-blue-500 shrink-0">
                {{ row.realName?.charAt(0) }}
              </div>
              <div class="min-w-0">
                <div class="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{{ row.realName }}</div>
                <div class="text-[11px] text-gray-400">{{ row.className }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="examName" label="考试" show-overflow-tooltip min-width="200" />
        <el-table-column label="交卷时间" width="170">
          <template #default="{ row }">
            <span class="text-[13px] text-gray-500">{{ formatTime(row.submitTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="客观题" width="90" align="center">
          <template #default="{ row }">
            <span class="text-sm font-medium text-blue-500">{{ row.objectiveScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.statusName === '已批改'" class="text-sm font-bold text-green-600">{{ row.totalScore }}</span>
            <span v-else-if="row.statusName === '缺考'" class="text-sm font-bold text-red-500">0</span>
            <span v-else class="text-xs text-gray-300">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140" align="center" :show-overflow-tooltip="false" class-name="no-ellipsis">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1 flex-wrap">
              <el-tag :type="row.statusName === '已批改' ? 'success' : row.statusName === '缺考' ? 'danger' : 'warning'" size="small" round>{{ row.statusName }}</el-tag>
              <el-tag v-if="row.allBlank && row.statusName === '待批改'" type="info" size="small" round>空白卷</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <template v-if="row.statusName === '缺考'">
              <span class="text-xs text-gray-400">无需批改</span>
            </template>
            <template v-else>
              <el-button v-if="row.statusName === '已批改'" type="warning" size="small" plain @click="router.push(`/exam-center/marking/${row.recordId}`)">
                <ArtSvgIcon icon="ri:edit-line" class="mr-1" />重新批改
              </el-button>
              <el-button v-else-if="row.allBlank && !row.hasSubjective" type="info" size="small" @click="router.push(`/exam-center/marking/${row.recordId}`)">
                <ArtSvgIcon icon="ri:checkbox-circle-line" class="mr-1" />确认空白卷
              </el-button>
              <el-button v-else type="primary" size="small" @click="router.push(`/exam-center/marking/${row.recordId}`)">
                <ArtSvgIcon icon="ri:edit-line" class="mr-1" />批改
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated, onDeactivated, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExamList } from '@/api/exam/exam'
import { getPendingList, publishScores } from '@/api/exam/marking'
import { formatDateTime as formatTime } from '@/utils/exam-format'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'MarkingList' })

const router = useRouter()
const markingTableRef = ref()
const exams = ref<any[]>([])
const allPendingRecords = ref<any[]>([])
const selectedExamIds = ref<Set<number>>(new Set())
// 正在发布成绩的考试 ID 集合：按钮 loading/disabled 状态防双击
const publishingExamIds = ref<Set<number>>(new Set())
// 批量发布的 loading 状态
const batchPublishing = ref(false)
const now = ref(Date.now())
let statusTimer: ReturnType<typeof setInterval> | null = null
function startStatusTimer() { stopStatusTimer(); statusTimer = setInterval(() => { now.value = Date.now() }, 1000) }
function stopStatusTimer() { if (statusTimer) { clearInterval(statusTimer); statusTimer = null } }

const pendingExams = computed(() => exams.value.filter(e => (e.totalCount || 0) > 0))
const groupedExams = computed(() => {
  const groups = new Map<string, any[]>()
  for (const exam of pendingExams.value) {
    const key = exam.examName
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(exam)
  }
  const entries = [...groups.entries()]
  entries.sort((a, b) => {
    const aPending = a[1].some((e: any) => e.pendingCount > 0)
    const bPending = b[1].some((e: any) => e.pendingCount > 0)
    if (aPending && !bPending) return -1
    if (!aPending && bPending) return 1
    return 0
  })
  return entries.map(([name, items]) => {
    const allPublished = items.every((e: any) => e.scorePublished)
    return {
      name,
      exams: items,
      totalPending: items.reduce((s: number, e: any) => s + e.pendingCount, 0),
      totalCount: items.reduce((s: number, e: any) => s + e.totalCount, 0),
      allDone: items.every((e: any) => e.pendingCount === 0),
      allPublished
    }
  })
})
const totalPending = computed(() => pendingExams.value.reduce((s, e) => s + e.pendingCount, 0))
const totalGraded = computed(() => allPendingRecords.value.filter(r => r.statusName === '已批改').length)
const totalAbsent = computed(() => allPendingRecords.value.filter(r => r.statusName === '缺考').length)
const progressPercent = computed(() => {
  const total = totalPending.value + totalGraded.value
  return total > 0 ? Math.round(totalGraded.value / total * 100) : 0
})

const listFilter = ref<'all' | 'pending' | 'graded' | 'absent'>('all')
const scopedRecords = computed(() => {
  if (selectedExamIds.value.size > 0) return allPendingRecords.value.filter(r => selectedExamIds.value.has(r.examId))
  return allPendingRecords.value
})
const displayList = computed(() => {
  const list = scopedRecords.value
  if (listFilter.value === 'pending') return list.filter(r => r.statusName === '待批改')
  if (listFilter.value === 'graded') return list.filter(r => r.statusName === '已批改')
  if (listFilter.value === 'absent') return list.filter(r => r.statusName === '缺考')
  return list
})

function isExamOngoing(exam: any) {
  if (!exam.endTime) return false
  return new Date(exam.endTime).getTime() > now.value
}

function selectExam(id: number | null) {
  if (id) toggleSelect(id)
}

async function handlePublishById(examId: number) {
  const exam = exams.value.find(e => e.id === examId)
  const name = exam ? `「${exam.examName}」` : '该考试'
  const isRepublish = exam?.scorePublished
  const label = isRepublish ? '重新发布' : '发布'
  await ElMessageBox.confirm(
    `确定${label}${name}的成绩？${isRepublish ? '学生将收到成绩已更新通知。' : '发布后学生可查看成绩。'}`,
    `${label}成绩`,
    { type: 'warning', confirmButtonText: `确认${label}` }
  )

  // 防双击：已在发布中则忽略本次点击
  if (publishingExamIds.value.has(examId)) return
  publishingExamIds.value = new Set([...publishingExamIds.value, examId])
  try {
    await publishScores(examId)
    ElMessage.success(`${label}成绩成功`)
    mittBus.emit('refreshNotification')
    await loadAllData()
  } finally {
    const next = new Set(publishingExamIds.value)
    next.delete(examId)
    publishingExamIds.value = next
  }
}

// 多选相关
function isActionable(exam: any) {
  return exam.pendingCount === 0 && exam.totalCount > 0 && !isExamOngoing(exam)
}
function toggleSelect(examId: number) {
  const s = new Set(selectedExamIds.value)
  if (s.has(examId)) s.delete(examId); else s.add(examId)
  selectedExamIds.value = s
}
const actionableSelectedIds = computed(() => {
  return [...selectedExamIds.value].filter(id => {
    const e = exams.value.find(ex => ex.id === id)
    return e && isActionable(e)
  })
})
const newPublishCount = computed(() => {
  return actionableSelectedIds.value.filter(id => {
    const e = exams.value.find(ex => ex.id === id)
    return e && !e.scorePublished
  }).length
})
const rePublishCount = computed(() => {
  return actionableSelectedIds.value.filter(id => {
    const e = exams.value.find(ex => ex.id === id)
    return e && e.scorePublished
  }).length
})

const pendingSelectedCount = computed(() => {
  return [...selectedExamIds.value].filter(id => {
    const e = exams.value.find(ex => ex.id === id)
    return e && e.pendingCount > 0
  }).length
})
const pendingSelectedTotal = computed(() => {
  return [...selectedExamIds.value].reduce((sum, id) => {
    const e = exams.value.find(ex => ex.id === id)
    return sum + (e ? e.pendingCount : 0)
  }, 0)
})

function goToMarking(examId?: number) {
  if (examId) {
    const s = new Set<number>()
    s.add(examId)
    selectedExamIds.value = s
  }
  // 查找第一份待批改试卷，直接跳转到批改详情页
  const pending = allPendingRecords.value.filter(r =>
    r.statusName === '待批改' && (selectedExamIds.value.size === 0 || selectedExamIds.value.has(r.examId))
  )
  if (pending.length > 0) {
    router.push(`/exam-center/marking/${pending[0].recordId}`)
    return
  }
  // 没有待批改的，滚动到列表区域
  listFilter.value = 'pending'
  nextTick(() => {
    markingTableRef.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

async function handleBatchPublish(mode: 'new' | 're') {
  const ids = actionableSelectedIds.value.filter(id => {
    const e = exams.value.find(ex => ex.id === id)
    return mode === 'new' ? (e && !e.scorePublished) : (e && e.scorePublished)
  })
  if (ids.length === 0) return
  const label = mode === 'new' ? '发布' : '重新发布'
  await ElMessageBox.confirm(
    `确定${label}选中的 ${ids.length} 个班级的成绩？${mode === 're' ? '学生将收到成绩已更新通知。' : '发布后学生可查看成绩。'}`,
    `${label}成绩`,
    { type: 'warning', confirmButtonText: `确认${label}` }
  )
  // 防双击：已在发布中则忽略本次点击
  if (batchPublishing.value) return
  batchPublishing.value = true
  try {
    let successCount = 0
    const errors: string[] = []
    for (const id of ids) {
      try { await publishScores(id); successCount++ } catch (err: any) { errors.push(err?.message || '未知错误') }
    }
    if (successCount === ids.length) {
      ElMessage.success(`成功${label} ${successCount} 个班级的成绩`)
    } else {
      ElMessage.warning({ message: `${label}成功 ${successCount}/${ids.length}，${errors.length} 个失败：${errors[0]}`, duration: 5000 })
    }
    selectedExamIds.value = new Set()
    mittBus.emit('refreshNotification')
    await loadAllData()
  } finally {
    batchPublishing.value = false
  }
}

async function loadAllData() {
  const res = await getExamList({ page: 1, size: 200 })
  const list = res.records || []
  const allRecords: any[] = []

  await Promise.all(list.map(async (e: any) => {
    try {
      const records = await getPendingList(e.id)
      const recs = Array.isArray(records) ? records : []
      e.pendingCount = recs.filter((r: any) => r.statusName === '待批改').length
      e.totalCount = recs.length
      recs.forEach((r: any) => {
        r.examId = e.id
        r.examName = `${e.examName}（${e.className}）`
      })
      allRecords.push(...recs)
    } catch { e.pendingCount = 0; e.totalCount = 0 }
  }))

  exams.value = list
  allPendingRecords.value = allRecords
}

// WebSocket 考试事件监听：学生交卷时即时刷新待批改列表
function onExamEvent(_event: any) {
  loadAllData()
}

let isFirstActivation = true
onMounted(() => {
  loadAllData()
  startStatusTimer()
  mittBus.on('examEvent', onExamEvent)
})
onActivated(() => {
  if (isFirstActivation) { isFirstActivation = false; return }
  loadAllData()
  startStatusTimer()
})
onDeactivated(() => { stopStatusTimer() })
onBeforeUnmount(() => { stopStatusTimer(); mittBus.off('examEvent', onExamEvent) })
</script>

<style scoped>
.exam-card-scroll {
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color-light) transparent;
}
.exam-card-scroll::-webkit-scrollbar { height: 4px; }
.exam-card-scroll::-webkit-scrollbar-thumb { background-color: var(--el-border-color-light); border-radius: 2px; }
.exam-card-scroll::-webkit-scrollbar-thumb:hover { background-color: var(--el-border-color); }
.exam-card-scroll::-webkit-scrollbar-track { background-color: transparent; }
</style>
