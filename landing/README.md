# 在线考试系统 · 营销落地页（landing）

基于 **Astro 5 + Tailwind 4** 构建的产品营销首页。零 JS 默认输出、构建为纯静态 HTML，可部署到 Vercel / Netlify / GitHub Pages。

> 与 `docs-site/`（VitePress 文档站）分工：本项目是「产品门面」，文档站是「技术文档区」（`/docs`）。

## 本地开发

```bash
cd landing
npm install
npm run dev       # http://localhost:4321
npm run build     # 构建到 dist/
npm run preview   # 预览构建产物
```

## 目录结构

```
landing/
├─ src/
│  ├─ consts.ts              # ★ 所有文案/数据/链接集中于此，改内容只动这里
│  ├─ layouts/BaseLayout.astro   # HTML 骨架 + SEO/OG
│  ├─ components/            # 9 段营销组件
│  │   Nav / Hero / Stats / Problem / Features /
│  │   Roles / HowItWorks / TechStack / FAQ / CTA / Footer
│  ├─ pages/index.astro      # 组装首页
│  └─ styles/global.css      # Tailwind + 品牌色令牌
└─ public/
   ├─ favicon.svg
   ├─ og-image.svg           # 社交分享卡片
   └─ screens/               # 三端截图（占位 SVG，待替换）
```

## ⚠️ 上线前必做替换清单（均在 `src/consts.ts`）

- [x] `SITE.githubUrl`：已改为真实仓库 `https://github.com/Awake-s/online-exam-system`
- [x] `SITE.demoUrl`：已改为 `http://124.222.21.219`（裸 IP，能直接访问已部署的后台）。**域名 `examplatform.online` 备案完成后，把这里换成 `https://examplatform.online` 即可**（Nginx 通配，前端/后端零改动）
- [ ] `SITE.demoUrl` 的 CORS：后端 `APP_CORS_ORIGINS`（服务器 `/etc/exam-system/secrets.env`）需加入 `http://124.222.21.219`，否则访客登录时 API 跨域被拦截
- [ ] `DEMO_ACCOUNTS`：当前是种子数据真实账号；对外公开建议改为只读/可重置专用账号
- [ ] `public/screens/{admin,teacher,student}.svg`：替换为**真实三端界面截图**（PNG/WebP 亦可，记得同步改 `consts.ts` 里的 `shot` 路径）。这是落地页从「普通」变「可信」的关键一步
- [ ] 可选：录制一段 10–30s 考试作答 GIF 放到 Hero 或功能区
- [ ] `astro.config.mjs` 的 `site`：改为真实域名；GitHub Pages 子路径部署再开启 `base`

## 部署

- **Vercel（推荐）**：导入仓库，Framework 选 Astro，自动 HTTPS + 预览部署。
- **GitHub Pages**：开启 `astro.config.mjs` 的 `base: '/仓库名/'`，用 Actions 构建 `dist/` 发布。

## 内容真实性说明

本页所有功能与数据均取自项目真实源码与压测报告 v2.0，**未包含任何虚构能力**（已剔除原 VitePress 版本中数据模型不支持的「知识点」描述）。
