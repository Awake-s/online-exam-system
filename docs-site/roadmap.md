# 技术演进规划

> 本项目已完整交付考试业务全闭环（题库 / 组卷 / 考试 / 阅卷 / 成绩 / 通知 / 聊天），并通过接口测试与性能压测验证。
> 以下为个人后续学习与工程演进的规划方向，非产品迭代计划。

## 全景路线图

```
✅ 已完成            ✅ 已完成            ⬜ 规划中
─────────────────────────────────────────────────────────────
全栈开发闭环 ──▶ 开源上线 ──▶ 云原生演进 ──▶ 微服务 + 可观测性
```

---

## ✅ 阶段一：全栈开发 · 完整闭环交付（已完成）

完成完整的考试业务闭环：需求分析、概要 / 详细设计、编码实现、系统测试（接口 + 性能压测）。

**核心交付**：

- 19 张表的数据库设计 + 完整 E-R 模型
- 20 个 Controller、19 个业务服务
- 三端（管理员 / 教师 / 学生）角色分治
- 完整的部署脚本链 + 三套环境配置

---

## ✅ 阶段二：开源上线 · GitHub 作品集（已完成）

- 根 `README.md`：项目横幅、徽章、技术栈、架构图、快速启动
- `LICENSE`（MIT）
- GitHub 仓库 Topics：`spring-boot` `vue3` `exam-system` `mybatis-plus`
- CI 流水线：官网 + 落地页自动部署到 GitHub Pages

---

## ⬜ 阶段三：P1 — 云原生 + 容器化

> 目标：告别裸机部署，进入容器化与自动化交付。

### Spring Boot 3 + Java 17 升级

依据 [官方迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)：

- `javax.*` → `jakarta.*`（推荐用 [OpenRewrite](https://docs.openrewrite.org/recipes/spring/boot3upgrade) 自动化）
- parent 升级到 3.2.x
- jjwt 升级到 0.12.x
- 验证 MyBatis-Plus 3.5.5+ 兼容性

### Docker 化

- 后端多阶段 `Dockerfile`（jdk17-slim 构建 + jre 运行，分层缓存优化）
- 前端 `Dockerfile`（node 构建 + nginx 运行）
- `docker-compose.yml` 一键拉起 mysql + redis + backend + frontend

### 配置中心化

引入 **Nacos**（先做配置中心，注册中心留到 P2），把生产配置迁到 Nacos，配置与代码彻底分离。

### 持续交付

扩展 CI → 构建 Docker 镜像 → 推到 GHCR（GitHub Container Registry），加 release 工作流（打 tag 自动发版 + changelog）。

---

## ⬜ 阶段四：P2 — 微服务拆分 + 可观测性（长期演进）

> 目标：从单体演进到微服务，具备企业级高可用与可观测能力。

### 微服务化（Spring Cloud Alibaba）

引入 **Spring Cloud Alibaba 2023.x**，按业务域拆分服务：

```
exam-gateway         网关（路由 / 限流 / 鉴权）
├─ exam-user-service      用户 / 权限 / 认证
├─ exam-question-service  题库 / 试卷 / 组卷
├─ exam-exam-service      考试 / 作答 / 阅卷
└─ exam-message-service   聊天 / 通知
```

**配套组件**：

- **Nacos**：注册中心 + 配置中心
- **Sentinel**：流量控制、熔断降级、热点参数限流
- **Seata**：分布式事务（组卷 + 考试提交等跨服务场景）
- **ShardingSphere**：分库分表（考试记录 / 答题记录按班级分表）

### 可观测性三件套

| 维度 | 方案 | 作用 |
| :--- | :--- | :--- |
| 日志 | Loki + Promtail | 聚合后端日志（比 ELK 轻量） |
| 指标 | Prometheus + Grafana | 监控 JVM / MySQL / Redis / Nginx，配告警 |
| 链路 | SkyWalking / OpenTelemetry | 跨服务慢调用追踪 |

### 高可用中间件

- **消息队列**：RocketMQ（成绩发布异步通知、考试结束自动阅卷解耦）
- **定时任务**：XXL-JOB 替换 `@Scheduled`（考试结束任务分布式调度）
- **缓存**：Redis 主从 + 哨兵，热点数据（题库 / 试卷）多级缓存（Caffeine + Redis）

---

::: tip 完整作战清单
本页是概览版。详细的**可勾选任务清单**（含每项的学习资源、预计工时、验收标准）见 `doc/迭代路线图.md`，具体的技术改造点见 `doc/技术债清单.md`。
:::
