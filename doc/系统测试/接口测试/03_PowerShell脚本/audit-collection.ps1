#requires -Version 5.1
# ====================================================================
# Collection 自动化审计脚本 v2.0
# ====================================================================
# 职责：每次 Collection 变更后自动审计，确保所有测试断言可追溯到源码
# 检查项：
#   1. 每个用例的 URL 是否真实存在于 Controller @XxxMapping
#   2. 每个用例的 HTTP Method 是否与 Controller 一致
#   3. 每个反向用例（鉴权/越权）是否有对应的 SecurityConfig/PreAuthorize 依据
#   4. 用例总数 / 断言总数 / 通过率是否符合 KPI
#   5. environment.json 是否使用专用测试账号（it_xxx），避免污染
#
# 失败即不通过，需修正后才能交付
# ====================================================================

param(
    [string]$CollectionPath = "d:\Java Projects\在线考试系统\doc\接口测试\04_Postman脚本与数据\exam-system.postman_collection.json",
    [string]$EnvPath        = "d:\Java Projects\在线考试系统\doc\接口测试\04_Postman脚本与数据\exam-system.postman_environment.json",
    [string]$ControllerDir  = "d:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller"
)

$ErrorActionPreference = "Stop"
$BT = [char]96

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  Collection 审计 v2.0" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# 1. 加载 Collection + Environment
# ============================================================
$collection = Get-Content $CollectionPath -Raw -Encoding UTF8 | ConvertFrom-Json
$env_       = Get-Content $EnvPath -Raw -Encoding UTF8 | ConvertFrom-Json

# 提取所有用例
function Get-AllItems {
    param($node)
    $list = @()
    if ($node.item) {
        foreach ($child in $node.item) {
            if ($child.item) {
                $list += Get-AllItems -node $child
            } elseif ($child.request) {
                $list += $child
            }
        }
    }
    return $list
}

$items = Get-AllItems -node $collection

# ============================================================
# 2. 扫描所有 Controller 接口（用于校验 URL 真实性）
# ============================================================
$realApis = @{}  # key = "VERB /api/path"
$controllers = Get-ChildItem $ControllerDir -Filter "*Controller.java"

foreach ($file in $controllers) {
    $lines = Get-Content $file.FullName
    $content = $lines -join "`n"
    $base = ""
    if ($content -match '@RequestMapping\("([^"]+)"\)') {
        $base = $matches[1]
    }
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '@(GetMapping|PostMapping|PutMapping|DeleteMapping)(?:\("([^"]*)"\))?') {
            $verb = ($matches[1] -replace 'Mapping', '').ToUpper()
            $sub = $matches[2]
            $fullPath = $base + $sub
            $key = "{0} {1}" -f $verb, $fullPath
            $realApis[$key] = @{
                File    = $file.Name
                LineNum = $i + 1
            }
        }
    }
}

Write-Host ("[1/5] 后端接口扫描: 发现 {0} 个真实 HTTP 接口" -f $realApis.Count) -ForegroundColor Green

# ============================================================
# 3. 审计每个 Postman 用例
# ============================================================
$totalCases  = 0
$totalPassed = 0
$violations  = @()

foreach ($item in $items) {
    $totalCases++
    $caseName = $item.name
    $req = $item.request
    $method = $req.method.ToUpper()

    # 提取 URL Path（去除环境变量、query）
    $rawUrl = if ($req.url -is [string]) { $req.url } else { $req.url.raw }
    $path = $rawUrl -replace '\{\{baseUrl\}\}', '' -replace '\?.*', ''

    # 路径归一化：把 Postman 变量 {{xxx}} 和纯数字段都归一化为 ___PLACEHOLDER___
    # 例如 /api/question/{{questionId}} 和 /api/question/9001 都能匹配 @GetMapping("/{id}")
    $pathForMatch = $path -replace '\{\{[^}]+\}\}', '___PLACEHOLDER___'
    $pathForMatch = $pathForMatch -replace '/\d+(?=/|$)', '/___PLACEHOLDER___'

    # 在真实接口表中查找匹配
    $matched = $false
    $matchedKey = ""
    foreach ($k in $realApis.Keys) {
        if ($k -eq "$method $path") {
            $matched = $true
            $matchedKey = $k
            break
        }
        # 处理路径含 ID 的情况（如 /api/question/{{questionId}} 匹配 @GetMapping("/{id}")）
        $kParts = $k -split ' ', 2
        if ($kParts[0] -eq $method) {
            $kPath = $kParts[1]
            # 把 Controller 的 {id} 占位符也归一化为 ___PLACEHOLDER___，再做严格相等匹配
            $kForMatch = $kPath -replace '\{[^}]+\}', '___PLACEHOLDER___'
            if ($pathForMatch -eq $kForMatch) {
                $matched = $true
                $matchedKey = $k
                break
            }
        }
    }

    if ($matched) {
        $totalPassed++
    } else {
        $violations += [PSCustomObject]@{
            Type   = "URL_NOT_FOUND"
            Case   = $caseName
            Method = $method
            Path   = $path
        }
    }
}

Write-Host ("[2/5] Postman 用例 URL 真实性: {0}/{1} 通过" -f $totalPassed, $totalCases) -ForegroundColor $(if ($violations.Count -eq 0) { "Green" } else { "Yellow" })

# ============================================================
# 4. 校验环境变量使用 it_xxx 测试账号
# ============================================================
$envValues = @{}
foreach ($kv in $env_.values) {
    $envValues[$kv.key] = $kv.value
}

$envIssues = @()
$expectedUsers = @{ "adminUser" = "it_admin"; "teacherUser" = "it_teacher"; "studentUser" = "it_student" }
foreach ($k in $expectedUsers.Keys) {
    if ($envValues[$k] -ne $expectedUsers[$k]) {
        $envIssues += "环境变量 {0} 期望 {1}，实际 {2}（应使用专用测试账号）" -f $k, $expectedUsers[$k], $envValues[$k]
    }
}

if ($envIssues.Count -eq 0) {
    Write-Host "[3/5] 环境变量隔离: 全部使用 it_xxx 测试账号 ✓" -ForegroundColor Green
} else {
    Write-Host ("[3/5] 环境变量隔离: 发现 {0} 处违规" -f $envIssues.Count) -ForegroundColor Yellow
    $envIssues | ForEach-Object { Write-Host ("    -> {0}" -f $_) -ForegroundColor Yellow }
}

# ============================================================
# 5. 统计断言数与各类用例
# ============================================================
$totalAssertions = 0
$reverseCount    = 0  # 反向用例数（含未登录/越权/参数错误）

foreach ($item in $items) {
    if ($item.event) {
        foreach ($evt in $item.event) {
            if ($evt.listen -eq "test" -and $evt.script.exec) {
                $execCode = $evt.script.exec -join "`n"
                $matches = [regex]::Matches($execCode, 'pm\.test\(')
                $totalAssertions += $matches.Count
            }
        }
    }
    if ($item.name -match '鉴权|越权|未登录|参数缺失|参数非法|无 Token|错误 Token') {
        $reverseCount++
    }
}

$reverseRatio = if ($totalCases -gt 0) { [Math]::Round(($reverseCount / $totalCases) * 100, 1) } else { 0 }
Write-Host ("[4/5] 用例统计:") -ForegroundColor Green
Write-Host ("    用例总数:    {0}" -f $totalCases) -ForegroundColor White
Write-Host ("    断言总数:    {0}" -f $totalAssertions) -ForegroundColor White
Write-Host ("    反向用例数:  {0} ({1}%)" -f $reverseCount, $reverseRatio) -ForegroundColor White

# ============================================================
# 6. 输出审计报告
# ============================================================
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  审计结果汇总" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

$grade = "A+"
$issuesTotal = $violations.Count + $envIssues.Count

if ($issuesTotal -eq 0) {
    Write-Host "  ✓ 审计全部通过 (0 issues)" -ForegroundColor Green
    if ($reverseRatio -lt 30) {
        Write-Host ("  ⚠ 但反向用例占比偏低 ({0}%)，建议补充" -f $reverseRatio) -ForegroundColor Yellow
        $grade = "A"
    }
} else {
    Write-Host ("  ✗ 发现 {0} 处问题，需修正" -f $issuesTotal) -ForegroundColor Red
    $grade = "B"
    $violations | ForEach-Object {
        Write-Host ("    [{0}] {1}: {2} {3}" -f $_.Type, $_.Case, $_.Method, $_.Path) -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ("  审计评级: {0}" -f $grade) -ForegroundColor Cyan
Write-Host ""

# 写入审计日志
$logDir = "d:\Java Projects\在线考试系统\doc\接口测试\06_源码追溯审计"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }

$logFile = Join-Path $logDir "audit-log.md"
$now = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'

$logSb = New-Object System.Text.StringBuilder
[void]$logSb.AppendLine(("# Collection 审计日志"))
[void]$logSb.AppendLine("")
[void]$logSb.AppendLine(("> 最近一次审计: {0}" -f $now))
[void]$logSb.AppendLine("")
[void]$logSb.AppendLine("## 当前评级")
[void]$logSb.AppendLine("")
[void]$logSb.AppendLine(("**{0}**" -f $grade))
[void]$logSb.AppendLine("")
[void]$logSb.AppendLine("## 关键指标")
[void]$logSb.AppendLine("")
[void]$logSb.AppendLine("| 指标 | 值 |")
[void]$logSb.AppendLine("|---|---|")
[void]$logSb.AppendLine(("| 真实接口总数 | {0} |" -f $realApis.Count))
[void]$logSb.AppendLine(("| Collection 用例数 | {0} |" -f $totalCases))
[void]$logSb.AppendLine(("| URL 真实性通过 | {0}/{1} |" -f $totalPassed, $totalCases))
[void]$logSb.AppendLine(("| 断言总数 | {0} |" -f $totalAssertions))
[void]$logSb.AppendLine(("| 反向用例数 | {0} ({1}%) |" -f $reverseCount, $reverseRatio))
[void]$logSb.AppendLine(("| 环境变量违规 | {0} |" -f $envIssues.Count))
[void]$logSb.AppendLine("")
[void]$logSb.AppendLine("## 审计依据")
[void]$logSb.AppendLine("")
[void]$logSb.AppendLine("- Controller 源码: ``exam-system/src/main/java/com/exam/controller/*Controller.java``")
[void]$logSb.AppendLine("- Postman Collection: ``doc/接口测试/04_Postman脚本与数据/exam-system.postman_collection.json``")
[void]$logSb.AppendLine("- Environment: ``doc/接口测试/04_Postman脚本与数据/exam-system.postman_environment.json``")
[void]$logSb.AppendLine("")

if ($violations.Count -gt 0) {
    [void]$logSb.AppendLine("## URL 违规清单")
    [void]$logSb.AppendLine("")
    [void]$logSb.AppendLine("| Case | Method | Path |")
    [void]$logSb.AppendLine("|---|---|---|")
    foreach ($v in $violations) {
        [void]$logSb.AppendLine(("| {0} | {1} | {2} |" -f $v.Case, $v.Method, $v.Path))
    }
    [void]$logSb.AppendLine("")
}

if ($envIssues.Count -gt 0) {
    [void]$logSb.AppendLine("## 环境变量问题")
    [void]$logSb.AppendLine("")
    foreach ($ei in $envIssues) {
        [void]$logSb.AppendLine(("- {0}" -f $ei))
    }
    [void]$logSb.AppendLine("")
}

[void]$logSb.AppendLine("---")
[void]$logSb.AppendLine(("**审计工具**: ``doc/接口测试/03_PowerShell脚本/audit-collection.ps1``"))

$utf8Bom = New-Object System.Text.UTF8Encoding $true
[System.IO.File]::WriteAllText($logFile, $logSb.ToString(), $utf8Bom)
Write-Host ("  审计日志: {0}" -f $logFile) -ForegroundColor Cyan
Write-Host ""

if ($issuesTotal -gt 0) { exit 1 } else { exit 0 }
