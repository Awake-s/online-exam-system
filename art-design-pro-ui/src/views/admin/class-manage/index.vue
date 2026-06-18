<template>
  <div>
    <!-- 搜索栏 -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input v-model="query.className" placeholder="班级名称" clearable style="width: 200px" @clear="loadData" @keyup.enter="loadData" />
        <el-select
          v-model="query.grade"
          placeholder="全部年级"
          clearable
          style="width: 120px"
          @change="loadData"
        >
          <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
        </el-select>
        <el-select
          v-model="query.majorId"
          placeholder="全部专业"
          clearable
          style="width: 120px"
          @change="loadData"
        >
          <el-option v-for="m in majorOptions" :key="m.id" :label="m.majorName" :value="m.id" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <ArtSvgIcon icon="ri:add-line" class="mr-1" />添加班级
      </ElButton>
    </div>

    <!-- 表格 -->
    <ElCard class="art-table-card" shadow="never">
      <el-table :data="tableData" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="className" label="班级名称" min-width="140" />
        <el-table-column prop="grade" label="年级" width="120" />
        <el-table-column prop="majorName" label="所属专业" width="140" show-overflow-tooltip />
        <el-table-column prop="studentCount" label="学生人数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" round>{{ row.studentCount || 0 }}人</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherNames" label="负责教师" width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <ArtButtonTable type="view" @click="viewStudents(row)" />
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '添加班级'" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班级名称" prop="className"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" /></el-form-item>
        <el-form-item label="所属专业">
          <el-select v-model="form.majorId" filterable clearable placeholder="选择专业" style="width: 100%">
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.majorName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 班级学生对话框 -->
    <el-dialog v-model="studentDialogVisible" title="班级学生" width="600px">
      <el-table :data="students" stripe>
        <el-table-column prop="username" label="学号" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column label="性别" width="60" align="center">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getClassList, addClass, updateClass, deleteClass, getClassStudents, getAllClasses } from '@/api/exam/class'
import { getAllMajors } from '@/api/exam/major'

defineOptions({ name: 'ClassManage' })

const query = ref<any>({ page: 1, size: 10, className: '', grade: null, majorId: null })
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const studentDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const students = ref<any[]>([])
const majorOptions = ref<any[]>([])
const grades = ref<string[]>([])
const form = ref<any>({ className: '', grade: '', majorId: null, description: '' })
const rules = { className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }] }

async function loadData() {
  const res = await getClassList(query.value)
  tableData.value = res.records
  total.value = res.total
}

function openDialog(row?: any) {
  isEdit.value = !!row
  editId.value = row?.id
  form.value = row
    ? { className: row.className, grade: row.grade, majorId: row.majorId || null, description: row.description }
    : { className: '', grade: '', majorId: null, description: '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) await updateClass(editId.value!, form.value)
  else await addClass(form.value)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该班级？', '提示', { type: 'warning' })
  await deleteClass(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function viewStudents(row: any) {
  students.value = await getClassStudents(row.id)
  studentDialogVisible.value = true
}

async function loadMajors() {
  try { majorOptions.value = await getAllMajors() } catch { /* ignore */ }
}

// 年级下拉项：从全量班级表动态去重，避免硬编码（数据库新增年级自动出现）
async function loadGrades() {
  try {
    const list: any[] = await getAllClasses()
    const set = new Set<string>()
    list.forEach((c: any) => { if (c.grade) set.add(c.grade) })
    grades.value = Array.from(set).sort()
  } catch { /* ignore */ }
}

onMounted(() => { loadData(); loadMajors(); loadGrades() })
onActivated(() => { loadData() })
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
