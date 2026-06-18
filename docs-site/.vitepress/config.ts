import { defineConfig } from 'vitepress'

// ====================================================================
// 在线考试系统 · 产品官网（VitePress）全局配置
// ====================================================================
// 部署说明：
//   - 默认部署到 GitHub Pages：https://<用户名>.github.io/online-exam-system/
//   - 若部署到自定义域名根路径，把下面的 base 改成 '/'，并设置 CNAME
//   - 将下方 <GITHUB_USER>、<REPO>、<LIVE_URL> 替换为你真实的值
// ====================================================================

const GITHUB_USER = 'Awake-s' // GitHub 用户名
const REPO = 'online-exam-system' // 仓库名
// 在线体验地址（已部署的后台系统），访客点击「在线体验」会跳到这里
// 域名 examplatform.online 备案完成后，把这里换成 https://examplatform.online 即可
// （Nginx server_name 通配 _，IP 与域名命中同一 server，前端/后端零改动）
const LIVE_URL = 'http://124.222.21.219'

export default defineConfig({
  lang: 'zh-CN',
  title: '在线考试系统',
  description:
    '面向高校与教育培训机构的一站式在线考试平台：智能题库管理、灵活组卷、在线考试、自动阅卷、成绩分析、即时通讯通知。Spring Boot + Vue3 + MySQL + Redis。',
  // GitHub Pages 项目站点需要子路径；自定义域名根路径时改为 '/'
  base: '/online-exam-system/',
  lastUpdated: true,
  cleanUrls: true,

  head: [
    ['meta', { name: 'theme-color', content: '#3c6fff' }],
    // Open Graph / 社交分享
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:title', content: '在线考试系统' }],
    [
      'meta',
      {
        property: 'og:description',
        content: '一站式在线考试平台：题库管理 · 智能组卷 · 在线考试 · 自动阅卷 · 成绩分析'
      }
    ],
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    // 站点图标（可替换为自有 favicon）
    ['link', { rel: 'icon', href: '/favicon.ico' }]
  ],

  markdown: {
    lineNumbers: false
  },

  themeConfig: {
    // 站点标识（左上角）
    logo: '/logo.svg',
    siteTitle: '在线考试系统',

    // 顶部导航
    nav: [
      { text: '首页', link: '/' },
      { text: '功能特性', link: '/features' },
      { text: '三端展示', link: '/roles' },
      { text: '技术架构', link: '/architecture' },
      { text: '性能数据', link: '/benchmark' },
      { text: '迭代路线', link: '/roadmap' },
      { text: '在线体验', link: LIVE_URL }
    ],

    // 右上角社交链接
    socialLinks: [
      { icon: 'github', link: `https://github.com/${GITHUB_USER}/${REPO}` }
    ],

    // 顶部大按钮（Hero 区由首页 frontmatter 控制，这里配搜索）
    search: {
      provider: 'local',
      options: {
        translations: {
          button: {
            buttonText: '搜索文档',
            buttonAriaLabel: '搜索文档'
          },
          modal: {
            noResultsText: '无法找到相关结果',
            resetButtonTitle: '清除查询条件',
            footer: {
              selectText: '选择',
              navigateText: '切换'
            }
          }
        }
      }
    },

    // 右侧大纲
    outline: {
      level: [2, 3],
      label: '本页内容'
    },

    docFooter: {
      prev: '上一篇',
      next: '下一篇'
    },

    lastUpdatedText: '最后更新',

    // 页脚
    footer: {
      message: '基于 MIT 协议开源 · 仅用于学习与个人作品展示',
      copyright: 'Copyright © 2026 在线考试系统'
    },

    // 暗黑模式开关
    darkModeSwitchLabel: '主题',
    sidebarMenuLabel: '菜单',
    returnToTopLabel: '回到顶部'
  }
})
