<template>
  <div class="art-full-height">
    <!-- 搜索栏 -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input v-model="query.examName" placeholder="搜索考试名称..." clearable style="width: 260px" @clear="loadData" @keyup.enter="loadData" />
        <el-select v-model="query.classId" placeholder="全部班级" clearable style="width: 180px" @change="loadData">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-select v-model="query.subjectId" placeholder="全部科目" clearable style="width: 180px" @change="loadData">
          <el-option v-for="s in filteredSubjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
          <el-option label="未开始" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已结束" :value="2" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <ElButton type="primary" @click="openDialog()"><ArtSvgIcon icon="ri:add-line" class="mr-1" />发布考试</ElButton>
    </div>

    <!-- 考试列表 -->
    <ElCard class="art-table-card" shadow="never" style="margin-top: 0">
      <template #header>
        <div class="flex-cb">
          <div class="flex items-center gap-3">
            <h4 class="m-0">考试列表</h4>
            <el-tag effect="light" round>共 {{ total }} 场</el-tag>
          </div>
          <div class="flex items-center gap-3">
            <div class="flex items-center gap-2 text-xs" v-if="examStats.total > 0">
              <span v-if="examStats.ongoing > 0" class="flex items-center gap-1"><span class="inline-block w-2 h-2 rounded-full bg-green-500"></span>进行中 {{ examStats.ongoing }}</span>
              <span v-if="examStats.pending > 0" class="flex items-center gap-1"><span class="inline-block w-2 h-2 rounded-full bg-blue-400"></span>未开始 {{ examStats.pending }}</span>
              <span v-if="examStats.ended > 0" class="flex items-center gap-1"><span class="inline-block w-2 h-2 rounded-full bg-gray-300"></span>已结束 {{ examStats.ended }}</span>
            </div>
            <div class="flex items-center gap-1 p-0.5 rounded-lg bg-gray-100/80 dark:bg-gray-800/60">
              <button class="view-toggle-btn" :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'" title="卡片视图">
                <ArtSvgIcon icon="ri:grid-fill" class="text-sm" />
              </button>
              <button class="view-toggle-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'" title="列表视图">
                <ArtSvgIcon icon="ri:list-check" class="text-sm" />
              </button>
            </div>
          </div>
        </div>
      </template>
      <div class="exam-list-body" style="flex: 1; overflow-y: auto">
        <el-empty v-if="tableData.length === 0" description="暂无考试数据" :image-size="100" />
        <!-- 卡片视图（紧凑网格，与列表视图同款外观） -->
        <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3 px-4 pt-3 pb-2">
          <div v-for="exam in tableData" :key="exam.id"
            class="exam-card-grid flex flex-col rounded-xl border border-gray-200 dark:border-gray-600 overflow-hidden hover:shadow-md transition-all">
            <div class="p-4 flex-1 space-y-2.5">
              <!-- 名称 + 状态 -->
              <div class="flex items-start justify-between gap-2">
                <h3 class="text-sm font-semibold text-gray-800 dark:text-gray-200 line-clamp-2 flex-1 leading-snug">{{ exam.examName }}</h3>
                <el-tag :type="exam.status === 1 ? 'success' : exam.status === 0 ? 'info' : 'danger'" size="small" round class="flex-shrink-0">{{ exam.statusName }}</el-tag>
              </div>
              <!-- 关键信息 -->
              <div class="space-y-1.5 text-xs text-gray-500">
                <div class="flex items-center gap-1.5"><ArtSvgIcon icon="ri:book-open-line" class="text-sm text-gray-400 flex-shrink-0" /><span class="truncate">{{ exam.paperName }}</span></div>
                <div class="flex items-center gap-1.5"><ArtSvgIcon icon="ri:building-line" class="text-sm text-gray-400 flex-shrink-0" /><span class="truncate">{{ exam.className }}</span></div>
                <div class="flex items-center gap-1.5"><ArtSvgIcon icon="ri:time-line" class="text-sm text-gray-400 flex-shrink-0" />{{ getExamDuration(exam) }}</div>
                <div class="flex items-center gap-1.5"><ArtSvgIcon icon="ri:calendar-line" class="text-sm text-gray-400 flex-shrink-0" /><span class="truncate text-[11px]">{{ formatTime(exam.startTime) }} ~ {{ formatTime(exam.endTime) }}</span></div>
              </div>
              <!-- 进度/倒计时 -->
              <template v-if="exam.status === 0">
                <div class="text-xs text-blue-500 font-medium pt-1"><ArtSvgIcon icon="ri:hourglass-line" class="text-sm mr-0.5 align-text-bottom" />{{ getCountdown(exam) }}</div>
              </template>
              <div v-else class="pt-1">
                <el-progress :percentage="exam.totalStudents ? Math.round(exam.submittedCount / exam.totalStudents * 100) : 0"
                  :stroke-width="5" :show-text="false"
                  :color="exam.submittedCount === exam.totalStudents ? '#67c23a' : '#409eff'" />
                <div class="flex items-center justify-between mt-1.5 text-[11px] text-gray-500">
                  <span>已交卷 <b class="text-blue-500">{{ exam.submittedCount }}</b>/{{ exam.totalStudents }}</span>
                  <span v-if="exam.absentCount > 0" class="text-red-500 font-medium">缺考 {{ exam.absentCount }}</span>
                </div>
              </div>
            </div>
            <!-- 底部操作 -->
            <div class="flex items-center justify-end gap-1.5 px-3 py-2 border-t border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/30">
              <el-button v-if="exam.status === 0" size="small" type="primary" plain @click="openEditDialog(exam)">
                <ArtSvgIcon icon="ri:edit-line" class="mr-0.5" />编辑
              </el-button>
              <el-button size="small" plain @click="viewRecords(exam)">
                <ArtSvgIcon icon="ri:file-list-3-line" class="mr-0.5" />记录
              </el-button>
              <el-button v-if="exam.status === 0" size="small" type="danger" plain @click="handleDelete(exam)">
                <ArtSvgIcon icon="ri:delete-bin-line" class="mr-0.5" />删除
              </el-button>
            </div>
          </div>
        </div>
        <!-- 列表视图（横向条形，原样保留） -->
        <div v-else class="space-y-3 px-4 pt-3 pb-2">
          <div v-for="exam in tableData" :key="exam.id"
            class="exam-card flex items-stretch rounded-xl border border-gray-200 dark:border-gray-600 overflow-hidden hover:shadow-md transition-all">
            <!-- 主内容区 -->
            <div class="flex-1 p-4 min-w-0">
              <!-- 第一行：考试名称 + 状态标签 -->
              <div class="flex items-center gap-2.5 mb-2">
                <span class="text-sm font-semibold text-gray-800 dark:text-gray-200 truncate">{{ exam.examName }}</span>
                <el-tag :type="exam.status === 1 ? 'success' : exam.status === 0 ? 'info' : 'danger'" size="small" round>{{ exam.statusName }}</el-tag>
              </div>
              <!-- 第二行：关键信息 -->
              <div class="flex flex-wrap items-center gap-x-5 gap-y-1 text-xs text-gray-500">
                <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:book-open-line" class="text-sm text-gray-400" />{{ exam.paperName }}</span>
                <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:building-line" class="text-sm text-gray-400" />{{ exam.className }}</span>
                <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:time-line" class="text-sm text-gray-400" />{{ getExamDuration(exam) }}</span>
                <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:calendar-line" class="text-sm text-gray-400" />{{ formatTime(exam.startTime) }} ~ {{ formatTime(exam.endTime) }}</span>
              </div>
              <!-- 第三行：进度条 + 统计 -->
              <div class="flex items-center gap-3 mt-2.5">
                <template v-if="exam.status === 0">
                  <span class="text-xs text-blue-500 font-medium"><ArtSvgIcon icon="ri:hourglass-line" class="text-sm mr-0.5 align-text-bottom" />{{ getCountdown(exam) }}</span>
                </template>
                <template v-else>
                  <el-progress :percentage="exam.totalStudents ? Math.round(exam.submittedCount / exam.totalStudents * 100) : 0"
                    :stroke-width="6" :show-text="false" class="flex-1 max-w-48"
                    :color="exam.submittedCount === exam.totalStudents ? '#67c23a' : '#409eff'" />
                  <span class="text-xs text-gray-500">已交卷 <b class="text-blue-500">{{ exam.submittedCount }}</b>/{{ exam.totalStudents }}</span>
                  <span v-if="exam.absentCount > 0" class="text-xs text-red-500 font-medium">缺考 {{ exam.absentCount }}</span>
                </template>
              </div>
            </div>
            <!-- 右侧操作区 -->
            <div class="shrink-0 flex flex-row flex-nowrap items-center gap-1.5 px-4 border-l border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/30">
              <el-button v-if="exam.status === 0" size="small" type="primary" plain @click="openEditDialog(exam)">
                <ArtSvgIcon icon="ri:edit-line" class="mr-1" />编辑
              </el-button>
              <el-button size="small" plain @click="viewRecords(exam)">
                <ArtSvgIcon icon="ri:file-list-3-line" class="mr-1" />记录
              </el-button>
              <el-button v-if="exam.status === 0" size="small" type="danger" plain @click="handleDelete(exam)">
                <ArtSvgIcon icon="ri:delete-bin-line" class="mr-1" />删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <div class="exam-pagination" v-if="total > 0">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" background @change="loadData" />
      </div>
    </ElCard>

    <!-- 发布/编辑考试对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑考试' : '发布考试'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="考试名称" prop="examName"><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="试卷" prop="paperId">
          <el-select v-model="form.paperId" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="p.paperName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingId" label="班级" prop="classId">
          <el-select v-model="form.classId" style="width: 100%">
            <el-option v-for="c in filteredDialogClasses" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="班级" prop="classIds">
          <el-select v-model="form.classIds" multiple style="width: 100%" placeholder="可选择多个班级">
            <el-option v-for="c in filteredDialogClasses" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>

        <!-- 防作弊设置 -->
        <div class="ac-panel">
          <div class="ac-panel-header">
            <ArtSvgIcon icon="ri:shield-check-line" style="font-size: 15px" />
            <span>防作弊设置</span>
          </div>
          <div class="ac-grid">
            <!-- 切屏限制 -->
            <div class="ac-card" :class="{ 'ac-card--active': antiCheatEnabled.switchScreen }">
              <div class="ac-card-top">
                <div class="ac-card-info">
                  <ArtSvgIcon icon="ri:swap-line" class="ac-card-icon" />
                  <span class="ac-card-title">切屏限制</span>
                </div>
                <el-switch v-model="antiCheatEnabled.switchScreen" size="small" />
              </div>
              <div class="ac-card-desc">超过限定次数强制交卷</div>
              <div v-if="antiCheatEnabled.switchScreen" class="ac-card-extra">
                <el-input-number v-model="form.antiCheat.switchScreenMax" :min="1" :max="20" size="small" controls-position="right" style="width: 90px" />
                <span class="ac-card-unit">次</span>
              </div>
            </div>
            <!-- 全屏作答 -->
            <div class="ac-card" :class="{ 'ac-card--active': form.antiCheat.fullscreenRequired }">
              <div class="ac-card-top">
                <div class="ac-card-info">
                  <ArtSvgIcon icon="ri:fullscreen-line" class="ac-card-icon" />
                  <span class="ac-card-title">全屏作答</span>
                </div>
                <el-switch v-model="form.antiCheat.fullscreenRequired" size="small" />
              </div>
              <div class="ac-card-desc">推荐全屏模式，提升专注度</div>
            </div>
            <!-- 禁止复制 -->
            <div class="ac-card" :class="{ 'ac-card--active': form.antiCheat.noCopyPaste }">
              <div class="ac-card-top">
                <div class="ac-card-info">
                  <ArtSvgIcon icon="ri:forbid-line" class="ac-card-icon" />
                  <span class="ac-card-title">禁止复制</span>
                </div>
                <el-switch v-model="form.antiCheat.noCopyPaste" size="small" />
              </div>
              <div class="ac-card-desc">禁止复制、粘贴、右键</div>
            </div>
            <!-- 题目乱序 -->
            <div class="ac-card" :class="{ 'ac-card--active': form.antiCheat.shuffleQuestion }">
              <div class="ac-card-top">
                <div class="ac-card-info">
                  <ArtSvgIcon icon="ri:shuffle-line" class="ac-card-icon" />
                  <span class="ac-card-title">题目乱序</span>
                </div>
                <el-switch v-model="form.antiCheat.shuffleQuestion" size="small" />
              </div>
              <div class="ac-card-desc">每人题目顺序不同</div>
            </div>
            <!-- 选项乱序 -->
            <div class="ac-card" :class="{ 'ac-card--active': form.antiCheat.shuffleOption }">
              <div class="ac-card-top">
                <div class="ac-card-info">
                  <ArtSvgIcon icon="ri:list-unordered" class="ac-card-icon" />
                  <span class="ac-card-title">选项乱序</span>
                </div>
                <el-switch v-model="form.antiCheat.shuffleOption" size="small" />
              </div>
              <div class="ac-card-desc">选择题选项随机排列</div>
            </div>
            <!-- 无操作超时 -->
            <div class="ac-card" :class="{ 'ac-card--active': antiCheatEnabled.inactivity }">
              <div class="ac-card-top">
                <div class="ac-card-info">
                  <ArtSvgIcon icon="ri:timer-line" class="ac-card-icon" />
                  <span class="ac-card-title">无操作超时</span>
                </div>
                <el-switch v-model="antiCheatEnabled.inactivity" size="small" />
              </div>
              <div class="ac-card-desc">长时间无操作自动交卷</div>
              <div v-if="antiCheatEnabled.inactivity" class="ac-card-extra">
                <el-input-number v-model="form.antiCheat.inactivityTimeout" :min="1" :max="60" size="small" controls-position="right" style="width: 90px" />
                <span class="ac-card-unit">分钟</span>
              </div>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ editingId ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>

    <!-- 考试记录对话框 -->
    <el-dialog v-model="recordDialogVisible" :title="currentExamName + ' — 考试记录'" width="900px" class="record-dialog">
      <!-- 统计面板 -->
      <div class="rs-panel">
        <div class="rs-group">
          <div class="rs-group-title">参与情况</div>
          <div class="rs-items">
            <div class="rs-item">
              <div class="rs-val">{{ records.length }}</div>
              <div class="rs-key">总人数</div>
            </div>
            <div v-if="recordStats.submitted > 0" class="rs-item">
              <div class="rs-val rs-val--primary">{{ recordStats.submitted }}</div>
              <div class="rs-key">已交卷</div>
            </div>
            <div v-if="recordStats.graded > 0" class="rs-item">
              <div class="rs-val rs-val--success">{{ recordStats.graded }}</div>
              <div class="rs-key">已批改</div>
            </div>
            <div v-if="recordStats.inProgress > 0" class="rs-item">
              <div class="rs-val rs-val--warning">{{ recordStats.inProgress }}</div>
              <div class="rs-key">答题中</div>
            </div>
            <div v-if="recordStats.absent > 0" class="rs-item">
              <div class="rs-val rs-val--danger">{{ recordStats.absent }}</div>
              <div class="rs-key">缺考</div>
            </div>
            <div v-if="recordStats.notEntered > 0" class="rs-item">
              <div class="rs-val rs-val--muted">{{ recordStats.notEntered }}</div>
              <div class="rs-key">未进入</div>
            </div>
          </div>
        </div>
        <div v-if="recordStats.graded > 0" class="rs-group">
          <div class="rs-group-title">成绩概览</div>
          <div class="rs-items">
            <div class="rs-item">
              <div class="rs-val rs-val--primary">{{ recordStats.avgScore }}</div>
              <div class="rs-key">平均分</div>
            </div>
            <div class="rs-item">
              <div class="rs-val rs-val--success">{{ recordStats.maxScore }}</div>
              <div class="rs-key">最高分</div>
            </div>
            <div class="rs-item">
              <div class="rs-val rs-val--danger">{{ recordStats.minScore }}</div>
              <div class="rs-key">最低分</div>
            </div>
          </div>
        </div>
      </div>
      <el-table :data="sortedRecords" size="small" style="width: 100%" max-height="420" :row-class-name="recordRowClass" class="record-table">
        <el-table-column prop="realName" label="姓名" min-width="90" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium">{{ row.realName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs">{{ row.startTime ? formatTime(row.startTime) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="交卷时间" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-xs">{{ row.submitTime ? formatTime(row.submitTime) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用时" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.startTime && row.submitTime" class="text-xs text-gray-500">{{ calcDuration(row.startTime, row.submitTime) }}</span>
            <template v-else>—</template>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="65" align="center">
          <template #default="{ row }">
            <b v-if="row.totalScore != null" class="text-sm" :style="{ color: row.totalScore >= 60 ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">{{ row.totalScore }}</b>
            <template v-else><span class="text-gray-300">—</span></template>
          </template>
        </el-table-column>
        <el-table-column label="客观" width="55" align="center">
          <template #default="{ row }">
            <span class="text-xs">{{ row.objectiveScore != null ? row.objectiveScore : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="主观" width="55" align="center">
          <template #default="{ row }">
            <span class="text-xs">{{ row.subjectiveScore != null ? row.subjectiveScore : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="切屏" width="50" align="center">
          <template #default="{ row }">
            <span v-if="row.switchCount > 0" class="record-switch-badge">{{ row.switchCount }}</span>
            <span v-else class="text-xs text-gray-300">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span class="record-status" :class="'record-status--' + row.status">{{ row.statusName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 3 || row.status === 2" link type="primary" size="small" @click="goMarkingDetail(row)">查看</el-button>
            <span v-else class="text-gray-300">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated, onDeactivated, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getExamList, publishExam, updateExam, deleteExam, getExamRecords } from '@/api/exam/exam'
import { getPaperList } from '@/api/exam/paper'
import { getMyClasses } from '@/api/exam/class'
import { getAllSubjects } from '@/api/exam/subject'
import { formatDateTime as formatTime } from '@/utils/exam-format'
import { buildSubjectLabelMap } from '@/utils/subject-label'
import { mittBus } from '@/utils/sys'

defineOptions({ name: 'ExamManage' })

const router = useRouter()
const query = ref<any>({ page: 1, size: 10, examName: '', classId: null, subjectId: null })
const statusFilter = ref<number | null>(null)
const tableData = ref<any[]>([])
const allExams = ref<any[]>([])
const total = ref(0)
const viewMode = ref<'card' | 'list'>('list')
const dialogVisible = ref(false)
const recordDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const papers = ref<any[]>([])
const classes = ref<any[]>([])
const subjects = ref<any[]>([])
const records = ref<any[]>([])
const currentExamName = ref('')
const editingId = ref<number | null>(null)
const form = ref<any>({ examName: '', paperId: null, classId: null, classIds: [], startTime: '', endTime: '', antiCheat: { switchScreenMax: 3, shuffleQuestion: false, shuffleOption: false, fullscreenRequired: false, noCopyPaste: false, inactivityTimeout: 10 } })
const antiCheatEnabled = reactive({ switchScreen: false, inactivity: false })

const examStats = computed(() => {
  const data = allExams.value
  return {
    total: data.length,
    pending: data.filter(e => e.status === 0).length,
    ongoing: data.filter(e => e.status === 1).length,
    ended: data.filter(e => e.status === 2).length
  }
})

// 班级筛选时联动过滤科目下拉：仅显示该班级所属专业开设且当前教师任课的科目。
// 设计参考：超星泛雅 / 正方教务 / Banner ERP 的「按班级开课计划过滤」模式（国内教育产品标准）。
// 数据链路：edu_class.major_id → subject_major(多对多) → edu_subject，
// 由后端 getTeacherClasses 返回 majorId、getAllSubjectsByTeacher 返回 majorIds 共同支撑。
const filteredSubjects = computed(() => {
  if (!query.value.classId) return subjects.value  // 未选班级 → 显示全部任课科目
  const cls = classes.value.find((c: any) => c.id === query.value.classId)
  // 兜底：班级数据无 majorId（如旧数据未迁移）→ 显示全部，绝不隐藏数据
  if (!cls?.majorId) return subjects.value
  return subjects.value.filter((s: any) =>
    Array.isArray(s.majorIds) && s.majorIds.includes(cls.majorId)
  )
})

// 「按需维度消歧」科目下拉显示：仅在 filteredSubjects 中存在同名科目时才加专业/年级后缀。
// 例：zhangwenge 教多专业「思想道德与法治」时显示「思想道德与法治（计算机科学与技术）」；
//     luweizhong 只教唯一「面向对象技术」时显示纯课程名，避免冗余噪音。
// 算法详见 @/utils/subject-label.ts。
const subjectLabelMap = computed(() => buildSubjectLabelMap(filteredSubjects.value))

// 切换班级时，若当前已选 subjectId 不在新的过滤后列表中，则自动清空，
// 避免出现「已选了但下拉里看不到」的不一致状态，并触发重新加载考试列表。
watch(() => query.value.classId, () => {
  if (query.value.subjectId &&
      !filteredSubjects.value.find((s: any) => s.id === query.value.subjectId)) {
    query.value.subjectId = null
    loadData()
  }
})

// 发布考试 dialog：班级下拉根据所选试卷的科目所属专业联动过滤。
// 业务约束：试卷绑定科目（exam_paper.subject_id），科目通过 subject_major 关联多个专业，
// 班级隐属单一专业（edu_class.major_id）。仅当班级专业 ∈ 科目专业集合时才允许发布。
// 设计参考：OWASP 「Defense in Depth」 + Canvas LMS / 超星泛雅「作业发布范围」。
const filteredDialogClasses = computed(() => {
  if (!form.value.paperId) return classes.value
  const paper = papers.value.find((p: any) => p.id === form.value.paperId)
  if (!paper?.subjectId) return classes.value  // 兜底：试卷无科目 → 不限制
  const subject = subjects.value.find((s: any) => s.id === paper.subjectId)
  if (!subject?.majorIds || !Array.isArray(subject.majorIds) || subject.majorIds.length === 0) {
    return classes.value  // 兜底：科目无专业关联（旧数据未迁移）→ 不限制
  }
  return classes.value.filter((c: any) =>
    c.id === form.value.classId ||                      // 编辑模式保留原 classId 对应班级，避免下拉显示成 id 而非 className
    !c.majorId ||                                       // 班级无 majorId 时兜底显示
    subject.majorIds.includes(c.majorId)                // 专业匹配
  )
})

// 切换试卷时，移除已选但不属于该试卷专业的班级，避免提交后被后端兑底校验拒绝。
// 关键 guard：仅在 dialog 已打开（用户主动交互）时清空。
// openEditDialog 流程为「先设 form.value（同步触发本 watch）→ 再设 dialogVisible=true」，
// 若不加 guard，会在打开编辑对话框的瞬间误清空历史 classId（即使用户只想改时间/名称），
// 破坏原始数据加载体验。dialog 关闭状态下的 watch 触发一律视作「初始化加载」直接跳过。
watch(() => form.value.paperId, (newId, oldId) => {
  if (newId === oldId) return
  if (!dialogVisible.value) return
  const validIds = new Set(filteredDialogClasses.value.map((c: any) => c.id))
  // 编辑模式（单值 classId）
  if (form.value.classId && !validIds.has(form.value.classId)) {
    form.value.classId = null
  }
  // 发布模式（多值 classIds）
  if (Array.isArray(form.value.classIds) && form.value.classIds.length > 0) {
    form.value.classIds = form.value.classIds.filter((id: any) => validIds.has(id))
  }
})


const rules = {
  examName: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  classIds: [{ type: 'array' as const, required: true, min: 1, message: '请至少选择一个班级', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

async function loadData() {
  const params: any = { ...query.value }
  if (statusFilter.value !== null) params.status = statusFilter.value
  const res = await getExamList(params)
  tableData.value = res.records
  total.value = res.total
  if (allExams.value.length === 0) {
    const allRes = await getExamList({ page: 1, size: 200 })
    allExams.value = allRes.records
  }
}

function getDefaultAntiCheat() {
  return { switchScreenMax: 3, shuffleQuestion: false, shuffleOption: false, fullscreenRequired: false, noCopyPaste: false, inactivityTimeout: 10 }
}

function openDialog() {
  editingId.value = null
  form.value = { examName: '', paperId: null, classId: null, classIds: [], startTime: '', endTime: '', antiCheat: getDefaultAntiCheat() }
  antiCheatEnabled.switchScreen = false
  antiCheatEnabled.inactivity = false
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  editingId.value = row.id
  const ac = row.antiCheat ? { ...getDefaultAntiCheat(), ...row.antiCheat } : getDefaultAntiCheat()
  form.value = { examName: row.examName, paperId: row.paperId, classId: row.classId, startTime: row.startTime, endTime: row.endTime, antiCheat: ac }
  antiCheatEnabled.switchScreen = ac.switchScreenMax > 0
  antiCheatEnabled.inactivity = ac.inactivityTimeout > 0
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  // 根据开关状态处理防作弊数值
  const submitData = { ...form.value }
  submitData.antiCheat = { ...form.value.antiCheat }
  if (!antiCheatEnabled.switchScreen) submitData.antiCheat.switchScreenMax = 0
  if (!antiCheatEnabled.inactivity) submitData.antiCheat.inactivityTimeout = 0
  if (editingId.value) {
    await updateExam(editingId.value, submitData)
    ElMessage.success('更新成功')
  } else {
    await publishExam(submitData)
    ElMessage.success(`成功发布到 ${submitData.classIds.length} 个班级`)
  }
  dialogVisible.value = false
  allExams.value = []
  loadData()
  mittBus.emit('refreshNotification')
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该考试？', '提示', { type: 'warning' })
  await deleteExam(row.id)
  ElMessage.success('删除成功')
  allExams.value = []
  loadData()
  mittBus.emit('refreshNotification')
}

const recordStats = computed(() => {
  const data = records.value
  const graded = data.filter((r: any) => r.status === 3)
  const scores = graded.map((r: any) => Number(r.totalScore) || 0)
  return {
    notEntered: data.filter((r: any) => r.status === -1).length,
    inProgress: data.filter((r: any) => r.status === 1).length,
    submitted: data.filter((r: any) => r.status === 2).length,
    graded: graded.length,
    absent: data.filter((r: any) => r.status === 4).length,
    avgScore: scores.length ? (scores.reduce((a: number, b: number) => a + b, 0) / scores.length).toFixed(1) : '-',
    maxScore: scores.length ? Math.max(...scores) : '-',
    minScore: scores.length ? Math.min(...scores) : '-'
  }
})

const sortedRecords = computed(() => {
  const order: Record<number, number> = { 1: 0, 2: 1, 3: 2, 4: 3, '-1': 4, 0: 5 }
  return [...records.value].sort((a: any, b: any) => {
    const oa = order[a.status] ?? 9, ob = order[b.status] ?? 9
    if (oa !== ob) return oa - ob
    if (a.totalScore != null && b.totalScore != null) return Number(b.totalScore) - Number(a.totalScore)
    return 0
  })
})

function getExamDuration(exam: any) {
  const ms = new Date(exam.endTime).getTime() - new Date(exam.startTime).getTime()
  if (ms <= 0) return '-'
  const mins = Math.round(ms / 60000)
  return mins >= 60 ? `${Math.floor(mins / 60)}小时${mins % 60 > 0 ? mins % 60 + '分钟' : ''}` : `${mins}分钟`
}

function getCountdown(exam: any) {
  const diff = new Date(exam.startTime).getTime() - Date.now()
  if (diff <= 0) return '即将开考'
  const days = Math.floor(diff / 86400000)
  const hours = Math.floor((diff % 86400000) / 3600000)
  const mins = Math.floor((diff % 3600000) / 60000)
  if (days > 0) return `距开考 ${days}天${hours}小时`
  if (hours > 0) return `距开考 ${hours}小时${mins}分钟`
  return `距开考 ${mins}分钟`
}

function recordRowClass({ row }: { row: any }) {
  if (row.status === -1) return 'record-row-muted'
  if (row.status === 4) return 'record-row-absent'
  return ''
}

function calcDuration(start: string, end: string) {
  const ms = new Date(end).getTime() - new Date(start).getTime()
  if (ms <= 0) return '-'
  const mins = Math.floor(ms / 60000)
  const secs = Math.floor((ms % 60000) / 1000)
  return mins > 0 ? `${mins}分${secs}秒` : `${secs}秒`
}

function goMarkingDetail(row: any) {
  if (row.id) {
    recordDialogVisible.value = false
    router.push(`/exam-center/marking/${row.id}`)
  }
}

async function viewRecords(row: any) {
  currentExamName.value = row.examName || '考试'
  records.value = await getExamRecords(row.id)
  recordDialogVisible.value = true
}

// --- 实时状态同步机制 ---
let pollTimer: ReturnType<typeof setInterval> | null = null
let statusTimer: ReturnType<typeof setInterval> | null = null

// 本地定时器：每秒重新计算考试状态（未开始/进行中/已结束），无需等待后端轮询
function recalcStatus() {
  const now = new Date()
  const statusNames = ['未开始', '进行中', '已结束']
  for (const exam of tableData.value) {
    const start = new Date(exam.startTime)
    const end = new Date(exam.endTime)
    let s = 0
    if (now >= start && now <= end) s = 1
    else if (now > end) s = 2
    exam.status = s
    exam.statusName = statusNames[s]
  }
}
function startStatusTimer() { stopStatusTimer(); statusTimer = setInterval(recalcStatus, 1000) }
function stopStatusTimer() { if (statusTimer) { clearInterval(statusTimer); statusTimer = null } }

// 后端轮询（15秒兆底）
function startPolling() { stopPolling(); pollTimer = setInterval(() => { allExams.value = []; loadData() }, 15000) }
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

// WebSocket 考试事件监听：学生交卷时即时刷新
function onExamEvent(_event: any) {
  allExams.value = []
  loadData()
}

onMounted(async () => {
  const [pRes, cRes, sRes] = await Promise.all([getPaperList({ page: 1, size: 200, status: 1 }), getMyClasses(), getAllSubjects()])
  papers.value = pRes.records
  classes.value = cRes
  subjects.value = sRes
  await loadData()
  startPolling()
  startStatusTimer()
  mittBus.on('examEvent', onExamEvent)
})
onActivated(async () => {
  const pRes = await getPaperList({ page: 1, size: 200, status: 1 })
  papers.value = pRes.records
  allExams.value = []
  await loadData()
  startPolling()
  startStatusTimer()
})
onDeactivated(() => { stopPolling(); stopStatusTimer() })
onBeforeUnmount(() => { stopPolling(); stopStatusTimer(); mittBus.off('examEvent', onExamEvent) })
</script>

<style lang="scss" scoped>
/* 视图切换按钮（与 my-exam 完全一致） */
.view-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 28px;
  border-radius: 6px;
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  background: transparent;
}
.view-toggle-btn:hover { color: var(--el-text-color-regular); }
.view-toggle-btn.active {
  background: var(--default-box-color, #fff);
  color: var(--el-color-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.art-table-card {
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

.exam-list-body {
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background-color: var(--el-border-color-light); border-radius: 3px; &:hover { background-color: var(--el-border-color); } }
  &::-webkit-scrollbar-track { background-color: transparent; }
}

:deep(.record-row-muted) {
  opacity: 0.55;
}
:deep(.record-row-absent) {
  .el-table__cell { color: var(--el-color-danger); }
}

// 考试记录统计面板
.rs-panel {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.rs-group {
  flex: 1;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px 16px;
}
.rs-group-title {
  font-size: 11px;
  font-weight: 500;
  color: var(--el-text-color-placeholder);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}
.rs-items {
  display: flex;
  gap: 20px;
}
.rs-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  min-width: 40px;
}
.rs-val {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
  &--primary { color: var(--el-color-primary); }
  &--success { color: var(--el-color-success); }
  &--warning { color: var(--el-color-warning); }
  &--danger { color: var(--el-color-danger); }
  &--muted { color: var(--el-text-color-placeholder); }
}
.rs-key {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

// 切屏徽章
.record-switch-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 10px;
  background: #fef2f2;
  color: #ef4444;
  font-size: 12px;
  font-weight: 600;
}

// 状态文字
.record-status {
  font-size: 12px;
  font-weight: 500;
  &--\-1 { color: var(--el-text-color-placeholder); }
  &--0 { color: var(--el-text-color-placeholder); }
  &--1 { color: var(--el-color-warning); }
  &--2 { color: var(--el-color-primary); }
  &--3 { color: var(--el-color-success); }
  &--4 { color: var(--el-color-danger); }
}

// 记录表格微调
.record-table {
  :deep(th.el-table__cell) {
    font-size: 12px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-lighter) !important;
  }
}

// 防作弊设置面板
.ac-panel {
  margin: 4px 0 0;
  padding: 0;
}
.ac-panel-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  padding: 0 0 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
  .art-icon { color: var(--el-color-primary); }
}
.ac-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.ac-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px 12px;
  background: var(--el-fill-color-blank);
  transition: all 0.2s ease;
  &:hover { border-color: var(--el-border-color); }
  &--active {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }
}
.ac-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.ac-card-info {
  display: flex;
  align-items: center;
  gap: 6px;
}
.ac-card-icon {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  .ac-card--active & { color: var(--el-color-primary); }
}
.ac-card-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.ac-card-desc {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  line-height: 1.4;
  padding-left: 20px;
}
.ac-card-extra {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding-left: 20px;
}
.ac-card-unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

:deep(.exam-pagination) {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px 8px;
  .el-pagination {
    .btn-prev, .btn-next {
      background-color: transparent; border: 1px solid var(--el-border-color-light); border-radius: 6px; transition: border-color 0.15s;
      &:hover:not(.is-disabled) { color: var(--theme-color); border-color: var(--theme-color); }
    }
    li {
      box-sizing: border-box; font-weight: 400 !important; background-color: transparent; border: 1px solid var(--el-border-color-light); border-radius: 6px; transition: border-color 0.15s;
      &.is-active { font-weight: 500 !important; color: #fff; background-color: var(--theme-color); border-color: var(--theme-color); }
      &:hover:not(.is-disabled):not(.is-active) { border-color: var(--theme-color); }
    }
  }
}
</style>
