<template>
  <div>
    <!-- 搜索栏 -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3">
        <el-input v-model="query.paperName" placeholder="试卷名称" clearable style="width: 200px" @clear="loadData" @keyup.enter="loadData" />
        <el-select v-model="query.subjectId" placeholder="全部科目" clearable style="width: 180px" @change="loadData">
          <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <div class="flex gap-2">
        <ElButton type="primary" @click="router.push('/teacher/paper/edit')"><ArtSvgIcon icon="ri:edit-line" class="mr-1" />手动组卷</ElButton>
        <ElButton @click="openRandomDialog"><ArtSvgIcon icon="ri:magic-line" class="mr-1" />随机组卷</ElButton>
      </div>
    </div>

    <!-- 表格 -->
    <ElCard class="art-table-card" shadow="never" style="margin-top: 0; margin-bottom: 20px">
      <template #header>
        <div class="flex-cb">
          <h4 class="m-0">试卷列表</h4>
          <el-tag effect="light">共 {{ total }} 卷</el-tag>
        </div>
      </template>
      <div class="table-wrap">
      <el-table :data="tableData" style="width: 100%" :header-cell-style="headerStyle">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="paperName" label="试卷名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="subjectName" label="科目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="totalScore" label="总分" width="70" align="center" />
        <el-table-column prop="passScore" label="及格分" width="70" align="center" />
        <el-table-column prop="duration" label="时长(分)" width="80" align="center" />
        <el-table-column prop="questionCount" label="题数" width="60" align="center" />
        <el-table-column label="状态" width="90" align="center" :show-overflow-tooltip="false" class-name="no-ellipsis">
          <template #default="{ row }">
            <el-tag v-if="row.examCount > 0 && row.status === 1" type="warning" size="small" round>已使用</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" size="small" round>可使用</el-tag>
            <el-tag v-else type="info" size="small" round>草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <ArtButtonTable
              v-if="row.examCount === 0"
              :icon="row.status === 1 ? 'ri:arrow-go-back-line' : 'ri:send-plane-line'"
              :icon-class="row.status === 1 ? 'bg-amber-500/12 text-amber-500' : 'bg-primary/12 text-primary'"
              @click="handleTogglePublish(row)"
            />
            <ArtButtonTable type="view" @click="router.push(`/teacher/paper/edit/${row.id}`)" />
            <ArtButtonTable v-if="row.examCount === 0" type="edit" @click="router.push(`/teacher/paper/edit/${row.id}?mode=edit`)" />
            <ArtButtonTable icon="ri:file-search-line" icon-class="bg-success/12 text-success" @click="handlePreview(row)" />
            <ArtButtonTable v-if="row.examCount === 0" type="delete" @click="handleDelete(row)" />
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
          :total="total" layout="total, sizes, prev, pager, next" background @change="loadData" />
      </div>
      </div>
    </ElCard>

    <!-- 随机组卷对话框 -->
    <el-dialog v-model="randomDialogVisible" title="随机组卷" width="600px" destroy-on-close>
      <el-form :model="randomForm" label-width="80px">
        <el-form-item label="套用模板">
          <el-select v-model="selectedTemplateId" placeholder="选择模板一键填充（可选）" clearable style="width: 100%" @change="applyTemplate">
            <el-option v-for="t in templateList" :key="t.id" :label="`${t.templateName} (${t.subjectName})`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="试卷名称"><el-input v-model="randomForm.paperName" /></el-form-item>
        <el-form-item label="科目">
          <el-select v-model="randomForm.subjectId" style="width: 100%">
            <el-option v-for="s in subjects" :key="s.id" :label="subjectLabelMap.get(s.id) || s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="及格分"><el-input-number v-model="randomForm.passScore" :min="0" /></el-form-item>
        <el-form-item label="时长(分)"><el-input-number v-model="randomForm.duration" :min="1" /></el-form-item>
        <el-form-item label="抽题规则">
          <div class="w-full space-y-2">
            <div class="flex gap-2 text-xs text-gray-400">
              <span class="w-24">题型</span><span class="w-20">数量</span><span class="w-24">每题分值</span><span class="w-24">难度</span><span class="w-8"></span>
            </div>
            <div v-for="(rule, i) in randomForm.rules" :key="i" class="flex items-center gap-2">
              <el-select v-model="rule.questionType" size="small" style="width: 96px">
                <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="Number(k)" />
              </el-select>
              <el-input-number v-model="rule.count" :min="1" size="small" style="width: 80px" />
              <el-input-number v-model="rule.scorePerQuestion" :min="0.5" :step="0.5" size="small" style="width: 96px" />
              <el-select v-model="rule.difficulty" placeholder="不限" clearable size="small" style="width: 96px">
                <el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" />
              </el-select>
              <el-button type="danger" circle size="small" @click="randomForm.rules.splice(i, 1)"><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-button type="primary" link @click="randomForm.rules.push({ questionType: 1, count: 5, scorePerQuestion: 2, difficulty: null })">+ 添加规则</el-button>
            <div class="flex items-center justify-between mt-3 pt-3 border-t border-gray-200">
              <span class="text-sm font-bold">
                当前总分：<span :class="scoreMismatch ? 'text-red-500' : 'text-green-600'">{{ currentTotalScore }} 分</span>
              </span>
              <span v-if="templateTargetScore !== null" class="text-xs" :class="scoreMismatch ? 'text-red-500' : 'text-green-600'">
                {{ scoreMismatch ? '⚠ 与模板目标 ' + templateTargetScore + ' 分不一致' : '✓ 与模板目标一致' }}
              </span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="randomDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRandom">生成试卷</el-button>
      </template>
    </el-dialog>

    <!-- 试卷预览对话框 -->
    <el-dialog v-model="previewVisible" title="试卷预览" width="950px" top="3vh" class="preview-dialog">
      <template #header>
        <div class="flex justify-between items-center pr-5">
          <span class="text-base font-bold">试卷预览</span>
          <el-button type="primary" size="small" :loading="pdfExporting" @click="exportPdf">
            <ArtSvgIcon v-if="!pdfExporting" icon="ri:file-pdf-2-line" class="mr-1" />{{ pdfExporting ? '导出中...' : '导出PDF' }}
          </el-button>
        </div>
      </template>
      <div class="paper-container">
        <div ref="previewRef" class="paper-content">
          <!-- 左侧密封线区域（含学生信息） -->
          <div class="binding-area">
            <p class="binding-info">学号______ 姓名______ 班级______ 专业______ 学院______</p>
            <p class="binding-seal">密封线内不要答题</p>
          </div>
          <!-- 右侧试卷正文 -->
          <div class="paper-main-content">
            <!-- 试卷头部 -->
            <div class="paper-header">
              <h1 class="school-name">苏州科技大学天平学院</h1>
              <h2 class="semester-info">{{ currentSemester }}</h2>
              <h2 class="paper-title">{{ previewData?.paperName }}</h2>
              <div class="exam-meta-bar">
                <span>考试形式：闭卷</span>
                <span>考试时间：{{ previewData?.duration }} 分钟</span>
                <span>满分：{{ previewData?.totalScore }} 分</span>
              </div>
              <div class="total-score-wrapper">
                <table class="score-table" v-if="groupedPreviewQuestions.length > 0">
                  <tr>
                    <th class="score-th-label">题号</th>
                    <th v-for="(group, idx) in groupedPreviewQuestions" :key="idx" class="score-th-num">{{ chineseNum[idx] }}</th>
                    <th class="score-th-label">总分</th>
                  </tr>
                  <tr>
                    <td class="score-td-label">得分</td>
                    <td v-for="(group, idx) in groupedPreviewQuestions" :key="idx"></td>
                    <td></td>
                  </tr>
                </table>
              </div>
              <div class="exam-notice">
                <strong>注意事项：</strong>
                <span>1. 答题前请将密封线内各项信息填写清楚；</span>
                <span>2. 所有答案必须写在试卷上，写在其他地方无效；</span>
                <span>3. 考试结束后将试卷交回。</span>
              </div>
            </div>
            <!-- 试题正文 -->
            <div class="paper-body" v-if="previewData">
              <div v-for="(group, gIndex) in groupedPreviewQuestions" :key="group.type" class="question-group">
                <div class="group-title">
                  <table class="group-score-box">
                    <tr><th>得分</th><th>评卷人</th></tr>
                    <tr><td></td><td></td></tr>
                  </table>
                  <div class="group-title-text">
                    <h3>{{ chineseNum[gIndex] }}、{{ typeMap[group.type] }}</h3>
                    <span class="group-desc">（本大题共 {{ group.items.length }} 小题，总计 {{ group.totalScore }} 分）</span>
                  </div>
                </div>
                <div v-for="(q, qIndex) in group.items" :key="q.id" class="question-item">
                  <div class="q-content">
                    <span class="q-num">{{ Number(qIndex) + 1 }}.</span>
                    <span class="q-text">{{ q.content }}</span>
                    <span class="q-score">（{{ q.score }}分）</span>
                  </div>
                  <div v-if="[1,2].includes(q.questionType) && q.options?.length" class="q-options">
                    <div v-for="opt in q.options" :key="opt" class="opt-item">{{ opt }}</div>
                  </div>
                  <div v-if="q.questionType === 3" class="q-options inline">
                    <div class="opt-item">A. 正确</div>
                    <div class="opt-item">B. 错误</div>
                  </div>
                  <div v-if="q.questionType === 4" class="q-blank">答：________________________________________________________</div>
                  <div v-if="q.questionType === 5" class="q-answer-area">答：</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getPaperList, getPaperDetail, deletePaper, randomPaper, togglePublishPaper } from '@/api/exam/paper'
import { getTemplateList, getTemplateDetail } from '@/api/exam/template'
import { getAllSubjects } from '@/api/exam/subject'
import { questionTypeMap as typeMap } from '@/utils/exam-format'
import { buildSubjectLabelMap } from '@/utils/subject-label'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

defineOptions({ name: 'PaperManage' })
const headerStyle = { background: 'var(--el-fill-color)', fontWeight: 600, fontSize: '13px', color: 'var(--el-text-color-regular)', borderBottom: '2px solid var(--el-border-color)' }

const router = useRouter()
const subjects = ref<any[]>([])
// 「按需维度消歧」科目下拉显示：仅在同名科目存在时才加专业/年级后缀。
// 例：zhangwenge 教多专业「思想道德与法治」时下拉显示「思想道德与法治（计算机科学与技术）」；
//     luweizhong 只教唯一「面向对象技术」时显示纯课程名，避免冗余。
// 算法详见 @/utils/subject-label.ts。
const subjectLabelMap = computed(() => buildSubjectLabelMap(subjects.value))
const query = ref<any>({ page: 1, size: 10, paperName: '', subjectId: null })
const tableData = ref<any[]>([])
const total = ref(0)
const randomDialogVisible = ref(false)
const randomForm = ref<any>({
  paperName: '', subjectId: null, passScore: 60, duration: 120,
  rules: [{ questionType: 1, count: 10, scorePerQuestion: 3, difficulty: null }]
})
const templateList = ref<any[]>([])
const selectedTemplateId = ref<number | null>(null)
const templateTargetScore = ref<number | null>(null)

const currentTotalScore = computed(() => {
  return randomForm.value.rules.reduce((sum: number, r: any) => sum + (r.count || 0) * (r.scorePerQuestion || 0), 0)
})
const scoreMismatch = computed(() => {
  return templateTargetScore.value !== null && currentTotalScore.value !== templateTargetScore.value
})

async function loadTemplates() {
  templateList.value = await getTemplateList()
}

function openRandomDialog() {
  selectedTemplateId.value = null
  templateTargetScore.value = null
  loadTemplates()
  randomDialogVisible.value = true
}

async function applyTemplate(id: number | null) {
  if (!id) { templateTargetScore.value = null; return }
  try {
    const detail = await getTemplateDetail(id)
    randomForm.value.paperName = detail.templateName
    randomForm.value.subjectId = detail.subjectId
    randomForm.value.passScore = detail.passScore
    randomForm.value.duration = detail.duration
    templateTargetScore.value = detail.targetScore ?? null
    randomForm.value.rules = detail.rules.map((r: any) => ({
      questionType: r.questionType,
      count: r.questionCount,
      scorePerQuestion: r.scorePerQuestion,
      difficulty: r.difficulty
    }))
    ElMessage.success('已套用模板规则')
  } catch { ElMessage.error('加载模板失败') }
}

const previewVisible = ref(false)
const previewData = ref<any>(null)
const previewRef = ref()
const chineseNum = ['一','二','三','四','五','六','七','八','九','十']
const currentSemester = (() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const startYear = m >= 9 ? y : y - 1
  const sem = m >= 2 && m < 9 ? '二' : '一'
  return `${startYear}—${startYear + 1}学年第${sem}学期`
})()

const groupedPreviewQuestions = computed(() => {
  if (!previewData.value?.questions) return []
  const groups: Record<number, any> = {}
  const typeOrder = [1, 2, 3, 4, 5]
  previewData.value.questions.forEach((q: any) => {
    const t = q.questionType || 0
    if (!groups[t]) groups[t] = { type: t, items: [], totalScore: 0 }
    groups[t].items.push(q)
    groups[t].totalScore += (q.score || 0)
  })
  return typeOrder.filter(t => groups[t]).map(t => groups[t])
})

async function loadData() {
  const res = await getPaperList(query.value)
  tableData.value = res.records
  total.value = res.total
}

async function handleTogglePublish(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定${action}该试卷？${row.status === 1 ? '停用后试卷将回到草稿状态，无法被考试使用。' : '启用后试卷可被考试引用。'}`, '提示', { type: 'warning' })
  await togglePublishPaper(row.id)
  ElMessage.success(`${action}成功`)
  loadData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该试卷？', '提示', { type: 'warning' })
  await deletePaper(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleRandom() {
  if (templateTargetScore.value !== null && currentTotalScore.value !== templateTargetScore.value) {
    try {
      await ElMessageBox.confirm(
        `当前规则总分为 ${currentTotalScore.value} 分，与模板目标总分 ${templateTargetScore.value} 分不一致。确定继续生成试卷吗？`,
        '总分不一致',
        { type: 'warning', confirmButtonText: '继续生成', cancelButtonText: '返回修改' }
      )
    } catch { return }
  }
  await randomPaper(randomForm.value)
  ElMessage.success('随机组卷成功')
  randomDialogVisible.value = false
  loadData()
}

async function handlePreview(row: any) {
  const d = await getPaperDetail(row.id)
  const sub = subjects.value.find((s: any) => s.id === d.subjectId)
  d.subjectName = sub ? sub.subjectName : ''
  previewData.value = d
  previewVisible.value = true
}

const pdfExporting = ref(false)
async function exportPdf() {
  if (!previewRef.value || pdfExporting.value) return
  pdfExporting.value = true
  try {
    await nextTick()
    const el = previewRef.value as HTMLElement

    // 1. 临时移除滚动容器限制
    const scrollBody = el.closest('.el-dialog__body') as HTMLElement | null
    const savedMaxH = scrollBody?.style.maxHeight || ''
    const savedOverflow = scrollBody?.style.overflow || ''
    if (scrollBody) {
      scrollBody.style.maxHeight = 'none'
      scrollBody.style.overflow = 'visible'
    }
    await nextTick()

    // 2. 收集所有可分页元素的Y坐标（相对于previewRef顶部）
    const elRect = el.getBoundingClientRect()
    const breakableEls = el.querySelectorAll('.question-item, .question-group, .group-title')
    const breakYSet = new Set<number>([0])
    breakableEls.forEach(child => {
      const y = child.getBoundingClientRect().top - elRect.top
      if (y > 0) breakYSet.add(Math.round(y))
    })
    const totalH = el.scrollHeight
    breakYSet.add(totalH)
    const breakYList = Array.from(breakYSet).sort((a, b) => a - b)

    // 3. 渲染完整画布
    const dpr = 2
    const canvas = await html2canvas(el, {
      scale: dpr,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      height: totalH,
      windowHeight: totalH + 200
    })

    // 恢复滚动容器
    if (scrollBody) {
      scrollBody.style.maxHeight = savedMaxH
      scrollBody.style.overflow = savedOverflow
    }

    // 4. 计算A4页高对应的DOM像素高度
    const pdfW = 210
    const pdfH = 297
    const domWidth = el.scrollWidth
    const pageHInDom = domWidth * pdfH / pdfW // 一页A4在DOM坐标中的高度

    // 5. 按元素边界智能分页
    const pageSlices: Array<{ start: number; end: number }> = []
    let pageStart = 0
    while (pageStart < totalH) {
      const pageEnd = pageStart + pageHInDom
      if (pageEnd >= totalH) {
        pageSlices.push({ start: pageStart, end: totalH })
        break
      }
      // 找到不超过pageEnd的最大分页点
      let bestBreak = pageEnd
      for (let i = breakYList.length - 1; i >= 0; i--) {
        if (breakYList[i] <= pageEnd && breakYList[i] > pageStart) {
          bestBreak = breakYList[i]
          break
        }
      }
      // 防止死循环：若没有合适的分页点，强制按固定高度切
      if (bestBreak <= pageStart) bestBreak = pageEnd
      pageSlices.push({ start: pageStart, end: bestBreak })
      pageStart = bestBreak
    }

    // 6. 按切片生成PDF
    const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
    for (let i = 0; i < pageSlices.length; i++) {
      if (i > 0) pdf.addPage()
      const sy = Math.round(pageSlices[i].start * dpr)
      const ey = Math.round(pageSlices[i].end * dpr)
      const sliceH = ey - sy
      const pageCanvas = document.createElement('canvas')
      pageCanvas.width = canvas.width
      pageCanvas.height = sliceH
      const ctx = pageCanvas.getContext('2d')!
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, pageCanvas.width, pageCanvas.height)
      ctx.drawImage(canvas, 0, sy, canvas.width, sliceH, 0, 0, canvas.width, sliceH)
      const pageImg = pageCanvas.toDataURL('image/jpeg', 0.92)
      const imgH = (sliceH * pdfW) / canvas.width
      pdf.addImage(pageImg, 'JPEG', 0, 0, pdfW, imgH)
    }

    pdf.save((previewData.value?.paperName || '试卷') + '.pdf')
    ElMessage.success('PDF导出成功')
  } catch (e) {
    console.error('PDF export error:', e)
    ElMessage.error('PDF导出失败，请重试')
  } finally {
    pdfExporting.value = false
  }
}

onMounted(async () => {
  subjects.value = await getAllSubjects()
  loadTemplates()
  loadData()
})
onActivated(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  background-color: #525659;
  max-height: 80vh;
  overflow-y: auto;
}
.paper-container { display: flex; justify-content: center; padding: 30px 0; }
.paper-content {
  background: white; width: 210mm; min-height: 297mm;
  box-shadow: 0 4px 8px rgba(0,0,0,0.2); padding: 15mm 20mm 20mm 20mm;
  box-sizing: border-box; font-family: "SimSun","STSong","宋体",serif;
  color: #000; line-height: 1.8; position: relative; display: flex;
}

/* ====== 密封线区域 ====== */
.binding-area {
  position: absolute; left: 0; top: 0; bottom: 0; width: 30mm;
  border-right: 1.5px dashed #000; box-sizing: border-box;
}
.binding-info {
  writing-mode: vertical-rl;
  position: absolute; left: 2mm; top: 20mm;
  margin: 0; font-size: 12px; letter-spacing: 1px;
  white-space: nowrap; line-height: 1.5;
}
.binding-seal {
  writing-mode: vertical-rl;
  position: absolute; right: 4mm; top: 15mm; bottom: 15mm;
  margin: 0; font-size: 14px; letter-spacing: 5mm;
  display: flex; align-items: center;
}

/* ====== 试卷正文 ====== */
.paper-main-content {
  margin-left: 14mm; flex: 1;
}

/* ====== 试卷头部 ====== */
.paper-header { text-align: center; margin-bottom: 18px; border-bottom: 2px solid #000; padding-bottom: 15px; }
.school-name {
  font-size: 28px; font-weight: bold; letter-spacing: 6px;
  margin: 0 0 6px 0; font-family: "SimHei","STHeiti","黑体",sans-serif;
}
.semester-info {
  font-size: 16px; font-weight: normal; margin: 0 0 4px 0;
  font-family: "SimSun","STSong","宋体",serif; letter-spacing: 1px;
}
.paper-title {
  font-size: 20px; font-weight: bold; letter-spacing: 2px;
  margin: 6px 0 12px 0; font-family: "SimHei","STHeiti","黑体",sans-serif;
}
.exam-meta-bar {
  display: flex; justify-content: center; gap: 30px;
  font-size: 14px; margin-bottom: 15px;
}
.total-score-wrapper { display: flex; justify-content: center; }
.score-table { border-collapse: collapse; margin: 12px 0; font-size: 14px; width: 90%; }
.score-table th, .score-table td {
  border: 1.5px solid #000; padding: 5px 8px; text-align: center;
  height: 32px; min-width: 40px;
}
.score-th-label { font-weight: bold; width: 60px; background: #f5f5f5; }
.score-th-num { font-weight: normal; }
.score-td-label { font-weight: bold; background: #f5f5f5; }
.exam-notice {
  text-align: left; font-size: 13px; line-height: 1.8;
  margin-top: 10px; padding: 0 5px;
  display: flex; flex-direction: column;
}
.exam-notice strong { margin-bottom: 2px; }

/* ====== 题组标题 ====== */
.question-group { margin-bottom: 25px; }
.group-title { display: flex; align-items: center; margin-bottom: 12px; }
.group-score-box { border-collapse: collapse; margin-right: 12px; }
.group-score-box th, .group-score-box td {
  border: 1.5px solid #000; width: 48px; height: 24px;
  text-align: center; font-size: 12px; font-weight: normal;
}
.group-title-text { display: flex; align-items: baseline; }
.group-title-text h3 {
  font-size: 17px; font-weight: bold; margin: 0;
  font-family: "SimHei","STHeiti","黑体",sans-serif;
}
.group-desc {
  font-weight: normal; font-size: 14px;
  font-family: "SimSun","STSong","宋体",serif; margin-left: 5px;
}

/* ====== 题目内容 ====== */
.question-item { margin-bottom: 18px; font-size: 15px; }
.q-content { display: flex; margin-bottom: 8px; align-items: flex-start; }
.q-num { font-weight: bold; margin-right: 5px; white-space: nowrap; }
.q-text { flex: 1; }
.q-score { white-space: nowrap; font-size: 14px; }
.q-options { padding-left: 25px; display: flex; flex-direction: column; gap: 6px; }
.q-options.inline { flex-direction: row; gap: 40px; }
.opt-item { word-break: break-all; }
.q-blank, .q-answer-area { padding-left: 25px; margin-top: 8px; }
.q-answer-area { min-height: 120px; }

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
  }
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
</style>

