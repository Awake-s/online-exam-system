# Newman 命令行指南

> Postman 官方命令行运行器，用于自动化批量执行 Collection。  
> 论文「技术详过」章节可引用本文。

---

## 1. 什么是 Newman？

**Newman** 是 Postman **官方**提供的命令行工具（npm 包），允许用户在终端 / CI/CD 流水线中直接执行 Postman Collection 文件，无需打开 GUI。

| 维度 | Postman GUI | Newman CLI |
|---|---|---|
| 适用场景 | 设计接口、单接口调试 | 批量执行、CI 集成、定时任务 |
| 执行速度 | 慢（GUI 渲染） | 快（无 UI 开销） |
| 报告 | 内置统计页 | 多格式导出（HTML / JSON / XML / CLI） |
| 可编程 | 否 | 是（PowerShell / Bash 脚本） |

---

## 2. 安装

```powershell
# 全局安装（已完成）
npm install -g newman newman-reporter-htmlextra

# 验证版本
newman --version          # 6.2.2
```

**全局位置**: `D:\JavaEEDev\node-v22.18.0-win-x64\node_global`

---

## 3. 核心命令

### 3.1 最简形式

```powershell
newman run exam-system.postman_collection.json
```

### 3.2 加 Environment

```powershell
newman run exam-system.postman_collection.json `
  -e exam-system.postman_environment.json
```

### 3.3 指定多个 Reporter

```powershell
newman run exam-system.postman_collection.json `
  -e exam-system.postman_environment.json `
  -r htmlextra,cli,json `
  --reporter-htmlextra-export "report.html" `
  --reporter-json-export "newman.json"
```

---

## 4. 关键参数详解

| 参数 | 说明 | 我们的值 |
|---|---|---|
| `run` | 子命令：运行 Collection | (必填) |
| `-e` / `--environment` | 环境变量文件 | `exam-system.postman_environment.json` |
| `-r` / `--reporters` | 报告器列表（逗号分隔） | `htmlextra,cli,json` |
| `--bail` | 遇到失败立即退出 | 不用（论文要看完整失败信息） |
| `-n` / `--iteration-count` | 迭代次数（数据驱动） | 1 |
| `--timeout` | 全局超时 (ms) | 默认（无限） |
| `--timeout-request` | 单请求超时 | 5000 (5s) |
| `--delay-request` | 请求间延迟 (ms) | 100 |
| `--color on/off` | 终端彩色输出 | on |

---

## 5. htmlextra Reporter 关键选项

| 参数 | 说明 |
|---|---|
| `--reporter-htmlextra-export <path>` | HTML 报告输出路径 |
| `--reporter-htmlextra-darkTheme` | 深色主题（论文截图友好） |
| `--reporter-htmlextra-title <text>` | 报告标题 |
| `--reporter-htmlextra-browserTitle <text>` | 浏览器标签标题 |
| `--reporter-htmlextra-showOnlyFails` | 只显示失败项（false 推荐） |
| `--reporter-htmlextra-showFolderDescription` | 显示文件夹描述 |
| `--reporter-htmlextra-timezone <tz>` | 时区（`Asia/Shanghai`） |

---

## 6. 完整一键命令（项目实际使用）

```powershell
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
newman run "doc\接口测试\04_Postman脚本与数据\exam-system.postman_collection.json" `
  -e "doc\接口测试\04_Postman脚本与数据\exam-system.postman_environment.json" `
  -r htmlextra,cli,json `
  --reporter-htmlextra-export "D:\Tools\接口测试结果\report-$ts.html" `
  --reporter-htmlextra-darkTheme `
  --reporter-htmlextra-title "在线考试系统 · 接口测试报告" `
  --reporter-htmlextra-timezone "Asia/Shanghai" `
  --reporter-json-export "D:\Tools\接口测试结果\newman-$ts.json"
```

**封装为脚本**：`doc\接口测试\03_PowerShell脚本\run-newman-test.ps1`

---

## 7. 退出码 (Exit Code)

| Code | 含义 |
|---|---|
| 0 | 所有断言通过 |
| 1 | 至少一个断言失败 |
| 2 | Collection 文件错误（解析失败） |

---

## 8. CI/CD 集成示例

### 8.1 Jenkins Pipeline

```groovy
stage('API Tests') {
    steps {
        sh 'newman run exam-system.postman_collection.json -e exam-system.postman_environment.json -r htmlextra'
    }
    post {
        always { archiveArtifacts artifacts: '*.html' }
    }
}
```

### 8.2 GitLab CI

```yaml
api-test:
  image: postman/newman:alpine
  script:
    - newman run exam-system.postman_collection.json -e exam-system.postman_environment.json -r junit
  artifacts:
    reports:
      junit: newman/*.xml
```

---

## 9. 故障排查

| 现象 | 原因 | 解决 |
|---|---|---|
| `command not found: newman` | npm 全局路径未加入 PATH | `npm config get prefix` 查路径，加到 PATH |
| `ECONNREFUSED localhost:8081` | 后端未启动 | 先 `mvn spring-boot:run` |
| `Collection schema invalid` | JSON 文件损坏 | 重新导出 Collection |
| HTML 报告中文乱码 | 系统区域设置 | `chcp 65001` 切换为 UTF-8 |
| 大量 401/403 错误 | Setup 未先执行 | 检查 collection.json 的 item 顺序 |

---

## 10. 性能基线

测试我们项目的 29 个 Request：

| 指标 | 数值 |
|---|---|
| 总耗时 | 约 5-10 秒 |
| 平均单请求 | 100-300ms |
| 总数据传输 | 约 200 KB |
| 内存占用 | 约 80 MB |

---

**文档作者**：陶展  
**最后更新**：2026-04-30  
**官方文档**：https://www.npmjs.com/package/newman
