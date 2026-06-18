<template>
  <div>
    <!-- 搜索栏 -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input v-model="query.subjectName" placeholder="课程名称" clearable
          style="width: 180px" @clear="loadData" @keyup.enter="loadData" />
        <el-select v-model="query.grade" placeholder="全部年级" clearable
          style="width: 120px" @change="loadData">
          <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
        </el-select>
        <el-select v-model="query.majorId" placeholder="全部专业" clearable filterable
          style="width: 200px" @change="loadData">
          <el-option v-for="m in majorOptions" :key="m.id" :label="m.majorName" :value="m.id" />
        </el-select>
        <!-- v7 KISS 减肥: 删“全部课程类型”筛选 (教务化字段在 OES 中零依赖) -->
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <ArtSvgIcon icon="ri:add-line" class="mr-1" />添加课程
      </ElButton>
    </div>

    <!-- 表格 -->
    <ElCard class="art-table-card" shadow="never" style="margin-bottom: 20px">
      <el-table :data="tableData" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="grade" label="年级" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.grade" size="small" type="info" effect="plain" round>{{ row.grade }}</el-tag>
            <span v-else class="text-gray-400">通用</span>
          </template>
        </el-table-column>
        <el-table-column prop="majorName" label="所属专业" min-width="150" show-overflow-tooltip />
        <el-table-column prop="subjectName" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <!-- v7 KISS 减肥: 删 courseType / credit / semester / hours / examType 多列
             理由: 学分/学期/课程类型是教务系统的 SoR, OES 重复维护只会数据漂移;
             需要展示可写在「描述」自由文本里 -->
        <el-table-column prop="questionCount" label="题目数" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" round>{{ row.questionCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任课教师" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="row.teacherNames?.length">
              <el-tag v-for="name in row.teacherNames" :key="name" size="small" type="success" effect="plain" round class="mr-1 mb-0.5">{{ name }}</el-tag>
            </template>
            <span v-else></span>
          </template>
        </el-table-column>
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
    </ElCard>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑课程' : '添加课程'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="课程名称" prop="subjectName" class="col-span-2">
            <el-input v-model="form.subjectName" placeholder="如：数据结构、Java EE 开发技术基础" />
          </el-form-item>
          <el-form-item label="年级" prop="grade">
            <el-select v-model="form.grade" placeholder="如 2022级" filterable allow-create style="width: 100%">
              <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属专业" prop="majorId">
            <el-select v-model="form.majorId" placeholder="选择专业" filterable clearable style="width: 100%">
              <el-option v-for="m in majorOptions" :key="m.id" :label="m.majorName" :value="m.id" />
            </el-select>
          </el-form-item>
          <!-- v7 KISS 减肥: 删课程类型/学分/开课学期 3 表单项
               理由: 这些是教务系统的权威字段，OES 重复采集只会与教务系统漂移;
               如需记录供参考，写在下方「描述」自由文本里即可 -->
          <el-form-item label="描述" class="col-span-2">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选。推荐 6 字段格式：[课程类型] · [专业简称][年级]第N学期 · [学分]学分/[学时]学时 · [核心内容]&#10;例：专业核心必修 · 计科24级第5学期 · 4学分/72学时 · SSM/Spring 框架" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getSubjectList, addSubject, updateSubject, deleteSubject, getAllSubjects } from '@/api/exam/subject'
import { getAllMajors } from '@/api/exam/major'
import { getAllClasses } from '@/api/exam/class'

defineOptions({ name: 'SubjectManage' })

// v7 KISS 减肥: 删 courseTypes 常量 + courseTypeTag 函数 (教务化字段全面去除)

const query = ref<any>({ page: 1, size: 10, subjectName: '', grade: null, majorId: null })
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const majorOptions = ref<any[]>([])
const grades = ref<string[]>([])
const form = ref<any>({
  subjectName: '', description: '',
  grade: '', majorId: null,
  hours: 0, examType: '试'  // hours/examType 仅为后端兑现提交，不暴露表单输入
})
const rules = {
  subjectName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

async function loadData() {
  const res = await getSubjectList(query.value)
  tableData.value = res.records
  total.value = res.total
}

async function loadMajors() {
  try { majorOptions.value = await getAllMajors() } catch { /* ignore */ }
}

// 年级下拉项：科目 + 班级两处去重，避免仅基础库时科目 grade 为空导致「无数据」
async function loadGrades() {
  const set = new Set<string>()
  try {
    const list: any[] = await getAllSubjects()
    list.forEach((s: any) => { if (s.grade) set.add(s.grade) })
  } catch { /* ignore */ }
  try {
    const classes: any[] = await getAllClasses()
    classes.forEach((c: any) => { if (c.grade) set.add(c.grade) })
  } catch { /* ignore */ }
  grades.value = Array.from(set).sort()
}

function openDialog(row?: any) {
  isEdit.value = !!row
  editId.value = row?.id
  form.value = row
    ? {
        subjectName: row.subjectName,
        description: row.description,
        grade: row.grade,
        majorId: row.majorId,
        hours: row.hours ?? 0,
        examType: '试'  // 后端兑现提交, 统一为 '试'
      }
    : {
        subjectName: '', description: '',
        grade: '', majorId: null,
        hours: 0, examType: '试'
      }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) await updateSubject(editId.value!, form.value)
  else await addSubject(form.value)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  loadData()
  loadGrades()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该课程？删除后该课程下的题目将无法被操作。', '提示', { type: 'warning' })
  await deleteSubject(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => { loadData(); loadMajors(); loadGrades() })
onActivated(() => { loadData(); loadMajors(); loadGrades() })
</script>

<style lang="scss" scoped>
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
