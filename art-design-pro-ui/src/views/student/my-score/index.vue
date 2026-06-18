<template>
  <div>
    <!-- 统计概览 -->
    <ElRow :gutter="20">
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-blue-500">考试总数</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="stats.totalExams" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-blue-50 dark:bg-blue-900/20"><ArtSvgIcon icon="ri:file-list-3-line" class="text-xl text-blue-500" /></div>
        </div>
      </ElCol>
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-green-500">及格次数</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="stats.passedCount" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-green-50 dark:bg-green-900/20"><ArtSvgIcon icon="ri:checkbox-circle-line" class="text-xl text-green-500" /></div>
        </div>
      </ElCol>
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-amber-500">平均分</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="stats.averageScore" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-amber-50 dark:bg-amber-900/20"><ArtSvgIcon icon="ri:trophy-line" class="text-xl text-amber-500" /></div>
        </div>
      </ElCol>
      <ElCol :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm text-red-500">最高分</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="stats.highestScore" :duration="1300" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-red-50 dark:bg-red-900/20"><ArtSvgIcon icon="ri:fire-line" class="text-xl text-red-500" /></div>
        </div>
      </ElCol>
    </ElRow>

    <!-- 成绩列表 -->
    <ElCard class="art-table-card" shadow="never" style="margin-top: 0">
      <template #header>
        <div class="flex-cb">
          <h4 class="m-0">成绩列表</h4>
          <el-tag effect="light">共 {{ total }} 条记录</el-tag>
        </div>
      </template>
      <div class="score-table-wrap">
        <el-table :data="scores" style="width: 100%" :header-cell-style="headerCellStyle" :row-class-name="rowClassName">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="examName" label="考试名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="subjectName" label="科目" min-width="140" show-overflow-tooltip />
          <el-table-column prop="totalScore" label="得分" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.status === 2" class="text-gray-400">待批改</span>
              <span v-else class="font-bold" :class="row.isPassed ? 'text-green-500' : 'text-red-500'">{{ row.totalScore }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="paperTotalScore" label="满分" width="80" align="center" />
          <el-table-column prop="rank" label="排名" width="90" align="center">
            <template #default="{ row }">
              <span v-if="row.rank">{{ row.rank }}<span class="text-gray-400 text-xs ml-0.5">/{{ row.totalParticipants || '-' }}</span></span>
              <span v-else class="text-gray-400">-</span>
            </template>
          </el-table-column>
          <el-table-column label="及格" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 2" type="warning" size="small" round>待批改</el-tag>
              <el-tag v-else :type="row.isPassed ? 'success' : 'danger'" size="small" round>{{ row.isPassed ? '及格' : '不及格' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 2" type="warning" size="small" effect="plain" round>待批改</el-tag>
              <el-tag v-else-if="row.status === 3" type="success" size="small" effect="plain" round>已批改</el-tag>
              <el-tag v-else-if="row.status === 4" type="danger" size="small" effect="plain" round>缺考</el-tag>
              <el-tag v-else type="info" size="small" effect="plain" round>{{ row.statusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <ArtButtonTable type="view" @click="router.push(`/my-study/score/${row.recordId}`)" />
            </template>
          </el-table-column>
        </el-table>
        <div class="score-pagination">
          <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" background @change="loadData" />
        </div>
      </div>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, onDeactivated, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getMyScores } from '@/api/exam/score'
import { getStudentDashboard } from '@/api/exam/dashboard'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'MyScore' })
const router = useRouter()
const query = ref({ page: 1, size: 10 })
const scores = ref<any[]>([])
const total = ref(0)
const stats = reactive({ totalExams: 0, passedCount: 0, averageScore: 0, highestScore: 0 })

const headerCellStyle = {
  background: 'var(--el-fill-color)',
  fontWeight: 600,
  fontSize: '13px',
  color: 'var(--el-text-color-regular)',
  borderBottom: '2px solid var(--el-border-color)',
  padding: '14px 0'
}

function rowClassName({ rowIndex }: { rowIndex: number }) {
  return rowIndex % 2 === 0 ? '' : 'stripe-row'
}

async function loadData() {
  const res = await getMyScores(query.value)
  scores.value = res.records
  total.value = res.total
}

async function refreshAll() {
  await loadData()
  const dashboard = await getStudentDashboard()
  stats.totalExams = dashboard.completedExamCount || 0
  stats.passedCount = dashboard.passedCount || 0
  stats.averageScore = dashboard.averageScore || 0
  stats.highestScore = dashboard.highestScore || 0
}

let pollTimer: ReturnType<typeof setInterval> | null = null
function startPolling() { stopPolling(); pollTimer = setInterval(refreshAll, 15000) }
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

// WebSocket 成绩发布事件监听：教师发布成绩时即时刷新
function onExamEvent(_event: any) {
  refreshAll()
}

onMounted(() => {
  refreshAll()
  startPolling()
  mittBus.on('examEvent', onExamEvent)
})
onActivated(() => {
  refreshAll()
  startPolling()
})
onDeactivated(() => stopPolling())
onBeforeUnmount(() => { stopPolling(); mittBus.off('examEvent', onExamEvent) })
</script>

<style lang="scss" scoped>
.score-table-wrap {
  :deep(.el-table) {
    --el-table-border-color: var(--el-border-color-lighter);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    overflow: hidden;

    // 去掉默认表格伪元素边框
    &::before,
    &::after {
      display: none;
    }

    .el-table__header-wrapper th {
      font-size: 13px;
      letter-spacing: 0.5px;
    }

    .el-table__row {
      transition: background-color 0.2s;

      &:hover > td {
        background-color: var(--el-fill-color-light) !important;
      }
    }

    .stripe-row > td {
      background-color: var(--el-fill-color-lighter) !important;
    }
  }

  // 分页器样式：参考源码 ArtTable 的 custom-pagination
  :deep(.score-pagination) {
    display: flex;
    justify-content: flex-end;
    padding: 16px 4px 4px;

    .el-pagination {
      .btn-prev,
      .btn-next {
        background-color: transparent;
        border: 1px solid var(--el-border-color-light);
        border-radius: 6px;
        transition: border-color 0.15s;

        &:hover:not(.is-disabled) {
          color: var(--theme-color);
          border-color: var(--theme-color);
        }
      }

      li {
        box-sizing: border-box;
        font-weight: 400 !important;
        background-color: transparent;
        border: 1px solid var(--el-border-color-light);
        border-radius: 6px;
        transition: border-color 0.15s;

        &.is-active {
          font-weight: 500 !important;
          color: #fff;
          background-color: var(--theme-color);
          border-color: var(--theme-color);
        }

        &:hover:not(.is-disabled):not(.is-active) {
          border-color: var(--theme-color);
        }
      }
    }
  }
}
</style>

