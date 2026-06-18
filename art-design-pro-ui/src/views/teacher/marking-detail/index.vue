<template>
  <div class="marking-detail-page">
    <!-- 顶部标题栏 -->
    <div class="art-card flex items-center justify-between px-6 py-5 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-5 min-w-0">
        <div class="w-12 h-12 rounded-xl flex-cc shrink-0" :class="isGraded ? 'bg-green-50 dark:bg-green-900/20' : 'bg-blue-50 dark:bg-blue-900/20'">
          <ArtSvgIcon :icon="isGraded ? 'ri:checkbox-circle-line' : 'ri:file-edit-line'" class="text-2xl" :class="isGraded ? 'text-green-500' : 'text-blue-500'" />
        </div>
        <div class="min-w-0">
          <div class="flex items-center gap-2.5 mb-1">
            <h3 class="text-lg font-bold text-gray-800 dark:text-gray-200 m-0 truncate leading-tight">{{ detail.examName }}</h3>
            <el-tag v-if="isGraded" type="success" effect="light" round>已批改</el-tag>
            <el-tag v-else-if="isPending" type="warning" effect="light" round>待批改</el-tag>
          </div>
          <div class="flex items-center gap-4 text-[13px] text-gray-400">
            <span class="flex items-center gap-1.5">
              <ArtSvgIcon icon="ri:user-3-line" class="text-sm text-blue-400" />
              <b class="text-gray-600 dark:text-gray-300">{{ detail.realName }}</b>
            </span>
            <span class="w-px h-3 bg-gray-200 dark:bg-gray-600"></span>
            <span>共 {{ totalQuestionCount }} 题</span>
            <span class="w-px h-3 bg-gray-200 dark:bg-gray-600"></span>
            <span>满分 {{ totalMaxScore }} 分</span>
            <span class="w-px h-3 bg-gray-200 dark:bg-gray-600"></span>
            <span>{{ formatTime(detail.submitTime) }} 交卷</span>
          </div>
        </div>
      </div>
      <el-button plain @click="router.back()">
        <ArtSvgIcon icon="ri:arrow-left-line" class="mr-1" />返回列表
      </el-button>
    </div>

    <!-- 信息指标栏 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-5 max-sm:mb-4">
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-blue-50 dark:bg-blue-900/20">
          <ArtSvgIcon icon="ri:trophy-line" class="text-xl text-blue-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">客观题得分</div>
          <div class="flex items-baseline gap-1 mt-0.5">
            <span class="text-2xl font-bold text-blue-500 leading-tight">{{ detail.objectiveScore ?? 0 }}</span>
            <span class="text-xs text-gray-400 font-medium">/ {{ objectiveMaxScore }}</span>
          </div>
        </div>
      </div>
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc" :class="hasSubjective ? (gradedSubjectiveCount === subjectiveCount ? 'bg-green-50 dark:bg-green-900/20' : 'bg-amber-50 dark:bg-amber-900/20') : 'bg-gray-50 dark:bg-gray-800'">
          <ArtSvgIcon :icon="hasSubjective ? 'ri:edit-circle-line' : 'ri:subtract-line'" class="text-xl" :class="hasSubjective ? (gradedSubjectiveCount === subjectiveCount ? 'text-green-500' : 'text-amber-500') : 'text-gray-400'" />
        </div>
        <div>
          <div class="text-xs text-gray-400">主观题评分</div>
          <div v-if="hasSubjective" class="flex items-baseline gap-1 mt-0.5">
            <span class="text-2xl font-bold leading-tight" :class="gradedSubjectiveCount === subjectiveCount ? 'text-green-500' : 'text-amber-500'">{{ gradedSubjectiveCount }}</span>
            <span class="text-xs text-gray-400 font-medium">/ {{ subjectiveCount }} 题{{ gradedSubjectiveCount === subjectiveCount ? ' 已评' : ' 待评' }}</span>
          </div>
          <div v-else class="text-sm text-gray-300 dark:text-gray-500 mt-0.5">无主观题</div>
        </div>
      </div>
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc bg-purple-50 dark:bg-purple-900/20">
          <ArtSvgIcon icon="ri:bar-chart-grouped-line" class="text-xl text-purple-500" />
        </div>
        <div>
          <div class="text-xs text-gray-400">答题统计</div>
          <div class="flex items-center gap-2 mt-1">
            <span class="text-sm font-bold text-gray-700 dark:text-gray-300">{{ answeredCount }}/{{ totalQuestionCount }}</span>
            <div class="flex items-center gap-1 text-[11px]">
              <span v-if="correctCount" class="px-1.5 py-0.5 rounded bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 font-medium">{{ correctCount }}对</span>
              <span v-if="wrongCount" class="px-1.5 py-0.5 rounded bg-red-50 dark:bg-red-900/20 text-red-500 font-medium">{{ wrongCount }}错</span>
              <span v-if="unansweredCount" class="px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-700 text-gray-500 font-medium">{{ unansweredCount }}空</span>
            </div>
          </div>
        </div>
      </div>
      <div class="art-card flex items-center gap-3.5 px-5 py-4">
        <div class="w-11 h-11 rounded-xl flex-cc" :class="estimatedTotal >= 60 ? 'bg-green-50 dark:bg-green-900/20' : 'bg-red-50 dark:bg-red-900/20'">
          <ArtSvgIcon icon="ri:medal-line" class="text-xl" :class="estimatedTotal >= 60 ? 'text-green-500' : 'text-red-500'" />
        </div>
        <div>
          <div class="text-xs text-gray-400">预估总分</div>
          <div class="flex items-baseline gap-1 mt-0.5">
            <span class="text-2xl font-bold leading-tight" :class="estimatedTotal >= 60 ? 'text-green-500' : 'text-red-500'">{{ estimatedTotal }}</span>
            <span class="text-xs text-gray-400 font-medium">/ {{ totalMaxScore }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 答题详情 -->
    <ElCard class="art-table-card" shadow="never" style="margin-top: 0; margin-bottom: 20px">
      <template #header>
        <div class="flex items-center gap-3">
          <h4 class="m-0">答题详情</h4>
        </div>
      </template>
      <!-- 筛选标签栏 -->
      <div class="flex items-center gap-0 rounded-xl border border-gray-200 dark:border-gray-600 overflow-hidden">
        <button v-for="tab in filterTabs" :key="tab.key"
          class="relative flex items-center gap-1.5 px-5 py-3.5 text-[13px] font-medium transition-colors"
          :class="answerFilter === tab.key
            ? 'text-blue-600 dark:text-blue-400'
            : 'text-gray-400 hover:text-gray-600'"
          @click="answerFilter = tab.key">
          <ArtSvgIcon v-if="tab.key === 'all'" icon="ri:list-check" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'marking'" icon="ri:edit-circle-line" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'correct'" icon="ri:checkbox-circle-line" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'wrong'" icon="ri:close-circle-line" class="text-sm" />
          <ArtSvgIcon v-else-if="tab.key === 'unanswered'" icon="ri:subtract-line" class="text-sm" />
          <ArtSvgIcon v-else icon="ri:indeterminate-circle-line" class="text-sm" />
          {{ tab.label }}
          <span class="min-w-[20px] text-center text-[11px] px-1 rounded-md leading-[18px]"
            :class="answerFilter === tab.key ? 'bg-blue-500 text-white' : 'bg-gray-100 dark:bg-gray-600 text-gray-400'">{{ tab.count }}</span>
          <div v-if="answerFilter === tab.key" class="absolute bottom-0 left-2 right-2 h-[2px] bg-blue-500 rounded-t-full"></div>
        </button>
      </div>

      <!-- 题目列表（按题型分组） -->
      <div class="pt-5 space-y-6">
        <el-empty v-if="!filteredAnswers.length" description="没有符合条件的题目" :image-size="80" />
        <div v-for="group in groupedFilteredAnswers" :key="group.type">
          <!-- 题型分组头 -->
          <div class="flex items-center justify-between px-4 py-2.5 mb-3 rounded-lg bg-gray-50 dark:bg-gray-800/60">
            <div class="flex items-center gap-2.5">
              <div class="w-1 h-4 rounded-full bg-blue-500"></div>
              <span class="text-[13px] font-bold text-gray-700 dark:text-gray-300">{{ group.typeName }}</span>
              <span class="text-xs text-gray-400 font-normal">· 共 {{ group.count }} 题</span>
            </div>
            <div class="flex items-center gap-1 text-xs text-gray-400">
              小计 <b class="text-blue-500 text-sm">{{ group.subtotal }}</b> 分
            </div>
          </div>

          <!-- 该类型下的题目卡片 -->
          <div class="space-y-3">
            <div v-for="a in group.questions" :key="a.answerId" :ref="el => { if (el) questionRefs[a.answerId] = el }"
              class="rounded-xl border overflow-hidden transition-all hover:shadow-sm"
              :class="a.needMarking
                ? 'border-amber-200 dark:border-amber-800 bg-amber-50/30 dark:bg-amber-900/5'
                : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800'">

              <!-- 题头行 -->
              <div class="flex items-center px-4 py-3 cursor-pointer select-none group" @click="toggleCollapse(a)">
                <span class="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold text-white shrink-0"
                  :class="a.isCorrect === 1 ? 'bg-green-500' : a.isCorrect === 0 ? 'bg-red-500' : a.isCorrect === 2 ? 'bg-blue-500' : a.needMarking ? 'bg-amber-500' : 'bg-gray-400'">
                  {{ a.originalIndex + 1 }}
                </span>
                <div class="flex-1 ml-3 min-w-0">
                  <div class="text-[13px] text-gray-800 dark:text-gray-200 leading-5 line-clamp-1">{{ a.content }}</div>
                </div>
                <div class="flex items-center gap-2 shrink-0 ml-3">
                  <!-- 状态徽章 -->
                  <span v-if="a.isCorrect === 1" class="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-[11px] font-medium bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400">
                    <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-xs" />正确
                  </span>
                  <span v-else-if="a.isCorrect === 0" class="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-[11px] font-medium bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400">
                    <ArtSvgIcon icon="ri:close-circle-fill" class="text-xs" />错误
                  </span>
                  <span v-else-if="a.isCorrect === 2" class="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-[11px] font-medium bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400">
                    <ArtSvgIcon icon="ri:indeterminate-circle-line" class="text-xs" />部分正确
                  </span>
                  <span v-else-if="a.needMarking" class="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-[11px] font-medium bg-amber-50 dark:bg-amber-900/20 text-amber-600 dark:text-amber-400">
                    <ArtSvgIcon icon="ri:edit-circle-line" class="text-xs" />待批改
                  </span>
                  <span v-else class="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-[11px] font-medium bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400">
                    <ArtSvgIcon icon="ri:subtract-line" class="text-xs" />未作答
                  </span>
                  <!-- 得分徽章 -->
                  <div class="flex items-baseline gap-0.5 px-2.5 py-1 rounded-lg bg-gray-50 dark:bg-gray-700/50">
                    <span class="text-sm font-bold" :class="a.isCorrect === 1 ? 'text-green-600' : a.isCorrect === 0 ? 'text-red-500' : a.isCorrect === 2 ? 'text-blue-500' : 'text-gray-500'">
                      {{ a.isCorrect !== null ? a.score : '—' }}
                    </span>
                    <span class="text-[11px] text-gray-400">/{{ a.fullScore }}</span>
                  </div>
                  <!-- 展开/收起箭头 -->
                  <div v-if="!a.needMarking"
                    class="w-6 h-6 rounded-md flex items-center justify-center transition-colors"
                    :class="collapsedSet[a.answerId] ? 'bg-gray-100 dark:bg-gray-600' : 'bg-blue-50 dark:bg-blue-900/30'">
                    <ArtSvgIcon :icon="collapsedSet[a.answerId] ? 'ri:arrow-down-s-line' : 'ri:arrow-up-s-line'"
                      class="text-sm" :class="collapsedSet[a.answerId] ? 'text-gray-400' : 'text-blue-500'" />
                  </div>
                </div>
              </div>

              <!-- 展开内容 -->
              <div v-show="!collapsedSet[a.answerId] || a.needMarking">
                <div class="mx-4 mb-4 rounded-lg border border-gray-100 dark:border-gray-600 bg-gray-50/50 dark:bg-gray-800/50 overflow-hidden">
                  <!-- 选项（仅选择题：单选1/多选2） -->
                  <div v-if="a.questionType === 1 || a.questionType === 2" class="p-4 space-y-2">
                    <div v-for="opt in a.options" :key="opt"
                      class="flex items-center text-[13px] px-3.5 py-2.5 rounded-lg border transition-colors"
                      :class="isCorrectOption(opt, a.correctAnswer)
                        ? 'bg-green-50 dark:bg-green-900/15 border-green-200 dark:border-green-800 text-green-700 dark:text-green-400'
                        : isStudentWrongOption(opt, a.studentAnswer, a.correctAnswer, a.isCorrect)
                          ? 'bg-red-50 dark:bg-red-900/15 border-red-200 dark:border-red-800 text-red-600 dark:text-red-400'
                          : 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-600 text-gray-600 dark:text-gray-400'">
                      <ArtSvgIcon v-if="isCorrectOption(opt, a.correctAnswer)" icon="ri:checkbox-circle-fill" class="text-sm text-green-500 mr-2 shrink-0" />
                      <ArtSvgIcon v-else-if="isStudentWrongOption(opt, a.studentAnswer, a.correctAnswer, a.isCorrect)" icon="ri:close-circle-fill" class="text-sm text-red-500 mr-2 shrink-0" />
                      <span class="leading-relaxed">{{ opt }}</span>
                    </div>
                    <!-- 答案行 -->
                    <div class="flex items-center gap-6 text-xs pt-2.5 mt-1 border-t border-gray-200 dark:border-gray-600">
                      <span class="text-gray-400">正确答案：<b class="text-green-600 dark:text-green-400">{{ a.correctAnswer }}</b></span>
                      <span class="text-gray-400">学生答案：<b :class="a.isCorrect === 0 ? 'text-red-600 dark:text-red-400' : 'text-blue-600 dark:text-blue-400'">{{ a.studentAnswer || '未作答' }}</b></span>
                      <span v-if="a.isCorrect === 1" class="text-green-500 font-medium">✓ 回答正确</span>
                      <span v-else-if="a.isCorrect === 0" class="text-red-500 font-medium">✗ 回答错误</span>
                    </div>
                  </div>

                  <!-- 非选择题答案对比（判断3/填空4/简答5） -->
                  <div v-if="a.questionType >= 3" class="p-4">
                    <div class="grid grid-cols-2 gap-3">
                      <div class="px-4 py-3 rounded-lg bg-green-50 dark:bg-green-900/10 border border-green-200 dark:border-green-800/30">
                        <div class="text-[11px] text-green-600 dark:text-green-400 mb-1.5 flex items-center gap-1 font-semibold">
                          <ArtSvgIcon icon="ri:checkbox-circle-fill" class="text-xs" />参考答案
                        </div>
                        <div class="text-[13px] text-green-800 dark:text-green-300 leading-6 whitespace-pre-wrap">{{ a.questionType === 3 ? (a.correctAnswer == '1' || a.correctAnswer === '正确' ? '正确 ✓' : '错误 ✗') : a.correctAnswer }}</div>
                      </div>
                      <div class="px-4 py-3 rounded-lg border"
                        :class="a.isCorrect === 0 ? 'bg-red-50 dark:bg-red-900/10 border-red-200 dark:border-red-800/30' : a.needMarking ? 'bg-amber-50 dark:bg-amber-900/10 border-amber-200 dark:border-amber-800/30' : 'bg-blue-50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800/30'">
                        <div class="text-[11px] mb-1.5 flex items-center gap-1 font-semibold" :class="a.isCorrect === 0 ? 'text-red-600 dark:text-red-400' : a.needMarking ? 'text-amber-600 dark:text-amber-400' : 'text-blue-600 dark:text-blue-400'">
                          <ArtSvgIcon :icon="a.isCorrect === 0 ? 'ri:close-circle-fill' : 'ri:user-3-fill'" class="text-xs" />学生答案
                        </div>
                        <div class="text-[13px] leading-6 whitespace-pre-wrap" :class="a.isCorrect === 0 ? 'text-red-800 dark:text-red-300' : a.needMarking ? 'text-amber-800 dark:text-amber-300' : 'text-blue-800 dark:text-blue-300'">{{ a.questionType === 3 ? (a.studentAnswer == '1' || a.studentAnswer === '正确' ? '正确 ✓' : a.studentAnswer == '0' || a.studentAnswer === '错误' ? '错误 ✗' : (a.studentAnswer || '未作答')) : (a.studentAnswer || '未作答') }}</div>
                      </div>
                    </div>
                  </div>

                  <!-- 题目解析（所有题型通用） -->
                  <div v-if="a.analysis" class="px-4 pb-4">
                    <div class="px-4 py-3 rounded-lg bg-purple-50/80 dark:bg-purple-900/10 border border-purple-100 dark:border-purple-800/30">
                      <div class="text-[11px] text-purple-600 dark:text-purple-400 mb-1.5 flex items-center gap-1 font-semibold">
                        <ArtSvgIcon icon="ri:lightbulb-line" class="text-xs" />题目解析
                      </div>
                      <div class="text-[13px] text-purple-800 dark:text-purple-300 leading-6 whitespace-pre-wrap">{{ a.analysis }}</div>
                    </div>
                  </div>
                </div>

                <!-- 主观题批改区 -->
                <div v-if="a.needMarking" :ref="el => { if (el) markingRefs[a.answerId] = el }" class="mx-4 mb-4 p-4 rounded-lg bg-gradient-to-br from-amber-50 to-orange-50/50 dark:from-amber-900/10 dark:to-orange-900/5 border border-amber-200 dark:border-amber-700 space-y-3">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2 text-[13px] font-bold text-amber-700 dark:text-amber-400">
                      <div class="w-6 h-6 rounded-md bg-amber-500 flex items-center justify-center">
                        <ArtSvgIcon icon="ri:edit-2-line" class="text-xs text-white" />
                      </div>
                      教师评分
                    </div>
                    <div class="flex items-center gap-1.5">
                      <button v-for="pct in [0, 50, 75, 100]" :key="pct"
                        class="px-3 h-7 text-[11px] rounded-lg border font-medium transition-all"
                        :class="gradedSet[a.answerId] && scoreMap[a.answerId] === Math.round(a.fullScore * pct / 100 * 10) / 10
                          ? 'bg-blue-500 text-white border-blue-500 shadow-sm'
                          : 'bg-white dark:bg-gray-700 text-gray-500 dark:text-gray-400 border-gray-200 dark:border-gray-600 hover:border-blue-400 hover:text-blue-500'"
                        @click="scoreMap[a.answerId] = Math.round(a.fullScore * pct / 100 * 10) / 10; gradedSet[a.answerId] = true">
                        {{ pct }}%
                      </button>
                    </div>
                  </div>
                  <div class="flex items-start gap-4">
                    <div class="w-36 shrink-0">
                      <div class="text-[11px] text-gray-500 dark:text-gray-400 mb-1.5 font-medium">给分（满分 {{ a.fullScore }}）</div>
                      <el-input-number v-model="scoreMap[a.answerId]" :min="0" :max="a.fullScore" :step="0.5" size="small" style="width: 100%" @change="(val: any) => { gradedSet[a.answerId] = val != null }" />
                    </div>
                    <div class="flex-1">
                      <div class="text-[11px] text-gray-500 dark:text-gray-400 mb-1.5 font-medium">评语（选填）</div>
                      <el-input v-model="commentMap[a.answerId]" type="textarea" :rows="2" placeholder="输入批改评语…" resize="none" size="small" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </ElCard>

    <!-- 浮动导航：精简版，只显示待批改题 -->
    <div v-if="hasSubjective && canMark" class="fixed right-4 top-1/2 -translate-y-1/2 z-20">
      <div class="flex flex-col items-center gap-2 p-2.5 rounded-2xl bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm shadow-xl border border-gray-100 dark:border-gray-700">
        <div class="text-[10px] text-gray-400 font-medium">待批改</div>
        <button v-for="(a, idx) in subjectiveAnswers" :key="'nav-'+a.answerId"
          class="w-8 h-8 rounded-lg text-xs font-bold flex items-center justify-center transition-all"
          :class="gradedSet[a.answerId]
            ? 'bg-green-500 text-white shadow-sm hover:bg-green-600'
            : 'bg-amber-100 text-amber-600 border border-amber-200 hover:bg-amber-200'"
          @click="scrollToQuestion(a.answerId)">
          {{ a.index + 1 }}
        </button>
        <div class="w-6 h-px bg-gray-200 dark:bg-gray-600"></div>
        <button class="w-8 h-8 rounded-lg bg-blue-500 text-white flex-cc shadow-sm hover:bg-blue-600 transition-all"
          @click="scrollToNextUngraded" title="下一题待批改">
          <ArtSvgIcon icon="ri:arrow-down-line" class="text-sm" />
        </button>
      </div>
    </div>

    <!-- 底部提交栏 -->
    <div v-if="canMark" class="art-card sticky bottom-3 z-10 flex-cb px-6 py-4 shadow-lg mb-5 max-sm:mb-4">
      <!-- 含主观题：显示批改进度 -->
      <div v-if="hasSubjective" class="flex items-center gap-5 text-sm">
        <div class="flex items-center gap-2">
          <span class="text-gray-400">批改进度</span>
          <div class="flex items-center gap-1.5">
            <div class="w-28 h-2.5 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all duration-500" :class="gradedSubjectiveCount === subjectiveCount ? 'bg-green-500' : 'bg-blue-500'" :style="{ width: subjectiveCount ? (gradedSubjectiveCount / subjectiveCount * 100) + '%' : '0%' }"></div>
            </div>
            <span class="font-bold" :class="gradedSubjectiveCount === subjectiveCount ? 'text-green-500' : 'text-blue-500'">{{ gradedSubjectiveCount }}/{{ subjectiveCount }}</span>
          </div>
        </div>
        <div class="w-px h-5 bg-gray-200 dark:bg-gray-600"></div>
        <div class="flex items-center gap-2">
          <span class="text-gray-400">主观题给分</span>
          <span class="text-lg font-bold text-blue-500">{{ subjectiveTotal }}</span>
          <span class="text-gray-400 text-xs">/ {{ subjectiveMaxTotal }}</span>
        </div>
        <div class="w-px h-5 bg-gray-200 dark:bg-gray-600"></div>
        <div class="flex items-center gap-2">
          <span class="text-gray-400">预估总分</span>
          <span class="text-lg font-bold" :class="(Number(detail.objectiveScore) || 0) + subjectiveTotal >= 60 ? 'text-green-500' : 'text-red-500'">{{ (Number(detail.objectiveScore) || 0) + subjectiveTotal }}</span>
        </div>
      </div>
      <!-- 纯客观题/空白卷：显示确认信息 -->
      <div v-else class="flex items-center gap-5 text-sm">
        <div class="flex items-center gap-2">
          <span class="text-gray-400">客观题得分</span>
          <span class="text-lg font-bold text-blue-500">{{ detail.objectiveScore || 0 }}</span>
        </div>
        <div class="w-px h-5 bg-gray-200 dark:bg-gray-600"></div>
        <div class="flex items-center gap-2 text-amber-500">
          <ArtSvgIcon icon="ri:error-warning-line" class="text-base" />
          <span class="text-sm">该试卷无主观题，确认后将直接完成批改</span>
        </div>
      </div>
      <el-button type="primary" size="large" @click="confirmSubmit" style="padding: 0 48px; font-weight: 600">{{ isGraded ? (hasSubjective ? '重新提交批改' : '重新确认批改') : (hasSubjective ? '提交批改' : '确认批改') }}</el-button>
    </div>

    <!-- 提交确认弹窗 -->
    <el-dialog v-model="showConfirmDialog" :title="isGraded ? '确认重新提交批改' : '确认提交批改'" width="480" :close-on-click-modal="false">
      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-3">
          <div class="p-4 rounded-xl bg-blue-50 dark:bg-blue-900/15 text-center">
            <div class="text-xs text-gray-400">客观题得分</div>
            <div class="text-2xl font-bold text-blue-500 mt-1.5">{{ detail.objectiveScore }}</div>
          </div>
          <div class="p-4 rounded-xl bg-amber-50 dark:bg-amber-900/15 text-center">
            <div class="text-xs text-gray-400">主观题给分</div>
            <div class="text-2xl font-bold text-amber-500 mt-1.5">{{ subjectiveTotal }}</div>
          </div>
        </div>
        <div class="p-4 rounded-xl bg-green-50 dark:bg-green-900/15 text-center">
          <div class="text-xs text-gray-400">总分</div>
          <div class="text-3xl font-bold text-green-500 mt-1.5">{{ (Number(detail.objectiveScore) || 0) + subjectiveTotal }}</div>
        </div>
        <div v-if="gradedSubjectiveCount < subjectiveCount" class="p-3.5 rounded-xl bg-red-50 dark:bg-red-900/10 flex items-center gap-2.5 text-sm text-red-600">
          <ArtSvgIcon icon="ri:error-warning-fill" class="text-lg shrink-0" />
          还有 {{ subjectiveCount - gradedSubjectiveCount }} 题未评分（将以 0 分计入）
        </div>
      </div>
      <template #footer>
        <el-button @click="showConfirmDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMarkingDetail, markScores } from '@/api/exam/marking'
import { formatDateTime as formatTime } from '@/utils/exam-format'

defineOptions({ name: 'MarkingDetail' })

const route = useRoute()
const router = useRouter()
const recordId = route.params.recordId as string
const detail = ref<any>({})
const scoreMap = ref<Record<number, number | undefined>>({})
const commentMap = ref<Record<number, string>>({})
const markingRefs = reactive<Record<number, any>>({})
const questionRefs = reactive<Record<number, any>>({})
const showConfirmDialog = ref(false)
const answerFilter = ref('all')
const collapsedSet = reactive<Record<number, boolean>>({})
const gradedSet = reactive<Record<number, boolean>>({})
const typeNames: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
const typeTagMap: Record<number, string> = { 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'danger' }

function isCorrectOption(opt: string, correctAnswer: string) {
  if (!correctAnswer) return false
  const letter = opt.charAt(0)
  return correctAnswer.split(',').map((s: string) => s.trim()).includes(letter)
}

function isStudentWrongOption(opt: string, studentAnswer: string, correctAnswer: string, isCorrect: number | null) {
  if ((isCorrect !== 0 && isCorrect !== 2) || !studentAnswer) return false
  const letter = opt.charAt(0)
  const studentLetters = studentAnswer.split(',').map((s: string) => s.trim())
  const correctLetters = correctAnswer.split(',').map((s: string) => s.trim())
  return studentLetters.includes(letter) && !correctLetters.includes(letter)
}

const hasSubjective = computed(() => (detail.value.answers || []).some((a: any) => a.needMarking))
const isPending = computed(() => detail.value.status === 2)
const isGraded = computed(() => detail.value.status === 3)
const canMark = computed(() => isPending.value || isGraded.value)
const answeredCount = computed(() => (detail.value.answers || []).filter((a: any) => a.studentAnswer).length)
const correctCount = computed(() => (detail.value.answers || []).filter((a: any) => a.isCorrect === 1).length)
const wrongCount = computed(() => (detail.value.answers || []).filter((a: any) => a.isCorrect === 0).length)
const partialCount = computed(() => (detail.value.answers || []).filter((a: any) => a.isCorrect === 2).length)
const unansweredCount = computed(() => (detail.value.answers || []).filter((a: any) => a.isCorrect === null && !a.needMarking).length)
const subjectiveCount = computed(() => (detail.value.answers || []).filter((a: any) => a.needMarking).length)
const totalQuestionCount = computed(() => (detail.value.answers || []).length)
const objectiveMaxScore = computed(() => (detail.value.answers || []).filter((a: any) => !a.needMarking).reduce((s: number, a: any) => s + (a.fullScore || 0), 0))
const totalMaxScore = computed(() => (detail.value.answers || []).reduce((s: number, a: any) => s + (a.fullScore || 0), 0))
const estimatedTotal = computed(() => (Number(detail.value.objectiveScore) || 0) + subjectiveTotal.value)

const filterTabs = computed(() => [
  { key: 'all', label: '全部', count: (detail.value.answers || []).length },
  { key: 'marking', label: '待批改', count: subjectiveCount.value },
  { key: 'correct', label: '正确', count: correctCount.value },
  { key: 'wrong', label: '错误', count: wrongCount.value },
  ...(partialCount.value > 0 ? [{ key: 'partial', label: '部分正确', count: partialCount.value }] : []),
  ...(unansweredCount.value > 0 ? [{ key: 'unanswered', label: '未作答', count: unansweredCount.value }] : [])
])

const answersWithIndex = computed(() => {
  return (detail.value.answers || []).map((a: any, i: number) => ({ ...a, originalIndex: i }))
})

const filteredAnswers = computed(() => {
  const all = answersWithIndex.value
  switch (answerFilter.value) {
    case 'marking': return all.filter((a: any) => a.needMarking)
    case 'correct': return all.filter((a: any) => a.isCorrect === 1)
    case 'wrong': return all.filter((a: any) => a.isCorrect === 0)
    case 'partial': return all.filter((a: any) => a.isCorrect === 2 && !a.needMarking)
    case 'unanswered': return all.filter((a: any) => a.isCorrect === null && !a.needMarking)
    default: return all
  }
})

function toggleCollapse(a: any) {
  if (a.needMarking) return
  collapsedSet[a.answerId] = !collapsedSet[a.answerId]
}

const groupedFilteredAnswers = computed(() => {
  const groups: Record<number, any[]> = {}
  for (const a of filteredAnswers.value) {
    if (!groups[a.questionType]) groups[a.questionType] = []
    groups[a.questionType].push(a)
  }
  const typeOrder = [1, 2, 3, 4, 5]
  return typeOrder
    .filter(t => groups[t])
    .map(t => ({
      type: t,
      typeName: typeNames[t],
      questions: groups[t],
      count: groups[t].length,
      subtotal: groups[t].reduce((sum: number, q: any) => sum + q.fullScore, 0)
    }))
})
const subjectiveAnswers = computed(() => {
  return (detail.value.answers || []).map((a: any, i: number) => ({ ...a, index: i })).filter((a: any) => a.needMarking)
})
const gradedSubjectiveCount = computed(() => {
  return (detail.value.answers || []).filter((a: any) => a.needMarking && gradedSet[a.answerId]).length
})
const subjectiveTotal = computed(() => {
  return (detail.value.answers || []).filter((a: any) => a.needMarking)
    .reduce((sum: number, a: any) => sum + (scoreMap.value[a.answerId] || 0), 0)
})
const subjectiveMaxTotal = computed(() => {
  return (detail.value.answers || []).filter((a: any) => a.needMarking)
    .reduce((sum: number, a: any) => sum + (a.fullScore || 0), 0)
})

function scrollToQuestion(answerId: number) {
  const el = markingRefs[answerId] || questionRefs[answerId]
  if (el) {
    (el as HTMLElement).scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

function scrollToNextUngraded() {
  const next = subjectiveAnswers.value.find((a: any) => !gradedSet[a.answerId])
  if (next) scrollToQuestion(next.answerId)
}

function confirmSubmit() {
  showConfirmDialog.value = true
}

const submitting = ref(false)
async function handleSubmit() {
  submitting.value = true
  try {
    const scores = (detail.value.answers || []).filter((a: any) => a.needMarking).map((a: any) => ({
      answerId: a.answerId,
      score: scoreMap.value[a.answerId] || 0,
      comment: commentMap.value[a.answerId] || ''
    }))
    await markScores({ recordId: Number(recordId), scores })
    showConfirmDialog.value = false
    ElMessage.success(isGraded.value ? '重新批改成功' : '批改成功')
    router.back()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const res = await getMarkingDetail(Number(recordId))
  detail.value = res
  for (const a of res.answers || []) {
    if (a.needMarking) {
      commentMap.value[a.answerId] = a.comment || ''
      if (a.score != null && a.score >= 0 && a.isCorrect !== null) {
        gradedSet[a.answerId] = true
        scoreMap.value[a.answerId] = a.score
      }
    } else {
      collapsedSet[a.answerId] = true
    }
  }
})
</script>

