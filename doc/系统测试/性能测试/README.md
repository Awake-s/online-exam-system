# 在线考试系统 - 并发性能测试目录

> 本目录包含**完整的并发性能测试体系**：从方案设计、操作指南、自动化脚本，到执行日志、测试报告、论文稿。
>
> 本测试遵循 **GB/T 25000.51-2016** 国标 + **ISO/IEC 25010** 国际标 + **ISO/IEC/IEEE 29119-2** 测试流程标，引用 **ITU-T G.1010** + **Nielsen 1993** + **Akamai 2017** 性能阈值依据。

---

## 📊 核心实测结论（2026-04-29 17:40 执行）

| 指标 | 实测值 | 评级 |
|---|---|---|
| 总并发用户 | **143** | ✅ 超 50 设计目标 2.86 倍 |
| 总请求数 | **315** | - |
| 错误率 | **0.00 %** | ✅ 完美 |
| 整体 P95 | **147 ms** | ✅ 优于 ITU-T 2 秒标准 13.5 倍 |
| APDEX 评分 | **1.000** | ✅ Excellent 满分 |
| 业务闭环完成率 | **43/43 = 100%** | ✅ 全部贯通 |

---

## 🇨🇳 中文化 JMeter 可视化报告（NEW）

**官方英文 Dashboard 已替换为中文版本**（基于 mzky/jmeter5.x-cn-report-template，社区主流方案）。

| 报告版本 | 路径 | 用途 |
|---|---|---|
| **中文版** ⭐ | `D:\Tools\jmeter-results\report-cn-20260429_174032\index.html` | **答辩演示 + 论文截图** |
| 英文版（原始） | `D:\Tools\jmeter-results\report-20260429_174032\index.html` | 历史对照 |

**一键打开中文报告：**

```powershell
Start-Process "D:\Tools\jmeter-results\report-cn-20260429_174032\index.html"
```

**未来跑新压测后，一键生成中文报告：**

```powershell
& "d:\Java Projects\在线考试系统\doc\性能测试\03_PowerShell脚本\generate-cn-report.ps1"
```

**详细方案与故障排查：** `02_操作指南/JMeter中文报告生成指南.md`

---

## 📁 目录结构（完美分类）

```
doc/性能测试/
├── README.md                              ← 本文件（总索引）
│
├── 01_方案设计/                            # 测试规划与设计阶段
│   ├── 并发测试实施方案_v2.0.md            # ⭐ 主方案（当前版本）
│   └── Phase1_验收Checkpoint.md            # 环境验收记录
│
├── 02_操作指南/                            # 工具使用与报告解读
│   ├── JMeter安装与配置指南.md             # JMeter 5.6.3 安装步骤
│   ├── JMeter运行指南.md                   # 跑压测的标准流程
│   ├── JMeter中文报告生成指南.md           # 🇨🇳 HTML 报告中文化方案
│   ├── HTML报告中文解读手册.md             # JMeter Dashboard 中英对照
│   └── 测试执行全流程复盘.md               # ⭐ 6 个 Phase 完整执行轨迹
│
├── 03_PowerShell脚本/                      # 自动化执行脚本（5 个核心脚本）
│   ├── start-perf-backend.ps1              # 启动 perf 后端（动态查找项目根）
│   ├── verify-login.ps1                    # 登录可用性验证
│   ├── generate-csv.ps1                    # 生成 4 个 CSV 数据集
│   ├── install-cn-template.ps1             # 🇨🇳 安装 JMeter 中文模板
│   └── generate-cn-report.ps1              # 🇨🇳 生成中文 HTML 报告
│
├── 04_JMeter脚本与数据/                    # JMeter 主脚本与参数化数据
│   ├── exam-system-perf.jmx                # 主脚本（4 个 Thread Group）
│   ├── students.csv                        # TG1 学生池（100 行）
│   ├── students-exam.csv                   # TG2 考试中学生池（43 行 班级 4）
│   ├── teachers.csv                        # TG3 教师池（15 行）
│   └── chat-pairs.csv                      # TG4 聊天对池（30 行）
│
├── 05_数据库备份/                          # 压测前快照备份
│   └── online_exam_system_20260429_153933.sql
│
├── 06_执行日志/                            # 数据生成与校验日志（仅保留最终成功版）
│   ├── datagen_full_run.log                # 全量造数据成功日志（300 学生）
│   ├── dataverify_full.log                 # 数据校验通过日志
│   └── datacleanup_run2.log                # 数据清理最终成功日志
│
├── 07_测试报告与论文稿/                    # 答辩与论文交付物
│   ├── 压测报告.md                         # 完整压测报告（毕设附录）
│   └── 论文6.3节升级稿.md                  # ⭐ 论文 §6.3 直接复用稿
│
└── 99_历史档案/                            # 已废弃文件归档（不删，保溯源）
    ├── 01_方案设计_v1/                     # 早期方案（已被 v2.0 取代）
    ├── 03_PowerShell脚本_废弃/             # 中文版/硬编码版脚本（PS5 编码 bug）
    └── 06_执行日志_过程产物/               # 早期失败/重试日志
```

> **JMeter 实际输出报告位置**：`D:\Tools\jmeter-results\report-20260429_174032\index.html`

---

## 🔄 推荐使用流程（按编号顺序读）

### 第一次接触：先读 01

```
01_方案设计/并发测试实施方案_v2.0.md   ← 了解整体设计
```

### 准备执行：再读 02

```
02_操作指南/JMeter安装与配置指南.md     ← 安装环境
02_操作指南/JMeter运行指南.md          ← 跑压测的标准流程
```

### 执行压测：用 03 + 04

```
03_PowerShell脚本/start-perf-backend.ps1     ← 启动后端
03_PowerShell脚本/verify-login.ps1            ← 验证账号
03_PowerShell脚本/generate-csv.ps1            ← 生成 CSV（如需重新生成）
04_JMeter脚本与数据/exam-system-perf.jmx     ← JMeter 加载执行
```

### 看结果：参考 02 + 06 + 07

```
02_操作指南/HTML报告中文解读手册.md          ← 看懂 JMeter 英文 Dashboard
06_执行日志/*.log                            ← 历史执行记录
07_测试报告与论文稿/压测报告.md              ← 完整中文报告
07_测试报告与论文稿/论文6.3节升级稿.md       ← 论文直接抄
```

---

## 🎯 测试体系全景

```
┌─────────── 标准依据（5 大权威）───────────┐
│  GB/T 25000.51-2016    国标 SQuaRE          │
│  ISO/IEC 25010:2011    国际质量模型         │
│  ISO/IEC/IEEE 29119-2  国际测试流程         │
│  ITU-T G.1010          Web 响应阈值         │
│  Nielsen 1993          人机交互             │
└──────────────────────────────────────────┘
                       ↓
┌─────────── 6 阶段执行（Phase 0~6）─────────┐
│  P0: 方案设计     →  01_方案设计/           │
│  P1: 环境与备份   →  05_数据库备份/         │
│  P2-3: 造数据     →  06_执行日志/           │
│  P4: 编写脚本     →  03_/, 04_/             │
│  P5: 执行压测     →  D:\Tools\jmeter-results\     │
│  P6: 报告分析     →  07_测试报告与论文稿/   │
└──────────────────────────────────────────┘
                       ↓
┌─────────── 答辩交付（最终产物）───────────┐
│  📄 论文 §6.3 节                          │
│  📊 JMeter HTML 报告（截图素材）          │
│  📋 压测报告.md（附录）                   │
│  💻 现场可重跑（JMX + 后端）              │
└──────────────────────────────────────────┘
```

---

## ⚙️ 快速命令参考

### 启动后端（perf profile）

```powershell
& "d:\Java Projects\在线考试系统\doc\性能测试\03_PowerShell脚本\start-perf-backend.ps1"
```

### 跑 JMeter 压测（命令行）

```powershell
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$resultsDir = "D:\Tools\jmeter-results"
New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null

& "D:\Tools\apache-jmeter-5.6.3\bin\jmeter.bat" -n `
  -t "d:\Java Projects\在线考试系统\doc\性能测试\04_JMeter脚本与数据\exam-system-perf.jmx" `
  -l "$resultsDir\result-$ts.jtl" `
  -e -o "$resultsDir\report-$ts" `
  -j "$resultsDir\jmeter-$ts.log"
```

### 打开 HTML 报告

```powershell
Start-Process "D:\Tools\jmeter-results\report-20260429_174032\index.html"
```

### 清理 perf 测试数据

```powershell
cd "d:\Java Projects\在线考试系统\exam-system"
mvn test "-Dtest=DataCleanupTest" "-Dspring.profiles.active=perf"
```

---

## 📌 重要提示

1. **PowerShell 兼容性**：5 个脚本全部内部用 ASCII 字符（避开 PS5 中文路径解码 bug），同时兼容 PowerShell 5 / 7+。早期的「中文版」脚本已归档到 `99_历史档案/03_PowerShell脚本_废弃/`，仅作为历史参考。

2. **JMX 中的 CSV 路径**：JMX 文件中的 CSV 引用是**相对路径**，所以 JMX 必须从 `04_JMeter脚本与数据/` 目录加载，CSV 才能被 JMeter 正确读取。

3. **HTML 报告中文化（已落地 ✅）**：本项目已用 mzky 社区方案完成 JMeter HTML Dashboard 全中文化，5 页共 799 个中文字符。详见 `02_操作指南/JMeter中文报告生成指南.md`。

4. **未看清自动执行过程？** → 阅读 `02_操作指南/测试执行全流程复盘.md`，按时间线还原 6 个 Phase 的完整轨迹。

---

**测试执行人**：陶展  
**执行日期**：2026-04-29  
**测试结论**：✅ 系统在 143 并发用户下零错误、P95 < 150ms、APDEX 满分，**完全达成毕设论文设计目标**。
