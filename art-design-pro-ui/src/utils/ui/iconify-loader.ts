/**
 * 离线图标加载器（已启用）
 *
 * 用于在中国大陆 / 内网 / 海外等 iconify 公共 CDN 不可达的环境下，
 * 预加载图标集数据到 @iconify/vue 的本地缓存，避免运行时 CDN 拉取超时。
 *
 * 加载策略：精准匹配本项目实际使用的图标前缀（共 6 类），
 * 不引入项目未使用的图标集，控制 dev 依赖体积。
 *
 * 当前覆盖：
 *   - ri (Remix Icons)        291 处  ~1.0 MB  主力业务图标
 *   - fluent (Microsoft)        3 处 ~11.7 MB  art-global-search 回车键
 *   - tabler                    1 处  ~2.0 MB  art-chat-window 滚动按钮
 *   - vaadin                    1 处  ~0.2 MB  art-header-bar Ctrl 键
 *   - iconamoon                 1 处  ~0.6 MB  art-work-tab 下拉箭头
 *   - system-uicons             1 处  ~0.1 MB  art-notification 空收件箱
 *
 * 新增图标集时：
 * 1. 安装：npm install -D @iconify-json/<name>
 * 2. 在此文件 import + addCollection 即可
 *
 * @module utils/ui/iconify-loader
 */

import { addCollection } from '@iconify/vue'

// 主力业务图标集
import riIcons from '@iconify-json/ri/icons.json'

// art-design-pro 模板组件用到的小众图标集
import fluentIcons from '@iconify-json/fluent/icons.json'
import tablerIcons from '@iconify-json/tabler/icons.json'
import vaadinIcons from '@iconify-json/vaadin/icons.json'
import iconamoonIcons from '@iconify-json/iconamoon/icons.json'
import systemUiconsIcons from '@iconify-json/system-uicons/icons.json'

// 注册到 @iconify/vue 全局缓存（运行时按需读取本地数据，零网络调用）
addCollection(riIcons)
addCollection(fluentIcons)
addCollection(tablerIcons)
addCollection(vaadinIcons)
addCollection(iconamoonIcons)
addCollection(systemUiconsIcons)
