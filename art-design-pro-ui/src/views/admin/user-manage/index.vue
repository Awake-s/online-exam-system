<template>
  <div>
    <!-- 搜索栏 -->
    <div class="art-card flex-cb flex-wrap gap-3 px-5 py-4 mb-5 max-sm:mb-4">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input
          v-model="query.username"
          placeholder="用户名"
          clearable
          style="width: 150px"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-input
          v-model="query.realName"
          placeholder="姓名"
          clearable
          style="width: 150px"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-select
          v-model="query.roleId"
          placeholder="全部角色"
          clearable
          style="width: 120px"
          @change="loadData"
        >
          <el-option label="教师" :value="2" />
          <el-option label="学生" :value="3" />
        </el-select>
        <el-select
          v-model="query.grade"
          placeholder="全部年级"
          clearable
          style="width: 120px"
          @change="onGradeChange"
        >
          <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
        </el-select>
        <el-select
          v-model="query.majorId"
          placeholder="全部专业"
          clearable
          style="width: 120px"
          @change="onMajorChange"
        >
          <el-option v-for="m in majors" :key="m.id" :label="m.majorName" :value="m.id" />
        </el-select>
        <el-select
          v-model="query.subjectId"
          placeholder="全部科目"
          clearable
          filterable
          style="width: 200px"
          @change="loadData"
        >
          <el-option v-for="s in visibleSubjects" :key="s.id" :label="s.subjectName" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <ArtSvgIcon icon="ri:add-line" class="mr-1" />添加用户
      </ElButton>
    </div>

    <!-- 表格 -->
    <ElCard class="art-table-card" shadow="never" style="margin-bottom: 20px">
      <el-table :data="tableData" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="roleName" label="角色" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.roleId === 2 ? 'success' : row.roleId === 3 ? 'primary' : 'danger'"
              size="small"
              round
            >
              {{ row.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="160" show-overflow-tooltip />
        <!--
          负责科目列: 列定义始终存在 (固定宽度 180px), 避免筛选学生时整列卸载导致右侧列左移。
          策略 (满足用户「列隐藏 + 其他列位置不动」的双重诉求):
            - 筛选 roleId === 3 (学生): 表头 label 留空 + 单元格内容留空, 视觉上是一段 180px 空白区域,
              但右侧 手机号/状态/操作 三列绝对保持原位置 (无任何重排)
            - 其他筛选场景: 表头显示「负责科目」, 单元格显示 subjectNames; 学生行单元格留空 (其本身就没有此概念)
        -->
        <el-table-column
          prop="subjectNames"
          :label="query.roleId === 3 ? '' : '负责科目'"
          width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span v-if="query.roleId === 3 || row.roleId === 3"></span>
            <span v-else>{{ row.subjectNames }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.id === currentUserId"
              content="不能禁用当前登录的账号"
              placement="top"
            >
              <el-switch :model-value="row.status === 1" disabled size="small" />
            </el-tooltip>
            <el-switch
              v-else
              :model-value="row.status === 1"
              @change="(val: string | number | boolean) => handleStatus(row, !!val)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <ArtButtonTable type="edit" @click="openDialog(row)" />
            <ArtButtonTable
              icon="ri:lock-password-line"
              icon-class="bg-warning/12 text-warning"
              @click="handleResetPwd(row)"
            />
            <ArtButtonTable
              v-if="row.id !== currentUserId"
              type="delete"
              @click="handleDelete(row)"
            />
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @change="loadData"
        />
      </div>
    </ElCard>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" @change="onRoleChange" style="width: 100%">
            <el-option label="教师" :value="2" />
            <el-option label="学生" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.roleId === 2" label="班级" prop="classIds">
          <el-select
            v-model="form.classIds"
            multiple
            placeholder="选择负责的班级"
            style="width: 100%"
            @change="onClassesChange"
          >
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.roleId === 2" label="负责科目">
          <div v-if="!form.classIds?.length" class="candidate-empty-hint">
            请先选择任课班级, 系统会按班级年级+专业自动缩小候选范围避免错挂
          </div>
          <el-select
            v-else
            v-model="form.subjectIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :max-collapse-tags="2"
            placeholder="从下方候选课程中勾选实际任课的课程 (可多选)"
            style="width: 100%"
          >
            <el-option-group
              v-for="group in candidateSubjectGroups"
              :key="group.label"
              :label="`${group.label} (${group.options.length})`"
            >
              <el-option
                v-for="s in group.options"
                :key="s.id"
                :label="subjectLabel(s)"
                :value="s.id"
              />
            </el-option-group>
            <template v-if="!candidateSubjectGroups.length">
              <el-option disabled label="所选班级的年级/专业下暂无课程录入" value="" />
            </template>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.roleId === 3" label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="选择班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onActivated } from 'vue'
  import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
  import {
    getUserList,
    addUser,
    updateUser,
    deleteUser,
    updateUserStatus,
    resetPassword
  } from '@/api/exam/user'
  import { getAllClasses } from '@/api/exam/class'
  import { getAllSubjects } from '@/api/exam/subject'
  import { getAllMajors } from '@/api/exam/major'
  import { mittBus } from '@/utils/sys'
  import { useUserStore } from '@/store/modules/user'

  defineOptions({ name: 'UserManage' })

  const userStore = useUserStore()
  // 修复：userStore 真实字段名为 `info`（Setup 风格 store 返回的 ref 名与定义名一致）
  // Api.Auth.UserInfo 接口里 userId: number 字段存在，改用正确字段名即可
  const currentUserId = computed(() => userStore.info?.userId)

  const query = ref<any>({
    page: 1,
    size: 10,
    username: '',
    realName: '',
    roleId: null,
    grade: null,
    majorId: null,
    subjectId: null
  })
  const tableData = ref<any[]>([])
  const total = ref(0)
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const formRef = ref<FormInstance>()
  const classes = ref<any[]>([])
  const subjects = ref<any[]>([])
  const majors = ref<any[]>([])
  // 年级下拉项：从全部科目记录动态去重，与科目管理页逻辑保持一致
  const grades = ref<string[]>([])
  const form = ref<any>({
    username: '',
    password: '',
    realName: '',
    roleId: 3,
    classId: null,
    classIds: [],
    subjectIds: [],
    gender: 1,
    email: '',
    phone: ''
  })

  const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
  }

  // 科目下拉「年级 × 专业」双维联动：在上游两个下拉选定后, 科目下拉只显示
  // 同时命中 (grade, majorId) 的课程; 不选则不过滤该维。该设计与科目页的 grade+
  // majorId+courseType 筛选逻辑完全一致，避免上下文不一致造成的认知负担。
  const visibleSubjects = computed(() => {
    return subjects.value.filter((s: any) => {
      if (query.value.grade && s.grade !== query.value.grade) return false
      if (query.value.majorId && s.majorId !== query.value.majorId) return false
      return true
    })
  })

  // 切换专业时，若当前已选科目不在新专业的科目集合中，自动清空科目以避免 0 结果
  function onMajorChange() {
    syncSubjectIfStale()
    loadData()
  }

  // 切换年级时同步处理：同样需要清理不在新可选集内的 subjectId, 保证搜索参数自洽
  function onGradeChange() {
    syncSubjectIfStale()
    loadData()
  }

  // 公共工具：上游筛选变动后, 如果当前 subjectId 已不在联动后的候选集内, 置空以避免空表
  function syncSubjectIfStale() {
    if (!query.value.subjectId) return
    const allowed = visibleSubjects.value.map((s: any) => s.id)
    if (!allowed.includes(query.value.subjectId)) {
      query.value.subjectId = null
    }
  }

  // 班级候选池: 以 (班级.grade, 班级.major_id) 双键严格匹配科目(同双键)。
  // v7 KISS 减肥: 分组从 (grade, majorId, courseType) 三键降为 (grade, majorId) 二键
  //   - 原因: courseType 字段已在后端物理删除 (教务系统 SoR, OES 零依赖)
  //   - 去歧义依据: 分组标题「年级·专业」 + 标签后缀「24计科」双保险
  //   - 每组最多 38 条, filterable 搜索仍可秒定位

  // 专业名简写词典: 优先人工词典, 缺省回退前 2 字
  const MAJOR_SHORT_NAME: Record<string, string> = {
    '计算机科学与技术': '计科',
    '通信工程': '通信',
    '电子信息工程': '电子',
    '机械设计制造及其自动化': '机械',
    '电气工程及其自动化': '电气',
    '土木工程': '土木',
    '日语': '日语',
    '资源环境与城乡规划管理': '城规'
  }
  function majorShortName(majorName?: string): string {
    if (!majorName) return ''
    return MAJOR_SHORT_NAME[majorName] || majorName.slice(0, 2)
  }
  function gradeShortName(grade?: string): string {
    if (!grade) return ''
    // '2024级' -> '24'  ; '2022级' -> '22'
    return grade.replace('级', '').slice(-2)
  }

  const candidateSubjectGroups = computed(() => {
    const classIds: any[] = form.value.classIds || []
    if (classIds.length === 0 || subjects.value.length === 0) return []
    const pairs = new Set<string>()
    for (const cid of classIds) {
      const cls = classes.value.find((c: any) => c.id === cid)
      if (cls?.majorId && cls?.grade) pairs.add(`${cls.grade}|${cls.majorId}`)
    }
    if (pairs.size === 0) return []
    // key: '2024级|100' 二键
    const buckets: Record<string, any[]> = {}
    const meta: Record<string, { grade: string; majorId: number }> = {}
    for (const s of subjects.value) {
      if (!s.grade || !s.majorId) continue
      if (!pairs.has(`${s.grade}|${s.majorId}`)) continue
      const key = `${s.grade}|${s.majorId}`
      if (!buckets[key]) {
        buckets[key] = []
        meta[key] = { grade: s.grade, majorId: s.majorId }
      }
      buckets[key].push(s)
    }
    // 排序: grade 升序 → majorId 升序; 组内按 subject_id 升序 (同同专业同年级不会重名)
    return Object.keys(buckets)
      .sort((a, b) => {
        const A = meta[a], B = meta[b]
        if (A.grade !== B.grade) return A.grade.localeCompare(B.grade)
        return A.majorId - B.majorId
      })
      .map(key => {
        const m = meta[key]
        const major = majors.value.find((mj: any) => mj.id === m.majorId)
        return {
          label: `${m.grade} · ${major?.majorName || '未知专业'}`,
          options: buckets[key].sort((a: any, b: any) => a.id - b.id)
        }
      })
  })

  // 科目选项展示: 「课程名 · 24计科」
  // 后缀「24计科」是年级+专业简写兼底: collapse 后或搜索时仍能竟分同名跨专业/年级课
  // v7 KISS: 去「第N学期」后缀 (semester 字段已后端物理删除)
  function subjectLabel(s: any): string {
    if (!s) return ''
    const parts: string[] = [s.subjectName]
    const major = majors.value.find((m: any) => m.id === s.majorId)
    const g = gradeShortName(s.grade)
    const mj = majorShortName(major?.majorName)
    if (g && mj) parts.push(`${g}${mj}`)
    return parts.join(' · ')
  }

  // 班级变化后同步清理已勾选但不在新候选集内的 subjectIds, 避免后端被过滤给出合法但 UI 不一致
  function onClassesChange() {
    if (!form.value.subjectIds?.length) return
    const allowed = new Set<number>()
    for (const g of candidateSubjectGroups.value) {
      for (const s of g.options) allowed.add(s.id)
    }
    form.value.subjectIds = form.value.subjectIds.filter((id: number) => allowed.has(id))
  }

  async function loadData() {
    const res = await getUserList(query.value)
    tableData.value = res.records
    total.value = res.total
  }

  async function loadClasses() {
    classes.value = await getAllClasses()
  }

  async function openDialog(row?: any) {
    isEdit.value = !!row
    // 每次打开对话框时重新加载班级和课程列表 (课程供「负责科目」候选池包使用)
    loadClasses()
    subjects.value = await getAllSubjects()
    if (row) {
      form.value = {
        id: row.id,
        username: row.username,
        realName: row.realName,
        roleId: row.roleId,
        classId: row.classId,
        classIds: row.classIds || [],
        subjectIds: row.subjectIds || [],
        gender: row.gender || 1,
        email: row.email,
        phone: row.phone,
        status: row.status
      }
    } else {
      form.value = {
        username: '',
        password: '',
        realName: '',
        roleId: 3,
        classId: null,
        classIds: [],
        subjectIds: [],
        gender: 1,
        email: '',
        phone: ''
      }
    }
    dialogVisible.value = true
  }

  function onRoleChange() {
    form.value.classId = null
    form.value.classIds = []
    form.value.subjectIds = []
  }

  async function handleSubmit() {
    await formRef.value?.validate()
    if (isEdit.value) await updateUser(form.value)
    else await addUser(form.value)
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    loadData()
    mittBus.emit('refreshNotification')
  }

  async function handleDelete(row: any) {
    await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  }

  async function handleStatus(row: any, val: boolean) {
    try {
      await updateUserStatus(row.id, { status: val ? 1 : 0 })
      loadData()
    } catch (e: any) {
      ElMessage.error(e?.message || '操作失败')
    }
  }

  async function handleResetPwd(row: any) {
    await ElMessageBox.confirm('确定重置密码为123456？', '提示', { type: 'warning' })
    await resetPassword(row.id)
    ElMessage.success('密码已重置')
  }

  // 年级下拉项：从全部科目记录动态去重，复用已加载的 subjects，避免重复请求
  function refreshGrades() {
    const set = new Set<string>()
    subjects.value.forEach((s: any) => { if (s.grade) set.add(s.grade) })
    grades.value = Array.from(set).sort()
  }

  onMounted(async () => {
    loadData()
    loadClasses()
    subjects.value = await getAllSubjects()
    majors.value = await getAllMajors()
    refreshGrades()
  })
  onActivated(() => {
    loadData()
  })
</script>

<style lang="scss" scoped>
  // 「负责科目」候选池未就绪提示 (管理员必须先选班级)
  .candidate-empty-hint {
    width: 100%;
    padding: 8px 12px;
    background: var(--el-fill-color-light);
    color: var(--el-text-color-secondary);
    border-radius: 6px;
    line-height: 1.6;
    font-size: 13px;
  }

  :deep(.table-pagination) {
    display: flex;
    justify-content: flex-end;
    padding: 16px 4px 4px;
    .el-pagination {
      .btn-prev,
      .btn-next {
        background-color: transparent;
        border: 1px solid var(--el-border-color-light);
        border-radius: 6px;
        transition: border-color 0.15s;
        &:hover:not(.is-disabled) {
          color: var(--theme-color);
          border-color: var(--theme-color);
        }
      }
      li {
        box-sizing: border-box;
        font-weight: 400 !important;
        background-color: transparent;
        border: 1px solid var(--el-border-color-light);
        border-radius: 6px;
        transition: border-color 0.15s;
        &.is-active {
          font-weight: 500 !important;
          color: #fff;
          background-color: var(--theme-color);
          border-color: var(--theme-color);
        }
        &:hover:not(.is-disabled):not(.is-active) {
          border-color: var(--theme-color);
        }
      }
    }
  }
</style>
