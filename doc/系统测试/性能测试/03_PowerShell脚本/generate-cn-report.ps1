# =====================================================================
# 在线考试系统 - JMeter 中文化 HTML 报告生成脚本
# =====================================================================
# 用途：从 JMeter 输出的 .jtl 文件生成中文化 HTML 仪表板报告
# 前提：JMeter 5.6.3 已安装 + bin/report-template 已替换为中文模板
# 依赖：D:\Tools\apache-jmeter-5.6.3\bin\jmeter.bat
# =====================================================================

param(
    [Parameter(Mandatory=$false)]
    [string]$JtlFile = "",

    [Parameter(Mandatory=$false)]
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"

$JMETER_HOME = "D:\Tools\apache-jmeter-5.6.3"
$JMETER_BAT  = Join-Path $JMETER_HOME "bin\jmeter.bat"
$RESULTS_DIR = "D:\Tools\jmeter-results"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  JMeter 中文化 HTML 报告生成器" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# 1. 校验 JMeter 安装
if (-not (Test-Path $JMETER_BAT)) {
    Write-Host "❌ JMeter 未找到: $JMETER_BAT" -ForegroundColor Red
    Write-Host "   请先安装 JMeter 5.6.3 到 D:\Tools\apache-jmeter-5.6.3" -ForegroundColor Gray
    exit 1
}
Write-Host "✅ JMeter 已就绪: $JMETER_BAT" -ForegroundColor Green

# 2. 校验中文模板
$tmplDir = Join-Path $JMETER_HOME "bin\report-template"
$indexFmkr = Join-Path $tmplDir "index.html.fmkr"
if (Test-Path $indexFmkr) {
    $gbk = [System.Text.Encoding]::GetEncoding(936)
    $tmplContent = [System.IO.File]::ReadAllText($indexFmkr, $gbk)
    if ($tmplContent -match '性能测试|仪表板') {
        Write-Host "✅ 中文模板已安装" -ForegroundColor Green
    } else {
        Write-Host "⚠️  模板似乎未中文化（仍是英文版）" -ForegroundColor Yellow
        Write-Host "   请运行 install-cn-template.ps1 安装中文模板" -ForegroundColor Gray
    }
}

# 3. 自动定位最新 JTL 文件（如果未指定）
if ([string]::IsNullOrEmpty($JtlFile)) {
    if (-not (Test-Path $RESULTS_DIR)) {
        Write-Host "❌ JMeter 结果目录不存在: $RESULTS_DIR" -ForegroundColor Red
        exit 1
    }
    $latestJtl = Get-ChildItem -Path $RESULTS_DIR -Filter "result-*.jtl" -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latestJtl) {
        Write-Host "❌ 未找到 result-*.jtl 文件" -ForegroundColor Red
        Write-Host "   用法: .\generate-cn-report.ps1 -JtlFile <jtl路径> -OutputDir <输出目录>" -ForegroundColor Gray
        exit 1
    }
    $JtlFile = $latestJtl.FullName
    Write-Host "✅ 自动选择最新 JTL: $($latestJtl.Name)" -ForegroundColor Green
} else {
    if (-not (Test-Path $JtlFile)) {
        Write-Host "❌ JTL 文件不存在: $JtlFile" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ 使用指定 JTL: $JtlFile" -ForegroundColor Green
}

# 4. 自动生成输出目录（如果未指定）
if ([string]::IsNullOrEmpty($OutputDir)) {
    $jtlBase = [System.IO.Path]::GetFileNameWithoutExtension($JtlFile) -replace '^result-', ''
    $OutputDir = Join-Path $RESULTS_DIR "report-cn-$jtlBase"
}
Write-Host "✅ 输出目录: $OutputDir" -ForegroundColor Green

# 5. 清理旧目录（如存在）
if (Test-Path $OutputDir) {
    Write-Host ""
    Write-Host "⚠️  输出目录已存在，将被清空：$OutputDir" -ForegroundColor Yellow
    Remove-Item $OutputDir -Recurse -Force
}

# 6. 调用 JMeter 生成报告
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  正在生成中文化 HTML 报告..." -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

& $JMETER_BAT -g "$JtlFile" -o "$OutputDir" 2>&1 | Where-Object { $_ -notmatch "WARN StatusConsoleListener" } | ForEach-Object { Write-Host $_ }

# 7. 验证输出
Write-Host ""
$indexHtml = Join-Path $OutputDir "index.html"
if (Test-Path $indexHtml) {
    $content = [System.IO.File]::ReadAllText($indexHtml, [System.Text.Encoding]::UTF8)
    $cnCount = ([regex]::Matches($content, '[\u4e00-\u9fff]')).Count
    $titleMatch = [regex]::Match($content, '<title>([^<]+)</title>')

    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host "  ✅ 中文报告生成成功" -ForegroundColor Green
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  报告标题  : $($titleMatch.Groups[1].Value)" -ForegroundColor White
    Write-Host "  中文字符  : $cnCount 个" -ForegroundColor White
    Write-Host "  报告路径  : $indexHtml" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  在浏览器中打开：" -ForegroundColor Cyan
    Write-Host "    Start-Process `"$indexHtml`"" -ForegroundColor Gray
    Write-Host ""
} else {
    Write-Host "❌ 报告生成失败：未找到 index.html" -ForegroundColor Red
    exit 1
}
