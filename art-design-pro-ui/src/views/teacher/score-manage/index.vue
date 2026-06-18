<template>
  <div>
    <!-- 选择考试 -->
    <div class="art-card px-5 mb-5 max-sm:mb-4">
      <div class="flex-cb flex-wrap gap-3 py-4">
        <ElSelect v-model="selectedExamId" placeholder="请选择要查看成绩的考试" style="width: 420px" @change="loadScores" clearable filterable>
          <ElOption v-for="e in exams" :key="e.id" :label="`${e.examName}（${e.className}）`" :value="e.id">
            <span>{{ e.examName }}（{{ e.className }}）</span>
            <ElTag v-if="e.status === 1" type="warning" size="small" class="ml-2">进行中</ElTag>
            <ElTag v-else-if="e.scorePublished" type="success" size="small" class="ml-2">已发布</ElTag>
          </ElOption>
        </ElSelect>
        <div class="flex gap-2" v-if="selectedExamId">
          <ElButton type="primary" :disabled="examInfo.examStatus === 1" @click="router.push(`/exam-center/score/analysis/${selectedExamId}`)">成绩分析</ElButton>
          <ElButton type="success" plain :disabled="examInfo.examStatus === 1" @click="handleExport">导出Excel</ElButton>
        </div>
      </div>
      <!-- 考试状态提示（内嵌在选择器卡片底部） -->
      <div v-if="selectedExamId && examInfo.examStatus === 1" class="flex items-center gap-2 px-3 py-2.5 -mx-5 border-t border-amber-200 bg-amber-50/80 dark:bg-amber-900/10 dark:border-amber-800/30" style="border-radius: 0 0 var(--art-card-radius, 12px) var(--art-card-radius, 12px)">
        <ArtSvgIcon icon="ri:error-warning-fill" class="text-sm text-amber-500 flex-shrink-0" />
        <span class="text-xs text-amber-600 dark:text-amber-400">考试正在进行中，当前数据仅包含已交卷学生，统计指标基于不完整数据仅供参考。</span>
      </div>
      <div v-else-if="selectedExamId && examInfo.examStatus === 2 && !examInfo.scorePublished" class="flex items-center gap-2 px-3 py-2.5 -mx-5 border-t border-blue-200 bg-blue-50/80 dark:bg-blue-900/10 dark:border-blue-800/30" style="border-radius: 0 0 var(--art-card-radius, 12px) var(--art-card-radius, 12px)">
        <ArtSvgIcon icon="ri:information-fill" class="text-sm text-blue-500 flex-shrink-0" />
        <span class="text-xs text-blue-600 dark:text-blue-400">成绩尚未发布，学生暂时无法查看。请在阅卷管理中完成批改后发布成绩。</span>
      </div>
    </div>

    <ElEmpty v-if="!selectedExamId" description="请在上方选择一场考试以查看成绩" :image-size="120" />
    <ElEmpty v-else-if="scores.length === 0" description="该场考试暂无成绩数据" :image-size="120" />

    <template v-if="selectedExamId && scores.length > 0">
      <!-- 统计概览 -->
      <ElRow :gutter="20">
        <ElCol v-for="(item, index) in statCards" :key="index" :sm="12" :md="8" :lg="4">
          <div class="art-card relative flex flex-col justify-center h-28 px-5 mb-5 max-sm:mb-4">
            <span class="text-g-700 text-sm">{{ item.label }}</span>
            <span class="text-xl font-medium mt-1 text-g-900">{{ item.value }}</span>
            <div class="absolute top-0 bottom-0 right-5 m-auto size-10 rounded-xl flex-cc bg-theme/10">
              <ArtSvgIcon :icon="item.icon" class="text-base text-theme" />
            </div>
          </div>
        </ElCol>
      </ElRow>

      <!-- 分数段分布 -->
      <div class="art-card h-80 p-5 mb-5 max-sm:mb-4">
        <div class="art-card-header">
          <div class="title">
            <h4>分数段分布<span v-if="stats.absentCount > 0" class="text-xs text-g-400 font-normal ml-2">（基于实考人数，不含缺考）</span></h4>
          </div>
        </div>
        <ArtBarChart
          height="calc(100% - 56px)"
          :data="segBarData"
          :xAxisData="segBarLabels"
          :showAxisLine="false"
        />
      </div>

      <!-- 成绩表格 -->
      <ElCard class="art-table-card" shadow="never" style="margin-top: 0; margin-bottom: 20px">
        <template #header>
          <div class="flex-cb">
            <div class="flex items-center gap-3">
              <h4 class="m-0">成绩明细</h4>
              <el-tag effect="light" round>共 {{ scores.length }} 人</el-tag>
            </div>
            <ElInput v-model="searchKeyword" placeholder="搜索学生姓名" clearable size="default" style="width: 180px">
              <template #prefix><ArtSvgIcon icon="ri:search-line" class="text-sm text-gray-400" /></template>
            </ElInput>
          </div>
        </template>
        <div class="table-wrap">
        <ArtTable
          :data="filteredScores"
          style="width: 100%"
          size="large"
          :border="false"
          :stripe="false"
          :header-cell-style="headerStyle"
          :row-class-name="tableRowClass"
        >
          <template #default>
            <ElTableColumn prop="rank" label="排名" width="70" align="center">
              <template #default="{ row }">
                <span v-if="row.rank <= 3" class="text-lg">{{ row.rank === 1 ? '🥇' : row.rank === 2 ? '🥈' : '🥉' }}</span>
                <span v-else class="text-g-400 font-medium">{{ row.rank }}</span>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="realName" label="姓名" min-width="100" :show-overflow-tooltip="false" class-name="no-ellipsis">
              <template #default="{ row }">
                <span class="font-medium">{{ row.realName }}</span>
                <ElTag v-if="row.status === 4" type="danger" size="small" class="ml-1.5">缺考</ElTag>
              </template>
            </ElTableColumn>
            <ElTableColumn label="总分" width="110" align="center">
              <template #default="{ row }">
                <span class="font-bold text-sm" :class="!row.isPassed ? 'text-danger' : row.totalScore >= stats.paperTotal * 0.9 ? 'text-success' : 'text-theme'">{{ row.totalScore }}</span>
                <span v-if="row.status !== 2" class="text-xs text-g-400"> / {{ stats.paperTotal }}</span>
                <el-tooltip v-if="row.status === 2" content="主观题未批改，分数为暂估" placement="top">
                  <span class="inline-flex items-center gap-0.5 ml-1.5 text-[11px] text-warning font-medium align-middle cursor-help"><ArtSvgIcon icon="ri:error-warning-line" class="text-xs" />暂估</span>
                </el-tooltip>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="objectiveScore" label="客观题" width="80" align="center" />
            <ElTableColumn label="主观题" width="80" align="center">
              <template #default="{ row }">
                <span v-if="row.status === 2" class="text-xs text-warning">待批</span>
                <span v-else>{{ row.subjectiveScore }}</span>
              </template>
            </ElTableColumn>
            <ElTableColumn label="及格" width="70" align="center">
              <template #default="{ row }">
                <ArtSvgIcon v-if="row.isPassed" icon="ri:checkbox-circle-fill" class="text-lg text-success" />
                <ArtSvgIcon v-else icon="ri:close-circle-fill" class="text-lg text-danger" />
              </template>
            </ElTableColumn>
            <ElTableColumn label="状态" width="90" align="center" :show-overflow-tooltip="false" class-name="no-ellipsis">
              <template #default="{ row }">
                <ElTag :type="row.status === 3 ? 'success' : row.status === 4 ? 'danger' : 'warning'" size="small" round>{{ row.statusName }}</ElTag>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="submitTime" label="交卷时间" width="170" align="center" />
            <ElTableColumn label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 2" type="warning" size="small" text @click="viewDetail(row)">批改</el-button>
                <ArtButtonTable v-else-if="row.status !== 4" type="view" @click="viewDetail(row)" />
                <span v-else class="text-g-300 text-xs">—</span>
              </template>
            </ElTableColumn>
          </template>
        </ArtTable>
        </div>
      </ElCard>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getExamList } from '@/api/exam/exam'
import { getClassScores, exportScores } from '@/api/exam/score'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'ScoreManage' })
const headerStyle = { background: 'var(--el-fill-color)', fontWeight: 600, fontSize: '13px', color: 'var(--el-text-color-regular)', borderBottom: '2px solid var(--el-border-color)' }

const router = useRouter()
const exams = ref<any[]>([])
const selectedExamId = ref<number | null>(null)
const scores = ref<any[]>([])
const examInfo = ref<{ examStatus: number; examStatusName: string; scorePublished: boolean }>({ examStatus: -1, examStatusName: '', scorePublished: false })

const stats = computed(() => {
  const data = scores.value
  if (!data || data.length === 0) return {} as any
  const totalCount = data.length
  const validScores = data.filter(d => d.status !== 4)
  const absentCount = data.filter(d => d.status === 4).length
  const examCount = validScores.length
  const scoreValues = validScores.map(d => Number(d.totalScore) || 0)
  const paperTotal = Number(data[0]?.paperTotalScore) || 100
  const sum = scoreValues.reduce((a, b) => a + b, 0)
  const avgScore = examCount > 0 ? (sum / examCount).toFixed(1) : '0'
  const maxScore = scoreValues.length > 0 ? Math.max(...scoreValues) : 0
  const minScore = scoreValues.length > 0 ? Math.min(...scoreValues) : 0
  const passCount = validScores.filter(d => d.isPassed).length
  const passRate = examCount > 0 ? ((passCount / examCount) * 100).toFixed(1) : '0'
  const excellentThreshold = paperTotal * 0.9
  const excellentCount = scoreValues.filter(s => s >= excellentThreshold).length
  const excellentRate = examCount > 0 ? ((excellentCount / examCount) * 100).toFixed(1) : '0'
  const segDefs = [
    { label: '90-100', min: paperTotal * 0.9, max: paperTotal + 1 },
    { label: '80-89', min: paperTotal * 0.8, max: paperTotal * 0.9 },
    { label: '70-79', min: paperTotal * 0.7, max: paperTotal * 0.8 },
    { label: '60-69', min: paperTotal * 0.6, max: paperTotal * 0.7 },
    { label: '<60', min: -1, max: paperTotal * 0.6 }
  ]
  const segments = segDefs.map(s => ({
    ...s,
    count: scoreValues.filter(v => v >= s.min && v < s.max).length
  }))
  return { totalCount, absentCount, examCount, avgScore, maxScore, minScore, passRate, excellentRate, paperTotal, segments }
})

const statCards = computed(() => [
  { label: '实考人数', value: stats.value.absentCount > 0 ? `${stats.value.examCount} / ${stats.value.totalCount}` : stats.value.totalCount, icon: 'ri:group-line' },
  { label: '平均分', value: stats.value.avgScore, icon: 'ri:bar-chart-line' },
  { label: '最高分', value: stats.value.maxScore, icon: 'ri:arrow-up-line' },
  { label: '最低分', value: stats.value.minScore, icon: 'ri:arrow-down-line' },
  { label: '及格率', value: stats.value.passRate + '%', icon: 'ri:checkbox-circle-line' },
  { label: '优秀率', value: stats.value.excellentRate + '%', icon: 'ri:star-line' }
])

const segBarLabels = computed(() => (stats.value.segments || []).map((s: any) => s.label))
const segBarData = computed(() => (stats.value.segments || []).map((s: any) => s.count))

const searchKeyword = ref('')
const filteredScores = computed(() => {
  if (!searchKeyword.value) return scores.value
  const kw = searchKeyword.value.toLowerCase()
  return scores.value.filter((s: any) => s.realName?.toLowerCase().includes(kw))
})

function formatExamTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}

function tableRowClass({ row }: any) { return row.status === 4 ? 'absent-row' : '' }
function viewDetail(row: any) { router.push(`/exam-center/marking/${row.recordId}`) }

async function loadScores() {
  if (!selectedExamId.value) { scores.value = []; examInfo.value = { examStatus: -1, examStatusName: '', scorePublished: false }; return }
  const res = await getClassScores(selectedExamId.value)
  scores.value = res.scores || []
  examInfo.value = { examStatus: res.examStatus, examStatusName: res.examStatusName, scorePublished: res.scorePublished }
}

async function handleExport() {
  try {
    const res = await exportScores(selectedExamId.value!)
    const disposition = res.headers?.['content-disposition'] || ''
    const match = disposition.match(/filename\*?=(?:UTF-8'')?(.+)/i)
    const fileName = match ? decodeURIComponent(match[1]) : '成绩单.xlsx'
    const url = URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url; a.download = fileName; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}

function onExamEvent() { if (selectedExamId.value) loadScores() }

async function refreshExamList() {
  const res = await getExamList({ page: 1, size: 200 })
  exams.value = res.records
}

onMounted(async () => {
  await refreshExamList()
  if (exams.value?.length > 0) {
    selectedExamId.value = exams.value[0].id
    loadScores()
  }
  mittBus.on('examEvent', onExamEvent)
})
onActivated(async () => {
  await refreshExamList()
  if (selectedExamId.value) loadScores()
})
onBeforeUnmount(() => { mittBus.off('examEvent', onExamEvent) })
</script>

<style lang="scss" scoped>
:deep(.absent-row) { background-color: #fafafa !important; color: #c0c4cc; }

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


