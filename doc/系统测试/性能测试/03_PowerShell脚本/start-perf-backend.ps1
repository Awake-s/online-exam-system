# =====================================================================
# Online Exam System - Backend Launcher (perf profile)
# =====================================================================
# IMPORTANT: This script intentionally contains ZERO Chinese characters
# to avoid PowerShell 5 ANSI mis-decoding of .ps1 files.
#
# USAGE:
#   Option 1 (recommended): cd to scripts folder, then .\start-perf-backend.ps1
#     cd "d:\Java Projects\在线考试系统\doc\性能测试\03_PowerShell脚本"
#     .\start-perf-backend.ps1
#   Option 2: full path invocation from anywhere
#     & "d:\...\03_PowerShell脚本\start-perf-backend.ps1"
# Note: This script auto-walks up from $PWD to locate the 'exam-system' folder,
#       so you don't need to cd to project root first.
# =====================================================================

$ErrorActionPreference = "Stop"
$env:MYSQL_PWD = "12345678"

# Strategy: try $PWD first; if no exam-system there, walk up from $PWD until found
function Find-ExamSystemDir {
    $current = (Get-Location).Path
    for ($i = 0; $i -lt 5; $i++) {
        $candidate = Join-Path $current "exam-system"
        if (Test-Path $candidate -PathType Container) {
            $pomFile = Join-Path $candidate "pom.xml"
            if (Test-Path $pomFile) { return (Get-Item $candidate).FullName }
        }
        $parent = Split-Path -Parent $current
        if ([string]::IsNullOrEmpty($parent) -or $parent -eq $current) { break }
        $current = $parent
    }
    return $null
}

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "    Backend Launcher (perf profile)" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

$backendDir = Find-ExamSystemDir
if ([string]::IsNullOrEmpty($backendDir)) {
    Write-Host "FAIL: cannot locate 'exam-system' folder from $((Get-Location).Path)" -ForegroundColor Red
    Write-Host "      Please cd to project root first (the folder containing exam-system)" -ForegroundColor Gray
    Write-Host "      Then re-run this script." -ForegroundColor Gray
    exit 1
}
Write-Host "  Backend dir   : $backendDir" -ForegroundColor Gray
Write-Host "  Spring Profile: perf" -ForegroundColor Yellow
Write-Host "  Database      : online_exam_system_perf" -ForegroundColor Yellow
Write-Host ""

# 1. MySQL check
Write-Host "[1/3] MySQL service check..." -ForegroundColor Yellow
& mysql -uroot -e "SELECT 1;" online_exam_system_perf 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "    FAIL: cannot connect to MySQL or perf database missing" -ForegroundColor Red
    exit 1
}
Write-Host "    OK: MySQL accessible" -ForegroundColor Green

# 2. Redis check
Write-Host "[2/3] Redis service check..." -ForegroundColor Yellow
$redisOk = Test-NetConnection -ComputerName 127.0.0.1 -Port 6379 -InformationLevel Quiet -WarningAction SilentlyContinue
if (-not $redisOk) {
    Write-Host "    FAIL: Redis port 6379 unreachable" -ForegroundColor Red
    exit 1
}
Write-Host "    OK: Redis port 6379 reachable" -ForegroundColor Green

# 3. Config check via relative path (no Chinese in script)
Write-Host "[3/3] Check application-perf.yml..." -ForegroundColor Yellow
$perfYml = Join-Path $backendDir "src\main\resources\application-perf.yml"
if (-not (Test-Path $perfYml)) {
    Write-Host "    FAIL: application-perf.yml missing at $perfYml" -ForegroundColor Red
    exit 1
}
Write-Host "    OK: application-perf.yml found" -ForegroundColor Green

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "  All checks passed. Starting Spring Boot..." -ForegroundColor Green
Write-Host "  Wait ~30s for: 'Started ExamSystemApplication in xx.x seconds'" -ForegroundColor Gray
Write-Host "  Health check:  http://localhost:8081/actuator/health" -ForegroundColor Gray
Write-Host "  Stop backend:  Ctrl+C" -ForegroundColor Gray
Write-Host "==============================================================" -ForegroundColor Green
Write-Host ""

Push-Location $backendDir
try {
    & mvn spring-boot:run "-Dspring-boot.run.profiles=perf"
} finally {
    Pop-Location
}
