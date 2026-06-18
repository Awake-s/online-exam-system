# 解析 Apifox HTML 报告：提取每个用例的 实际HTTP状态码 + 断言通过/失败 + URL
# 用途：定位失败用例的根本原因 / 解析全量报告

param(
    [string]$ReportPath = ""
)

# 默认值：自动选取 07_测试报告与论文稿/ 下最新的 apifox-reports-*.html
if (-not $ReportPath) {
    $reportDir = "d:\Java Projects\在线考试系统\doc\系统测试\接口测试\07_测试报告与论文稿"
    $latest = Get-ChildItem -Path $reportDir -Filter "apifox-reports-*.html" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latest) {
        Write-Host "❌ 未找到任何 apifox-reports-*.html 报告" -ForegroundColor Red
        exit 1
    }
    $ReportPath = $latest.FullName
    Write-Host "📄 自动选取最新 Apifox 报告: $($latest.Name)" -ForegroundColor Cyan
}

if (-not (Test-Path $ReportPath)) {
    Write-Host "❌ 报告文件不存在: $ReportPath" -ForegroundColor Red
    exit 1
}

$html = Get-Content $ReportPath -Encoding UTF8 -Raw

# 用例分隔模式：每个 card 以 "<h5 class=\"card-header\">" 开始
$cases = [regex]::Split($html, '(?=<h5 class="card-header">)')

$results = @()
foreach ($block in $cases) {
    if ($block -notmatch '<h5 class="card-header">\s*([^<]+)\s*</h5>') { continue }
    $name = $Matches[1].Trim()

    $url = if ($block -match 'target="_blank">(http[^<]+)</a>') { $Matches[1].Trim() } else { 'N/A' }
    $method = if ($block -match '<div class="col-md-4">Method</div>\s*<div class="col-md-8">([^<]+)</div>') { $Matches[1] } else { 'N/A' }
    $httpCode = if ($block -match 'HTTP 状态码</div>\s*<div class="col-md-8">(\d+)</div>') { $Matches[1] } else { 'NO_RESPONSE' }
    $assertPass = if ($block -match '断言通过数</div>\s*<div class="col-md-8">(\d+)</div>') { [int]$Matches[1] } else { 0 }
    $assertFail = if ($block -match '断言失败数</div>\s*<div class="col-md-8">(\d+)</div>') { [int]$Matches[1] } else { 0 }
    $duration = if ($block -match '耗时</div>\s*<div class="col-md-8">([^<]+)</div>') { $Matches[1] } else { 'N/A' }

    # 整体用例结果：HTTP无响应 / HTTP错误码 / 断言失败 → 失败
    $caseStatus = if ($httpCode -eq 'NO_RESPONSE') {
        'FAIL_NETWORK'
    } elseif ($assertFail -gt 0) {
        'FAIL_ASSERT'
    } elseif ([int]$httpCode -ge 500) {
        'FAIL_5XX'
    } else {
        'PASS'
    }

    $results += [PSCustomObject]@{
        Name = $name
        Method = $method
        Url = $url
        HttpCode = $httpCode
        AssertPass = $assertPass
        AssertFail = $assertFail
        Duration = $duration
        Status = $caseStatus
    }
}

Write-Host ""
Write-Host "==================== 总览 ====================" -ForegroundColor Cyan
Write-Host "解析到用例数: $($results.Count)"
$grp = $results | Group-Object Status
foreach ($g in $grp) {
    $color = if ($g.Name -eq 'PASS') { 'Green' } else { 'Red' }
    Write-Host ("  {0,-15} : {1}" -f $g.Name, $g.Count) -ForegroundColor $color
}

Write-Host ""
Write-Host "==================== HTTP 状态码分布 ====================" -ForegroundColor Cyan
$results | Group-Object HttpCode | Sort-Object Count -Descending | ForEach-Object {
    Write-Host ("  HTTP {0,-15} : {1} 用例" -f $_.Name, $_.Count)
}

Write-Host ""
Write-Host "==================== 失败用例明细（前30条）====================" -ForegroundColor Yellow
$results | Where-Object Status -ne 'PASS' | Select-Object -First 30 | Format-Table Name, Method, HttpCode, AssertFail, Status -AutoSize

# 导出全量 CSV
$csvOut = "d:\Java Projects\在线考试系统\doc\系统测试\接口测试\07_测试报告与论文稿\report-analysis.csv"
$results | Export-Csv -Path $csvOut -Encoding UTF8 -NoTypeInformation
Write-Host ""
Write-Host "全量结果已导出：$csvOut" -ForegroundColor Green
