<!-- 系统聊天窗口 -->
<template>
  <div>
    <!-- Fin/Intercom 风格：图片灯箱预览 -->
    <Teleport to="body">
      <transition name="lightbox">
        <div v-if="lightboxVisible" class="lightbox-overlay" @click.self="closeLightbox">
          <button class="lightbox-close" @click="closeLightbox"><ArtSvgIcon icon="ri:close-line" /></button>
          <img :src="lightboxUrl" class="lightbox-image" @click.stop />
          <a :href="lightboxUrl" :download="lightboxUrl.split('/').pop()" class="lightbox-download" @click.stop>
            <ArtSvgIcon icon="ri:download-2-line" />
          </a>
        </div>
      </transition>
    </Teleport>
    <transition name="chat-overlay">
      <div v-if="isDrawerVisible" class="chat-overlay" @click="closeChat"></div>
    </transition>
    <transition name="chat-panel">
      <div v-if="isDrawerVisible" :class="['chat-panel', isMobile && 'chat-panel-mobile']">
        <div class="chat-root">
          <div class="chat-header">
            <template v-if="currentView === 'chat'">
              <div class="flex-c gap-2.5">
                <div class="back-btn" @click="backToList"><ArtSvgIcon icon="ri:arrow-left-s-line" class="text-lg" /></div>
                <ElAvatar :size="36" :src="getAvatar(chatPartner?.otherAvatar, chatPartner?.otherUserName)" class="chat-avatar-ring" />
                <div>
                  <div class="header-title text-[14px]">{{ chatPartner?.otherUserName || '聊天' }}</div>
                  <!-- L3-M0-6：typing 状态最高优先级（对齐 WhatsApp/微信 header 设计）-->
                  <div v-if="isPartnerTyping" class="flex-c gap-1 mt-0.5 header-typing">
                    <span class="typing-dots">
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                    </span>
                    <span class="header-status-text typing-text">正在输入...</span>
                  </div>
                  <div v-else class="flex-c gap-1 mt-0.5">
                    <span :class="partnerOnline ? 'online-dot' : 'offline-dot'"></span>
                    <span :class="['header-status-text', partnerOnline ? 'status-online' : 'status-offline']">{{ partnerOnline ? '在线' : '离线' }}</span>
                  </div>
                </div>
              </div>
              <div class="header-action-btn" @click="closeChat">
                <ArtSvgIcon icon="ri:close-line" class="text-[16px]" />
              </div>
            </template>
            <template v-else>
              <div class="header-spacer"></div>
              <div class="header-title">{{ spaceTitle }}</div>
              <div class="header-action-btn" @click="closeChat">
                <ArtSvgIcon icon="ri:close-line" class="text-[16px]" />
              </div>
            </template>
          </div>
          <div class="space-content-area" ref="spaceContentRef" @mouseenter="csHover = true; updateScrollThumb()" @mouseleave="csHover = false">
          <!-- ====== Space 内容切换（crossfade + fade-up） ====== -->
          <transition name="space-fade" mode="out-in">
            <!-- ① 主页 Space -->
            <div v-if="activeSpace === 'home' && currentView !== 'chat'" key="home" class="space-view">
              <div class="home-space">
                <div class="home-card stagger-item" :style="{ animationDelay: '0ms' }" @click="switchSpace('contacts')">
                  <div class="home-card-icon-wrap">
                    <ArtSvgIcon icon="ri:chat-new-line" class="home-card-icon" />
                  </div>
                  <div class="home-card-content">
                    <div class="home-card-title">发起聊天</div>
                    <div class="home-card-desc">选择联系人开始对话</div>
                  </div>
                  <ArtSvgIcon icon="ri:arrow-right-s-line" class="home-card-arrow" />
                </div>
                <!-- CHAT-325：首页"最近的消息"同样合并 ghost 卡片，让刚打字未发的草稿立即可见 -->
                <div v-if="displayConversations.length > 0" class="home-recent-card stagger-item" :style="{ animationDelay: '60ms' }">
                  <div class="home-section-title">最近的消息</div>
                  <div v-for="(conv, ci) in displayConversations.slice(0, 3)" :key="conv.id"
                    class="home-conv-item" @click="openConversation(conv)">
                    <div class="relative shrink-0">
                      <ElAvatar :size="40" :src="getAvatar(conv.otherAvatar, conv.otherUserName)" class="conv-avatar" />
                      <transition name="badge-pop">
                        <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
                      </transition>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex-cb">
                        <span class="conv-name truncate">{{ conv.otherUserName }}</span>
                        <span class="conv-time shrink-0 ml-2">{{ formatTime(conv.lastMessageTime) }}</span>
                      </div>
                      <!-- CHAT-325：ghost 会话也能正确渲染"[草稿]"前缀（hasDraftPreview 按 peerUserId 判定）-->
                      <div v-if="hasDraftPreview(conv)" class="conv-preview truncate mt-1">
                        <span class="conv-draft-prefix">[草稿]</span> {{ getDraftPreview(conv) }}
                      </div>
                      <div v-else class="conv-preview truncate mt-1">{{ formatLastMessage(conv) }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- ② 消息 Space -->
            <div v-else-if="activeSpace === 'messages' && currentView !== 'chat'" key="messages" class="space-view">
              <!-- 骨架屏 -->
              <div v-if="!spaceLoaded.messages" class="skeleton-list">
                <div v-for="i in 5" :key="i" class="skeleton-row">
                  <div class="skeleton-avatar"></div>
                  <div class="skeleton-text">
                    <div class="skeleton-line skeleton-line-name"></div>
                    <div class="skeleton-line skeleton-line-msg"></div>
                  </div>
                </div>
              </div>
              <template v-else>
                <!-- L3-M1-4 + CHAT-325：空状态判定同时考虑 display（含 ghost）+ 归档列表
                     CHAT-325：用 displayConversations 让 ghost 草稿存在时不误判为"暂无" -->
                <div v-if="displayConversations.length === 0 && chatStore.archivedConversations.length === 0" class="empty-state">
                  <div class="empty-icon-circle">
                    <ArtSvgIcon icon="ri:message-3-line" class="text-3xl text-theme" />
                  </div>
                  <div class="empty-title">暂无聊天记录</div>
                  <div class="empty-subtitle">点击右下角按钮发起新的聊天</div>
                </div>
                <div v-else class="msg-list">
                  <!-- L3-M1-4：已归档会话折叠卡片（Telegram Archive 模式）
                       仅当存在归档会话时显示；点击展开/折叠归档子列表 -->
                  <div v-if="chatStore.archivedConversations.length > 0" class="archive-card">
                    <div class="archive-card-header" @click="archiveExpanded = !archiveExpanded">
                      <div class="archive-icon-wrap">
                        <ArtSvgIcon icon="ri:archive-2-line" class="archive-icon" />
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="flex-cb">
                          <span class="archive-title">已归档会话</span>
                          <span class="archive-count">{{ chatStore.archivedConversations.length }}</span>
                        </div>
                        <div class="archive-subtitle truncate">{{ archivedSubtitle }}</div>
                      </div>
                      <ArtSvgIcon :icon="archiveExpanded ? 'ri:arrow-up-s-line' : 'ri:arrow-down-s-line'" class="archive-chevron" />
                    </div>
                    <transition name="archive-expand">
                      <div v-if="archiveExpanded" class="archive-list">
                        <div v-for="conv in chatStore.archivedConversations" :key="'arch-' + conv.id"
                          class="msg-conv-card msg-conv-card--archived"
                          @click="openConversation(conv)"
                          @contextmenu.prevent="onConvContextMenu($event, conv, true)">
                          <div class="relative shrink-0">
                            <ElAvatar :size="42" :src="getAvatar(conv.otherAvatar, conv.otherUserName)" class="conv-avatar" />
                          </div>
                          <div class="flex-1 min-w-0">
                            <div class="flex-cb">
                              <div class="flex-c gap-1.5 min-w-0">
                                <span class="conv-name truncate">{{ conv.otherUserName }}</span>
                                <span v-if="conv.otherClassName && conv.otherRoleId === 3" class="conv-class-tag">{{ conv.otherClassName }}</span>
                              </div>
                              <span class="conv-time shrink-0 ml-2">{{ formatTime(conv.lastMessageTime) }}</span>
                            </div>
                            <div class="conv-preview truncate mt-1">{{ formatLastMessage(conv) }}</div>
                          </div>
                        </div>
                      </div>
                    </transition>
                  </div>
                  <!-- 主会话列表 - CHAT-325：改用 displayConversations（真实 + ghost）
                       ghost 卡片只能点开，不能右键（由 onConvContextMenu 内部防守）-->
                  <div v-for="conv in displayConversations" :key="conv.id"
                    :class="['msg-conv-card', conv.pinned && 'msg-conv-card--pinned', conv.isGhost && 'msg-conv-card--ghost']"
                    @click="openConversation(conv)"
                    @contextmenu.prevent="onConvContextMenu($event, conv, false)">
                    <div class="relative shrink-0">
                      <ElAvatar :size="42" :src="getAvatar(conv.otherAvatar, conv.otherUserName)" class="conv-avatar" />
                      <!-- L3-M0-7：未读角标 — muted 时置灰（对齐 WhatsApp "消息进入但不打扰"语义） -->
                      <transition name="badge-pop">
                        <span v-if="conv.unreadCount > 0"
                          :class="['unread-badge', conv.muted && 'unread-badge--muted']">
                          {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
                        </span>
                      </transition>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex-cb">
                        <div class="flex-c gap-1.5 min-w-0">
                          <span class="conv-name truncate">{{ conv.otherUserName }}</span>
                          <span v-if="conv.otherClassName && conv.otherRoleId === 3" class="conv-class-tag">{{ conv.otherClassName }}</span>
                        </div>
                        <div class="flex-c gap-1 shrink-0 ml-2">
                          <!-- L3-M0-7：免打扰图标（WhatsApp/微信 静音标识） -->
                          <ArtSvgIcon v-if="conv.muted" icon="ri:notification-off-line" class="conv-mute-icon" title="已免打扰" />
                          <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
                        </div>
                      </div>
                      <!-- L3-M0-8：草稿预览 — 微信红色前缀"[草稿]"对齐业界设计 -->
                      <div v-if="hasDraftPreview(conv)" class="conv-preview truncate mt-1">
                        <span class="conv-draft-prefix">[草稿]</span> {{ getDraftPreview(conv) }}
                      </div>
                      <div v-else class="conv-preview truncate mt-1">{{ formatLastMessage(conv) }}</div>
                    </div>
                    <!-- L3-M0-7：置顶角标（右上角 pin 图标，Telegram 风格） -->
                    <div v-if="conv.pinned" class="conv-pin-badge" title="已置顶">
                      <ArtSvgIcon icon="ri:pushpin-fill" />
                    </div>
                  </div>
                </div>
                <!-- L3：会话右键菜单（根据是否归档显示不同操作）
                     L3-bugfix：ref 供 onClickOutside 精确判断"点击外部"（根治一次性激活 bug）-->
                <div v-if="convContextMenu.visible"
                  ref="convContextMenuRef"
                  class="conv-context-menu"
                  :style="{ left: convContextMenu.x + 'px', top: convContextMenu.y + 'px' }"
                  @click.stop>
                  <!-- 归档列表里的会话：只显示"取消归档" -->
                  <template v-if="convContextMenu.isArchived">
                    <button class="ccm-item" @click="doUnhideConversation">
                      <ArtSvgIcon icon="ri:inbox-unarchive-line" /> 取消归档
                    </button>
                  </template>
                  <!-- 主列表的会话：置顶 + 免打扰 + 归档（L3-M0-7 新增前两项）-->
                  <template v-else>
                    <!-- L3-M0-7：置顶切换（幂等动作名对齐状态）-->
                    <button v-if="!convContextMenu.conv?.pinned" class="ccm-item" @click="doTogglePin(true)">
                      <ArtSvgIcon icon="ri:pushpin-line" /> 置顶会话
                    </button>
                    <button v-else class="ccm-item" @click="doTogglePin(false)">
                      <ArtSvgIcon icon="ri:unpin-line" /> 取消置顶
                    </button>
                    <!-- L3-M0-7：免打扰切换 -->
                    <button v-if="!convContextMenu.conv?.muted" class="ccm-item" @click="doToggleMute(true)">
                      <ArtSvgIcon icon="ri:notification-off-line" /> 消息免打扰
                    </button>
                    <button v-else class="ccm-item" @click="doToggleMute(false)">
                      <ArtSvgIcon icon="ri:notification-4-line" /> 取消免打扰
                    </button>
                    <div class="ccm-divider"></div>
                    <button class="ccm-item" @click="doHideConversation">
                      <ArtSvgIcon icon="ri:archive-2-line" /> 归档会话
                    </button>
                  </template>
                </div>
                <!-- 发起聊天浮动按钮 - 右下角固定 -->
                <button class="fab-btn-fixed" @click="switchSpace('contacts')">
                  <ArtSvgIcon icon="ri:edit-line" class="fab-btn-icon" />
                </button>
              </template>
            </div>
            <!-- ③ 联系人 Space -->
            <div v-else-if="activeSpace === 'contacts' && currentView !== 'chat'" key="contacts" class="space-view ct-space">
              <!-- 骨架屏 -->
              <template v-if="!spaceLoaded.contacts">
                <div class="ct-search ct-search--disabled">
                  <ArtSvgIcon icon="ri:search-line" class="ct-search-icon" />
                  <span class="ct-search-placeholder">搜索联系人...</span>
                </div>
                <div class="ct-skel-bar skeleton-line"></div>
                <div v-for="i in 4" :key="i" class="ct-skel-card">
                  <div class="skeleton-avatar ct-skel-avatar"></div>
                  <div class="skeleton-text">
                    <div class="skeleton-line skeleton-line-name"></div>
                    <div class="skeleton-line skeleton-line-msg"></div>
                  </div>
                </div>
              </template>
              <template v-else>
              <div class="ct-search">
                <ArtSvgIcon icon="ri:search-line" class="ct-search-icon" />
                <input v-model="contactSearch" type="text" placeholder="搜索联系人..." class="ct-search-input" />
              </div>
              <!-- 分组筛选 Segmented Control -->
              <div class="ct-filter-bar">
                <div class="ct-filter-slider" :style="{ width: `calc((100% - 8px) / ${contactFilterTabs.length})`, transform: `translateX(${activeFilterIndex * 100}%)` }"></div>
                <button v-for="tab in contactFilterTabs" :key="tab.label" :class="['ct-filter-tab', { 'ct-filter-tab--active': contactFilter === tab.label }]" @click="contactFilter = tab.label">
                  {{ tab.label }}
                </button>
              </div>
              <template v-for="group in filteredContactGroups" :key="group.role">
                <!-- 无子分组：Persona 双行卡片（管理员/教师） -->
                <template v-if="!group.subGroups">
                  <div v-for="c in group.items" :key="c.id" class="ct-persona-card" @click="startChatWith(c)">
                    <ElAvatar :size="40" :src="getAvatar(c.avatar, c.realName)" class="ct-persona-avatar" />
                    <div class="ct-persona-info">
                      <span class="ct-persona-name">{{ c.realName }}</span>
                      <span class="ct-persona-meta">
                        <span class="ct-persona-dot" :style="{ background: group.color }"></span>
                        <span class="ct-persona-role">{{ group.role }}</span>
                        <span class="ct-persona-sep">·</span>
                        <span :class="chatStore.isUserOnline(c.id) ? 'ct-persona-online' : 'ct-persona-offline'">{{ chatStore.isUserOnline(c.id) ? '在线' : '离线' }}</span>
                      </span>
                    </div>
                    <ArtSvgIcon icon="ri:chat-1-line" class="ct-persona-action" />
                  </div>
                </template>
                <!-- 有子分组：班级独立卡片（学生） -->
                <template v-else>
                  <div v-if="contactFilter === '全部'" class="ct-group-title">
                    <span class="ct-group-dot" :style="{ background: group.color }"></span>
                    <span class="ct-group-name">{{ group.role }}</span>
                    <span class="ct-group-count">{{ group.totalCount }}</span>
                  </div>
                  <div v-for="sub in group.subGroups" :key="sub.className" class="ct-class-card">
                    <div class="ct-class-header" @click="toggleClassGroup(sub.className)">
                      <ArtSvgIcon :icon="expandedClasses.has(sub.className) ? 'ri:arrow-down-s-line' : 'ri:arrow-right-s-line'" class="ct-class-arrow" />
                      <ArtSvgIcon icon="ri:graduation-cap-line" class="ct-class-icon" />
                      <span class="ct-class-name">{{ sub.className }}</span>
                      <span class="ct-class-count">{{ getSubOnlineCount(sub.items) }}/{{ sub.items.length }}</span>
                    </div>
                    <transition name="sub-expand">
                      <div v-if="expandedClasses.has(sub.className)" class="ct-class-list">
                        <div v-for="c in sub.items" :key="c.id" class="ct-row" @click="startChatWith(c)">
                          <ElAvatar :size="38" :src="getAvatar(c.avatar, c.realName)" class="ct-row-avatar" />
                          <div class="ct-row-info">
                            <span class="ct-row-name">{{ c.realName }}</span>
                            <span :class="chatStore.isUserOnline(c.id) ? 'ct-row-online' : 'ct-row-offline'">{{ chatStore.isUserOnline(c.id) ? '在线' : '离线' }}</span>
                          </div>
                          <ArtSvgIcon icon="ri:chat-1-line" class="ct-row-action" />
                        </div>
                      </div>
                    </transition>
                  </div>
                </template>
              </template>
              <div v-if="chatStore.contacts.length === 0" class="empty-state" style="height: auto; padding: 48px 0;">
                <ArtSvgIcon icon="ri:user-unfollow-line" class="text-2xl text-g-600 mb-2" />
                <span class="text-[12px] text-g-600">暂无可联系的用户</span>
              </div>
              </template>
            </div>
          </transition>
          <transition name="slide-chat">
            <div v-if="currentView === 'chat'" class="chat-view-overlay">
          <div class="chat-body chat-messages-area" ref="messageContainer" @scroll="onMessagesScroll">
            <!-- Intercom/Fin 风格：居中圆环旋转加载器 -->
            <div v-if="messagesLoading" class="msg-preloader">
              <div class="msg-preloader-spinner"></div>
            </div>
            <template v-if="!messagesLoading">
            <div v-if="chatStore.currentMessages.length === 0" class="empty-state">
              <ArtSvgIcon icon="ri:chat-new-line" class="text-4xl text-g-600 mb-2" />
              <div class="empty-subtitle">发送第一条消息开始聊天</div>
            </div>
            <template v-for="(msg, idx) in chatStore.currentMessages" :key="msg.id">
              <div v-if="shouldShowTimeDivider(idx)" class="time-divider">
                <span>{{ formatTimeFull(msg.createTime) }}</span>
              </div>
              <div :class="['msg-row', msg.isMe ? 'msg-row-me' : 'msg-row-other', isContinuous(idx) && 'msg-row-continuous']">
                <div :class="['msg-bubble-wrap', msg.isMe ? 'items-end' : 'items-start']">
                  <div :class="['msg-bubble', msg.isMe ? 'msg-bubble-me' : 'msg-bubble-other', isContinuous(idx) && (msg.isMe ? 'msg-continuous-me' : 'msg-continuous-other'), msg.deleted && 'msg-bubble-deleted']">
                    <!-- L3：已撤回占位（L3-M0-4：发送者自己的撤回 + 2min 内 + 文字消息 显示"重新编辑"） -->
                    <span v-if="msg.deleted" class="msg-deleted-placeholder">
                      <ArtSvgIcon icon="ri:forbid-line" class="msg-deleted-icon" />
                      {{ msg.isMe ? '你撤回了一条消息' : '对方撤回了一条消息' }}
                      <button v-if="canReEditRecalled(msg)"
                        class="msg-re-edit-btn"
                        @click.stop="doReEditRecalled(msg)">
                        重新编辑
                      </button>
                    </span>
                    <!-- 单张图片消息（不安全的 URL 会 fallback 到纯文本分支） -->
                    <div v-else-if="isImageMsg(msg.content) && getImageUrl(msg.content)" class="msg-image-wrap" @click="previewImage(msg.content)">
                      <img :src="getImageUrl(msg.content)" class="msg-image" @load="($event.target as HTMLElement)?.classList.add('loaded')" @error="($event.target as HTMLElement)?.classList.add('error')" />
                      <div class="msg-image-hover">
                        <ArtSvgIcon icon="ri:zoom-in-line" class="msg-image-zoom-icon" />
                      </div>
                    </div>
                    <!-- 多图grid布局（Intercom紧凑网格，至少 1 张安全 URL 才渲染） -->
                    <div v-else-if="isMultiImageMsg(msg.content) && getMultiImageUrls(msg.content).length > 0" :class="['msg-image-grid', `msg-image-grid-${Math.min(getMultiImageUrls(msg.content).length, 4)}`]">
                      <div v-for="(url, i) in getMultiImageUrls(msg.content)" :key="i" class="msg-image-wrap msg-grid-item" @click="lightboxUrl = url; lightboxVisible = true">
                        <img :src="url" class="msg-grid-image" @load="($event.target as HTMLElement)?.classList.add('loaded')" @error="($event.target as HTMLElement)?.classList.add('error')" />
                        <div class="msg-image-hover">
                          <ArtSvgIcon icon="ri:zoom-in-line" class="msg-image-zoom-icon" />
                        </div>
                      </div>
                    </div>
                    <!-- 文件消息：下载箭头+hover效果 -->
                    <a v-else-if="isFileMsg(msg.content) && getFileInfo(msg.content)" class="msg-file-card" :href="getFileInfo(msg.content)!.url" target="_blank" download>
                      <div class="msg-file-icon" :style="{ background: getFileColor(getFileInfo(msg.content)!.ext) }">
                        <span v-if="getFileInfo(msg.content)!.ext === 'txt'" class="file-icon-aa">Aa</span>
                        <ArtSvgIcon v-else :icon="getFileIcon(getFileInfo(msg.content)!.ext)" class="file-icon-svg" />
                      </div>
                      <div class="msg-file-info">
                        <!-- L3-M0-BUGFIX-2：middle-ellipsis 保留扩展名（对齐 Slack/Discord） -->
                        <div class="msg-file-name" :title="getFileInfo(msg.content)!.name">
                          <span class="msg-file-name-base">{{ getFileInfo(msg.content)!.baseName }}</span>
                          <span class="msg-file-name-ext">.{{ getFileInfo(msg.content)!.ext }}</span>
                        </div>
                        <span class="msg-file-ext">{{ getFileInfo(msg.content)!.ext.toUpperCase() }}</span>
                      </div>
                      <div class="msg-file-download">
                        <ArtSvgIcon icon="ri:download-2-line" />
                      </div>
                    </a>
                    <span v-else>{{ msg.content }}</span>
                  </div>
                  <!-- L3：消息操作菜单（撤回/强删），仅悬浮时显示 -->
                  <div v-if="!msg.deleted && canShowMessageActions(msg)" class="msg-actions">
                    <button v-if="canRecallMessage(msg)" class="msg-action-btn" title="撤回（2 分钟内）" @click="doRecallMessage(msg)">
                      <ArtSvgIcon icon="ri:arrow-go-back-line" /> 撤回
                    </button>
                    <button v-if="canAdminDeleteMessage(msg)" class="msg-action-btn msg-action-danger" title="管理员强制删除" @click="doAdminDeleteMessage(msg)">
                      <ArtSvgIcon icon="ri:delete-bin-line" /> 删除
                    </button>
                  </div>
                  <!-- 消息状态指示器（仅自己发送的消息显示） -->
                  <div v-if="msg.isMe && msg.status && !msg.deleted" class="msg-status-indicator">
                    <ArtSvgIcon v-if="msg.status === 'sending'" icon="ri:time-line" class="msg-status-icon msg-status-sending" title="发送中" />
                    <ArtSvgIcon v-else-if="msg.status === 'sent'" icon="ri:check-line" class="msg-status-icon msg-status-sent" title="已发送" />
                    <ArtSvgIcon v-else-if="msg.status === 'delivered'" icon="ri:check-double-line" class="msg-status-icon msg-status-delivered" title="已送达" />
                    <ArtSvgIcon v-else-if="msg.status === 'read'" icon="ri:check-double-line" class="msg-status-icon msg-status-read" title="已读" />
                    <ArtSvgIcon v-else-if="msg.status === 'failed'" icon="ri:error-warning-line" class="msg-status-icon msg-status-failed" title="发送失败，点击重发" @click="retryMessage(msg)" />
                  </div>
                </div>
              </div>
            </template>
            </template>
            <!-- L3-M0-6：对方正在输入...气泡（WhatsApp/Telegram 风格左对齐跳动点） -->
            <transition name="msg-fade">
              <div v-if="isPartnerTyping" class="msg-row msg-row-other typing-bubble-row">
                <div class="msg-bubble-wrap items-start">
                  <div class="msg-bubble msg-bubble-other typing-bubble">
                    <span class="typing-dots">
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                    </span>
                  </div>
                </div>
              </div>
            </transition>
          </div>
          <!-- Intercom 风格：置底按钮锚点 -->
          <div v-show="!messagesLoading" class="scroll-btn-anchor">
            <transition name="scroll-btn-fade">
              <button v-if="showScrollBottom" class="scroll-to-bottom-btn" @click="scrollToBottomSmooth">
                <ArtSvgIcon icon="tabler:chevron-down" class="text-xl" />
              </button>
            </transition>
          </div>
          <div v-show="!messagesLoading" class="chat-input-area">
            <!-- 表情选择器面板 -->
            <transition name="emoji-panel">
              <div v-if="showEmojiPicker" class="emoji-picker" v-click-outside="closeEmojiPicker">
                <div class="emoji-search-bar">
                  <ArtSvgIcon icon="ri:search-line" class="emoji-search-icon" />
                  <input v-model="emojiSearch" type="text" placeholder="搜索表情符号..." class="emoji-search-input" />
                </div>
                <div class="emoji-scroll-area">
                  <template v-for="cat in filteredEmojiCategories" :key="cat.name">
                    <div class="emoji-category-title">{{ cat.name }}</div>
                    <div class="emoji-grid">
                      <span v-for="emoji in cat.emojis" :key="emoji"
                        class="emoji-item" :title="getEmojiName(emoji)" @click="insertEmoji(emoji)">{{ emoji }}</span>
                    </div>
                  </template>
                  <div v-if="filteredEmojiCategories.length === 0" class="emoji-empty">
                    <span class="text-[13px] text-gray-400">未找到匹配的表情</span>
                  </div>
                </div>
              </div>
            </transition>
            <!-- Intercom 风格：统一卡片容器（支持拖拽上传） -->
            <div :class="['input-card', inputFocused && 'input-card-focused', voiceState !== 'idle' && 'input-card-voice', isDragging && 'input-card-dragging']"
              @dragenter="onDragEnter" @dragleave="onDragLeave" @dragover="onDragOver" @drop="onDrop">
              <!-- 拖拽覆盖层 -->
              <transition name="fade">
                <div v-if="isDragging" class="drag-overlay">
                  <ArtSvgIcon icon="ri:upload-cloud-2-line" class="drag-overlay-icon" />
                  <span class="drag-overlay-text">拖放文件到此处上传</span>
                </div>
              </transition>
              <!-- 正常输入状态 -->
              <template v-if="voiceState === 'idle'">
                <!-- Fin/Intercom 风格：文件预览卡片（TransitionGroup 入场动画） -->
                <div v-if="pendingFiles.length" class="file-preview-scroll">
                  <TransitionGroup name="file-card">
                    <!-- ===== 图片文件：独立正方形纯图卡片（Fin风格） ===== -->
                    <div v-for="pf in pendingFiles" :key="pf.id"
                      :class="pf.isImage ? 'file-preview-card-image' : 'file-preview-card'">
                      <template v-if="pf.isImage">
                        <img v-if="pf.preview" :src="pf.preview" class="image-card-thumb" @click.stop="previewPendingImage(pf)" />
                        <div v-else class="image-card-skeleton"></div>
                        <div v-if="pf.status === 'uploading'" class="image-card-loading">
                          <div class="file-card-spinner"></div>
                        </div>
                        <div v-if="pf.status === 'error'" class="file-card-error-badge" title="上传失败，点击重试" @click.stop="retryUpload(pf.id)">
                          <ArtSvgIcon icon="ri:refresh-line" class="file-error-retry-icon" />
                        </div>
                      </template>
                      <!-- ===== 非图片文件：横向卡片（图标+文件名+类型） ===== -->
                      <template v-else>
                        <div class="file-card-visual">
                          <div class="file-card-icon" :style="{ background: getFileColor(pf.ext) }">
                            <span v-if="pf.ext === 'txt'" class="file-icon-aa">Aa</span>
                            <ArtSvgIcon v-else :icon="getFileIcon(pf.ext)" class="file-icon-svg" />
                          </div>
                          <div v-if="pf.status === 'uploading'" class="file-card-loading">
                            <div class="file-card-spinner"></div>
                          </div>
                          <div v-if="pf.status === 'error'" class="file-card-error-badge" title="上传失败，点击重试" @click.stop="retryUpload(pf.id)">
                            <ArtSvgIcon icon="ri:refresh-line" class="file-error-retry-icon" />
                          </div>
                        </div>
                        <div class="file-card-info">
                          <span class="file-card-name">{{ pf.name }}</span>
                          <span :class="['file-card-ext', pf.status === 'error' && 'file-card-ext-error']">
                            {{ pf.status === 'error' ? '上传失败' : pf.status === 'uploading' ? '上传中...' : pf.ext.toUpperCase() }}
                          </span>
                        </div>
                      </template>
                      <span class="file-card-remove" @click="removeFile(pf.id)"><ArtSvgIcon icon="ri:close-line" /></span>
                    </div>
                  </TransitionGroup>
                </div>
                <div ref="chatInputRef" contenteditable="plaintext-only" class="chat-input-editable" data-placeholder="发消息..." @input="onEditableInput" @keydown.enter.exact.prevent="handleSend" @focus="inputFocused = true" @blur="inputFocused = false" @paste="onPaste"></div>
                <div class="input-card-bottom">
                  <div class="input-toolbar">
                    <span class="toolbar-icon" @click="triggerFileUpload" title="附件"><ArtSvgIcon icon="ri:attachment-2" /></span>
                    <span :class="['toolbar-icon', showEmojiPicker && 'toolbar-icon-active']" @click="showEmojiPicker = !showEmojiPicker" title="表情"><ArtSvgIcon :icon="showEmojiPicker ? 'ri:emotion-happy-fill' : 'ri:emotion-happy-line'" /></span>
                    <span class="toolbar-icon" title="语音输入" @click="startVoiceRecording"><ArtSvgIcon icon="ri:mic-line" /></span>
                    <input ref="fileInputRef" type="file" accept=".jpg,.jpeg,.png,.gif,.webp,.bmp,.pdf,.txt,.doc,.docx,.csv,.xls,.xlsx" multiple hidden @change="onFilesSelected" />
                  </div>
                  <!-- 字数计数器：接近上限时显示（3500+ 黄色，超限红色） -->
                  <span v-if="messageTextLength > MAX_MESSAGE_LENGTH * 0.875"
                    :class="['msg-length-counter', isMessageTooLong && 'msg-length-over']">
                    {{ MAX_MESSAGE_LENGTH - messageTextLength }}
                  </span>
                  <button class="send-btn" :class="{ 'send-btn-active': canSend }" :disabled="!canSend" @click="handleSend">
                    <ArtSvgIcon icon="ri:arrow-up-line" class="text-base" />
                  </button>
                </div>
              </template>
              <!-- 录音状态 -->
              <template v-else-if="voiceState === 'recording'">
                <div class="voice-recording-bar">
                  <button class="voice-cancel-btn" @click="cancelVoiceRecording">
                    <ArtSvgIcon icon="ri:close-line" class="text-lg" />
                  </button>
                  <div class="voice-center">
                    <span class="recording-dot"></span>
                    <span class="recording-duration">{{ recordingDurationStr }}</span>
                    <div class="voice-waveform">
                      <span v-for="(h, i) in waveformBars" :key="i" class="waveform-bar" :style="{ height: h + 'px' }"></span>
                    </div>
                  </div>
                  <button class="voice-stop-btn" @click="stopVoiceRecording">
                    <ArtSvgIcon icon="ri:stop-fill" class="text-base" />
                  </button>
                </div>
                <div v-if="interimText" class="voice-interim-preview">{{ interimText.length > 50 ? '…' + interimText.slice(-50) : interimText }}</div>
                <div v-else-if="recordingSeconds >= 2" class="voice-interim-preview voice-interim-hint">语音录入中，停止后识别</div>
              </template>
              <!-- 转写状态 -->
              <template v-else-if="voiceState === 'transcribing'">
                <div class="voice-recording-bar">
                  <button class="voice-cancel-btn" @click="cancelVoiceRecording">
                    <ArtSvgIcon icon="ri:close-line" class="text-lg" />
                  </button>
                  <div class="voice-transcribing">
                    <span class="transcribing-spinner"></span>
                    <span class="transcribing-text">语音转写中...</span>
                  </div>
                  <button class="send-btn" disabled>
                    <ArtSvgIcon icon="ri:arrow-up-line" class="text-base" />
                  </button>
                </div>
              </template>
            </div>
          </div>
          </div>
          </transition>
          <!-- 自定义浮动滚动条（QQ 风格，零布局偏移） -->
          <div class="cs-track" :class="{ 'cs-visible': csHover && csHasScroll }">
            <div class="cs-thumb" :style="{ height: csThumbH + 'px', transform: `translateY(${csThumbT}px)` }"></div>
          </div>
          </div>
          <!-- ====== 底部 Tab Bar（Intercom 4-Space 导航） ====== -->
          <transition name="tab-bar-slide">
            <div v-if="currentView !== 'chat'" class="tab-bar">
              <ElTooltip v-for="tab in spaceTabs" :key="tab.id" :content="tab.label" placement="top" :show-after="400" :hide-after="0" :offset="8">
                <div :class="['tab-item', activeSpace === tab.id && 'tab-item-active']"
                  @click="switchSpace(tab.id)">
                  <div class="tab-icon-wrap">
                    <ArtSvgIcon :icon="activeSpace === tab.id ? tab.filledIcon : tab.outlineIcon" class="tab-icon" />
                    <span v-if="tab.badge > 0" class="tab-badge">{{ tab.badge > 99 ? '99+' : tab.badge }}</span>
                  </div>
                </div>
              </ElTooltip>
            </div>
          </transition>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
  import { mittBus } from '@/utils/sys'
  import { useChatStore } from '@/store/modules/chat'
  import { useUserStore } from '@/store/modules/user'
  import { ElMessage } from 'element-plus'
  import { uploadChatFile, speechRecognize, getRecallDraft } from '@/api/exam/chat'
  // L3-M0-8 + CHAT-325：会话草稿持久化（纯前端 localStorage，切会话保留未发送的输入）
  // CHAT-325 根治：草稿改为 peer-keyed（对齐 Telegram/WhatsApp/微信 PC），
  // 未建立服务端会话的 "零会话草稿" 也能持久化 + 在列表渲染 ghost 卡片
  import {
    saveDraftByPeerDebounced,
    saveDraftByPeer,
    loadDraftByPeer,
    clearDraftByPeer,
    loadAllDraftsOfUser,
    migrateLegacyDraftsToPeerKeyed,
    type DraftRecord,
  } from '@/utils/chatDraft'
  // L3-M0-6：Typing Indicator 入站事件发送器（走 STOMP /app/chat/typing）
  import { sendTypingEvent } from '@/utils/websocket'
  // notification API removed — using system notification module instead
  import { getUserAvatar } from '@/utils/avatar'
  import { ClickOutside as vClickOutside } from 'element-plus'

  defineOptions({ name: 'ArtChatWindow' })

  const MOBILE_BREAKPOINT = 640
  const { width } = useWindowSize()
  const isMobile = computed(() => width.value < MOBILE_BREAKPOINT)

  const chatStore = useChatStore()
  function getAvatar(src: string | null | undefined, name?: string | null) { return getUserAvatar(src, name) }
  const isDrawerVisible = ref(false)
  const messageText = ref('')
  const messageContainer = ref<HTMLElement | null>(null)
  const chatInputRef = ref<HTMLElement | null>(null)

  // ========== L3-M0-6：Typing Indicator 客户端节流器 ==========
  // 业界参考：WhatsApp "Typing..." 事件驱动实现 — 心跳 3s + 自动 stop 5s。
  // 避免每次键入都发 WS 事件（否则会成为 typing 风暴）。
  const TYPING_HEARTBEAT_MS = 3000   // 与 ChatConstants.TYPING_HEARTBEAT_MS 对齐
  const TYPING_AUTO_STOP_MS = 5000   // 与 ChatConstants.TYPING_AUTO_STOP_MS 对齐
  let _typingLastHeartbeat = 0        // 最后一次 TYPING_START 时间戳（ms），用于节流
  let _typingStopTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * L3-M0-6：向对方发送 TYPING_START 心跳（节流版）。
   * <p>
   * 调用频率取决于用户打字节奏，函数内部保证不超过 1 次/3s。
   * 同时重置 auto-stop 定时器：5 秒无输入自动发 TYPING_STOP。
   */
  function _pingTyping() {
    const receiverId = chatPartner.value?.otherUserId
    if (typeof receiverId !== 'number') return
    const now = Date.now()
    if (now - _typingLastHeartbeat >= TYPING_HEARTBEAT_MS) {
      sendTypingEvent(receiverId, 'TYPING_START')
      _typingLastHeartbeat = now
    }
    // 每次输入都重置 auto-stop 定时器
    if (_typingStopTimer) clearTimeout(_typingStopTimer)
    _typingStopTimer = setTimeout(() => {
      _stopTyping()
    }, TYPING_AUTO_STOP_MS)
  }

  /**
   * L3-M0-6：发送 TYPING_STOP（显式停止）。
   * <p>
   * 触发场景：
   * 1. 输入停止 5 秒（auto-stop 定时器）
   * 2. 发送消息成功后立即停止
   * 3. 切换会话 / 关闭聊天窗口
   * 4. 清空输入框
   * <p>
   * 幂等：即使已经发过 STOP，再次发送也无副作用（后端会按速率限制丢弃）。
   */
  function _stopTyping() {
    const receiverId = chatPartner.value?.otherUserId
    if (_typingStopTimer) { clearTimeout(_typingStopTimer); _typingStopTimer = null }
    if (_typingLastHeartbeat === 0) return  // 从未发送过 START，无需发 STOP
    _typingLastHeartbeat = 0
    if (typeof receiverId === 'number') {
      sendTypingEvent(receiverId, 'TYPING_STOP')
    }
  }

  function onEditableInput() {
    if (chatInputRef.value) {
      const text = chatInputRef.value.textContent || ''
      messageText.value = text
      // contenteditable 删除所有文字后可能残留 <br>，导致 :empty 不触发 placeholder
      if (!text.trim() && chatInputRef.value.innerHTML !== '') {
        chatInputRef.value.innerHTML = ''
      }
      // L3-M0-6：根据文字是否非空决定 typing 行为
      // 空串不触发 TYPING_START（避免全选删除时误发），非空则心跳节流发送
      if (text.trim()) {
        _pingTyping()
      } else if (_typingLastHeartbeat !== 0) {
        // 从有输入变为空 → 立即发 STOP（用户明确"清空"）
        _stopTyping()
      }

      // L3-M0-8 + CHAT-325：草稿持久化 — 按 peerUserId（稳定标识）存储
      // 旧版 bug：用 currentConversationId 做 key 时，未发过消息的新会话 convId = null → 草稿丢失
      // 新版根治：用 chatPartner.otherUserId 做 key，新老会话统一处理（对齐 Telegram peer-keyed）
      const myId = userStore.info?.userId
      const peerUserId = chatPartner.value?.otherUserId
      if (myId && typeof peerUserId === 'number') {
        const meta = {
          peerUserName: chatPartner.value?.otherUserName,
          peerUserAvatar: chatPartner.value?.otherAvatar,
        }
        saveDraftByPeerDebounced(myId, peerUserId, text, meta)
        // 响应式缓存立即同步（不等 debounce），让会话列表"[草稿]"前缀 + ghost 卡片实时显示
        _syncDraftCache(peerUserId, text, meta)
      }
    }
  }

  function moveCursorToEnd() {
    const el = chatInputRef.value
    if (!el) return
    const range = document.createRange()
    const sel = window.getSelection()
    range.selectNodeContents(el)
    range.collapse(false)
    sel?.removeAllRanges()
    sel?.addRange(range)
  }
  const currentView = ref<'list' | 'chat'>('list')
  const messagesLoading = ref(false)
  const chatPartner = ref<any>(null)
  const showContacts = ref(false)
  const contactSearch = ref('')
  const contactFilter = ref('全部')
  const expandedClasses = ref<Set<string>>(new Set())
  const inputFocused = ref(false)

  // ====== Intercom 多 Space 架构 ======
  type SpaceId = 'home' | 'messages' | 'contacts'
  const activeSpace = ref<SpaceId>('home')
  const userStore = useUserStore()
  const userDisplayName = computed(() => userStore.info?.realName || userStore.info?.userName || '用户')

  const spaceLoaded = reactive({ messages: false, contacts: false })

  const spaceTabs = computed(() => [
    { id: 'home' as SpaceId, label: '主页', outlineIcon: 'ri:home-4-line', filledIcon: 'ri:home-4-fill', badge: 0 },
    { id: 'messages' as SpaceId, label: '消息', outlineIcon: 'ri:chat-3-line', filledIcon: 'ri:chat-3-fill', badge: chatStore.unreadTotal },
    { id: 'contacts' as SpaceId, label: '联系人', outlineIcon: 'ri:user-line', filledIcon: 'ri:user-fill', badge: 0 },
  ])

  const spaceTitle = computed(() => {
    const titles: Record<SpaceId, string> = { home: '主页', messages: '消息', contacts: '联系人' }
    return titles[activeSpace.value]
  })

  async function switchSpace(id: SpaceId) {
    if (activeSpace.value === id) return
    activeSpace.value = id
    if (id === 'messages' && !spaceLoaded.messages) {
      try {
        // L3-M1-4：同时拉主列表 + 归档列表。归档通常只有零星几条，一次性并行加载
        // 比"用户点展开再加载"体验更自然，首屏能直接看到归档卡片的数量
        await Promise.all([
          chatStore.loadConversations(),
          chatStore.loadArchivedConversations(),
        ])
      } finally { spaceLoaded.messages = true }
    }
    if (id === 'contacts' && !spaceLoaded.contacts) {
      try { await chatStore.loadContacts() } finally { spaceLoaded.contacts = true }
    }
  }

  // 判断是否是同一发送者的连续消息（用于紧凑间距和圆角调整）
  function isContinuous(idx: number) {
    if (idx === 0) return false
    const msgs = chatStore.currentMessages
    const curr = msgs[idx]
    const prev = msgs[idx - 1]
    if (curr.isMe !== prev.isMe) return false
    if (shouldShowTimeDivider(idx)) return false
    return true
  }

  function toggleClassGroup(className: string) {
    const s = new Set(expandedClasses.value)
    if (s.has(className)) s.delete(className)
    else s.add(className)
    expandedClasses.value = s
  }

  // ====== 表情选择器 ======
  const showEmojiPicker = ref(false)
  const activeEmojiTab = ref(0)
  const emojiSearch = ref('')

  const emojiNameMap: Record<string, string> = {
    '👍':'thumbs up','👎':'thumbs down','😂':'face with tears of joy joy','❤️':'red heart love',
    '🙏':'folded hands pray','😊':'smiling face smile','🔥':'fire hot','😍':'heart eyes love',
    '😭':'loudly crying face cry','🤔':'thinking face think','👏':'clapping hands clap',
    '🎉':'party popper celebrate','😅':'sweat smile','🥰':'smiling face with hearts love',
    '😘':'face blowing a kiss kiss','😀':'grinning face grin','😁':'beaming face beam',
    '🤣':'rolling on the floor laughing rofl','😃':'grinning face big eyes',
    '😄':'grinning face smiling eyes smile','😆':'grinning squinting face laugh',
    '😉':'winking face wink','😋':'face savoring food yummy','😎':'smiling face sunglasses cool',
    '😗':'kissing face kiss','😙':'kissing face smiling eyes','😚':'kissing face closed eyes',
    '🙂':'slightly smiling face smile','🤗':'hugging face hug','🤩':'star struck amazing',
    '🤨':'face with raised eyebrow','😐':'neutral face','😑':'expressionless face',
    '😶':'face without mouth silent','🙄':'face with rolling eyes','😏':'smirking face smirk',
    '😣':'persevering face','😥':'sad but relieved face','😮':'face with open mouth surprised',
    '🤐':'zipper mouth face secret','😯':'hushed face','😪':'sleepy face',
    '😫':'tired face','😴':'sleeping face sleep','😌':'relieved face',
    '😛':'face with tongue','😜':'winking face tongue','😝':'squinting face tongue',
    '🤤':'drooling face drool','😒':'unamused face','😓':'downcast face sweat',
    '😔':'pensive face sad','😕':'confused face','🙃':'upside down face',
    '🤑':'money mouth face rich','😲':'astonished face shocked','🤯':'exploding head mind blown',
    '😳':'flushed face blush','🥺':'pleading face puppy eyes','😱':'face screaming fear',
    '😨':'fearful face scared','😰':'anxious face sweat','😢':'crying face cry sad',
    '😤':'face with steam angry','😠':'angry face','😡':'pouting face rage',
    '🤬':'face with symbols swearing','💀':'skull dead','💩':'pile of poo poop',
    '🤡':'clown face','👹':'ogre','👺':'goblin','👻':'ghost boo','👽':'alien','🤖':'robot',
    '👋':'waving hand wave hello','🤚':'raised back of hand','🖐️':'hand with fingers splayed',
    '✋':'raised hand stop','🖖':'vulcan salute spock','👌':'ok hand okay',
    '🤌':'pinched fingers','🤏':'pinching hand small','✌️':'victory hand peace',
    '🤞':'crossed fingers luck','🤟':'love you gesture','🤘':'sign of the horns rock',
    '🤙':'call me hand','👈':'backhand index pointing left','👉':'backhand index pointing right',
    '👆':'backhand index pointing up','👇':'backhand index pointing down',
    '☝️':'index pointing up','✊':'raised fist','👊':'oncoming fist punch',
    '🤛':'left facing fist','🤜':'right facing fist','🙌':'raising hands hooray',
    '👐':'open hands','🤲':'palms up together','🤝':'handshake deal',
    '💪':'flexed biceps strong muscle','🦾':'mechanical arm','🖕':'middle finger',
    '✍️':'writing hand write','💅':'nail polish',
    '🧡':'orange heart','💛':'yellow heart','💚':'green heart','💙':'blue heart',
    '💜':'purple heart','🖤':'black heart','🤍':'white heart','🤎':'brown heart',
    '💔':'broken heart','❣️':'heart exclamation','💕':'two hearts','💞':'revolving hearts',
    '💓':'beating heart','💗':'growing heart','💖':'sparkling heart','💘':'heart with arrow cupid',
    '💝':'heart with ribbon gift','💟':'heart decoration','♥️':'heart suit',
    '⭐':'star','🌟':'glowing star','✨':'sparkles','💫':'dizzy star',
    '🎊':'confetti ball','🎈':'balloon','🎁':'wrapped gift present','🏆':'trophy winner',
    '🥇':'gold medal first','💯':'hundred points perfect','✅':'check mark done',
    '❌':'cross mark wrong','⚠️':'warning','💢':'anger symbol','💤':'zzz sleep',
    '💬':'speech bubble chat','👁️‍🗨️':'eye in speech bubble',
    '🐶':'dog puppy','🐱':'cat kitty','🐭':'mouse','🐹':'hamster','🐰':'rabbit bunny',
    '🦊':'fox','🐻':'bear','🐼':'panda','🐨':'koala','🐯':'tiger',
    '🦁':'lion','🐮':'cow','🐷':'pig','🐸':'frog','🐵':'monkey',
    '🙈':'see no evil monkey','🙉':'hear no evil monkey','🙊':'speak no evil monkey',
    '🐔':'chicken','🐧':'penguin','🐦':'bird','🦆':'duck','🦅':'eagle',
    '🦉':'owl','🐴':'horse','🦄':'unicorn','🐝':'bee honey','🐛':'bug caterpillar',
    '🦋':'butterfly','🐌':'snail','🐞':'ladybug','🐠':'tropical fish','🐟':'fish',
    '🐬':'dolphin','🐳':'spouting whale','🐋':'whale','🦈':'shark','🐊':'crocodile',
    '🐅':'tiger','🐆':'leopard',
    '🍎':'red apple','🍐':'pear','🍊':'tangerine orange','🍋':'lemon','🍌':'banana',
    '🍉':'watermelon','🍇':'grapes','🍓':'strawberry','🫐':'blueberries','🍈':'melon',
    '🍒':'cherries','🍑':'peach','🥭':'mango','🍍':'pineapple','🥥':'coconut',
    '🥝':'kiwi','🍅':'tomato','🥑':'avocado','🍆':'eggplant','🌽':'corn',
    '🥕':'carrot','🧄':'garlic','🧅':'onion','🥔':'potato','🍞':'bread',
    '🥐':'croissant','🥖':'baguette','🧀':'cheese','🍖':'meat on bone','🍗':'poultry leg chicken',
    '🍔':'hamburger burger','🍟':'french fries','🍕':'pizza','🌭':'hot dog',
    '🥪':'sandwich','🌮':'taco','🍜':'steaming bowl noodles ramen','🍝':'spaghetti pasta',
    '🍣':'sushi','🍱':'bento box',
    '⚽':'soccer ball football','🏀':'basketball','🏈':'american football','⚾':'baseball',
    '🥎':'softball','🎾':'tennis','🏐':'volleyball','🏉':'rugby',
    '🥏':'flying disc frisbee','🎱':'pool billiards','🪀':'yo yo','🏓':'ping pong table tennis',
    '🏸':'badminton','🏒':'ice hockey','🥅':'goal net','⛳':'golf',
    '🏹':'bow and arrow archery','🎣':'fishing','🤿':'diving mask','🥊':'boxing glove',
    '🥋':'martial arts','🎽':'running shirt','🛹':'skateboard','🛼':'roller skate',
    '⛸️':'ice skate','🎿':'ski','🛷':'sled','🎯':'bullseye target dart',
    '🪁':'kite','🎮':'video game controller','🕹️':'joystick','🎲':'dice game',
    '♟️':'chess pawn','🎭':'performing arts theater','🎨':'artist palette paint',
    '🎬':'clapper board movie film','🎤':'microphone karaoke sing',
    '🎧':'headphone music','🎹':'musical keyboard piano','🎸':'guitar music'
  }

  function getEmojiName(emoji: string): string {
    return emojiNameMap[emoji] || ''
  }

  const emojiCategories = [
    { icon: '⭐', name: '频繁使用', emojis: ['👍','👎','😂','❤️','🙏','😊',''] },
    { icon: '😀', name: '笑脸和情感', emojis: ['😀','😁','😂','🤣','😃','😄','😅','😆','😉','😊','😋','😎','😍','🥰','😘','😗','😙','😚','🙂','🤗','🤩','🤔','🤨','😐','😑','😶','🙄','😏','😣','😥','😮','🤐','😯','😪','😫','😴','😌','😛','😜','😝','🤤','😒','😓','😔','😕','🙃','🤑','😲','🤯','😳','🥺','😱','😨','😰','😢','😭','😤','😠','😡','🤬','💀','💩','🤡','👹','👺','👻','👽','🤖'] },
    { icon: '👋', name: '手势', emojis: ['👋','🤚','🖐️','✋','🖖','👌','🤌','🤏','✌️','🤞','🤟','🤘','🤙','👈','👉','👆','👇','☝️','👍','👎','✊','👊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','💪','🦾','🖕','✍️','💅'] },
    { icon: '❤️', name: '爱心和符号', emojis: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝','💟','♥️','🔥','⭐','🌟','✨','💫','🎉','🎊','🎈','🎁','🏆','🥇','💯','✅','❌','⚠️','💢','💤','💬','👁️‍🗨️'] },
    { icon: '🐱', name: '动物', emojis: ['🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐨','🐯','🦁','🐮','🐷','🐸','🐵','🙈','🙉','🙊','🐔','🐧','🐦','🦆','🦅','🦉','🐴','🦄','🐝','🐛','🦋','🐌','🐞','🐠','🐟','🐬','🐳','🐋','🦈','🐊','🐅','🐆'] },
    { icon: '🍕', name: '食物', emojis: ['🍎','🍐','🍊','🍋','🍌','🍉','🍇','🍓','🫐','🍈','🍒','🍑','🥭','🍍','🥥','🥝','🍅','🥑','🍆','🌽','🥕','🧄','🧅','🥔','🍞','🥐','🥖','🧀','🍖','🍗','🍔','🍟','🍕','🌭','🥪','🌮','🍜','🍝','🍣','🍱'] },
    { icon: '⚽', name: '运动', emojis: ['⚽','🏀','🏈','⚾','🥎','🎾','🏐','🏉','🥏','🎱','🪀','🏓','🏸','🏒','🥅','⛳','🏹','🎣','🤿','🥊','🥋','🎽','🛹','🛼','⛸️','🎿','🛷','🎯','🪁','🎮','🕹️','🎲','♟️','🎭','🎨','🎬','🎤','🎧','🎹','🎸'] }
  ]

  const filteredEmojiCategories = computed(() => {
    const q = emojiSearch.value.trim().toLowerCase()
    if (!q) return emojiCategories
    return emojiCategories.map(cat => ({
      ...cat,
      emojis: cat.emojis.filter(e => e.includes(q) || (emojiNameMap[e] || '').toLowerCase().includes(q))
    })).filter(cat => cat.emojis.length > 0)
  })

  function insertEmoji(emoji: string) {
    messageText.value += emoji
    if (chatInputRef.value) {
      chatInputRef.value.textContent = messageText.value
      nextTick(() => moveCursorToEnd())
    }
  }

  function closeEmojiPicker() {
    showEmojiPicker.value = false
    emojiSearch.value = ''
  }

  // ====== 文件附件（Fin/Intercom 风格：多文件 + 后台上传 + 拖拽 + 粘贴） ======
  const fileInputRef = ref<HTMLInputElement | null>(null)
  interface PendingFile {
    id: string; file: File; name: string; ext: string; preview: string; isImage: boolean
    status: 'uploading' | 'done' | 'error'; uploadUrl: string; error: string
  }
  const pendingFiles = ref<PendingFile[]>([])
  const isDragging = ref(false)
  let dragCounter = 0
  // L3-M3-1：批量发送去重锁 —— 防止连点 / 回车重入，对齐 WhatsApp/微信/飞书
  const isSending = ref(false)
  // 客户端预节流间隔（ms）—— 对齐后端 ChatConstants.RATE_LIMIT_STUDENT_PER_SECOND=2 的令牌桶节奏，
  // 400~500ms 可稳妥吸收 Guava SmoothBursty 的突发 + 补充速率（2 msg/s ≈ 500ms/条）
  const CHAT_SEND_MIN_INTERVAL_MS = 500
  // 后端 BusinessException 暂未携带 HTTP 429，按文案特征识别限流错误
  // 未来若切换到标准 HTTP 429 + Retry-After，仅需替换此函数
  const _isRateLimitError = (e: any): boolean => {
    const msg = String(e?.response?.data?.message || e?.message || '')
    return /频繁|rate.?limit|429/i.test(msg)
  }
  const _sleep = (ms: number) => new Promise(r => setTimeout(r, ms))

  const IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp']
  const ALLOWED_EXTS = [...IMAGE_EXTS, 'pdf', 'txt', 'doc', 'docx', 'csv', 'xls', 'xlsx']
  const MAX_FILE_SIZE = 10 * 1024 * 1024
  const MAX_FILES = 10

  function triggerFileUpload() { fileInputRef.value?.click() }

  function onFilesSelected(e: Event) {
    const input = e.target as HTMLInputElement
    if (!input.files) return
    addFiles(input.files)
    input.value = ''
  }

  function addFiles(files: FileList | File[]) {
    for (const file of Array.from(files)) {
      if (pendingFiles.value.length >= MAX_FILES) { ElMessage.warning(`最多选择 ${MAX_FILES} 个文件`); break }
      const ext = file.name.split('.').pop()?.toLowerCase() || ''
      if (!ALLOWED_EXTS.includes(ext)) { ElMessage.warning(`不支持的文件格式: ${file.name}`); continue }
      if (file.size > MAX_FILE_SIZE) { ElMessage.warning(`文件 ${file.name} 超过 10MB 限制`); continue }
      const isImage = IMAGE_EXTS.includes(ext)
      const fileId = Date.now().toString() + Math.random().toString(36).slice(2)
      pendingFiles.value.push({ id: fileId, file, name: file.name, ext, preview: '', isImage, status: 'uploading', uploadUrl: '', error: '' })
      if (isImage) {
        const reader = new FileReader()
        reader.onload = (ev) => {
          const t = pendingFiles.value.find(f => f.id === fileId)
          if (t) t.preview = ev.target?.result as string
        }
        reader.readAsDataURL(file)
      }
      uploadFileInBackground(fileId)
    }
  }

  async function uploadFileInBackground(fileId: string) {
    const pf = pendingFiles.value.find(f => f.id === fileId)
    if (!pf) return
    pf.status = 'uploading'
    pf.error = ''
    try {
      const res = await uploadChatFile(pf.file)
      const t = pendingFiles.value.find(f => f.id === fileId)
      if (t) { t.status = 'done'; t.uploadUrl = res.url || res }
    } catch (e: any) {
      const t = pendingFiles.value.find(f => f.id === fileId)
      if (t) { t.status = 'error'; t.error = e?.message || '上传失败' }
    }
  }

  function retryUpload(fileId: string) { uploadFileInBackground(fileId) }

  function removeFile(id: string) { pendingFiles.value = pendingFiles.value.filter(f => f.id !== id) }
  function cancelAllFiles() { pendingFiles.value = [] }

  // 拖拽上传
  function onDragEnter(e: DragEvent) { e.preventDefault(); dragCounter++; if (e.dataTransfer?.types.includes('Files')) isDragging.value = true }
  function onDragLeave(e: DragEvent) { e.preventDefault(); dragCounter--; if (dragCounter <= 0) { isDragging.value = false; dragCounter = 0 } }
  function onDragOver(e: DragEvent) { e.preventDefault() }
  function onDrop(e: DragEvent) {
    e.preventDefault(); isDragging.value = false; dragCounter = 0
    if (e.dataTransfer?.files?.length) addFiles(e.dataTransfer.files)
  }

  // 粘贴处理：文件（截图/图片）→ 上传，纯文本由 plaintext-only 原生处理
  function onPaste(e: ClipboardEvent) {
    const items = e.clipboardData?.items
    if (!items) return
    const files: File[] = []
    for (const item of Array.from(items)) {
      if (item.kind === 'file') { const f = item.getAsFile(); if (f) files.push(f) }
    }
    if (files.length) { e.preventDefault(); addFiles(files) }
  }

  function getFileColor(ext: string): string {
    if (['pdf', 'doc', 'docx'].includes(ext)) return '#E54D2E'
    if (['csv', 'xls', 'xlsx'].includes(ext)) return '#30A46C'
    return '#6B7280'
  }
  function getFileIcon(ext: string): string {
    if (ext === 'pdf') return 'ri:file-pdf-2-fill'
    if (['doc', 'docx'].includes(ext)) return 'ri:file-word-2-fill'
    if (ext === 'csv') return 'ri:grid-fill'
    if (['xls', 'xlsx'].includes(ext)) return 'ri:file-excel-2-fill'
    return 'ri:file-fill'
  }

  /**
   * L3-M0-5：会话列表最后一条消息预览文案渲染。
   * <p>
   * 接收整个 conversation 对象（而非仅 content），以便根据 lastMessageSenderId 区分
   * "你撤回了一条消息"和"对方撤回了一条消息"（对齐微信/QQ/WhatsApp 业界设计）。
   * <p>
   * 渲染优先级：
   * 1. content 为空 → "暂无消息"
   * 2. content === "[此消息已撤回]" → 按发送者身份区分你/对方
   * 3. 图片/多图/文件 → 媒体类型标记
   * 4. 其他 → 原文（已在后端按 code point 安全截断）
   */
  function formatLastMessage(conv: any) {
    const content = conv?.lastMessage
    if (!content) return '暂无消息'
    // L3-M0-5：撤回消息按发送者身份差异化渲染（微信/QQ 标准）
    if (content === '[此消息已撤回]') {
      const myId = userStore.info?.userId
      const senderId = conv?.lastMessageSenderId
      // senderId 缺失时兜底为中性文案（迁移前的老会话或前端数据过期）
      if (senderId == null || myId == null) return '此消息已撤回'
      return senderId === myId ? '你撤回了一条消息' : '对方撤回了一条消息'
    }
    if (content.startsWith('[img]') && content.endsWith('[/img]')) return '[图片]'
    if (content.startsWith('[imgs]') && content.endsWith('[/imgs]')) return '[图片]'
    if (content.startsWith('[file:')) return '[文件]'
    return content
  }

  /**
   * L3-M0-8 + CHAT-325：判定会话卡片是否应以"[草稿]"前缀渲染预览。
   * <p>
   * 规则：
   * <ul>
   *   <li>该会话对应 peerUserId 在 peerDraftMap 中有非空草稿</li>
   *   <li>非当前正在聊天的会话（因为当前会话的草稿已在输入框里展示）</li>
   * </ul>
   * <p>
   * "当前会话不显示" 是对齐 WhatsApp/微信的"聚焦原则" —— 用户已经在看输入框，
   * 再在列表里显示"[草稿]"前缀会冗余且视觉分散。
   * <p>
   * CHAT-325 更新：改用 peerUserId 做 key + "当前会话" 判定改为对比 chatPartner.otherUserId，
   * 让 ghost 会话（无 convId 但有 draft）也能正确判定"当前"状态不重复显示前缀。
   */
  function hasDraftPreview(conv: any): boolean {
    const peerId = conv?.otherUserId
    if (typeof peerId !== 'number') return false
    // "当前正在聊天" 的两种情况：convId 匹配（存量会话）或 peerUserId 匹配（ghost）
    if (currentView.value === 'chat' && chatPartner.value?.otherUserId === peerId) return false
    return !!peerDraftMap.value[peerId]?.content
  }

  /** L3-M0-8 + CHAT-325：获取草稿预览文本（已 trim，模板用，按 peerUserId 查） */
  function getDraftPreview(conv: any): string {
    const peerId = conv?.otherUserId
    if (typeof peerId !== 'number') return ''
    return peerDraftMap.value[peerId]?.content || ''
  }

  /**
   * URL 协议白名单校验（防 XSS）
   * 只允许：http://、https://、站内相对路径（/xxx、./xxx）
   * 拒绝：javascript:、data:、vbscript:、file: 等危险协议
   * 参考：OWASP XSS Prevention Cheat Sheet §4 - URL Contexts
   */
  function isSafeUrl(url: string): boolean {
    if (!url || typeof url !== 'string') return false
    const trimmed = url.trim()
    if (!trimmed) return false
    // 允许的协议白名单
    if (/^https?:\/\//i.test(trimmed)) return true
    // 允许站内相对路径（/xxx、./xxx、../xxx）
    if (/^\.?\.?\//.test(trimmed)) return true
    // 协议相对 URL（//example.com/foo.png）
    if (trimmed.startsWith('//')) return true
    // 其他情况一律拒绝（javascript:、data:、vbscript:、file:、ftp:、mailto: 等）
    return false
  }

  function isImageMsg(content: string) {
    return content?.startsWith('[img]') && content?.endsWith('[/img]')
  }

  function isMultiImageMsg(content: string) {
    return content?.startsWith('[imgs]') && content?.endsWith('[/imgs]')
  }

  function getMultiImageUrls(content: string): string[] {
    const inner = content.replace('[imgs]', '').replace('[/imgs]', '')
    // 过滤空字符串 + 不安全协议
    return inner.split('||').map(u => u.trim()).filter(u => u && isSafeUrl(u))
  }

  function isFileMsg(content: string) {
    return content?.startsWith('[file:') && content?.endsWith('[/file]')
  }

  function getFileInfo(content: string) {
    const match = content.match(/^\[file:(\w+):(.+?)\](.+)\[\/file\]$/)
    if (!match) return null
    const url = match[3]
    // URL 协议白名单校验：不安全直接返回 null，模板会 fallback 为纯文本展示
    if (!isSafeUrl(url)) return null
    const ext = match[1]
    const name = match[2]
    // L3-M0-BUGFIX-1：middle-ellipsis 需要的"无扩展名"基础名
    // 形如 "长文件名.doc" → baseName = "长文件名"；若文件名末尾不带 .ext 后缀（老数据容错），baseName = name
    const suffix = '.' + ext
    const baseName = name.toLowerCase().endsWith(suffix.toLowerCase())
      ? name.slice(0, -suffix.length)
      : name
    return { ext, name, baseName, url }
  }

  function getImageUrl(content: string) {
    const url = content.replace('[img]', '').replace('[/img]', '').trim()
    // 不安全的 URL 返回空字符串，img 标签会显示 broken 状态，不会触发 XSS
    if (!isSafeUrl(url)) return ''
    return url
  }

  // ====== 图片灯箱预览（Fin/Intercom 风格） ======
  const lightboxVisible = ref(false)
  const lightboxUrl = ref('')

  function previewImage(content: string) {
    lightboxUrl.value = getImageUrl(content)
    lightboxVisible.value = true
  }

  function previewPendingImage(pf: PendingFile) {
    if (pf.isImage && pf.preview) {
      lightboxUrl.value = pf.preview
      lightboxVisible.value = true
    }
  }

  function closeLightbox() {
    lightboxVisible.value = false
    lightboxUrl.value = ''
  }

  function onLightboxKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') closeLightbox()
  }

  onMounted(() => document.addEventListener('keydown', onLightboxKeydown))
  onUnmounted(() => document.removeEventListener('keydown', onLightboxKeydown))

  // ====== 语音录制（双引擎：Web Speech API 主引擎 + 后端百度API降级兜底） ======
  const voiceState = ref<'idle' | 'recording' | 'transcribing'>('idle')
  const waveformBars = ref<number[]>(Array(30).fill(2))
  const recordingSeconds = ref(0)
  let recordingTimer: ReturnType<typeof setInterval> | null = null
  let mediaRecorder: MediaRecorder | null = null
  let audioChunks: Blob[] = []
  let audioContext: AudioContext | null = null
  let analyserNode: AnalyserNode | null = null
  let mediaStream: MediaStream | null = null
  let waveformTimer: ReturnType<typeof setInterval> | null = null

  // Web Speech API（Chrome/Edge/Safari 原生语音识别引擎，Chrome 通过 Google 服务器处理）
  const SpeechRecognitionCtor = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  let speechRecognition: any = null
  let webSpeechResult = ''
  let webSpeechFinalText = ''
  let webSpeechRestartCount = 0
  const MAX_WEBSPEECH_RESTARTS = 10
  const MAX_RECORDING_SECONDS = 300
  const interimText = ref('')

  const recordingDurationStr = computed(() => {
    const m = Math.floor(recordingSeconds.value / 60)
    const s = recordingSeconds.value % 60
    return `${m}:${s.toString().padStart(2, '0')}`
  })

  async function convertToWav(webmBlob: Blob): Promise<Blob> {
    const arrayBuffer = await webmBlob.arrayBuffer()
    const tempCtx = new AudioContext()
    const audioBuffer = await tempCtx.decodeAudioData(arrayBuffer)
    await tempCtx.close()
    // 重采样为 16000Hz 单声道
    const numSamples = Math.ceil(audioBuffer.duration * 16000)
    const offlineCtx = new OfflineAudioContext(1, numSamples, 16000)
    const source = offlineCtx.createBufferSource()
    source.buffer = audioBuffer
    source.connect(offlineCtx.destination)
    source.start()
    const rendered = await offlineCtx.startRendering()
    const float32 = rendered.getChannelData(0)
    // Float32 → Int16 PCM
    const pcm16 = new Int16Array(float32.length)
    for (let i = 0; i < float32.length; i++) {
      pcm16[i] = Math.max(-32768, Math.min(32767, Math.round(float32[i] * 32767)))
    }
    // 构建 WAV 文件
    const wavHeader = new ArrayBuffer(44)
    const v = new DataView(wavHeader)
    const dataSize = pcm16.length * 2
    const writeStr = (offset: number, s: string) => { for (let i = 0; i < s.length; i++) v.setUint8(offset + i, s.charCodeAt(i)) }
    writeStr(0, 'RIFF'); v.setUint32(4, 36 + dataSize, true); writeStr(8, 'WAVE')
    writeStr(12, 'fmt '); v.setUint32(16, 16, true); v.setUint16(20, 1, true)
    v.setUint16(22, 1, true); v.setUint32(24, 16000, true); v.setUint32(28, 32000, true)
    v.setUint16(32, 2, true); v.setUint16(34, 16, true)
    writeStr(36, 'data'); v.setUint32(40, dataSize, true)
    return new Blob([wavHeader, pcm16.buffer], { type: 'audio/wav' })
  }

  async function startVoiceRecording() {
    if (voiceState.value !== 'idle') return
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaStream = stream
      voiceState.value = 'recording'
      showEmojiPicker.value = false
      audioChunks = []

      // 初始化 MediaRecorder 录制音频
      mediaRecorder = new MediaRecorder(stream, {
        mimeType: MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
          ? 'audio/webm;codecs=opus'
          : 'audio/webm'
      })
      mediaRecorder.ondataavailable = (e: BlobEvent) => {
        if (e.data.size > 0) audioChunks.push(e.data)
      }
      mediaRecorder.onstop = async () => {
        // 录音结束，转换为 WAV 后上传到后端进行语音识别
        if (voiceState.value === 'transcribing' && audioChunks.length > 0) {
          const webmBlob = new Blob(audioChunks, { type: 'audio/webm' })
          try {
            const wavBlob = await convertToWav(webmBlob)
            const res = await speechRecognize(wavBlob)
            if (res?.text) {
              messageText.value += res.text
            } else {
              ElMessage.warning('语音识别未返回文本')
            }
          } catch (err: any) {
            ElMessage.warning('语音识别请求失败: ' + (err.message || '网络错误'))
          }
        }
        voiceState.value = 'idle'
        cleanupVoice()
        // voiceState 变为 idle 后 Vue 会重新创建 contenteditable div，需等渲染完成再同步文字
        nextTick(() => {
          if (chatInputRef.value && messageText.value) {
            chatInputRef.value.textContent = messageText.value
            moveCursorToEnd()
          }
        })
      }
      mediaRecorder.start(200) // 每200ms产生一个数据块

      // Web Audio API 实时波形可视化
      audioContext = new AudioContext()
      const source = audioContext.createMediaStreamSource(stream)
      analyserNode = audioContext.createAnalyser()
      analyserNode.fftSize = 64
      source.connect(analyserNode)
      startWaveformUpdate()
      // 启动录音计时器
      recordingSeconds.value = 0
      if (recordingTimer) clearInterval(recordingTimer)
      recordingTimer = setInterval(() => {
        recordingSeconds.value++
        if (recordingSeconds.value >= MAX_RECORDING_SECONDS) stopVoiceRecording()
      }, 1000)

      // 同时启动 Web Speech API 作为主引擎（Chrome/Edge/Safari 原生语音识别）
      webSpeechResult = ''
      webSpeechFinalText = ''
      webSpeechRestartCount = 0
      interimText.value = ''
      if (SpeechRecognitionCtor) initWebSpeech()
    } catch (err: any) {
      ElMessage.warning('无法访问麦克风: ' + (err.message || '请检查浏览器权限'))
      voiceState.value = 'idle'
    }
  }

  /** 初始化 Web Speech API（支持静默后自动重启 + 指数退避 + 跨重启累计结果） */
  function initWebSpeech() {
    try {
      speechRecognition = new SpeechRecognitionCtor()
      speechRecognition.lang = 'zh-CN'
      speechRecognition.continuous = true
      speechRecognition.interimResults = true
      speechRecognition.maxAlternatives = 1
      speechRecognition.onresult = (event: any) => {
        let currentTranscript = ''
        for (let i = 0; i < event.results.length; i++) {
          currentTranscript += event.results[i][0].transcript
        }
        webSpeechResult = webSpeechFinalText + currentTranscript
        interimText.value = webSpeechResult
        webSpeechRestartCount = 0
      }
      speechRecognition.onerror = (event: any) => {
        if (event.error === 'not-allowed' || event.error === 'service-not-allowed') return
        if (event.error !== 'no-speech' && event.error !== 'aborted') {
          console.warn('Web Speech API error:', event.error)
        }
      }
      speechRecognition.onend = () => {
        // Chrome 在静默约 10-15 秒后会自动停止 continuous 模式（已确认的浏览器限制）
        // 需自动重启并采用指数退避策略防止快速循环
        if (voiceState.value === 'recording' && webSpeechRestartCount < MAX_WEBSPEECH_RESTARTS) {
          webSpeechFinalText = webSpeechResult
          webSpeechRestartCount++
          const delay = Math.min(300 * webSpeechRestartCount, 2000)
          setTimeout(() => {
            if (voiceState.value === 'recording') initWebSpeech()
          }, delay)
        }
      }
      speechRecognition.start()
    } catch { /* 浏览器不支持则忽略，走后端百度API */ }
  }

  function startWaveformUpdate() {
    if (waveformTimer) clearInterval(waveformTimer)
    waveformTimer = setInterval(() => {
      if (!analyserNode || voiceState.value !== 'recording') return
      const data = new Uint8Array(analyserNode.frequencyBinCount)
      analyserNode.getByteFrequencyData(data)
      const bars: number[] = []
      const step = Math.floor(data.length / 30) || 1
      for (let i = 0; i < 30; i++) {
        const val = data[i * step] || 0
        bars.push(Math.max(2, (val / 255) * 28))
      }
      waveformBars.value = bars
    }, 60)
  }

  function stopVoiceRecording() {
    if (voiceState.value !== 'recording') return

    // 录音时间太短检查
    if (recordingSeconds.value < 1 && !webSpeechResult.trim()) {
      ElMessage.warning('录音时间太短，请至少录制1秒')
      cancelVoiceRecording()
      return
    }

    // 优先获取 Web Speech API 结果（interimResults 保证近实时更新）
    const webText = webSpeechResult.trim()
    if (speechRecognition) {
      try { speechRecognition.stop() } catch {}
      speechRecognition = null
    }
    webSpeechResult = ''
    webSpeechFinalText = ''
    interimText.value = ''

    if (webText) {
      // 主引擎成功：Web Speech API 直接提供结果，无需后端
      messageText.value += webText
      voiceState.value = 'idle'
    } else {
      // 降级兜底：Web Speech API 无结果，走后端百度API
      voiceState.value = 'transcribing'
    }

    // 通用资源清理
    if (recordingTimer) { clearInterval(recordingTimer); recordingTimer = null }
    if (waveformTimer) { clearInterval(waveformTimer); waveformTimer = null }
    if (audioContext) { audioContext.close(); audioContext = null }
    analyserNode = null
    if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null }
    // 停止录音（若 voiceState 为 idle，onstop 中不会上传后端；若为 transcribing 则触发后端识别）
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    }
  }

  function cancelVoiceRecording() {
    voiceState.value = 'idle'
    audioChunks = []
    webSpeechResult = ''
    webSpeechFinalText = ''
    interimText.value = ''
    if (speechRecognition) {
      try { speechRecognition.abort() } catch {}
      speechRecognition = null
    }
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.onstop = null // 取消时不触发上传
      mediaRecorder.stop()
    }
    cleanupVoice()
  }

  function cleanupVoice() {
    if (recordingTimer) { clearInterval(recordingTimer); recordingTimer = null }
    if (waveformTimer) { clearInterval(waveformTimer); waveformTimer = null }
    if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null }
    if (audioContext) { audioContext.close(); audioContext = null }
    analyserNode = null
    mediaRecorder = null
    waveformBars.value = Array(30).fill(2)
    if (speechRecognition) {
      try { speechRecognition.abort() } catch {}
      speechRecognition = null
    }
    webSpeechResult = ''
    webSpeechFinalText = ''
    interimText.value = ''
  }

  onUnmounted(() => {
    cleanupVoice()
  })

  // 消息长度上限（与后端 ChatConstants.MAX_MESSAGE_LENGTH 保持一致）
  const MAX_MESSAGE_LENGTH = 4000
  // 按 Unicode code point 计算真实字符数（emoji 算 1 个而非 2 个 UTF-16 unit）
  const messageTextLength = computed(() => {
    return messageText.value ? [...messageText.value].length : 0
  })
  const isMessageTooLong = computed(() => messageTextLength.value > MAX_MESSAGE_LENGTH)

  const canSend = computed(() => {
    // L3-M3-1：发送中硬禁用，防止批量场景下用户连点触发"已成功文件被二次提交"事故
    if (isSending.value) return false
    const hasText = !!messageText.value.trim()
    const hasFiles = pendingFiles.value.length > 0
    const allDone = hasFiles && pendingFiles.value.every(f => f.status === 'done')
    // 超长时禁止发送
    if (isMessageTooLong.value) return false
    return hasText || allDone
  })

  const partnerOnline = computed(() => {
    const uid = chatPartner.value?.otherUserId
    return uid ? chatStore.isUserOnline(uid) : false
  })

  /**
   * L3-M0-6：对方是否正在输入（typing indicator 呈现判据）。
   * <p>
   * 直接读 chatStore.typingUsers —— 这是 ref<Record>，过期时间戳会在 store 的 GC 定时器里被删除，
   * 触发 Vue 响应性让 UI 自动隐藏气泡，无需组件自己定时器。
   */
  const isPartnerTyping = computed(() => {
    const uid = chatPartner.value?.otherUserId
    if (typeof uid !== 'number') return false
    const exp = chatStore.typingUsers[uid]
    return typeof exp === 'number' && exp > Date.now()
  })

  const ROLE_CONFIG: Record<string, { order: number; color: string }> = {
    '管理员': { order: 1, color: '#ef4444' },
    '教师': { order: 2, color: '#6366f1' },
    '学生': { order: 3, color: '#22c55e' }
  }

  function getSubOnlineCount(items: any[]) {
    return items.filter(c => chatStore.isUserOnline(c.id)).length
  }

  const contactFilterTabs = computed(() => {
    const roleCounts: Record<string, number> = {}
    let total = 0
    for (const c of chatStore.contacts) {
      const role = c.roleName || '其他'
      roleCounts[role] = (roleCounts[role] || 0) + 1
      total++
    }
    const tabs = [{ label: '全部', count: total }]
    const sortedRoles = Object.keys(roleCounts).sort((a, b) => (ROLE_CONFIG[a]?.order || 99) - (ROLE_CONFIG[b]?.order || 99))
    for (const role of sortedRoles) {
      tabs.push({ label: role, count: roleCounts[role] })
    }
    return tabs
  })

  const activeFilterIndex = computed(() => {
    const idx = contactFilterTabs.value.findIndex(t => t.label === contactFilter.value)
    return idx >= 0 ? idx : 0
  })

  const filteredContactGroups = computed(() => {
    const keyword = contactSearch.value.trim().toLowerCase()
    const activeFilter = contactFilter.value
    const groups: { role: string; items: any[]; color: string; order: number; totalCount: number; subGroups?: { className: string; items: any[] }[] }[] = []
    const nonStudents: Record<string, any[]> = {}
    const studentsByClass: Record<string, any[]> = {}

    for (const c of chatStore.contacts) {
      if (keyword && !c.realName?.toLowerCase().includes(keyword) && !c.className?.toLowerCase().includes(keyword)) continue
      if (activeFilter !== '全部' && c.roleName !== activeFilter) continue
      if (c.roleName === '学生') {
        const cls = c.className || '未分配班级'
        if (!studentsByClass[cls]) studentsByClass[cls] = []
        studentsByClass[cls].push(c)
      } else {
        const role = c.roleName || '其他'
        if (!nonStudents[role]) nonStudents[role] = []
        nonStudents[role].push(c)
      }
    }

    for (const [role, items] of Object.entries(nonStudents)) {
      groups.push({ role, items, color: ROLE_CONFIG[role]?.color || '#94a3b8', order: ROLE_CONFIG[role]?.order || 99, totalCount: items.length })
    }

    const classEntries = Object.entries(studentsByClass).sort((a, b) => a[0].localeCompare(b[0]))
    if (classEntries.length > 0) {
      const total = classEntries.reduce((sum, [, items]) => sum + items.length, 0)
      groups.push({
        role: '学生',
        items: [],
        color: ROLE_CONFIG['学生']?.color || '#22c55e',
        order: 3,
        totalCount: total,
        subGroups: classEntries.map(([className, items]) => ({ className, items }))
      })
    }

    return groups.sort((a, b) => a.order - b.order)
  })

  function formatTime(dt: string | null) {
    if (!dt) return ''
    const d = new Date(dt)
    const now = new Date()
    if (d.toDateString() === now.toDateString()) {
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
    return `${d.getMonth() + 1}/${d.getDate()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  }

  function formatTimeFull(dt: string | null) {
    if (!dt) return ''
    const d = new Date(dt)
    const now = new Date()
    if (d.toDateString() === now.toDateString()) return '今天 ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    const yesterday = new Date(now); yesterday.setDate(now.getDate() - 1)
    if (d.toDateString() === yesterday.toDateString()) return '昨天 ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    return `${d.getMonth() + 1}月${d.getDate()}日 ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  }

  function shouldShowTimeDivider(idx: number) {
    if (idx === 0) return true
    const msgs = chatStore.currentMessages
    const curr = new Date(msgs[idx].createTime).getTime()
    const prev = new Date(msgs[idx - 1].createTime).getTime()
    return curr - prev > 5 * 60 * 1000
  }

  // ====== 置底按钮（scroll-to-bottom） ======
  const showScrollBottom = ref(false)

  function onMessagesScroll() {
    const el = messageContainer.value
    if (!el) return
    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
    showScrollBottom.value = distanceFromBottom > 100
  }

  function scrollToBottomSmooth() {
    if (messageContainer.value) {
      messageContainer.value.scrollTo({ top: messageContainer.value.scrollHeight, behavior: 'smooth' })
    }
    showScrollBottom.value = false
  }

  function scrollToBottom() {
    nextTick(() => {
      setTimeout(() => {
        if (messageContainer.value) messageContainer.value.scrollTop = messageContainer.value.scrollHeight
        showScrollBottom.value = false
      }, 80)
    })
  }

  async function openConversation(conv: any) {
    // CHAT-325：ghost 会话（有草稿但无服务端 convId）—— 走简化分支
    // 不调 chatStore.openConversation（无 convId），不拉消息历史（没历史）
    // 通过 _flushDraftForCurrentConv + 切换 chatPartner 启动对话
    if (conv?.isGhost) {
      _flushDraftForCurrentConv()  // 保存前一个会话的草稿
      _stopTyping()
      chatPartner.value = {
        otherUserId: conv.otherUserId,
        otherUserName: conv.otherUserName,
        otherAvatar: conv.otherAvatar,
      }
      chatStore.currentConversationId = null  // ghost 无 convId
      chatStore.currentMessages = []
      currentView.value = 'chat'
      if (conv.otherUserId) chatStore.checkUserOnline(conv.otherUserId)
      // 回填草稿
      const myId = userStore.info?.userId
      if (myId && typeof conv.otherUserId === 'number') {
        const draft = loadDraftByPeer(myId, conv.otherUserId)
        messageText.value = draft?.content || ''
      } else {
        messageText.value = ''
      }
      await nextTick()
      if (chatInputRef.value) {
        chatInputRef.value.textContent = messageText.value
        if (messageText.value) moveCursorToEnd()
      }
      return
    }

    // L3-M0-8：切会话前先把当前输入框的内容立即持久化（绕过 debounce），避免丢失
    _flushDraftForCurrentConv()
    // L3-M0-6：切换会话时先停止对前一个会话对方的 typing 提示
    // 否则前一个对方的界面会残留"正在输入..."直到 6s TTL 过期
    _stopTyping()
    chatPartner.value = conv
    messagesLoading.value = true
    currentView.value = 'chat'
    const minDelay = new Promise(resolve => setTimeout(resolve, 600))
    await Promise.all([chatStore.openConversation(conv.id), minDelay])
    if (conv.otherUserId) chatStore.checkUserOnline(conv.otherUserId)
    messagesLoading.value = false

    // L3-M0-8 + CHAT-325：读草稿并回填输入框（对齐 WhatsApp/微信打开会话立即显示草稿）
    // 按 peerUserId 读取（存量会话已通过 migrateLegacyDraftsToPeerKeyed 迁移到新格式）
    const myId = userStore.info?.userId
    if (myId && typeof conv.otherUserId === 'number') {
      const draft = loadDraftByPeer(myId, conv.otherUserId)
      messageText.value = draft?.content || ''
    } else {
      messageText.value = ''
    }

    await nextTick()
    // 草稿回填到 contenteditable，聚焦到末尾
    if (chatInputRef.value) {
      chatInputRef.value.textContent = messageText.value
      if (messageText.value) moveCursorToEnd()
    }
    scrollToBottom()
  }

  /**
   * L3-M0-8 + CHAT-325：响应式 draft 缓存 —— 按 peerUserId（稳定标识）索引。
   * <p>
   * 职责：
   * <ul>
   *   <li>会话列表"[草稿]"前缀实时渲染（对齐微信 PC）</li>
   *   <li>ghost 会话卡片合成 —— 有草稿但无服务端会话的 peer 渲染为虚拟列表项</li>
   * </ul>
   * <p>
   * 设计：localStorage 是真相源，此 ref 仅为 UI 层响应性适配；
   * 所有改动走 {@link _syncDraftCache} 保持一致。
   */
  const peerDraftMap = ref<Record<number, DraftRecord>>({})

  /**
   * L3-M0-8 + CHAT-325：把当前会话的输入框内容立即持久化到 localStorage。
   * <p>
   * 触发时机：切会话、backToList、组件卸载、页面关闭等"必须立即落盘"的场景，
   * 绕过 debounce 500ms 窗口防止数据丢失。
   * <p>
   * CHAT-325：改用 chatPartner.otherUserId 作为 key，兼顾存量会话 + ghost 零会话场景。
   */
  function _flushDraftForCurrentConv() {
    const myId = userStore.info?.userId
    const peerUserId = chatPartner.value?.otherUserId
    if (!myId || typeof peerUserId !== 'number') return
    const meta = {
      peerUserName: chatPartner.value?.otherUserName,
      peerUserAvatar: chatPartner.value?.otherAvatar,
    }
    saveDraftByPeer(myId, peerUserId, messageText.value, meta)
    _syncDraftCache(peerUserId, messageText.value, meta)
  }

  /**
   * L3-M0-8 + CHAT-325：同步响应式 peerDraftMap —— 空串/whitespace 相当于删除条目。
   * <p>
   * meta（peerUserName/peerUserAvatar）在首次写入 ghost 草稿时必传，用于渲染 ghost 卡片；
   * 存量会话场景下 meta 可省略，因为渲染会话卡片时不依赖 peerDraftMap 里的 meta。
   */
  function _syncDraftCache(
    peerUserId: number | string,
    content: string,
    meta?: { peerUserName?: string; peerUserAvatar?: string },
  ) {
    const id = Number(peerUserId)
    if (!Number.isFinite(id)) return
    const trimmed = (content ?? '').trim()
    if (trimmed) {
      peerDraftMap.value[id] = {
        content: trimmed,
        peerUserId: id,
        peerUserName: meta?.peerUserName ?? peerDraftMap.value[id]?.peerUserName,
        peerUserAvatar: meta?.peerUserAvatar ?? peerDraftMap.value[id]?.peerUserAvatar,
        updatedAt: Date.now(),
      }
    } else {
      delete peerDraftMap.value[id]
    }
  }

  /**
   * CHAT-325：从 localStorage 重新加载 peer-keyed 草稿到响应式 map。
   * <p>
   * 调用时机：onMounted 初始化 ↔ legacy 迁移完成后刷新。
   * 幂等——可重复调用不产生副作用。
   */
  function _reloadPeerDraftMap() {
    const myId = userStore.info?.userId
    if (!myId) return
    const drafts = loadAllDraftsOfUser(myId)
    const next: Record<number, DraftRecord> = {}
    drafts.forEach((record, peerUserId) => {
      next[peerUserId] = record
    })
    peerDraftMap.value = next
  }

  /**
   * CHAT-325：Ghost 会话卡片合成——零会话草稿核心机制。
   * <p>
   * <b>场景</b>：用户选择联系人后在输入框打字，但尚未发送首条消息时，
   * 后端 conversations 表还没记录，不可能从 getChatConversations API 返回。
   * 这类"零会话草稿"在旧架构里不可见（与 Signal #7612 自 2018 未修复同型 bug）。
   * <p>
   * <b>修复</b>：遍历 peerDraftMap，对"有草稿但主列表/归档列表都不包含此 peerUserId"
   * 的条目合成 ghost 卡片插入主列表。
   * <p>
   * 业界对标：Telegram/WhatsApp/微信 PC —— 草稿作为一级实体，应当在列表独立可见。
   */
  const ghostConversations = computed(() => {
    const existingPeerIds = new Set<number>()
    chatStore.conversations.forEach((c: any) => {
      if (typeof c?.otherUserId === 'number') existingPeerIds.add(c.otherUserId)
    })
    chatStore.archivedConversations.forEach((c: any) => {
      if (typeof c?.otherUserId === 'number') existingPeerIds.add(c.otherUserId)
    })
    const result: any[] = []
    for (const [peerIdStr, record] of Object.entries(peerDraftMap.value)) {
      const peerUserId = Number(peerIdStr)
      if (!Number.isFinite(peerUserId)) continue
      if (existingPeerIds.has(peerUserId)) continue  // 已有服务端会话 → 不合成 ghost
      if (!record?.content || !record.content.trim()) continue
      result.push({
        id: `ghost-${peerUserId}`,
        isGhost: true,
        otherUserId: peerUserId,
        otherUserName: record.peerUserName || '联系人',
        otherAvatar: record.peerUserAvatar || null,
        lastMessage: '',  // 列表通过 hasDraftPreview 判定显示"[草稿]"前缀
        lastMessageTime: new Date(record.updatedAt).toISOString(),
        unreadCount: 0,
        pinned: false,
        muted: false,
        otherClassName: null,
        otherRoleId: 0,
      })
    }
    return result
  })

  /**
   * CHAT-325：合并后的主会话展示列表（真实 + ghost），按"置顶优先 + 时间倒排"排序。
   * <p>
   * 归档列表不与 ghost 混合（归档本身语义与"刚打字的活跃草稿"相反）。
   * <p>
   * 模板切换：{@code v-for="conv in chatStore.conversations"} → {@code displayConversations}
   */
  const displayConversations = computed(() => {
    const real = chatStore.conversations as any[]
    const ghosts = ghostConversations.value
    if (ghosts.length === 0) return real
    return [...real, ...ghosts].sort((a, b) => {
      if (!!a.pinned !== !!b.pinned) return a.pinned ? -1 : 1
      const ta = new Date(a.lastMessageTime || 0).getTime()
      const tb = new Date(b.lastMessageTime || 0).getTime()
      return tb - ta
    })
  })

  /**
   * CHAT-325：conversations 首次加载完成后触发 legacy 草稿迁移。
   * <p>
   * 为什么用 watch 而不用 onMounted：conversations 是 lazy-loaded——
   * 用户切到 messages space 才通过 switchSpace 加载，onMounted 时还是空数组。
   * <p>
   * 幂等：_legacyMigrated 标志保证仅执行一次，避免重复扰动草稿时间戳。
   */
  let _legacyMigrated = false
  watch(
    () => chatStore.conversations.length + chatStore.archivedConversations.length,
    (n) => {
      if (_legacyMigrated) return
      if (n === 0) return
      const myId = userStore.info?.userId
      if (!myId) return
      const convIdToPeer = new Map<number, { peerUserId: number; peerUserName?: string; peerUserAvatar?: string }>()
      const allConvs = [...chatStore.conversations, ...chatStore.archivedConversations]
      allConvs.forEach((c: any) => {
        if (typeof c?.id === 'number' && typeof c?.otherUserId === 'number') {
          convIdToPeer.set(c.id, {
            peerUserId: c.otherUserId,
            peerUserName: c.otherUserName,
            peerUserAvatar: c.otherAvatar,
          })
        }
      })
      const migrated = migrateLegacyDraftsToPeerKeyed(myId, convIdToPeer)
      if (migrated > 0) {
        // eslint-disable-next-line no-console
        console.info('[CHAT-325] legacy 草稿迁移完成，共', migrated, '条')
        _reloadPeerDraftMap()
      }
      _legacyMigrated = true
    },
    { immediate: true },
  )

  async function startChatWith(contact: any) {
    activeSpace.value = 'messages'
    const existing = chatStore.conversations.find((c: any) => c.otherUserId === contact.id)
    if (existing) {
      await openConversation(existing)
      return
    }
    // 无存量会话——进入"零会话"对话界面（首条消息发送后后端才创建 convId）
    // CHAT-325：先清理上个会话的草稿 + typing，再切换 chatPartner
    _flushDraftForCurrentConv()
    _stopTyping()
    chatPartner.value = { otherUserId: contact.id, otherUserName: contact.realName, otherAvatar: contact.avatar }
    chatStore.currentConversationId = null  // 新会话无 convId
    currentView.value = 'chat'
    chatStore.currentMessages = []
    chatStore.checkUserOnline(contact.id)
    // CHAT-325：回填该联系人已有的 ghost 草稿（对齐 Telegram/微信 PC 打开聊天立即显示草稿）
    const myId = userStore.info?.userId
    if (myId) {
      const draft = loadDraftByPeer(myId, contact.id)
      messageText.value = draft?.content || ''
    } else {
      messageText.value = ''
    }
    await nextTick()
    if (chatInputRef.value) {
      chatInputRef.value.textContent = messageText.value
      if (messageText.value) moveCursorToEnd()
    }
  }

  /**
   * 批量发送：图片 + 文件 + 文本 —— 工业级方案（L3-M3-1 终极版）
   *
   * 对齐全球权威 IM 生产架构：
   * - **Slack** chat.postMessage batch 一次请求多消息
   * - **Telegram** sendMediaGroup 原子媒体组
   * - **Discord** 单消息多附件
   * - **WhatsApp / 微信** 失败消息红叹号点击重发
   *
   * 核心架构（全部由后端 /messages:batch 根治）：
   * - **批次级限流**：整批 1 permit（而非 N permit），5 个 PDF 一次搞定不撞限流
   * - **Partial Success**：per-item clientMsgId 精确定位失败项 → 气泡 ❗ 独立重发
   * - **严格有序**：批次内按数组顺序写库，接收方看到的顺序 100% 与发送一致
   * - **共享校验**：sender/receiver/权限/会话 upsert 批次只执行 1 次 → 性能翻倍
   * - **per-item 幂等**：每项独立 clientMsgId，重复提交自动 already_sent
   *
   * 前端职责收敛（相比旧方案）：
   * - ❌ 不再前端预节流（后端批次级限流根治）
   * - ❌ 不再逐条循环（一次请求完成）
   * - ✅ 保留发送锁 isSending（防连点双发）
   * - ✅ 保留原子清理 pendingFiles（发送后立即清空，失败靠气泡 ❗ 兜底）
   */
  async function handleSend() {
    if (isSending.value) return // 去重锁：双击 / 回车重入保护
    const receiverId = chatPartner.value?.otherUserId
    if (!receiverId) return

    const hasFiles = pendingFiles.value.length > 0
    const text = messageText.value.trim()
    if (!hasFiles && !text) return

    // 前端长度校验（与后端 ChatConstants.MAX_MESSAGE_LENGTH 一致）
    if (text && isMessageTooLong.value) {
      ElMessage.warning(`消息内容不能超过 ${MAX_MESSAGE_LENGTH} 字符`)
      return
    }

    if (hasFiles) {
      if (pendingFiles.value.some(f => f.status === 'uploading')) { ElMessage.warning('文件正在上传中，请稍候'); return }
      if (pendingFiles.value.some(f => f.status === 'error')) { ElMessage.warning('有文件上传失败，请重试或移除'); return }
    }

    // 1️⃣ 构造批次 contents 数组（顺序：图片 → 文件 → 文本，符合"附件+说明"语义）
    const snapshot = hasFiles ? [...pendingFiles.value] : []
    const images = snapshot.filter(f => f.isImage)
    const files = snapshot.filter(f => !f.isImage)
    const contents: string[] = []

    // 图片：单张独立；多张合并为一条（对齐 Telegram sendMediaGroup）
    if (images.length === 1) {
      contents.push(`[img]${images[0].uploadUrl}[/img]`)
    } else if (images.length > 1) {
      contents.push(`[imgs]${images.map(f => f.uploadUrl).join('||')}[/imgs]`)
    }
    // 每个非图片文件独立一条消息（与现有气泡渲染约定一致）
    for (const pf of files) {
      contents.push(`[file:${pf.ext}:${pf.name}]${pf.uploadUrl}[/file]`)
    }
    // 文本放最后（"图 + 文字说明"常见语序）
    if (text) contents.push(text)

    // 批次上限兜底（后端硬约束 20，前端防呆提前拦截）
    if (contents.length > 20) {
      ElMessage.warning('单次最多发送 20 条消息，请分批操作')
      return
    }

    isSending.value = true
    try {
      // 2️⃣ 调用 store 批量 action —— 乐观 UI + REST 一次完成 + per-item 升级
      const { sentCount, failedCount, rateLimited, failedTempIds } =
        await chatStore.sendMessagesBatch(receiverId, contents)

      // 3️⃣ 原子清理输入框（预览区 + 文本 + 草稿）
      // 成功/失败都清 —— 失败项已在聊天气泡里标 ❗，可点击重发（幂等）
      pendingFiles.value = []
      // CHAT-325：清草稿统一改用 peerUserId（= receiverId），新老会话都适用
      // 特别是 ghost 会话首发场景：convId 此时还为 null，但 peerUserId 始终可用
      if (text && failedCount < contents.length) {
        // 至少有一条成功 或 全部失败（但已有 ❗ 气泡可重发）→ 统一清空输入框
        messageText.value = ''
        if (chatInputRef.value) chatInputRef.value.textContent = ''
        _stopTyping()
        const myId = userStore.info?.userId
        if (myId && typeof receiverId === 'number') {
          clearDraftByPeer(myId, receiverId)
          _syncDraftCache(receiverId, '')
        }
      } else if (text && failedCount === contents.length && rateLimited) {
        // 特殊情况：整批限流失败且只有文本 → 保留文本（便于用户稍后直接点发送重试）
        // 但预览区文件已清 + 失败气泡已在聊天区 ❗ 提示用户
      } else if (text) {
        // 其它失败：同样清空（失败气泡已有重发入口）
        messageText.value = ''
        if (chatInputRef.value) chatInputRef.value.textContent = ''
        _stopTyping()
        const myId = userStore.info?.userId
        if (myId && typeof receiverId === 'number') {
          clearDraftByPeer(myId, receiverId)
          _syncDraftCache(receiverId, '')
        }
      }

      // 4️⃣ 结果提示（按业界规范：批量操作仅弹 1 次 toast，优先限流 > 普通失败 > 成功静默）
      if (rateLimited) {
        ElMessage.warning({
          message: `发送过于频繁，${failedCount} 条未发出，点击消息上的 ❗ 可重发`,
          duration: 4000,
        })
      } else if (failedCount > 0 && sentCount === 0) {
        ElMessage.error(`${failedCount} 条消息发送失败，点击消息上的 ❗ 可重发`)
      } else if (failedCount > 0) {
        ElMessage.warning(`${sentCount} 条成功，${failedCount} 条失败，点击 ❗ 可重发`)
      }
      // 全部成功：静默（对齐 WhatsApp/微信，不打扰用户）
      void failedTempIds  // 当前不需要额外处理，失败项已在 store 标 failed

      // 5️⃣ 会话列表刷新 + 滚动
      showEmojiPicker.value = false
      await chatStore.loadConversations()
      const conv = chatStore.conversations.find((c: any) => c.otherUserId === receiverId)
      if (conv) {
        chatPartner.value = conv
        if (chatStore.currentConversationId !== conv.id) {
          await chatStore.openConversation(conv.id)
        } else {
          chatStore.currentConversationId = conv.id
        }
      }
      scrollToBottom()
    } finally {
      isSending.value = false
    }
  }

  /**
   * 重发失败的消息（点击失败状态图标触发）
   */
  async function retryMessage(msg: any) {
    if (!msg?.tempId || msg.status !== 'failed') return
    try {
      await chatStore.resendMessage(msg.tempId)
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '重发失败')
    }
  }

  // ========== L3：消息撤回 / 强删 ==========

  /**
   * 判断是否为管理员角色（复用 RoleConstants.ADMIN_ROLE_ID = 1）
   */
  const isAdmin = computed(() => userStore.info?.roleId === 1)

  /**
   * 撤回窗口（秒）—— 与后端 ChatConstants.MESSAGE_RECALL_WINDOW_SECONDS 保持一致。
   * 前端与后端双重校验：前端隐藏不可撤回消息的按钮（UX），后端仍强制校验（安全）。
   */
  const RECALL_WINDOW_SECONDS = 120

  /**
   * 操作菜单是否显示（撤回/强删 任一可行即显示容器）
   * 条件：非自动乐观 UI 临时消息（需要真实 id） + （可撤回 OR 可强删）
   */
  function canShowMessageActions(msg: any): boolean {
    if (!msg || typeof msg.id !== 'number') return false
    return canRecallMessage(msg) || canAdminDeleteMessage(msg)
  }

  /**
   * 是否可撤回：自己发的 + 未删除 + 2 分钟内
   */
  function canRecallMessage(msg: any): boolean {
    if (!msg?.isMe || msg.deleted) return false
    if (typeof msg.id !== 'number') return false // 乐观 UI 占位不允许
    const t = msg.createTime ? new Date(msg.createTime).getTime() : 0
    if (!t) return false
    return (Date.now() - t) / 1000 < RECALL_WINDOW_SECONDS
  }

  /**
   * 是否可强删：管理员 + 未删除 + 非自己消息（自己消息走撤回按钮）
   */
  function canAdminDeleteMessage(msg: any): boolean {
    if (!isAdmin.value) return false
    if (!msg || msg.deleted || msg.isMe) return false
    if (typeof msg.id !== 'number') return false
    return true
  }

  async function doRecallMessage(msg: any) {
    try {
      await chatStore.recallMyMessage(msg.id)
      ElMessage.success('已撤回')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '撤回失败')
    }
  }

  /**
   * L3-M0-4：是否可以"重新编辑"这条撤回消息（对齐微信/QQ 2 分钟窗口）。
   * <p>
   * 判据链：
   * 1. 必须是已撤回的消息（msg.deleted === true）
   * 2. 必须是自己发的（避免用户点别人撤回的消息）
   * 3. 必须是真实 DB id（排除 tmp_ 乐观占位）
   * 4. 必须是文字消息（messageType=1，或没有类型字段的老数据也兜底允许）
   * 5. 撤回后不超过 2 分钟 —— 这里用 createTime 近似替代 deletedAt（后者没下发到前端历史消息）
   *    严格意义上用 deletedAt 更准，但撤回窗口和编辑窗口都是 2min，误差 < 几秒可忽略
   */
  function canReEditRecalled(msg: any): boolean {
    if (!msg?.deleted || !msg.isMe) return false
    if (typeof msg.id !== 'number') return false
    // messageType 缺失时默认允许（兼容旧数据，服务端会二次校验）
    if (msg.messageType != null && msg.messageType !== 1) return false
    const refTime = msg.deletedAt ? new Date(msg.deletedAt).getTime()
                  : msg.createTime ? new Date(msg.createTime).getTime() : 0
    if (!refTime) return false
    return (Date.now() - refTime) / 1000 < RECALL_WINDOW_SECONDS
  }

  /**
   * L3-M0-4：点击"重新编辑" — 拉后端 2min 草稿回填输入框并 focus。
   * <p>
   * 边界：
   * - 草稿过期 → 后端返回 content=null → 提示 "草稿已过期，无法重新编辑"
   * - 网络错误 → 降级提示，不影响其他消息交互
   * <p>
   * UX：回填后聚焦输入框末尾，让用户立即可以继续编辑。
   */
  async function doReEditRecalled(msg: any) {
    try {
      const res: any = await getRecallDraft(msg.id)
      const content = res?.content
      if (content == null || content === '') {
        ElMessage.info('草稿已过期，无法重新编辑')
        return
      }
      // 回填到 contenteditable 输入框
      messageText.value = content
      if (chatInputRef.value) {
        chatInputRef.value.textContent = content
        moveCursorToEnd()
        chatInputRef.value.focus()
      }
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '获取草稿失败')
    }
  }

  async function doAdminDeleteMessage(msg: any) {
    try {
      await chatStore.adminDeleteMessage(msg.id)
      ElMessage.success('已删除')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
    }
  }

  // ========== L3：会话列表右键菜单 ==========

  const convContextMenu = reactive({
    visible: false,
    x: 0,
    y: 0,
    conv: null as any,
    // L3-M1-4：标记右键的会话来源（主列表 vs 归档列表），决定显示"归档"还是"取消归档"
    isArchived: false,
  })

  /**
   * L3-bugfix：菜单 DOM 引用，供 VueUse onClickOutside 精确判断点击边界。
   * <p>
   * 使用 ref 而非手动 document.addEventListener 的原因：
   * - onClickOutside 底层使用 click 事件（而非 pointerdown/contextmenu），不会拦截右键操作
   * - 自动处理 v-if 卸载时的清理（无残留监听器）
   * - VueUse 天然兼容 Teleport / iframe / Shadow DOM 边界
   */
  const convContextMenuRef = ref<HTMLElement | null>(null)

  /**
   * L3-bugfix：菜单尺寸估算，用于视口边界 clamp（场景 7）。
   * 与 .conv-context-menu 的 CSS 尺寸对齐；保守估计大于实际尺寸即可，多余空间不影响视觉。
   */
  const CONV_MENU_ESTIMATED_WIDTH = 160
  const CONV_MENU_ESTIMATED_HEIGHT = 180

  /**
   * L3-M1-4：归档卡片的展开/折叠状态。默认折叠以保持主列表视觉聚焦。
   */
  const archiveExpanded = ref(false)

  /**
   * L3-M1-4：归档卡片副标题 — 显示前几个归档会话的对方姓名，帮用户快速识别是否有需要找回的会话。
   * 例："陶展、张老师 等 3 个会话"
   */
  const archivedSubtitle = computed(() => {
    const arr = chatStore.archivedConversations
    if (!arr || arr.length === 0) return ''
    const names = arr.slice(0, 2).map((c: any) => c.otherUserName || '未知').filter(Boolean)
    if (arr.length <= 2) return names.join('、')
    return `${names.join('、')} 等 ${arr.length} 个会话`
  })

  /**
   * L3-bugfix：会话右键菜单打开（根治"一次性激活"bug 的终极实现）。
   * <p>
   * <b>对齐全球顶级 IM 桌面端规范</b>（Windows Explorer / macOS Finder /
   * WhatsApp Desktop / Slack Desktop / Telegram Desktop / 微信 PC 端），
   * 覆盖 8 大真实生产场景：
   * <ol>
   *   <li>点菜单外部关闭 → {@code onClickOutside}（VueUse 底层用 click 事件，不干扰右键）</li>
   *   <li>Escape 键关闭 → {@code onKeyStroke('Escape')}（OS 级键盘规范）</li>
   *   <li>再次右键其他卡片：先关旧、再开新（下方 visible=false → nextTick 后 true）</li>
   *   <li>再次右键同一卡片：幂等重新打开（新坐标刷新）</li>
   *   <li>窗口失焦（Alt+Tab 切应用）关闭 → {@code useEventListener(window, 'blur')}</li>
   *   <li>任意容器滚动时关闭 → {@code useEventListener(document, 'scroll', {capture:true})}</li>
   *   <li>菜单超出视口自动 clamp → 本函数 MENU_ESTIMATED_* 边界约束</li>
   *   <li>组件卸载自动清理 → VueUse 天然 onScopeDispose（无内存泄漏）</li>
   * </ol>
   * <p>
   * <b>旧方案的致命缺陷（已根治）</b>：
   * {@code document.addEventListener('contextmenu', close, {once:true})} 监听器在
   * 99% 用户路径下（用户用 click 而非再次右键关菜单）永远不会被 once 自动移除，
   * 残留在 document 上。下次右键时，新菜单打开后立即被残留监听器关闭 → "只能用一次"。
   */
  function onConvContextMenu(e: MouseEvent, conv: any, isArchived = false) {
    // CHAT-325：ghost 会话（未建立服务端会话的草稿卡片）不开启右键菜单
    // 原因：置顶 / 免打扰 / 归档 全部需要 server-side convId，ghost 无 convId 无法执行。
    // 用户若要删除 ghost 草稿，可进入对话后清空输入框（空草稿自动清除机制）。
    if (conv?.isGhost) return

    // 场景 3/4：先强制关闭可能存在的旧菜单（幂等 —— 即使原本未开也无副作用）
    // 这一步对「连续右键不同卡片」「在当前菜单上右键切换目标」等场景至关重要
    convContextMenu.visible = false

    // 场景 7：视口边界 clamp（菜单永不越出屏幕，对齐 Windows Explorer）
    let x = e.clientX
    let y = e.clientY
    if (x + CONV_MENU_ESTIMATED_WIDTH > window.innerWidth) {
      x = window.innerWidth - CONV_MENU_ESTIMATED_WIDTH - 4
    }
    if (y + CONV_MENU_ESTIMATED_HEIGHT > window.innerHeight) {
      y = window.innerHeight - CONV_MENU_ESTIMATED_HEIGHT - 4
    }
    if (x < 0) x = 4
    if (y < 0) y = 4

    convContextMenu.x = x
    convContextMenu.y = y
    convContextMenu.conv = conv
    convContextMenu.isArchived = isArchived

    // nextTick 确保 v-if 先经历 "false → DOM 卸载" 再 "true → DOM 重新挂载"
    // 这让 onClickOutside 的 ref 能感知到「新的」DOM 元素，避免状态错乱
    nextTick(() => {
      convContextMenu.visible = true
    })
  }

  /**
   * L3-bugfix：关闭右键菜单（统一出口）。
   * <p>
   * 所有关闭路径（点选项、点外部、Escape、窗口失焦、滚动、组件卸载）最终都汇聚到此函数，
   * 保证状态一致性 + 无残留副作用。
   */
  function closeConvContextMenu() {
    if (!convContextMenu.visible) return  // 已关闭时短路，避免无谓的响应式更新
    convContextMenu.visible = false
    convContextMenu.conv = null
    convContextMenu.isArchived = false
  }

  // ========== L3-bugfix：右键菜单生命周期管理（VueUse 组合）==========
  // 场景 1：点菜单外部关闭（核心修复）
  // onClickOutside 底层使用 click 事件（不监听 contextmenu/pointerdown 主路径），
  // 天然支持「右键外部区域时不关当前菜单，由 onConvContextMenu 接管切换目标」
  onClickOutside(convContextMenuRef, () => {
    closeConvContextMenu()
  })

  // 场景 2：Escape 键关闭（对齐 OS 级键盘规范）
  onKeyStroke('Escape', () => {
    if (convContextMenu.visible) closeConvContextMenu()
  })

  // 场景 5：窗口失焦关闭（用户 Alt+Tab 切应用时避免残留菜单）
  useEventListener(window, 'blur', () => {
    if (convContextMenu.visible) closeConvContextMenu()
  })

  // 场景 6：任意容器滚动时关闭（capture=true 能捕获所有嵌套滚动容器事件，
  // 包括会话列表内滚、浏览器主滚动、其他业务组件滚动 —— 一次监听全覆盖）
  // passive=true 声明不调用 preventDefault，让浏览器优化滚动性能
  useEventListener(document, 'scroll', () => {
    if (convContextMenu.visible) closeConvContextMenu()
  }, { capture: true, passive: true })

  // 场景补充：右键点到菜单外的空白 / 其他组件时也关闭菜单，
  // 避免「自定义菜单 + 浏览器原生菜单」同时出现的糟糕 UX。
  // 若右键目标是会话卡片，onConvContextMenu 会接管「关旧开新」流程（场景 3）。
  useEventListener(document, 'contextmenu', (e: MouseEvent) => {
    if (!convContextMenu.visible) return
    const menuEl = convContextMenuRef.value
    if (menuEl && menuEl.contains(e.target as Node)) return  // 在菜单内右键：不关
    const targetEl = e.target as HTMLElement
    if (targetEl?.closest?.('.msg-conv-card')) return  // 右键其他卡片：由 onConvContextMenu 接管
    closeConvContextMenu()
  }, { capture: true })

  async function doHideConversation() {
    const conv = convContextMenu.conv
    closeConvContextMenu()
    if (!conv?.id) return
    try {
      await chatStore.hideConversationLocal(conv.id)
      // L3-M1-4：归档后自动展开归档卡片一次，让用户看到"会话去哪了"，降低"是不是被删了"的焦虑
      archiveExpanded.value = true
      ElMessage.success('已归档，可在"已归档会话"中找回')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '归档失败')
    }
  }

  async function doUnhideConversation() {
    const conv = convContextMenu.conv
    closeConvContextMenu()
    if (!conv?.id) return
    try {
      await chatStore.unhideConversationLocal(conv.id)
      ElMessage.success('已恢复到消息列表')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '恢复失败')
    }
  }

  /**
   * L3-M0-7：会话置顶 / 取消置顶右键菜单 handler。
   * 上限 5 个由后端强制校验；超限时后端抛业务异常，这里捕获后端的 message 透传给用户。
   */
  async function doTogglePin(pinned: boolean) {
    const conv = convContextMenu.conv
    closeConvContextMenu()
    if (!conv?.id) return
    try {
      await chatStore.setConversationPinnedLocal(conv.id, pinned)
      ElMessage.success(pinned ? '已置顶' : '已取消置顶')
    } catch (e: any) {
      // 关键 UX：后端"最多可置顶 5 个会话"等业务错误必须透传，让用户知道上限存在
      ElMessage.error(e?.response?.data?.message || e?.message || (pinned ? '置顶失败' : '取消置顶失败'))
    }
  }

  /**
   * L3-M0-7：会话免打扰 / 取消免打扰右键菜单 handler。
   */
  async function doToggleMute(muted: boolean) {
    const conv = convContextMenu.conv
    closeConvContextMenu()
    if (!conv?.id) return
    try {
      await chatStore.setConversationMutedLocal(conv.id, muted)
      ElMessage.success(muted ? '已设为免打扰' : '已取消免打扰')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || (muted ? '操作失败' : '操作失败'))
    }
  }

  /**
   * L3-bugfix（CHAT-307 草稿持久化 bug 根治版）：统一的「离开当前会话」清理出口。
   * <p>
   * <b>对齐全球顶级 IM 桌面端规范</b>（WhatsApp Web / Telegram Web /
   * Slack Desktop / 微信 PC），所有「离开当前聊天」的路径都必须调用此函数：
   * <ul>
   *   <li>{@link backToList} —— 返回消息列表（仍在抽屉内）</li>
   *   <li>{@link closeChat} —— 关闭整个聊天抽屉</li>
   *   <li>{@code pagehide} / {@code onUnmounted} —— 组件 / 页面级卸载（已单独兜底）</li>
   * </ul>
   * <p>
   * <b>防止跨会话泄漏的 4 类状态</b>：
   * <ol>
   *   <li>草稿未 flush 到 localStorage（&lt; 500ms debounce 尚未执行的数据丢失）</li>
   *   <li>{@code chatStore.currentConversationId} 残留 →
   *       {@link hasDraftPreview} 误判 / 未读计数误判 / WS 推送被过滤</li>
   *   <li>typing START 心跳残留 → 对方看到假「正在输入」直到 6s TTL</li>
   *   <li>{@code chatPartner} 残留 → 下次打开抽屉时可能误启用旧会话对象</li>
   * </ol>
   * <p>
   * <b>执行顺序严格依赖</b>：
   * flush 草稿需读取 currentConversationId；_stopTyping 需读取 chatPartner.otherUserId；
   * 故必须「先用后清」— 最后才把两个依赖状态置 null。
   * <p>
   * 业界对标：WhatsApp Web 的 {@code leaveChat()} / Telegram Web 的
   * {@code clearOpenedChat} / Slack 的 {@code ACTIVE_CONVERSATION_CHANGED(null)}。
   */
  function _leaveCurrentConversation() {
    _flushDraftForCurrentConv()                // ① 草稿立即落盘（绕过 debounce）
    _stopTyping()                              // ② 停 typing 心跳并发最后一次 STOP
    chatStore.currentConversationId = null     // ③ 清理 Pinia 全局会话标识（核心）
    chatPartner.value = null                   // ④ 清理组件本地对话对象
  }

  function backToList() {
    _leaveCurrentConversation()
    currentView.value = 'list'
    activeSpace.value = 'messages'
  }

  let _statusPollTimer: ReturnType<typeof setInterval> | null = null

  function startStatusPolling() {
    stopStatusPolling()
    _statusPollTimer = setInterval(() => {
      if (isDrawerVisible.value && chatStore.contacts.length > 0) {
        chatStore.syncAllOnlineStatus()
      }
    }, 30000)
  }

  function stopStatusPolling() {
    if (_statusPollTimer) {
      clearInterval(_statusPollTimer)
      _statusPollTimer = null
    }
  }

  async function openChat() {
    isDrawerVisible.value = true
    currentView.value = 'list'
    activeSpace.value = 'home'
    spaceLoaded.messages = false
    spaceLoaded.contacts = false
    try {
      // L3-M1-4：首屏三个接口并行 - 主列表 / 归档列表 / 联系人，
      // 相比串行能少 2x RTT（对于弱网用户感知明显）
      await Promise.all([
        chatStore.loadConversations(),
        chatStore.loadArchivedConversations(),
        chatStore.loadContacts(),
      ])
    } finally {
      spaceLoaded.messages = true
      spaceLoaded.contacts = true
    }
    chatStore.syncAllOnlineStatus()
    startStatusPolling()
  }

  /**
   * L3-bugfix（CHAT-307）：关闭聊天抽屉前必须统一清理会话级状态。
   * <p>
   * 旧版致命缺陷：仅设 isDrawerVisible=false，未清理 Pinia 的
   * {@code currentConversationId}，导致重开抽屉后 {@link hasDraftPreview}
   * 误判「还在当前会话」→ [草稿] 前缀消失；同时 typing 心跳残留、
   * 输入框草稿 &lt; 500ms 可能丢失等连锁问题。
   */
  function closeChat() {
    // 核心：统一清理出口（flush 草稿 + 停 typing + 清 store + 清 partner）
    _leaveCurrentConversation()
    // 复位视图到初始状态，确保下次打开抽屉时 UX 干净（不会残留在上次的 chat 视图）
    currentView.value = 'list'
    isDrawerVisible.value = false
    stopStatusPolling()
  }

  // 监听消息列表变化，自动滚动到底部（处理 WebSocket 收到的新消息）
  watch(() => chatStore.currentMessages.length, () => {
    if (currentView.value === 'chat') scrollToBottom()
  })

  // ====== 自定义浮动滚动条（QQ 风格，零布局偏移） ======
  const spaceContentRef = ref<HTMLElement | null>(null)
  const csHover = ref(false)
  const csHasScroll = ref(false)
  const csThumbH = ref(0)
  const csThumbT = ref(0)
  let _csScrollEl: HTMLElement | null = null
  let _csRaf = 0

  function updateScrollThumb() {
    const el = _csScrollEl
    if (!el) { csHasScroll.value = false; return }
    const { scrollHeight, clientHeight, scrollTop } = el
    csHasScroll.value = scrollHeight > clientHeight
    if (csHasScroll.value) {
      const ratio = clientHeight / scrollHeight
      csThumbH.value = Math.max(ratio * clientHeight, 24)
      csThumbT.value = (scrollTop / (scrollHeight - clientHeight)) * (clientHeight - csThumbH.value)
    }
  }

  function onSpaceScroll() {
    if (_csRaf) cancelAnimationFrame(_csRaf)
    _csRaf = requestAnimationFrame(updateScrollThumb)
  }

  function bindScrollEl() {
    if (_csScrollEl) _csScrollEl.removeEventListener('scroll', onSpaceScroll)
    nextTick(() => {
      _csScrollEl = spaceContentRef.value?.querySelector('.space-view') || null
      if (_csScrollEl) {
        _csScrollEl.addEventListener('scroll', onSpaceScroll, { passive: true })
        updateScrollThumb()
      } else {
        csHasScroll.value = false
      }
    })
  }

  watch(activeSpace, bindScrollEl)
  watch(expandedClasses, () => nextTick(updateScrollThumb), { deep: true })

  // L3-M0-8：页面关闭/隐藏时 flush 草稿 — 用 pagehide 而不是 beforeunload
  // 原因：beforeunload 在 iOS Safari、Chrome 移动端的后台切换场景可能被跳过；
  // pagehide 在所有现代浏览器都会触发，符合 W3C Page Visibility 最佳实践
  const _flushDraftOnUnload = () => _flushDraftForCurrentConv()

  onMounted(() => {
    mittBus.on('openChat', openChat)
    chatStore.initWebSocket()
    chatStore.refreshUnreadCount()
    bindScrollEl()
    window.addEventListener('pagehide', _flushDraftOnUnload)
    // L3-M0-8 + CHAT-325：立即加载 peer-keyed 草稿（新格式）到响应式 map
    // peer-keyed 不依赖 conversations（不需等 switchSpace('messages') 加载），
    // 能保证 "[草稿]" 前缀 + ghost 卡片在用户首次打开消息面板的瞬间即可见。
    // legacy 格式草稿由上方 watch 在 conversations 加载完成后迁移。
    _reloadPeerDraftMap()
  })
  onUnmounted(() => {
    mittBus.off('openChat', openChat)
    if (_csScrollEl) _csScrollEl.removeEventListener('scroll', onSpaceScroll)
    stopStatusPolling()
    // L3-M0-6：组件卸载前清理 typing 定时器并发最后一次 STOP
    _stopTyping()
    // L3-M0-8：组件卸载前 flush 草稿（比如路由跳转离开聊天页场景）
    _flushDraftForCurrentConv()
    window.removeEventListener('pagehide', _flushDraftOnUnload)
    chatStore.destroyWebSocket()
  })
</script>

<style lang="scss" scoped>
/* ====== 遮罩层 ====== */
.chat-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.08);
  backdrop-filter: blur(1px);
}
.chat-overlay-enter-active, .chat-overlay-leave-active { transition: opacity 0.3s ease; }
.chat-overlay-enter-from, .chat-overlay-leave-to { opacity: 0; }

/* ====== 悬浮聊天卡片 ====== */
.chat-panel {
  position: fixed; z-index: 2001;
  top: 20px; right: 20px; bottom: 20px;
  width: 380px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 8px 40px rgba(0,0,0,0.12),
    0 0 0 1px rgba(0,0,0,0.04);
}
.chat-panel-mobile {
  top: 8px; right: 8px; bottom: 8px; left: 8px;
  width: auto;
}
.chat-panel-enter-active { transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
.chat-panel-leave-active { transition: all 0.25s cubic-bezier(0.4, 0, 1, 1); }
.chat-panel-enter-from { opacity: 0; transform: translateX(40px) scale(0.95); }
.chat-panel-leave-to { opacity: 0; transform: translateX(30px) scale(0.97); }

.chat-root {
  display: flex; flex-direction: column; height: 100%;
  background: var(--el-bg-color);
  border-radius: 16px;
  overflow: hidden;
  position: relative;
}

/* ====== 顶部栏 ====== */
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid rgba(0,0,0,0.06);
  border-radius: 16px 16px 0 0;
}
.header-title {
  font-size: 16px; font-weight: 700;
  color: #1D1D1F;
  line-height: 1.3;
  letter-spacing: -0.02em;
}
.header-status-text {
  font-size: 11px; font-weight: 400;
}
.status-online { color: #22c55e; }
.status-offline { color: #9ca3af; }
.back-btn {
  width: 32px; height: 32px; border-radius: 8px;
  background: transparent;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
  color: var(--el-text-color-secondary);
  &:hover { background: var(--el-fill-color); color: var(--el-text-color-primary); }
}
.header-action-btn {
  width: 28px; height: 28px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
  color: #9CA3AF;
  &:hover { background: var(--el-fill-color); color: #1D1D1F; }
}
.header-spacer {
  width: 28px; height: 28px; flex-shrink: 0;
}
.chat-avatar-ring {
  border: 2px solid var(--el-border-color-extra-light);
}
.online-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34,197,94,0.5);
  animation: pulse-green 2s infinite;
}
.offline-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #9ca3af;
}
@keyframes pulse-green {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ====== 联系人面板 ====== */
.contacts-panel {
  padding: 10px 20px 8px; max-height: 300px; overflow-y: auto;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  background: var(--el-bg-color);
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: var(--el-border-color-light); border-radius: 2px; }
}
.sub-expand-enter-active, .sub-expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.sub-expand-enter-from, .sub-expand-leave-to {
  max-height: 0; opacity: 0; padding-bottom: 0;
  overflow: hidden;
}
.sub-expand-enter-to, .sub-expand-leave-from {
  max-height: 800px; opacity: 1;
}
.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.25s ease;
}
.slide-down-enter-from, .slide-down-leave-to {
  max-height: 0; opacity: 0; padding-top: 0; padding-bottom: 0; overflow: hidden;
}

/* ====== 聊天主体 ====== */
.chat-body {
  flex: 1; overflow-y: auto;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--el-border-color-light); border-radius: 2px; }
}
.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%;
}
.empty-icon-circle {
  width: 64px; height: 64px; border-radius: 50%;
  background: color-mix(in srgb, var(--theme-color), transparent 90%);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
}
.empty-title {
  font-size: 15px; font-weight: 600;
  color: #4B5563;
  margin-top: 4px;
}
.empty-subtitle {
  font-size: 13px; font-weight: 400;
  color: #9CA3AF;
  margin-top: 4px;
}

/* ====== 会话列表 ====== */
.conv-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.15s ease;
  border-bottom: 1px solid rgba(0,0,0,0.04);
  &:hover {
    background: rgba(0,0,0,0.02);
  }
  &:active {
    background: rgba(0,0,0,0.05);
  }
  &:last-child {
    border-bottom: none;
  }
}
.conv-avatar {
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.conv-name {
  font-size: 14px; font-weight: 600;
  color: #1D1D1F;
}
.conv-time {
  font-size: 12px; font-weight: 400;
  color: #9CA3AF;
}
.conv-preview {
  font-size: 13px; font-weight: 400;
  color: #6B7280;
  line-height: 1.4;
}
.conv-class-tag {
  font-size: 10px; font-weight: 500; line-height: 1;
  padding: 2px 6px; border-radius: 4px;
  background: color-mix(in srgb, #22c55e, transparent 88%);
  color: #16a34a; white-space: nowrap; flex-shrink: 0;
}
.unread-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 18px; height: 18px;
  border-radius: 9px; padding: 0 5px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: #fff; font-size: 10px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(239,68,68,0.4);
}
/* L3-M0-7：免打扰会话的未读角标置灰（对齐 WhatsApp "消息进入但不打扰"视觉） */
.unread-badge--muted {
  background: linear-gradient(135deg, #9ca3af, #6b7280) !important;
  box-shadow: 0 2px 6px rgba(107,114,128,0.3) !important;
}
.badge-pop-enter-active { animation: badge-pop-in 0.3s ease; }
@keyframes badge-pop-in {
  0% { transform: scale(0); }
  60% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

/* ====== Space 内容区 ====== */
.space-content-area {
  flex: 1; position: relative; overflow: hidden;
  display: flex; flex-direction: column;
}
.space-view {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar { display: none; }
}

/* ====== 自定义浮动滚动条（QQ 风格，零布局偏移） ====== */
.cs-track {
  position: absolute;
  right: 1px;
  top: 0;
  bottom: 0;
  width: 4px;
  z-index: 10;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.25s ease;
}
.cs-visible { opacity: 1; }
.cs-thumb {
  position: absolute;
  left: 0;
  width: 100%;
  min-height: 24px;
  background: rgba(0,0,0,0.15);
  border-radius: 2px;
  will-change: transform;
}
.cs-thumb:hover { background: rgba(0,0,0,0.3); }

/* ====== Space 切换过渡（Intercom crossfade + fade-up） ====== */
.space-fade-enter-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1),
              transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.space-fade-leave-active {
  transition: opacity 0.15s ease-out;
  position: absolute; inset: 0;
}
.space-fade-enter-from {
  opacity: 0; transform: translateY(8px);
}
.space-fade-leave-to {
  opacity: 0;
}

/* ====== 底部 Tab Bar（QQ 纯图标导航） ====== */
.tab-bar {
  display: flex; align-items: center;
  height: 52px; flex-shrink: 0;
  border-top: 1px solid rgba(0,0,0,0.06);
  background: var(--el-bg-color);
}
.tab-item {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  height: 100%;
  cursor: pointer;
  transition: color 0.2s ease;
  color: #8E8E93;
  -webkit-tap-highlight-color: transparent;
  &:hover { color: #636366; }
  &:active { opacity: 0.7; }
}
.tab-item-active {
  color: #1D1D1F;
  &:hover { color: #1D1D1F; }
}
.tab-icon-wrap {
  position: relative;
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px;
}
.tab-icon {
  font-size: 24px;
}
.tab-badge {
  position: absolute; top: -5px; right: -11px;
  min-width: 16px; height: 16px; border-radius: 8px;
  padding: 0 4px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: #fff; font-size: 9px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 1px 4px rgba(239,68,68,0.3);
}

/* Tab Bar 滑入滑出过渡 */
.tab-bar-slide-enter-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.tab-bar-slide-leave-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.6, 1);
}
.tab-bar-slide-enter-from,
.tab-bar-slide-leave-to {
  transform: translateY(100%);
}

/* ====== 主页 Space ====== */
.home-space {
  padding: 16px 20px;
}
.home-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px; margin-bottom: 10px;
  border-radius: 16px;
  background: rgba(0,0,0,0.04);
  border: none;
  cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: rgba(0,0,0,0.06); }
  &:active { background: rgba(0,0,0,0.08); }
}
.home-card-icon-wrap {
  width: 40px; height: 40px; border-radius: 10px; flex-shrink: 0;
  background: color-mix(in srgb, var(--theme-color), transparent 90%);
  display: flex; align-items: center; justify-content: center;
}
.home-card-content {
  flex: 1; min-width: 0;
}
.home-card-title {
  font-size: 14px; font-weight: 600;
  color: #1D1D1F;
}
.home-card-desc {
  font-size: 12px; margin-top: 2px;
  color: #8E8E93;
}
.home-card-icon {
  font-size: 20px;
  color: var(--theme-color);
}
.home-card-arrow {
  font-size: 16px; color: #C7C7CC; flex-shrink: 0;
}
.home-recent-card {
  margin-bottom: 10px;
  border-radius: 16px;
  background: rgba(0,0,0,0.04);
  overflow: hidden;
}
.home-section-title {
  font-size: 14px; font-weight: 700;
  color: #1D1D1F;
  padding: 16px 16px 4px;
}
.home-conv-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: rgba(0,0,0,0.03); }
  &:active { background: rgba(0,0,0,0.06); }
}

/* Stagger 入场动画 */
@keyframes stagger-fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.stagger-item {
  animation: stagger-fade-up 0.3s cubic-bezier(0.4, 0, 0.2, 1) both;
}

/* ====== 骨架屏 ====== */
.skeleton-list {
  padding: 8px 0;
}
.skeleton-row {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px;
}
.skeleton-avatar {
  width: 40px; height: 40px; border-radius: 50%; flex-shrink: 0;
  background: rgba(0,0,0,0.08);
  animation: shimmer 1.5s ease-in-out infinite;
  background-image: linear-gradient(90deg, transparent 25%, rgba(0,0,0,0.04) 50%, transparent 75%);
  background-size: 200% 100%;
}
.skeleton-avatar-sq {
  border-radius: 10px;
}
.skeleton-text {
  flex: 1; display: flex; flex-direction: column; gap: 8px;
}
.skeleton-line {
  height: 10px; border-radius: 5px;
  background: rgba(0,0,0,0.08);
  animation: shimmer 1.5s ease-in-out infinite;
  background-image: linear-gradient(90deg, transparent 25%, rgba(0,0,0,0.04) 50%, transparent 75%);
  background-size: 200% 100%;
}
.skeleton-line-name { width: 45%; }
.skeleton-line-msg { width: 75%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ====== 联系人 Space ====== */
.ct-space {
  padding: 8px 16px 20px;
  display: flex; flex-direction: column; gap: 8px;
  & > * { flex-shrink: 0; }
}
/* 联系人骨架屏 */
.ct-search--disabled {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(0,0,0,0.04);
  pointer-events: none;
}
.ct-search-placeholder {
  font-size: 13px; color: #8E8E93;
}
.ct-skel-bar {
  height: 36px; border-radius: 7px; width: 100%;
}
.ct-skel-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(0,0,0,0.04);
}
.ct-skel-avatar {
  width: 40px; height: 40px; border-radius: 50%; flex-shrink: 0;
}
.ct-search {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(0,0,0,0.04);
  border: none;
  transition: background 0.2s;
  &:focus-within { background: rgba(0,0,0,0.06); }
}
.ct-search-icon { font-size: 15px; color: #8E8E93; flex-shrink: 0; }
.ct-search-input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 13px; color: #1D1D1F;
  &::placeholder { color: #8E8E93; }
}
/* 分组筛选 Segmented Control（QQ 滑块式） */
.ct-filter-bar {
  position: relative;
  display: flex;
  padding: 2px;
  background: rgba(0,0,0,0.06);
  border-radius: 7px;
  flex-shrink: 0;
}
.ct-filter-slider {
  position: absolute;
  top: 4px; bottom: 4px; left: 4px;
  border-radius: 7px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.12), 0 0.5px 1px rgba(0,0,0,0.08);
  transition: transform 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  pointer-events: none;
  z-index: 0;
}
.ct-filter-tab {
  position: relative; z-index: 1;
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  padding: 7px 0;
  border-radius: 7px;
  border: none; outline: none;
  background: transparent;
  color: #3C3C43;
  font-size: 13px; font-weight: 500;
  cursor: pointer;
  transition: color 0.15s ease, opacity 0.15s ease;
  -webkit-tap-highlight-color: transparent;
  &:hover { color: #8E8E93; }
  &:active { color: #AEAEB2; }
}
.ct-filter-tab--active {
  color: #1D1D1F;
  font-weight: 600;
  &:hover { color: #1D1D1F; }
  &:active { color: #1D1D1F; }
}
/* 角色分组标题（仅"全部"筛选下显示） */
.ct-group-title {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 2px;
}
.ct-group-dot {
  width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0;
}
.ct-group-name {
  font-size: 13px; font-weight: 700; color: #1D1D1F;
}
.ct-group-count {
  font-size: 11px; font-weight: 600; color: #8E8E93;
  background: rgba(0,0,0,0.06);
  padding: 1px 7px; border-radius: 10px;
}
/* 班级独立卡片 */
.ct-class-card {
  border-radius: 14px;
  background: rgba(0,0,0,0.04);
  overflow: hidden;
}
.ct-class-header {
  display: flex; align-items: center; gap: 6px;
  padding: 12px 14px;
  cursor: pointer;
  transition: background 0.15s;
  &:hover { background: rgba(0,0,0,0.02); }
}
.ct-class-arrow {
  font-size: 16px; color: #8E8E93; flex-shrink: 0;
}
.ct-class-icon {
  font-size: 15px; color: #8E8E93; flex-shrink: 0;
}
.ct-class-name {
  font-size: 13px; font-weight: 600; color: #4B5563;
  flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.ct-class-count {
  font-size: 12px; font-weight: 500; color: #8E8E93; flex-shrink: 0;
}
.ct-class-list {
  border-top: 1px solid rgba(0,0,0,0.04);
}
/* Persona 双行卡片（管理员/教师） */
.ct-persona-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(0,0,0,0.04);
  cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: rgba(0,0,0,0.06); }
  &:active { background: rgba(0,0,0,0.08); }
}
.ct-persona-avatar {
  box-shadow: 0 1px 4px rgba(0,0,0,0.08); flex-shrink: 0;
}
.ct-persona-info {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column; gap: 2px;
}
.ct-persona-name {
  font-size: 14px; font-weight: 600; color: #1D1D1F;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.ct-persona-meta {
  display: flex; align-items: center; gap: 4px;
}
.ct-persona-dot {
  width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;
}
.ct-persona-role {
  font-size: 11px; font-weight: 500; color: #8E8E93;
}
.ct-persona-sep {
  font-size: 11px; color: #C7C7CC;
}
.ct-persona-online {
  font-size: 11px; font-weight: 500; color: #22c55e;
}
.ct-persona-offline {
  font-size: 11px; font-weight: 500; color: #9ca3af;
}
.ct-persona-action {
  font-size: 16px; color: #C7C7CC; flex-shrink: 0;
  transition: color 0.15s;
}
.ct-persona-card:hover .ct-persona-action { color: #8E8E93; }
/* 学生联系人行 */
.ct-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: rgba(0,0,0,0.03); }
  &:active { background: rgba(0,0,0,0.06); }
}
.ct-row-avatar {
  box-shadow: 0 1px 3px rgba(0,0,0,0.06); flex-shrink: 0;
}
.ct-row-info {
  flex: 1; min-width: 0;
  display: flex; align-items: center; gap: 6px;
}
.ct-row-name {
  font-size: 14px; font-weight: 600; color: #1D1D1F;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.ct-row-online {
  font-size: 10px; font-weight: 500; color: #22c55e;
  background: rgba(34,197,94,0.1);
  padding: 1px 6px; border-radius: 8px;
  flex-shrink: 0;
}
.ct-row-offline {
  font-size: 10px; font-weight: 500; color: #9ca3af;
  background: rgba(156,163,175,0.1);
  padding: 1px 6px; border-radius: 8px;
  flex-shrink: 0;
}
.ct-row-action {
  font-size: 16px; color: #C7C7CC; flex-shrink: 0;
  transition: color 0.15s;
}
.ct-row:hover .ct-row-action { color: #8E8E93; }

/* ====== 消息列表 ====== */
.msg-list {
  padding: 8px 16px 72px;
  display: flex; flex-direction: column; gap: 8px;
}
.msg-conv-card {
  position: relative;
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(0,0,0,0.04);
  cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: rgba(0,0,0,0.06); }
  &:active { background: rgba(0,0,0,0.08); }
}
/* L3-M0-7：置顶卡片淡色高亮（对齐 Telegram Pin 视觉） */
.msg-conv-card--pinned {
  background: color-mix(in srgb, var(--theme-color, #409eff), transparent 94%);
  &:hover { background: color-mix(in srgb, var(--theme-color, #409eff), transparent 90%); }
  &:active { background: color-mix(in srgb, var(--theme-color, #409eff), transparent 86%); }
}
/* L3-M0-7：右上角 pin 角标 —— Telegram 风格小图标 */
.conv-pin-badge {
  position: absolute;
  top: 6px;
  right: 8px;
  width: 14px; height: 14px;
  display: flex; align-items: center; justify-content: center;
  color: var(--theme-color, #409eff);
  font-size: 12px;
  opacity: 0.75;
  pointer-events: none;
}
/* L3-M0-7：免打扰图标（会话卡片时间位左侧） */
.conv-mute-icon {
  font-size: 14px;
  color: #9ca3af;
  opacity: 0.85;
}
/* L3-M0-8：草稿红色前缀（对齐微信会话列表"[草稿]"设计） */
.conv-draft-prefix {
  color: #ef4444;
  font-weight: 500;
  margin-right: 2px;
}

/* ====== L3-M1-4：归档卡片（Telegram Archive 模式） ====== */
.archive-card {
  border-radius: 14px;
  background: rgba(0,0,0,0.03);
  overflow: hidden;
  margin-bottom: 4px;
}
.archive-card-header {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
  &:hover { background: rgba(0,0,0,0.05); }
  &:active { background: rgba(0,0,0,0.07); }
}
.archive-icon-wrap {
  width: 38px; height: 38px; border-radius: 10px;
  background: rgba(0,0,0,0.06);
  display: flex; align-items: center; justify-content: center;
  color: #86868B;
  flex-shrink: 0;
}
.archive-icon { font-size: 18px; }
.archive-title {
  font-size: 14px; font-weight: 600; color: #1D1D1F;
}
.archive-subtitle {
  font-size: 12px; color: #86868B; margin-top: 2px;
}
.archive-count {
  font-size: 12px; font-weight: 600; color: #86868B;
  background: rgba(0,0,0,0.06);
  padding: 2px 8px; border-radius: 10px;
  margin-left: 8px;
  flex-shrink: 0;
}
.archive-chevron {
  color: #C7C7CC; font-size: 18px;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}
.archive-list {
  display: flex; flex-direction: column; gap: 4px;
  padding: 4px 8px 8px 8px;
}
.msg-conv-card--archived {
  /* 归档子项略透明并加上左边淡色分隔线（层级暗示） */
  opacity: 0.92;
  padding-left: 20px;
  position: relative;
  &::before {
    content: ''; position: absolute;
    left: 8px; top: 14px; bottom: 14px;
    width: 2px; border-radius: 1px;
    background: rgba(0,0,0,0.08);
  }
}
/* 归档展开/折叠动画 */
.archive-expand-enter-active, .archive-expand-leave-active {
  transition: max-height 0.25s ease, opacity 0.2s ease;
  overflow: hidden;
}
.archive-expand-enter-from, .archive-expand-leave-to {
  max-height: 0; opacity: 0;
}
.archive-expand-enter-to, .archive-expand-leave-from {
  max-height: 1000px; opacity: 1;
}

/* ====== FAB 右下角固定按钮 ====== */
.fab-btn-fixed {
  position: absolute; bottom: 16px; right: 16px;
  width: 48px; height: 48px; border-radius: 50%;
  background: #1D1D1F;
  color: #fff; border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 14px rgba(0,0,0,0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  z-index: 5;
  &:hover { transform: scale(1.06); box-shadow: 0 6px 18px rgba(0,0,0,0.3); }
  &:active { transform: scale(0.95); }
}
.fab-btn-icon {
  font-size: 20px;
}

/* ====== 视图滑动过渡（Fin 官方风格：卡片堆叠） ====== */
.chat-views-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}
.chat-view-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  z-index: 1;
  background: var(--el-bg-color);
}
.slide-chat-enter-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-chat-leave-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.6, 1);
}
.slide-chat-enter-from,
.slide-chat-leave-to {
  transform: translateX(100%);
}

/* ====== 消息预加载指示器（Fin 官方风格：居中圆环 spinner） ====== */
.msg-preloader {
  position: absolute;
  inset: 0;
  display: flex; align-items: center; justify-content: center;
  animation: preloader-fade-in 0.2s ease;
  z-index: 1;
}
.msg-preloader-spinner {
  width: 24px; height: 24px;
  border: 2px solid rgba(0,0,0,0.08);
  border-top-color: rgba(0,0,0,0.35);
  border-radius: 50%;
  animation: preloader-spin 0.75s linear infinite;
}
@keyframes preloader-spin {
  to { transform: rotate(360deg); }
}
@keyframes preloader-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ====== 消息区域 ====== */
.chat-messages-area {
  position: relative;
  padding: 16px;
  background: #fff;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: #D4D4D4 transparent;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #D4D4D4; border-radius: 4px; }
  &::-webkit-scrollbar-thumb:hover { background: #999; }
}
.time-divider {
  display: flex; align-items: center; gap: 12px;
  margin: 16px 0 12px;
  &::before, &::after {
    content: ''; flex: 1; height: 1px;
    background: #E5E7EB;
  }
  span {
    font-size: 12px; color: #6C6F74;
    white-space: nowrap; flex-shrink: 0;
  }
}
.msg-row {
  display: flex; align-items: flex-start;
  margin-bottom: 12px;
  &.msg-row-me { justify-content: flex-end; }
  &.msg-row-other { justify-content: flex-start; }
  &.msg-row-continuous { margin-bottom: 3px; }
}
.msg-bubble-wrap {
  display: flex; flex-direction: column; max-width: 70%;
}
.msg-bubble {
  padding: 12px 16px;
  font-size: 14px; line-height: 1.5;
  word-break: break-word;
  animation: msg-appear 0.2s ease;
}
@keyframes msg-appear {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
.msg-bubble-me {
  background: var(--theme-color);
  color: #fff;
  border-radius: 20px 20px 4px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.msg-bubble-other {
  background: #F5F5F5;
  color: #1A1A1A;
  border-radius: 20px 20px 20px 4px;
}

/* ====== L3-M0-6：Typing Indicator（对方正在输入...）====== */
/* 业界参考：WhatsApp/Telegram/iMessage/Signal 的经典三点跳动动画 */
.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.typing-dot {
  width: 6px;
  height: 6px;
  background: currentColor;
  border-radius: 50%;
  opacity: 0.4;
  animation: typing-bounce 1.4s infinite ease-in-out;
}
.typing-dot:nth-child(1) { animation-delay: 0s; }
.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* Header 位置的 typing 提示（替代在线/离线状态） */
.header-typing .typing-dots {
  color: var(--el-color-primary, #409eff);
}
.header-typing .typing-dot {
  width: 5px;
  height: 5px;
}
.typing-text {
  color: var(--el-color-primary, #409eff) !important;
  font-style: italic;
  font-weight: 500;
}

/* 消息气泡版本（左侧 WhatsApp 风格跳动气泡） */
.typing-bubble {
  padding: 10px 16px !important;
  min-width: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.typing-bubble .typing-dots {
  color: #6B7280;
}
.typing-bubble .typing-dot {
  width: 7px;
  height: 7px;
}
.typing-bubble-row {
  margin-top: 4px;
}

/* 连续消息圆角调整 */
.msg-continuous-me {
  border-radius: 20px 4px 4px 20px;
}
.msg-continuous-other {
  border-radius: 4px 20px 20px 4px;
}

/* ====== 图片消息（Fin/Intercom 灯箱预览风格） ====== */
.msg-image-wrap {
  position: relative; cursor: zoom-in; display: inline-block;
  border-radius: 12px; overflow: hidden;
}
.msg-image {
  max-width: 240px; max-height: 240px;
  border-radius: 12px; display: block;
  object-fit: cover;
  background: linear-gradient(90deg, #E5E7EB 25%, #F3F4F6 50%, #E5E7EB 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease infinite;
  transition: opacity 0.3s, transform 0.2s ease;
  opacity: 0;
  &.loaded { animation: none; background: transparent; opacity: 1; }
  &.error {
    animation: none; background: #F3F4F6;
    min-width: 120px; min-height: 80px; opacity: 1;
  }
  .msg-image-wrap:hover & { transform: scale(1.03); }
}
.msg-image-hover {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.25);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.2s;
  border-radius: 12px;
  .msg-image-wrap:hover & { opacity: 1; }
}
.msg-image-zoom-icon { font-size: 24px; color: #fff; }
.msg-bubble:has(.msg-image-wrap) {
  padding: 4px; background: transparent !important;
  border: none !important; box-shadow: none !important;
}

/* ====== 多图 grid 布局（Intercom 紧凑网格） ====== */
.msg-image-grid {
  display: grid; gap: 3px; border-radius: 12px; overflow: hidden;
  max-width: 260px;
}
.msg-image-grid-2 { grid-template-columns: 1fr 1fr; }
.msg-image-grid-3 { grid-template-columns: 1fr 1fr; }
.msg-image-grid-3 .msg-grid-item:first-child { grid-column: 1 / -1; }
.msg-image-grid-4 { grid-template-columns: 1fr 1fr; }
.msg-grid-item {
  border-radius: 0; overflow: hidden;
  aspect-ratio: 1; cursor: zoom-in;
}
.msg-grid-item .msg-image-hover { border-radius: 0; }
.msg-grid-image {
  width: 100%; height: 100%; object-fit: cover; display: block;
  background: linear-gradient(90deg, #E5E7EB 25%, #F3F4F6 50%, #E5E7EB 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease infinite;
  &.loaded { animation: none; background: transparent; }
  &.error { animation: none; background: #F3F4F6; }
}
.msg-bubble:has(.msg-image-grid) {
  padding: 3px; background: transparent !important;
  border: none !important; box-shadow: none !important;
}

/* ====== 置底按钮（Intercom 风格） ====== */
.scroll-btn-anchor {
  position: relative;
  height: 0;
  z-index: 5;
  flex-shrink: 0;
}
.scroll-to-bottom-btn {
  position: absolute;
  left: 50%; bottom: 6px;
  transform: translateX(-50%);
  width: 34px; height: 34px;
  border-radius: 50%;
  border: none;
  background: #fff;
  color: #111;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  padding: 0;
  box-shadow: 0 1px 6px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.05);
  transition: all 0.2s ease;
  &:hover {
    box-shadow: 0 2px 10px rgba(0,0,0,0.18);
    transform: translateX(-50%) scale(1.06);
  }
  &:active { transform: translateX(-50%) scale(0.95); }
}
.scroll-btn-fade-enter-active, .scroll-btn-fade-leave-active {
  transition: all 0.25s ease;
}
.scroll-btn-fade-enter-from, .scroll-btn-fade-leave-to {
  opacity: 0; transform: translateX(-50%) translateY(8px) scale(0.9);
}

/* ====== 输入区域 ====== */
.chat-input-area {
  padding: 12px 16px 16px;
  background: var(--el-bg-color);
  border-radius: 0 0 16px 16px;
  position: relative;
}
.input-card {
  position: relative;
  border: 0.5px solid #E5E7EB;
  border-radius: 16px;
  padding: 10px 14px;
  background: var(--el-bg-color);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.input-card-focused {
  border-color: #000;
  box-shadow: 0 0 0 1px #000;
}
.input-card-bottom {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 6px;
}
.input-toolbar {
  display: flex; align-items: center; gap: 14px;
}
.toolbar-icon {
  font-size: 18px; line-height: 1;
  color: #4B5563;
  cursor: pointer;
  background: none; border: none; outline: none;
  padding: 0; border-radius: 0;
  -webkit-tap-highlight-color: transparent;
  &:hover { color: #1F2937; }
  &:active, &:focus { outline: none; }
}
.toolbar-icon-active {
  color: #3B82F6 !important;
  &:hover { color: #2563EB !important; }
}
.chat-input-editable {
  flex: 1;
  outline: none;
  border: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.45;
  min-height: 22px;
  max-height: 88px;
  overflow-y: auto;
  word-break: break-word;
  white-space: pre-wrap;
  color: var(--el-text-color-primary);
  caret-color: #000;
  -webkit-user-modify: read-write-plaintext-only;
  scrollbar-width: thin;
  scrollbar-color: #C4C4C4 transparent;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #C4C4C4; border-radius: 4px; }
  &::-webkit-scrollbar-thumb:hover { background: #999; }
  &:empty::before {
    content: attr(data-placeholder);
    color: #9CA3AF;
    font-size: 15px;
    pointer-events: none;
  }
}
.send-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: #E5E7EB;
  color: #9CA3AF;
  display: flex; align-items: center; justify-content: center;
  cursor: not-allowed; transition: all 0.2s;
  flex-shrink: 0;
  &.send-btn-active {
    background: #000;
    color: #fff; cursor: pointer;
    &:hover { background: #1a1a1a; }
    &:active { transform: scale(0.92); }
  }
}

/* ====== 语音录制（Fin 风格） ====== */
.input-card-voice {
  padding: 10px 12px;
}
.voice-recording-bar {
  display: flex; align-items: center; gap: 12px; min-height: 40px;
}
.voice-cancel-btn {
  width: 38px; height: 38px; border-radius: 50%; border: none;
  background: #F3F4F6; color: #6B7280;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0; transition: all 0.2s;
  &:hover { background: #E5E7EB; color: #374151; }
  &:active { transform: scale(0.92); }
}
.voice-center {
  flex: 1; display: flex; align-items: center; gap: 8px;
  min-width: 0;
}
.recording-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #EF4444; flex-shrink: 0;
  animation: pulse-dot 1.2s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}
.recording-duration {
  font-size: 13px; font-weight: 600; color: #374151;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0; min-width: 32px;
}
.voice-stop-btn {
  width: 38px; height: 38px; border-radius: 50%; border: none;
  background: #1F2937; color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0; transition: all 0.2s;
  &:hover { background: #111827; transform: scale(1.05); }
  &:active { transform: scale(0.92); }
}
.voice-waveform {
  flex: 1; display: flex; align-items: center; justify-content: center;
  gap: 2.5px; height: 32px; overflow: hidden;
}
.waveform-bar {
  width: 2.5px; min-height: 2px; max-height: 28px;
  background: #374151; border-radius: 2px;
  transition: height 0.08s ease-out;
}
.voice-interim-preview {
  font-size: 12px; color: #6B7280; font-style: italic;
  padding: 2px 16px 0; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  animation: fadeInText 0.3s ease;
}
.voice-interim-hint { color: #9CA3AF; }
@keyframes fadeInText {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.voice-transcribing {
  flex: 1; display: flex; align-items: center; justify-content: center;
  gap: 10px;
}
.transcribing-spinner {
  width: 18px; height: 18px; border-radius: 50%;
  border: 2.5px solid #E5E7EB; border-top-color: #374151;
  animation: spin 0.75s linear infinite;
}
.transcribing-text {
  font-size: 14px; color: #6B7280; font-weight: 500;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ====== 表情选择器 ====== */
.emoji-picker {
  position: absolute; bottom: calc(100% + 4px); left: 20px; right: 20px;
  background: var(--el-bg-color);
  border: 1px solid #E5E7EB;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.10), 0 1px 4px rgba(0,0,0,0.06);
  z-index: 10; overflow: hidden;
  display: flex; flex-direction: column;
}
.emoji-search-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px 8px;
  border-bottom: 1px solid #F0F0F0;
}
.emoji-search-icon {
  font-size: 18px; color: #4B5563; flex-shrink: 0;
  font-weight: bold;
}
.emoji-search-input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 16px; color: var(--el-text-color-primary);
  &::placeholder { color: #6B7280; font-size: 14px; }
}
.emoji-scroll-area {
  max-height: 220px; overflow-y: auto;
  padding: 2px 14px 10px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #D1D5DB; border-radius: 4px; }
  &::-webkit-scrollbar-thumb:hover { background: #9CA3AF; }
}
.emoji-category-title {
  font-size: 13px; font-weight: 700; color: #374151;
  padding: 10px 0 6px; line-height: 1;
}
.emoji-grid {
  display: grid; grid-template-columns: repeat(5, 1fr);
  gap: 2px;
}
.emoji-item {
  font-size: 26px; padding: 4px 0;
  border-radius: 10px; cursor: pointer; transition: transform 0.12s;
  line-height: 1.2; width: fit-content;
  &:hover { transform: scale(1.2); }
  &:active { transform: scale(0.95); }
}
.emoji-empty {
  display: flex; align-items: center; justify-content: center;
  padding: 32px 0;
}
.emoji-panel-enter-active, .emoji-panel-leave-active {
  transition: all 0.2s ease;
}
.emoji-panel-enter-from, .emoji-panel-leave-to {
  opacity: 0; transform: translateY(8px);
}

/* ====== 文件预览（Fin/Intercom 源码 1:1 精确复刻） ====== */
.file-preview-scroll {
  display: flex; gap: 6px; padding: 6px 8px 12px 0;
  overflow-x: auto; overflow-y: hidden;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

/* — 非图片文件卡片：横向布局 — */
.file-preview-card {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  width: 200px;
  padding: 8px 12px 8px 8px;
  background: #F3F4F6;
  border-radius: 12px;
  border: none;
  flex-shrink: 0;
  transition: background 0.18s, box-shadow 0.18s;
  &:hover { background: #EAECEF; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  &:hover .file-card-remove { opacity: 1; }
}

/* — 图片文件：独立正方形纯图卡片（Fin精确尺寸） — */
.file-preview-card-image {
  position: relative;
  width: 60px; height: 60px;
  border-radius: 12px;
  flex-shrink: 0;
  background: #F3F4F6;
  box-shadow: 0 0 0 0 rgba(255,255,255,0.1);
  transition: box-shadow 0.18s;
  &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.10); }
  &:hover .file-card-remove { opacity: 1; }
}
.image-card-thumb {
  width: 100%; height: 100%;
  object-fit: cover; display: block;
  border-radius: 12px;
  cursor: zoom-in;
  transition: filter 0.2s;
  &:hover { filter: brightness(0.92); }
}
.image-card-skeleton {
  width: 100%; height: 100%; border-radius: 12px;
  background: linear-gradient(90deg, #E5E7EB 25%, #F3F4F6 50%, #E5E7EB 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease infinite;
}
.image-card-loading {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  border-radius: 12px;
}
.file-preview-card-image .file-card-remove {
  top: -6px; right: -6px;
}
.file-preview-card-image .file-card-error-badge {
  bottom: -4px; right: -4px;
}

/* — 文件卡片图标（44px Fin尺寸） — */
.file-card-thumb {
  width: 44px; height: 44px; border-radius: 10px;
  object-fit: cover; flex-shrink: 0;
}
.file-card-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.file-icon-aa {
  font-size: 18px; font-weight: 800; color: #fff;
  font-family: Georgia, 'Times New Roman', serif;
  letter-spacing: -0.5px;
}
.file-icon-svg {
  font-size: 22px; color: #fff;
}
.file-card-info {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column; gap: 1px;
}
.file-card-name {
  font-size: 13px; font-weight: 500; color: #1F2937;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  line-height: 1.3;
}
.file-card-ext {
  font-size: 11px; color: #6B7280;
  font-weight: 500; line-height: 1.2;
}

/* — × 关闭按钮（Fin精确：18px 深色圆点 紧贴角落） — */
.file-card-remove {
  position: absolute; top: -6px; right: -6px;
  width: 20px; height: 20px; border-radius: 50%;
  background: #333; color: #fff;
  border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; font-size: 14px; line-height: 1;
  z-index: 2;
  box-shadow: 0 1px 4px rgba(0,0,0,0.3);
  transition: opacity 0.15s, background 0.15s, transform 0.15s;
  &:hover { background: #000; transform: scale(1.12); }
  &:active { transform: scale(0.9); }
}

/* — 文件卡片图标容器 — */
.file-card-visual {
  position: relative; flex-shrink: 0;
  width: 44px; height: 44px;
}
.file-card-visual .file-card-thumb,
.file-card-visual .file-card-icon,
.file-card-visual .file-card-skeleton {
  width: 44px; height: 44px; border-radius: 10px;
}

/* 图片骨架占位 */
.file-card-skeleton {
  background: linear-gradient(90deg, #E5E7EB 25%, #F3F4F6 50%, #E5E7EB 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease infinite;
}
@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 上传中 spinner 覆盖层 */
.file-card-loading {
  position: absolute; inset: 0; border-radius: 10px;
  background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
}
.file-card-spinner {
  width: 18px; height: 18px; border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 上传失败错误标记 + 重试 */
.file-card-error-badge {
  position: absolute; bottom: -4px; right: -4px;
  width: 20px; height: 20px; border-radius: 50%;
  background: #EF4444; color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; font-size: 12px;
  box-shadow: 0 1px 4px rgba(239,68,68,0.4);
  transition: transform 0.15s, background 0.15s;
  z-index: 2;
  &:hover { background: #DC2626; transform: scale(1.15); }
}
.file-error-retry-icon { font-size: 12px; }
.file-card-ext-error { color: #EF4444 !important; }

/* 拖拽覆盖层 */
.drag-overlay {
  position: absolute; inset: 0; z-index: 10;
  border-radius: 20px;
  background: rgba(59,130,246,0.06);
  border: 2px dashed #3B82F6;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; pointer-events: none;
}
.drag-overlay-icon { font-size: 28px; color: #3B82F6; }
.drag-overlay-text { font-size: 13px; color: #3B82F6; font-weight: 500; }
.input-card-dragging {
  border-color: #3B82F6 !important;
  box-shadow: 0 0 0 2px rgba(59,130,246,0.15) !important;
}
/* 卡片入场/离场动画 (TransitionGroup) */
.file-card-enter-active { transition: all 0.25s ease; }
.file-card-leave-active { transition: all 0.2s ease; }
.file-card-enter-from { opacity: 0; transform: scale(0.85) translateY(6px); }
.file-card-leave-to { opacity: 0; transform: scale(0.85); }
.file-card-move { transition: transform 0.25s ease; }

/* fade 过渡 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ====== 聊天中文件消息卡片（L3-M0-BUGFIX-V3：业界权威三层硬约束） ======
   对齐全球顶级 IM 产品的共同做法（Slack/Discord/WhatsApp/Telegram/Teams 全部是卡片自身 max-width）：
   
   【为什么不依赖 :has()】
   - :has() 在 Vue scoped 编译后有歧义，特定浏览器版本可能降级
   - flex min-content 陷阱：wrapper 作为 flex 子项其 min-width:auto 会反吃 max-width
   
   【三层硬约束防护】
   L1 - 卡片自身 max-width: 300px  ← 核心硬约束（任何浏览器兼容）
   L2 - wrapper :has 同步收缩      ← 现代浏览器补充，视觉更紧凑
   L3 - info 区 flex 压缩链         ← 文件名 ellipsis 生效保障
*/

/* L2：wrapper 级约束（现代浏览器 :has 支持下生效） */
.msg-bubble-wrap:has(.msg-file-card) {
  max-width: min(300px, 85%);
  min-width: 0; /* 突破 flex min-width: auto 陷阱，防止内容反吃 max-width */
}
/* 气泡透明 —— 把视觉让给文件卡片（WhatsApp/Telegram 一致做法） */
.msg-bubble:has(.msg-file-card) {
  padding: 0; background: transparent !important;
  box-shadow: none !important; border: none !important;
  max-width: 300px; /* 补充硬约束，双保险 */
}

.msg-file-card {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px;
  text-decoration: none; color: inherit;
  /* ★ L1：卡片自身硬约束 — 业界所有顶级产品的共同做法 ★
     max-width: 300px 不依赖任何父级传递、任何选择器特性，任何浏览器都生效
     width: 100% 让卡片在小容器下按父级收缩（移动端兼容）*/
  width: 100%;
  max-width: 300px;
  min-width: 0; /* 配合 flex 父级，防止 min-content 陷阱 */
  box-sizing: border-box;
  /* 圆角 14px 与气泡圆角 20px 形成 6px 层级差 */
  border-radius: 14px;
  border: 1px solid rgba(0,0,0,0.08);
  background: #FAFAFA;
  transition: background 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  cursor: pointer;
  &:hover {
    background: #F3F4F6;
    border-color: rgba(0,0,0,0.12);
    box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  }
}
/* L3-M0-BUGFIX-V2：文件图标升级到 44px（业界主流尺寸，视觉重量足够） */
.msg-file-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
/* L3-M0-BUGFIX-V2：消息气泡内文件图标 SVG / Aa 尺寸微调匹配 44px 容器（不影响预览区 40px 容器）*/
.msg-file-icon .file-icon-svg { font-size: 24px; }
.msg-file-icon .file-icon-aa { font-size: 20px; }
.msg-file-info {
  /* 经典 flexbox 压缩链 — flex:1 + min-width:0 突破 flex min-width:auto 陷阱 */
  flex: 1 1 auto;
  min-width: 0;
  display: flex; flex-direction: column;
  gap: 3px;
  overflow: hidden;
}
/* middle-ellipsis：baseName 可压缩 ellipsis，扩展名永不压缩 */
.msg-file-name {
  display: flex; align-items: baseline;
  min-width: 0; max-width: 100%;
  font-size: 14px; font-weight: 500; color: #1A1A1A;
  line-height: 1.35;
}
.msg-file-name-base {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  min-width: 0;
}
.msg-file-name-ext {
  flex-shrink: 0;
  color: inherit;
}
/* L3-M0-BUGFIX-V2：扩展名副标题加 letter-spacing + uppercase，大小写字母宽度感更专业 */
.msg-file-ext {
  font-size: 12px; color: #6B7280;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-transform: uppercase;
}
/* L3-M0-BUGFIX-V2：下载按钮升级为 32px 圆形背景（对齐 WhatsApp/Discord），
   悬停时主题色填充，提供清晰的交互反馈 */
.msg-file-download {
  flex-shrink: 0;
  width: 32px; height: 32px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.04);
  color: #6B7280;
  font-size: 16px;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}
.msg-file-card:hover .msg-file-download {
  background: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary, #409eff);
  transform: translateY(-1px);
}

/* ====== 图片灯箱预览（Fin/Intercom 风格） ====== */
.lightbox-overlay {
  position: fixed; inset: 0; z-index: 99999;
  background: rgba(0, 0, 0, 0.85);
  display: flex; align-items: center; justify-content: center;
  cursor: zoom-out;
}
.lightbox-image {
  max-width: 90vw; max-height: 85vh;
  object-fit: contain; border-radius: 8px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.4);
  cursor: default;
  user-select: none;
}
.lightbox-close {
  position: absolute; top: 16px; right: 16px;
  width: 40px; height: 40px; border-radius: 50%;
  background: rgba(255,255,255,0.15); color: #fff;
  border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; backdrop-filter: blur(4px);
  transition: background 0.2s, transform 0.15s;
  &:hover { background: rgba(255,255,255,0.25); transform: scale(1.08); }
}
.lightbox-download {
  position: absolute; bottom: 20px; right: 20px;
  width: 44px; height: 44px; border-radius: 50%;
  background: rgba(255,255,255,0.15); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; backdrop-filter: blur(4px);
  text-decoration: none; cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  &:hover { background: rgba(255,255,255,0.3); transform: scale(1.08); }
}

/* 灯箱过渡动画 */
.lightbox-enter-active { transition: opacity 0.25s ease; }
.lightbox-leave-active { transition: opacity 0.2s ease; }
.lightbox-enter-from, .lightbox-leave-to { opacity: 0; }
.lightbox-enter-active .lightbox-image {
  transition: transform 0.25s ease;
}
.lightbox-enter-from .lightbox-image {
  transform: scale(0.9);
}
</style>

<!-- 深色模式适配（Fin DevTools 精确值：border rgb(56,55,69) / bg rgb(23,22,26)） -->
<style lang="scss">
html.dark {
  /* ====== 头部边框暗色适配 ====== */
  .chat-header {
    border-bottom-color: rgba(255,255,255,0.08);
  }

  /* ====== 会话列表行暗色适配 ====== */
  .conv-item {
    border-bottom-color: rgba(255,255,255,0.06);
    &:hover {
      background: rgba(255,255,255,0.04);
    }
    &:active {
      background: rgba(255,255,255,0.07);
    }
  }

  /* ====== 联系人面板暗色适配 ====== */
  .contacts-panel {
    border-bottom-color: rgba(255,255,255,0.08);
  }
  .contact-chip {
    background: rgba(255,255,255,0.06);
    &:hover { background: rgba(255,255,255,0.10); }
    &:active { background: rgba(255,255,255,0.14); }
  }

  /* 输入卡片：边框+背景 */
  .input-card {
    border-color: rgba(255,255,255,0.1);
    border-width: 1px;
  }
  .input-card-focused {
    border-color: #fff;
    box-shadow: 0 0 0 0.5px #fff;
  }

  /* 非图片文件卡片 */
  .file-preview-card {
    background: #2A2A2E;
    border: 1px solid rgba(255,255,255,0.06);
    &:hover { background: #333338; box-shadow: 0 2px 8px rgba(0,0,0,0.35); }
  }

  /* 图片卡片 */
  .file-preview-card-image {
    background: #2A2A2E;
    border: 1px solid rgba(255,255,255,0.06);
    &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.35); }
  }

  /* 文件卡片文字 */
  .file-card-name {
    color: rgba(255,255,255,0.85);
  }
  .file-card-ext {
    color: rgba(255,255,255,0.45);
  }

  /* 图片骨架屏 */
  .image-card-skeleton {
    background: linear-gradient(90deg, #2A2A2E 25%, #333338 50%, #2A2A2E 75%);
    background-size: 200% 100%;
  }

  /* × 关闭按钮 */
  .file-card-remove {
    background: rgba(255,255,255,0.15);
    box-shadow: 0 1px 4px rgba(0,0,0,0.5);
    &:hover { background: rgba(255,255,255,0.25); }
  }

  /* 输入框光标+placeholder+滚动条 */
  .chat-input-editable {
    caret-color: #fff;
    scrollbar-color: rgba(255,255,255,0.25) transparent;
    &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.25); }
    &::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.4); }
    &:empty::before {
      color: rgba(255,255,255,0.35);
    }
  }

  /* 工具栏图标 */
  .toolbar-icon {
    color: rgba(255,255,255,0.45);
    &:hover { color: rgba(255,255,255,0.8); }
  }

  /* 发送按钮 */
  .send-btn {
    background: #3A3A40;
    color: #666;
    &.send-btn-active {
      background: #fff;
      color: #000;
      &:hover { background: #e0e0e0; }
    }
  }

  /* 语音相关按钮 */
  .voice-cancel-btn {
    background: #2A2A2E; color: rgba(255,255,255,0.5);
    &:hover { background: #333338; color: rgba(255,255,255,0.8); }
  }

  /* 置底按钮 */
  .scroll-to-bottom-btn {
    background: #fff;
    color: #111;
    box-shadow: 0 1px 6px rgba(0,0,0,0.3), 0 0 0 1px rgba(255,255,255,0.06);
    &:hover { box-shadow: 0 2px 10px rgba(0,0,0,0.4); }
  }

  /* 对话覆盖层暗色适配 */
  .chat-view-overlay {
    background: #1A1A1E;
  }
  /* 预加载指示器暗色适配 */
  .msg-preloader-spinner {
    border-color: rgba(255,255,255,0.08);
    border-top-color: rgba(255,255,255,0.4);
  }

  /* ====== 对话区域暗色适配 ====== */
  .chat-messages-area {
    background: #1A1A1E;
    scrollbar-color: rgba(255,255,255,0.15) transparent;
    &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); }
    &::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.3); }
  }
  .time-divider {
    &::before, &::after { background: rgba(255,255,255,0.1); }
    span { color: #A0A2A6; }
  }
  /* L3-M0-6：typing 气泡暗色适配（继承 msg-bubble-other 背景但改三点色） */
  .typing-bubble .typing-dots {
    color: rgba(255,255,255,0.65);
  }
  .msg-bubble-other {
    background: #2A2A2E;
    color: rgba(255,255,255,0.9);
    &::selection { background: rgba(255,255,255,0.2); }
  }
  .msg-bubble-me {
    box-shadow: 0 1px 4px rgba(0,0,0,0.25);
  }
  .msg-image {
    background: linear-gradient(90deg, #2A2A2E 25%, #333338 50%, #2A2A2E 75%);
    background-size: 200% 100%;
    &.loaded { background: transparent; }
    &.error { background: #2A2A2E; }
  }
  .msg-grid-image {
    background: linear-gradient(90deg, #2A2A2E 25%, #333338 50%, #2A2A2E 75%);
    background-size: 200% 100%;
    &.loaded { background: transparent; }
    &.error { background: #2A2A2E; }
  }
  .msg-file-card {
    background: #2A2A2E;
    border-color: rgba(255,255,255,0.08);
    &:hover {
      background: #333338;
      border-color: rgba(255,255,255,0.14);
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    }
  }
  .msg-file-name { color: rgba(255,255,255,0.85); }
  .msg-file-ext  { color: rgba(255,255,255,0.4); }
  /* L3-M0-BUGFIX-V2：下载按钮圆形背景暗色适配 */
  .msg-file-download {
    background: rgba(255,255,255,0.06);
    color: rgba(255,255,255,0.45);
  }
  .msg-file-card:hover .msg-file-download {
    background: rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.18);
    color: var(--el-color-primary, #409eff);
  }
  .empty-state {
    color: rgba(255,255,255,0.4) !important;
    .text-g-600 { color: rgba(255,255,255,0.35) !important; }
  }

  /* ====== 全局排版暗色覆盖 ====== */
  .header-title { color: rgba(255,255,255,0.92); }
  .header-action-btn { color: rgba(255,255,255,0.5); &:hover { color: rgba(255,255,255,0.85); } }

  .home-card-title { color: rgba(255,255,255,0.92); }
  .home-card-desc { color: rgba(255,255,255,0.45); }
  .home-card-arrow { color: rgba(255,255,255,0.3); }
  .home-section-title { color: rgba(255,255,255,0.45); }
  .home-card-icon-wrap { background: rgba(255,255,255,0.08); }

  .conv-name { color: rgba(255,255,255,0.92); }
  .conv-time { color: rgba(255,255,255,0.35); }
  .conv-preview { color: rgba(255,255,255,0.45); }

  .ct-search { background: rgba(255,255,255,0.06); &:focus-within { background: rgba(255,255,255,0.08); } }
  .ct-search--disabled { background: rgba(255,255,255,0.06); }
  .ct-search-placeholder { color: rgba(255,255,255,0.35); }
  .ct-skel-card { background: rgba(255,255,255,0.06); }
  .ct-search-input { color: rgba(255,255,255,0.85); &::placeholder { color: rgba(255,255,255,0.35); } }
  .ct-filter-bar { background: rgba(255,255,255,0.06); }
  .ct-filter-slider { background: rgba(255,255,255,0.12); box-shadow: 0 1px 4px rgba(0,0,0,0.3); }
  .ct-filter-tab { color: rgba(255,255,255,0.65); &:hover { color: rgba(255,255,255,0.4); } &:active { color: rgba(255,255,255,0.3); } }
  .ct-filter-tab--active { color: rgba(255,255,255,0.92); &:hover { color: rgba(255,255,255,0.92); } &:active { color: rgba(255,255,255,0.92); } }
  .ct-class-card { background: rgba(255,255,255,0.06); }
  .ct-class-header { &:hover { background: rgba(255,255,255,0.03); } }
  .ct-class-name { color: rgba(255,255,255,0.65); }
  .ct-class-count { color: rgba(255,255,255,0.35); }
  .ct-class-icon { color: rgba(255,255,255,0.4); }
  .ct-class-arrow { color: rgba(255,255,255,0.3); }
  .ct-class-list { border-top-color: rgba(255,255,255,0.06); }
  .ct-persona-card { background: rgba(255,255,255,0.06); &:hover { background: rgba(255,255,255,0.08); } &:active { background: rgba(255,255,255,0.10); } }
  .ct-persona-name { color: rgba(255,255,255,0.92); }
  .ct-persona-role { color: rgba(255,255,255,0.45); }
  .ct-persona-sep { color: rgba(255,255,255,0.25); }
  .ct-persona-offline { color: rgba(255,255,255,0.35); }
  .ct-persona-action { color: rgba(255,255,255,0.3); }
  .ct-persona-card:hover .ct-persona-action { color: rgba(255,255,255,0.5); }
  .ct-group-name { color: rgba(255,255,255,0.92); }
  .ct-group-count { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.5); }
  .ct-row-name { color: rgba(255,255,255,0.92); }
  .ct-row-online { color: #4ade80; background: rgba(74,222,128,0.12); }
  .ct-row-offline { color: rgba(255,255,255,0.35); background: rgba(255,255,255,0.06); }
  .ct-row-action { color: rgba(255,255,255,0.3); }
  .ct-row { &:hover { background: rgba(255,255,255,0.04); } &:active { background: rgba(255,255,255,0.07); } }
  .ct-row:hover .ct-row-action { color: rgba(255,255,255,0.5); }

  .empty-title { color: rgba(255,255,255,0.55); }
  .empty-subtitle { color: rgba(255,255,255,0.35); }

  /* ====== 自定义滚动条暗色适配 ====== */
  .cs-thumb { background: rgba(255,255,255,0.15); }
  .cs-thumb:hover { background: rgba(255,255,255,0.3); }

  /* ====== Tab Bar 暗色适配 ====== */
  .tab-bar {
    border-top-color: rgba(255,255,255,0.08);
  }
  .tab-item {
    color: rgba(255,255,255,0.45);
    &:hover { color: rgba(255,255,255,0.65); }
    &:active { opacity: 0.7; }
  }
  .tab-item-active {
    color: rgba(255,255,255,0.95);
    &:hover { color: rgba(255,255,255,0.95); }
  }

  /* ====== 主页 Space 暗色适配 ====== */
  .home-card {
    background: rgba(255,255,255,0.06);
    &:hover { background: rgba(255,255,255,0.08); }
    &:active { background: rgba(255,255,255,0.10); }
  }
  .home-recent-card {
    background: rgba(255,255,255,0.06);
  }
  .home-conv-item {
    &:hover { background: rgba(255,255,255,0.04); }
    &:active { background: rgba(255,255,255,0.07); }
  }
  .home-section-title { color: rgba(255,255,255,0.92); }

  /* ====== 骨架屏暗色适配 ====== */
  .skeleton-avatar,
  .skeleton-line {
    background: rgba(255,255,255,0.06);
    background-image: linear-gradient(90deg, transparent 25%, rgba(255,255,255,0.04) 50%, transparent 75%);
    background-size: 200% 100%;
  }

  /* ====== 消息卡片暗色适配 ====== */
  .msg-conv-card {
    background: rgba(255,255,255,0.06);
    &:hover { background: rgba(255,255,255,0.08); }
    &:active { background: rgba(255,255,255,0.10); }
  }
  /* L3-M0-7：置顶卡片 + pin 角标 + mute 图标 + 菜单分隔线暗色适配 */
  .msg-conv-card--pinned {
    background: color-mix(in srgb, var(--theme-color, #409eff), transparent 85%);
    &:hover { background: color-mix(in srgb, var(--theme-color, #409eff), transparent 80%); }
    &:active { background: color-mix(in srgb, var(--theme-color, #409eff), transparent 75%); }
  }
  .conv-pin-badge { color: var(--theme-color, #409eff); opacity: 0.85; }
  .conv-mute-icon { color: rgba(255,255,255,0.55); }
  /* L3-M0-8：草稿前缀暗色下稍亮以保证对比度 */
  .conv-draft-prefix { color: #f87171; }
  .unread-badge--muted {
    background: linear-gradient(135deg, rgba(255,255,255,0.35), rgba(255,255,255,0.2)) !important;
    color: rgba(0,0,0,0.7) !important;
    box-shadow: 0 2px 6px rgba(0,0,0,0.3) !important;
  }
  .ccm-divider { background: rgba(255,255,255,0.1); }

  /* ====== L3-M1-4：归档卡片暗色适配 ====== */
  .archive-card { background: rgba(255,255,255,0.04); }
  .archive-card-header {
    &:hover { background: rgba(255,255,255,0.06); }
    &:active { background: rgba(255,255,255,0.08); }
  }
  .archive-icon-wrap { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.55); }
  .archive-title { color: rgba(255,255,255,0.92); }
  .archive-subtitle { color: rgba(255,255,255,0.5); }
  .archive-count {
    background: rgba(255,255,255,0.1);
    color: rgba(255,255,255,0.7);
  }
  .archive-chevron { color: rgba(255,255,255,0.4); }
  .msg-conv-card--archived::before { background: rgba(255,255,255,0.1); }

  /* ====== FAB 暗色适配 ====== */
  .fab-btn-fixed {
    background: rgba(255,255,255,0.92);
    color: #1D1D1F;
    box-shadow: 0 4px 14px rgba(0,0,0,0.4);
    &:hover { box-shadow: 0 6px 18px rgba(0,0,0,0.5); }
  }

  /* ====== 消息状态指示器 & 字数计数器 暗色适配 ====== */
  .msg-status-sending { color: rgba(255,255,255,0.35); }
  .msg-status-sent { color: rgba(255,255,255,0.45); }
  .msg-status-delivered { color: #4ade80; }
  .msg-status-read { color: #60a5fa; }
  .msg-status-failed { color: #f87171; }
  .msg-length-counter { color: rgba(255,255,255,0.45); }
  .msg-length-counter.msg-length-over { color: #f87171; }
}

/* ====== L3：已撤回消息占位 + 消息操作菜单 + 会话右键菜单 ====== */
.msg-bubble-deleted {
  background: transparent !important;
  color: var(--el-text-color-placeholder, #9ca3af) !important;
  border: 1px dashed var(--el-border-color, #e5e7eb) !important;
  box-shadow: none !important;
  font-style: italic;
  font-size: 13px !important;
}
.msg-deleted-placeholder {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--el-text-color-placeholder, #9ca3af);
}
.msg-deleted-icon {
  font-size: 14px;
}
/* L3-M0-4：撤回后 2 分钟内的"重新编辑"按钮（对齐微信风格） */
.msg-re-edit-btn {
  margin-left: 6px;
  padding: 2px 8px;
  background: transparent;
  border: none;
  color: var(--el-color-primary, #409eff);
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s ease;
  &:hover {
    background: rgba(64, 158, 255, 0.1);
  }
  &:active {
    background: rgba(64, 158, 255, 0.15);
  }
}

/* 消息操作菜单（气泡旁，悬浮显示） */
.msg-actions {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.msg-row:hover .msg-actions {
  opacity: 1;
}
.msg-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  font-size: 12px;
  line-height: 1;
  background: var(--el-bg-color, #fff);
  color: var(--el-text-color-regular, #4b5563);
  border: 1px solid var(--el-border-color, #e5e7eb);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.msg-action-btn:hover {
  background: var(--el-color-primary-light-9, #ecf5ff);
  border-color: var(--el-color-primary-light-5, #a0cfff);
  color: var(--el-color-primary, #409eff);
}
.msg-action-btn.msg-action-danger:hover {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
}

/* 会话右键上下文菜单 */
.conv-context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  padding: 4px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color, #e5e7eb);
  border-radius: 8px;
  box-shadow: 0 6px 24px rgba(0,0,0,0.12);
  animation: ccm-in 0.12s ease-out;
}
@keyframes ccm-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.ccm-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  background: transparent;
  color: var(--el-text-color-regular, #4b5563);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}
.ccm-item:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}
.ccm-item.ccm-item-danger:hover {
  background: #fef2f2;
  color: #dc2626;
}
/* L3-M0-7：右键菜单分隔线（区分置顶/免打扰与归档两组操作） */
.ccm-divider {
  height: 1px;
  background: var(--el-border-color-lighter, #eef0f5);
  margin: 4px 6px;
}

/* ====== 消息状态指示器（气泡右下角小图标，仅自己消息显示） ====== */
.msg-status-indicator {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 2px;
  padding-right: 2px;
  justify-content: flex-end;
}
.msg-status-icon {
  font-size: 12px;
  line-height: 1;
  transition: color 0.15s;
}
.msg-status-sending {
  color: var(--el-text-color-placeholder, #9ca3af);
  animation: msg-status-pulse 1.5s ease-in-out infinite;
}
.msg-status-sent {
  color: var(--el-text-color-placeholder, #9ca3af);
}
.msg-status-delivered {
  color: #10b981; /* WhatsApp 风格的已送达绿色 */
}
.msg-status-read {
  color: #2563eb; /* WhatsApp 风格的蓝色双勾 "已读" */
}
.msg-status-failed {
  color: #ef4444;
  cursor: pointer;
  &:hover { opacity: 0.8; }
}
@keyframes msg-status-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

/* ====== 字数计数器（靠近上限时显示） ====== */
.msg-length-counter {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: #f59e0b; /* 靠近上限：橙色 */
  margin-right: 8px;
  align-self: center;
  user-select: none;
  transition: color 0.15s;
}
.msg-length-counter.msg-length-over {
  color: #ef4444; /* 超限：红色 */
  font-weight: 600;
}
</style>
