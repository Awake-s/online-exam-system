<!-- 登录页面 -->
<template>
  <div class="login-page-bg">
    <AuthTopBar />

    <div class="login-card">
      <LoginLeftView />

      <div class="login-card-right">
        <div class="form">
          <h3 class="title">{{ $t('login.title') }}</h3>
          <p class="sub-title">{{ $t('login.subTitle') }}</p>
          <ElForm
            ref="formRef"
            :model="formData"
            :rules="rules"
            :key="formKey"
            @keyup.enter="handleSubmit"
            style="margin-top: 20px"
          >
            <!-- 账号角色选择（和 art-design-pro 原版一致的下拉选择器） -->
            <ElFormItem prop="account">
              <ElSelect v-model="formData.account" @change="setupAccount">
                <ElOption
                  v-for="account in accounts"
                  :key="account.key"
                  :label="account.label"
                  :value="account.key"
                >
                  <span>{{ account.label }}</span>
                </ElOption>
              </ElSelect>
            </ElFormItem>
            <ElFormItem prop="username">
              <ElInput
                class="custom-height"
                :placeholder="$t('login.placeholder.username')"
                v-model.trim="formData.username"
              />
            </ElFormItem>
            <ElFormItem prop="password">
              <ElInput
                class="custom-height"
                :placeholder="$t('login.placeholder.password')"
                v-model.trim="formData.password"
                type="password"
                autocomplete="off"
                show-password
              />
            </ElFormItem>

            <!-- 拖拽验证（保留 art-design-pro 原版风格） -->
            <div class="relative pb-5 mt-2">
              <div
                class="relative z-[2] overflow-hidden select-none rounded-[var(--el-border-radius-base)] border border-[var(--el-border-color)] tad-300"
                :class="{ '!border-[#FF4E4F]': !isPassing && isClickPass }"
              >
                <ArtDragVerify
                  ref="dragVerify"
                  v-model:value="isPassing"
                  :text="$t('login.sliderText')"
                  textColor="var(--art-gray-700)"
                  :successText="$t('login.sliderSuccessText')"
                  progressBarBg="var(--main-color)"
                  :background="isDark ? '#26272F' : '#F1F1F4'"
                  handlerBg="var(--default-box-color)"
                />
              </div>
              <p
                class="absolute top-0 z-[1] px-px mt-2 text-xs text-[#f56c6c] tad-300"
                :class="{ 'translate-y-10': !isPassing && isClickPass }"
              >
                {{ $t('login.placeholder.slider') }}
              </p>
            </div>

            <div class="flex-cb mt-5 text-sm">
              <ElCheckbox v-model="formData.rememberPassword">{{
                $t('login.rememberPwd')
              }}</ElCheckbox>
            </div>

            <div style="margin-top: 16px">
              <ElButton
                class="w-full custom-height"
                type="primary"
                @click="handleSubmit"
                :loading="loading"
                v-ripple
              >
                {{ $t('login.btnText') }}
              </ElButton>
            </div>
          </ElForm>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import AppConfig from '@/config'
  import { useUserStore } from '@/store/modules/user'
  import { useI18n } from 'vue-i18n'
  import { HttpError } from '@/utils/http/error'
  import { fetchLogin } from '@/api/auth'
  import { ElNotification, type FormInstance, type FormRules } from 'element-plus'
  import { useSettingStore } from '@/store/modules/setting'

  defineOptions({ name: 'Login' })

  const settingStore = useSettingStore()
  const { isDark } = storeToRefs(settingStore)
  const { t, locale } = useI18n()
  const formKey = ref(0)

  watch(locale, () => {
    formKey.value++
  })

  // 角色码到路由角色的映射
  const roleMap: Record<string, string[]> = {
    ADMIN: ['R_ADMIN'],
    TEACHER: ['R_TEACHER'],
    STUDENT: ['R_STUDENT']
  }

  // 角色码到首页的映射
  const roleHomeMap: Record<string, string> = {
    ADMIN: '/admin-home',
    TEACHER: '/teacher-home',
    STUDENT: '/student-home'
  }

  type AccountKey = 'admin' | 'teacher' | 'student'

  interface Account {
    key: AccountKey
    label: string
    userName: string
    password: string
  }

  const accounts = computed<Account[]>(() => [
    {
      key: 'admin',
      label: t('login.roles.admin'),
      userName: 'admin',
      password: 'admin123'
    },
    {
      key: 'teacher',
      label: t('login.roles.teacher'),
      userName: 'luweizhong',
      password: '123456'
    },
    {
      key: 'student',
      label: t('login.roles.student'),
      userName: '2130107203',
      password: '123456'
    }
  ])

  const dragVerify = ref()

  const userStore = useUserStore()
  const router = useRouter()
  const route = useRoute()
  const isPassing = ref(false)
  const isClickPass = ref(false)

  const systemName = AppConfig.systemInfo.name
  const formRef = ref<FormInstance>()

  const formData = reactive({
    account: '' as string,
    username: '',
    password: '',
    rememberPassword: true
  })

  const rules = computed<FormRules>(() => ({
    username: [{ required: true, message: t('login.placeholder.username'), trigger: 'blur' }],
    password: [{ required: true, message: t('login.placeholder.password'), trigger: 'blur' }]
  }))

  const loading = ref(false)

  onMounted(() => {
    setupAccount('admin')
  })

  const setupAccount = (key: AccountKey) => {
    const selectedAccount = accounts.value.find((account: Account) => account.key === key)
    formData.account = key
    formData.username = selectedAccount?.userName ?? ''
    formData.password = selectedAccount?.password ?? ''
  }

  const handleSubmit = async () => {
    if (!formRef.value) return

    try {
      const valid = await formRef.value.validate()
      if (!valid) return

      if (!isPassing.value) {
        isClickPass.value = true
        return
      }

      loading.value = true

      const { username, password } = formData

      const loginResult = await fetchLogin({
        username,
        password
      } as any)

      const { token, userInfo } = loginResult

      if (!token) {
        throw new Error('登录失败 - 未获取到令牌')
      }

      userStore.setToken(`Bearer ${token}`)
      userStore.setLoginStatus(true)

      const roleCode = userInfo.roleCode || 'STUDENT'
      userStore.setUserInfo({
        userId: userInfo.id,
        userName: userInfo.username,
        realName: userInfo.realName,
        roleId: userInfo.roleId ?? 0,
        roleCode: roleCode,
        roles: roleMap[roleCode] || ['R_STUDENT'],
        buttons: [],
        email: userInfo.email,
        avatar: userInfo.avatar,
        classId: userInfo.classId,
        className: userInfo.className,
        phone: userInfo.phone
      })

      showLoginSuccessNotice(userInfo.realName)

      const redirect = route.query.redirect as string
      router.push(redirect || roleHomeMap[roleCode] || '/')
    } catch (error) {
      if (error instanceof HttpError) {
        console.error('[Login] HttpError:', error.message)
      } else {
        console.error('[Login] Unexpected error:', error)
      }
    } finally {
      loading.value = false
      resetDragVerify()
    }
  }

  const resetDragVerify = () => {
    dragVerify.value?.reset()
  }

  const showLoginSuccessNotice = (realName: string) => {
    setTimeout(() => {
      ElNotification({
        title: t('login.success.title'),
        type: 'success',
        duration: 2500,
        zIndex: 10000,
        message: `欢迎回来，${realName}！`
      })
    }, 1000)
  }
</script>

<style scoped>
  @import './style.css';
</style>

<style lang="scss" scoped>
  :deep(.el-select__wrapper) {
    height: 42px !important;
  }
</style>
