# 在线考试系统 · 项目落地页（landing）

基于 **Astro 5 + Tailwind 4** 构建的项目展示首页。零 JS 默认输出、构建为纯静态 HTML，可部署到 Vercel / Netlify / GitHub Pages。

> 与 `docs-site/`（VitePress 文档站）分工：本页面是「项目门面」，文档站是「技术文档区」（`/docs`）。

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

## ✅ 配置状态

- [x] `SITE.githubUrl`：`https://github.com/Awake-s/online-exam-system`
- [x] `SITE.demoUrl`：`http://124.222.21.219`（备案通过后换 `https://examplatform.online`）
- [x] `astro.config.mjs`：已配置 GitHub Pages 子路径 `base: '/online-exam-system/landing/'`
- [x] CI 自动部署：`.github/workflows/deploy-docs.yml` 已包含 landing 构建
- [ ] `public/screens/{admin,teacher,student}.svg`：替换为**真实三端界面截图**（PNG/WebP 亦可，记得同步改 `consts.ts` 里的 `shot` 路径）
- [ ] 可选：录制一段 10–30s 考试作答 GIF 放到 Hero 或功能区

## 部署

- **GitHub Pages（已配置）**：推送到 main 分支后，`.github/workflows/deploy-docs.yml` 自动构建 docs-site + landing，合并发布到 GitHub Pages。
  - landing 访问地址：`https://awake-s.github.io/online-exam-system/landing/`
- **Vercel（可选独立部署）**：导入仓库，Framework 选 Astro，`base` 改为 `/`，自动 HTTPS + 预览部署。

## 内容真实性说明

本页所有功能与数据均取自项目真实源码与压测报告 v2.0，**未包含任何虚构能力**（已剔除原 VitePress 版本中数据模型不支持的「知识点」描述）。
