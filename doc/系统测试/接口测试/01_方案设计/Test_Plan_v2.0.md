# Test Plan v2.0 — 在线考试系统接口测试

> **标准依据**: ISO/IEC/IEEE 29119-3:2021 §7.2 Test Plan  
> **版本**: v2.0  
> **作者**: 陶展  
> **修订**: 2026-04-30  
> **状态**: 已执行 ✅（145 用例 / 216 断言 / 100% 通过）

---

## 一、测试上下文 (Context of Testing)

### 1.1 项目背景

本系统是以高校考试为业务原型的在线考试管理系统（个人技术实践项目），采用 Spring Boot 2.7 + Vue 3 + MySQL 8 + Redis 7 技术栈，提供考试发布、学生答题、教师阅卷、成绩管理、错题本、AI 助手等核心功能。

### 1.2 测试目标

依据 ISO/IEC 25010:2011 软件质量模型，对系统 RESTful API 层进行全面验证：

| 质量维度 | 验证内容 | 预期 |
|---|---|---|
| 功能适合性 | HTTP 状态码、响应字段、业务逻辑正确性 | 100% 符合源码设计 |
| 可靠性 | 异常处理路径、错误恢复 | 全部异常分支可达 |
| 安全性 | 鉴权、越权、参数校验 | OWASP API Top 10 覆盖 ≥ 7/10 |
| 性能效率 | 接口响应时间 | 平均 < 200 ms |

### 1.3 测试范围

**包含**:
- ✅ 全部 20 个 Controller / 108 个接口
- ✅ JWT 鉴权与 RBAC 角色越权
- ✅ 参数校验与业务异常
- ✅ 跨用户数据隔离

**不包含**:
- ❌ 前端 UI 测试（属于系统测试章节）
- ❌ 数据库性能测试（属于性能测试章节）
- ❌ 第三方 API 调用安全（项目无此场景）
- ❌ 网络层 DDoS / WAF 测试

---

## 二、假设与约束 (Assumptions and Constraints)

### 2.1 假设

| # | 假设 | 影响 |
|---|---|---|
| 1 | 后端服务以 dev profile 启动并监听 localhost:8081 | 无法启动则测试无法执行 |
| 2 | MySQL 8 / Redis 7 已就绪并完成初始化 | 数据存储依赖 |
| 3 | 测试账号 `it_admin / it_teacher / it_student` 已存在 | 鉴权前置 |
| 4 | Apifox v2.8.26+ / Newman v6.2.2+ 已安装 | 工具链可用 |

### 2.2 约束

| # | 约束 | 应对 |
|---|---|---|
| 1 | 测试不能污染生产业务数据 | 使用专用 it_* 测试账号 + cleanup-test-data.sql |
| 2 | 测试集必须可在 CI/CD 中无人值守执行 | 全部断言自动化，无手动验证 |
| 3 | 测试结果必须有第三方可复现证据 | 双工具（Apifox + Newman）交叉验证 |
| 4 | 答辩展示需 < 15 秒完成全量回归 | Newman 实测 13.1 秒 ✅ |

---

## 三、干系人 (Stakeholders)

| 角色 | 姓名 / 来源 | 职责 |
|---|---|---|
| 测试工程师（执行）| 陶展 | 测试用例设计、执行、报告 |
| 系统开发（被测）| 陶展 | 修复测试中发现的缺陷 |
| 项目验收方 | 答辩老师团队 | 接受测试结果作为质量依据 |
| 测试工具供应商 | Apifox 官方 / Postman 官方 | 工具支持、文档参考 |
| 安全标准提供方 | OWASP / ISO / IEEE / ISTQB | 测试方法论与标准依据 |

---

## 四、测试沟通 (Testing Communication)

| 沟通对象 | 形式 | 频率 | 内容 |
|---|---|---|---|
| 个人记录 | Markdown 文档 | 持续更新 | 测试用例、发现问题、修复方案 |
| 自检审计 | PowerShell 脚本输出 | 每次提交前 | 自动化审计 / 覆盖率报告 |
| 答辩展示 | PPT + 现场演示 | 答辩当日 | 测试结果、关键技术、创新点 |
| 论文章节 | 4.4 章节 | 终稿 | 测试方法、数据、结论 |

---

## 五、风险登记 (Risk Register)

> ISO/IEC/IEEE 29119-3:2021 强制要求

### 5.1 项目风险（影响测试能否完成）

| ID | 风险描述 | 概率 | 严重度 | 缓解措施 | 状态 |
|---|---|---|---|---|---|
| PR-1 | 后端服务启动失败 | 低 | 高 | 提前验证 secrets.env 与 application.yml | ✅ 已缓解 |
| PR-2 | MySQL/Redis 服务不可用 | 低 | 高 | docker-compose 一键启动备份方案 | ✅ 已缓解 |
| PR-3 | Apifox 与 Newman 报告不一致 | 中 | 中 | 双工具独立验证 + 差异分析文档 | ✅ 已识别并解决 |
| PR-4 | 测试账号被误删 | 低 | 中 | 提供 init-test-users.sql 一键重建 | ✅ 已缓解 |
| PR-5 | Token 过期导致中途失败 | 低 | 低 | 测试集 Setup 阶段重新登录获取新 Token | ✅ 已缓解 |

### 5.2 产品风险（影响测试发现质量问题）

| ID | 风险描述 | 概率 | 严重度 | 缓解措施 | 状态 |
|---|---|---|---|---|---|
| QR-1 | 鉴权机制存在绕过漏洞 | 低 | **高** | 108 个反向用例全覆盖 401 路径 | ✅ 已验证无漏洞 |
| QR-2 | 越权访问他人数据 (BOLA) | 中 | **高** | 5+ 个 BOLA 用例 | ✅ 已验证 |
| QR-3 | 参数校验绕过 | 中 | 中 | @Valid + 5 个反向参数用例 | ✅ 已验证 |
| QR-4 | 业务异常未被妥善处理 | 中 | 中 | GlobalExceptionHandler 统一封装 + 用例验证 | ✅ 已验证 |
| QR-5 | 文件上传超大导致 OOM | 低 | 中 | SpeechController 10MB 限制 + 用例验证 | ✅ 已验证 |
| QR-6 | 暴力破解登录 | 中 | 中 | LoginRateLimiter 5次/15分钟 | ✅ 已验证 |
| QR-7 | JWT 重放攻击 | 低 | 中 | （v1.7 改进项，引入 JTI 黑名单） | ⚠️ 遗留 |
| QR-8 | SQL 注入 | 低 | **高** | MyBatis-Plus 参数化查询 | ✅ ORM 自动防护 |

### 5.3 残留风险（接受 / 转移到下个版本）

| ID | 风险描述 | 处置 |
|---|---|---|
| RR-1 | 性能压力测试缺失 | 接受（毕设范围外，推荐 JMeter）|
| RR-2 | 状态迁移测试未覆盖 | 转移到 v1.7 |
| RR-3 | CI/CD 集成未实施 | 转移到 v1.7 |

---

## 六、测试策略 (Test Strategy)

### 6.1 测试层级

```
本测试 (4.4 接口测试) 仅聚焦 API 层
       ↓
其他测试层级（不在本范围）：
- 4.1 单元测试（JUnit）
- 4.2 集成测试（@SpringBootTest）
- 4.3 数据库测试
- 4.5 系统功能测试（Selenium UI）
- 4.6 性能测试（JMeter）
```

### 6.2 测试类型

| 类型 | 是否实施 | 占比 |
|---|---|---|
| 功能测试 | ✅ | 100% |
| 鉴权测试 | ✅ | 含在功能内（74.5%）|
| 边界测试 | ✅ 部分 | ~10% |
| 异常测试 | ✅ | ~10% |
| 性能测试（响应时间）| ✅（顺带）| — |
| 压力测试 | ❌ | — |
| 安全渗透测试 | ❌ | — |

### 6.3 测试设计技术（详见《测试设计技术说明》）

| 技术 | 应用 |
|---|---|
| 等价类划分 | ✅ 角色、Token、参数 |
| 边界值分析 | ✅ 分页、文件大小、限流 |
| 错误推测 | ✅ 84.8% 反向用例 |
| 用例测试 | ✅ Setup/Cleanup 三段式 |

### 6.4 入口准则 (Entry Criteria)

- [x] 后端服务可启动并响应 `/actuator/health`
- [x] 数据库已初始化业务数据
- [x] 测试账号已存在
- [x] 测试集 collection 已通过 audit-collection.ps1 审计

### 6.5 出口准则 (Exit Criteria)

- [x] 全部 145 用例已执行
- [x] 断言通过率 ≥ 95%（实际 100%）
- [x] 接口覆盖率 ≥ 90%（实际 100%）
- [x] 严重缺陷数 = 0
- [x] 双工具结果一致

**所有出口准则已达成 ✅**

---

## 七、测试活动与估时 (Testing Activities and Estimates)

| 阶段 | 活动 | 估时 | 实际 | 偏差 |
|---|---|---|---|---|
| 1. 准备 | 后端 Controller 扫描、接口画像生成 | 2h | 2.5h | +25% |
| 2. 设计 | 145 用例设计 + 216 断言编写 | 8h | 10h | +25% |
| 3. 执行 | 双工具运行 + 验证 | 1h | 0.5h | -50% |
| 4. 调试 | Apifox 契约测试问题排查 | 0h | 1h | +∞ |
| 5. 报告 | 论文章节 + 全部测试文档 | 4h | 6h | +50% |
| 6. 审计 | 自动化审计 + 覆盖率分析 | 1h | 1h | 0% |
| **合计** | — | **16h** | **21h** | +31% |

---

## 八、人员与资源 (Staffing)

| 角色 | 人员 | 投入 |
|---|---|---|
| 测试工程师 | 陶展（一人）| 21 小时 |
| 工具采购 | Apifox 免费版 + Newman 开源 | $0 |
| 硬件 | 个人电脑 Windows 10 | — |
| 软件许可 | 全部开源 / 免费 | $0 |

---

## 九、测试数据需求 (Test Data Requirements)

> ISO/IEC/IEEE 29119-3:2021 §8.2.4

### 9.1 数据分类

| 类别 | 来源 | 隔离方式 |
|---|---|---|
| 测试用户 | `init-test-users.sql` | 用户名前缀 `it_` |
| 测试试卷 | 业务接口动态创建 | 名称含 `[INT-TEST]` 标记 |
| 测试题目 | 业务接口动态创建 | 关联到测试试卷 |
| 测试考试 | 业务接口动态创建 | 关联到测试试卷 |

### 9.2 数据生命周期

```
init-test-users.sql (创建)
       ↓
Setup 阶段 (登录获取 Token)
       ↓
业务测试阶段 (创建/读/写/删 数据)
       ↓
Cleanup 阶段 (登出)
       ↓
cleanup-test-data.sql (按 it_* 前缀清理)
```

### 9.3 测试数据就绪状态

| 数据集 | 文件 | 是否就绪 |
|---|---|---|
| 测试用户初始化 SQL | `05_测试数据/init-test-users.sql` | ✅ |
| 测试数据清理 SQL | `05_测试数据/cleanup-test-data.sql` | ✅ |
| 业务种子数据 | `exam-system/sql/online_exam_system.sql` | ✅ |

### 9.4 数据敏感性

- ✅ 测试集中的密码均为非生产值（`Admin@2026 / Teacher@2026 / Student@2026`）
- ✅ 不包含任何真实用户个人信息（PII）
- ✅ 不包含任何 API 密钥（已使用 `secrets.env` 隔离）

---

## 十、测试环境需求 (Test Environment Requirements)

> ISO/IEC/IEEE 29119-3:2021 §8.2.5

### 10.1 硬件要求

| 资源 | 最小值 | 推荐值 |
|---|---|---|
| CPU | 2 核 | 4 核+ |
| 内存 | 4 GB | 8 GB+ |
| 磁盘 | 5 GB 可用 | 20 GB+ |
| 网络 | 本地回环（localhost）| — |

### 10.2 软件要求

| 软件 | 版本 | 用途 | 是否必需 |
|---|---|---|---|
| OS | Windows 10/11 x64（或 Linux）| 运行环境 | ✅ |
| JDK | OpenJDK 17 | 后端运行时 | ✅ |
| Maven | 3.8+ | 后端构建 | ✅ |
| Node.js | 18+ | 前端 / Newman | ✅ |
| MySQL | 8.0 | 业务数据存储 | ✅ |
| Redis | 7.0 | 缓存/会话 | ✅ |
| Apifox | v2.8.26+ | GUI 测试 | ✅ |
| Newman | v6.2.2 | CLI 测试 | ✅ |
| newman-reporter-htmlextra | v1.23.1 | HTML 报告 | ✅ |
| PowerShell | 5.1+ | 自动化辅助 | ✅ |
| Git | 任意版本 | 版本控制 | 推荐 |

### 10.3 网络要求

| 项目 | 配置 |
|---|---|
| Backend 监听 | localhost:8081 |
| Frontend 监听 | localhost:5173（不在本测试范围）|
| MySQL 端口 | 3306 |
| Redis 端口 | 6379 |
| 防火墙 | 测试期间允许 localhost 全部端口 |

### 10.4 环境变量与配置

| 配置项 | 值 | 来源 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | secrets.env |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/online_exam_system?...` | secrets.env |
| `SPRING_DATA_REDIS_HOST` | `localhost` | secrets.env |
| `JWT_SECRET` | （从 secrets.env 读取） | secrets.env |
| Apifox 环境名 | `exam-system-dev` | postman_environment.json |
| `baseUrl` | `http://localhost:8081` | postman_environment.json |

### 10.5 测试环境就绪检查清单 (Readiness Report)

| # | 检查项 | 检查方式 | 状态 |
|---|---|---|---|
| 1 | 后端服务运行 | `curl http://localhost:8081/actuator/health` | ✅ |
| 2 | MySQL 可连接 | `mysql -u root -e "SELECT 1"` | ✅ |
| 3 | Redis 可连接 | `redis-cli ping` | ✅ |
| 4 | 测试账号存在 | `SELECT username FROM user WHERE username LIKE 'it_%'` | ✅ |
| 5 | Apifox 已安装并加载 collection | 手动检查 | ✅ |
| 6 | Newman 全局安装 | `newman --version` 应输出 6.x | ✅ |
| 7 | newman-reporter-htmlextra 已安装 | `npm list -g newman-reporter-htmlextra` | ✅ |
| 8 | Apifox 项目已关闭"自动化测试步骤校验响应" | 手动检查 | ✅（修复指南执行后） |
| 9 | 防火墙允许 localhost:8081 | `Test-NetConnection localhost -Port 8081` | ✅ |
| 10 | 磁盘空间充足（≥ 1 GB） | `Get-PSDrive D` | ✅ |

**环境就绪度**: 10/10 = 100% ✅

---

## 十一、测试交付物清单 (Deliverables)

详见 `接口测试报告_v1.6.md` §八

---

**文档作者**: 陶展 | **版本**: v2.0 | **修订**: 2026-04-30  
**符合标准**: ISO/IEC/IEEE 29119-3:2021 §7.2 Test Plan Template
