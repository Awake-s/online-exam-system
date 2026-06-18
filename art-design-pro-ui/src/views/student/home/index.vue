<template>
  <div>
    <!-- 数据概览 -->
    <ElRow :gutter="20">
      <ElCol v-for="(item, index) in cards" :key="index" :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm" :class="item.labelClass">{{ item.label }}</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="item.value" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc" :class="item.bgClass">
            <ArtSvgIcon :icon="item.icon" class="text-xl" :class="item.iconClass" />
          </div>
        </div>
      </ElCol>
    </ElRow>

    <!-- 待考试 + 最近成绩 -->
    <ElRow :gutter="20">
      <ElCol :sm="24" :md="12" :lg="12">
      <div class="art-card p-5 mb-5 max-sm:mb-4">
        <div class="flex-cb mb-4">
          <h4 class="text-base font-semibold text-g-800 flex items-center gap-2">
            <span class="w-1 h-4 rounded-full bg-blue-500"></span>待考试
          </h4>
          <span class="text-xs text-g-400">{{ (data.pendingExams || []).length }} 场</span>
        </div>
        <div v-if="data.pendingExams?.length" class="space-y-2.5">
          <div v-for="exam in data.pendingExams" :key="exam.examId"
            class="flex items-center gap-3 px-4 py-3 rounded-xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 hover:shadow-md hover:border-blue-200 dark:hover:border-blue-800 transition-all cursor-pointer"
            @click="handleExamClick(exam)">
            <div class="shrink-0 w-9 h-9 rounded-lg flex items-center justify-center bg-blue-50 dark:bg-blue-900/20">
              <ArtSvgIcon icon="ri:file-edit-line" class="text-base text-blue-500" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{{ exam.examName }}</div>
              <div class="text-xs text-gray-400 mt-0.5">{{ exam.subjectName }}</div>
            </div>
            <el-button size="small" type="primary" round>{{ getExamButtonText(exam) }}</el-button>
          </div>
        </div>
        <div v-else class="flex flex-col items-center py-10 text-gray-400">
          <ArtSvgIcon icon="ri:emotion-happy-line" class="text-3xl mb-2 text-green-400" />
          <span class="text-sm">暂无待考试，好好休息吧</span>
        </div>
      </div>

      </ElCol>
      <ElCol :sm="24" :md="12" :lg="12">
      <!-- 最近成绩 -->
      <div class="art-card p-5 mb-5 max-sm:mb-4">
        <div class="flex-cb mb-4">
          <h4 class="text-base font-semibold text-g-800 flex items-center gap-2">
            <span class="w-1 h-4 rounded-full bg-green-500"></span>最近成绩
          </h4>
          <span class="text-xs text-g-400">{{ (data.recentScores || []).length }} 条</span>
        </div>
        <div v-if="data.recentScores?.length" class="space-y-2.5">
          <div v-for="score in data.recentScores" :key="score.recordId"
            class="flex items-center gap-3 px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 hover:shadow-md hover:border-blue-300 dark:hover:border-blue-700 transition-all cursor-pointer"
            @click="router.push(`/my-study/score/${score.recordId}`)">
            <div class="shrink-0 w-9 h-9 rounded-lg flex items-center justify-center"
              :class="score.isPassed ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'">
              <ArtSvgIcon :icon="score.isPassed ? 'ri:checkbox-circle-fill' : 'ri:close-circle-fill'"
                class="text-base" :class="score.isPassed ? 'text-green-500' : 'text-red-400'" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{{ score.examName }}</div>
              <div class="text-xs mt-0.5">
                <el-tag :type="score.isPassed ? 'success' : 'danger'" size="small" round>{{ score.isPassed ? '通过' : '未通过' }}</el-tag>
              </div>
            </div>
            <div class="shrink-0 text-right">
              <div class="text-lg font-bold" :class="score.isPassed ? 'text-green-500' : 'text-red-500'">{{ score.totalScore }}</div>
              <div class="text-[10px] text-gray-400">分</div>
            </div>
          </div>
        </div>
        <div v-else class="flex flex-col items-center py-10 text-gray-400">
          <ArtSvgIcon icon="ri:bar-chart-box-line" class="text-3xl mb-2" />
          <span class="text-sm">暂无成绩记录</span>
        </div>
      </div>
      </ElCol>
    </ElRow>

    <!-- 成绩趋势 -->
    <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
      <div class="art-card-header">
        <div class="title">
          <h4>成绩趋势</h4>
        </div>
        <ElSelect v-model="trendSubjectId" placeholder="全部科目" clearable size="small" style="width: 140px" @change="loadTrend">
          <ElOption v-for="s in trendSubjects" :key="s.id" :label="s.subjectName" :value="s.id" />
        </ElSelect>
      </div>
      <div v-if="trendData.length === 0" class="flex flex-col items-center justify-center h-[calc(100%-56px)] text-gray-400">
        <ArtSvgIcon icon="ri:line-chart-line" class="text-3xl mb-2" />
        <span class="text-sm">暂无成绩数据</span>
      </div>
      <div v-else ref="trendChartRef" class="w-full" style="height: calc(100% - 56px)"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentDashboard, getStudentScoreTrend } from '@/api/exam/dashboard'
import { echarts, graphic } from '@/plugins/echarts'
import { ElMessage } from 'element-plus'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'StudentHome' })

const router = useRouter()
const data = ref<any>({})
const trendSubjectId = ref<number | null>(null)
const trendSubjects = ref<any[]>([])
const trendData = ref<any[]>([])

/**
 * 智能处理考试点击事件
 * 根据考试状态和当前时间，决定跳转到哪个页面
 */
function handleExamClick(exam: any) {
  const now = new Date().getTime()
  const startTime = new Date(exam.startTime).getTime()
  const endTime = new Date(exam.endTime).getTime()

  // 1. 考试未开始 → 跳转到考试列表页
  if (now < startTime) {
    ElMessage.info('考试尚未开始，请耐心等待')
    router.push('/student/exam')
    return
  }

  // 2. 考试已结束 → 跳转到考试列表页
  if (now > endTime) {
    ElMessage.warning('考试已结束')
    router.push('/student/exam')
    return
  }

  // 3. 考试进行中 → 跳转到答题页
  router.push(`/student/exam/do/${exam.examId}`)
}

/**
 * 根据考试状态返回按钮文本
 */
function getExamButtonText(exam: any) {
  const now = new Date().getTime()
  const startTime = new Date(exam.startTime).getTime()
  const endTime = new Date(exam.endTime).getTime()

  if (now < startTime) return '等待开考'
  if (now > endTime) return '已结束'
  return '参加考试'
}

const cards = computed(() => [
  { label: '待考试', value: data.value.pendingExamCount || 0, icon: 'ri:time-line',
    labelClass: 'text-blue-500', valueClass: 'text-blue-600 dark:text-blue-400',
    bgClass: 'bg-blue-50 dark:bg-blue-900/20', iconClass: 'text-blue-500' },
  { label: '已完成', value: data.value.completedExamCount || 0, icon: 'ri:checkbox-circle-line',
    labelClass: 'text-green-500', valueClass: 'text-green-600 dark:text-green-400',
    bgClass: 'bg-green-50 dark:bg-green-900/20', iconClass: 'text-green-500' },
  { label: '平均分', value: data.value.averageScore || 0, icon: 'ri:trophy-line',
    labelClass: 'text-amber-500', valueClass: 'text-amber-600 dark:text-amber-400',
    bgClass: 'bg-amber-50 dark:bg-amber-900/20', iconClass: 'text-amber-500' },
  { label: '最高分', value: data.value.highestScore || 0, icon: 'ri:fire-line',
    labelClass: 'text-red-500', valueClass: 'text-red-600 dark:text-red-400',
    bgClass: 'bg-red-50 dark:bg-red-900/20', iconClass: 'text-red-500' }
])

const trendChartRef = ref<HTMLElement>()
let trendChart: ReturnType<typeof echarts.init> | null = null

async function loadTrend() {
  const params: any = {}
  if (trendSubjectId.value) params.subjectId = trendSubjectId.value
  trendData.value = await getStudentScoreTrend(params) || []
  await nextTick()
  renderTrendChart()
}

function renderTrendChart() {
  if (!trendChartRef.value || !trendData.value.length) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const xData = trendData.value.map((d: any) => d.examName)
  const scores = trendData.value.map((d: any) => d.totalScore ?? 0)
  const passScores = trendData.value.map((d: any) => d.passScore ?? 60)
  const avgPass = passScores.reduce((a: number, b: number) => a + b, 0) / passScores.length
  const primaryColor = '#3b82f6'
  trendChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e5e7eb', textStyle: { color: '#333' } },
    grid: { top: 20, right: 20, bottom: 40, left: 45 },
    xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11, color: '#9ca3af', rotate: xData.length > 4 ? 15 : 0 }, axisLine: { lineStyle: { color: '#e5e7eb' } }, axisTick: { show: false } },
    yAxis: { type: 'value', min: 0, max: Math.max(100, ...scores, ...trendData.value.map((d: any) => d.paperTotalScore ?? 100)), axisLabel: { fontSize: 11, color: '#9ca3af' }, splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }, axisLine: { show: false } },
    series: [{
      name: '得分', type: 'line', data: scores, smooth: true, symbol: 'circle', symbolSize: 8,
      lineStyle: { width: 3, color: primaryColor },
      itemStyle: { color: primaryColor, borderWidth: 2, borderColor: '#fff' },
      areaStyle: { color: new graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(59,130,246,0.25)' }, { offset: 1, color: 'rgba(59,130,246,0.02)' }]) },
      markLine: { silent: true, symbol: 'none', label: { formatter: '及格线 {c}', fontSize: 11, color: '#f59e0b', position: 'insideEndTop' }, lineStyle: { color: '#f59e0b', type: 'dashed', width: 1.5 }, data: [{ yAxis: avgPass }] }
    }]
  }, true)
}

async function refreshDashboard() {
  data.value = await getStudentDashboard()
  const allTrend = await getStudentScoreTrend({}) || []
  trendData.value = allTrend
  const subjectMap = new Map()
  allTrend.forEach((d: any) => {
    if (d.subjectId && !subjectMap.has(d.subjectId)) subjectMap.set(d.subjectId, d.subjectName)
  })
  trendSubjects.value = Array.from(subjectMap, ([id, subjectName]) => ({ id, subjectName }))
  await nextTick()
  renderTrendChart()
}

function onExamEvent() { refreshDashboard() }

onMounted(async () => {
  await refreshDashboard()
  window.addEventListener('resize', handleResize)
  mittBus.on('examEvent', onExamEvent)
})
onActivated(() => refreshDashboard())

function handleResize() {
  trendChart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  mittBus.off('examEvent', onExamEvent)
  trendChart?.dispose()
  trendChart = null
})
</script>

