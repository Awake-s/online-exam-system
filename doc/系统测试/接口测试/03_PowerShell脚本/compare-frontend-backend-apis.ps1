#requires -Version 5.1
# ============================================================
# 前后端接口对接情况对照分析
# 扫描:
#   - 后端: exam-system/src/main/java/com/exam/controller/*.java
#   - 前端: art-design-pro-ui/src/api/**/*.ts
# 输出:
#   - 后端接口总数 / 前端调用总数
#   - 前端已对接的后端接口
#   - 前端调用但后端没实现的接口（异常）
#   - 后端有但前端没用的接口（闲置）
# ============================================================

$ErrorActionPreference = "Stop"

$BackendDir  = "d:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller"
$FrontendDir = "d:\Java Projects\在线考试系统\art-design-pro-ui\src\api"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  前后端接口对接情况对照分析" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# 1. 扫描后端接口（与 audit 脚本同样逻辑）
# ============================================================
$backendApis = @{}  # key = "VERB /api/path" → @{ File; LineNum }
$controllers = Get-ChildItem $BackendDir -Filter "*Controller.java"

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
            $backendApis[$key] = @{
                File    = $file.Name
                LineNum = $i + 1
            }
        }
    }
}

Write-Host ("[1/3] 后端: 扫描 {0} 个 Controller，发现 {1} 个接口" -f $controllers.Count, $backendApis.Count) -ForegroundColor Green

# ============================================================
# 2. 扫描前端调用（全局多行正则，支持单行和换行的 request.xxx({ ... url: ... }) 写法）
# ============================================================
$frontendCalls = @{}  # key = "VERB /api/path" → @{ File; LineNum }
$tsFiles = Get-ChildItem $FrontendDir -Filter "*.ts" -Recurse

foreach ($tsFile in $tsFiles) {
    $content = Get-Content $tsFile.FullName -Raw
    $lines = Get-Content $tsFile.FullName

    # 全局正则: request.<verb>(<泛型>)?({ ... url: '...' ... })
    # 用 [\s\S]*? 支持跨行
    # ⚠️ 用单引号字符串避免 PowerShell 反引号转义，这样字符类 [`'"] 才能正确匹配反引号字符串
    $pattern = 'request\.(get|post|put|del|delete|download)(?:<[^>]*>)?\(\s*\{[\s\S]*?url:\s*[`''"]([^`''"]+)[`''"]'
    $rxMatches = [regex]::Matches($content, $pattern)

    foreach ($m in $rxMatches) {
        $verb = $m.Groups[1].Value.ToUpper()
        if ($verb -eq "DEL") { $verb = "DELETE" }
        if ($verb -eq "DOWNLOAD") { $verb = "GET" }
        $url = $m.Groups[2].Value -replace '\$\{[^}]+\}', '___PLACEHOLDER___'

        # 找到该匹配在原文的行号（取首字符的位置）
        $charIdx = $m.Index
        $lineNum = ($content.Substring(0, $charIdx) -split "`n").Count

        $key = "{0} {1}" -f $verb, $url
        if (-not $frontendCalls.ContainsKey($key)) {
            $frontendCalls[$key] = @{
                File    = $tsFile.Name
                LineNum = $lineNum
            }
        }
    }

    # 处理 request.download('/api/...') 这种简写（不带对象参数）
    # 同样用单引号字符串以正确支持反引号字符串字面量
    $rxDl = [regex]::Matches($content, 'request\.download\(\s*[`''"]([^`''"]+)[`''"]')
    foreach ($m in $rxDl) {
        $url = $m.Groups[1].Value -replace '\$\{[^}]+\}', '___PLACEHOLDER___'
        $charIdx = $m.Index
        $lineNum = ($content.Substring(0, $charIdx) -split "`n").Count
        $key = "GET {0}" -f $url
        if (-not $frontendCalls.ContainsKey($key)) {
            $frontendCalls[$key] = @{
                File    = $tsFile.Name
                LineNum = $lineNum
            }
        }
    }
}

Write-Host ("[2/3] 前端: 扫描 {0} 个 .ts 文件，发现 {1} 个调用" -f $tsFiles.Count, $frontendCalls.Count) -ForegroundColor Green

# ============================================================
# 3. 对照分析
# ============================================================

# 匹配规则: 后端的 /{id} 占位符应能匹配前端的 ___PLACEHOLDER___
function Test-ApiMatch {
    param([string]$frontendKey, [string]$backendKey)
    if ($frontendKey -eq $backendKey) { return $true }

    $fParts = $frontendKey -split ' ', 2
    $bParts = $backendKey -split ' ', 2
    if ($fParts[0] -ne $bParts[0]) { return $false }

    # 把后端的 {xxx} 占位符也归一化
    $bNorm = $bParts[1] -replace '\{[^}]+\}', '___PLACEHOLDER___'
    return ($fParts[1] -eq $bNorm)
}

$matchedFrontend = @{}  # 前端调用 → 匹配到的后端 key
$unmatchedFrontend = @() # 前端调用，后端无对应

foreach ($fk in $frontendCalls.Keys) {
    $found = $false
    foreach ($bk in $backendApis.Keys) {
        if (Test-ApiMatch -frontendKey $fk -backendKey $bk) {
            $matchedFrontend[$fk] = $bk
            $found = $true
            break
        }
    }
    if (-not $found) {
        $unmatchedFrontend += $fk
    }
}

# 后端有但前端没用的
$matchedBackendKeys = @($matchedFrontend.Values) | Sort-Object -Unique
$unusedBackend = @()
foreach ($bk in $backendApis.Keys) {
    if ($bk -notin $matchedBackendKeys) {
        $unusedBackend += $bk
    }
}

# ============================================================
# 4. 输出汇总
# ============================================================
Write-Host "[3/3] 对照分析" -ForegroundColor Green
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  对接情况汇总" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

$frontMatched = $matchedFrontend.Count
$frontTotal   = $frontendCalls.Count
$backMatched  = $matchedBackendKeys.Count
$backTotal    = $backendApis.Count
$frontMatchRate = if ($frontTotal -gt 0) { [Math]::Round(($frontMatched / $frontTotal) * 100, 1) } else { 0 }
$backCoverRate  = if ($backTotal  -gt 0) { [Math]::Round(($backMatched  / $backTotal)  * 100, 1) } else { 0 }

Write-Host ("  后端接口总数:        {0}" -f $backTotal) -ForegroundColor White
Write-Host ("  前端调用总数:        {0}" -f $frontTotal) -ForegroundColor White
Write-Host ""
Write-Host ("  前端调用成功匹配:    {0}/{1}  ({2}%)" -f $frontMatched, $frontTotal, $frontMatchRate) -ForegroundColor $(if ($frontMatchRate -ge 95) { "Green" } else { "Yellow" })
Write-Host ("  后端接口被前端使用:  {0}/{1}  ({2}%)" -f $backMatched, $backTotal, $backCoverRate) -ForegroundColor $(if ($backCoverRate -ge 80) { "Green" } else { "Yellow" })
Write-Host ""

if ($unmatchedFrontend.Count -gt 0) {
    Write-Host ("⚠ 前端调用但后端没实现 ({0} 个):" -f $unmatchedFrontend.Count) -ForegroundColor Red
    foreach ($u in ($unmatchedFrontend | Sort-Object)) {
        Write-Host ("    {0}    [{1}:L{2}]" -f $u, $frontendCalls[$u].File, $frontendCalls[$u].LineNum) -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($unusedBackend.Count -gt 0) {
    Write-Host ("ℹ 后端有但前端未使用 ({0} 个):" -f $unusedBackend.Count) -ForegroundColor Cyan
    foreach ($u in ($unusedBackend | Sort-Object)) {
        Write-Host ("    {0}    [{1}:L{2}]" -f $u, $backendApis[$u].File, $backendApis[$u].LineNum) -ForegroundColor Gray
    }
    Write-Host ""
}

# ============================================================
# 5. 写报告文件
# ============================================================
$reportPath = "d:\Java Projects\在线考试系统\doc\接口测试\06_源码追溯审计\前后端对接情况.md"
$now = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
$sb = New-Object System.Text.StringBuilder

[void]$sb.AppendLine("# 前后端接口对接情况报告")
[void]$sb.AppendLine("")
[void]$sb.AppendLine(("> 生成时间: {0}" -f $now))
[void]$sb.AppendLine("> 扫描器: ``compare-frontend-backend-apis.ps1``")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("## 1. 关键指标")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("| 指标 | 值 |")
[void]$sb.AppendLine("|---|---|")
[void]$sb.AppendLine(("| 后端接口总数 | {0} |" -f $backTotal))
[void]$sb.AppendLine(("| 前端调用总数 | {0} |" -f $frontTotal))
[void]$sb.AppendLine(("| 前端调用匹配率 | {0}/{1} ({2}%) |" -f $frontMatched, $frontTotal, $frontMatchRate))
[void]$sb.AppendLine(("| 后端接口覆盖率 | {0}/{1} ({2}%) |" -f $backMatched, $backTotal, $backCoverRate))
[void]$sb.AppendLine(("| 前端孤立调用 | {0} |" -f $unmatchedFrontend.Count))
[void]$sb.AppendLine(("| 后端闲置接口 | {0} |" -f $unusedBackend.Count))
[void]$sb.AppendLine("")

if ($unmatchedFrontend.Count -gt 0) {
    [void]$sb.AppendLine("## 2. 前端调用但后端没实现（需修复）")
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("| Method | URL | Frontend File | Line |")
    [void]$sb.AppendLine("|---|---|---|---|")
    foreach ($u in ($unmatchedFrontend | Sort-Object)) {
        $kv = $u -split ' ', 2
        [void]$sb.AppendLine(("| {0} | ``{1}`` | ``{2}`` | {3} |" -f $kv[0], $kv[1], $frontendCalls[$u].File, $frontendCalls[$u].LineNum))
    }
    [void]$sb.AppendLine("")
}

if ($unusedBackend.Count -gt 0) {
    [void]$sb.AppendLine("## 3. 后端有但前端未使用（闲置接口）")
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("| Method | URL | Controller | Line |")
    [void]$sb.AppendLine("|---|---|---|---|")
    foreach ($u in ($unusedBackend | Sort-Object)) {
        $kv = $u -split ' ', 2
        [void]$sb.AppendLine(("| {0} | ``{1}`` | ``{2}`` | {3} |" -f $kv[0], $kv[1], $backendApis[$u].File, $backendApis[$u].LineNum))
    }
    [void]$sb.AppendLine("")
}

[void]$sb.AppendLine("## 4. 已成功对接的接口（前后端都有）")
[void]$sb.AppendLine("")
[void]$sb.AppendLine(("共 {0} 个接口前后端均已实现：" -f $matchedFrontend.Count))
[void]$sb.AppendLine("")
[void]$sb.AppendLine("| Method | URL | Controller | Frontend |")
[void]$sb.AppendLine("|---|---|---|---|")
foreach ($fk in ($matchedFrontend.Keys | Sort-Object)) {
    $bk = $matchedFrontend[$fk]
    $fkParts = $fk -split ' ', 2
    [void]$sb.AppendLine(("| {0} | ``{1}`` | ``{2}`` | ``{3}`` |" -f $fkParts[0], $fkParts[1], $backendApis[$bk].File, $frontendCalls[$fk].File))
}
[void]$sb.AppendLine("")

$utf8Bom = New-Object System.Text.UTF8Encoding $true
[System.IO.File]::WriteAllText($reportPath, $sb.ToString(), $utf8Bom)
Write-Host ("📄 报告已生成: {0}" -f $reportPath) -ForegroundColor Cyan
Write-Host ""
