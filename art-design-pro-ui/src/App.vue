<template>
  <ElConfigProvider size="default" :locale="locales[language]" :z-index="3000">
    <RouterView></RouterView>
  </ElConfigProvider>
</template>

<script setup lang="ts">
  import { useUserStore } from './store/modules/user'
  import zh from 'element-plus/es/locale/lang/zh-cn'
  import en from 'element-plus/es/locale/lang/en'
  import { systemUpgrade } from './utils/sys'
  import { toggleTransition } from './utils/ui/animation'
  import { checkStorageCompatibility } from './utils/storage'
  import { initializeTheme } from './hooks/core/useTheme'
  import { onMounted, onUnmounted, watch } from 'vue'
  import { useNotificationWebSocketStore } from '@/store/modules/notificationWebSocket'
  import { useChatStore } from '@/store/modules/chat'


  const userStore = useUserStore()
  const { language } = storeToRefs(userStore)
  const notificationWsStore = useNotificationWebSocketStore()
  const chatStore = useChatStore()


  const locales = {
    zh: zh,
    en: en
  }

  onBeforeMount(() => {
    toggleTransition(true)
    initializeTheme()
  })

  onMounted(() => {
    checkStorageCompatibility()
    toggleTransition(false)
    systemUpgrade()
  })
  onMounted(() => {
  // 用户登录后连接 WebSocket
  if (userStore.accessToken) {
    console.log('🚀 初始化通知 WebSocket 连接')
    notificationWsStore.connect()
    notificationWsStore.requestNotificationPermission()
  }

  // 监听页面可见性变化（移动端优化）
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  notificationWsStore.disconnect()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

// 监听用户登录状态变化
watch(() => userStore.accessToken, (newToken, oldToken) => {
  if (newToken && !oldToken) {
    // 用户刚登录，连接 WebSocket
    notificationWsStore.connect()
    notificationWsStore.requestNotificationPermission()
  } else if (!newToken && oldToken) {
    // 用户退出登录，断开所有 WebSocket 连接
    notificationWsStore.disconnect()
    chatStore.reset()
  }
})

// 页面可见性变化处理（移动端优化）
const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    // 页面重新可见时，检查连接状态
    if (userStore.accessToken && !notificationWsStore.connected) {
      console.log('📱 页面重新可见，重新连接 WebSocket')
      notificationWsStore.connect()
    }
  }
}
</script>
