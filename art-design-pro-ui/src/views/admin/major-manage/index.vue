<template>
  <div>
    <!-- 搜索栏 -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3">
        <el-input v-model="query.majorName" placeholder="专业名称" clearable style="width: 200px" @clear="loadData" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <ArtSvgIcon icon="ri:add-line" class="mr-1" />添加专业
      </ElButton>
    </div>

    <!-- 表格 -->
    <ElCard class="art-table-card" shadow="never" style="margin-bottom: 20px">
      <el-table :data="tableData" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="majorName" label="专业名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ').substring(0, 19) : '' }}
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑专业' : '添加专业'" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="专业名称" prop="majorName"><el-input v-model="form.majorName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
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
import { getMajorList, addMajor, updateMajor, deleteMajor } from '@/api/exam/major'

defineOptions({ name: 'MajorManage' })

const query = ref<any>({ page: 1, size: 10, majorName: '' })
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref<any>({ majorName: '', description: '' })
const rules = { majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }] }

async function loadData() {
  const res = await getMajorList(query.value)
  tableData.value = res.records
  total.value = res.total
}

function openDialog(row?: any) {
  isEdit.value = !!row
  editId.value = row?.id
  form.value = row
    ? { majorName: row.majorName, description: row.description }
    : { majorName: '', description: '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) await updateMajor(editId.value!, form.value)
  else await addMajor(form.value)
  ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该专业？', '提示', { type: 'warning' })
  await deleteMajor(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
onActivated(loadData)
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
