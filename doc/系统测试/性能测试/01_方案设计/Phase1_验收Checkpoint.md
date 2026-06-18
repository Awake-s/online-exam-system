# Phase 1 验收 Checkpoint

> **执行日期**：2026-04-29
> **耗时**：~30 分钟（预期 30 分钟，实际 ~5 分钟，AI 自动化执行）
> **状态**：✅ 6/7 项完成，⏳ 1 项需用户手动操作（JMeter 下载）

---

## ✅ 已完成项

### 1.1 mysqldump 全库备份（防线 1）

```
✅ 备份文件：d:\Java Projects\在线考试系统\doc\性能测试\backup\online_exam_system_20260429_153933.sql
📦 文件大小：76.89 KB
🔧 备份命令：cmd /c "mysqldump -u root -p12345678 --default-character-set=utf8mb4 --single-transaction --routines --triggers online_exam_system > backup.sql"
```

**回退命令（任何时候可用）**：
```powershell
$latest = Get-ChildItem "d:\Java Projects\在线考试系统\doc\性能测试\backup\*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
cmd /c "mysql -u root -p12345678 --default-character-set=utf8mb4 online_exam_system < `"$($latest.FullName)`""
```

### 1.2 创建独立 schema（防线 3）

```
✅ schema：online_exam_system_perf
🔧 命令：CREATE DATABASE online_exam_system_perf DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
🛡️ 隔离效果：开发库 online_exam_system 完全不动，所有压测在 perf 库进行
```

### 1.3 复制结构 + 基础数据到 perf 库

| 验证项 | 期望 | 实测 | 通过 |
|---|---:|---:|:-:|
| 表数量 | 19 | 19 | ✅ |
| sys_user | 9 | 9 | ✅ |
| edu_class | 6 | 6 | ✅ |
| edu_subject | 8 | 8 | ✅ |
| exam_question | 179 | 179 | ✅ |
| sys_role | 3 | 3 | ✅ |

### 1.4 创建 application-perf.yml

```
✅ 路径：exam-system/src/main/resources/application-perf.yml
✅ 数据库切换：jdbc:mysql://localhost:3306/online_exam_system_perf
✅ Redis 隔离：database: 1（开发用 db0 不动）
✅ HikariCP 池：50（dev 默认 10）
✅ MyBatis SQL 日志：关闭（避免压测拖慢）
✅ Actuator：扩展暴露 metrics/httptrace/threaddump/heapdump
```

**启用方式**：
```powershell
cd exam-system
mvn spring-boot:run "-Dspring-boot.run.profiles=perf"
# 或
mvn test "-Dspring.profiles.active=perf"
```

### 1.5 pom.xml 增加 Datafaker 1.9.0

```
✅ 依赖坐标：net.datafaker:datafaker:1.9.0:test
✅ 验证：mvn dependency:tree '-Dincludes=net.datafaker' → BUILD SUCCESS
✅ scope：test（不污染生产 jar）
```

### 1.7 Phase 1 验收 Checkpoint 文档（**本文档**）

✅ 已生成。

---

## ⏳ 待用户手动操作项

### 1.6 JMeter 5.6.3 + WebSocket 插件下载

**原因**：JMeter zip 文件 ~80MB，需用户联网下载。

**指南**：详见 `@d:/Java Projects/在线考试系统/doc/性能测试/JMeter安装与配置指南.md`

**简化步骤**：
1. 下载 https://jmeter.apache.org/download_jmeter.cgi → `apache-jmeter-5.6.3.zip`
2. 解压到 `D:\Tools\apache-jmeter-5.6.3\`
3. 下载 https://github.com/Luminis-Arnhem/jmeter-websocket-samplers/releases → `JMeterWebSocketSamplers-x.x.x.jar`
4. 将 jar 放入 `D:\Tools\apache-jmeter-5.6.3\lib\ext\`
5. 修改 `bin/jmeter.bat` 中堆内存 1g→4g
6. 命令行验证：`D:\Tools\apache-jmeter-5.6.3\bin\jmeter.bat --version`

完成后回复"JMeter 已就绪"。

> 💡 **提示**：JMeter 下载和 Phase 2 造数据脚本开发可以**并行**进行——Phase 2 不依赖 JMeter，可以在用户下载 JMeter 的同时启动。

---

## 🛡️ 三道安全防线现状

| 防线 | 内容 | 状态 |
|---|---|:-:|
| 1️⃣ | mysqldump 全库备份 | ✅ 已备份（76.89 KB） |
| 2️⃣ | 测试数据加 `perf_` 前缀 | ✅ 设计就绪（Phase 2 实现）|
| 3️⃣ | 独立 schema `online_exam_system_perf` | ✅ 已创建并同步基础数据 |

**任何时刻的应急回退**：

```powershell
# 方式 A：直接删除 perf schema（最快，最稳）
mysql -u root -p12345678 -e "DROP DATABASE online_exam_system_perf;"

# 方式 B：恢复开发库到压测前快照（万一压测意外影响了 dev 库）
$latest = Get-ChildItem "d:\Java Projects\在线考试系统\doc\性能测试\backup\*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
cmd /c "mysql -u root -p12345678 --default-character-set=utf8mb4 online_exam_system < `"$($latest.FullName)`""

# 方式 C：清理 application-perf.yml + 移除 pom.xml Datafaker（撤销所有 Phase 1 改动）
Remove-Item "exam-system\src\main\resources\application-perf.yml"
# 然后手动 git checkout -- exam-system/pom.xml
```

---

## 📊 当前数据规模（perf 库）

| 表 | 当前 | Phase 3 后目标 |
|---|---:|---:|
| sys_user | 9 | 317（admin 2 + tea 15 + stu 300）|
| edu_class | 6 | 8 |
| edu_subject | 8 | 15 |
| exam_question | 179 | 1500 |
| exam_paper | 0 | 30 |
| exam_exam | 0 | 20 |
| exam_record | 0 | 1500 |
| exam_answer | 0 | 37500 |
| chat_conversation | 0 | 200 |
| chat_message | 0 | 5000 |
| sys_notification | 0 | 1000 |

---

## 下一步：Phase 2 造数据脚本开发

**预计耗时**：2 小时
**主要产物**：`exam-system/src/test/java/com/exam/perf/` 下 7 个 Java 文件

**可立即开始**（不依赖 JMeter）。

---

**Phase 1 验收完成 ✅**
