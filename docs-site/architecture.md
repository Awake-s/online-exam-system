# 技术架构

> 经典单体分层架构，工程化程度接近生产级。后续规划平滑演进到微服务（见 [迭代路线](/roadmap)）。

## 整体架构

系统采用前后端分离的 B/S 架构，前端为单 Vue3 SPA（三端角色分治），后端为 Spring Boot 单体应用，MySQL 持久化 + Redis 缓存。

```
┌─────────────────────────────────────────────────────────┐
│                    浏览器 / 客户端                         │
│   管理员端  │  教师端  │  学生端   (角色驱动动态路由)        │
└───────────────┬─────────────────────────────────────────┘
                │  HTTPS
┌───────────────▼─────────────────────────────────────────┐
│                    Nginx 反向代理                         │
│   静态资源 │ /api 反代 │ /ws WebSocket │ 安全头 │ 限流     │
└───────┬───────────────────┬─────────────────────────────┘
        │ HTTP              │ WebSocket(STOMP)
┌───────▼───────────────────▼─────────────────────────────┐
│              Spring Boot 单体应用 (端口 8081)              │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Controller 层   (20 个, @PreAuthorize 角色控制)   │   │
│  ├──────────────────────────────────────────────────┤   │
│  │  Service 层      (19 个业务服务 + impl)            │   │
│  ├──────────────────────────────────────────────────┤   │
│  │  Mapper 层       (MyBatis-Plus BaseMapper)         │   │
│  ├──────────────────────────────────────────────────┤   │
│  │  横切：Security/JWT · WebSocket · 定时任务 · 异常   │   │
│  └──────────────────────────────────────────────────┘   │
└───────┬───────────────────────────────────┬─────────────┘
        │                                   │
┌───────▼───────────────┐         ┌────────▼─────────┐
│      MySQL 8.0         │         │      Redis        │
│  19 张表 · HikariCP    │         │ 缓存 · Token黑名单 │
└────────────────────────┘         └───────────────────┘
```

## 后端技术栈

| 类别 | 技术 | 版本 | 用途 |
| :--- | :--- | :--- | :--- |
| 基础框架 | Spring Boot | 2.7.18 | Web 服务、自动配置 |
| Web | Spring MVC + Validation | 随 Boot | RESTful API、参数校验 |
| 安全 | Spring Security + jjwt | 0.11.5 | JWT 无状态认证、角色鉴权 |
| 实时通信 | spring-boot-starter-websocket | 随 Boot | STOMP 协议，聊天 + 通知推送 |
| ORM | MyBatis-Plus | 3.5.5 | 增删改查、逻辑删除、分页 |
| 数据库 | MySQL | 8.0 | 业务持久化 |
| 连接池 | HikariCP | 随 Boot | 高性能连接池（生产 max=20） |
| 缓存 | Redis（Lettuce） | 6.5.1 | 缓存、JWT 黑名单、限流 |
| Excel | EasyExcel | 3.3.3 | 题库 / 用户批量导入导出 |
| 工具 | Hutool / Guava | 5.8.25 / 33.0 | 通用工具、登录限流 |
| 运维 | Actuator | 随 Boot | 健康检查、指标暴露 |

## 前端技术栈

| 类别 | 技术 | 版本 | 用途 |
| :--- | :--- | :--- | :--- |
| 框架 | Vue | 3.5.21 | 渐进式框架（Composition API） |
| 语言 | TypeScript | 5.6.3 | 类型安全 |
| 构建 | Vite | 7.1.5 | 极速 HMR + 生产打包 |
| UI 库 | Element Plus | 2.11.2 | 企业级组件（按需引入） |
| 状态管理 | Pinia | 3.0.3 | 组合式 store（持久化插件） |
| 路由 | Vue Router | 4.5.1 | 动态路由、权限守卫 |
| HTTP | Axios | 1.12.2 | 请求封装、拦截器 |
| 样式 | Tailwind CSS 4 + Sass | 4.1.14 | 原子化 + 预处理 |
| 图表 | ECharts | 6.0.0 | 成绩分析可视化 |
| 富文本 | wangEditor | 5.1.23 | 题目编辑（公式 / 图片） |
| 实时通信 | @stomp/stompjs + sockjs | 7.x | 聊天 / 通知 |
| 国际化 | vue-i18n | 9.14.0 | 多语言预留 |

## 认证授权设计

采用 **JWT 无状态认证 + Redis 黑名单 + Spring Security 角色** 的组合方案：

```
登录流程：
  ① 客户端 POST /api/auth/login (账号+密码)
  ② LoginRateLimiter 限流校验 (Guava, 防 brute force)
  ③ BCrypt 校验密码 → 生成 JWT (HS256, 含 username + roleCode, 24h)
  ④ 返回 token

请求鉴权：
  ⑤ JwtAuthenticationFilter 解析 Bearer token
  ⑥ 校验签名 + 查 Redis 黑名单 (登出失效)
  ⑦ 查 DB 确认用户 status==1 → 构造 Authentication (ROLE_{roleCode})
  ⑧ Controller @PreAuthorize("hasRole('XXX')") 角色校验

登出：
  ⑨ token 加入 Redis 黑名单 → 立即失效
  (Redis 故障时降级为内存 ConcurrentHashMap, @Scheduled 清理)
```

::: tip 设计亮点
- **无状态**：`SessionCreationPolicy.STATELESS`，天然适配水平扩展
- **黑名单降级**：Redis 不可用时退回内存 Map，保证可用性
- **双重校验**：路由守卫（前端）+ `@PreAuthorize`（后端）双重权限拦截
- **生产安全**：prod 环境 CORS 精确白名单、错误信息全隐藏、Actuator 端点收敛
:::

## 数据库设计

19 张表，按业务域前缀命名，覆盖五大模块：

| 模块 | 表 | 说明 |
| :--- | :--- | :--- |
| 系统 / 权限 | `sys_role` `sys_user` `sys_notification` | 角色、用户、通知（12 类） |
| 教务基础 | `edu_major` `edu_class` `edu_subject` `subject_major` | 专业、班级、科目及关联 |
| 题库 / 试卷 | `exam_question` `exam_paper` `exam_paper_question` | 题目、试卷、试卷题目关联 |
| 试卷模板 | `exam_paper_template` `exam_template_rule` | 自动组卷模板与抽题规则 |
| 考试 / 作答 | `exam_exam` `exam_record` `exam_answer` | 考试、考试记录、答题明细 |
| 任课关系 | `teacher_class` `teacher_subject` | 教师与班级 / 科目关联 |
| 即时通讯 | `chat_conversation` `chat_message` | 聊天会话、消息 |

- **逻辑删除**：全局 `deleted` 字段（1 删 / 0 存），MyBatis-Plus 自动过滤
- **字符集**：utf8mb4（支持 emoji 与特殊字符）
- **存储引擎**：InnoDB（事务支持）

## 部署架构

当前采用**裸机脚本部署**（systemd + Nginx），后续规划 Docker 化（见 [路线图](/roadmap)）：

| 组件 | 托管方式 | 说明 |
| :--- | :--- | :--- |
| 前端 | Nginx 静态托管 | `dist/` + SPA fallback + gzip + 长缓存 |
| 后端 | systemd 服务 | JAR 包，优雅停机，日志滚动（保留 30 天） |
| Nginx | 反向代理 | `/api` `/ws` `/uploads` 反代，安全头，登录限流 5r/m |
| MySQL / Redis | 独立进程 | 生产密码环境变量注入 |

::: details 部署工具链（deploy/ 目录）
- `01-首次部署/`：环境安装 + 应用部署 + 密钥生成
- `02-更新上线/`：本地构建 → scp 上传 → 健康检查 → **自动回滚**
- `03-日常维护/`：每日 03:00 数据库备份、测试数据清理
- `04-运行配置/`：nginx.conf（HTTP）/ nginx-https.conf（HTTPS 切换）/ systemd service
- `05-文档手册/`：部署上线指南、上线实战手册、日常运维手册（共 2247 行）
:::
