# =====================================================================
#  冷冻对虾全产业链溯源系统 —— 一键启动（含手机扫码演示支持）
#
#  作用：
#    1) 自动探测本机局域网 IP（排除虚拟网卡），手机要连的就是它
#    2) 把它注入 TRACE_QR_BASE_URL，让后端生成的二维码指向正确的地址
#       —— 否则二维码里是 localhost，手机扫了会指向手机自己，打不开
#    3) 分别拉起后端(8080)与前端(5173)，vite 已配置 host:true 允许局域网访问
#    4) 打印手机该访问的地址
#
#  用法：右键本文件 →「使用 PowerShell 运行」
#        或在 PowerShell 里执行  .\start-demo.ps1
#
#  换网络环境（教室 WiFi ↔ 手机热点）后重跑本脚本即可，不用手改任何配置。
# =====================================================================

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

Write-Host ''
Write-Host '=== 冷冻对虾全产业链溯源系统 · 启动 ===' -ForegroundColor Cyan
Write-Host ''

# ---------------------------------------------------------------------
# 1. 探测本机局域网 IP
#    排除：回环(127.*)、自动私有地址(169.254.*)、以及虚拟网卡
#    （VMware / VirtualBox / Hyper-V 的地址手机连不上，必须排掉）
# ---------------------------------------------------------------------
function Get-LanCandidates {
    Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } |
        ForEach-Object {
            $adapter = Get-NetAdapter -InterfaceIndex $_.InterfaceIndex -ErrorAction SilentlyContinue
            [PSCustomObject]@{
                IP    = $_.IPAddress
                Alias = if ($adapter) { $adapter.InterfaceAlias } else { '(未知网卡)' }
                Desc  = if ($adapter) { "$($adapter.InterfaceDescription)" } else { '' }
            }
        } |
        # VPN 类网卡（Radmin / OpenVPN / 向日葵等）手机也连不上，一并排除
        Where-Object { $_.Desc -notmatch 'VMware|VirtualBox|Hyper-V|Loopback|Bluetooth|TAP-|Npcap|WSL|VPN|Radmin|Tunnel' }
}

# 打分：手机热点网段最优先，其次常见家用/办公网段，最后兜底
function Get-IPScore([string]$ip) {
    if ($ip -like '172.20.10.*') { return 3 }   # iPhone 个人热点
    if ($ip -like '192.168.*')   { return 2 }   # 家用/办公路由器
    if ($ip -like '10.*')        { return 1 }   # 企业网
    return 0
}

$candidates = @(Get-LanCandidates)
if ($candidates.Count -eq 0) {
    Write-Host '[错误] 没找到可用的局域网 IP。' -ForegroundColor Red
    Write-Host '       请确认已连接 WiFi 或已开启手机热点。' -ForegroundColor Red
    exit 1
}

$best = $candidates | Sort-Object -Property @{Expression={ Get-IPScore $_.IP }} -Descending | Select-Object -First 1

Write-Host '检测到的可用网卡：' -ForegroundColor Yellow
foreach ($c in $candidates) {
    $mark = if ($c.IP -eq $best.IP) { '  <- 使用这个' } else { '' }
    Write-Host ("  {0,-16} {1}{2}" -f $c.IP, $c.Alias, $mark)
}
Write-Host ''

$lanIp = $best.IP
$qrBase = "http://${lanIp}:5173/trace?code="
$phoneUrl = "http://${lanIp}:5173"

# ---------------------------------------------------------------------
# 2. 拉起后端（带上二维码地址）
# ---------------------------------------------------------------------
Write-Host "[1/2] 启动后端（端口 8080）..." -ForegroundColor Green
$backendCmd = "`$env:TRACE_QR_BASE_URL='$qrBase'; Set-Location '$root\Seafood_Traceability_System'; Write-Host '后端启动中，首次编译约需 1 分钟...' -ForegroundColor Cyan; mvn spring-boot:run"
Start-Process powershell -ArgumentList '-NoExit', '-Command', $backendCmd

# ---------------------------------------------------------------------
# 3. 拉起前端（vite 已配 host:true，监听所有网卡）
# ---------------------------------------------------------------------
Write-Host '[2/2] 启动前端（端口 5173）...' -ForegroundColor Green
$frontendCmd = "Set-Location '$root\Seafood_Traceability_System_FrontEnd'; Write-Host '前端启动中...' -ForegroundColor Cyan; npm run dev"
Start-Process powershell -ArgumentList '-NoExit', '-Command', $frontendCmd

# ---------------------------------------------------------------------
# 4. 提示
# ---------------------------------------------------------------------
Start-Sleep -Seconds 3
Write-Host ''
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host ' 已启动（两个新窗口，别关掉）' -ForegroundColor Cyan
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host ''
Write-Host "  电脑上访问：  http://localhost:5173" -ForegroundColor White
Write-Host "  手机上访问：  $phoneUrl" -ForegroundColor Yellow
Write-Host ''
Write-Host '  手机扫码演示步骤：' -ForegroundColor White
Write-Host "    1. 手机连到和电脑同一个 WiFi（或电脑开的热点）"
Write-Host "    2. 打开 $phoneUrl/trace"
Write-Host "    3. 输入溯源码（如 SHZ202602010002）点查询"
Write-Host "    4. 页面下方会出现二维码，另用手机扫它 → 直接出溯源结果"
Write-Host ''
Write-Host '  二维码里写的是：' -ForegroundColor DarkGray
Write-Host "    $qrBase<溯源码>" -ForegroundColor DarkGray
Write-Host ''
Write-Host '  手机连不上？多半是 Windows 防火墙拦了 5173 / 8080，' -ForegroundColor Yellow
Write-Host '  首次运行时弹的"允许网络访问"要点【专用网络+公用网络】都允许。' -ForegroundColor Yellow
Write-Host ''
