<template>
  <div>
    <!-- 顶部标题 -->
    <div class="art-card flex-cb px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3">
        <ArtSvgIcon icon="ri:bar-chart-grouped-line" class="text-xl text-theme" />
        <span class="text-lg font-semibold text-g-900">成绩分析 — {{ data.examName }}</span>
        <el-tag size="small">{{ data.className }}</el-tag>
      </div>
      <el-button plain size="small" @click="router.back()">
        <ArtSvgIcon icon="ri:arrow-left-line" class="mr-1" />返回
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <ElRow :gutter="20">
      <ElCol v-for="(item, index) in statCards" :key="index" :sm="12" :md="6" :lg="6">
        <div class="art-card relative flex flex-col justify-center h-28 px-5 mb-5 max-sm:mb-4">
          <span class="text-sm" :class="item.labelClass">{{ item.label }}</span>
          <ArtCountTo class="text-[26px] font-medium mt-2" :target="item.value" :duration="1300" :suffix="item.suffix || ''" />
          <div class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc" :class="item.bgClass">
            <ArtSvgIcon :icon="item.icon" class="text-xl" :class="item.iconClass" />
          </div>
        </div>
      </ElCol>
    </ElRow>

    <!-- 图表 + 题目分析 -->
    <ElRow :gutter="20">
      <ElCol :sm="24" :md="12" :lg="12">
        <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
          <div class="art-card-header">
            <div class="title"><h4>分数分布</h4></div>
          </div>
          <ArtBarChart
            height="calc(100% - 56px)"
            :data="barData"
            :xAxisData="barLabels"
            :showAxisLine="false"
          />
        </div>
      </ElCol>
      <ElCol :sm="24" :md="12" :lg="12">
        <ElCard class="h-105 mb-5 max-sm:mb-4" shadow="never">
          <template #header>
            <div class="flex-cb">
              <h4 class="m-0">题目正确率分析</h4>
              <el-tag effect="light" size="small">共 {{ (data.questionAnalysis || []).length }} 题</el-tag>
            </div>
          </template>
          <el-table
            :data="data.questionAnalysis || []"
            style="width: 100%"
            size="small"
            max-height="300"
          >
            <ElTableColumn prop="content" label="题目" show-overflow-tooltip min-width="160" />
            <ElTableColumn prop="correctRate" label="正确率" width="100" align="center">
              <template #default="{ row }">
                <span class="font-bold" :class="row.correctRate >= 80 ? 'text-green-500' : row.correctRate >= 50 ? 'text-amber-500' : 'text-red-500'">{{ row.correctRate }}%</span>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="averageScore" label="平均得分" width="90" align="center" />
            <ElTableColumn prop="fullScore" label="满分" width="70" align="center" />
          </el-table>
        </ElCard>
      </ElCol>
    </ElRow>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getScoreAnalysis } from '@/api/exam/score'

defineOptions({ name: 'ScoreAnalysis' })

const route = useRoute()
const router = useRouter()
const examId = route.params.examId as string
const data = ref<any>({})

const statCards = computed(() => [
  { label: '平均分', value: data.value.averageScore || 0, icon: 'ri:bar-chart-box-line', labelClass: 'text-blue-500', bgClass: 'bg-blue-50 dark:bg-blue-900/20', iconClass: 'text-blue-500' },
  { label: '最高分', value: data.value.maxScore || 0, icon: 'ri:arrow-up-circle-line', labelClass: 'text-green-500', bgClass: 'bg-green-50 dark:bg-green-900/20', iconClass: 'text-green-500' },
  { label: '最低分', value: data.value.minScore || 0, icon: 'ri:arrow-down-circle-line', labelClass: 'text-amber-500', bgClass: 'bg-amber-50 dark:bg-amber-900/20', iconClass: 'text-amber-500' },
  { label: '及格率', value: data.value.passRate || 0, suffix: '%', icon: 'ri:percent-line', labelClass: 'text-red-500', bgClass: 'bg-red-50 dark:bg-red-900/20', iconClass: 'text-red-500' }
])

const barLabels = computed(() => {
  if (!data.value.scoreDistribution) return []
  return Object.keys(data.value.scoreDistribution)
})

const barData = computed(() => {
  if (!data.value.scoreDistribution) return []
  return Object.values(data.value.scoreDistribution) as number[]
})

onMounted(async () => {
  data.value = await getScoreAnalysis(Number(examId))
})
</script>

