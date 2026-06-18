<template>
  <!-- 考前须知遮罩 -->
  <div v-if="showExamNotice" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
    <div class="notice-modal notice-enter">
      <!-- 标题 -->
      <div class="n-head">
        <div class="n-head-icon">
          <ArtSvgIcon icon="ri:file-shield-2-line" class="text-[22px]" />
        </div>
        <div>
          <h2 class="n-title">考试须知</h2>
          <p class="n-subtitle">请仔细阅读以下内容，确认后方可开始作答</p>
        </div>
      </div>

      <!-- 监控提示 -->
      <div v-if="antiCheat.switchScreenMax > 0 || antiCheat.fullscreenRequired" class="n-monitor">
        <div class="n-monitor-head">
          <ArtSvgIcon icon="ri:shield-check-fill" class="text-sm" />
          防作弊监控已开启
        </div>
        <div class="n-monitor-cards">
          <div v-if="antiCheat.switchScreenMax > 0" class="n-mcard">
            <div class="n-mcard-val">{{ antiCheat.switchScreenMax }}<small>次</small></div>
            <div class="n-mcard-label">切屏上限，超出强制交卷</div>
          </div>
          <div v-if="antiCheat.fullscreenRequired" class="n-mcard">
            <div class="n-mcard-val text-indigo-500">
              <ArtSvgIcon icon="ri:fullscreen-fill" class="inline-block -mt-1 mr-0.5" />全屏
            </div>
            <div class="n-mcard-label">推荐全屏模式，提升专注度</div>
          </div>
        </div>
      </div>

      <!-- 规则 -->
      <div class="n-rules">
        <div class="n-rules-label">
          <span class="n-rules-line"></span>作答规则<span class="n-rules-line"></span>
        </div>
        <div class="n-rule">
          <ArtSvgIcon icon="ri:timer-line" class="text-indigo-500 text-base flex-shrink-0" />
          <span>考试开始后系统自动计时，到时自动交卷</span>
        </div>
        <div v-if="antiCheat.noCopyPaste" class="n-rule">
          <ArtSvgIcon icon="ri:file-forbid-line" class="text-red-500 text-base flex-shrink-0" />
          <span>禁止复制、粘贴、右键及开发者工具</span>
        </div>
        <div v-if="antiCheat.inactivityTimeout > 0" class="n-rule">
          <ArtSvgIcon icon="ri:zzz-line" class="text-amber-500 text-base flex-shrink-0" />
          <span>连续 <b>{{ antiCheat.inactivityTimeout }}</b> 分钟无操作自动交卷</span>
        </div>
        <div class="n-rule">
          <ArtSvgIcon icon="ri:cloud-line" class="text-emerald-500 text-base flex-shrink-0" />
          <span>答案每 30 秒自动保存，也可手动保存</span>
        </div>
      </div>

      <!-- 底部 -->
      <div class="n-foot">
        <label class="n-agree" :class="{ 'is-active': agreeExamRules }">
          <el-checkbox v-model="agreeExamRules">
            我已阅读并同意遵守以上规则
          </el-checkbox>
        </label>
        <button class="n-btn" :disabled="!agreeExamRules" @click="confirmStartExam">开始考试</button>
      </div>
    </div>
  </div>

  <!-- 自定义全屏提示覆盖层（覆盖浏览器原生通知） -->
  <Teleport to="body">
    <Transition name="fullscreen-tip">
      <div v-if="showFullscreenTip" class="fullscreen-tip-overlay">
        <div class="fullscreen-tip-bar">
          <ArtSvgIcon icon="ri:fullscreen-fill" class="text-sm text-white/90" />
          <span>已进入全屏模式，按 <kbd>Esc</kbd> 可退出</span>
        </div>
      </div>
    </Transition>
  </Teleport>

  <div class="flex flex-col overflow-hidden" :class="{ 'exam-no-copy': antiCheat.noCopyPaste }" style="height: 100vh">
    <!-- 顶部信息栏 -->
    <div class="exam-header flex-shrink-0">
      <div class="flex items-center gap-3 min-w-0 flex-1">
        <div class="flex items-center gap-2.5 min-w-0">
          <div class="w-7 h-7 rounded-lg bg-blue-500/10 flex items-center justify-center flex-shrink-0">
            <ArtSvgIcon icon="ri:file-edit-line" class="text-sm text-blue-500" />
          </div>
          <h1 class="text-[13px] font-semibold text-gray-800 dark:text-gray-100 truncate">{{ examData.examName }}</h1>
        </div>
        <div v-if="questions.length" class="hidden sm:flex items-center gap-2 pl-3 border-l border-gray-200/60 dark:border-gray-700/60 flex-shrink-0">
          <span class="text-xs text-gray-400 font-medium">{{ questions.length }}题 · {{ paperTotalScore }}分</span>
          <div class="flex items-center gap-1">
            <span v-for="(pill, idx) in typePills" :key="idx"
              class="px-1.5 py-0.5 rounded text-[10px] font-medium text-gray-500 dark:text-gray-400 bg-gray-100/80 dark:bg-gray-700/50">
              {{ pill.label }}
            </span>
          </div>
        </div>
      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        <div class="exam-timer" :class="remainSeconds <= 60 ? 'timer-danger' : remainSeconds <= 300 ? 'timer-warning' : 'timer-normal'">
          <ArtSvgIcon icon="ri:timer-line" class="text-[13px]" />
          <span class="font-mono tracking-wider">{{ fmtTime(remainSeconds) }}</span>
        </div>
        <Transition name="fade">
          <button v-if="antiCheat.fullscreenRequired && !isInFullscreen && clientEndTime > 0 && !isSubmitting"
            class="h-7 flex items-center gap-1 px-2.5 rounded-lg text-xs text-gray-500 dark:text-gray-400 hover:text-blue-500 hover:bg-blue-50/80 dark:hover:bg-blue-900/20 transition-all cursor-pointer border border-gray-200/60 dark:border-gray-700/60"
            @click="requestFullscreen" title="进入全屏模式">
            <ArtSvgIcon icon="ri:fullscreen-line" class="text-xs" />
            <span>全屏</span>
          </button>
        </Transition>
      </div>
    </div>
    <!-- 主内容区 -->
    <div class="flex gap-5 px-5 py-4 overflow-hidden flex-1 min-h-0">
    <!-- 左侧面板 -->
    <div class="w-56 flex-shrink-0 flex flex-col gap-3">
      <!-- 答题卡 -->
      <div class="art-card p-3">
        <div class="flex items-center justify-between mb-2.5">
          <span class="text-xs font-semibold text-gray-700 dark:text-gray-300">答题卡</span>
          <span class="text-[11px] font-mono text-blue-500 font-semibold">{{ answeredCount }}/{{ questions.length }}</span>
        </div>
        <!-- 进度条 -->
        <div class="w-full h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden mb-3">
          <div class="h-full bg-gradient-to-r from-blue-400 to-blue-500 rounded-full transition-all duration-500" :style="{ width: (questions.length ? (answeredCount / questions.length * 100) : 0) + '%' }"></div>
        </div>
        <!-- 图例 -->
        <div class="flex items-center gap-3 mb-2 text-[10px] text-gray-400">
          <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded bg-blue-500"></span>已答</span>
          <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded bg-gray-200 dark:bg-gray-600"></span>未答</span>
        </div>
        <!-- 题号网格 -->
        <div class="grid grid-cols-7 gap-1">
          <div v-for="(q, i) in questions" :key="q.id"
            class="h-7 rounded flex items-center justify-center text-[11px] font-medium cursor-pointer transition-all"
            :class="answers[q.id] ? 'bg-blue-500 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-500 hover:bg-gray-200 dark:hover:bg-gray-600'"
            @click="scrollTo(i)">{{ i + 1 }}</div>
        </div>
      </div>
      <!-- 操作区 -->
      <div class="art-card p-3 space-y-2">
        <!-- 保存状态 -->
        <div class="flex items-center justify-center gap-1.5 text-[11px] py-0.5">
          <template v-if="saveStatus === 'saving'">
            <ArtSvgIcon icon="ri:loader-4-line" class="text-blue-500 animate-spin text-xs" />
            <span class="text-blue-500">保存中...</span>
          </template>
          <template v-else-if="saveStatus === 'saved'">
            <ArtSvgIcon icon="ri:checkbox-circle-line" class="text-green-500 text-xs" />
            <span class="text-green-500">答案已保存</span>
          </template>
          <template v-else>
            <ArtSvgIcon icon="ri:error-warning-line" class="text-amber-500 text-xs" />
            <span class="text-amber-500">有未保存的修改</span>
          </template>
        </div>
        <el-button type="primary" class="!w-full !h-8" style="border-radius: 8px" plain size="small" @click="handleManualSave">
          <ArtSvgIcon icon="ri:save-line" class="mr-1 text-xs" /> 保存答案
        </el-button>
        <el-button type="danger" class="!w-full !ml-0 !h-9 !font-semibold !text-[13px]" style="border-radius: 8px; box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3)" @click="handleSubmit">
          <ArtSvgIcon icon="ri:send-plane-fill" class="mr-1" /> 交卷
        </el-button>
      </div>
    </div>
    <!-- 题目区域 -->
    <div class="flex-1 space-y-4 overflow-y-auto pl-2 hide-scrollbar">
      <template v-for="(q, i) in questions" :key="q.id">
        <!-- 题型分组标题 -->
        <div v-if="sectionHeaders.get(i)" class="flex items-center gap-2.5" :class="i > 0 ? 'pt-2' : ''">
          <div class="w-1 h-5 rounded-full" :class="typeAccent[q.questionType]"></div>
          <span class="text-sm font-bold text-gray-800 dark:text-gray-200">{{ sectionHeaders.get(i)!.label }}</span>
          <span class="text-xs text-gray-400 font-normal">{{ sectionHeaders.get(i)!.count }}题 · {{ sectionHeaders.get(i)!.totalScore }}分</span>
          <div class="flex-1 h-px bg-gradient-to-r from-gray-200 dark:from-gray-600 to-transparent"></div>
        </div>
        <!-- 题目卡片 -->
        <div :id="'q-' + i"
          class="art-card p-5 transition-all duration-200"
          :class="answers[q.id] ? 'question-answered' : ''">
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-2">
              <span class="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold"
                :class="answers[q.id] ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-500'">{{ i + 1 }}</span>
              <el-tag :type="(typeTagMap[q.questionType] as any)" size="small" round>{{ typeNames[q.questionType] }}</el-tag>
            </div>
            <span class="text-xs text-gray-400 bg-gray-50 dark:bg-gray-700 px-2.5 py-1 rounded-md">{{ q.score }} 分</span>
          </div>
        <div class="text-sm leading-relaxed text-gray-800 dark:text-gray-200 mb-4">{{ q.content }}</div>

        <!-- 单选题 -->
        <div v-if="q.questionType === 1" class="space-y-2">
          <div v-for="opt in q.options" :key="opt"
            class="flex items-center gap-3 px-4 py-3 rounded-lg cursor-pointer border transition-all text-sm"
            :class="answers[q.id] === opt.charAt(0) ? 'border-blue-400 bg-blue-50 dark:bg-blue-900/20 text-blue-600' : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 text-gray-600 dark:text-gray-400'"
            @click="answers[q.id] = answers[q.id] === opt.charAt(0) ? '' : opt.charAt(0); markUnsavedAndDebounce()">
            <span class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
              :class="answers[q.id] === opt.charAt(0) ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-500'">{{ opt.charAt(0) }}</span>
            <span>{{ opt.substring(2) }}</span>
          </div>
        </div>

        <!-- 多选题 -->
        <div v-else-if="q.questionType === 2" class="space-y-2">
          <div v-for="opt in q.options" :key="opt"
            class="flex items-center gap-3 px-4 py-3 rounded-lg cursor-pointer border transition-all text-sm"
            :class="(multiAnswers[q.id] || []).includes(opt.charAt(0)) ? 'border-blue-400 bg-blue-50 dark:bg-blue-900/20 text-blue-600' : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 text-gray-600 dark:text-gray-400'"
            @click="toggleMulti(q.id, opt.charAt(0))">
            <span class="w-6 h-6 rounded flex items-center justify-center text-xs font-bold flex-shrink-0"
              :class="(multiAnswers[q.id] || []).includes(opt.charAt(0)) ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-500'">{{ opt.charAt(0) }}</span>
            <span>{{ opt.substring(2) }}</span>
          </div>
        </div>

        <!-- 判断题 -->
        <div v-else-if="q.questionType === 3" class="flex gap-4">
          <div class="flex-1 flex items-center justify-center gap-2 py-3 rounded-lg cursor-pointer border transition-all text-sm"
            :class="answers[q.id] === '1' ? 'border-green-400 bg-green-50 text-green-600 font-semibold' : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 text-gray-500'"
            @click="answers[q.id] = answers[q.id] === '1' ? '' : '1'; markUnsavedAndDebounce()">
            <ArtSvgIcon icon="ri:check-line" /> 正确
          </div>
          <div class="flex-1 flex items-center justify-center gap-2 py-3 rounded-lg cursor-pointer border transition-all text-sm"
            :class="answers[q.id] === '0' ? 'border-red-400 bg-red-50 text-red-600 font-semibold' : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 text-gray-500'"
            @click="answers[q.id] = answers[q.id] === '0' ? '' : '0'; markUnsavedAndDebounce()">
            <ArtSvgIcon icon="ri:close-line" /> 错误
          </div>
        </div>

        <!-- 填空题 -->
        <el-input v-else-if="q.questionType === 4" v-model="answers[q.id]" placeholder="请输入答案，多个空请用英文逗号分隔" clearable @input="markUnsavedAndDebounce()" />

        <!-- 简答题 -->
        <el-input v-else-if="q.questionType === 5" v-model="answers[q.id]" type="textarea" :rows="6" placeholder="请输入简答题答案..." @input="markUnsavedAndDebounce()" />
      </div>
      </template>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { startExam, submitExam, autoSaveAnswers, recordSwitchScreen } from '@/api/exam/studentExam'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'ExamPage' })

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.examId)
const examData = ref<any>({})
const questions = ref<any[]>([])
const answers = reactive<Record<number, string>>({})
const multiAnswers = reactive<Record<number, string[]>>({})
const recordId = ref<number | null>(null)
const remainSeconds = ref(0)
const totalSeconds = ref(0)
let timer: any = null
let autoSaveTimer: any = null
const isSubmitting = ref(false)
const clientEndTime = ref(0)
const saveStatus = ref<'saved' | 'saving' | 'unsaved'>('saved')
let debounceSaveTimer: any = null

// 防作弊相关
const antiCheat = reactive<any>({ switchScreenMax: 0, shuffleQuestion: false, shuffleOption: false, fullscreenRequired: false, noCopyPaste: false, inactivityTimeout: 0 })
const switchCount = ref(0)
const isInFullscreen = ref(false)
const showFullscreenTip = ref(false)
let fullscreenTipTimer: any = null
const showExamNotice = ref(false)
const agreeExamRules = ref(false)
let inactivityTimer: any = null
let lastInteractionTime = 0
let lastSwitchTime = 0
let switchAlertShowing = false
const typeNames: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
const typeTagMap: Record<number, string> = { 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'danger' }

const typeAccent: Record<number, string> = { 1: 'bg-blue-500', 2: 'bg-amber-500', 3: 'bg-green-500', 4: 'bg-cyan-500', 5: 'bg-red-400' }
const sectionHeaders = computed(() => {
  const headers = new Map<number, { label: string; count: number; totalScore: number }>()
  const ordinals = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  let lastType = -1
  const sections: { startIndex: number; type: number }[] = []
  for (let i = 0; i < questions.value.length; i++) {
    if (questions.value[i].questionType !== lastType) {
      sections.push({ startIndex: i, type: questions.value[i].questionType })
      lastType = questions.value[i].questionType
    }
  }
  for (let s = 0; s < sections.length; s++) {
    const start = sections[s].startIndex
    const end = s + 1 < sections.length ? sections[s + 1].startIndex : questions.value.length
    let count = 0, totalScore = 0
    for (let i = start; i < end; i++) { count++; totalScore += questions.value[i].score || 0 }
    headers.set(start, {
      label: `${ordinals[s] || s + 1}、${typeNames[sections[s].type]}`,
      count, totalScore
    })
  }
  return headers
})

const paperTotalScore = computed(() => questions.value.reduce((sum, q) => sum + (q.score || 0), 0))
const pillColors: Record<number, string> = {
  1: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  2: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  3: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
  4: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-300',
  5: 'bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300'
}
const typePills = computed(() => {
  const counts: Record<number, number> = {}
  questions.value.forEach(q => { counts[q.questionType] = (counts[q.questionType] || 0) + 1 })
  return Object.entries(counts).map(([type, count]) => ({
    label: `${typeNames[Number(type)].replace('题', '')} ${count}`,
    colorClass: pillColors[Number(type)] || 'bg-gray-100 text-gray-600'
  }))
})

const answeredCount = computed(() => questions.value.filter(q => !!answers[q.id]).length)
const timePercent = computed(() => totalSeconds.value <= 0 ? 100 : Math.max(0, Math.round((remainSeconds.value / totalSeconds.value) * 100)))
const progressColor = computed(() => remainSeconds.value <= 60 ? '#F56C6C' : remainSeconds.value <= 300 ? '#E6A23C' : '#409EFF')

function fmtTime(s: number) {
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${h > 0 ? h + ':' : ''}${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function markUnsavedAndDebounce() {
  saveStatus.value = 'unsaved'
  if (debounceSaveTimer) clearTimeout(debounceSaveTimer)
  debounceSaveTimer = setTimeout(() => doSilentSave(), 5000)
}

function toggleMulti(qId: number, letter: string) {
  const arr = multiAnswers[qId] || []
  const idx = arr.indexOf(letter)
  if (idx >= 0) arr.splice(idx, 1)
  else arr.push(letter)
  multiAnswers[qId] = [...arr]
  answers[qId] = arr.sort().join(',')
  markUnsavedAndDebounce()
}

function scrollTo(i: number) {
  const el = document.getElementById('q-' + i)
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function doSilentSave() {
  if (!recordId.value || remainSeconds.value <= 0) return
  const answerList = questions.value.filter(q => !!answers[q.id]).map(q => ({ questionId: q.id, answer: answers[q.id] }))
  if (answerList.length === 0) return
  saveStatus.value = 'saving'
  try {
    await autoSaveAnswers({ recordId: recordId.value, answers: answerList })
    saveStatus.value = 'saved'
  } catch {
    saveStatus.value = 'unsaved'
  }
}

async function handleManualSave() {
  if (!recordId.value || remainSeconds.value <= 0) return
  if (debounceSaveTimer) { clearTimeout(debounceSaveTimer); debounceSaveTimer = null }
  const answerList = questions.value.filter(q => !!answers[q.id]).map(q => ({ questionId: q.id, answer: answers[q.id] }))
  if (answerList.length === 0) { ElMessage.info('暂无答案需要保存'); return }
  saveStatus.value = 'saving'
  try {
    await autoSaveAnswers({ recordId: recordId.value, answers: answerList })
    saveStatus.value = 'saved'
    ElMessage.success('答案保存成功')
  } catch {
    saveStatus.value = 'unsaved'
    ElMessage.error('保存失败，请检查网络')
  }
}

async function handleSubmit() {
  if (isSubmitting.value) return
  try {
    await ElMessageBox.confirm('确定要交卷吗？交卷后将无法修改答案。', '交卷确认', {
      confirmButtonText: '确定交卷', cancelButtonText: '继续检查', type: 'warning', center: true
    })
  } catch {
    return
  }
  isSubmitting.value = true
  try {
    const answerList = questions.value.map(q => ({ questionId: q.id, answer: answers[q.id] || '' }))
    await submitExam({ recordId: recordId.value, answers: answerList })
    ElMessage.success('提交成功')
    mittBus.emit('refreshNotification')
    router.replace('/student/exam')
  } catch (e: any) {
    const msg = e?.response?.data?.msg || e?.message || ''
    if (msg.includes('已提交') || msg.includes('已完成')) {
      ElMessage.info('试卷已提交')
      router.replace('/student/exam')
    } else {
      ElMessage.error('交卷失败，请检查网络后重试')
      isSubmitting.value = false
    }
  }
}

let lastWarningMin = -1

function recalcRemain() {
  const now = Date.now()
  remainSeconds.value = Math.max(0, Math.floor((clientEndTime.value - now) / 1000))
}

function startTimerFn() {
  timer = setInterval(() => {
    recalcRemain()
    const mins = Math.floor(remainSeconds.value / 60)
    if (remainSeconds.value <= 300 && remainSeconds.value > 0 && mins !== lastWarningMin && remainSeconds.value % 60 === 0) {
      lastWarningMin = mins
      ElMessage.warning(`考试剩余时间不足 ${mins} 分钟`)
    }
    if (remainSeconds.value <= 0) {
      clearInterval(timer); clearInterval(autoSaveTimer)
      timer = null; autoSaveTimer = null
      doAutoSubmit()
    }
  }, 1000)
}

function doRecordSwitch() {
  const now = Date.now()
  if (now - lastSwitchTime < 1000) return
  lastSwitchTime = now
  switchCount.value++
  if (recordId.value) {
    recordSwitchScreen(recordId.value).then((res: any) => {
      switchCount.value = res.switchCount
      if (res.forceSubmit) {
        ElMessage.error('切屏次数已达上限，系统将自动交卷')
        doAutoSubmit()
      }
    }).catch(() => {
      // 网络失败时回退本地计数，避免与服务端不同步导致警告偏差
      switchCount.value = Math.max(0, switchCount.value - 1)
    })
  }
}

function onVisibilityChange() {
  if (document.visibilityState === 'hidden' && clientEndTime.value > 0 && !isSubmitting.value && antiCheat.switchScreenMax > 0 && !showExamNotice.value) {
    doRecordSwitch()
  }
  if (document.visibilityState === 'visible' && clientEndTime.value > 0) {
    recalcRemain()
    showSwitchWarning()
    if (remainSeconds.value <= 0 && !isSubmitting.value) {
      clearInterval(timer); clearInterval(autoSaveTimer)
      timer = null; autoSaveTimer = null
      doAutoSubmit()
    } else if (saveStatus.value === 'unsaved') {
      doSilentSave()
    }
    resetInactivityTimer()
  }
}

function showSwitchWarning() {
  if (antiCheat.switchScreenMax <= 0 || isSubmitting.value || switchCount.value <= 0 || switchAlertShowing) return
  const remaining = antiCheat.switchScreenMax - switchCount.value
  if (remaining <= 0) {
    ElMessage.error('切屏次数已达上限，系统将自动交卷')
    doAutoSubmit()
    return
  }
  switchAlertShowing = true
  ElMessageBox.alert(
    `您已切屏 ${switchCount.value} 次，还可切屏 ${remaining} 次，超过将强制交卷！`,
    '切屏警告', { type: 'warning', confirmButtonText: '我知道了' }
  ).catch(() => {}).finally(() => { switchAlertShowing = false })
}

function onWindowBlur() {
  if (clientEndTime.value > 0 && !isSubmitting.value && antiCheat.switchScreenMax > 0 && !showExamNotice.value) {
    doRecordSwitch()
  }
}

function onBeforeUnload(e: BeforeUnloadEvent) {
  if (!isSubmitting.value && remainSeconds.value > 0) {
    e.preventDefault()
    e.returnValue = ''
  }
}

// 防复制粘贴+防DevTools+防打印+防拖拽
function preventCopyPaste(e: Event) { e.preventDefault() }
function preventContextMenu(e: Event) { e.preventDefault() }
function preventDrag(e: Event) { e.preventDefault() }
function preventKeyShortcuts(e: KeyboardEvent) {
  let blocked = false
  // 禁止复制/粘贴/全选/剪切/查看源代码
  if ((e.ctrlKey || e.metaKey) && ['c', 'v', 'a', 'x', 'u', 'p', 's'].includes(e.key.toLowerCase())) {
    blocked = true
  }
  // 禁止DevTools快捷键：F12, Ctrl+Shift+I/J/C
  if (e.key === 'F12') { blocked = true }
  if ((e.ctrlKey || e.metaKey) && e.shiftKey && ['i', 'j', 'c'].includes(e.key.toLowerCase())) {
    blocked = true
  }
  if (blocked) {
    e.preventDefault()
    e.stopImmediatePropagation()
  }
}

// 全屏管理
async function requestFullscreen() {
  try {
    await document.documentElement.requestFullscreen()
    // 延迟显示自定义全屏提示，等待浏览器原生通知自动消失（约3秒）后再出现
    if (fullscreenTipTimer) clearTimeout(fullscreenTipTimer)
    fullscreenTipTimer = setTimeout(() => {
      if (document.fullscreenElement) {
        showFullscreenTip.value = true
        fullscreenTipTimer = setTimeout(() => { showFullscreenTip.value = false }, 3000)
      }
    }, 3200)
  } catch { /* 部分浏览器不支持 */ }
}
function onFullscreenChange() {
  isInFullscreen.value = !!document.fullscreenElement
  if (!document.fullscreenElement) {
    showFullscreenTip.value = false
    if (antiCheat.fullscreenRequired && !isSubmitting.value && clientEndTime.value > 0) {
      ElMessage.info({ message: '已退出全屏模式，点击右上角可返回全屏', duration: 3000 })
    }
  }
}

// 无操作超时
function resetInactivityTimer() {
  lastInteractionTime = Date.now()
  if (inactivityTimer) clearTimeout(inactivityTimer)
  if (antiCheat.inactivityTimeout > 0 && !isSubmitting.value) {
    const timeoutMs = antiCheat.inactivityTimeout * 60 * 1000
    inactivityTimer = setTimeout(() => {
      if (isSubmitting.value) return
      ElMessage.error(`长时间无操作（${antiCheat.inactivityTimeout}分钟），系统将自动交卷`)
      doAutoSubmit()
    }, timeoutMs)
  }
}
function onUserInteraction() { resetInactivityTimer() }

function confirmStartExam() {
  showExamNotice.value = false
  // 标记当前考试已确认须知，刷新页面时跳过
  sessionStorage.setItem(`exam-notice-${examId}`, '1')
  // 开启防作弊功能
  if (antiCheat.noCopyPaste) {
    document.addEventListener('copy', preventCopyPaste, true)
    document.addEventListener('paste', preventCopyPaste, true)
    document.addEventListener('cut', preventCopyPaste, true)
    document.addEventListener('contextmenu', preventContextMenu, true)
    document.addEventListener('keydown', preventKeyShortcuts, true)
    document.addEventListener('dragstart', preventDrag, true)
    document.addEventListener('drop', preventDrag, true)
  }
  if (antiCheat.fullscreenRequired) {
    requestFullscreen()
    document.addEventListener('fullscreenchange', onFullscreenChange)
  }
  if (antiCheat.inactivityTimeout > 0) {
    document.addEventListener('click', onUserInteraction, true)
    document.addEventListener('keydown', onUserInteraction, true)
    document.addEventListener('scroll', onUserInteraction, true)
    resetInactivityTimer()
  }
}

async function doAutoSubmit(retryCount = 0) {
  if (isSubmitting.value) return
  isSubmitting.value = true
  ElMessage.warning('考试时间已到，系统正在自动交卷...')
  const answerList = questions.value.map(q => ({ questionId: q.id, answer: answers[q.id] || '' }))
  try {
    await submitExam({ recordId: recordId.value, answers: answerList })
    ElMessage.success('自动交卷成功')
    mittBus.emit('refreshNotification')
    router.replace('/student/exam')
  } catch (e: any) {
    const msg = e?.response?.data?.msg || e?.message || ''
    if (msg.includes('已提交') || msg.includes('已完成')) {
      ElMessage.info('试卷已由系统自动提交')
      router.replace('/student/exam')
    } else if (retryCount < 2) {
      isSubmitting.value = false
      await new Promise(r => setTimeout(r, (retryCount + 1) * 2000))
      doAutoSubmit(retryCount + 1)
    } else {
      ElMessage.error('自动交卷失败，系统将在后台自动处理')
      setTimeout(() => router.replace('/student/exam'), 3000)
    }
  }
}

function startAutoSave() {
  autoSaveTimer = setInterval(async () => {
    if (!recordId.value || remainSeconds.value <= 0) return
    try {
      const answerList = questions.value.filter(q => !!answers[q.id]).map(q => ({ questionId: q.id, answer: answers[q.id] }))
      if (answerList.length > 0) await autoSaveAnswers({ recordId: recordId.value, answers: answerList })
    } catch { /* 静默 */ }
  }, 30000)
}

onMounted(async () => {
  try {
    const d = await startExam(examId)
    examData.value = d
    recordId.value = d.recordId
    questions.value = d.questions || []
    const saved = d.savedAnswers || {}
    for (const q of questions.value) {
      const savedAns = saved[q.id] || ''
      answers[q.id] = savedAns
      if (q.questionType === 2) {
        multiAnswers[q.id] = savedAns ? savedAns.split(',').filter(Boolean) : []
      }
    }
    // 加载防作弊配置
    if (d.antiCheat) Object.assign(antiCheat, d.antiCheat)
    switchCount.value = d.switchCount || 0

    const serverTime = Number(d.serverTime)
    const endTime = Number(d.endTime)
    remainSeconds.value = Math.max(0, Math.floor((endTime - serverTime) / 1000))
    totalSeconds.value = remainSeconds.value
    clientEndTime.value = Date.now() + remainSeconds.value * 1000
    startTimerFn()
    startAutoSave()
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('beforeunload', onBeforeUnload)
    window.addEventListener('blur', onWindowBlur)

    // 判断是否需要显示考前须知（刷新页面时跳过，直接恢复防作弊功能）
    const hasAntiCheat = antiCheat.switchScreenMax > 0 || antiCheat.fullscreenRequired || antiCheat.noCopyPaste || antiCheat.inactivityTimeout > 0
    const alreadyConfirmed = sessionStorage.getItem(`exam-notice-${examId}`)
    if (hasAntiCheat && !alreadyConfirmed) {
      showExamNotice.value = true
    } else if (hasAntiCheat && alreadyConfirmed) {
      // 刷新页面续答：跳过须知，直接激活防作弊功能
      confirmStartExam()
    }
  } catch (e: any) {
    const msg = e?.response?.data?.msg || e?.message || ''
    if (msg.includes('已结束') || msg.includes('未开始')) {
      ElMessage.warning('考试已结束，正在返回...')
    } else if (msg.includes('已完成') || msg.includes('已提交')) {
      ElMessage.info('您已完成该考试')
    } else {
      ElMessage.error(msg || '无法开始考试')
    }
    router.replace('/student/exam')
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (autoSaveTimer) clearInterval(autoSaveTimer)
  if (debounceSaveTimer) clearTimeout(debounceSaveTimer)
  if (inactivityTimer) clearTimeout(inactivityTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('beforeunload', onBeforeUnload)
  window.removeEventListener('blur', onWindowBlur)
  // 清理防作弊事件
  document.removeEventListener('copy', preventCopyPaste, true)
  document.removeEventListener('paste', preventCopyPaste, true)
  document.removeEventListener('cut', preventCopyPaste, true)
  document.removeEventListener('contextmenu', preventContextMenu, true)
  document.removeEventListener('keydown', preventKeyShortcuts, true)
  document.removeEventListener('dragstart', preventDrag, true)
  document.removeEventListener('drop', preventDrag, true)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('click', onUserInteraction, true)
  document.removeEventListener('keydown', onUserInteraction, true)
  document.removeEventListener('scroll', onUserInteraction, true)
  // 清除考前须知确认标记（SPA路由导航时清除，页面刷新时onUnmounted不触发所以标记保留）
  sessionStorage.removeItem(`exam-notice-${route.params.examId}`)
  // 退出全屏
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
  }
})
</script>

<style scoped>
.hide-scrollbar {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}
.hide-scrollbar::-webkit-scrollbar {
  display: none; /* Chrome/Safari/Opera */
}
.question-answered {
  border-color: rgba(59, 130, 246, 0.3) !important;
  background: rgba(59, 130, 246, 0.03) !important;
}
.exam-no-copy {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

/* ===== 顶部信息栏 ===== */
.exam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  background: var(--default-box-color, #fff);
  border-bottom: 1px solid var(--art-card-border, #f0f0f0);
}

.exam-timer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.timer-normal { background: #eff6ff; color: #3b82f6; }
.timer-warning { background: #fffbeb; color: #f59e0b; }
.timer-danger { background: #fef2f2; color: #ef4444; animation: timerPulse 1s ease-in-out infinite; }
@keyframes timerPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* ===== 考试须知弹窗 ===== */
@keyframes noticeIn {
  from { opacity: 0; transform: translateY(12px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.notice-enter { animation: noticeIn 0.28s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
.notice-modal {
  width: 440px;
  background: var(--default-box-color, #fff);
  border-radius: calc(var(--custom-radius, 8px) + 6px);
  border: 1px solid var(--art-card-border, transparent);
  box-shadow: 0 20px 50px -12px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(0, 0, 0, 0.03);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 标题 */
.n-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 22px 24px 16px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}
.n-head-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--el-color-primary-light-3), var(--el-color-primary));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 10px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.3);
}
.n-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.3;
}
.n-subtitle {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--el-text-color-placeholder);
  line-height: 1.4;
}

/* 监控 */
.n-monitor {
  margin: 16px 24px 0;
  padding: 12px 14px;
  border-radius: calc(var(--custom-radius, 8px) + 2px);
  background: var(--el-color-primary-light-9, #f8f7ff);
  border: 1px solid var(--el-color-primary-light-8, #e8e5ff);
}
.n-monitor-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}
.n-monitor-cards {
  display: flex;
  gap: 8px;
}
.n-mcard {
  flex: 1;
  padding: 10px 12px;
  border-radius: calc(var(--custom-radius, 8px));
  background: var(--default-box-color, #fff);
  border: 1px solid var(--art-card-border, #eeedf5);
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}
.n-mcard-val {
  font-size: 20px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  display: flex;
  align-items: baseline;
}
.n-mcard-val small {
  font-size: 12px;
  font-weight: 600;
  margin-left: 2px;
  color: var(--el-text-color-secondary);
}
.n-mcard-label {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 3px;
  line-height: 1.4;
}

/* 规则 */
.n-rules {
  padding: 16px 24px 4px;
}
.n-rules-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
.n-rules-line {
  flex: 1;
  height: 1px;
  background: var(--el-border-color-lighter);
}
.n-rule {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  border-radius: 8px;
  transition: background 0.15s;
}
.n-rule:hover { background: var(--el-fill-color-light); }
.n-rule b { font-weight: 600; color: var(--el-text-color-primary); }

/* 底部 */
.n-foot {
  padding: 14px 24px 24px;
  border-top: 1px solid var(--el-border-color-extra-light);
  margin-top: 8px;
  background: var(--el-fill-color-blank);
}
.n-agree {
  display: block;
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}
.n-agree:hover {
  background: var(--el-fill-color);
}
.n-agree.is-active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}
.n-agree :deep(.el-checkbox) {
  margin: 0;
  height: auto;
  width: 100%;
}
.n-agree :deep(.el-checkbox__label) {
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
  transition: color 0.2s;
}
.n-agree.is-active :deep(.el-checkbox__label) {
  color: var(--el-color-primary);
}

.n-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 10px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-dark-2));
  border: none;
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.3);
  cursor: pointer;
  transition: all 0.2s;
}
.n-btn:hover:not(:disabled) { 
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.4); 
}
.n-btn:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 2px 8px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.3); 
}
.n-btn:disabled {
  background: var(--el-color-info-light-5);
  color: var(--el-color-info-light-3);
  box-shadow: none;
  cursor: not-allowed;
}

/* 全屏按钮淡入淡出 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ===== 自定义全屏提示覆盖层 ===== */
.fullscreen-tip-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 2147483647;
  display: flex;
  justify-content: center;
  pointer-events: none;
}
.fullscreen-tip-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 24px;
  background: rgba(30, 30, 30, 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(255, 255, 255, 0.08) inset;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.3px;
}
.fullscreen-tip-bar kbd {
  display: inline-block;
  padding: 1px 6px;
  margin: 0 2px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.6;
}
.fullscreen-tip-enter-active { transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
.fullscreen-tip-leave-active { transition: all 0.4s cubic-bezier(0.4, 0, 1, 1); }
.fullscreen-tip-enter-from { opacity: 0; transform: translateY(-20px); }
.fullscreen-tip-leave-to { opacity: 0; transform: translateY(-12px); }
</style>

