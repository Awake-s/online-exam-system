# 项目官网（docs-site）

基于 **VitePress 1.6** 构建的在线考试系统项目展示官网，构建产物为纯静态 HTML，部署到 GitHub Pages。

## 本地开发

```bash
cd docs-site
pnpm install      # 安装依赖（推荐 pnpm；本机若 npm 异常请用 pnpm）
pnpm dev          # 启动开发服务器 http://localhost:5173
pnpm build        # 构建到 .vitepress/dist/
pnpm preview      # 本地预览构建产物
```

> 说明：本机若出现 `npm install` 报 "up to date" 但依赖未实际安装的情况（npm 缓存损坏），
> 请改用 `pnpm install`。CI 环境（GitHub Actions / Linux）用 `npm ci` 即可，不受影响。

## 目录结构

```
docs-site/
├── .vitepress/
│   ├── config.ts        # 站点全局配置（导航、SEO、主题、base 路径）
│   └── theme/
│       ├── index.ts     # 主题入口
│       └── custom.css   # 品牌色与样式覆盖
├── public/
│   ├── logo.svg         # 站点 Logo
│   └── hero-architecture.svg  # 首页 Hero 架构图
├── index.md             # 首页（Hero + 数据亮点 + 项目历程 + 技术栈）
├── features.md          # 功能特性
├── roles.md             # 三端展示
├── architecture.md      # 技术架构
├── benchmark.md         # 性能数据
├── roadmap.md           # 迭代路线
└── package.json
```

## 部署

推送到 main 分支后，`.github/workflows/deploy-docs.yml` 会自动构建并发布到 GitHub Pages。

**首次使用前需配置（见 `config.ts` 顶部注释）：**

1. 把 `GITHUB_USER`、`REPO`、`LIVE_URL` 替换为你的真实值
2. GitHub 仓库 → Settings → Pages → Source 选择 "GitHub Actions"
3. `base` 默认 `/online-exam-system/`（项目站点）；若用自定义域名根路径，改为 `/`

## 内容来源

官网内容复用了 `doc/` 下的项目设计素材：

- 性能数据 ← `doc/系统测试/性能测试/压测报告_v2.0.md`
- 架构图 ← `doc/概要设计/系统整体架构图/`
- 功能需求 ← `doc/需求分析/产品需求文档(PRD).md`
- 演进规划 ← `doc/迭代路线图.md`
