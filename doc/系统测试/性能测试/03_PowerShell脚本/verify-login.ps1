# =====================================================================
# Online Exam System - Login Availability Verifier
# =====================================================================
# Usage: Validate perf accounts can login before JMeter test
# Prerequisite: start-perf-backend.ps1 running in another terminal
# =====================================================================

$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8081"
$password = "Test@123456"

$accounts = @(
    @{ role = "Admin  "; username = "perf_admin_01" },
    @{ role = "Teacher"; username = "perf_tea_001" },
    @{ role = "Student"; username = "perf_stu_001" },
    @{ role = "Student"; username = "perf_stu_150" },
    @{ role = "Student"; username = "perf_stu_300" }
)

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "    Online Exam System - Login Availability Verifier" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Backend health check
Write-Host "[1/2] Backend health check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 5
    if ($health.status -eq "UP") {
        Write-Host "    OK: backend is UP" -ForegroundColor Green
        if ($health.components) {
            Write-Host "      Components:" -ForegroundColor Gray
            $health.components.PSObject.Properties | ForEach-Object {
                $color = if ($_.Value.status -eq "UP") { "Green" } else { "Red" }
                Write-Host ("        {0,-12} = {1}" -f $_.Name, $_.Value.status) -ForegroundColor $color
            }
        }
    } else {
        Write-Host "    WARN: backend status = $($health.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "    FAIL: backend unreachable at $baseUrl" -ForegroundColor Red
    Write-Host "          Make sure start-perf-backend.ps1 is running" -ForegroundColor Gray
    Write-Host "          Error: $($_.Exception.Message)" -ForegroundColor Gray
    exit 1
}

Write-Host ""

# 2. Try login each account
Write-Host "[2/2] Try login 5 perf accounts..." -ForegroundColor Yellow
Write-Host ""

$success = 0
$fail = 0
foreach ($acc in $accounts) {
    $body = @{ username = $acc.username; password = $password } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json; charset=utf-8" -TimeoutSec 10

        if ($response.code -eq 200 -and $response.data.token) {
            $tokenPreview = $response.data.token.Substring(0, [Math]::Min(30, $response.data.token.Length)) + "..."
            $userInfo = $response.data.userInfo
            Write-Host ("    OK   {0} {1,-18} -> id={2,3} role={3,-7} token={4}" -f $acc.role, $acc.username, $userInfo.id, $userInfo.roleCode, $tokenPreview) -ForegroundColor Green
            $success++
        } else {
            Write-Host ("    FAIL {0} {1,-18} -> code={2} msg={3}" -f $acc.role, $acc.username, $response.code, $response.message) -ForegroundColor Red
            $fail++
        }
    } catch {
        $errMsg = $_.Exception.Message
        if ($_.ErrorDetails.Message) { $errMsg = $_.ErrorDetails.Message }
        Write-Host ("    FAIL {0} {1,-18} -> exception: {2}" -f $acc.role, $acc.username, $errMsg) -ForegroundColor Red
        $fail++
    }
}

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
$summary = "Result: $success success / $fail fail"
$color = if ($fail -eq 0) { "Green" } else { "Yellow" }
Write-Host "  $summary" -ForegroundColor $color
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

if ($fail -eq 0) {
    Write-Host "  All accounts can login. Ready for JMeter test." -ForegroundColor Green
    Write-Host ""
    Write-Host "  Next:" -ForegroundColor Cyan
    Write-Host "    1. Open JMeter GUI" -ForegroundColor Gray
    Write-Host "    2. Load doc/xx-test/04_JMeter-scripts/exam-system-perf.jmx" -ForegroundColor Gray
    Write-Host "    3. Click Run button to start test" -ForegroundColor Gray
    exit 0
} else {
    Write-Host "  Some accounts failed login. Check:" -ForegroundColor Red
    Write-Host "    1. Backend started with perf profile?" -ForegroundColor Gray
    Write-Host "    2. perf data generated? Run DataVerifyTest" -ForegroundColor Gray
    Write-Host "    3. Password is Test@123456?" -ForegroundColor Gray
    exit 1
}
