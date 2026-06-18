# ==============================================================
# 在线考试系统 接口测试一键运行脚本
# 工具栈: Postman + Newman + htmlextra
# 作者: 陶展
# ==============================================================

$ErrorActionPreference = "Stop"

$PROJECT_ROOT = "d:\Java Projects\在线考试系统"
$COLLECTION   = "$PROJECT_ROOT\doc\接口测试\04_Postman脚本与数据\exam-system.postman_collection.json"
$ENVIRONMENT  = "$PROJECT_ROOT\doc\接口测试\04_Postman脚本与数据\exam-system.postman_environment.json"
$RESULTS_DIR  = "D:\Tools\接口测试结果"
$BACKEND_URL  = "http://localhost:8081"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  在线考试系统 接口测试 (Postman + Newman)" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: 检查 Newman
Write-Host "[1/4] 检查 Newman CLI..." -ForegroundColor Yellow
try {
    $newmanVersion = newman --version 2>&1
    Write-Host "    OK Newman v$newmanVersion" -ForegroundColor Green
} catch {
    Write-Host "    FAIL Newman 未安装" -ForegroundColor Red
    Write-Host "    请先运行: npm install -g newman newman-reporter-htmlextra" -ForegroundColor Gray
    exit 1
}

# Step 2: 检查后端
Write-Host "[2/4] 检查后端服务 ($BACKEND_URL)..." -ForegroundColor Yellow
try {
    $r = Invoke-WebRequest "$BACKEND_URL/api/auth/captcha" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
    if ($r.StatusCode -eq 200) {
        Write-Host "    OK 后端运行中" -ForegroundColor Green
    } else {
        throw "HTTP $($r.StatusCode)"
    }
} catch {
    Write-Host "    FAIL 后端未启动" -ForegroundColor Red
    Write-Host "    请先启动后端: cd exam-system; mvn spring-boot:run" -ForegroundColor Gray
    exit 1
}

# Step 3: 检查 Postman 文件
Write-Host "[3/4] 检查 Postman 文件..." -ForegroundColor Yellow
if (-not (Test-Path $COLLECTION)) { Write-Host "    FAIL collection.json 不存在" -ForegroundColor Red; exit 1 }
if (-not (Test-Path $ENVIRONMENT)) { Write-Host "    FAIL environment.json 不存在" -ForegroundColor Red; exit 1 }
Write-Host "    OK Postman 文件就绪" -ForegroundColor Green

# Step 4: 准备输出目录
Write-Host "[4/4] 准备输出目录..." -ForegroundColor Yellow
if (-not (Test-Path $RESULTS_DIR)) { New-Item -ItemType Directory -Force -Path $RESULTS_DIR | Out-Null }
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$htmlReport = "$RESULTS_DIR\report-$ts.html"
$jsonReport = "$RESULTS_DIR\newman-$ts.json"
Write-Host "    OK 输出目录: $RESULTS_DIR" -ForegroundColor Green

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  开始执行接口测试..." -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# 执行 Newman（注意：reporter 列表用引号包裹，避免 PowerShell 将逗号解析为数组）
& newman run $COLLECTION `
    -e $ENVIRONMENT `
    --reporters "htmlextra,cli,json" `
    --reporter-htmlextra-export $htmlReport `
    --reporter-htmlextra-darkTheme `
    --reporter-htmlextra-title "在线考试系统 · 接口测试报告" `
    --reporter-htmlextra-titleSize 4 `
    --reporter-htmlextra-browserTitle "接口测试报告" `
    --reporter-htmlextra-showOnlyFails false `
    --reporter-htmlextra-showFolderDescription `
    --reporter-htmlextra-timezone "Asia/Shanghai" `
    --reporter-json-export $jsonReport `
    --color on

$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  测试结果汇总" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $htmlReport) {
    $sz = [Math]::Round((Get-Item $htmlReport).Length / 1KB, 1)
    Write-Host "OK HTML 报告: $htmlReport ($sz KB)" -ForegroundColor Green
} else {
    Write-Host "FAIL HTML 报告未生成" -ForegroundColor Red
}

if ($exitCode -eq 0) {
    Write-Host "OK 所有测试通过 (exit=0)" -ForegroundColor Green
} else {
    Write-Host "WARN 部分测试失败 (exit=$exitCode), 详见报告" -ForegroundColor Yellow
}

# 自动打开报告
Write-Host ""
Write-Host "正在打开 HTML 报告..." -ForegroundColor Yellow
Start-Process $htmlReport

exit $exitCode