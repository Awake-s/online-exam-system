<template>
  <div>
    <!-- 搜索栏 -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3 flex-wrap">
        <el-select v-model="query.subjectId" placeholder="科目" clearable style="width: 150px" @change="loadData">
          <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
        </el-select>
        <el-select v-model="query.questionType" placeholder="题型" clearable style="width: 120px" @change="loadData">
          <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="Number(k)" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 100px" @change="loadData">
          <el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="关键词" clearable style="width: 150px" @clear="loadData" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <div class="flex gap-2">
        <ElButton type="primary" @click="openDialog()"><ArtSvgIcon icon="ri:add-line" class="mr-1" />添加题目</ElButton>
        <ElButton @click="importDialogVisible = true"><ArtSvgIcon icon="ri:upload-line" class="mr-1" />导入题目</ElButton>
      </div>
    </div>

    <!-- 表格 -->
    <ElCard class="art-table-card" shadow="never" style="margin-top: 0; margin-bottom: 20px">
      <template #header>
        <div class="flex-cb">
          <h4 class="m-0">题目列表</h4>
          <div class="flex items-center gap-3">
            <ElButton v-if="selectedRows.length > 0" @click="handleBatchDelete" v-ripple>
              <ElIcon><Delete /></ElIcon>
              批量删除 ({{ selectedRows.length }})
            </ElButton>
            <el-tag effect="light">共 {{ total }} 题</el-tag>
          </div>
        </div>
      </template>
      <div class="table-wrap">
      <el-table ref="tableRef" :data="tableData" row-key="id" style="width: 100%" :header-cell-style="headerStyle" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-detail">
              <!-- 题目头部：标签行 -->
              <div class="expand-meta">
                <el-tag :type="(typeTagMap[row.questionType] as any)" size="small" round>{{ row.questionTypeName }}</el-tag>
                <span class="expand-meta-dot"></span>
                <span class="flex items-center gap-1 text-xs text-amber-500 font-medium">
                  <ArtSvgIcon icon="ri:star-fill" class="text-xs" />{{ row.score }}分
                </span>
                <span class="expand-meta-dot"></span>
                <el-tag :type="(diffTagType[row.difficulty] as any)" size="small" round effect="plain">{{ row.difficultyName }}</el-tag>
              </div>
              <!-- 题目内容 -->
              <div class="expand-question-text">{{ row.content }}</div>

              <!-- 选项列表（选择题） -->
              <div v-if="row.options && row.options.length" class="expand-options">
                <div v-for="(opt, oi) in row.options" :key="oi"
                  class="expand-option"
                  :class="isCorrectExpandOption(opt, row) ? 'expand-option--correct' : ''">
                  <span class="expand-option-badge"
                    :class="isCorrectExpandOption(opt, row) ? 'expand-option-badge--correct' : ''">
                    {{ getExpandOptionLetter(opt) || String.fromCharCode(65 + Number(oi)) }}
                  </span>
                  <span class="flex-1 text-sm">{{ getExpandOptionText(opt) }}</span>
                  <ArtSvgIcon v-if="isCorrectExpandOption(opt, row)" icon="ri:checkbox-circle-fill" class="text-base text-green-500 shrink-0" />
                </div>
              </div>

              <!-- 答案展示：短答案（选择/判断）用内联，长答案（填空/简答）用块级 -->
              <div v-if="[1, 2, 3].includes(row.questionType)" class="expand-answer-bar">
                <ArtSvgIcon icon="ri:checkbox-circle-line" class="text-sm text-green-500 shrink-0" />
                <span class="text-xs text-gray-400 shrink-0">答案</span>
                <template v-if="row.questionType === 1">
                  <el-tag type="success" size="small" round effect="plain">{{ row.answer }}</el-tag>
                </template>
                <template v-else-if="row.questionType === 2">
                  <el-tag v-for="l in (row.answer || '').split(',')" :key="l" type="success" size="small" round effect="plain">{{ l.trim() }}</el-tag>
                </template>
                <template v-else>
                  <el-tag :type="row.answer === '1' ? 'success' : 'danger'" size="small" round>{{ row.answer === '1' ? '✓ 正确' : '✗ 错误' }}</el-tag>
                </template>
              </div>
              <div v-else class="expand-answer-block">
                <div class="expand-answer-block-label">
                  <ArtSvgIcon icon="ri:checkbox-circle-line" class="text-sm text-green-500" />答案
                </div>
                <div class="expand-answer-block-content">{{ row.answer }}</div>
              </div>

              <!-- 解析面板 -->
              <div v-if="row.analysis" class="expand-analysis">
                <div class="expand-analysis-label">
                  <ArtSvgIcon icon="ri:lightbulb-line" class="text-sm" />解析
                </div>
                <div class="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{{ row.analysis }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="subjectName" label="科目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="questionTypeName" label="题型" width="90" align="center" :show-overflow-tooltip="false" class-name="no-ellipsis">
          <template #default="{ row }">
            <el-tag :type="(typeTagMap[row.questionType] as any)" size="small" round>{{ row.questionTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="答案" width="120" align="center" :show-overflow-tooltip="false" class-name="no-ellipsis">
          <template #default="{ row }">
            <!-- 单选题：单个字母Tag -->
            <el-tag v-if="row.questionType === 1" type="success" size="small" round effect="plain">
              {{ row.answer }}
            </el-tag>
            <!-- 多选题：多个字母Tag横排 -->
            <div v-else-if="row.questionType === 2" class="answer-tags">
              <el-tag v-for="letter in (row.answer || '').split(',')" :key="letter" type="success" size="small" round effect="plain" class="answer-tag">
                {{ letter.trim() }}
              </el-tag>
            </div>
            <!-- 判断题：正确/错误Tag -->
            <el-tag v-else-if="row.questionType === 3" :type="row.answer === '1' ? 'success' : 'danger'" size="small" round>
              {{ row.answer === '1' ? '✓ 正确' : '✗ 错误' }}
            </el-tag>
            <!-- 填空题：截断 + Tooltip -->
            <el-tooltip v-else-if="row.questionType === 4" :content="row.answer" placement="top" :show-after="300" :disabled="!row.answer || row.answer.length <= 8">
              <span class="answer-truncate">{{ row.answer && row.answer.length > 8 ? row.answer.substring(0, 8) + '...' : row.answer }}</span>
            </el-tooltip>
            <!-- 简答题：提示展开查看 -->
            <span v-else class="text-gray-400 text-xs">展开查看</span>
          </template>
        </el-table-column>
        <el-table-column prop="difficultyName" label="难度" width="80" align="center" :show-overflow-tooltip="false" class-name="no-ellipsis">
          <template #default="{ row }">
            <el-tag :type="(diffTagType[row.difficulty] as any)" size="small" round>{{ row.difficultyName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="60" align="center" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <ArtButtonTable type="edit" @click="openDialog(row)" />
            <ArtButtonTable type="delete" @click="handleDelete(row)" />
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
          :total="total" layout="total, sizes, prev, pager, next" background @change="loadData" />
      </div>
      </div>
    </ElCard>

    <!-- 添加/编辑题目对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑题目' : '添加题目'" width="700px" top="5vh" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="form.subjectId" style="width: 100%">
                <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题型" prop="questionType">
              <el-select v-model="form.questionType" style="width: 100%" @change="onTypeChange">
                <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="Number(k)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="题目" prop="content"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>

        <!-- 选项 -->
        <el-form-item v-if="[1, 2].includes(form.questionType)" label="选项">
          <div class="w-full space-y-2">
            <div v-for="(opt, i) in form.options" :key="i" class="flex items-center gap-2">
              <el-tag size="small" :type="isOptionSelected(Number(i)) ? 'success' : 'info'" class="w-6 text-center">{{ String.fromCharCode(65 + Number(i)) }}</el-tag>
              <el-input v-model="form.options[Number(i)]" :placeholder="'请输入选项' + String.fromCharCode(65 + Number(i)) + '的内容'" />
              <el-button v-if="form.options.length > 2" type="danger" circle size="small" @click="removeOption(Number(i))"><ArtSvgIcon icon="ri:delete-bin-line" /></el-button>
            </div>
            <el-button type="primary" link @click="addOption" :disabled="form.options.length >= 8">+ 添加选项</el-button>
          </div>
        </el-form-item>

        <!-- 答案 -->
        <el-form-item label="答案" prop="answer">
          <el-radio-group v-if="form.questionType === 1" v-model="form.answer">
            <el-radio v-for="(opt, i) in form.options" :key="i" :value="String.fromCharCode(65 + Number(i))">{{ String.fromCharCode(65 + Number(i)) }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="form.questionType === 2" v-model="multiAnswer" @change="(v: any) => form.answer = (v as string[]).sort().join(',')">
            <el-checkbox v-for="(opt, i) in form.options" :key="i" :value="String.fromCharCode(65 + Number(i))">{{ String.fromCharCode(65 + Number(i)) }}</el-checkbox>
          </el-checkbox-group>
          <div v-else-if="form.questionType === 3" class="flex items-center gap-3">
            <el-switch v-model="judgeAnswer" active-text="正确" inactive-text="错误" @change="(v: any) => form.answer = v ? '1' : '0'" />
            <el-tag :type="judgeAnswer ? 'success' : 'danger'" size="small">{{ judgeAnswer ? '正确' : '错误' }}</el-tag>
          </div>
          <el-input v-else-if="form.questionType === 4" v-model="form.answer" placeholder="多个空用逗号分隔" />
          <el-input v-else v-model="form.answer" type="textarea" :rows="3" placeholder="请输入参考答案" />
        </el-form-item>

        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="选填" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分值" prop="score"><el-input-number v-model="form.score" :min="0.5" :step="0.5" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度" prop="difficulty">
              <el-radio-group v-model="form.difficulty">
                <el-radio-button :value="1">简单</el-radio-button>
                <el-radio-button :value="2">中等</el-radio-button>
                <el-radio-button :value="3">困难</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" :title="importStep === 'done' ? '导入结果' : '导入题目'" width="580px" :close-on-click-modal="importStep !== 'importing'" :close-on-press-escape="importStep !== 'importing'" :show-close="importStep !== 'importing'" @closed="resetImport">
      <!-- Step 1: 选择文件 -->
      <template v-if="importStep === 'select'">
        <div class="import-subject-bar">
          <span class="import-label">导入科目</span>
          <el-select v-model="importSubjectId" placeholder="请选择科目" style="flex: 1">
            <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
          </el-select>
        </div>
        <div class="import-dropzone" :class="{ 'import-dropzone--active': isDragging, 'import-dropzone--compact': importFiles.length > 0 }" @dragover.prevent="isDragging = true" @dragleave.prevent="isDragging = false" @drop.prevent="handleDrop" @click="triggerFileInput">
          <input ref="fileInputRef" type="file" multiple accept=".xlsx,.xls" class="hidden" @change="handleFileInput" />
          <div v-if="importFiles.length === 0" class="import-dropzone-content">
            <div class="import-dropzone-icon"><ArtSvgIcon icon="ri:file-excel-2-line" /></div>
            <p class="import-dropzone-title">拖拽文件到此处，或 <span class="import-dropzone-link">点击选择</span></p>
            <p class="import-dropzone-hint">支持 .xlsx / .xls 格式，可同时选择多个文件</p>
          </div>
          <div v-else class="import-dropzone-strip">
            <ArtSvgIcon icon="ri:add-circle-line" class="text-base" />
            <span>点击或拖拽添加更多文件</span>
          </div>
        </div>
        <div v-if="importFiles.length > 0" class="import-file-list">
          <div v-for="(f, idx) in importFiles" :key="idx" class="import-file-item">
            <div class="import-file-icon-wrap"><ArtSvgIcon icon="ri:file-excel-2-fill" /></div>
            <div class="import-file-info">
              <span class="import-file-name">{{ f.name }}</span>
              <span class="import-file-size">{{ formatFileSize(f.size) }}</span>
            </div>
            <div class="import-file-remove" @click.stop="removeFile(idx)"><ArtSvgIcon icon="ri:close-line" /></div>
          </div>
        </div>
      </template>
      <!-- Step 2: 导入中 -->
      <template v-if="importStep === 'importing'">
        <div class="import-progress">
          <div class="import-progress-center">
            <el-progress type="circle" :percentage="importProgressPercent" :width="100" :stroke-width="8" color="var(--theme-color)">
              <template #default>
                <div class="import-progress-circle-inner">
                  <span class="import-progress-circle-num">{{ importCurrent + 1 }}<span class="import-progress-circle-sep">/</span>{{ importFiles.length }}</span>
                  <span class="import-progress-circle-label">文件</span>
                </div>
              </template>
            </el-progress>
          </div>
          <p class="import-progress-desc">正在导入题目到「{{ importSubjectName }}」</p>
          
          <div class="import-progress-list">
            <div v-for="(f, idx) in importFiles" :key="idx" class="import-progress-item" :class="{ 'import-progress-item--active': f.status === 'importing', 'import-progress-item--done': f.status === 'success', 'import-progress-item--fail': f.status === 'error' }">
              <div class="import-file-icon-wrap" :class="{ 'import-file-icon-wrap--fail': f.status === 'error' }">
                <ArtSvgIcon :icon="f.status === 'error' ? 'ri:close-line' : 'ri:file-excel-2-fill'" />
              </div>
              <div class="import-file-info">
                <span class="import-file-name">{{ f.name }}</span>
                <span v-if="f.status === 'success'" class="import-file-size" style="color: var(--el-color-success)">成功导入 {{ f.successCount }} 题</span>
                <span v-else-if="f.status === 'error'" class="import-file-size" style="color: var(--el-color-danger)">{{ f.errorMsg }}</span>
                <span v-else-if="f.status === 'importing'" class="import-file-size" style="color: var(--el-color-primary)">正在处理...</span>
                <span v-else class="import-file-size">等待中</span>
              </div>
              <div class="import-progress-badge" :class="{ 'import-progress-badge--ok': f.status === 'success', 'import-progress-badge--fail': f.status === 'error', 'import-progress-badge--active': f.status === 'importing' }">
                <ArtSvgIcon v-if="f.status === 'success'" icon="ri:check-line" />
                <ArtSvgIcon v-else-if="f.status === 'error'" icon="ri:close-line" />
                <ArtSvgIcon v-else-if="f.status === 'importing'" icon="ri:loader-4-line" class="import-spin" />
                <ArtSvgIcon v-else icon="ri:time-line" />
              </div>
            </div>
          </div>
        </div>
      </template>
      <!-- Step 3: 导入结果 -->
      <template v-if="importStep === 'done'">
        <div class="import-result">
          <div class="import-result-hero">
            <div class="import-result-check" :class="{ 'import-result-check--warn': importTotalFail > 0 }">
              <ArtSvgIcon :icon="importTotalFail > 0 ? 'ri:error-warning-line' : 'ri:check-line'" />
            </div>
            <div class="import-result-title">{{ importTotalFail > 0 ? '导入完成，部分失败' : '导入完成' }}</div>
            <div class="import-result-subtitle">题目已添加到「{{ importSubjectName }}」题库</div>
          </div>
          <div class="import-result-stats">
            <div class="import-result-stat">
              <span class="import-result-stat-num">{{ importTotalSuccess }}</span>
              <span class="import-result-stat-label">题目已导入</span>
            </div>
            <div class="import-result-stat-divider"></div>
            <div class="import-result-stat">
              <span class="import-result-stat-num">{{ importFiles.length }}</span>
              <span class="import-result-stat-label">文件已处理</span>
            </div>
            <template v-if="importTotalFail > 0">
              <div class="import-result-stat-divider"></div>
              <div class="import-result-stat import-result-stat--fail">
                <span class="import-result-stat-num">{{ importTotalFail }}</span>
                <span class="import-result-stat-label">导入失败</span>
              </div>
            </template>
          </div>
          <div class="import-result-details">
            <div v-for="(f, idx) in importFiles" :key="idx" class="import-result-file">
              <div class="import-file-icon-wrap" :class="{ 'import-file-icon-wrap--fail': f.status !== 'success' }">
                <ArtSvgIcon :icon="f.status === 'success' ? 'ri:file-excel-2-fill' : 'ri:close-line'" />
              </div>
              <div class="import-file-info">
                <span class="import-file-name">{{ f.name }}</span>
                <span v-if="f.status === 'success'" class="import-file-size">{{ f.successCount }} 题<template v-if="f.failCount > 0"> · {{ f.failCount }} 题失败</template> · {{ importSubjectName }}</span>
                <span v-else class="import-file-size" style="color: var(--el-color-danger)">{{ f.errorMsg }}</span>
              </div>
              <div class="import-result-badge" :class="f.status === 'success' ? 'import-result-badge--ok' : 'import-result-badge--fail'">
                <ArtSvgIcon :icon="f.status === 'success' ? 'ri:check-line' : 'ri:close-line'" />
              </div>
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <template v-if="importStep === 'select'">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!importSubjectId || importFiles.length === 0" @click="startImport">
            <ArtSvgIcon icon="ri:upload-2-line" class="mr-1" />开始导入{{ importFiles.length > 0 ? ` (${importFiles.length} 个文件)` : '' }}
          </el-button>
        </template>
        <template v-else-if="importStep === 'done'">
          <el-button @click="importDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="importDialogVisible = false; query.subjectId = importSubjectId; loadData()">
            <ArtSvgIcon icon="ri:eye-line" class="mr-1" />查看已导入题目
          </el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getQuestionList, getQuestionDetail, addQuestion, updateQuestion, deleteQuestion, batchDeleteQuestions, importQuestions } from '@/api/exam/question'
import { getAllSubjects } from '@/api/exam/subject'
import { questionTypeMap as typeMap, difficultyTagType as diffTagType } from '@/utils/exam-format'
import { buildSubjectLabelMap, getSubjectLabel } from '@/utils/subject-label'

defineOptions({ name: 'QuestionManage' })
const route = useRoute()
const headerStyle = { background: 'var(--el-fill-color)', fontWeight: 600, fontSize: '13px', color: 'var(--el-text-color-regular)', borderBottom: '2px solid var(--el-border-color)' }
const typeTagMap: Record<number, string> = { 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'danger' }
const subjects = ref<any[]>([])
// 「按需维度消歧」科目下拉显示：仅在同名科目存在时才加专业/年级后缀。
// 例：zhangwenge 教多专业「思想道德与法治」时下拉显示「思想道德与法治（计算机科学与技术）」；
//     luweizhong 只教唯一「面向对象技术」时显示纯课程名，避免冗余。
// 算法详见 @/utils/subject-label.ts。
const subjectLabelMap = computed(() => buildSubjectLabelMap(subjects.value))
const query = ref<any>({ page: 1, size: 10, subjectId: null, questionType: null, difficulty: null, keyword: '' })
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const importSubjectId = ref<number | null>(null)
const importFiles = ref<any[]>([])
const importStep = ref<'select' | 'importing' | 'done'>('select')
const importCurrent = ref(0)
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedRows = ref<any[]>([])
const tableRef = ref<any>(null)
const multiAnswer = ref<string[]>([])
const judgeAnswer = ref(true)

const defaultForm = () => ({ subjectId: null as any, questionType: 1, content: '', options: ['', '', '', ''], answer: '', analysis: '', score: 5, difficulty: 1 })
const form = ref<any>(defaultForm())
const rules = {
  subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目', trigger: 'blur' }],
  answer: [{ required: true, message: '请设置答案', trigger: 'change' }],
  score: [{ required: true, message: '请输入分值', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

function formatAnswer(row: any) {
  if (row.questionType === 3) return row.answer === '1' ? '正确' : '错误'
  return row.answer || ''
}

function getExpandOptionLetter(opt: string): string {
  const m = opt.match(/^([A-Z])[\.\s、：:]/)
  return m ? m[1] : ''
}

function getExpandOptionText(opt: string): string {
  return opt.replace(/^[A-Z][\.\s、：:]\s*/, '')
}

function isCorrectExpandOption(opt: string, row: any): boolean {
  const letter = getExpandOptionLetter(opt)
  if (!letter || !row.answer) return false
  return row.answer.includes(letter)
}

function isOptionSelected(i: number) {
  const letter = String.fromCharCode(65 + i)
  if (form.value.questionType === 1) return form.value.answer === letter
  if (form.value.questionType === 2) return multiAnswer.value.includes(letter)
  return false
}

function addOption() { form.value.options.push('') }
function removeOption(i: number) {
  form.value.options.splice(i, 1)
  form.value.answer = ''
  multiAnswer.value = []
}

function onTypeChange() {
  form.value.answer = ''
  multiAnswer.value = []
  judgeAnswer.value = true
  if ([1, 2].includes(form.value.questionType)) form.value.options = ['', '', '', '']
  else form.value.options = []
  if (form.value.questionType === 3) form.value.answer = '1'
}

async function loadData() {
  const res = await getQuestionList(query.value)
  tableData.value = res.records
  total.value = res.total
}

function openDialog(row?: any) {
  isEdit.value = !!row
  editId.value = row?.id
  if (row) {
    const opts = row.options ? row.options.map((o: string) => o.replace(/^[A-Z]\./, '')) : ['', '', '', '']
    form.value = { subjectId: row.subjectId, questionType: row.questionType, content: row.content, options: opts, answer: row.answer, analysis: row.analysis, score: row.score, difficulty: row.difficulty }
    if (row.questionType === 2) multiAnswer.value = row.answer ? row.answer.split(',') : []
    else multiAnswer.value = []
    judgeAnswer.value = row.questionType === 3 ? row.answer === '1' : true
  } else {
    form.value = defaultForm()
    multiAnswer.value = []
    judgeAnswer.value = true
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  const submitData = { ...form.value }
  if ([1, 2].includes(submitData.questionType)) {
    submitData.options = submitData.options.map((o: string, i: number) => String.fromCharCode(65 + i) + '.' + o)
  }
  if (isEdit.value) await updateQuestion(editId.value!, submitData)
  else await addQuestion(submitData)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  loadData()
}

function handleSelectionChange(selection: any[]) {
  selectedRows.value = selection
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 道题目？`, '批量删除', { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' })
  } catch { return }
  try {
    const ids = selectedRows.value.map((r: any) => r.id)
    const res: any = await batchDeleteQuestions(ids)
    const msg = `成功删除 ${res.success || 0} 题` + (res.softDelete ? `，${res.softDelete} 题已被试卷引用（已移除）` : '') + (res.skip ? `，${res.skip} 题跳过` : '')
    ElMessage.success(msg)
    selectedRows.value = []
    loadData()
  } catch (e: any) {
    console.error('批量删除失败:', e)
    ElMessage.error('批量删除失败，请重试')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该题目？', '提示', { type: 'warning' })
  } catch { return }
  try {
    const res = await deleteQuestion(row.id)
    const msg = (typeof res === 'string' ? res : null) || '删除成功'
    if (msg.includes('移除')) {
      ElMessage.warning(msg)
    } else {
      ElMessage.success(msg)
    }
    // 从列表中移除该项并更新总数
    tableData.value = tableData.value.filter((item: any) => item.id !== row.id)
    total.value--
  } catch (e: any) {
    console.error('删除题目失败:', e)
    ElMessage.error('删除失败，请重试')
  }
}

const importSubjectName = computed(() => {
  // 与下拉选项 label 保持一致：同名科目场景下显示「思想道德与法治（计算机...）」，
  // 让教师在导入进度/完成提示中能明确看到导到哪条 subject。
  const s = subjects.value.find((s: any) => s.id === importSubjectId.value)
  return getSubjectLabel(importSubjectId.value, subjectLabelMap.value, s?.subjectName)
})
const importProgressPercent = computed(() => {
  if (importFiles.value.length === 0) return 0
  const done = importFiles.value.filter((f: any) => f.status === 'success' || f.status === 'error').length
  return Math.round(((done + 0.5) / importFiles.value.length) * 100)
})
const importTotalSuccess = computed(() => importFiles.value.reduce((sum: number, f: any) => sum + (f.successCount || 0), 0))
const importTotalFail = computed(() => {
  const rowFails = importFiles.value.reduce((sum: number, f: any) => sum + (f.failCount || 0), 0)
  const fileFails = importFiles.value.filter((f: any) => f.status === 'error').length
  return rowFails + fileFails
})

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function triggerFileInput() { fileInputRef.value?.click() }

function handleFileInput(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) { addFiles(Array.from(input.files)); input.value = '' }
}

function handleDrop(e: DragEvent) {
  isDragging.value = false
  if (e.dataTransfer?.files) addFiles(Array.from(e.dataTransfer.files))
}

function addFiles(files: File[]) {
  const validExts = ['.xlsx', '.xls']
  for (const file of files) {
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()
    if (!validExts.includes(ext)) { ElMessage.warning(`"${file.name}" 格式不支持，仅支持 .xlsx / .xls`); continue }
    if (importFiles.value.some((f: any) => f.name === file.name && f.size === file.size)) continue
    importFiles.value.push({ file, name: file.name, size: file.size, status: 'pending', successCount: 0, failCount: 0, errorMsg: '' })
  }
}

function removeFile(idx: number) { importFiles.value.splice(idx, 1) }

function resetImport() {
  importStep.value = 'select'
  importFiles.value = []
  importCurrent.value = 0
  isDragging.value = false
}

async function startImport() {
  if (!importSubjectId.value || importFiles.value.length === 0) { ElMessage.warning('请选择科目和文件'); return }
  importStep.value = 'importing'
  importCurrent.value = 0
  for (let i = 0; i < importFiles.value.length; i++) {
    importCurrent.value = i
    importFiles.value[i].status = 'importing'
    try {
      const fd = new FormData()
      fd.append('file', importFiles.value[i].file)
      fd.append('subjectId', String(importSubjectId.value))
      const res: any = await importQuestions(fd)
      importFiles.value[i].status = 'success'
      importFiles.value[i].successCount = res.success || 0
      importFiles.value[i].failCount = res.fail || 0
      if (res.failDetails?.length) {
        importFiles.value[i].errorMsg = res.failDetails.map((d: any) => `第${d.row}行: ${d.reason}`).join('; ')
      }
    } catch (e: any) {
      importFiles.value[i].status = 'error'
      importFiles.value[i].errorMsg = e?.message || '导入失败'
    }
  }
  importStep.value = 'done'
  loadData()
}

onMounted(async () => {
  subjects.value = await getAllSubjects()
  loadData()
  const qEditId = route.query.editId
  if (qEditId) {
    try {
      const row = await getQuestionDetail(Number(qEditId))
      if (row) openDialog(row)
    } catch (e) { /* ignore */ }
  }
})
onActivated(() => { loadData() })
</script>

<style lang="scss" scoped>
.art-table-card {
  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

.table-wrap {
  :deep(.el-table) {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    overflow: hidden;
    &::before, &::after { display: none; }
    .el-table__row {
      transition: background-color 0.2s;
      &:hover > td { background-color: var(--el-fill-color-light) !important; }
    }
    .el-table__expanded-cell {
      padding: 0 !important;
    }
  }
}
/* ===== 展开行详情样式 ===== */
.expand-detail {
  margin: 0;
  padding: 16px 20px;
  background: var(--el-fill-color-light);
}
.expand-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  margin-bottom: 10px;
}
.expand-meta-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
  flex-shrink: 0;
}
.expand-question-text {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}
.expand-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.expand-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  transition: all 0.15s ease;
  font-size: 13px;
  &:hover {
    border-color: var(--el-border-color);
  }
}
.expand-option--correct {
  border-color: var(--el-color-success-light-5) !important;
  background: var(--el-color-success-light-9) !important;
}
.expand-option-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  border: 1.5px solid var(--el-border-color-light);
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
}
.expand-option-badge--correct {
  border-color: var(--el-color-success) !important;
  background: var(--el-color-success) !important;
  color: #fff !important;
}
.expand-answer-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--el-color-success-light-9);
  margin-bottom: 10px;
  font-size: 13px;
}
.expand-answer-block {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-color-success-light-9);
  margin-bottom: 10px;
}
.expand-answer-block-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-success);
  margin-bottom: 6px;
}
.expand-answer-block-content {
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-color-success-dark-2);
  word-break: break-all;
}
.expand-analysis {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
  border-left: 3px solid var(--el-color-primary-light-5);
}
.expand-analysis-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 4px;
}

/* ===== 答案列标签样式 ===== */
.answer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}
.answer-tag {
  margin: 0 !important;
}
.answer-truncate {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-color-success);
  font-weight: 500;
  cursor: default;
}
:deep(.table-pagination) {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 4px;
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

/* ===== 导入对话框样式 ===== */
.import-subject-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.import-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}
.import-dropzone {
  position: relative;
  border: 2px dashed var(--el-border-color);
  border-radius: 10px;
  padding: 36px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  background: var(--el-fill-color-lighter);
  &:hover {
    border-color: var(--theme-color);
    background: var(--el-color-primary-light-9);
  }
}
.import-dropzone--active {
  border-color: var(--theme-color) !important;
  background: var(--el-color-primary-light-9) !important;
  transform: scale(1.01);
}
.import-dropzone--compact {
  padding: 12px 16px;
}
.import-dropzone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
}
.import-dropzone-icon {
  font-size: 36px;
  color: var(--theme-color);
  opacity: 0.7;
}
.import-dropzone-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin: 0;
}
.import-dropzone-link {
  color: var(--theme-color);
  font-weight: 600;
}
.import-dropzone-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin: 2px 0 0;
}
.import-dropzone-strip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: var(--theme-color);
  font-weight: 500;
  pointer-events: none;
}
.import-file-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}
.import-file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.15s;
  &:hover {
    border-color: var(--el-border-color);
    background: var(--el-fill-color);
  }
}
.import-file-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e8f5ee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #217346;
  flex-shrink: 0;
}
.import-file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.import-file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.import-file-size {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
.import-file-remove {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
  &:hover {
    background: var(--el-color-danger-light-9);
    color: var(--el-color-danger);
  }
}
/* ===== Step 2: 导入进度 ===== */
.import-progress {
  padding: 0;
}
.import-progress-center {
  display: flex;
  justify-content: center;
  padding: 8px 0 12px;
}
.import-progress-circle-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.import-progress-circle-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1;
}
.import-progress-circle-sep {
  font-size: 16px;
  font-weight: 400;
  color: var(--el-text-color-placeholder);
  margin: 0 1px;
}
.import-progress-circle-label {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  font-weight: 500;
}
.import-progress-desc {
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0 0 20px;
}
.import-progress-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}
.import-progress-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
  border: 1px solid transparent;
  transition: all 0.3s ease;
}
.import-progress-item--active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 0 0 3px rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.1);
}
.import-progress-item--done {
  background: var(--el-color-success-light-9);
  border-color: var(--el-color-success-light-7);
}
.import-progress-item--fail {
  background: var(--el-color-danger-light-9);
  border-color: var(--el-color-danger-light-7);
}
.import-progress-badge {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-placeholder);
  transition: all 0.2s;
  margin-left: auto; /* Push to the right */
}
.import-progress-badge--ok {
  background: var(--el-color-success);
  color: #fff;
}
.import-progress-badge--fail {
  background: var(--el-color-danger);
  color: #fff;
}
.import-progress-badge--active {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}
.import-spin {
  animation: import-spin 1s linear infinite;
}
@keyframes import-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== Step 3: 导入结果 ===== */
.import-result-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding-bottom: 24px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.import-result-check {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--el-color-success);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  margin-bottom: 6px;
  box-shadow: 0 4px 12px rgba(var(--el-color-success-rgb, 103, 194, 58), 0.2);
}
.import-result-check--warn {
  background: var(--el-color-warning);
  box-shadow: 0 4px 12px rgba(var(--el-color-warning-rgb, 230, 162, 60), 0.2);
}
.import-result-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.import-result-subtitle {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.import-result-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 20px 16px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  margin-bottom: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}
.import-result-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.import-result-stat-num {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  color: var(--el-color-success);
}
.import-result-stat--fail .import-result-stat-num {
  color: var(--el-color-danger);
}
.import-result-stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}
.import-result-stat-divider {
  width: 1px;
  height: 48px;
  background: var(--el-border-color-lighter);
  flex-shrink: 0;
}
.import-result-details {
  display: flex;
  flex-direction: column;
  gap: 0;
  max-height: 200px;
  overflow-y: auto;
}
.import-result-file {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  transition: all 0.15s;
}
.import-result-file:last-child {
  border-bottom: none;
}
.import-result-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
  margin-left: auto; /* Push to the right */
}
.import-result-badge--ok {
  background: var(--el-color-success);
  color: #fff;
}
.import-result-badge--fail {
  background: var(--el-color-danger);
  color: #fff;
}
.import-file-icon-wrap--fail {
  background: #fef0f0 !important;
  color: var(--el-color-danger) !important;
}
</style>
