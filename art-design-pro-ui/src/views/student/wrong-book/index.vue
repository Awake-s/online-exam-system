<template>
  <div>
    <!-- 科目筛选 -->
    <div class="art-card flex items-center gap-3 flex-wrap px-5 py-4 mb-5 max-sm:mb-4">
      <el-check-tag :checked="!selectedSubject" @change="selectSubject(null)">全部</el-check-tag>
      <el-check-tag v-for="s in subjects" :key="s.subjectId" :checked="selectedSubject === s.subjectId"
        @change="selectSubject(s.subjectId)">
        {{ s.subjectName }} ({{ s.wrongCount }})
      </el-check-tag>
    </div>
    <!-- 考试二级筛选（仅选中具体科目且该科目下有多场考试错题时显示） -->
    <div v-if="selectedSubject && examOptions.length" class="art-card flex items-center gap-3 flex-wrap px-5 py-3 mb-5 max-sm:mb-4">
      <!-- <span class="text-xs text-gray-400 mr-1 flex items-center gap-1">
        <ArtSvgIcon icon="ri:file-text-line" class="text-sm" />考试
      </span> -->
      <el-check-tag :checked="!selectedExam" @change="selectExam(null)">全部考试</el-check-tag>
      <el-check-tag v-for="e in examOptions" :key="e.examId" :checked="selectedExam === e.examId"
        @change="selectExam(e.examId)">
        {{ e.examName }} ({{ e.wrongCount }})
      </el-check-tag>
    </div>

    <!-- 错题卡片列表 -->
    <ElCard class="art-table-card" shadow="never" style="margin-top: 0">
      <template #header>
        <div class="flex-cb">
          <h4 class="m-0">错题列表</h4>
          <el-tag effect="light">共 {{ total }} 题</el-tag>
        </div>
      </template>

      <!-- 题型筛选栏（对齐 marking-detail 风格：紧凑左对齐 + 外框圆角包紧 + 选中只用文字蓝/数字胶囊/下划线三重指示） -->
      <div class="px-2 pt-2">
        <div class="inline-flex items-center gap-0 rounded-xl border border-gray-200 dark:border-gray-600 overflow-hidden">
          <button v-for="tab in typeFilterTabs" :key="tab.key"
            class="relative flex items-center gap-1.5 px-5 py-3.5 text-[13px] font-medium transition-colors"
            :class="selectedType === tab.value
              ? 'text-blue-600 dark:text-blue-400'
              : 'text-gray-400 hover:text-gray-600'"
            @click="selectType(tab.value)">
            <ArtSvgIcon :icon="tab.icon" class="text-sm" />
            {{ tab.label }}
            <span class="min-w-[20px] text-center text-[11px] px-1 rounded-md leading-[18px]"
              :class="selectedType === tab.value ? 'bg-blue-500 text-white' : 'bg-gray-100 dark:bg-gray-600 text-gray-400'">
              {{ tab.count }}
            </span>
            <div v-if="selectedType === tab.value" class="absolute bottom-0 left-2 right-2 h-[2px] bg-blue-500 rounded-t-full"></div>
          </button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="wrongList.length === 0" class="flex flex-col items-center py-16 text-gray-400">
        <ArtSvgIcon icon="ri:emotion-happy-line" class="text-4xl mb-3" />
        <span class="text-sm">暂无错题，继续保持！</span>
      </div>

      <!-- 错题卡片 -->
      <div v-else class="space-y-3 px-2 pt-3 pb-3">
        <div v-for="(item, idx) in wrongList" :key="idx"
          class="wrong-card group flex items-center gap-4 px-5 py-4 rounded-xl border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 hover:shadow-md hover:border-blue-300 dark:hover:border-blue-700 transition-all cursor-pointer"
          @click="viewDetail(item)">
          <!-- 序号 -->
          <span class="shrink-0 w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold bg-red-50 text-red-500 dark:bg-red-900/20">
            {{ (query.page - 1) * query.size + idx + 1 }}
          </span>

          <!-- 题目信息 -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1.5">
              <el-tag :type="(typeTagMap[item.questionType] as any)" size="small" round>{{ item.questionTypeName }}</el-tag>
              <span v-if="!selectedSubject" class="text-xs text-gray-400 bg-gray-50 dark:bg-gray-700 px-2 py-0.5 rounded">{{ item.subjectName }}</span>
            </div>
            <div class="text-sm text-g-700 leading-relaxed truncate">{{ item.content }}</div>
          </div>

          <!-- 答案对比 -->
          <div class="shrink-0 w-52 space-y-1 max-sm:hidden">
            <div class="flex items-center gap-2 text-xs">
              <span class="shrink-0 inline-flex items-center gap-1 text-red-400">
                <ArtSvgIcon icon="ri:close-circle-fill" class="text-sm" />我的
              </span>
              <span class="text-red-500 font-medium truncate">{{ item.myAnswer || '未作答' }}</span>
            </div>
            <div class="flex items-center gap-2 text-xs">
              <span class="shrink-0 inline-flex items-center gap-1 text-green-500">
                <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-sm" />正确
              </span>
              <span class="text-green-600 font-medium truncate">{{ item.correctAnswer }}</span>
            </div>
          </div>

          <!-- 来源 + 操作 -->
          <div class="shrink-0 flex items-center gap-3 max-sm:hidden">
            <span v-if="!selectedExam" class="text-xs text-gray-500 max-w-40 truncate flex items-center gap-1" :title="item.examName">
              <ArtSvgIcon icon="ri:file-text-line" class="text-sm flex-shrink-0 text-gray-400" />{{ item.examName }}
            </span>
            <el-tooltip content="已掌握" placement="top">
              <span class="w-7 h-7 rounded-lg flex items-center justify-center bg-green-50 dark:bg-green-900/20 text-green-500 hover:bg-green-100 dark:hover:bg-green-900/40 transition-colors"
                @click.stop="handleRemove(item)">
                <ArtSvgIcon icon="ri:checkbox-circle-line" class="text-base" />
              </span>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="wrong-pagination">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          layout="total, sizes, prev, pager, next" background @change="loadWrongList" />
      </div>
    </ElCard>

    <!-- 错题详情对话框 -->
    <el-dialog v-model="detailVisible" title="错题详情" width="640px" top="6vh">
      <div v-if="detail" class="space-y-5">
        <!-- 题目 -->
        <div>
          <div class="flex items-center gap-2 mb-2">
            <el-tag :type="(typeTagMap[detail.questionType] as any)" size="small" round>{{ detail.questionTypeName || '题目' }}</el-tag>
            <span class="text-xs text-gray-400">{{ detail.subjectName }}</span>
          </div>
          <div class="text-base font-semibold leading-relaxed text-g-800">{{ detail.content }}</div>
        </div>
        <!-- 选项 -->
        <div v-if="detail.options && detail.options.length" class="space-y-2">
          <div v-for="opt in detail.options" :key="opt"
            class="text-sm px-4 py-2.5 bg-gray-50 dark:bg-gray-700 rounded-lg text-gray-600 dark:text-gray-400">
            {{ opt }}
          </div>
        </div>
        <!-- 答案对比 -->
        <div class="grid grid-cols-2 gap-3">
          <div class="px-4 py-3 rounded-xl bg-red-50 dark:bg-red-900/15 border border-red-100 dark:border-red-800/30">
            <div class="text-xs text-red-400 mb-1 flex items-center gap-1">
              <ArtSvgIcon icon="ri:close-circle-fill" class="text-sm" />我的答案
            </div>
            <div class="text-sm text-red-600 dark:text-red-400 font-medium leading-relaxed break-all">{{ detail.myAnswer || '未作答' }}</div>
          </div>
          <div class="px-4 py-3 rounded-xl bg-green-50 dark:bg-green-900/15 border border-green-100 dark:border-green-800/30">
            <div class="text-xs text-green-500 mb-1 flex items-center gap-1">
              <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-sm" />正确答案
            </div>
            <div class="text-sm text-green-600 dark:text-green-400 font-medium leading-relaxed break-all">{{ detail.correctAnswer }}</div>
          </div>
        </div>
        <!-- 解析 -->
        <div v-if="detail.analysis" class="p-4 bg-blue-50 dark:bg-blue-900/15 border border-blue-100 dark:border-blue-800/30 rounded-xl">
          <div class="text-xs text-blue-500 mb-1.5 flex items-center gap-1">
            <ArtSvgIcon icon="ri:lightbulb-line" class="text-sm" />解析
          </div>
          <div class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{{ detail.analysis }}</div>
        </div>
        <!-- 来源 -->
        <div class="text-xs text-gray-400 flex items-center gap-1">
          <ArtSvgIcon icon="ri:time-line" class="text-sm" />
          来源：{{ detail.examName }} {{ detail.examTime ? `(${detail.examTime})` : '' }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWrongSubjects, getWrongExams, getWrongTypeCounts, getWrongList, getWrongDetail, removeWrong } from '@/api/exam/wrong'

defineOptions({ name: 'WrongBook' })

const typeTagMap: Record<number, string> = { 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'danger' }

const subjects = ref<any[]>([])
const selectedSubject = ref<number | null>(null)
const examOptions = ref<any[]>([])
const selectedExam = ref<number | null>(null)
const selectedType = ref<number | null>(null)
const typeCounts = ref<Record<string, number>>({ total: 0, '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 })
const wrongList = ref<any[]>([])
const total = ref(0)
const query = ref<any>({ page: 1, size: 10 })
const detailVisible = ref(false)
const detail = ref<any>(null)

const typeFilterTabs = computed(() => [
  { key: 'all', value: null, label: '全部', icon: 'ri:list-check', count: typeCounts.value.total || 0 },
  { key: '1', value: 1, label: '单选', icon: 'ri:radio-button-line', count: typeCounts.value['1'] || 0 },
  { key: '2', value: 2, label: '多选', icon: 'ri:checkbox-multiple-line', count: typeCounts.value['2'] || 0 },
  { key: '3', value: 3, label: '判断', icon: 'ri:check-double-line', count: typeCounts.value['3'] || 0 },
  { key: '4', value: 4, label: '填空', icon: 'ri:input-cursor-move', count: typeCounts.value['4'] || 0 },
  { key: '5', value: 5, label: '简答', icon: 'ri:chat-1-line', count: typeCounts.value['5'] || 0 },
])

async function loadSubjects() { subjects.value = await getWrongSubjects() }

async function loadTypeCounts() {
  const params: any = {}
  if (selectedSubject.value) params.subjectId = selectedSubject.value
  if (selectedExam.value) params.examId = selectedExam.value
  try {
    typeCounts.value = await getWrongTypeCounts(params)
  } catch {
    typeCounts.value = { total: 0, '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 }
  }
}

async function loadWrongList() {
  const params: any = { ...query.value }
  if (selectedSubject.value) params.subjectId = selectedSubject.value
  if (selectedExam.value) params.examId = selectedExam.value
  if (selectedType.value) params.questionType = selectedType.value
  const res = await getWrongList(params)
  wrongList.value = res.records
  total.value = res.total
}

async function selectSubject(subjectId: number | null) {
  selectedSubject.value = subjectId
  selectedExam.value = null
  selectedType.value = null
  query.value.page = 1
  if (subjectId) {
    try { examOptions.value = await getWrongExams(subjectId) } catch { examOptions.value = [] }
  } else {
    examOptions.value = []
  }
  await loadTypeCounts()
  loadWrongList()
}

async function selectExam(examId: number | null) {
  selectedExam.value = examId
  selectedType.value = null
  query.value.page = 1
  await loadTypeCounts()
  loadWrongList()
}

function selectType(type: number | null) {
  selectedType.value = type
  query.value.page = 1
  loadWrongList()
}

async function viewDetail(row: any) {
  detail.value = await getWrongDetail(row.answerId)
  detailVisible.value = true
}

async function handleRemove(row: any) {
  await ElMessageBox.confirm('确定已掌握该题？移除后不再显示。', '提示')
  await removeWrong(row.answerId)
  ElMessage.success('已移除')
  loadTypeCounts()
  loadWrongList()
  loadSubjects()
}

onMounted(() => { loadSubjects(); loadTypeCounts(); loadWrongList() })
onActivated(() => { loadSubjects(); loadTypeCounts(); loadWrongList() })
</script>

<style lang="scss" scoped>
.wrong-card .truncate {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 分页器样式
:deep(.wrong-pagination) {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px 4px;

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
</style>

