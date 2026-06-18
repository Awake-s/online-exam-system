#requires -Version 5.1
# ====================================================================
# 接口画像自动生成器 v2.0 阶段 1
# 扫描所有 Controller，提取 HTTP 接口元数据，生成 Markdown 接口画像
# 设计原则：使用 -f 格式化操作符避免 PowerShell 变量插值歧义
# ====================================================================

$ErrorActionPreference = "Stop"

$ProjectRoot   = "d:\Java Projects\在线考试系统"
$ControllerDir = "$ProjectRoot\exam-system\src\main\java\com\exam\controller"
$OutputDir     = "$ProjectRoot\doc\接口测试\06_源码追溯审计\接口画像"

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

# 反引号字符（避免 PowerShell 字符串内反引号转义）
$BT = [char]96

function Parse-Controller {
    param([string]$FilePath)

    $lines = Get-Content $FilePath
    $content = $lines -join "`n"
    $name = [System.IO.Path]::GetFileNameWithoutExtension($FilePath)

    $classBase = ""
    $classAuth = ""
    if ($content -match '@RequestMapping\("([^"]+)"\)') {
        $classBase = $matches[1]
    }
    if ($content -match '(?s)@RequestMapping\("[^"]+"\)\s*\r?\n@PreAuthorize\("([^"]+)"\)') {
        $classAuth = $matches[1]
    }

    $methods = @()
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match '@(GetMapping|PostMapping|PutMapping|DeleteMapping)(?:\("([^"]*)"\))?') {
            $verb = ($matches[1] -replace 'Mapping', '').ToUpper()
            $subPath = $matches[2]
            $lineNum = $i + 1

            $methodAuth = ""
            $startScan = [Math]::Max(0, $i - 5)
            for ($j = $startScan; $j -lt $i; $j++) {
                if ($lines[$j] -match '@PreAuthorize\("([^"]+)"\)') {
                    $methodAuth = $matches[1]
                }
            }

            $methodSig = ""
            $methodName = ""
            $params = @()
            $endScan = [Math]::Min($lines.Count, $i + 15)
            for ($k = $i + 1; $k -lt $endScan; $k++) {
                $sigLine = $lines[$k]
                if ($sigLine -match 'public\s+(.+?)\s+(\w+)\s*\(') {
                    $methodSig = $matches[1].Trim()
                    $methodName = $matches[2]
                    $paramText = ""
                    $paramEnd = [Math]::Min($lines.Count, $k + 10)
                    for ($p = $k; $p -lt $paramEnd; $p++) {
                        $paramText += $lines[$p] + " "
                        if ($lines[$p] -match '\)\s*\{') { break }
                    }
                    if ($paramText -match '\(([^)]*)\)') {
                        $rawParams = $matches[1].Trim()
                        if ($rawParams) {
                            $params = ($rawParams -split ',\s*') | ForEach-Object { $_.Trim() }
                        }
                    }
                    break
                }
            }

            $finalAuth = if ($methodAuth) {
                $methodAuth
            } elseif ($classAuth) {
                $classAuth
            } else {
                "公开 / 默认认证"
            }

            $methods += [PSCustomObject]@{
                Verb       = $verb
                Path       = ($classBase + $subPath)
                MethodName = $methodName
                ReturnType = $methodSig
                Params     = $params
                Auth       = $finalAuth
                LineNum    = $lineNum
            }
        }
    }

    return @{
        Name      = $name
        ClassBase = $classBase
        ClassAuth = $classAuth
        Methods   = $methods
        FilePath  = $FilePath
    }
}

function Wrap-Bt {
    param([string]$Text)
    return ($BT + $Text + $BT)
}

function Write-ControllerPortrait {
    param([hashtable]$Ctrl)

    $name = $Ctrl.Name
    $sb = New-Object System.Text.StringBuilder

    [void]$sb.AppendLine(("# {0} 接口画像" -f $name))
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine(("> **源文件**: {0}  " -f (Wrap-Bt $Ctrl.FilePath)))
    [void]$sb.AppendLine(("> **路径前缀**: {0}  " -f (Wrap-Bt $Ctrl.ClassBase)))

    $authDisplay = if ($Ctrl.ClassAuth) { Wrap-Bt $Ctrl.ClassAuth } else { "_(未声明类级，按方法级或默认认证)_" }
    [void]$sb.AppendLine(("> **类级权限**: {0}  " -f $authDisplay))
    [void]$sb.AppendLine(("> **接口数量**: {0}" -f $Ctrl.Methods.Count))
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("---")
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("## 接口清单")
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |")
    [void]$sb.AppendLine("|---|---|---|---|---|---|---|")

    $i = 0
    foreach ($m in $Ctrl.Methods) {
        $i++
        $row = "| {0} | {1} | {2} | {3} | {4} | {5} | L{6} |" -f `
            $i, $m.Verb, (Wrap-Bt $m.Path), $m.Auth, (Wrap-Bt $m.MethodName), (Wrap-Bt $m.ReturnType), $m.LineNum
        [void]$sb.AppendLine($row)
    }

    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("---")
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("## 接口详细签名")
    [void]$sb.AppendLine("")

    $i = 0
    foreach ($m in $Ctrl.Methods) {
        $i++
        [void]$sb.AppendLine(("### {0}. {1} {2}" -f $i, $m.Verb, (Wrap-Bt $m.Path)))
        [void]$sb.AppendLine("")

        $loc = "{0}:{1}" -f $Ctrl.FilePath, $m.LineNum
        [void]$sb.AppendLine(("- **源码定位**: {0}" -f (Wrap-Bt $loc)))

        $sig = "{0} {1}(...)" -f $m.ReturnType, $m.MethodName
        [void]$sb.AppendLine(("- **Java 方法**: {0}" -f (Wrap-Bt $sig)))
        [void]$sb.AppendLine(("- **鉴权要求**: {0}" -f $m.Auth))

        if ($m.Params.Count -gt 0) {
            [void]$sb.AppendLine("- **参数清单**:")
            foreach ($p in $m.Params) {
                [void]$sb.AppendLine(("  - {0}" -f (Wrap-Bt $p)))
            }
        } else {
            [void]$sb.AppendLine("- **参数清单**: _(无参数)_")
        }
        [void]$sb.AppendLine("")
    }

    [void]$sb.AppendLine("---")
    [void]$sb.AppendLine("")
    $now = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    [void]$sb.AppendLine(("**生成时间**: {0}  " -f $now))
    [void]$sb.AppendLine(("**生成方式**: {0} 自动扫描源码" -f (Wrap-Bt "generate-api-portraits.ps1")))

    return $sb.ToString()
}

# ============================================================
# 主流程
# ============================================================
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  接口画像自动生成器 v2.0 阶段 1" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host ("扫描目录: {0}" -f $ControllerDir) -ForegroundColor Yellow
Write-Host ("输出目录: {0}" -f $OutputDir) -ForegroundColor Yellow
Write-Host ""

$controllers = Get-ChildItem $ControllerDir -Filter "*Controller.java"
$totalApis = 0
$summary = @()

foreach ($file in ($controllers | Sort-Object Name)) {
    $ctrl = Parse-Controller -FilePath $file.FullName
    $totalApis += $ctrl.Methods.Count

    $summary += [PSCustomObject]@{
        Controller = $ctrl.Name
        Base       = $ctrl.ClassBase
        ClassAuth  = $ctrl.ClassAuth
        ApiCount   = $ctrl.Methods.Count
    }

    if ($ctrl.Methods.Count -gt 0) {
        $portrait = Write-ControllerPortrait -Ctrl $ctrl
        $outFile = Join-Path $OutputDir ("{0}.md" -f $ctrl.Name)
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($outFile, $portrait, $utf8NoBom)
        Write-Host ("  OK {0,-26} 接口数={1,3}  -> {2}.md" -f $ctrl.Name, $ctrl.Methods.Count, $ctrl.Name) -ForegroundColor Green
    } else {
        Write-Host ("  -- {0,-26} 无 HTTP 接口" -f $ctrl.Name) -ForegroundColor Gray
    }
}

# 总览索引
$idxSb = New-Object System.Text.StringBuilder
[void]$idxSb.AppendLine("# 接口画像 总览索引")
[void]$idxSb.AppendLine("")
$now = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
[void]$idxSb.AppendLine(("> 自动生成于 {0}，源自 exam-system/src/main/java/com/exam/controller/" -f $now))
[void]$idxSb.AppendLine("")
[void]$idxSb.AppendLine("## 统计")
[void]$idxSb.AppendLine("")
[void]$idxSb.AppendLine(("- Controller 总数: {0} 个" -f $controllers.Count))
[void]$idxSb.AppendLine(("- HTTP 接口总数: {0} 个" -f $totalApis))
[void]$idxSb.AppendLine("")
[void]$idxSb.AppendLine("## Controller 清单")
[void]$idxSb.AppendLine("")
[void]$idxSb.AppendLine("| # | Controller | 路径前缀 | 类级权限 | 接口数 | 画像文档 |")
[void]$idxSb.AppendLine("|---|---|---|---|---|---|")

$idx = 0
foreach ($s in ($summary | Sort-Object Controller)) {
    $idx++
    $authDisplay = if ($s.ClassAuth) { Wrap-Bt $s.ClassAuth } else { "_(公开/方法级)_" }
    $linkDisplay = if ($s.ApiCount -gt 0) { "[{0}.md]({0}.md)" -f $s.Controller } else { "_(无)_" }

    $row = "| {0} | {1} | {2} | {3} | {4} | {5} |" -f `
        $idx, (Wrap-Bt $s.Controller), (Wrap-Bt $s.Base), $authDisplay, $s.ApiCount, $linkDisplay
    [void]$idxSb.AppendLine($row)
}

[void]$idxSb.AppendLine("")
[void]$idxSb.AppendLine("---")
[void]$idxSb.AppendLine(("生成器: {0}" -f (Wrap-Bt "doc/接口测试/03_PowerShell脚本/generate-api-portraits.ps1")))

$indexFile = Join-Path $OutputDir "README.md"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($indexFile, $idxSb.ToString(), $utf8NoBom)

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  生成总览" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host ("  共 {0} 个 Controller，{1} 个 HTTP 接口" -f $controllers.Count, $totalApis) -ForegroundColor White
Write-Host ("  总览索引: {0}" -f $indexFile) -ForegroundColor Cyan
Write-Host ""
Write-Host "OK 阶段 1 接口画像生成完成" -ForegroundColor Green
