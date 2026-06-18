# 接口测试覆盖率精确审计 v2 — 修正路径拼接 bug
# 改进点：每个方法只匹配一次，正确区分 @PostMapping 与 @PostMapping("/path")

$ErrorActionPreference = 'Stop'
$root = "d:\Java Projects\在线考试系统"
$collectionPath = "$root\doc\系统测试\接口测试\04_Postman脚本与数据\exam-system.postman_collection.json"
$srcPath = "$root\exam-system\src\main\java\com\exam\controller"

# ========== 1. 后端接口扫描（精确版）==========
$apis = @()

Get-ChildItem $srcPath -Filter "*.java" -Recurse | ForEach-Object {
    $controller = $_.BaseName
    $content = Get-Content $_.FullName -Raw

    # 提取 class @RequestMapping 的 base path
    $classBase = ''
    if ($content -match '@RequestMapping\s*\(\s*"([^"]+)"\s*\)') {
        $classBase = $Matches[1].TrimEnd('/')
    } elseif ($content -match '@RequestMapping\s*\(\s*value\s*=\s*"([^"]+)"\s*\)') {
        $classBase = $Matches[1].TrimEnd('/')
    } elseif ($content -match '@RequestMapping\s*\(\s*\{\s*"([^"]+)"') {
        $classBase = $Matches[1].TrimEnd('/')
    }

    # 用单一正则匹配方法上的 @XxxMapping，捕获括号内可选的 path
    # 匹配模式： @GetMapping  或  @GetMapping("...")  或  @GetMapping(value="...")
    $pattern = '@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)(\s*\(\s*(?:value\s*=\s*)?"([^"]*)"\s*[\),]|\s*(?=\s*[a-zA-Z]|\s*$))'
    $matches = [regex]::Matches($content, $pattern)
    foreach ($m in $matches) {
        $verb = ($m.Groups[1].Value -replace 'Mapping', '').ToUpper()
        $methodPath = if ($m.Groups[3].Success) { $m.Groups[3].Value.TrimEnd('/') } else { '' }
        
        $fullPath = if ($methodPath) {
            if ($methodPath.StartsWith('/')) { $classBase + $methodPath } else { $classBase + '/' + $methodPath }
        } else {
            $classBase
        }
        $fullPath = $fullPath -replace '//', '/'
        if (-not $fullPath.StartsWith('/')) { $fullPath = '/' + $fullPath }

        $apis += [PSCustomObject]@{
            Controller = $controller
            Method = $verb
            Path = $fullPath
            Key = "$verb $fullPath"
        }
    }
}
$backendApis = $apis | Sort-Object Key -Unique

Write-Host "`n========== 一、后端接口扫描（精确版）==========" -ForegroundColor Cyan
Write-Host "Controller 总数:    $((Get-ChildItem $srcPath -Filter '*.java' -Recurse).Count)"
Write-Host "后端接口总数:        $($backendApis.Count)"
Write-Host "  GET:    $(($backendApis | Where-Object Method -eq 'GET').Count)"
Write-Host "  POST:   $(($backendApis | Where-Object Method -eq 'POST').Count)"
Write-Host "  PUT:    $(($backendApis | Where-Object Method -eq 'PUT').Count)"
Write-Host "  DELETE: $(($backendApis | Where-Object Method -eq 'DELETE').Count)"
Write-Host "  PATCH:  $(($backendApis | Where-Object Method -eq 'PATCH').Count)"

# ========== 2. Apifox Collection 用例扫描 ==========
$collection = Get-Content $collectionPath -Raw -Encoding UTF8 | ConvertFrom-Json

function Get-AllCases($items) {
    $result = @()
    foreach ($item in $items) {
        if ($item.item) { $result += Get-AllCases $item.item }
        if ($item.request) {
            $rawUrl = if ($item.request.url.raw) { $item.request.url.raw } else { '' }
            $cleanPath = $rawUrl -replace '^\{\{[^}]+\}\}', '' -replace '\?.*$', ''
            $cleanPath = $cleanPath -replace '\{\{[^}]+\}\}', '{var}'
            $result += [PSCustomObject]@{
                Name = $item.name
                Method = $item.request.method
                Path = $cleanPath.TrimEnd('/')
                Key = "$($item.request.method) $($cleanPath.TrimEnd('/'))"
            }
        }
    }
    return $result
}

$cases = Get-AllCases $collection.item
Write-Host "`n========== 二、Collection 用例扫描 ==========" -ForegroundColor Cyan
Write-Host "用例总数:                  $($cases.Count)"
Write-Host "唯一 (Method+URL):         $(($cases | Sort-Object Key -Unique).Count)"

# ========== 3. 路径标准化匹配 ==========
function Normalize-Path($path) {
    $p = $path -replace '\{[^}]+\}', '{x}'  # 路径变量统一为 {x}
    $p = $p -replace '/\d+(?=/|$)', '/{x}'  # 数字 ID 标准化
    return $p.TrimEnd('/')
}

$backendIndex = @{}
foreach ($api in $backendApis) {
    $normKey = "$($api.Method) $(Normalize-Path $api.Path)"
    if (-not $backendIndex.ContainsKey($normKey)) {
        $backendIndex[$normKey] = $api
    }
}

$caseIndex = @{}
foreach ($case in $cases) {
    $normKey = "$($case.Method) $(Normalize-Path $case.Path)"
    if (-not $caseIndex.ContainsKey($normKey)) {
        $caseIndex[$normKey] = @()
    }
    $caseIndex[$normKey] += $case
}

# ========== 4. 覆盖率分析 ==========
$covered = @()
$uncovered = @()
foreach ($api in $backendApis) {
    $normKey = "$($api.Method) $(Normalize-Path $api.Path)"
    if ($caseIndex.ContainsKey($normKey)) {
        $covered += $api
    } else {
        $uncovered += $api
    }
}

Write-Host "`n========== 三、覆盖率分析 ==========" -ForegroundColor Cyan
$pct = [Math]::Round($covered.Count / $backendApis.Count * 100, 1)
Write-Host "已覆盖接口:       $($covered.Count) / $($backendApis.Count) = $pct%" -ForegroundColor Green
Write-Host "未覆盖接口:       $($uncovered.Count)" -ForegroundColor Yellow

# ========== 5. 未覆盖明细 ==========
if ($uncovered.Count -gt 0) {
    Write-Host "`n========== 四、未覆盖接口明细 ==========" -ForegroundColor Yellow
    $uncovered | Group-Object Controller | Sort-Object Count -Descending | ForEach-Object {
        Write-Host "`n📂 $($_.Name) ($($_.Count) 个未覆盖)" -ForegroundColor Yellow
        $_.Group | Sort-Object Method, Path | ForEach-Object {
            Write-Host "   $($_.Method.PadRight(7)) $($_.Path)"
        }
    }
}

# ========== 6. Controller 维度覆盖率 ==========
Write-Host "`n========== 五、Controller 维度覆盖率 ==========" -ForegroundColor Cyan
$stats = @()
foreach ($g in ($backendApis | Group-Object Controller)) {
    $tot = $g.Count
    $hit = ($covered | Where-Object Controller -eq $g.Name).Count
    $stats += [PSCustomObject]@{
        Controller = $g.Name
        Total = $tot
        Covered = $hit
        Missing = $tot - $hit
        'Pct%' = if ($tot -gt 0) { [Math]::Round($hit/$tot*100, 0) } else { 0 }
    }
}
$stats | Sort-Object 'Pct%', Controller | Format-Table -AutoSize

# ========== 7. 反向覆盖：collection 中是否有"路径不存在于后端"的用例 ==========
$ghost = @()
foreach ($caseKey in $caseIndex.Keys) {
    if (-not $backendIndex.ContainsKey($caseKey)) {
        # 不在后端，但允许是反向用例 (路径错或参数错故意构造)
        $ghost += $caseIndex[$caseKey][0]
    }
}
Write-Host "`n========== 六、Collection 中后端不存在的路径（可能是反向用例 / 已删接口）==========" -ForegroundColor Magenta
Write-Host "数量: $($ghost.Count)"
if ($ghost.Count -gt 0) {
    $ghost | Sort-Object Method, Path | Select-Object -First 10 | ForEach-Object {
        Write-Host "  $($_.Method.PadRight(7)) $($_.Path)   名称: $($_.Name)"
    }
    if ($ghost.Count -gt 10) { Write-Host "  ... ($($ghost.Count - 10) more)" }
}

# 导出（仅当有数据时）
$outDir = "$root\doc\系统测试\接口测试\07_测试报告与论文稿"
$backendApis | Export-Csv "$outDir\backend-apis.csv" -Encoding UTF8 -NoTypeInformation
$stats | Export-Csv "$outDir\controller-coverage.csv" -Encoding UTF8 -NoTypeInformation

Write-Host "`n详细数据导出:" -ForegroundColor Green
Write-Host "  backend-apis.csv         (后端 $($backendApis.Count) 接口完整列表)"
Write-Host "  controller-coverage.csv  (Controller 维度统计)"

if ($uncovered.Count -gt 0) {
    $uncovered | Export-Csv "$outDir\uncovered-apis.csv" -Encoding UTF8 -NoTypeInformation
    Write-Host "  uncovered-apis.csv       (未覆盖 $($uncovered.Count) 接口)" -ForegroundColor Yellow
} else {
    Write-Host "  uncovered-apis.csv       跳过（覆盖率 100%，无未覆盖接口）" -ForegroundColor Green
}

if ($ghost.Count -gt 0) {
    $ghost | Export-Csv "$outDir\ghost-cases.csv" -Encoding UTF8 -NoTypeInformation
    Write-Host "  ghost-cases.csv          (后端不存在的用例 $($ghost.Count) 条)" -ForegroundColor Yellow
} else {
    Write-Host "  ghost-cases.csv          跳过（无后端缺失用例）" -ForegroundColor Green
}
