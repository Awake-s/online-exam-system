# JMeter 性能压测运行指南

> 在线考试系统并发性能压测 v1.0  
> 适用：本科毕设《基于 SSM 的在线考试系统》

## 📋 前置条件

| 检查项 | 状态 |
|---|:-:|
| Phase 1 完成（perf 库已建、备份已完成） | ☐ |
| Phase 2 + 3 完成（DataGeneratorTest 跑过 full 版） | ☐ |
| Phase 3 verify 通过（300 学生 / 1500 题 / 20 考试） | ☐ |
| JMeter 5.6.3+ 已安装（见 `JMeter安装与配置指南.md`） | ☐ |
| MySQL 服务运行中（端口 3306） | ☐ |
| Redis 服务运行中（端口 6379） | ☐ |

## 🚀 操作流程（4 步）

### Step 1 — 启动 perf 后端

打开 **新 PowerShell 终端 1**，执行：

```powershell
cd "d:\Java Projects\在线考试系统\doc\性能测试\03_PowerShell脚本"
.\start-perf-backend.ps1
```

> ℹ️ 脚本内部纯 ASCII 字符，同时兼容 PowerShell 5 / 7+。会自动向上查找包含 `exam-system` 的项目根目录，无需 `cd` 到项目根。

✅ 等待看到日志：

```
Started ExamSystemApplication in xx.x seconds
```

⚠️ **保持此终端不关闭！** 后端必须持续运行。

---

### Step 2 — 验证登录可用性

打开 **新 PowerShell 终端 2**，执行：

```powershell
cd "d:\Java Projects\在线考试系统\doc\性能测试\03_PowerShell脚本"
.\verify-login.ps1
```

✅ 期望输出：

```
✅ 后端健康，状态：UP
✅ 管理员   perf_admin_01   → token=eyJxxxxx... (id=30 role=ADMIN)
✅ 教师     perf_tea_001    → token=eyJxxxxx... (id=31 role=TEACHER)
✅ 学生     perf_stu_001    → token=eyJxxxxx... (id=46 role=STUDENT)
✅ 学生     perf_stu_150    → token=eyJxxxxx... (id=195 role=STUDENT)
✅ 学生     perf_stu_300    → token=eyJxxxxx... (id=345 role=STUDENT)
登录验证结果：成功 5 / 失败 0
```

---

### Step 3 — 生成 CSV 参数化文件

仍在 **终端 2**，执行：

```powershell
.\generate-csv.ps1
```

✅ 期望输出 4 个文件：

```
students.csv       100 行  (TG1 登录基线)
teachers.csv        15 行  (TG3 教师查询)
students-exam.csv   50 行  (TG2 考试流程，含 examId)
chat-pairs.csv      30 行  (TG4 聊天压测)
```

---

### Step 4 — 启动 JMeter GUI 跑压测

打开 **新 PowerShell 终端 3**，执行：

```powershell
# 假设 JMeter 已加入 PATH，或使用完整路径
jmeter -t "d:\Java Projects\在线考试系统\doc\性能测试\04_JMeter脚本与数据\exam-system-perf.jmx"
```

或者直接双击 JMeter 安装目录的 `bin\jmeter.bat`，然后：

1. **菜单 File → Open** → 选择 `exam-system-perf.jmx`
2. 在左侧测试树中，按需启用/禁用 Thread Group：
   - ✅ **TG1-登录性能基线**（默认启用）
   - ✅ **TG2-学生考试全流程**（默认启用，**核心场景**）
   - ☐ TG3-教师查询性能（按需启用）
   - ☐ TG4-聊天消息压测（按需启用）
3. 点击工具栏 **绿色三角▶** 按钮启动测试
4. 在 **聚合报告（Aggregate Report）** 中实时查看结果

---

## 📊 关键性能指标（KPI）

| 接口 | 期望响应时间 | 期望吞吐率 | 期望错误率 |
|---|---|---|---|
| `POST /api/auth/login` | P50 < 500ms / P95 < 1500ms | ≥ 50 req/s | < 0.1% |
| `GET /api/student/exam/start` | P50 < 800ms / P95 < 2000ms | ≥ 30 req/s | < 1% |
| `POST /api/student/exam/submit` | P50 < 1200ms / P95 < 3000ms | ≥ 15 req/s | < 1% |
| `POST /api/chat/messages` | P50 < 400ms / P95 < 1000ms | ≥ 50 req/s | < 0.5% |

> 数据来源：v2.0 实施方案 §6 KPI 设计  
> 标准依据：GB/T 25000.51-2016（SQuaRE）性能效率特性

---

## 🛠 常用 JMeter 操作

### 命令行模式（推荐用于正式压测，节省 GUI 内存）

```powershell
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$resultsDir = "d:\Java Projects\在线考试系统\doc\性能测试\results"
New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null

jmeter -n `
  -t "d:\Java Projects\在线考试系统\doc\性能测试\04_JMeter脚本与数据\exam-system-perf.jmx" `
  -l "$resultsDir\result-$ts.jtl" `
  -e -o "$resultsDir\report-$ts" `
  -j "$resultsDir\jmeter-$ts.log"
```

参数说明：
- `-n` 非 GUI 模式
- `-t` 测试计划文件
- `-l` 结果 JTL 文件（可后续导入 GUI 查看）
- `-e -o` 自动生成 HTML 报告
- `-j` JMeter 日志文件

### 增大 JMeter 内存（高并发时必备）

编辑 `JMeter\bin\jmeter.bat`，找到：

```bat
set HEAP=-Xms1g -Xmx1g
```

修改为：

```bat
set HEAP=-Xms2g -Xmx4g
```

---

## 🚧 故障排查

| 症状 | 排查 |
|---|---|
| `Connection refused: localhost:8081` | 后端未启动 / 用 dev profile 起的（端口可能不同） |
| `code=401 Unauthorized` | token 提取失败，检查 Sampler 顺序与 JSON Extractor |
| `登录失败次数过多，请 X 分钟后再试` | 触发 LoginRateLimiter（5 次失败锁 15 分钟）。**确保密码正确**（Test@123456） |
| `Duplicate entry for key 'idx_exam_user'` | 同一学生重复进同一考试。删除 students-exam.csv 中已使用的学生，或重新生成 CSV |
| 大量 `OutOfMemoryError` | 调大 JMeter 堆内存 |
| 聚合报告全部 0% 错误率但 TPS 极低 | 后端瓶颈，查看 actuator/metrics 或 application.log |

---

## 🔬 测试策略建议（毕设论文关键）

按照 v2.0 §7 执行计划：

### 阶段 1：基线测试（建立指标基线）

- 启用 TG1-登录性能基线（100 并发）
- 记录稳态下：吞吐率、P50/P95/P99 响应时间、错误率
- 持续 5 分钟

### 阶段 2：业务闭环测试（核心场景）

- 启用 TG2-学生考试全流程（50 并发）
- 关注 5 个 Step 各自的延迟与失败率
- 持续 10 分钟

### 阶段 3：极限并发测试（找瓶颈）

- 同时启用 TG1 + TG2 + TG3 + TG4
- 总并发约 195 用户
- 持续 15 分钟，观察是否出现 SLA 降级

### 阶段 4：稳定性测试（可选）

- 启用 TG2，但将 Loop Count 改为 -1（无限）
- 持续运行 30~60 分钟
- 关注内存增长 / GC 频率 / DB 连接池占用

---

## 📁 文件清单

```
doc/性能测试/
├── README.md                                # 总索引
├── 01_方案设计/
│   ├── 并发测试实施方案_v2.0.md            # 主方案文档
│   └── Phase1_验收Checkpoint.md
├── 02_操作指南/
│   ├── JMeter安装与配置指南.md
│   ├── JMeter运行指南.md                   # 本文件
│   ├── JMeter中文报告生成指南.md           # 🇨🇳 HTML 报告中文化
│   ├── HTML报告中文解读手册.md
│   └── 测试执行全流程复盘.md               # ⭐ 6 个 Phase 执行轨迹
├── 03_PowerShell脚本/                      # 5 个核心脚本（全部内部 ASCII）
│   ├── start-perf-backend.ps1              # 启动 perf 后端（动态查找项目根）
│   ├── verify-login.ps1                    # 登录验证
│   ├── generate-csv.ps1                    # CSV 生成
│   ├── install-cn-template.ps1             # 🇨🇳 安装中文模板
│   └── generate-cn-report.ps1              # 🇨🇳 生成中文报告
├── 04_JMeter脚本与数据/
│   ├── exam-system-perf.jmx                # JMeter 主脚本（4 个 Thread Group）
│   ├── students.csv                        # TG1 学生池
│   ├── students-exam.csv                   # TG2 考试中学生池
│   ├── teachers.csv                        # TG3 教师池
│   └── chat-pairs.csv                      # TG4 聊天对池
├── 05_数据库备份/
│   └── online_exam_system_*.sql            # MySQL dump
├── 06_执行日志/                            # 仅保留最终成功版
│   ├── datagen_full_run.log
│   ├── dataverify_full.log
│   └── datacleanup_run2.log
├── 07_测试报告与论文稿/
│   ├── 压测报告.md                          # 主报告
│   └── 论文6.3节升级稿.md                   # 论文直接复用
└── 99_历史档案/                            # 废弃文件归档（可溯源）
    ├── 01_方案设计_v1/                     # v1.0 方案
    ├── 03_PowerShell脚本_废弃/             # 中文版脚本（PS5 bug）
    └── 06_执行日志_过程产物/               # 早期失败重试日志

JMeter 输出（独立于项目之外）：
D:\Tools\jmeter-results/
├── report-*/index.html        # JMeter HTML Dashboard（英文原版）
└── report-cn-*/index.html     # JMeter HTML Dashboard（中文化版本 ⭐）
```
