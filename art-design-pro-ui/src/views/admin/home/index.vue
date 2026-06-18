<template>
  <div>
    <!-- 统计卡片 -->
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

    <!-- 图表区域 -->
    <ElRow :gutter="20">
      <ElCol :sm="24" :md="12" :lg="10">
        <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
          <div class="art-card-header">
            <div class="title">
              <h4>用户角色分布</h4>
            </div>
          </div>
          <ArtRingChart
            height="calc(100% - 56px)"
            :data="ringChartData"
            :colors="ringColors"
            :showLegend="true"
            legendPosition="bottom"
            :showLabel="true"
            :radius="['35%', '60%']"
            :minAngle="25"
          />
        </div>
      </ElCol>
      <ElCol :sm="24" :md="12" :lg="14">
        <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
          <div class="art-card-header">
            <div class="title">
              <h4>系统数据概览</h4>
            </div>
          </div>
          <ArtBarChart
            height="calc(100% - 56px)"
            :data="barChartValues"
            :xAxisData="barChartLabels"
            :showAxisLine="false"
          />
        </div>
      </ElCol>
    </ElRow>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, computed } from 'vue'
import { getAdminDashboard } from '@/api/exam/dashboard'

defineOptions({ name: 'AdminHome' })

const data = ref<any>({})

const cards = computed(() => [
  { label: '用户总数', value: data.value.totalUsers || 0, icon: 'ri:group-line',
    labelClass: 'text-blue-500', valueClass: 'text-blue-600 dark:text-blue-400',
    bgClass: 'bg-blue-50 dark:bg-blue-900/20', iconClass: 'text-blue-500' },
  { label: '教师数量', value: data.value.teacherCount || 0, icon: 'ri:user-star-line',
    labelClass: 'text-purple-500', valueClass: 'text-purple-600 dark:text-purple-400',
    bgClass: 'bg-purple-50 dark:bg-purple-900/20', iconClass: 'text-purple-500' },
  { label: '学生数量', value: data.value.studentCount || 0, icon: 'ri:graduation-cap-line',
    labelClass: 'text-green-500', valueClass: 'text-green-600 dark:text-green-400',
    bgClass: 'bg-green-50 dark:bg-green-900/20', iconClass: 'text-green-500' },
  { label: '班级数量', value: data.value.classCount || 0, icon: 'ri:building-line',
    labelClass: 'text-amber-500', valueClass: 'text-amber-600 dark:text-amber-400',
    bgClass: 'bg-amber-50 dark:bg-amber-900/20', iconClass: 'text-amber-500' }
])

const ringColors = ['#6366f1', '#3b82f6', '#22d3ee']

const ringChartData = computed(() => [
  { name: '管理员', value: data.value.adminCount || 1 },
  { name: '教师', value: data.value.teacherCount || 0 },
  { name: '学生', value: data.value.studentCount || 0 }
])

const barChartLabels = ['用户', '教师', '学生', '班级', '科目', '考试']
const barChartValues = computed(() => [
  data.value.totalUsers || 0,
  data.value.teacherCount || 0,
  data.value.studentCount || 0,
  data.value.classCount || 0,
  data.value.subjectCount || 0,
  data.value.examCount || 0
])

async function loadDashboard() {
  try {
    data.value = await getAdminDashboard()
  } catch (e) {
    console.error('加载仪表盘数据失败:', e)
  }
}

onMounted(loadDashboard)
onActivated(loadDashboard)
</script>
