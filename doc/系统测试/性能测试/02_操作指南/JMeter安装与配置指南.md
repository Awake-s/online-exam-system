# JMeter 5.6.3 安装与配置指南

> **目的**：为 Phase 4~5 的压测做准备，安装最新稳定版 JMeter 并集成 WebSocket 插件
> **预计耗时**：10~15 分钟
> **执行人**：用户（需联网下载）

---

## 1. JMeter 主程序下载

### 1.1 官方下载（首选，最稳）

访问 Apache JMeter 官网：

🔗 **https://jmeter.apache.org/download_jmeter.cgi**

下载文件：**`apache-jmeter-5.6.3.zip`**（约 80MB）

> Windows 用户必选 zip 版本，**不要**下 tgz。

### 1.2 国内镜像（如果官方慢）

清华 TUNA 镜像：
🔗 **https://mirrors.tuna.tsinghua.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.zip**

### 1.3 推荐安装目录

```
D:\Tools\apache-jmeter-5.6.3\
```

> ⚠ 路径**不要含中文或空格**，否则 JMeter 启动会有诡异错误。

---

## 2. WebSocket 插件安装（用于场景 S6）

JMeter 原生**不支持** WebSocket 协议测试，需手动安装插件。

### 2.1 推荐插件：jmeter-websocket-samplers（Luminis-Arnhem 维护）

#### 下载链接

🔗 **https://github.com/Luminis-Arnhem/jmeter-websocket-samplers/releases**

下载最新版 jar：**`JMeterWebSocketSamplers-x.x.x.jar`**（约 1MB）

> 截至 2024 年最新稳定版为 **1.2.10**，支持 STOMP 协议（与本系统 Spring WebSocket STOMP 兼容）。

### 2.2 安装方式

将下载的 jar **放到** JMeter 的 `lib/ext/` 目录：

```
D:\Tools\apache-jmeter-5.6.3\lib\ext\JMeterWebSocketSamplers-1.2.10.jar
```

重启 JMeter 后，`Add > Sampler` 菜单底部应能看到 `WebSocket request-response Sampler` 等多个新选项。

---

## 3. 内存配置（**重要**）

JMeter 默认堆内存仅 1GB，压测 200 并发会 OOM。修改启动配置：

### 3.1 修改 `bin/jmeter.bat`

打开 `D:\Tools\apache-jmeter-5.6.3\bin\jmeter.bat`，找到这一行：

```bat
set HEAP=-Xms1g -Xmx1g -XX:MaxMetaspaceSize=256m
```

**改为**（推荐 4GB，本机 16GB 内存足够）：

```bat
set HEAP=-Xms2g -Xmx4g -XX:MaxMetaspaceSize=512m
```

### 3.2 启用最新 G1 GC（可选优化）

在同文件找到 `JVM_ARGS` 设置，追加：

```bat
set JVM_ARGS=-XX:+UseG1GC -XX:MaxGCPauseMillis=100
```

---

## 4. 命令行执行配置（**推荐压测用**）

### 4.1 关键文件：`bin/user.properties`

打开 `D:\Tools\apache-jmeter-5.6.3\bin\user.properties`，**追加以下内容**：

```properties
# ===== v2.0 在线考试系统压测专用配置 =====

# 1. 减少 Listener 内存占用（不在 GUI 看，只跑命令行）
jmeter.save.saveservice.output_format=csv
jmeter.save.saveservice.response_data=false
jmeter.save.saveservice.samplerData=false
jmeter.save.saveservice.requestHeaders=false
jmeter.save.saveservice.responseHeaders=false

# 2. 启用 P95/P99 等高精度百分位（默认是 LEGACY 估算器）
backend_metrics_percentile_estimator=R_3

# 3. HTTP 默认连接超时（避免死链）
httpclient.timeout=10000

# 4. 启用 SSL 重用（提升压测吞吐）
hc.parameters.file=hc.parameters

# 5. CSV 编码（中文数据用 UTF-8）
jmeter.save.saveservice.default_delimiter=,
jmeter.save.saveservice.thread_counts=true
file_format.csv_encoding=UTF-8
```

---

## 5. 验证安装

### 5.1 命令行版本检查

打开 PowerShell 或 cmd，进入 `D:\Tools\apache-jmeter-5.6.3\bin`：

```powershell
.\jmeter.bat --version
```

**期望输出**：
```
    _    ____   _    ____ _   _ _____
   / \  |  _ \ / \  / ___| | | | ____|
  / _ \ | |_) / _ \| |   | |_| |  _|
 / ___ \|  __/ ___ \ |___|  _  | |___
/_/   \_\_| /_/   \_\____|_| |_|_____|
                                      5.6.3
```

### 5.2 WebSocket 插件验证

启动 GUI（仅验证用）：
```powershell
.\jmeter.bat
```

GUI 中右键空白测试计划 → `Add` → `Threads (Users)` → `Thread Group` → 右键线程组 → `Add` → `Sampler` → 滚动到底部，应能看到：

- `WebSocket request-response Sampler`
- `WebSocket Single Read Sampler`
- `WebSocket Single Write Sampler`
- `WebSocket Close Sampler`
- `WebSocket Open Connection Sampler`
- `WebSocket Ping/Pong Sampler`

看到这 6 个就证明插件已正确安装。

### 5.3 加入 PATH（可选，便于命令行执行）

把 `D:\Tools\apache-jmeter-5.6.3\bin` 加到系统 PATH，重启 PowerShell 后可直接：

```powershell
jmeter --version
```

---

## 6. 常见问题

### Q1: 启动闪退

**A**: 99% 概率是 Java 版本问题。JMeter 5.6.3 需要 **Java 8+**。本项目已用 Java 11，没问题。检查：
```powershell
java -version
```

### Q2: GUI 卡顿严重

**A**: GUI 仅用于编辑脚本和查看结果，**正式压测必须用命令行**：
```powershell
jmeter -n -t 脚本.jmx -l 结果.jtl -e -o dashboard
```

### Q3: WebSocket 插件菜单看不到

**A**: 检查 jar 是否放对位置（`lib/ext/`，**不是** `lib/`）。如果还看不到，重启 JMeter。

### Q4: 中文乱码

**A**: GUI 字体设置：JMeter > Options > Look and Feel > **Darcula** 或在 `jmeter.properties` 加：
```properties
jmeter.hidpi.mode=true
jmeter.hidpi.scale.factor=1.0
```

---

## 7. 完成 Checkpoint

下载安装完成后，请在 PowerShell 执行以下命令，将输出截图给我：

```powershell
$jmeter = "D:\Tools\apache-jmeter-5.6.3\bin\jmeter.bat"
& $jmeter --version
Write-Host "---"
Get-ChildItem "D:\Tools\apache-jmeter-5.6.3\lib\ext\" -Filter "*WebSocket*" | Select-Object Name, Length
```

预期看到：
- JMeter Logo + 版本号 5.6.3
- WebSocket Samplers jar 文件名 + 大小

---

**完成后回复"JMeter 已就绪"即可进入 Phase 2：造数据脚本开发**
