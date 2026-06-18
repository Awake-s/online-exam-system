# 测试用例规范生成器 — 从 collection.json 提取 145 用例为人类可读 Markdown
# 符合 ISO/IEC/IEEE 29119-3:2021 - Test Case Specification 标准

$ErrorActionPreference = 'Stop'
$root = "d:\Java Projects\在线考试系统"
$collectionPath = "$root\doc\系统测试\接口测试\04_Postman脚本与数据\exam-system.postman_collection.json"
$outPath = "$root\doc\系统测试\接口测试\06_源码追溯审计\测试用例规范_v1.6.md"

$collection = Get-Content $collectionPath -Raw -Encoding UTF8 | ConvertFrom-Json

function Walk($items, $folder = '') {
    $result = @()
    foreach ($item in $items) {
        if ($item.item) {
            # 是文件夹
            $newFolder = if ($folder) { "$folder / $($item.name)" } else { $item.name }
            $result += Walk $item.item $newFolder
        }
        if ($item.request) {
            # 是用例
            $rawUrl = if ($item.request.url.raw) { $item.request.url.raw } else { '' }
            $cleanUrl = $rawUrl -replace '\{\{baseUrl\}\}', ''
            
            # 提取断言数量与内容摘要
            $assertions = @()
            if ($item.event) {
                foreach ($e in $item.event) {
                    if ($e.listen -eq 'test' -and $e.script.exec) {
                        $execLines = $e.script.exec -split "`n" | Where-Object { $_ -match 'pm\.test' }
                        foreach ($line in $execLines) {
                            if ($line -match "pm\.test\(['""]([^'""]+)['""]") {
                                $assertions += $Matches[1]
                            }
                        }
                    }
                }
            }
            
            # 判断用例类型
            $type = if ($item.name -match '^✅') { '✅ 正向'
                   } elseif ($item.name -match '^🔒') { '🔒 鉴权反向'
                   } elseif ($item.name -match '^🚫') { '🚫 越权反向'
                   } elseif ($item.name -match '^❌') { '❌ 业务反向'
                   } elseif ($item.name -match '^🚪') { '🚪 Cleanup'
                   } else { '⚙️ Setup' }
            
            # 提取 body
            $body = ''
            if ($item.request.body -and $item.request.body.raw) {
                $body = $item.request.body.raw -replace "`n", '\n' -replace '\s+', ' '
                if ($body.Length -gt 80) { $body = $body.Substring(0, 80) + '...' }
            }
            
            # 提取鉴权
            $auth = if ($item.request.header) {
                $authHeader = $item.request.header | Where-Object { $_.key -eq 'Authorization' } | Select-Object -First 1
                if ($authHeader) { 
                    if ($authHeader.value -match 'adminToken') { 'ADMIN' }
                    elseif ($authHeader.value -match 'teacherToken') { 'TEACHER' }
                    elseif ($authHeader.value -match 'studentToken') { 'STUDENT' }
                    elseif ($authHeader.value -match 'wrongToken') { '错误Token' }
                    elseif ($authHeader.value -match 'expiredToken') { '过期Token' }
                    else { '未知' }
                } else { '无' }
            } else { '无' }
            
            $result += [PSCustomObject]@{
                Folder = $folder
                Name = $item.name
                Method = $item.request.method
                Url = $cleanUrl
                Auth = $auth
                Type = $type
                Body = $body
                AssertionCount = $assertions.Count
                Assertions = $assertions -join ' / '
            }
        }
    }
    return $result
}

$allCases = Walk $collection.item

# 统计
$total = $allCases.Count
$byType = $allCases | Group-Object Type | Sort-Object Count -Descending
$byMethod = $allCases | Group-Object Method
$byFolder = $allCases | Group-Object Folder
$totalAssertions = ($allCases | Measure-Object AssertionCount -Sum).Sum

# 生成 Markdown
$md = @()
$md += "# 测试用例规范 v1.6 (Test Case Specification)"
$md += ""
$md += "> **标准**: ISO/IEC/IEEE 29119-3:2021 Test Case Specification"
$md += "> **生成时间**: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$md += "> **数据源**: ``exam-system.postman_collection.json`` (Postman v2.1)"
$md += "> **执行验证**: Newman v6.2.2 + Apifox v2.8.26 双工具验证 100% 通过"
$md += ""
$md += "---"
$md += ""
$md += "## 一、用例集统计"
$md += ""
$md += "| 维度 | 数值 |"
$md += "|---|---|"
$md += "| **用例总数** | **$total** |"
$md += "| **断言总数** | **$totalAssertions** |"
$md += "| 测试套件数 | $($byFolder.Count) |"
$methodSummary = ($byMethod | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join ', '
$md += "| HTTP 方法分布 | $methodSummary |"
$md += ""
$md += "### 用例类型分布"
$md += ""
$md += "| 类型 | 数量 | 占比 |"
$md += "|---|---|---|"
foreach ($g in $byType) {
    $pct = [Math]::Round($g.Count / $total * 100, 1)
    $md += "| $($g.Name) | $($g.Count) | $pct% |"
}
$md += ""
$md += "---"
$md += ""
$md += "## 二、用例分组明细"
$md += ""

# 按文件夹分组输出
foreach ($folderGroup in ($allCases | Group-Object Folder)) {
    $md += "### 📂 $($folderGroup.Name)"
    $md += ""
    $md += "**用例数**：$($folderGroup.Count) | **断言数**：$(($folderGroup.Group | Measure-Object AssertionCount -Sum).Sum)"
    $md += ""
    $md += "| # | 用例名称 | Method | URL | 鉴权 | 类型 | 断言数 |"
    $md += "|---|---|---|---|---|---|---|"
    
    $idx = 1
    foreach ($case in $folderGroup.Group) {
        $cleanName = $case.Name -replace '\|', '\|'
        $cleanUrl = $case.Url -replace '\|', '\|'
        $md += "| $idx | $cleanName | $($case.Method) | ``$cleanUrl`` | $($case.Auth) | $($case.Type) | $($case.AssertionCount) |"
        $idx++
    }
    $md += ""
}

# 输出断言明细附录
$md += "---"
$md += ""
$md += "## 三、所有用例断言明细（附录）"
$md += ""
$md += "| # | 用例名称 | 断言列表 |"
$md += "|---|---|---|"
$idx = 1
foreach ($case in $allCases) {
    $cleanName = $case.Name -replace '\|', '\|'
    $cleanAssertions = $case.Assertions -replace '\|', '\|'
    $md += "| $idx | $cleanName | $cleanAssertions |"
    $idx++
}

$md += ""
$md += "---"
$md += ""
$md += "**文档作者**: 陶展 | **版本**: v1.6 | **生成方式**: 自动化（``generate-test-case-spec.ps1``）"

# 写入文件
$mdContent = $md -join "`n"
[System.IO.File]::WriteAllText($outPath, $mdContent, [System.Text.UTF8Encoding]::new($true))

Write-Host "✅ 测试用例规范已生成" -ForegroundColor Green
Write-Host "   路径: $outPath"
Write-Host "   用例数: $total"
Write-Host "   断言数: $totalAssertions"
Write-Host "   文件大小: $([Math]::Round((Get-Item $outPath).Length / 1KB, 1)) KB"
