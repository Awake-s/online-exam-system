#requires -Version 5.1
# ====================================================================
# Collection 反向用例增强器 v2.0
# 在 v1 Collection 基础上批量注入未登录 / 越权 / 参数错误 / 业务异常用例
# 让反向用例占比从 27.6% 拉到 ≥ 60%，所有断言可追溯到 SecurityConfig / GlobalExceptionHandler
# ====================================================================

$ErrorActionPreference = "Stop"

$ColPath = "d:\Java Projects\在线考试系统\doc\接口测试\04_Postman脚本与数据\exam-system.postman_collection.json"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  Collection 反向用例增强器 v2.0" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# 读 Collection
$col = Get-Content $ColPath -Raw -Encoding UTF8 | ConvertFrom-Json

# ============================================================
# 工具函数：构造 Postman item 对象
# ============================================================
function New-PostmanItem {
    param(
        [string]$Name,
        [string]$Method,
        [string]$UrlPath,        # 不含 baseUrl，如 /api/user/list
        [string]$Token = $null,  # 可选 token 类型: adminToken / teacherToken / studentToken / "" (无 token)
        [string]$BadToken = $null, # 错误的 token 字符串
        [string]$Body = $null,
        [string[]]$QueryParams = @(),  # 例: @("page=1", "size=10")
        [string[]]$Tests = @()         # 测试脚本行
    )

    $headers = @()

    if ($Token -and $Token -ne "") {
        $headers += [PSCustomObject]@{
            key   = "Authorization"
            value = "Bearer {{$Token}}"
        }
    } elseif ($BadToken) {
        $headers += [PSCustomObject]@{
            key   = "Authorization"
            value = $BadToken
        }
    }

    if ($Body) {
        $headers += [PSCustomObject]@{
            key   = "Content-Type"
            value = "application/json"
        }
    }

    $urlRaw = "{{baseUrl}}$UrlPath"
    if ($QueryParams.Count -gt 0) {
        $urlRaw += "?" + ($QueryParams -join "&")
    }

    # 构造 url 对象
    $pathSegs = ($UrlPath.TrimStart('/').Split('/') | Where-Object { $_ -ne '' })
    $urlObj = [PSCustomObject]@{
        raw  = $urlRaw
        host = @("{{baseUrl}}")
        path = $pathSegs
    }

    if ($QueryParams.Count -gt 0) {
        $queryArr = @()
        foreach ($q in $QueryParams) {
            $kv = $q -split '=', 2
            $queryArr += [PSCustomObject]@{ key = $kv[0]; value = $kv[1] }
        }
        $urlObj | Add-Member -NotePropertyName query -NotePropertyValue $queryArr
    }

    $request = [PSCustomObject]@{
        method = $Method
        header = $headers
    }

    if ($Body) {
        $request | Add-Member -NotePropertyName body -NotePropertyValue ([PSCustomObject]@{
            mode = "raw"
            raw  = $Body
        })
    }

    $request | Add-Member -NotePropertyName url -NotePropertyValue $urlObj

    $event = @(
        [PSCustomObject]@{
            listen = "test"
            script = [PSCustomObject]@{
                type = "text/javascript"
                exec = $Tests
            }
        }
    )

    return [PSCustomObject]@{
        name    = $Name
        event   = $event
        request = $request
    }
}

# ============================================================
# 工具函数：在指定 folder 追加 item
# ============================================================
function Add-ItemToFolder {
    param($Collection, [string]$FolderNamePattern, $Item)

    foreach ($folder in $Collection.item) {
        if ($folder.name -match $FolderNamePattern) {
            # 转成可变 ArrayList
            $itemList = [System.Collections.ArrayList]@($folder.item)
            [void]$itemList.Add($Item)
            $folder.item = $itemList.ToArray()
            return $true
        }
    }
    return $false
}

# ============================================================
# 反向用例清单
# ============================================================

$reverseCases = @()

# ---------- Auth 模块 ----------
$reverseCases += @{
    Folder = "认证模块"
    Item = New-PostmanItem -Name "❌ POST /auth/login · 反向 · 用户名为空" `
        -Method "POST" -UrlPath "/api/auth/login" `
        -Body '{ "username": "", "password": "12345678" }' `
        -Tests @(
            "// 源码: @LoginRequest.java 字段 @NotBlank(message=`"用户名不能为空`")",
            "// → MethodArgumentNotValidException → GlobalExceptionHandler.java:25-31 → code=400",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "const json = pm.response.json();",
            "pm.test('业务码 400 (参数校验失败)', () => pm.expect(json.code).to.equal(400));",
            "pm.test('错误信息含「用户名」', () => pm.expect(json.message).to.include('用户名'));"
        )
}

$reverseCases += @{
    Folder = "认证模块"
    Item = New-PostmanItem -Name "❌ POST /auth/login · 反向 · 密码为空" `
        -Method "POST" -UrlPath "/api/auth/login" `
        -Body '{ "username": "it_admin", "password": "" }' `
        -Tests @(
            "// 源码: @LoginRequest.java 字段 @NotBlank(message=`"密码不能为空`")",
            "// → MethodArgumentNotValidException → GlobalExceptionHandler.java:25-31 → code=400",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "const json = pm.response.json();",
            "pm.test('业务码 400 (参数校验失败)', () => pm.expect(json.code).to.equal(400));",
            "pm.test('错误信息含「密码」', () => pm.expect(json.message).to.include('密码'));"
        )
}

$reverseCases += @{
    Folder = "认证模块"
    Item = New-PostmanItem -Name "🔒 GET /auth/info · 鉴权 · 错误 Token 格式" `
        -Method "GET" -UrlPath "/api/auth/info" `
        -BadToken "Bearer xxx.invalid.token" `
        -Tests @(
            "// 源码: @JwtAuthenticationFilter.java:32-50 解析失败 → SecurityContext 无 Authentication",
            "// → SecurityConfig.java:60-65 authenticationEntryPoint → HTTP 401",
            "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));",
            "pm.test('业务码 401', () => pm.expect(pm.response.json().code).to.equal(401));"
        )
}

# ---------- 用户管理 ----------
$reverseCases += @{
    Folder = "用户管理"
    Item = New-PostmanItem -Name "🔒 GET /user/list · 鉴权 · 未登录" `
        -Method "GET" -UrlPath "/api/user/list" -QueryParams @("page=1", "size=10") `
        -Tests @(
            "// 源码: @UserController.java:25 @PreAuthorize(`"hasRole('ADMIN')`")",
            "// 无 Token → SecurityConfig.java:60-65 → HTTP 401",
            "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));",
            "pm.test('业务码 401', () => pm.expect(pm.response.json().code).to.equal(401));"
        )
}

$reverseCases += @{
    Folder = "用户管理"
    Item = New-PostmanItem -Name "🔒 GET /user/list · 越权 · 学生角色访问管理员接口" `
        -Method "GET" -UrlPath "/api/user/list" -QueryParams @("page=1", "size=10") `
        -Token "studentToken" `
        -Tests @(
            "// 源码: @UserController.java:25 @PreAuthorize(`"hasRole('ADMIN')`") 拒绝 STUDENT",
            "// → AccessDeniedException → GlobalExceptionHandler.java:84-87 → HTTP 200 + code=403",
            "pm.test('HTTP 200 (越权返回 200 + 业务码 403)', () => pm.response.to.have.status(200));",
            "const json = pm.response.json();",
            "pm.test('业务码 403 (无权限)', () => pm.expect(json.code).to.equal(403));",
            "pm.test('错误信息含「无权限」', () => pm.expect(json.message).to.include('无权限'));"
        )
}

$reverseCases += @{
    Folder = "用户管理"
    Item = New-PostmanItem -Name "🔒 POST /user/save · 越权 · 教师角色尝试创建用户" `
        -Method "POST" -UrlPath "/api/user/save" `
        -Token "teacherToken" `
        -Body '{ "username":"hack","password":"123456","realName":"测试","roleId":3 }' `
        -Tests @(
            "// 源码: @UserController.java POST /save 仅 ADMIN 可调用",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

# ---------- 题库 ----------
$reverseCases += @{
    Folder = "题库"
    Item = New-PostmanItem -Name "🔒 GET /question/page · 鉴权 · 未登录" `
        -Method "GET" -UrlPath "/api/question/page" -QueryParams @("page=1", "size=10") `
        -Tests @(
            "// 无 Token → 401",
            "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));"
        )
}

$reverseCases += @{
    Folder = "题库"
    Item = New-PostmanItem -Name "🔒 POST /question/save · 越权 · 学生尝试新增题目" `
        -Method "POST" -UrlPath "/api/question/save" `
        -Token "studentToken" `
        -Body '{"subjectId":9001,"questionType":1,"content":"hack","options":"[]","answer":"A","score":5}' `
        -Tests @(
            "// 源码: @QuestionController.java @PreAuthorize 仅 TEACHER/ADMIN",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

$reverseCases += @{
    Folder = "题库"
    Item = New-PostmanItem -Name "❌ GET /question/{id} · 反向 · ID 不存在" `
        -Method "GET" -UrlPath "/api/question/999999" `
        -Token "teacherToken" `
        -Tests @(
            "// 源码: 题目 999999 不存在 → BusinessException → code=409",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "const json = pm.response.json();",
            "pm.test('业务码非 200 (题目不存在)', () => pm.expect(json.code).to.not.equal(200));",
            "pm.test('错误信息存在', () => pm.expect(json.message).to.exist.and.not.be.empty);"
        )
}

# ---------- 试卷 ----------
$reverseCases += @{
    Folder = "试卷"
    Item = New-PostmanItem -Name "🔒 GET /paper/page · 鉴权 · 未登录" `
        -Method "GET" -UrlPath "/api/paper/page" -QueryParams @("page=1", "size=10") `
        -Tests @(
            "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));"
        )
}

$reverseCases += @{
    Folder = "试卷"
    Item = New-PostmanItem -Name "🔒 POST /paper/save · 越权 · 学生尝试新增试卷" `
        -Method "POST" -UrlPath "/api/paper/save" `
        -Token "studentToken" `
        -Body '{"paperName":"hack","subjectId":9001,"totalScore":100,"passScore":60,"duration":60}' `
        -Tests @(
            "// 源码: @PaperController.java POST /save @PreAuthorize 仅 TEACHER/ADMIN",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

$reverseCases += @{
    Folder = "试卷"
    Item = New-PostmanItem -Name "❌ GET /paper/{id} · 反向 · ID 不存在" `
        -Method "GET" -UrlPath "/api/paper/999999" `
        -Token "teacherToken" `
        -Tests @(
            "// 源码: 试卷 999999 不存在 → BusinessException → code=409",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "const json = pm.response.json();",
            "pm.test('业务码非 200 (试卷不存在)', () => pm.expect(json.code).to.not.equal(200));"
        )
}

# ---------- 考试 ----------
$reverseCases += @{
    Folder = "考试 \(Exam"
    Item = New-PostmanItem -Name "🔒 GET /exam/list · 鉴权 · 未登录" `
        -Method "GET" -UrlPath "/api/exam/list" -QueryParams @("page=1", "size=10") `
        -Tests @(
            "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));"
        )
}

$reverseCases += @{
    Folder = "考试 \(Exam"
    Item = New-PostmanItem -Name "🔒 POST /exam/save · 越权 · 学生尝试创建考试" `
        -Method "POST" -UrlPath "/api/exam/save" `
        -Token "studentToken" `
        -Body '{"examName":"hack","paperId":9001,"classId":1,"startTime":"2026-12-01 09:00:00","endTime":"2026-12-01 11:00:00"}' `
        -Tests @(
            "// 源码: @ExamController.java POST /save @PreAuthorize 仅 TEACHER/ADMIN",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

# ---------- 学生考试 ----------
$reverseCases += @{
    Folder = "学生考试"
    Item = New-PostmanItem -Name "🔒 GET /student/exam/list · 鉴权 · 未登录" `
        -Method "GET" -UrlPath "/api/student/exam/list" -QueryParams @("page=1", "size=10") `
        -Tests @(
            "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));"
        )
}

$reverseCases += @{
    Folder = "学生考试"
    Item = New-PostmanItem -Name "🔒 GET /student/exam/list · 越权 · 教师角色访问学生接口" `
        -Method "GET" -UrlPath "/api/student/exam/list" -QueryParams @("page=1", "size=10") `
        -Token "teacherToken" `
        -Tests @(
            "// 源码: @StudentExamController.java @PreAuthorize 仅 STUDENT",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

# ---------- 阅卷 ----------
$reverseCases += @{
    Folder = "阅卷"
    Item = New-PostmanItem -Name "🔒 GET /marking/list/{examId} · 鉴权 · 未登录" `
        -Method "GET" -UrlPath "/api/marking/list/9001" `
        -Tests @(
            "pm.test('HTTP 401 Unauthorized', () => pm.response.to.have.status(401));"
        )
}

$reverseCases += @{
    Folder = "阅卷"
    Item = New-PostmanItem -Name "🔒 GET /marking/list/{examId} · 越权 · 学生角色访问阅卷接口" `
        -Method "GET" -UrlPath "/api/marking/list/9001" `
        -Token "studentToken" `
        -Tests @(
            "// 源码: @MarkingController.java @PreAuthorize 仅 TEACHER/ADMIN",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

# ---------- 成绩 ----------
$reverseCases += @{
    Folder = "成绩"
    Item = New-PostmanItem -Name "🔒 GET /score/class/{examId} · 越权 · 学生角色查教师接口" `
        -Method "GET" -UrlPath "/api/score/class/9001" `
        -Token "studentToken" `
        -Tests @(
            "// 源码: @ScoreController.java GET /class/{examId} @PreAuthorize 仅 TEACHER/ADMIN",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

$reverseCases += @{
    Folder = "成绩"
    Item = New-PostmanItem -Name "🔒 GET /score/my-scores · 越权 · 教师查学生个人成绩接口" `
        -Method "GET" -UrlPath "/api/score/my-scores" -QueryParams @("page=1", "size=10") `
        -Token "teacherToken" `
        -Tests @(
            "// 源码: @ScoreController.java GET /my-scores @PreAuthorize 仅 STUDENT",
            "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
            "pm.test('业务码 403 (越权)', () => pm.expect(pm.response.json().code).to.equal(403));"
        )
}

# ============================================================
# 把 25 个反向用例追加到 Collection
# ============================================================
Write-Host ("准备注入 {0} 个反向用例..." -f $reverseCases.Count) -ForegroundColor Yellow
Write-Host ""

$injected = 0
foreach ($rc in $reverseCases) {
    $ok = Add-ItemToFolder -Collection $col -FolderNamePattern $rc.Folder -Item $rc.Item
    if ($ok) {
        $injected++
        Write-Host ("  + [{0}] {1}" -f $rc.Folder, $rc.Item.name) -ForegroundColor Green
    } else {
        Write-Host ("  ! 未找到 folder 包含「{0}」" -f $rc.Folder) -ForegroundColor Red
    }
}

# 写回 Collection
$json = $col | ConvertTo-Json -Depth 30
# Postman 兼容性：用 Tab 缩进
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($ColPath, $json, $utf8NoBom)

Write-Host ""
Write-Host ("✓ 已注入 {0}/{1} 个反向用例" -f $injected, $reverseCases.Count) -ForegroundColor Green
Write-Host ("✓ Collection 已更新: {0}" -f $ColPath) -ForegroundColor Green
Write-Host ""
