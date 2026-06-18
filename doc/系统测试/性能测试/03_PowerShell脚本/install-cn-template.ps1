# =====================================================================
# JMeter HTML 报告中文模板安装脚本
# =====================================================================
# 用途：将 JMeter 5.x 的 HTML 报告模板替换为社区中文版
# 来源：https://github.com/mzky/jmeter5.x-cn-report-template
# 备份：原英文模板会备份到 bin\report-template-en-backup
# =====================================================================

$ErrorActionPreference = "Stop"

$JMETER_HOME = "D:\Tools\apache-jmeter-5.6.3"
$RT_DIR = Join-Path $JMETER_HOME "bin\report-template"
$BACKUP_DIR = Join-Path $JMETER_HOME "bin\report-template-en-backup"
$TMP_DIR = "D:\Tools\jmeter-cn-template-tmp"
$REPO = "https://github.com/mzky/jmeter5.x-cn-report-template.git"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  JMeter HTML 报告中文模板安装" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# 1. 校验 JMeter 安装
if (-not (Test-Path $RT_DIR)) {
    Write-Host "❌ JMeter 报告模板目录未找到: $RT_DIR" -ForegroundColor Red
    Write-Host "   请先安装 JMeter 5.6.3 到 D:\Tools\apache-jmeter-5.6.3" -ForegroundColor Gray
    exit 1
}
Write-Host "✅ JMeter 安装目录就绪" -ForegroundColor Green

# 2. 备份原版（如未备份）
if (Test-Path $BACKUP_DIR) {
    Write-Host "ℹ️  英文模板已有备份: $BACKUP_DIR" -ForegroundColor Gray
} else {
    Copy-Item -Path $RT_DIR -Destination $BACKUP_DIR -Recurse -Force
    Write-Host "✅ 已备份英文模板到: $BACKUP_DIR" -ForegroundColor Green
}

# 3. 克隆中文模板
if (Test-Path $TMP_DIR) {
    Remove-Item $TMP_DIR -Recurse -Force
}
Write-Host ""
Write-Host "📥 正在克隆中文模板..." -ForegroundColor Cyan
git clone --depth 1 $REPO $TMP_DIR 2>&1 | Out-Null
if (-not (Test-Path "$TMP_DIR\report-template")) {
    Write-Host "❌ 克隆失败，尝试 Gitee 镜像..." -ForegroundColor Yellow
    git clone --depth 1 https://gitee.com/smooth00/jmeter-cn-report-template.git $TMP_DIR 2>&1 | Out-Null
    if (-not (Test-Path "$TMP_DIR\report-template")) {
        Write-Host "❌ 两个源都克隆失败，请检查网络" -ForegroundColor Red
        exit 1
    }
}
Write-Host "✅ 中文模板克隆成功" -ForegroundColor Green

# 4. 替换 .fmkr 文件（保留原版 css/js/字体）
$srcRt = Join-Path $TMP_DIR "report-template"
$copied = 0
Write-Host ""
Write-Host "📝 替换 .fmkr 模板文件（保留 css/js）..." -ForegroundColor Cyan
Get-ChildItem $srcRt -Recurse -Filter "*.fmkr" | ForEach-Object {
    $rel = $_.FullName.Replace($srcRt + "\", "")
    $target = Join-Path $RT_DIR $rel
    $targetDir = Split-Path $target -Parent
    if (-not (Test-Path $targetDir)) { New-Item -ItemType Directory -Path $targetDir -Force | Out-Null }
    Copy-Item -Path $_.FullName -Destination $target -Force
    $copied++
    Write-Host ("    ✓ {0}" -f $rel) -ForegroundColor Gray
}

# 5. 转换编码 UTF-8 -> GBK（Windows JMeter 期望 GBK）
Write-Host ""
Write-Host "🔧 转换 .fmkr 编码 (UTF-8 -> GBK Windows 兼容)..." -ForegroundColor Cyan
$gbk = [System.Text.Encoding]::GetEncoding(936)
$utf8 = [System.Text.Encoding]::UTF8
Get-ChildItem $RT_DIR -Recurse -Filter "*.fmkr" | ForEach-Object {
    # 先尝试 UTF-8 读取（如果是 UTF-8 文件）
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    # 检测是否包含合法 UTF-8 序列（粗略）
    try {
        $contentUtf8 = [System.Text.Encoding]::UTF8.GetString($bytes)
        # 如果文本包含中文且不是乱码，说明是 UTF-8
        if ($contentUtf8 -match '性能|响应|请求|图表') {
            [System.IO.File]::WriteAllText($_.FullName, $contentUtf8, $gbk)
        }
    } catch {}
}
Write-Host "✅ 编码转换完成" -ForegroundColor Green

# 6. 清理临时目录
Remove-Item $TMP_DIR -Recurse -Force -ErrorAction SilentlyContinue

# 7. 验证安装
$indexFmkr = Join-Path $RT_DIR "index.html.fmkr"
$verify = [System.IO.File]::ReadAllText($indexFmkr, $gbk)
Write-Host ""
if ($verify -match '性能测试|仪表板') {
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host "  ✅ 中文模板安装成功" -ForegroundColor Green
    Write-Host "==============================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  替换文件数: $copied 个 .fmkr 模板" -ForegroundColor White
    Write-Host "  备份位置  : $BACKUP_DIR" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  下一步：跑 generate-cn-report.ps1 生成中文 HTML 报告" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host "❌ 安装验证失败" -ForegroundColor Red
    exit 1
}
