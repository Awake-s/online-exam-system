<#
.SYNOPSIS
  在线考试系统 - 本地一键构建与上传工具

.DESCRIPTION
  在 Windows 上对后端（Maven）和前端（npm + vite）进行构建，
  并通过 scp 上传到服务器的 staging 目录，配合服务器端 update.sh 使用。

  典型工作流：
    1. 本地改代码 + 本地跑起来测试通过
    2. 运行此脚本打包并上传到服务器 staging
    3. SSH 到服务器执行 update.sh

.PARAMETER Target
  构建目标：backend（后端）/ frontend（前端）/ all（全部）
  默认：all

.PARAMETER Server
  服务器 SSH 登录串，默认 ubuntu@124.222.21.219

.PARAMETER StagingPath
  服务器 staging 目录，默认 /home/ubuntu/staging

.PARAMETER SkipBuild
  跳过构建，直接上传已有的 target/*.jar 或 dist/

.PARAMETER DryRun
  只构建、不上传

.PARAMETER SetupStaging
  仅创建服务器 staging 目录（首次使用时执行一次）

.EXAMPLE
  # 首次使用（只做一次）
  .\deploy\02-更新上线\local-build.ps1 -SetupStaging

.EXAMPLE
  # 最常用：改完代码 → 构建后端 + 上传
  .\deploy\02-更新上线\local-build.ps1 -Target backend

.EXAMPLE
  # 前端改了样式 → 构建前端 + 上传
  .\deploy\02-更新上线\local-build.ps1 -Target frontend

.EXAMPLE
  # 全套构建 + 上传
  .\deploy\02-更新上线\local-build.ps1 -Target all

.EXAMPLE
  # 不重新编译，只上传现有产物（比如构建阶段已手动跑过）
  .\deploy\02-更新上线\local-build.ps1 -Target backend -SkipBuild
#>

[CmdletBinding()]
param(
    [ValidateSet('backend', 'frontend', 'all')]
    [string]$Target = 'all',

    [string]$Server = 'ubuntu@124.222.21.219',

    [string]$StagingPath = '/home/ubuntu/staging',

    [switch]$SkipBuild,

    [switch]$DryRun,

    [switch]$SetupStaging
)

# ==================== 初始化 ====================
$ErrorActionPreference = 'Stop'
$ProjectRoot = $PWD
$BackendDir = Join-Path $ProjectRoot 'exam-system'
$FrontendDir = Join-Path $ProjectRoot 'art-design-pro-ui'
$JarName = 'exam-system-1.0.0.jar'

$StartTime = Get-Date

# ==================== 输出辅助 ====================
function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan
}

function Write-Info  { param([string]$Msg) Write-Host "[INFO]  $Msg" -ForegroundColor Gray }
function Write-Ok    { param([string]$Msg) Write-Host "[OK]    $Msg" -ForegroundColor Green }
function Write-Warn  { param([string]$Msg) Write-Host "[WARN]  $Msg" -ForegroundColor Yellow }
function Write-Err   { param([string]$Msg) Write-Host "[ERROR] $Msg" -ForegroundColor Red }

# ==================== 工具检测 ====================
function Test-Command {
    param([string]$Name)
    $null = Get-Command $Name -ErrorAction SilentlyContinue
    return $?
}

function Require-Command {
    param([string]$Name, [string]$InstallHint)
    if (-not (Test-Command $Name)) {
        Write-Err "$Name 未安装或不在 PATH 中"
        if ($InstallHint) { Write-Host "     $InstallHint" -ForegroundColor Gray }
        exit 1
    }
}

# ==================== Setup Staging（首次用） ====================
if ($SetupStaging) {
    Write-Step "创建服务器 staging 目录"
    Require-Command 'ssh' '请安装 OpenSSH 客户端（Win10+ 自带）'
    Write-Info "目标：${Server}:${StagingPath}"
    ssh $Server "mkdir -p $StagingPath && ls -la $StagingPath"
    if ($LASTEXITCODE -eq 0) {
        Write-Ok "staging 目录已就绪"
    } else {
        Write-Err "创建 staging 目录失败"
        exit 1
    }
    exit 0
}

# ==================== 通用检查 ====================
Require-Command 'scp' '请安装 OpenSSH 客户端（Win10+ 自带）'
Require-Command 'ssh' '请安装 OpenSSH 客户端（Win10+ 自带）'

# ==================== 后端构建 + 上传 ====================
function Build-Backend {
    Write-Step "构建后端 JAR"

    $JarPath = Join-Path $BackendDir "target\$JarName"

    if (-not $SkipBuild) {
        Require-Command 'mvn' '请安装 Maven 3.6+ 并加入 PATH'

        if (-not (Test-Path $BackendDir)) {
            Write-Err "后端目录不存在：$BackendDir"
            exit 1
        }

        Write-Info "目录：$BackendDir"
        Write-Info "命令：mvn clean package -DskipTests"

        Push-Location $BackendDir
        try {
            $mvnStart = Get-Date
            & mvn clean package -DskipTests 2>&1 | ForEach-Object {
                # 只显示关键行，避免刷屏
                if ($_ -match '^\[ERROR\]|^\[WARN\]|BUILD SUCCESS|BUILD FAILURE|Building jar:') {
                    Write-Host "  $_"
                }
            }
            if ($LASTEXITCODE -ne 0) {
                Write-Err "Maven 构建失败"
                exit 1
            }
            $mvnElapsed = (Get-Date) - $mvnStart
            Write-Ok ("Maven 构建成功（耗时 {0:F1} 秒）" -f $mvnElapsed.TotalSeconds)
        } finally {
            Pop-Location
        }
    } else {
        Write-Warn "跳过构建（-SkipBuild），直接上传现有 JAR"
    }

    if (-not (Test-Path $JarPath)) {
        Write-Err "未找到 JAR：$JarPath"
        Write-Err "请先手动执行 mvn package 或去掉 -SkipBuild"
        exit 1
    }

    $jarSize = (Get-Item $JarPath).Length / 1MB
    Write-Info ("JAR 大小：{0:F1} MB" -f $jarSize)

    if ($DryRun) {
        Write-Warn "Dry-Run 模式，跳过上传"
        return
    }

    Write-Step "上传 JAR 到服务器"
    Write-Info "scp -> ${Server}:${StagingPath}/$JarName"

    $scpStart = Get-Date
    & scp $JarPath "${Server}:${StagingPath}/$JarName"
    if ($LASTEXITCODE -ne 0) {
        Write-Err "scp 上传失败"
        Write-Host ""
        Write-Host "可能原因：" -ForegroundColor Yellow
        Write-Host "  1. 首次使用：先跑 .\deploy\02-更新上线\local-build.ps1 -SetupStaging"
        Write-Host "  2. 网络不通：检查服务器是否可达"
        Write-Host "  3. 密码错误：重新输入密码"
        exit 1
    }
    $scpElapsed = (Get-Date) - $scpStart
    Write-Ok ("JAR 上传成功（耗时 {0:F1} 秒）" -f $scpElapsed.TotalSeconds)

    Write-Host ""
    Write-Host "📋 下一步（在服务器上执行）：" -ForegroundColor Cyan
    Write-Host "     ssh $Server" -ForegroundColor White
    Write-Host "     sudo bash /opt/exam-system/update.sh backend" -ForegroundColor White
}

# ==================== 前端构建 + 上传 ====================
function Build-Frontend {
    Write-Step "构建前端 dist"

    $DistPath = Join-Path $FrontendDir 'dist'

    if (-not $SkipBuild) {
        Require-Command 'npm' '请安装 Node.js 18+（https://nodejs.org）'

        if (-not (Test-Path $FrontendDir)) {
            Write-Err "前端目录不存在：$FrontendDir"
            exit 1
        }

        Write-Info "目录：$FrontendDir"

        Push-Location $FrontendDir
        try {
            # 依赖检查
            if (-not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
                Write-Warn "node_modules 不存在，自动执行 npm install"
                & npm install
                if ($LASTEXITCODE -ne 0) {
                    Write-Err "npm install 失败"
                    exit 1
                }
            }

            Write-Info "命令：npm run build"
            $npmStart = Get-Date
            & npm run build 2>&1 | ForEach-Object {
                if ($_ -match 'error|ERR|vite v|built in|modules transformed|^✓|^✗') {
                    Write-Host "  $_"
                }
            }
            if ($LASTEXITCODE -ne 0) {
                Write-Err "前端构建失败"
                exit 1
            }
            $npmElapsed = (Get-Date) - $npmStart
            Write-Ok ("前端构建成功（耗时 {0:F1} 秒）" -f $npmElapsed.TotalSeconds)
        } finally {
            Pop-Location
        }
    } else {
        Write-Warn "跳过构建（-SkipBuild），直接上传现有 dist"
    }

    if (-not (Test-Path $DistPath)) {
        Write-Err "未找到 dist：$DistPath"
        Write-Err "请先手动执行 npm run build 或去掉 -SkipBuild"
        exit 1
    }

    $fileCount = (Get-ChildItem $DistPath -Recurse -File).Count
    $totalSize = (Get-ChildItem $DistPath -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Info ("dist：{0} 个文件，共 {1:F1} MB" -f $fileCount, $totalSize)

    if ($DryRun) {
        Write-Warn "Dry-Run 模式，跳过上传"
        return
    }

    Write-Step "上传 dist 到服务器"
    Write-Info "scp -r -> ${Server}:${StagingPath}/dist/"

    # 上传前先清理服务器端可能存在的旧 dist（避免新旧文件混合）
    Write-Info "清理服务器端旧 staging/dist..."
    & ssh $Server "rm -rf $StagingPath/dist"
    if ($LASTEXITCODE -ne 0) {
        Write-Warn "清理旧 dist 失败，继续尝试上传"
    }

    $scpStart = Get-Date
    & scp -r $DistPath "${Server}:${StagingPath}/"
    if ($LASTEXITCODE -ne 0) {
        Write-Err "scp 上传失败"
        exit 1
    }
    $scpElapsed = (Get-Date) - $scpStart
    Write-Ok ("dist 上传成功（耗时 {0:F1} 秒）" -f $scpElapsed.TotalSeconds)

    Write-Host ""
    Write-Host "📋 下一步（在服务器上执行）：" -ForegroundColor Cyan
    Write-Host "     ssh $Server" -ForegroundColor White
    Write-Host "     sudo bash /opt/exam-system/update.sh frontend" -ForegroundColor White
}

# ==================== 主流程 ====================
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   在线考试系统 - 本地构建与上传工具              ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host "  目标：       $Target"
Write-Host "  服务器：     $Server"
Write-Host "  Staging：    $StagingPath"
Write-Host "  跳过构建：   $SkipBuild"
Write-Host "  Dry-Run：    $DryRun"
Write-Host ""

switch ($Target) {
    'backend'  { Build-Backend }
    'frontend' { Build-Frontend }
    'all'      {
        Build-Frontend
        Build-Backend
    }
}

$TotalElapsed = (Get-Date) - $StartTime
Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Ok ("全部完成（总耗时 {0:F1} 秒）" -f $TotalElapsed.TotalSeconds)
Write-Host "==================================================" -ForegroundColor Green

if (-not $DryRun) {
    Write-Host ""
    Write-Host "📋 服务器端一键执行命令：" -ForegroundColor Cyan
    Write-Host ""
    switch ($Target) {
        'backend'  { Write-Host "   ssh $Server 'sudo bash /opt/exam-system/update.sh backend'" -ForegroundColor White }
        'frontend' { Write-Host "   ssh $Server 'sudo bash /opt/exam-system/update.sh frontend'" -ForegroundColor White }
        'all'      { Write-Host "   ssh $Server 'sudo bash /opt/exam-system/update.sh all'"      -ForegroundColor White }
    }
    Write-Host ""
}
