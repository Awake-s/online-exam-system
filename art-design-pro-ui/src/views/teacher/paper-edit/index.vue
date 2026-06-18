<template>
  <div>
    <!-- 顶部操作栏 -->
    <div class="art-card flex-cb px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-2">
        <ArtSvgIcon :icon="isView ? 'ri:file-text-line' : 'ri:draft-line'" class="text-xl text-blue-500" />
        <span class="text-base font-bold text-gray-800 dark:text-gray-200">
          {{ isView ? '试卷详情' : (isEdit ? '编辑试卷' : '手动组卷') }}
        </span>
      </div>
      <div class="flex gap-2">
        <el-button v-if="!isView" type="primary" @click="handleSave">
          <ArtSvgIcon icon="ri:save-line" class="mr-1" />保存试卷
        </el-button>
        <el-button plain @click="router.back()">
          <ArtSvgIcon icon="ri:arrow-left-line" class="mr-1" />返回
        </el-button>
      </div>
    </div>

    <!-- 试卷基本信息 -->
    <ElCard shadow="never" class="mb-5 max-sm:mb-4">
      <template #header>
        <div class="flex items-center gap-2">
          <ArtSvgIcon icon="ri:file-info-line" class="text-lg text-blue-500" />
          <h4 class="m-0 text-sm font-semibold">基本信息</h4>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="paperName">
              <el-input v-model="form.paperName" :disabled="isView" placeholder="请输入试卷名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="form.subjectId" :disabled="isView || isEdit" @change="loadQuestions" placeholder="请选择" style="width: 100%">
                <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="及格分" prop="passScore">
              <el-input-number v-model="form.passScore" :min="0" :disabled="isView" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分钟)" prop="duration">
              <el-input-number v-model="form.duration" :min="1" :disabled="isView" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </ElCard>

    <!-- 题目统计摘要 -->
    <div v-if="form.questions.length" class="flex items-center gap-3 flex-wrap px-4 py-3 rounded-xl mb-5 bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800">
      <div class="flex items-center gap-1.5">
        <ArtSvgIcon icon="ri:bar-chart-box-line" class="text-base text-blue-500" />
        <span class="font-bold text-sm text-blue-600 dark:text-blue-400">试卷概况</span>
      </div>
      <el-tag round>共 {{ form.questions.length }} 题</el-tag>
      <el-tag type="success" round>总分 {{ totalScore }} 分</el-tag>
      <el-tag v-for="(count, type) in questionStats" :key="type" :type="(typeTagMap[Number(type)] as any) || 'info'" size="small" round>
        {{ typeMap[Number(type)] || '' }} × {{ count }}
      </el-tag>
    </div>

    <!-- 题库选择区 -->
    <ElCard v-if="!isView" shadow="never" class="mb-5 max-sm:mb-4 pool-card">
      <template #header>
        <div class="flex-cb">
          <div class="flex items-center gap-2">
            <ArtSvgIcon icon="ri:database-2-line" class="text-lg text-orange-500" />
            <h4 class="m-0 text-sm font-semibold">题库</h4>
            <el-tag v-if="filteredPool.length" size="small" round>{{ filteredPool.length }} 题可选</el-tag>
          </div>
          <div class="flex items-center gap-2">
            <el-select v-model="poolTypeFilter" placeholder="题型" clearable size="small" style="width: 100px">
              <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="Number(k)" />
            </el-select>
            <el-select v-model="poolDiffFilter" placeholder="难度" clearable size="small" style="width: 90px">
              <el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" />
            </el-select>
          </div>
        </div>
      </template>
      <div class="pool-list" style="max-height: 320px; overflow-y: auto">
        <div v-if="!filteredPool.length" class="py-8 text-center text-sm text-gray-400">
          <ArtSvgIcon icon="ri:inbox-line" class="text-3xl mb-2 block mx-auto" />
          请先选择科目
        </div>
        <div v-for="row in filteredPool" :key="row.id"
          class="pool-item flex items-center gap-3 mx-2 mb-2 px-4 py-3 rounded-lg border transition-all"
          :class="isAdded(row.id)
            ? 'border-gray-300 dark:border-gray-700 bg-gray-100 dark:bg-gray-800/50 opacity-45'
            : 'border-[#c0c5cf] dark:border-gray-600 bg-white dark:bg-gray-800 hover:shadow-md hover:border-blue-400 dark:hover:border-blue-600 cursor-pointer'"
          @click="!isAdded(row.id) && addToSelected(row)">
          <!-- 题目内容 -->
          <div class="flex-1 min-w-0">
            <div class="text-sm font-semibold text-black dark:text-gray-100 truncate">{{ row.content }}</div>
          </div>
          <!-- 标签组 -->
          <div class="flex items-center gap-2 shrink-0">
            <el-tag :type="(typeTagMap[row.questionType] as any)" size="small" round effect="dark">{{ row.questionTypeName }}</el-tag>
            <el-tag :type="(diffTagType[row.difficulty] as any)" size="small" round effect="dark">{{ row.difficultyName }}</el-tag>
            <span class="text-xs font-bold text-gray-700 dark:text-gray-300 w-7 text-right">{{ row.score }}分</span>
          </div>
          <!-- 操作 -->
          <div class="shrink-0 w-8 text-center">
            <ArtSvgIcon v-if="!isAdded(row.id)" icon="ri:add-circle-fill" class="text-xl text-blue-500 hover:text-blue-600 transition-colors" />
            <ArtSvgIcon v-else icon="ri:checkbox-circle-fill" class="text-xl text-green-500" />
          </div>
        </div>
      </div>
    </ElCard>

    <!-- 已选题目 - 标签页切换 -->
    <ElCard shadow="never" class="mb-5 max-sm:mb-4 selected-questions-card">
      <template #header>
        <div class="flex-cb">
          <div class="flex items-center gap-2">
            <ArtSvgIcon icon="ri:list-ordered-2" class="text-lg text-green-500" />
            <h4 class="m-0 text-sm font-semibold">已选题目</h4>
            <el-tag type="success" size="small" round>总分 {{ totalScore }} 分</el-tag>
          </div>
        </div>
      </template>
      <template v-if="groupedQuestions.length">
        <!-- Segmented 标签栏（滑动指示器） -->
        <div ref="segRef" class="type-segmented">
          <div class="seg-slider" :style="sliderStyle"></div>
          <button v-for="group in groupedQuestions" :key="group.type"
            :ref="(el: any) => { if (el) segBtnRefs[String(group.type)] = el }"
            class="seg-item" :class="{ 'seg-active': activeTypeTab === String(group.type) }"
            @click="activeTypeTab = String(group.type)">
            <ArtSvgIcon :icon="typeIconMap[group.type] || 'ri:question-line'" class="text-sm" />
            <span>{{ typeMap[group.type] || '未知' }}</span>
            <span class="seg-count" :class="activeTypeTab === String(group.type) ? 'seg-count-active' : ''">{{ getTypeCount(group.type) }}</span>
          </button>
        </div>
        <!-- 当前题型统计栏 -->
        <div v-if="activeGroup" class="flex justify-between items-center px-1 py-2.5 mb-4 border-b border-gray-100 dark:border-gray-700">
          <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
            <span class="w-1.5 h-4 rounded-full" :class="typeCircleClass(activeGroup.type)"></span>
            <span>{{ typeMap[activeGroup.type] }} · 共 <b class="text-gray-700 dark:text-gray-200">{{ activeGroup.items.length }}</b> 题</span>
          </div>
          <span class="text-sm text-gray-500 dark:text-gray-400">小计：<b class="text-gray-700 dark:text-gray-200">{{ activeGroup.items.reduce((s: number, q: any) => s + (q.score || 0), 0) }}</b> 分</span>
        </div>
        <!-- 题目卡片列表 -->
        <div v-if="activeGroup" class="space-y-3">
          <div v-for="(q, qi) in activeGroup.items" :key="q.questionId"
            class="rounded-xl border overflow-hidden transition-all hover:shadow-sm bg-white dark:bg-gray-800"
            :class="expandedSet[q.questionId] ? 'border-blue-200 dark:border-blue-700' : 'border-gray-200 dark:border-gray-600'">
            <!-- 题头行 -->
            <div class="flex items-center gap-3 px-4 py-3 cursor-pointer select-none" @click="expandedSet[q.questionId] = !expandedSet[q.questionId]">
              <span class="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold text-white shrink-0"
                :class="typeCircleClass(activeGroup.type)">
                {{ qi + 1 }}
              </span>
              <div class="flex-1 min-w-0">
                <div class="text-sm text-gray-800 dark:text-gray-200 leading-relaxed" :class="expandedSet[q.questionId] ? '' : 'truncate'">{{ q.content }}</div>
              </div>
              <div class="flex items-center gap-2 shrink-0" @click.stop>
                <el-tag v-if="q.difficultyName" :type="(diffTagType[q.difficulty] as any)" size="small" round class="shrink-0">{{ q.difficultyName }}</el-tag>
                <el-input-number v-model="q.score" :min="0.5" :step="0.5" size="small" :disabled="isView"
                  style="width: 90px" controls-position="right"
                  :class="{ 'high-score': q.score >= 10 }" />
                <span class="text-xs text-gray-400">分</span>
              </div>
              <div class="flex items-center gap-1 shrink-0">
                <div v-if="!isView" class="flex items-center gap-1" @click.stop>
                  <el-tooltip content="上移" placement="top" :show-after="300">
                    <el-button size="small" circle :disabled="getGlobalIndex(q) === 0" @click="moveQuestion(q, -1)">
                      <ArtSvgIcon icon="ri:arrow-up-s-line" />
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="下移" placement="top" :show-after="300">
                    <el-button size="small" circle :disabled="getGlobalIndex(q) === form.questions.length - 1" @click="moveQuestion(q, 1)">
                      <ArtSvgIcon icon="ri:arrow-down-s-line" />
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="编辑题目" placement="top" :show-after="300">
                    <el-button size="small" circle type="primary" @click="router.push({ path: '/teacher/question', query: { editId: q.questionId } })">
                      <ArtSvgIcon icon="ri:edit-line" />
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="移除" placement="top" :show-after="300">
                    <el-button size="small" circle type="danger" @click="removeQuestion(q)">
                      <ArtSvgIcon icon="ri:delete-bin-line" />
                    </el-button>
                  </el-tooltip>
                </div>
                <!-- 展开/收起 -->
                <div class="w-6 h-6 rounded-md flex items-center justify-center transition-colors"
                  :class="expandedSet[q.questionId] ? 'bg-blue-50 dark:bg-blue-900/30' : 'bg-gray-100 dark:bg-gray-600'">
                  <ArtSvgIcon :icon="expandedSet[q.questionId] ? 'ri:arrow-up-s-line' : 'ri:arrow-down-s-line'"
                    class="text-sm" :class="expandedSet[q.questionId] ? 'text-blue-500' : 'text-gray-400'" />
                </div>
              </div>
            </div>
            <!-- 展开内容 -->
            <div v-show="expandedSet[q.questionId]" class="mx-4 mb-4 rounded-lg border border-gray-100 dark:border-gray-600 bg-gray-50/50 dark:bg-gray-800/50 overflow-hidden">
              <!-- 选项（选择题） -->
              <div v-if="q.options && q.options.length && (q.questionType === 1 || q.questionType === 2)" class="p-4 space-y-2">
                <div v-for="opt in q.options" :key="opt"
                  class="flex items-center text-[13px] px-3.5 py-2.5 rounded-lg border transition-colors"
                  :class="isOptionCorrect(opt, q.answer)
                    ? 'bg-green-50 dark:bg-green-900/15 border-green-200 dark:border-green-800 text-green-700 dark:text-green-400'
                    : 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400'">
                  <ArtSvgIcon v-if="isOptionCorrect(opt, q.answer)" icon="ri:checkbox-circle-fill" class="text-sm text-green-500 mr-2 shrink-0" />
                  <span class="leading-relaxed">{{ opt }}</span>
                </div>
                <div class="flex items-center gap-4 text-xs pt-2.5 mt-1 border-t border-gray-200 dark:border-gray-600">
                  <span class="text-gray-400">正确答案：<b class="text-green-600 dark:text-green-400">{{ q.answer }}</b></span>
                </div>
              </div>
              <!-- 非选择题答案 -->
              <div v-else class="p-4">
                <div class="px-4 py-3 rounded-lg bg-green-50 dark:bg-green-900/10 border border-green-200 dark:border-green-800/30">
                  <div class="text-[11px] text-green-600 dark:text-green-400 mb-1.5 flex items-center gap-1 font-semibold">
                    <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-xs" />正确答案
                  </div>
                  <div class="text-[13px] text-green-800 dark:text-green-300 leading-6 whitespace-pre-wrap">{{ q.questionType === 3 ? (q.answer == '1' || q.answer === '正确' ? '正确 ✓' : '错误 ✗') : q.answer }}</div>
                </div>
              </div>
              <!-- 题目解析 -->
              <div v-if="q.analysis" class="px-4 pb-4">
                <div class="px-4 py-3 rounded-lg bg-purple-50/80 dark:bg-purple-900/10 border border-purple-100 dark:border-purple-800/30">
                  <div class="text-[11px] text-purple-600 dark:text-purple-400 mb-1.5 flex items-center gap-1 font-semibold">
                    <ArtSvgIcon icon="ri:lightbulb-line" class="text-xs" />题目解析
                  </div>
                  <div class="text-[13px] text-purple-800 dark:text-purple-300 leading-6 whitespace-pre-wrap">{{ q.analysis }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无题目，请从上方题库中添加" :image-size="60" />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { getPaperDetail, createPaper, updatePaper } from '@/api/exam/paper'
import { getQuestionList } from '@/api/exam/question'
import { getAllSubjects } from '@/api/exam/subject'
import { questionTypeMap as typeMap, difficultyTagType as diffTagType } from '@/utils/exam-format'
import { buildSubjectLabelMap } from '@/utils/subject-label'

defineOptions({ name: 'PaperEdit' })
const typeTagMap: Record<number, string> = { 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'danger' }
const typeIconMap: Record<number, string> = {
  1: 'ri:radio-button-line', 2: 'ri:checkbox-line', 3: 'ri:question-answer-line',
  4: 'ri:text-wrap', 5: 'ri:chat-quote-line'
}
function groupHeaderClass(type: number) {
  const map: Record<number, string> = {
    1: 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400',
    2: 'bg-amber-50 dark:bg-amber-900/20 text-amber-600 dark:text-amber-400',
    3: 'bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400',
    4: 'bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400',
    5: 'bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400'
  }
  return map[type] || 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
}
function typeCircleClass(type: number) {
  const map: Record<number, string> = {
    1: 'bg-blue-500', 2: 'bg-amber-500', 3: 'bg-green-500', 4: 'bg-purple-500', 5: 'bg-rose-500'
  }
  return map[type] || 'bg-gray-400'
}

const route = useRoute()
const router = useRouter()
const paperId = route.params.id as string | undefined
const isEdit = route.query.mode === 'edit'
const isView = !!paperId && !isEdit
const subjects = ref<any[]>([])
// 「按需维度消歧」科目下拉显示：仅在同名科目存在时才加专业/年级后缀。
// 例：zhangwenge 教多专业「思想道德与法治」时下拉显示「思想道德与法治（计算机科学与技术）」；
//     luweizhong 只教唯一「面向对象技术」时显示纯课程名，避免冗余。
// 算法详见 @/utils/subject-label.ts。
const subjectLabelMap = computed(() => buildSubjectLabelMap(subjects.value))
const questionPool = ref<any[]>([])
const formRef = ref<FormInstance>()
const form = ref<any>({ paperName: '', subjectId: null, passScore: 60, duration: 120, questions: [] })
const poolTypeFilter = ref<number | null>(null)
const poolDiffFilter = ref<number | null>(null)
const activeTypeTab = ref<string>('1')
const expandedSet = reactive<Record<number, boolean>>({})
const segRef = ref<HTMLElement>()
const segBtnRefs = reactive<Record<string, HTMLElement>>({})
const sliderStyle = ref<Record<string, string>>({ opacity: '0' })

function updateSlider() {
  const btn = segBtnRefs[activeTypeTab.value]
  const container = segRef.value
  if (!btn || !container) { sliderStyle.value = { opacity: '0' }; return }
  sliderStyle.value = {
    width: btn.offsetWidth + 'px',
    height: btn.offsetHeight + 'px',
    transform: `translate(${btn.offsetLeft}px, ${btn.offsetTop}px)`,
    opacity: '1'
  }
}
watch(activeTypeTab, () => nextTick(updateSlider))

const rules = {
  paperName: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
  passScore: [{ required: true, message: '请输入及格分', trigger: 'blur' }],
  duration: [{ required: true, message: '请输入时长', trigger: 'blur' }]
}

const totalScore = computed(() => form.value.questions.reduce((sum: number, q: any) => sum + (q.score || 0), 0))

const questionStats = computed(() => {
  const stats: Record<number, number> = {}
  form.value.questions.forEach((q: any) => { stats[q.questionType] = (stats[q.questionType] || 0) + 1 })
  return stats
})

const filteredPool = computed(() => {
  return questionPool.value.filter((q: any) => {
    if (poolTypeFilter.value && q.questionType !== poolTypeFilter.value) return false
    if (poolDiffFilter.value && q.difficulty !== poolDiffFilter.value) return false
    return true
  })
})

const groupedQuestions = computed(() => {
  const groups: Record<number, any[]> = {}
  const typeOrder = [1, 2, 3, 4, 5]
  form.value.questions.forEach((q: any) => {
    const t = q.questionType || 0
    if (!groups[t]) groups[t] = []
    groups[t].push(q)
  })
  return typeOrder.filter(t => groups[t]).map(t => ({ type: t, items: groups[t] }))
})

const activeGroup = computed(() => groupedQuestions.value.find(g => String(g.type) === activeTypeTab.value))
function getTypeCount(type: number) { return form.value.questions.filter((q: any) => q.questionType === type).length }

watch(groupedQuestions, (groups) => {
  if (groups.length && !groups.find(g => String(g.type) === activeTypeTab.value)) {
    activeTypeTab.value = String(groups[0].type)
  }
  nextTick(updateSlider)
}, { immediate: true })

function isAdded(id: number) { return form.value.questions.some((q: any) => q.questionId === id) }
function isOptionCorrect(opt: string, answer: string) {
  if (!opt || !answer) return false
  const letter = opt.charAt(0).toUpperCase()
  return answer.split(',').map((a: string) => a.trim().toUpperCase()).includes(letter)
}
function poolRowClass({ row }: any) { return isAdded(row.id) ? 'pool-row-added' : '' }

async function loadQuestions() {
  if (!form.value.subjectId) return
  const res = await getQuestionList({ page: 1, size: 200, subjectId: form.value.subjectId })
  questionPool.value = res.records
}

function addToSelected(row: any) {
  if (isAdded(row.id)) { ElMessage.warning('该题已添加'); return }
  form.value.questions.push({
    questionId: row.id, content: row.content, questionType: row.questionType,
    questionTypeName: row.questionTypeName, difficulty: row.difficulty,
    difficultyName: row.difficultyName, score: row.score, sortOrder: form.value.questions.length + 1,
    options: row.options || null, answer: row.answer || '', analysis: row.analysis || ''
  })
}

function removeQuestion(row: any) {
  const idx = form.value.questions.findIndex((q: any) => q.questionId === row.questionId)
  if (idx >= 0) form.value.questions.splice(idx, 1)
}

function getGlobalIndex(row: any) {
  return form.value.questions.findIndex((q: any) => q.questionId === row.questionId)
}

function moveQuestion(row: any, dir: number) {
  const idx = getGlobalIndex(row)
  if (idx < 0) return
  const newIdx = idx + dir
  if (newIdx < 0 || newIdx >= form.value.questions.length) return
  const temp = form.value.questions[idx]
  form.value.questions[idx] = form.value.questions[newIdx]
  form.value.questions[newIdx] = temp
  form.value.questions = [...form.value.questions]
}

async function handleSave() {
  await formRef.value?.validate()
  if (form.value.questions.length === 0) { ElMessage.warning('请至少添加一道题目'); return }
  if (totalScore.value > 0 && form.value.passScore > totalScore.value) {
    ElMessage.warning(`及格分(${form.value.passScore})不能超过试卷总分(${totalScore.value})`)
    return
  }
  const payload = {
    paperName: form.value.paperName, subjectId: form.value.subjectId,
    passScore: form.value.passScore, duration: form.value.duration,
    questions: form.value.questions.map((q: any, i: number) => ({ questionId: q.questionId, score: q.score, sortOrder: i + 1 }))
  }
  if (isEdit && paperId) {
    await updatePaper(Number(paperId), payload)
    ElMessage.success(`试卷更新成功，共 ${form.value.questions.length} 题，总分 ${totalScore.value} 分`)
  } else {
    await createPaper(payload)
    ElMessage.success(`试卷创建成功，共 ${form.value.questions.length} 题，总分 ${totalScore.value} 分`)
  }
  router.push('/teacher/paper')
}

onMounted(async () => {
  subjects.value = await getAllSubjects()
  if (paperId) {
    const d: any = await getPaperDetail(Number(paperId))
    form.value = {
      paperName: d.paperName, subjectId: d.subjectId, passScore: d.passScore, duration: d.duration,
      questions: d.questions.map((q: any) => ({
        questionId: q.id, content: q.content, questionType: q.questionType,
        questionTypeName: q.questionTypeName, difficulty: q.difficulty,
        difficultyName: q.difficultyName, score: q.score, sortOrder: q.sortOrder,
        options: q.options || null, answer: q.answer || '', analysis: q.analysis || ''
      }))
    }
    if (isEdit) loadQuestions()
  }
  nextTick(() => setTimeout(updateSlider, 100))
})
</script>

<style lang="scss" scoped>
:deep(.pool-row-added) {
  opacity: 0.45;
  background: #f5f5f5 !important;
}
.high-score :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-warning) inset;
}
.type-segmented {
  display: inline-flex;
  position: relative;
  gap: 4px;
  padding: 4px;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  margin-bottom: 16px;
  :root.dark & {
    background: transparent;
    border-color: #4b5563;
  }
}
.seg-slider {
  position: absolute;
  top: 0;
  left: 0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06), 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), width 0.3s cubic-bezier(0.4, 0, 0.2, 1), height 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s;
  z-index: 0;
  :root.dark & { background: #3a3a40; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2); }
}
.seg-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #8b8fa3;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  position: relative;
  z-index: 1;
  &:hover:not(.seg-active) {
    color: #606266;
    :root.dark & { color: #c0c4cc; }
  }
  &.seg-active {
    color: var(--theme-color, #409eff);
    font-weight: 600;
    :root.dark & { color: #79bbff; }
  }
  &:active:not(:disabled) { transform: scale(0.97); }
}
.seg-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 600;
  background: #e0e3eb;
  color: #8c8fa3;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  :root.dark & { background: #3a3a3e; color: #a0a0a5; }
}
.seg-count-active {
  background: var(--theme-color, #409eff);
  color: #fff;
}
.pool-item {
  &:hover:not(.pool-item-added) {
    background: #f5f7ff;
    :root.dark & { background: rgba(255, 255, 255, 0.03); }
  }
}
.pool-item-added {
  opacity: 0.45;
  background: #fafafa;
  :root.dark & { background: rgba(255, 255, 255, 0.02); }
}
.pool-list::-webkit-scrollbar { width: 6px; }
.pool-list::-webkit-scrollbar-thumb { background-color: var(--el-border-color-light); border-radius: 3px; &:hover { background-color: var(--el-border-color); } }
.pool-list::-webkit-scrollbar-track { background-color: transparent; }
</style>

