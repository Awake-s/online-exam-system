# =====================================================================
# Online Exam System - JMeter CSV Parameterization Generator
# =====================================================================
# Purpose: Generate 4 CSV datasets from perf database for JMeter Thread Groups
# Output : doc/xx-test/04_JMeter-scripts/*.csv
# Note   : All comments in English to avoid PowerShell 5 ANSI decoding issues
# =====================================================================

$ErrorActionPreference = "Continue"
# Suppress mysql "Using a password on the command line is insecure" warnings
$env:MYSQL_PWD = "12345678"

$mysqlPwd = "12345678"
$mysqlDb  = "online_exam_system_perf"

# IMPORTANT: PowerShell 5 mis-decodes Chinese paths in $PSCommandPath.
# Hardcode the absolute path to ensure correct UTF-8 -> NTFS resolution.
$outputDir = "d:\Java Projects\在线考试系统\doc\性能测试\04_JMeter脚本与数据"

if (-not (Test-Path $outputDir)) { New-Item -ItemType Directory -Path $outputDir -Force | Out-Null }

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  Generate JMeter CSV parameterization files" -ForegroundColor Cyan
Write-Host "  Output dir: $outputDir" -ForegroundColor Gray
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# ====================================================================
# 1. students.csv - 100 students for TG1 login baseline
# ====================================================================
Write-Host "[1/4] Generate students.csv (100 students - TG1 login baseline)..." -ForegroundColor Yellow
$studentsPath = Join-Path $outputDir "students.csv"
"username,password" | Out-File -FilePath $studentsPath -Encoding UTF8
1..100 | ForEach-Object {
    $username = "perf_stu_" + ("{0:D3}" -f $_)
    "$username,Test@123456" | Add-Content -Path $studentsPath -Encoding UTF8
}
$lineCount = (Get-Content $studentsPath).Count - 1
Write-Host "    OK: $lineCount lines written to students.csv" -ForegroundColor Green

# ====================================================================
# 2. teachers.csv - 15 teachers for TG3 teacher query
# ====================================================================
Write-Host "[2/4] Generate teachers.csv (15 teachers - TG3 teacher query)..." -ForegroundColor Yellow
$teachersPath = Join-Path $outputDir "teachers.csv"
"username,password" | Out-File -FilePath $teachersPath -Encoding UTF8
1..15 | ForEach-Object {
    $username = "perf_tea_" + ("{0:D3}" -f $_)
    "$username,Test@123456" | Add-Content -Path $teachersPath -Encoding UTF8
}
Write-Host "    OK: 15 lines written to teachers.csv" -ForegroundColor Green

# ====================================================================
# 3. students-exam.csv - 50 students + examId for TG2 exam flow
# ====================================================================
Write-Host "[3/4] Generate students-exam.csv (50 students + examId - TG2 exam flow)..." -ForegroundColor Yellow
$examPath = Join-Path $outputDir "students-exam.csv"

# Find an in-progress exam (start_time <= NOW <= end_time)
$sqlFindExam = "SELECT id FROM exam_exam WHERE start_time <= NOW() AND end_time >= NOW() ORDER BY id LIMIT 1;"
$examIdResult = & mysql -uroot -N -B -e $sqlFindExam $mysqlDb 2>$null
if ([string]::IsNullOrWhiteSpace($examIdResult)) {
    Write-Host "    WARN: no in-progress exam, fallback to latest perf exam" -ForegroundColor Yellow
    $sqlFindExam = "SELECT id FROM exam_exam ORDER BY id DESC LIMIT 1;"
    $examIdResult = & mysql -uroot -N -B -e $sqlFindExam $mysqlDb 2>$null
}
$examId = ($examIdResult -split "`n" | Where-Object { $_ -match "^\d+$" } | Select-Object -First 1).Trim()
Write-Host "    Using examId = $examId" -ForegroundColor Gray

# Find perf students NOT yet attended that exam (avoid record duplicate)
$sqlFindStudents = "SELECT u.username FROM sys_user u WHERE u.username LIKE 'perf_stu_%' AND u.id NOT IN (SELECT user_id FROM exam_record WHERE exam_id = $examId) ORDER BY u.username LIMIT 50;"
$availableStudents = & mysql -uroot -N -B -e $sqlFindStudents $mysqlDb 2>$null
$studentLines = @($availableStudents -split "`n" | Where-Object { $_.Trim() -match "^perf_stu_" })

if ($studentLines.Count -lt 50) {
    Write-Host "    WARN: only $($studentLines.Count) students free, falling back to first 50 perf students" -ForegroundColor Yellow
    $sqlAllStudents = "SELECT username FROM sys_user WHERE username LIKE 'perf_stu_%' ORDER BY username LIMIT 50;"
    $studentLines = @((& mysql -uroot -N -B -e $sqlAllStudents $mysqlDb 2>$null) -split "`n" | Where-Object { $_.Trim() -match "^perf_stu_" })
}

"username,password,examId" | Out-File -FilePath $examPath -Encoding UTF8
foreach ($s in ($studentLines | Select-Object -First 50)) {
    "$($s.Trim()),Test@123456,$examId" | Add-Content -Path $examPath -Encoding UTF8
}
$realCount = (Get-Content $examPath).Count - 1
Write-Host "    OK: $realCount lines written to students-exam.csv (examId=$examId)" -ForegroundColor Green

# ====================================================================
# 4. chat-pairs.csv - 30 student->teacher pairs for TG4 chat
# ====================================================================
Write-Host "[4/4] Generate chat-pairs.csv (30 student->teacher pairs - TG4 chat)..." -ForegroundColor Yellow
$chatPath = Join-Path $outputDir "chat-pairs.csv"

# 30 perf students
$sqlStudent = "SELECT username FROM sys_user WHERE username LIKE 'perf_stu_%' ORDER BY username LIMIT 30;"
$studentRows = & mysql -uroot -N -B -e $sqlStudent $mysqlDb 2>$null

# Receiver: perf_tea_001 id
$sqlTeacher = "SELECT id FROM sys_user WHERE username = 'perf_tea_001';"
$teacherIdResult = & mysql -uroot -N -B -e $sqlTeacher $mysqlDb 2>$null
$receiverId = ($teacherIdResult -split "`n" | Where-Object { $_ -match "^\d+$" } | Select-Object -First 1).Trim()

if ([string]::IsNullOrWhiteSpace($receiverId)) {
    Write-Host "    ERROR: perf_tea_001 not found" -ForegroundColor Red
    exit 1
}

"username,password,receiverId" | Out-File -FilePath $chatPath -Encoding UTF8
$studentRows -split "`n" | Where-Object { $_.Trim() -match "^perf_stu_" } | ForEach-Object {
    $username = $_.Trim()
    "$username,Test@123456,$receiverId" | Add-Content -Path $chatPath -Encoding UTF8
}
$chatLines = (Get-Content $chatPath).Count - 1
Write-Host "    OK: $chatLines lines written to chat-pairs.csv (receiverId=$receiverId)" -ForegroundColor Green

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  All 4 CSVs generated successfully" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Get-ChildItem -Path $outputDir -Filter "*.csv" | ForEach-Object {
    $lines = (Get-Content $_.FullName).Count
    Write-Host ("    {0,-25}  {1,3} lines" -f $_.Name, $lines) -ForegroundColor White
}
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Confirm perf backend running (start-perf-backend.ps1)" -ForegroundColor Gray
Write-Host "  2. Verify login (verify-login.ps1)" -ForegroundColor Gray
Write-Host "  3. Open JMeter GUI, load 04_JMeter-scripts/exam-system-perf.jmx" -ForegroundColor Gray
Write-Host "  4. Run test, check Aggregate Report and Summary Report" -ForegroundColor Gray
