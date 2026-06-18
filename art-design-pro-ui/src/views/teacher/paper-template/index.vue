<template>
  <div>
    <!-- 顶部搜索栏（独立卡片） -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3">
        <el-select v-model="querySubjectId" placeholder="全部科目" clearable style="width: 180px" @change="loadData">
          <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
        </el-select>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <ArtSvgIcon icon="ri:add-line" class="mr-1" />新建模板
      </ElButton>
    </div>

    <!-- 模板列表大框：tabs 头部 + 视图切换 + 内容区（与学生"我的考试"一致） -->
    <div class="art-card mb-5 max-sm:mb-4">
      <!-- 头部行：左侧伪 tab + 右侧视图切换 -->
      <div class="flex items-center justify-between px-5 border-b border-g-300">
        <div class="flex gap-0">
          <button class="relative px-5 py-3.5 text-sm font-medium text-blue-500 transition-colors">
            全部模板
            <span class="ml-1.5 px-1.5 py-0.5 text-[11px] rounded-full bg-blue-50 text-blue-500">{{ templates.length }}</span>
            <div class="absolute bottom-0 left-0 w-full h-0.5 bg-blue-500 rounded-t"></div>
          </button>
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

      <!-- 内容区 -->
      <div class="p-5">
        <el-empty v-if="!templates.length" description="暂无试卷模板，点击上方按钮创建" :image-size="100" />

        <!-- 卡片视图（紧凑版） -->
        <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          <div v-for="tpl in templates" :key="tpl.id"
            class="tpl-card art-card-sm overflow-hidden transition-all duration-300 hover:shadow-lg hover:-translate-y-1">
          <!-- 顶部：标签 + 操作菜单 + 标题 -->
          <div class="px-4 pt-3 pb-2.5 border-b border-gray-100 dark:border-gray-700">
            <div class="flex-cb mb-1.5">
              <el-tag size="small" effect="dark" round class="!border-0">{{ tpl.subjectName }}</el-tag>
              <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, tpl)">
                <div class="size-6 rounded-md flex-cc cursor-pointer text-g-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                  <ArtSvgIcon icon="ri:more-2-fill" class="text-sm" />
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit"><ArtSvgIcon icon="ri:edit-line" class="mr-1.5" />编辑</el-dropdown-item>
                    <el-dropdown-item command="apply"><ArtSvgIcon icon="ri:magic-line" class="mr-1.5" />套用组卷</el-dropdown-item>
                    <el-dropdown-item command="delete" divided><ArtSvgIcon icon="ri:delete-bin-line" class="mr-1.5 text-red-500" /><span class="text-red-500">删除</span></el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <h4 class="m-0 text-sm font-bold truncate leading-tight">{{ tpl.templateName }}</h4>
            <p v-if="tpl.description" class="m-0 mt-1 text-[11px] text-g-500 line-clamp-1">{{ tpl.description }}</p>
          </div>

          <!-- 核心数据 -->
          <div class="flex items-start justify-between px-4 py-2.5 gap-2">
            <div class="flex-1 rounded-md bg-gray-50 dark:bg-gray-800/50 px-2.5 py-1.5">
              <div class="text-base font-bold text-g-900">{{ tpl.questionCount }}</div>
              <div class="text-[10px] text-g-400 mt-0.5">题目数</div>
            </div>
            <div class="flex-1 rounded-md bg-blue-50 dark:bg-blue-900/20 px-2.5 py-1.5 text-center">
              <div class="text-base font-bold text-blue-500">{{ tpl.totalScore }}</div>
              <div class="text-[10px] text-blue-400 mt-0.5">总分</div>
            </div>
            <div class="flex-1 rounded-md bg-gray-50 dark:bg-gray-800/50 px-2.5 py-1.5 text-right">
              <div class="text-base font-bold text-g-900">{{ tpl.duration }}<span class="text-[10px] font-normal text-g-400">min</span></div>
              <div class="text-[10px] text-g-400 mt-0.5">时长</div>
            </div>
          </div>

          <!-- 题型分布 -->
          <div class="px-4 pb-0">
            <div class="border-t border-gray-100 dark:border-gray-700 pt-2">
              <div v-if="tpl.ruleDetails?.length" class="space-y-1.5">
                <div v-for="rule in tpl.ruleDetails" :key="rule.type" class="flex items-center gap-2">
                  <span class="text-[10px] w-9 shrink-0 text-right font-medium" :class="typeLabelClass[rule.type] || 'text-g-600'">{{ typeMap[rule.type] || '' }}</span>
                  <div class="flex-1 h-1.5 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div class="h-full rounded-full transition-all" :style="{ width: tpl.questionCount ? (rule.count / tpl.questionCount * 100) + '%' : '0%' }" :class="typeBarClass[rule.type] || 'bg-gray-400'"></div>
                  </div>
                  <span class="text-[10px] text-g-500 w-12 shrink-0 text-right">{{ rule.count }}题 {{ rule.score }}分</span>
                </div>
              </div>
              <div v-else class="text-[11px] text-g-400 text-center py-1.5">暂无规则</div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="px-4 py-2.5 mt-2 bg-gray-50/60 dark:bg-gray-800/30 border-t border-gray-100 dark:border-gray-700">
            <div class="flex-cb">
              <span class="text-[10px] text-g-400">{{ formatTime(tpl.updateTime) }}</span>
              <el-button type="primary" size="small" round @click="openApplyDialog(tpl)">
                <ArtSvgIcon icon="ri:magic-line" class="mr-1" />套用组卷
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 列表视图（与学生我的考试一致） -->
      <div v-else class="space-y-3">
        <div v-for="tpl in templates" :key="tpl.id" class="art-card-sm tpl-list-item">
          <!-- 左侧：图标 + 模板信息 -->
          <div class="flex items-center gap-3.5 flex-1 min-w-0">
            <div class="tpl-list-icon icon-primary">
              <ArtSvgIcon icon="ri:file-list-3-line" class="text-base" />
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="text-[13px] font-semibold text-gray-800 dark:text-gray-200 truncate">{{ tpl.templateName }}</h3>
                <el-tag size="small" effect="light" round>{{ tpl.subjectName }}</el-tag>
              </div>
              <div class="flex items-center gap-3 mt-1.5 text-xs text-gray-400 flex-wrap">
                <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:question-line" class="text-[11px]" />{{ tpl.questionCount }}题</span>
                <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:award-line" class="text-[11px]" />{{ tpl.totalScore }}分</span>
                <span class="flex items-center gap-1"><ArtSvgIcon icon="ri:timer-line" class="text-[11px]" />{{ tpl.duration }}分钟</span>
                <span v-if="tpl.typeSummary" class="hidden md:flex items-center gap-1"><ArtSvgIcon icon="ri:layout-masonry-line" class="text-[11px]" />{{ tpl.typeSummary }}</span>
                <span class="hidden lg:flex items-center gap-1"><ArtSvgIcon icon="ri:time-line" class="text-[11px]" />{{ formatTime(tpl.updateTime) }}</span>
              </div>
            </div>
          </div>
          <!-- 右侧：操作 -->
          <div class="flex items-center gap-2 flex-shrink-0 ml-4">
            <el-button type="primary" round size="small" @click="openApplyDialog(tpl)">
              <ArtSvgIcon icon="ri:magic-line" class="mr-1" />套用组卷
            </el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, tpl)">
              <div class="size-7 rounded-md flex-cc cursor-pointer text-g-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                <ArtSvgIcon icon="ri:more-2-fill" class="text-base" />
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit"><ArtSvgIcon icon="ri:edit-line" class="mr-1.5" />编辑</el-dropdown-item>
                  <el-dropdown-item command="delete" divided><ArtSvgIcon icon="ri:delete-bin-line" class="mr-1.5 text-red-500" /><span class="text-red-500">删除</span></el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
      </div>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模板' : '新建试卷模板'" width="650px" destroy-on-close>
      <el-form :model="form" label-width="85px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="如：计算机组成原理期末考模板" />
        </el-form-item>
        <el-form-item label="科目" required>
          <el-select v-model="form.subjectId" placeholder="请选择科目" style="width: 100%">
            <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="目标总分" required>
              <el-input-number v-model="form.targetScore" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分">
              <el-input-number v-model="form.passScore" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(分)">
              <el-input-number v-model="form.duration" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选，模板用途说明" />
        </el-form-item>
        <el-form-item label="题型规则" required>
          <div class="w-full">
            <!-- 规则表头 -->
            <div class="flex gap-2 text-xs text-g-400 mb-2 px-1">
              <span class="w-24">题型</span>
              <span class="w-20">数量</span>
              <span class="w-24">每题分值</span>
              <span class="w-24">难度</span>
              <span class="w-12 text-center">小计</span>
              <span class="w-8"></span>
            </div>
            <!-- 规则行 -->
            <div v-for="(rule, i) in form.rules" :key="i" class="flex items-center gap-2 mb-2">
              <el-select v-model="rule.questionType" size="small" style="width: 96px">
                <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="Number(k)" />
              </el-select>
              <el-input-number v-model="rule.questionCount" :min="1" size="small" style="width: 80px" />
              <el-input-number v-model="rule.scorePerQuestion" :min="0.5" :step="0.5" size="small" style="width: 96px" />
              <el-select v-model="rule.difficulty" placeholder="不限" clearable size="small" style="width: 96px">
                <el-option label="简单" :value="1" />
                <el-option label="中等" :value="2" />
                <el-option label="困难" :value="3" />
              </el-select>
              <span class="w-12 text-center text-xs font-medium text-g-700">{{ (rule.questionCount || 0) * (rule.scorePerQuestion || 0) }}分</span>
              <el-button type="danger" circle size="small" @click="form.rules.splice(i, 1)">
                <ArtSvgIcon icon="ri:delete-bin-line" />
              </el-button>
            </div>
            <el-button type="primary" link size="small" @click="addRule">
              <ArtSvgIcon icon="ri:add-line" class="mr-1" />添加题型规则
            </el-button>
            <!-- 总计 -->
            <div v-if="form.rules.length" class="flex items-center gap-3 flex-wrap mt-3 pt-3 border-t border-gray-200 dark:border-gray-600 text-sm">
              <span class="text-g-500">合计：</span>
              <el-tag round>{{ formTotalCount }} 题</el-tag>
              <el-tag :type="scoreDiffType" round>
                {{ formTotalScore }} / {{ form.targetScore || 0 }} 分
              </el-tag>
              <span v-if="form.targetScore && formTotalScore < form.targetScore" class="text-xs text-red-500">
                差 {{ form.targetScore - formTotalScore }} 分未达标
              </span>
              <span v-else-if="form.targetScore && formTotalScore > form.targetScore" class="text-xs text-amber-500">
                超出目标 {{ formTotalScore - form.targetScore }} 分
              </span>
              <span v-else-if="form.targetScore && formTotalScore === form.targetScore" class="text-xs text-green-500">
                ✓ 恰好达标
              </span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存模板</el-button>
      </template>
    </el-dialog>

    <!-- 套用组卷对话框 -->
    <el-dialog v-model="applyDialogVisible" title="使用模板组卷" width="500px" destroy-on-close>
      <div v-if="applyTemplate" class="space-y-4">
        <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4">
          <div class="flex items-center gap-2 mb-2">
            <ArtSvgIcon icon="ri:file-copy-2-line" class="text-blue-500" />
            <span class="font-bold text-sm">{{ applyTemplate.templateName }}</span>
          </div>
          <div class="text-xs text-g-500">{{ applyTemplate.typeSummary }}</div>
        </div>
        <el-form :model="applyForm" label-width="85px">
          <el-form-item label="试卷名称" required>
            <el-input v-model="applyForm.paperName" placeholder="请输入试卷名称" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="handleApply">
          <ArtSvgIcon icon="ri:magic-line" class="mr-1" />生成试卷
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTemplateList, getTemplateDetail, createTemplate, updateTemplate, deleteTemplate } from '@/api/exam/template'
import { randomPaper } from '@/api/exam/paper'
import { getAllSubjects } from '@/api/exam/subject'
import { questionTypeMap as typeMap } from '@/utils/exam-format'
import { buildSubjectLabelMap } from '@/utils/subject-label'

defineOptions({ name: 'PaperTemplate' })

const typeBarClass: Record<number, string> = {
  1: 'bg-blue-500', 2: 'bg-amber-500', 3: 'bg-green-500', 4: 'bg-purple-500', 5: 'bg-rose-500'
}
const typeLabelClass: Record<number, string> = {
  1: 'text-blue-500', 2: 'text-amber-500', 3: 'text-green-500', 4: 'text-purple-500', 5: 'text-rose-500'
}

const subjects = ref<any[]>([])
// 「按需维度消歧」科目下拉显示：仅在同名科目存在时才加专业/年级后缀。
// 例：zhangwenge 教多专业「思想道德与法治」时下拉显示「思想道德与法治（计算机科学与技术）」；
//     luweizhong 只教唯一「面向对象技术」时显示纯课程名，避免冗余。
// 算法详见 @/utils/subject-label.ts。
const subjectLabelMap = computed(() => buildSubjectLabelMap(subjects.value))
const templates = ref<any[]>([])
const querySubjectId = ref<number | null>(null)
const viewMode = ref<'card' | 'list'>('card')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const form = ref<any>({
  templateName: '', subjectId: null, targetScore: 100, passScore: 60, duration: 120, description: '',
  rules: [{ questionType: 1, questionCount: 10, scorePerQuestion: 2, difficulty: null }]
})

const formTotalCount = computed(() => form.value.rules.reduce((s: number, r: any) => s + (r.questionCount || 0), 0))
const formTotalScore = computed(() => form.value.rules.reduce((s: number, r: any) => s + (r.questionCount || 0) * (r.scorePerQuestion || 0), 0))
const scoreDiffType = computed(() => {
  if (!form.value.targetScore) return 'info'
  if (formTotalScore.value === form.value.targetScore) return 'success'
  if (formTotalScore.value < form.value.targetScore) return 'danger'
  return 'warning'
})

const applyDialogVisible = ref(false)
const applyTemplate = ref<any>(null)
const applyForm = ref({ paperName: '' })
const applying = ref(false)

function formatTime(dt: string) {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 16)
}

function addRule() {
  form.value.rules.push({ questionType: 1, questionCount: 5, scorePerQuestion: 2, difficulty: null })
}

async function loadData() {
  templates.value = await getTemplateList(querySubjectId.value ? { subjectId: querySubjectId.value } : undefined)
}

function openDialog(tpl?: any) {
  editingId.value = null
  form.value = {
    templateName: '', subjectId: null, targetScore: 100, passScore: 60, duration: 120, description: '',
    rules: [{ questionType: 1, questionCount: 10, scorePerQuestion: 2, difficulty: null }]
  }
  if (tpl) {
    editingId.value = tpl.id
    loadTemplateForEdit(tpl.id)
  }
  dialogVisible.value = true
}

async function loadTemplateForEdit(id: number) {
  const d = await getTemplateDetail(id)
  form.value = {
    templateName: d.templateName,
    subjectId: d.subjectId,
    targetScore: d.targetScore || 100,
    passScore: d.passScore,
    duration: d.duration,
    description: d.description || '',
    rules: d.rules.map((r: any) => ({
      questionType: r.questionType,
      questionCount: r.questionCount,
      scorePerQuestion: r.scorePerQuestion,
      difficulty: r.difficulty
    }))
  }
}

async function handleSave() {
  if (!form.value.templateName) { ElMessage.warning('请输入模板名称'); return }
  if (!form.value.subjectId) { ElMessage.warning('请选择科目'); return }
  if (!form.value.rules.length) { ElMessage.warning('请至少添加一条题型规则'); return }
  if (form.value.targetScore && formTotalScore.value !== form.value.targetScore) {
    ElMessage.warning(`题型规则合计 ${formTotalScore.value} 分，与目标总分 ${form.value.targetScore} 分不一致，请调整`)
    return
  }

  saving.value = true
  try {
    const payload = { ...form.value }
    if (editingId.value) {
      await updateTemplate(editingId.value, payload)
      ElMessage.success('模板更新成功')
    } else {
      await createTemplate(payload)
      ElMessage.success('模板创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

function handleCommand(cmd: string, tpl: any) {
  if (cmd === 'edit') openDialog(tpl)
  else if (cmd === 'delete') handleDelete(tpl)
  else if (cmd === 'apply') openApplyDialog(tpl)
}

async function handleDelete(tpl: any) {
  await ElMessageBox.confirm(`确定删除模板「${tpl.templateName}」？`, '提示', { type: 'warning' })
  await deleteTemplate(tpl.id)
  ElMessage.success('删除成功')
  loadData()
}

async function openApplyDialog(tpl: any) {
  applyTemplate.value = tpl
  applyForm.value = { paperName: '' }
  applyDialogVisible.value = true
  // 加载模板详情获取完整规则
  const detail = await getTemplateDetail(tpl.id)
  applyTemplate.value = { ...tpl, detail }
}

async function handleApply() {
  if (!applyForm.value.paperName) { ElMessage.warning('请输入试卷名称'); return }
  const detail = applyTemplate.value.detail
  if (!detail) { ElMessage.error('模板数据加载失败'); return }

  applying.value = true
  try {
    await randomPaper({
      paperName: applyForm.value.paperName,
      subjectId: detail.subjectId,
      passScore: detail.passScore,
      duration: detail.duration,
      rules: detail.rules.map((r: any) => ({
        questionType: r.questionType,
        count: r.questionCount,
        scorePerQuestion: r.scorePerQuestion,
        difficulty: r.difficulty
      }))
    })
    ElMessage.success('使用模板组卷成功！')
    applyDialogVisible.value = false
  } finally {
    applying.value = false
  }
}

onMounted(async () => {
  subjects.value = await getAllSubjects()
  loadData()
})
onActivated(() => { loadData() })
</script>

<style scoped>
/* 视图切换按钮（与学生我的考试一致） */
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

/* 列表视图行 */
.tpl-list-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  transition: all 0.25s ease;
}
.tpl-list-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

/* 列表图标 */
.tpl-list-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-primary { background: rgba(64, 158, 255, 0.1); color: var(--el-color-primary); }
</style>
