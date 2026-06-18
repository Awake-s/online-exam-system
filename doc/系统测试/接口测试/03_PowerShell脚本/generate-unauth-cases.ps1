#requires -Version 5.1
# ====================================================================
# 未登录用例批量生成器 v1.0
# 为所有需鉴权的后端接口生成「无 Token → HTTP 401」反向用例
#
# 排除（来自 SecurityConfig.java:51 permitAll）：
#   POST /api/auth/login
#   GET  /api/auth/captcha
#   POST /api/auth/logout
#
# 去重：检查 Collection 中是否已存在 (method + path) 相同且为反向用例
# ====================================================================

$ErrorActionPreference = "Stop"

$ColPath       = "d:\Java Projects\在线考试系统\doc\接口测试\04_Postman脚本与数据\exam-system.postman_collection.json"
$BackendDir    = "d:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  未登录用例批量生成器 v1.0" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# 1. 公开接口白名单（不需鉴权 → 不生成 401 用例）
# ============================================================
$publicApis = @(
    "POST /api/auth/login",
    "GET /api/auth/captcha",
    "POST /api/auth/logout"
)

# ============================================================
# 2. 扫描后端 108 个接口
# ============================================================
$backendApis = @{}
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
            if (-not $backendApis.ContainsKey($key)) {
                $backendApis[$key] = @{
                    File           = $file.Name
                    LineNum        = $i + 1
                    ControllerName = ($file.Name -replace '\.java$', '')
                }
            }
        }
    }
}

# 按 ControllerName 分组（用于决定追加到哪个 folder）
$apisByCtrl = @{}
foreach ($k in $backendApis.Keys) {
    $ctrl = $backendApis[$k].ControllerName
    if (-not $apisByCtrl.ContainsKey($ctrl)) {
        $apisByCtrl[$ctrl] = @()
    }
    $apisByCtrl[$ctrl] += $k
}

Write-Host ("[1/4] 后端: {0} 个接口分布在 {1} 个 Controller" -f $backendApis.Count, $apisByCtrl.Count) -ForegroundColor Green

# ============================================================
# 3. 加载 Collection，提取已有用例（去重依据）
# ============================================================
$col = Get-Content $ColPath -Raw -Encoding UTF8 | ConvertFrom-Json

# 收集已有用例的 (method + path 归一化)
$existingCases = @{}  # key = "VERB /api/path/PLACEHOLDER" → name
function Walk($node) {
    if ($node.item) {
        foreach ($child in $node.item) {
            if ($child.item) {
                Walk $child
            } elseif ($child.request) {
                $rawUrl = if ($child.request.url -is [string]) { $child.request.url } else { $child.request.url.raw }
                $path = $rawUrl -replace '\{\{baseUrl\}\}', '' -replace '\?.*', ''
                $pathNorm = $path -replace '\{\{[^}]+\}\}', '___PLACEHOLDER___'
                $pathNorm = $pathNorm -replace '/\d+(?=/|$)', '/___PLACEHOLDER___'
                $method = $child.request.method.ToUpper()
                $key = "{0} {1}" -f $method, $pathNorm

                # 标记是否已有 401/未登录类用例
                $isUnauthCase = ($child.name -match '未登录|无 ?Token|HTTP 401' -or
                                 ($child.event -and ($child.event.script.exec -join "`n") -match 'expect.*status.*401'))

                if (-not $script:existingCases.ContainsKey($key)) {
                    $script:existingCases[$key] = @{ HasUnauth = $isUnauthCase; Name = $child.name }
                } elseif ($isUnauthCase) {
                    $script:existingCases[$key].HasUnauth = $true
                }
            }
        }
    }
}
Walk $col

$existingTotal = $existingCases.Count
$existingUnauth = ($existingCases.Values | Where-Object { $_.HasUnauth }).Count
Write-Host ("[2/4] Collection: 已有 {0} 个用例（含 {1} 个未登录类）" -f $existingTotal, $existingUnauth) -ForegroundColor Green

# ============================================================
# 4. 决定每个 Controller 对应的 folder
# ============================================================
$controllerToFolder = @{
    "AuthController"        = "认证模块"
    "ProfileController"     = "个人资料"
    "UserController"        = "用户管理"
    "ClassController"       = "班级管理"
    "QuestionController"    = "题库"
    "PaperController"       = "试卷"
    "ExamController"        = "考试 \(Exam"
    "StudentExamController" = "学生考试"
    "MarkingController"     = "阅卷"
    "ScoreController"       = "成绩"
}

# ============================================================
# 5. 为缺失未登录用例的接口生成 401 用例
# ============================================================
$generated = 0
$skipped_public = 0
$skipped_existing = 0
$noFolder = 0

# Controller → 是否新建 folder 的标记
$newFolders = @{}

foreach ($apiKey in ($backendApis.Keys | Sort-Object)) {
    # 公开接口跳过
    if ($apiKey -in $publicApis) {
        $skipped_public++
        continue
    }

    # 归一化 path 以查询去重
    $apiParts = $apiKey -split ' ', 2
    $method = $apiParts[0]
    $path = $apiParts[1]
    $pathForDedup = $path -replace '\{[^}]+\}', '___PLACEHOLDER___'
    $dedupKey = "{0} {1}" -f $method, $pathForDedup

    if ($existingCases.ContainsKey($dedupKey) -and $existingCases[$dedupKey].HasUnauth) {
        $skipped_existing++
        continue
    }

    $ctrlName = $backendApis[$apiKey].ControllerName
    $folderPattern = $controllerToFolder[$ctrlName]

    # ---- 构造 url：把 {id} 路径占位符替换成 9001（避免触发 404）----
    $sampleUrlPath = $path -replace '\{[^}]+\}', '9001'
    $urlRaw = "{{baseUrl}}$sampleUrlPath"
    $pathSegs = ($sampleUrlPath.TrimStart('/').Split('/') | Where-Object { $_ -ne '' })

    # ---- 构造 item ----
    $item = [PSCustomObject]@{
        name    = ("🔒 {0} {1} · 鉴权 · 未登录" -f $method, $path)
        event   = @(
            [PSCustomObject]@{
                listen = "test"
                script = [PSCustomObject]@{
                    type = "text/javascript"
                    exec = @(
                        ("// 源码: @{0}.java:{1} 需鉴权" -f $ctrlName, $backendApis[$apiKey].LineNum),
                        "// 无 Token → SecurityConfig.java:60-65 authenticationEntryPoint → HTTP 401",
                        "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));"
                    )
                }
            }
        )
        request = [PSCustomObject]@{
            method = $method
            header = @()
            url    = [PSCustomObject]@{
                raw  = $urlRaw
                host = @("{{baseUrl}}")
                path = $pathSegs
            }
        }
    }

    # ---- 找 folder 追加 ----
    $added = $false
    if ($folderPattern) {
        foreach ($folder in $col.item) {
            if ($folder.name -match $folderPattern) {
                $itemList = [System.Collections.ArrayList]@($folder.item)
                [void]$itemList.Add($item)
                $folder.item = $itemList.ToArray()
                $added = $true
                $generated++
                break
            }
        }
    }

    if (-not $added) {
        # 没有匹配的 folder，新建一个 "<ControllerName> · 鉴权扩展" folder
        $newFolderName = ("🔐 {0} · 鉴权扩展" -f $ctrlName)
        if (-not $newFolders.ContainsKey($newFolderName)) {
            $newFolder = [PSCustomObject]@{
                name        = $newFolderName
                description = ("自动生成的未登录鉴权用例集合（来自 generate-unauth-cases.ps1）")
                item        = @()
            }
            $itemList = [System.Collections.ArrayList]@($col.item)
            [void]$itemList.Add($newFolder)
            $col.item = $itemList.ToArray()
            $newFolders[$newFolderName] = $newFolder
        }
        # 找到刚创建的 folder（在 $col.item 数组里）
        foreach ($folder in $col.item) {
            if ($folder.name -eq $newFolderName) {
                $itemList = [System.Collections.ArrayList]@($folder.item)
                [void]$itemList.Add($item)
                $folder.item = $itemList.ToArray()
                $generated++
                $noFolder++
                break
            }
        }
    }
}

# ============================================================
# 6. 写回 Collection
# ============================================================
$json = $col | ConvertTo-Json -Depth 30
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($ColPath, $json, $utf8NoBom)

Write-Host ("[3/4] 已生成 {0} 个未登录用例" -f $generated) -ForegroundColor Green
Write-Host ("    跳过 (公开接口): {0}" -f $skipped_public) -ForegroundColor Gray
Write-Host ("    跳过 (已有未登录用例): {0}" -f $skipped_existing) -ForegroundColor Gray
Write-Host ("    新增到「鉴权扩展」folder: {0}" -f $noFolder) -ForegroundColor Gray
Write-Host ("[4/4] Collection 已更新: {0}" -f $ColPath) -ForegroundColor Green
Write-Host ""
Write-Host "下一步：跑 Newman 验证" -ForegroundColor Yellow
