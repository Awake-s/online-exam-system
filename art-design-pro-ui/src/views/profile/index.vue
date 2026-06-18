<template>
  <div class="w-full h-full p-0 bg-transparent border-none shadow-none">
    <div class="relative flex-b mt-2.5 max-md:block max-md:mt-1">
      <!-- 左侧用户卡片 -->
      <div class="w-112 mr-5 max-md:w-full max-md:mr-0">
        <div class="art-card-sm relative p-9 pb-6 overflow-hidden text-center">
          <img class="absolute top-0 left-0 w-full h-50 object-cover" src="@imgs/user/bg.webp" />
          <img
            class="relative z-10 w-20 h-20 mt-30 mx-auto object-cover border-2 border-white rounded-full cursor-pointer"
            :src="profileForm.avatar || defaultAvatar"
            @click="triggerFileInput"
          />
          <input ref="fileInputRef" type="file" accept="image/*" class="hidden" @change="handleFileChange" />
          <h2 class="mt-5 text-xl font-normal">{{ userStore.info?.userName }}</h2>
          <p class="mt-2 text-sm text-g-600">{{ roleName }}</p>

          <div class="w-75 mx-auto mt-7.5 text-left">
            <div class="mt-2.5">
              <ArtSvgIcon icon="ri:user-3-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ profileForm.realName || '-' }}</span>
            </div>
            <div class="mt-2.5">
              <ArtSvgIcon icon="ri:mail-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ profileForm.email || '-' }}</span>
            </div>
            <div class="mt-2.5">
              <ArtSvgIcon icon="ri:phone-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ profileForm.phone || '-' }}</span>
            </div>
            <div v-if="userStore.info?.className" class="mt-2.5">
              <ArtSvgIcon icon="ri:team-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ userStore.info.className }}</span>
            </div>
          </div>

          <div class="mt-10">
            <h3 class="text-sm font-medium">标签</h3>
            <div class="flex flex-wrap justify-center mt-3.5">
              <div
                v-for="tag in userTags"
                :key="tag"
                class="py-1 px-1.5 mr-2.5 mb-2.5 text-xs border border-g-300 rounded"
              >
                {{ tag }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区域 -->
      <div class="flex-1 overflow-hidden max-md:w-full max-md:mt-3.5">
        <div class="art-card-sm">
          <h1 class="p-4 text-xl font-normal border-b border-g-300">基本设置</h1>

          <ElForm
            :model="profileForm"
            ref="profileFormRef"
            :rules="profileRules"
            class="box-border p-5 [&>.el-row_.el-form-item]:w-[calc(50%-10px)] [&>.el-row_.el-input]:w-full [&>.el-row_.el-select]:w-full"
            label-width="86px"
            label-position="top"
          >
            <ElRow>
              <ElFormItem label="真实姓名" prop="realName">
                <ElInput v-model="profileForm.realName" :disabled="!isEdit" />
              </ElFormItem>
              <ElFormItem label="性别" prop="gender" class="ml-5">
                <ElSelect v-model="profileForm.gender" placeholder="请选择" :disabled="!isEdit">
                  <ElOption :value="1" label="男" />
                  <ElOption :value="2" label="女" />
                </ElSelect>
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="邮箱" prop="email">
                <ElInput v-model="profileForm.email" :disabled="!isEdit" />
              </ElFormItem>
              <ElFormItem label="手机号" prop="phone" class="ml-5">
                <ElInput v-model="profileForm.phone" :disabled="!isEdit" />
              </ElFormItem>
            </ElRow>

            <div class="flex-c justify-end [&_.el-button]:!w-27.5">
              <ElButton type="primary" v-ripple @click="toggleEdit">
                {{ isEdit ? '保存' : '编辑' }}
              </ElButton>
            </div>
          </ElForm>
        </div>

        <div class="art-card-sm my-5">
          <h1 class="p-4 text-xl font-normal border-b border-g-300">更改密码</h1>

          <ElForm
            :model="pwdForm"
            ref="pwdFormRef"
            :rules="pwdRules"
            class="box-border p-5"
            label-width="86px"
            label-position="top"
          >
            <ElFormItem label="当前密码" prop="oldPassword">
              <ElInput v-model="pwdForm.oldPassword" type="password" :disabled="!isEditPwd" show-password />
            </ElFormItem>

            <ElFormItem label="新密码" prop="newPassword">
              <ElInput v-model="pwdForm.newPassword" type="password" :disabled="!isEditPwd" show-password />
            </ElFormItem>

            <ElFormItem label="确认新密码" prop="confirmPassword">
              <ElInput v-model="pwdForm.confirmPassword" type="password" :disabled="!isEditPwd" show-password />
            </ElFormItem>

            <div class="flex-c justify-end [&_.el-button]:!w-27.5">
              <ElButton type="primary" v-ripple @click="toggleEditPwd">
                {{ isEditPwd ? '保存' : '编辑' }}
              </ElButton>
            </div>
          </ElForm>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { updateProfile, changePassword, uploadAvatar } from '@/api/exam/profile'
import { fetchGetUserInfo } from '@/api/auth'

defineOptions({ name: 'Profile' })

const userStore = useUserStore()
const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="50" fill="%23ddd"/><circle cx="50" cy="38" r="18" fill="%23bbb"/><ellipse cx="50" cy="80" rx="30" ry="22" fill="%23bbb"/></svg>'

const isEdit = ref(false)
const isEditPwd = ref(false)
const profileFormRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const fileInputRef = ref<HTMLInputElement>()
const profileForm = ref<any>({ realName: '', email: '', phone: '', gender: 1, avatar: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const roleMap: Record<string, string> = {
  R_ADMIN: '系统管理员',
  R_TEACHER: '教师',
  R_STUDENT: '学生'
}
const roleName = computed(() => {
  const roles = userStore.info?.roles || []
  return roles.map((r: string) => roleMap[r] || r).join('、') || '-'
})

const tagMap: Record<string, string[]> = {
  R_ADMIN: ['系统管理', '用户管理', '数据维护'],
  R_TEACHER: ['出题组卷', '考试管理', '阅卷批改', '成绩分析'],
  R_STUDENT: ['在线考试', '成绩查询', '错题复习']
}
const userTags = computed(() => {
  const roles = userStore.info?.roles || []
  for (const r of roles) {
    if (tagMap[r]) return tagMap[r]
  }
  return []
})

const profileRules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号码', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value && value !== pwdForm.value.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function loadProfile() {
  const res: any = await fetchGetUserInfo()
  profileForm.value = {
    realName: res.realName,
    email: res.email,
    phone: res.phone,
    gender: res.gender || 1,
    avatar: res.avatar || ''
  }
}

async function toggleEdit() {
  if (isEdit.value) {
    await profileFormRef.value?.validate()
    await updateProfile(profileForm.value)
    ElMessage.success('更新成功')
  }
  isEdit.value = !isEdit.value
}

async function toggleEditPwd() {
  if (isEditPwd.value) {
    await pwdFormRef.value?.validate()
    await changePassword(pwdForm.value)
    ElMessage.success('密码修改成功')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  }
  isEditPwd.value = !isEditPwd.value
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function syncAvatarToStore(avatarUrl: string) {
  if (userStore.info) {
    ;(userStore.info as any).avatar = avatarUrl
  }
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 通过后端API上传头像到服务器
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await uploadAvatar(formData)
    const avatarUrl = res.avatarUrl
    profileForm.value.avatar = avatarUrl
    syncAvatarToStore(avatarUrl)
    ElMessage.success('头像更新成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '头像上传失败')
  }

  // 重置 input 以允许重复选择同一文件
  input.value = ''
}

onMounted(loadProfile)
</script>

