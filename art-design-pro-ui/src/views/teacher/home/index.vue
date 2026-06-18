<template>
  <div>
    <!-- 统计卡片 - 标准art-design-pro卡片样式 -->
    <ElRow :gutter="20">
      <ElCol v-for="(item, index) in cards" :key="index" :sm="12" :md="6" :lg="6">
        <div
          class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4 cursor-pointer"
          @click="item.link && router.push(item.link)"
        >
          <span class="text-g-700 text-sm">{{ item.label }}</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="item.value" :duration="1300" />
          <div class="flex-c mt-1">
            <span class="text-xs text-g-600">{{ item.sub }}</span>
          </div>
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc" :class="item.bgClass">
            <ArtSvgIcon :icon="item.icon" class="text-xl" :class="item.iconClass" />
          </div>
        </div>
      </ElCol>
    </ElRow>

    <!-- 图表区域 - 标准art-design-pro图表卡片布局 -->
    <ElRow :gutter="20">
      <ElCol :sm="24" :md="12" :lg="10">
        <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
          <div class="art-card-header">
            <div class="title">
              <h4>批改概览</h4>
              <p v-if="(data.pendingMarkCount || 0) > 0">待批改 <span class="text-danger">{{ data.pendingMarkCount }}</span></p>
              <p v-else>全部完成</p>
            </div>
          </div>
          <ArtRingChart
            v-if="ringData.some(d => d.value > 0)"
            height="calc(100% - 56px)"
            :data="ringData"
            :colors="['#f59e0b', '#10b981']"
            :showLegend="true"
            legendPosition="bottom"
            :showLabel="true"
            :radius="['35%', '60%']"
            :minAngle="25"
          />
          <div v-else class="flex-cc h-[calc(100%-56px)] text-sm text-g-500">暂无批改数据</div>
        </div>
      </ElCol>
      <ElCol :sm="24" :md="12" :lg="14">
        <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
          <div class="art-card-header">
            <div class="title">
              <h4>考试平均分</h4>
              <p>近期考试成绩</p>
            </div>
          </div>
          <ArtBarChart
            v-if="barLabels.length > 0"
            height="calc(100% - 56px)"
            :data="barValues"
            :xAxisData="barLabels"
            :showAxisLine="false"
          />
          <div v-else class="flex-cc h-[calc(100%-56px)] text-sm text-g-500">暂无成绩数据</div>
        </div>
      </ElCol>
    </ElRow>

    <!-- 最近考试 + 快捷操作 -->
    <ElRow :gutter="20">
      <ElCol :sm="24" :md="24" :lg="18">
        <ElCard class="art-table-card" shadow="never" style="margin-top: 0; margin-bottom: 20px">
          <template #header>
            <div class="flex-cb">
              <h4 class="m-0">最近考试</h4>
              <el-button size="small" text type="primary" @click="router.push('/exam-center/exam')">
                查看全部 <ArtSvgIcon icon="ri:arrow-right-s-line" class="ml-0.5" />
              </el-button>
            </div>
          </template>
          <div class="table-wrap">
            <el-table :data="data.recentExams || []" style="width: 100%" :header-cell-style="headerStyle"
              row-class-name="cursor-pointer" @row-click="(row: any) => router.push(`/exam-center/score?examId=${row.examId}`)">
              <ElTableColumn prop="examName" label="考试名称" min-width="180" show-overflow-tooltip />
              <ElTableColumn prop="className" label="班级" width="120" />
              <ElTableColumn label="时间" width="120" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="text-[13px] text-g-500">{{ formatDate(row.startTime) }}</span>
                </template>
              </ElTableColumn>
              <ElTableColumn label="批改进度" width="140" align="center">
                <template #default="{ row }">
                  <div v-if="row.submittedCount > 0" class="flex items-center gap-2">
                    <el-progress
                      :percentage="row.submittedCount ? Math.round((row.gradedCount || 0) / row.submittedCount * 100) : 0"
                      :stroke-width="6" :show-text="false"
                      :color="row.pendingCount > 0 ? '#f59e0b' : '#10b981'"
                      class="flex-1"
                    />
                    <span class="text-xs text-g-500 shrink-0">{{ row.gradedCount || 0 }}/{{ row.submittedCount }}</span>
                  </div>
                  <span v-else class="text-xs text-g-400">—</span>
                </template>
              </ElTableColumn>
              <ElTableColumn label="平均分" width="80" align="center">
                <template #default="{ row }">
                  <span v-if="row.avgScore != null" class="text-sm font-medium text-theme">{{ row.avgScore }}</span>
                  <span v-else class="text-xs text-g-400">—</span>
                </template>
              </ElTableColumn>
              <ElTableColumn prop="statusName" label="状态" width="90" align="center" :show-overflow-tooltip="false" class-name="no-ellipsis">
                <template #default="{ row }">
                  <ElTag :type="row.status === 1 ? 'success' : row.status === 0 ? 'info' : 'danger'" size="small" round>
                    {{ row.statusName }}
                  </ElTag>
                </template>
              </ElTableColumn>
            </el-table>
          </div>
        </ElCard>
      </ElCol>
      <ElCol :sm="24" :md="24" :lg="6">
        <div class="art-card p-5 mb-5 max-sm:mb-4">
          <div class="art-card-header">
            <div class="title">
              <h4>快捷操作</h4>
            </div>
          </div>
          <div class="mt-2">
            <div v-for="(action, index) in quickActions" :key="action.label"
              class="flex-cb h-17.5 text-sm cursor-pointer"
              :class="{ 'border-b border-g-300': index < quickActions.length - 1 }"
              @click="router.push(action.link)">
              <div class="flex-c gap-3">
                <div class="size-9 rounded-lg flex-cc" :class="action.bgClass">
                  <ArtSvgIcon :icon="action.icon" class="text-base" :class="action.iconClass" />
                </div>
                <span>{{ action.label }}</span>
              </div>
              <ArtSvgIcon icon="ri:arrow-right-s-line" class="text-g-400" />
            </div>
          </div>
        </div>
      </ElCol>
    </ElRow>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherDashboard } from '@/api/exam/dashboard'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'TeacherHome' })

const headerStyle = { background: 'var(--el-fill-color)', fontWeight: 600, fontSize: '13px', color: 'var(--el-text-color-regular)', borderBottom: '2px solid var(--el-border-color)' }
const router = useRouter()
const data = ref<any>({})

const cards = computed(() => [
  { label: '我的题目', value: data.value.myQuestionCount || 0, icon: 'ri:file-text-line', link: '/teacher/question',
    sub: '题库总量',
    bgClass: 'bg-blue-50 dark:bg-blue-900/20', iconClass: 'text-blue-500' },
  { label: '我的试卷', value: data.value.myPaperCount || 0, icon: 'ri:book-open-line', link: '/teacher/paper',
    sub: '已创建试卷',
    bgClass: 'bg-purple-50 dark:bg-purple-900/20', iconClass: 'text-purple-500' },
  { label: '进行中考试', value: data.value.ongoingExamCount || 0, icon: 'ri:time-line', link: '/exam-center/exam',
    sub: '正在进行',
    bgClass: 'bg-amber-50 dark:bg-amber-900/20', iconClass: 'text-amber-500' },
  { label: '待批改', value: data.value.pendingMarkCount || 0, icon: 'ri:edit-line', link: '/exam-center/marking',
    sub: '需要阅卷',
    bgClass: 'bg-red-50 dark:bg-red-900/20', iconClass: 'text-red-500' }
])

const ringData = computed(() => {
  const exams = data.value.recentExams || []
  let totalPending = 0
  let totalGraded = 0
  exams.forEach((e: any) => {
    totalPending += e.pendingCount || 0
    totalGraded += e.gradedCount || 0
  })
  return [
    { name: '待批改', value: totalPending },
    { name: '已批改', value: totalGraded }
  ]
})

const barLabels = computed(() => {
  const exams = (data.value.recentExams || []).filter((e: any) => e.avgScore != null)
  return exams.map((e: any) => {
    const name = e.examName || ''
    return name.length > 6 ? name.substring(0, 6) + '…' : name
  })
})

const barValues = computed(() => {
  return (data.value.recentExams || [])
    .filter((e: any) => e.avgScore != null)
    .map((e: any) => Number(e.avgScore))
})

const quickActions = [
  { label: '创建题目', icon: 'ri:add-circle-line', link: '/teacher/question',
    bgClass: 'bg-blue-50 dark:bg-blue-900/20', iconClass: 'text-blue-500' },
  { label: '创建试卷', icon: 'ri:draft-line', link: '/teacher/paper',
    bgClass: 'bg-purple-50 dark:bg-purple-900/20', iconClass: 'text-purple-500' },
  { label: '发布考试', icon: 'ri:calendar-check-line', link: '/exam-center/exam',
    bgClass: 'bg-green-50 dark:bg-green-900/20', iconClass: 'text-green-500' },
  { label: '阅卷管理', icon: 'ri:edit-2-line', link: '/exam-center/marking',
    bgClass: 'bg-amber-50 dark:bg-amber-900/20', iconClass: 'text-amber-500' }
]

const formatDate = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadDashboard() {
  try {
    data.value = await getTeacherDashboard()
  } catch (e) {
    console.error('加载教师仪表盘失败:', e)
  }
}

function onExamEvent() { loadDashboard() }

onMounted(async () => {
  await loadDashboard()
  mittBus.on('examEvent', onExamEvent)
})
onActivated(() => loadDashboard())
onBeforeUnmount(() => {
  mittBus.off('examEvent', onExamEvent)
})
</script>

<style lang="scss" scoped>
.table-wrap {
  :deep(.el-table) {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    overflow: hidden;
    &::before, &::after { display: none; }
    .el-table__row {
      transition: background-color 0.2s;
      &:hover > td { background-color: var(--el-fill-color-light) !important; }
    }
  }
}
</style>
