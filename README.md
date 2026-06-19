<div align="center">

# 🎓 在线考试系统

**个人全栈技术实践项目 · 在线考试系统**

以考试业务为载体的 Spring Boot + Vue 3 全栈工程实践，覆盖题库管理 · 智能组卷 · 在线考试 · 自动阅卷 · 成绩分析 · 即时通讯完整闭环，所有数据均为演示数据

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883.svg)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6.x-dc382d.svg)](https://redis.io/)

[在线体验](http://124.222.21.219) · [产品官网](https://awake-s.github.io/online-exam-system/) · [功能特性](docs-site/features.md) · [迭代路线](docs-site/roadmap.md)

</div>

---

## ✨ 项目简介

一个基于 **Spring Boot + Vue3** 前后端分离架构的在线考试系统，支持**管理员 / 教师 / 学生**三端角色，权限严格隔离。从出题、组卷、发布考试、在线答题、自动阅卷到成绩分析，形成完整的数字化考试闭环。

> 本项目源自毕业设计（获评「优」），现作为个人技术作品集持续迭代，规划向企业级微服务架构演进。

## 🌟 核心功能

| 模块 | 能力 |
| :--- | :--- |
| **智能题库** | 单选/多选/判断/填空/简答 5 种题型，富文本编辑，Excel 批量导入导出 |
| **灵活组卷** | 手动组卷 + 模板自动组卷（按题型/难度/知识点抽题，生成平行试卷） |
| **在线考试** | 全屏答题、倒计时、自动保存、断网续答、切屏检测防作弊、到时自动交卷 |
| **自动阅卷** | 客观题自动批改 + 主观题教师评分，成绩自动汇总 |
| **成绩分析** | 班级统计、正确率分析、分数分布图、成绩趋势图（ECharts） |
| **即时通讯** | 基于 WebSocket(STOMP) 的实时通知（12 类）与师生一对一聊天 |

## 🏗️ 技术栈

### 后端（`exam-system/`）

| 类别 | 技术 |
| :--- | :--- |
| 框架 | Spring Boot 2.7.18 · Spring Security · Validation |
| 持久化 | MyBatis-Plus 3.5 · MySQL 8.0 · HikariCP · Redis（Lettuce） |
| 认证 | JWT（jjwt 0.11.5）+ Redis 黑名单 + 内存降级 |
| 实时通信 | Spring WebSocket（STOMP 协议） |
| 工具 | EasyExcel 3.3 · Hutool · Guava（登录限流） |
| 运维 | Actuator · 多环境配置（dev/prod/perf） |

### 前端（`art-design-pro-ui/`）

| 类别 | 技术 |
| :--- | :--- |
| 框架 | Vue 3.5 · TypeScript 5.6 · Vite 7 |
| UI | Element Plus 2.11（按需引入） · Tailwind CSS 4 · Sass |
| 状态/路由 | Pinia 3（持久化） · Vue Router 4 |
| 可视化 | ECharts 6 · wangEditor 5 |
| 通信 | Axios · @stomp/stompjs · sockjs-client |
| 质量 | ESLint 9 · Prettier · Stylelint · vue-tsc |

### 产品官网（`docs-site/`）

| 技术 | 用途 |
| :--- | :--- |
| VitePress 1.6 | 静态站点生成（SEO 友好、首屏秒开） |
| GitHub Pages | 免费托管 |

## 📐 系统架构

```
┌─────────────────────────────────────────────────┐
│              浏览器（管理员/教师/学生）            │
└──────────────────────┬──────────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────────┐
│            Nginx（反代 · 安全头 · 限流）           │
└──────┬────────────────────────┬─────────────────┘
       │ /api                   │ /ws (STOMP)
┌──────▼────────────────────────▼─────────────────┐
│         Spring Boot 单体应用 (8081)              │
│   Controller (20) → Service (19) → Mapper (19)   │
│   Security/JWT · WebSocket · 定时任务 · 全局异常  │
└──────┬───────────────────────────┬──────────────┘
       │                           │
┌──────▼──────────┐       ┌────────▼────────┐
│   MySQL 8.0     │       │     Redis       │
│  19 张表 · 逻辑删除 │       │ 缓存 · Token黑名单 │
└─────────────────┘       └─────────────────┘
```

> 详细的分层说明、认证流程、数据库设计见 [`docs-site/architecture.md`](docs-site/architecture.md)

## 🚀 快速开始

### 环境要求

- **JDK** 11+（推荐 17）
- **Node.js** 18+（推荐 22）
- **MySQL** 8.0
- **Redis** 6+
- **Maven** 3.6+

### 1. 准备数据库

```bash
# 导入数据库脚本
mysql -u root -p < doc/sql/online_exam_system.sql
# 首次启动会自动创建管理员账号 admin / admin123
```

### 2. 启动后端

```bash
cd exam-system
# 修改 application-dev.yml 中的数据库/Redis 连接信息
mvn spring-boot:run
# 后端启动于 http://localhost:8081
```

### 3. 启动前端

```bash
cd art-design-pro-ui
npm install
npm run dev
# 前端启动于 http://localhost:5173
```

### 4. 默认账号

| 角色 | 账号 | 密码 |
| :--- | :--- | :--- |
| 管理员 | `admin` | `admin123` |
| 教师 / 学生 | 由管理员在「用户管理」中创建 |

## 📊 性能数据

基于 Apache JMeter 5.6.3 全场景压测（详见 [`docs-site/benchmark.md`](docs-site/benchmark.md)）：

| 指标 | 数值 |
| :--- | ---: |
| 并发用户 | 188 |
| P50 响应中位数 | **321 ms** |
| 吞吐量 TPS | 34.7 req/s |
| 系统级错误率 | **0.00%** |
| APDEX 得分 | 0.75 |

## 📁 项目结构

```
在线考试系统/
├── exam-system/          # 后端（Spring Boot）
├── art-design-pro-ui/    # 前端（Vue3 + Vite）
├── docs-site/            # 产品官网（VitePress → GitHub Pages）
├── deploy/               # 部署工具链（脚本 + 配置 + 文档）
├── doc/                  # 项目文档（数据库设计/PRD/测试报告/论文）
└── .github/workflows/    # CI/CD（官网自动部署）
```

## 🗺️ 迭代路线

本项目持续向企业级架构演进，完整路线见 [`doc/迭代路线图.md`](doc/迭代路线图.md)：

- ✅ **已完成**：毕业设计全闭环（题库/组卷/考试/阅卷/成绩/通知/聊天）
- 🔵 **P0**：开源上线 · API 文档（Knife4j）· 单元测试 · CI/CD
- ⬜ **P1**：Spring Boot 3 + Java 17 · Docker 容器化 · Nacos 配置中心
- ⬜ **P2**：Spring Cloud Alibaba 微服务 · 可观测性 · 高可用中间件

## 📝 License

[MIT](LICENSE) — 仅用于学习与个人作品展示。

---

<div align="center">

**[在线体验](http://124.222.21.219)** · **[产品官网](https://awake-s.github.io/online-exam-system/)** · **[迭代路线](doc/迭代路线图.md)**

如果这个项目对你有帮助，欢迎 ⭐ Star 支持！

</div>
