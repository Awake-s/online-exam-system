<template>
  <div>
    <!-- 顶部标题栏 -->
    <div class="art-card flex items-center justify-between px-6 py-5 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-5 min-w-0">
        <div class="w-12 h-12 rounded-xl flex-cc shrink-0" :class="data.isPassed ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'">
          <ArtSvgIcon :icon="data.isPassed ? 'ri:checkbox-circle-line' : 'ri:close-circle-line'" class="text-2xl" :class="data.isPassed ? 'text-green-500' : 'text-red-500'" />
        </div>
        <div class="min-w-0">
          <div class="flex items-center gap-2.5 mb-1">
            <h3 class="text-lg font-bold text-gray-800 dark:text-gray-200 m-0 truncate leading-tight">{{ data.examName || '考试成绩' }}</h3>
            <el-tag v-if="data.isPassed" type="success" effect="light" round>及格</el-tag>
            <el-tag v-else-if="data.isPassed === false" type="danger" effect="light" round>不及格</el-tag>
            <el-tag v-if="data.statusName" :type="data.status === 3 ? 'success' : 'warning'" effect="plain" round size="small">{{ data.statusName }}</el-tag>
          </div>
          <div class="flex items-center gap-4 text-[13px] text-gray-400">
            <span>共 {{ totalCount }} 题</span>
            <span class="w-px h-3 bg-gray-200 dark:bg-gray-600"></span>
            <span>满分 {{ data.paperTotalScore }} 分</span>
            <span class="w-px h-3 bg-gray-200 dark:bg-gray-600"></span>
            <span>及格 {{ data.passScore }} 分</span>
          </div>
        </div>
      </div>
      <el-button plain @click="router.back()">
        <ArtSvgIcon icon="ri:arrow-left-line" class="mr-1" />返回列表
      </el-button>
    </div>

    <!-- 信息指标栏 -->
    <div class="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-5 max-sm:mb-4">
      <!-- 总分（分数环） -->
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="shrink-0 relative w-12 h-12 flex items-center justify-center">
          <svg class="absolute inset-0 w-full h-full -rotate-90" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="42" fill="none" stroke-width="8" class="stroke-gray-100 dark:stroke-gray-700" />
            <circle cx="50" cy="50" r="42" fill="none" stroke-width="8" stroke-linecap="round"
              :class="data.isPassed ? 'stroke-green-500' : 'stroke-red-400'"
              :stroke-dasharray="`${scorePercent * 2.64} 264`" style="transition: stroke-dasharray 0.8s ease" />
          </svg>
          <span class="text-[11px] font-bold z-10" :class="data.isPassed ? 'text-green-500' : 'text-red-500'">{{ scorePercent }}%</span>
        </div>
        <div>
          <div class="text-xs text-gray-400">总分</div>
          <div class="flex items-baseline gap-1 mt-0.5">
            <span class="text-2xl font-bold leading-tight" :class="data.isPassed ? 'text-green-500' : 'text-red-500'">{{ data.totalScore }}</span>
            <span class="text-xs text-gray-400 font-medium">/ {{ data.paperTotalScore }}</span>
          </div>
        </div>
      </div>
      <!-- 客观题 -->
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-blue-50 dark:bg-blue-900/20">
          <ArtSvgIcon icon="ri:trophy-line" class="text-xl text-blue-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">客观题</div>
          <div class="flex items-baseline gap-1 mt-0.5">
            <span class="text-2xl font-bold text-blue-500 leading-tight">{{ data.objectiveScore ?? 0 }}</span>
            <span class="text-xs text-gray-400 font-medium">分</span>
          </div>
        </div>
      </div>
      <!-- 主观题 -->
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-purple-50 dark:bg-purple-900/20">
          <ArtSvgIcon icon="ri:edit-circle-line" class="text-xl text-purple-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">主观题</div>
          <div class="flex items-baseline gap-1 mt-0.5">
            <span class="text-2xl font-bold text-purple-500 leading-tight">{{ data.subjectiveScore ?? 0 }}</span>
            <span class="text-xs text-gray-400 font-medium">分</span>
          </div>
        </div>
      </div>
      <!-- 答题统计 -->
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-green-50 dark:bg-green-900/20">
          <ArtSvgIcon icon="ri:bar-chart-grouped-line" class="text-xl text-green-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">正确率</div>
          <div class="flex items-center gap-2 mt-0.5">
            <span class="text-2xl font-bold leading-tight" :class="correctRate >= 60 ? 'text-green-500' : 'text-red-500'">{{ correctRate }}%</span>
            <span class="text-[11px] text-gray-400">{{ correctCount }}/{{ totalCount }}</span>
          </div>
        </div>
      </div>
      <!-- 排名 -->
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc" :class="data.rank && data.rank <= 3 ? 'bg-amber-50 dark:bg-amber-900/20' : 'bg-gray-50 dark:bg-gray-800'">
          <ArtSvgIcon icon="ri:medal-line" class="text-xl" :class="data.rank && data.rank <= 3 ? 'text-amber-500' : 'text-gray-400'" />
        </div>
        <div>
          <div class="text-xs text-gray-400">排名</div>
          <div class="flex items-baseline gap-1 mt-0.5">
            <span v-if="data.rank" class="text-2xl font-bold leading-tight" :class="data.rank <= 3 ? 'text-amber-500' : 'text-gray-700 dark:text-gray-300'">{{ data.rank }}</span>
            <span v-else class="text-sm text-gray-300 dark:text-gray-500">暂无</span>
            <span v-if="data.totalParticipants" class="text-xs text-gray-400 font-medium">/ {{ data.totalParticipants }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 缺考提示 -->
    <div v-if="data.status === 4" class="art-card flex flex-col items-center py-16 space-y-4 text-center">
      <ArtSvgIcon icon="ri:error-warning-fill" class="text-5xl text-danger" />
      <h3 class="text-xl font-semibold text-gray-800 dark:text-gray-200">您未参加本次考试</h3>
      <p class="text-sm text-gray-500 max-w-md leading-relaxed">由于您在考试开放时间内未进入考试，系统已自动记录为缺考，成绩计为 0 分。如有疑问请联系任课教师。</p>
      <el-button type="primary" @click="router.back()">返回成绩列表</el-button>
    </div>

    <!-- 答题详情 -->
    <div v-if="data.status !== 4" class="art-card p-5 mb-5 max-sm:mb-4">
      <div class="flex-cb mb-4">
        <h4 class="text-base font-semibold text-g-800">答题详情</h4>
        <span class="text-xs text-g-400">共 {{ totalCount }} 题</span>
      </div>
      <!-- 筛选标签栏 -->
      <div class="flex items-center gap-0 rounded-xl border border-gray-200 dark:border-gray-600 overflow-hidden mb-4">
        <button v-for="tab in filterTabs" :key="tab.key"
          class="relative flex items-center gap-1.5 px-5 py-3 text-[13px] font-medium transition-colors"
          :class="answerFilter === tab.key ? 'text-blue-600 dark:text-blue-400' : 'text-gray-400 hover:text-gray-600'"
          @click="answerFilter = tab.key">
          <ArtSvgIcon v-if="tab.key === 'all'" icon="ri:list-check" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'correct'" icon="ri:checkbox-circle-line" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'wrong'" icon="ri:close-circle-line" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'partial'" icon="ri:error-warning-line" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'unanswered'" icon="ri:subtract-line" class="text-sm" />
          {{ tab.label }}
          <span class="ml-0.5 px-1.5 py-0.5 rounded-full text-[11px] leading-none"
            :class="answerFilter === tab.key ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'bg-gray-100 dark:bg-gray-700 text-gray-400'">{{ tab.count }}</span>
          <span v-if="answerFilter === tab.key" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-8 h-0.5 rounded-full bg-blue-500"></span>
        </button>
      </div>
      <div class="space-y-4">
        <div v-for="(a, i) in filteredAnswers" :key="a._origIndex"
          class="rounded-xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 overflow-hidden hover:shadow-md transition-shadow">
          <!-- 题目头部 -->
          <div class="flex items-center justify-between px-5 py-3 border-b border-gray-50 dark:border-gray-700/50"
            :class="a.isCorrect === 1 ? 'bg-green-50/50 dark:bg-green-900/10' : a.isCorrect === 0 ? 'bg-red-50/50 dark:bg-red-900/10' : a.isCorrect === 2 ? 'bg-amber-50/50 dark:bg-amber-900/10' : 'bg-gray-50/50 dark:bg-gray-800/30'">
            <div class="flex items-center gap-2.5">
              <span class="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold"
                :class="a.isCorrect === 1 ? 'bg-green-100 text-green-600 dark:bg-green-800/30 dark:text-green-400' : a.isCorrect === 0 ? 'bg-red-100 text-red-600 dark:bg-red-800/30 dark:text-red-400' : a.isCorrect === 2 ? 'bg-amber-100 text-amber-600 dark:bg-amber-800/30 dark:text-amber-400' : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'">
                {{ a._origIndex + 1 }}
              </span>
              <el-tag :type="a.isCorrect === 1 ? 'success' : a.isCorrect === 0 ? 'danger' : a.isCorrect === 2 ? 'warning' : 'info'" size="small" round>
                {{ a.isCorrect === 1 ? '正确' : a.isCorrect === 0 ? '错误' : a.isCorrect === 2 ? '部分正确' : '未作答' }}
              </el-tag>
              <el-tag v-if="a.questionTypeName" type="info" size="small" round>{{ a.questionTypeName }}</el-tag>
            </div>
            <span class="text-sm font-medium">
              <span class="text-lg font-bold" :class="a.isCorrect === 1 ? 'text-green-500' : a.isCorrect === 0 ? 'text-red-500' : a.isCorrect === 2 ? 'text-amber-500' : 'text-gray-400'">{{ a.score ?? '—' }}</span>
              <span class="text-gray-400"> / {{ a.fullScore }} 分</span>
            </span>
          </div>
          <!-- 题目内容 -->
          <div class="px-5 py-4 space-y-3">
            <div class="text-sm leading-relaxed text-gray-800 dark:text-gray-200 font-medium">{{ a.content }}</div>
            <!-- 选项：高亮正确/错误 -->
            <div v-if="a.options && a.options.length" class="space-y-2">
              <div v-for="(opt, oi) in a.options" :key="oi"
                class="flex items-center gap-2 text-sm px-3.5 py-2.5 rounded-lg border transition-colors"
                :class="getOptionClass(opt, a)">
                <span class="shrink-0 w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold border"
                  :class="getOptionBadgeClass(opt, a)">{{ String.fromCharCode(65 + Number(oi)) }}</span>
                <span>{{ getOptionText(opt) }}</span>
              </div>
            </div>
            <!-- 答案对比色块 -->
            <div class="grid grid-cols-2 gap-3 mt-1">
              <div class="px-4 py-2.5 rounded-lg bg-green-50 dark:bg-green-900/15 border border-green-100 dark:border-green-800/30">
                <div class="text-xs text-green-500 mb-0.5 flex items-center gap-1">
                  <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-sm" />正确答案
                </div>
                <div class="text-sm text-green-600 dark:text-green-400 font-medium break-all">{{ a.correctAnswer }}</div>
              </div>
              <div class="px-4 py-2.5 rounded-lg" :class="a.isCorrect === 1 ? 'bg-green-50 dark:bg-green-900/15 border border-green-100 dark:border-green-800/30' : a.isCorrect === 0 ? 'bg-red-50 dark:bg-red-900/15 border border-red-100 dark:border-red-800/30' : a.isCorrect === 2 ? 'bg-amber-50 dark:bg-amber-900/15 border border-amber-100 dark:border-amber-800/30' : 'bg-gray-50 dark:bg-gray-800/30 border border-gray-100 dark:border-gray-700'">
                <div class="text-xs mb-0.5 flex items-center gap-1" :class="a.isCorrect === 1 ? 'text-green-500' : a.isCorrect === 0 ? 'text-red-400' : a.isCorrect === 2 ? 'text-amber-500' : 'text-gray-400'">
                  <ArtSvgIcon :icon="a.isCorrect === 1 ? 'ri:checkbox-circle-fill' : a.isCorrect === 0 ? 'ri:close-circle-fill' : a.isCorrect === 2 ? 'ri:error-warning-fill' : 'ri:subtract-line'" class="text-sm" />我的答案
                </div>
                <div class="text-sm font-medium break-all" :class="a.isCorrect === 1 ? 'text-green-600 dark:text-green-400' : a.isCorrect === 0 ? 'text-red-500 dark:text-red-400' : a.isCorrect === 2 ? 'text-amber-600 dark:text-amber-400' : 'text-gray-400'">{{ a.myAnswer || '未作答' }}</div>
              </div>
            </div>
            <!-- 解析 -->
            <div v-if="a.analysis" class="p-3.5 bg-blue-50 dark:bg-blue-900/15 border border-blue-100 dark:border-blue-800/30 rounded-lg">
              <div class="text-xs text-blue-500 mb-1 flex items-center gap-1">
                <ArtSvgIcon icon="ri:lightbulb-line" class="text-sm" />解析
              </div>
              <div class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{{ a.analysis }}</div>
            </div>
            <!-- 教师评语 -->
            <div v-if="a.comment" class="p-3.5 bg-amber-50 dark:bg-amber-900/15 border border-amber-100 dark:border-amber-800/30 rounded-lg">
              <div class="text-xs text-amber-500 mb-1 flex items-center gap-1">
                <ArtSvgIcon icon="ri:chat-1-line" class="text-sm" />教师评语
              </div>
              <div class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{{ a.comment }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExamResult } from '@/api/exam/studentExam'

defineOptions({ name: 'ScoreDetail' })
const route = useRoute()
const router = useRouter()
const data = ref<any>({})
const answerFilter = ref('all')

const scorePercent = computed(() => {
  if (!data.value.paperTotalScore) return 0
  return Math.min(100, Math.round((data.value.totalScore / data.value.paperTotalScore) * 100))
})

const answers = computed(() => (data.value.answers || []) as any[])
const totalCount = computed(() => answers.value.length)
const correctCount = computed(() => answers.value.filter((a: any) => a.isCorrect === 1).length)
const wrongCount = computed(() => answers.value.filter((a: any) => a.isCorrect === 0).length)
const partialCount = computed(() => answers.value.filter((a: any) => a.isCorrect === 2).length)
const unansweredCount = computed(() => answers.value.filter((a: any) => a.isCorrect === null).length)
const correctRate = computed(() => {
  if (!totalCount.value) return 0
  return Math.round((correctCount.value / totalCount.value) * 100)
})

const filterTabs = computed(() => [
  { key: 'all', label: '全部', count: totalCount.value },
  { key: 'correct', label: '正确', count: correctCount.value },
  { key: 'wrong', label: '错误', count: wrongCount.value },
  ...(partialCount.value > 0 ? [{ key: 'partial', label: '部分正确', count: partialCount.value }] : []),
  ...(unansweredCount.value > 0 ? [{ key: 'unanswered', label: '未作答', count: unansweredCount.value }] : [])
])

const filteredAnswers = computed(() => {
  const all = answers.value.map((a: any, i: number) => ({ ...a, _origIndex: i }))
  if (answerFilter.value === 'all') return all
  if (answerFilter.value === 'correct') return all.filter((a: any) => a.isCorrect === 1)
  if (answerFilter.value === 'wrong') return all.filter((a: any) => a.isCorrect === 0)
  if (answerFilter.value === 'partial') return all.filter((a: any) => a.isCorrect === 2)
  if (answerFilter.value === 'unanswered') return all.filter((a: any) => a.isCorrect === null)
  return all
})

function getOptionLetter(opt: string): string {
  const m = opt.match(/^([A-Z])[\.\s、：:]/)
  return m ? m[1] : ''
}

function getOptionText(opt: string): string {
  return opt.replace(/^[A-Z][\.\s、：:]\s*/, '')
}

function isCorrectOption(opt: string, answer: any): boolean {
  const letter = getOptionLetter(opt)
  if (!letter || !answer.correctAnswer) return false
  return answer.correctAnswer.includes(letter)
}

function isMyOption(opt: string, answer: any): boolean {
  const letter = getOptionLetter(opt)
  if (!letter || !answer.myAnswer) return false
  return answer.myAnswer.includes(letter)
}

function getOptionClass(opt: string, answer: any): string {
  const correct = isCorrectOption(opt, answer)
  const mine = isMyOption(opt, answer)
  if (correct && mine) return 'border-green-200 bg-green-50 dark:border-green-700 dark:bg-green-900/20'
  if (correct) return 'border-green-200 bg-green-50/50 dark:border-green-800 dark:bg-green-900/10'
  if (mine && !correct) return 'border-red-200 bg-red-50 dark:border-red-700 dark:bg-red-900/20'
  return 'border-gray-100 bg-gray-50/50 dark:border-gray-700 dark:bg-gray-800'
}

function getOptionBadgeClass(opt: string, answer: any): string {
  const correct = isCorrectOption(opt, answer)
  const mine = isMyOption(opt, answer)
  if (correct && mine) return 'border-green-400 bg-green-500 text-white'
  if (correct) return 'border-green-300 bg-green-100 text-green-600 dark:bg-green-800/40 dark:text-green-400 dark:border-green-600'
  if (mine && !correct) return 'border-red-300 bg-red-500 text-white'
  return 'border-gray-200 dark:border-gray-600 text-gray-400'
}

onMounted(async () => {
  data.value = await getExamResult(Number(route.params.recordId))
})
</script>

