<#
.SYNOPSIS
  E2E del token del WebSocket (D9 fase 4) contra el notification-dispatcher en Docker.
.DESCRIPTION
  Forja tokens HS256 con el JWT_SECRET del compose (misma regla Base64->UTF-8 que security/core) y abre
  conexiones con System.Net.WebSockets.ClientWebSocket contra ws://localhost:9000/ws/events.
  -Mode indica el modo esperado (LOG | ENFORCE). Con -Apply recrea SOLO notification-dispatcher con
  FINDU_WS_AUTH_MODE=<Mode> (override temporal por entorno, sin tocar el compose; sin -Build no reconstruye la
  imagen: si el contenedor corre codigo viejo el script lo detecta y falla). Al terminar con -Apply vuelve a LOG.
  Sale con codigo 1 si algun caso falla. No imprime ni guarda tokens.
.EXAMPLE
  ./scripts/ws-auth-e2e.ps1 -Mode ENFORCE -Apply
  ./scripts/ws-auth-e2e.ps1 -Mode LOG -Apply
#>
param(
    [ValidateSet('LOG', 'ENFORCE')][string]$Mode = 'LOG',
    [switch]$Apply,
    [switch]$Build,
    [string]$Container = 'findu-notification-dispatcher',
    [string]$Service = 'notification-dispatcher',
    [string]$WsBase = 'ws://localhost:9000/ws/events',
    [string]$JwtSecret = 'ZmluZHUtZGVmYXVsdC1zZWNyZXQta2V5LWF0LWxlYXN0LTI1Ni1iaXRzLWZvci1oczI1Ng==',
    [long]$OwnerAuthUserId = 2,   # perfil_proveedor id=1 tiene auth_user_id=2 en el stack local
    [long]$OwnerProveedorId = 1,
    [long]$OtherAuthUserId = 999  # sin perfil de proveedor
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

function ConvertTo-B64Url([byte[]]$bytes) {
    [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function Get-KeyBytes([string]$secret) {
    try { return [Convert]::FromBase64String($secret) } catch { return [Text.Encoding]::UTF8.GetBytes($secret) }
}

function New-Jwt([long]$userId, [string]$typ = 'JWT', [int]$expOffsetSec = 3600) {
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $header = ConvertTo-B64Url ([Text.Encoding]::UTF8.GetBytes((@{ alg = 'HS256'; typ = $typ } | ConvertTo-Json -Compress)))
    $payload = ConvertTo-B64Url ([Text.Encoding]::UTF8.GetBytes((@{
                    userId = $userId; sub = "e2e-user-$userId"; role = 'ROLE_PROVEEDOR'; iss = 'findu-spring-security'
                    iat = ($now - 10); exp = ($now + $expOffsetSec)
                } | ConvertTo-Json -Compress)))
    $hmac = New-Object Security.Cryptography.HMACSHA256 (, (Get-KeyBytes $JwtSecret))
    $sig = ConvertTo-B64Url ($hmac.ComputeHash([Text.Encoding]::ASCII.GetBytes("$header.$payload")))
    return "$header.$payload.$sig"
}

# Devuelve 'OPEN' si el handshake se acepta, o el codigo HTTP (p. ej. '401') si se rechaza.
function Test-WsConnect([string]$Query) {
    $ws = New-Object System.Net.WebSockets.ClientWebSocket
    $cts = New-Object System.Threading.CancellationTokenSource (8000)
    try {
        $ws.ConnectAsync([Uri]"${WsBase}?$Query", $cts.Token).GetAwaiter().GetResult() | Out-Null
        if ($ws.State -eq 'Open') {
            # Si el servidor acepta el upgrade pero cierra de inmediato (BAD_DATA), se considera rechazo.
            Start-Sleep -Milliseconds 400
            if ($ws.State -eq 'Open') { return 'OPEN' }
            return 'CLOSED'
        }
        return "STATE:$($ws.State)"
    } catch {
        $m = ''
        for ($ex = $_.Exception; $null -ne $ex; $ex = $ex.InnerException) { $m += " $($ex.Message)" }
        # "The server returned status code "401" when status code "101" was expected."
        if ($m -match 'status code "(\d{3})"') { return $Matches[1] }
        if ($m -match "(\d{3})") { return $Matches[1] }
        return "ERR:$($_.Exception.GetType().Name)"
    } finally {
        try { $ws.Dispose() } catch {}
    }
}

function Get-Violations([string]$Since) {
    @(docker logs --since $Since $Container 2>&1 | Select-String 'WS_AUTH_VIOLATION')
}

if ($Apply) {
    Write-Host "== Recreando $Service con FINDU_WS_AUTH_MODE=$Mode (override temporal) =="
    $env:FINDU_WS_AUTH_MODE = $Mode
    Push-Location $repoRoot
    try {
        $flags = @('up', '-d', '--no-deps')
        if ($Build) { $flags += '--build' }
        docker compose @flags $Service | Out-Host
    } finally { Pop-Location; Remove-Item Env:FINDU_WS_AUTH_MODE -ErrorAction SilentlyContinue }
}

try {
    Write-Host "== Esperando health de $Container =="
    for ($i = 0; $i -lt 60; $i++) {
        if ((docker inspect -f '{{.State.Health.Status}}' $Container 2>$null) -eq 'healthy') { break }
        Start-Sleep -Seconds 3
    }
    $actualMode = "$(docker exec $Container printenv FINDU_WS_AUTH_MODE 2>$null)".Trim()
    Write-Host "Modo real del contenedor: $actualMode (esperado: $Mode)"
    if ($actualMode -ne $Mode) { Write-Error "El contenedor no corre en $Mode. Use -Apply."; exit 2 }
    $banner = @(docker logs $Container 2>&1 | Select-String 'findu.websocket.auth.mode=')
    if ($banner.Count -eq 0) {
        Write-Error "El dispatcher no tiene el codigo nuevo (sin log 'findu.websocket.auth.mode='): reconstruya la imagen (-Apply -Build)."
        exit 3
    }

    $enforce = $Mode -eq 'ENFORCE'
    $own = "PROVEEDOR_$OwnerProveedorId"
    $foreign = "PROVEEDOR_$($OwnerProveedorId + 1000)"
    $owner = New-Jwt $OwnerAuthUserId
    $other = New-Jwt $OtherAuthUserId
    $refresh = New-Jwt $OwnerAuthUserId 'JWTRefresh'
    $expired = New-Jwt $OwnerAuthUserId 'JWT' -3600

    $since = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    $results = New-Object System.Collections.Generic.List[object]
    function Add-Case([string]$Id, [string]$Desc, [string]$Actual, [string[]]$Expected) {
        $ok = $Expected -contains $Actual
        $results.Add([pscustomobject]@{ Caso = $Id; Descripcion = $Desc; Esperado = ($Expected -join '|'); Obtenido = $Actual; Resultado = $(if ($ok) { 'PASS' } else { 'FAIL' }) })
    }
    $rej = @('401', '403')
    $denied = if ($enforce) { $rej } else { @('OPEN') }

    Add-Case 'a1' 'token del duenio + canal propio' (Test-WsConnect "userId=$own&token=$owner") @('OPEN')
    Add-Case 'a2' 'token del duenio sin userId (identidad desde el token)' (Test-WsConnect "token=$owner") $(if ($enforce) { @('OPEN') } else { @('OPEN', 'CLOSED', '400') })
    Add-Case 'b1' 'token de otro usuario + canal ajeno' (Test-WsConnect "userId=$own&token=$other") $denied
    Add-Case 'b2' 'token del duenio + canal de otro proveedor' (Test-WsConnect "userId=$foreign&token=$owner") $denied
    Add-Case 'b3' 'token del duenio + canal no verificable (CLIENTE_9)' (Test-WsConnect "userId=CLIENTE_9&token=$owner") $denied
    Add-Case 'c1' 'sin token + userId (cliente viejo)' (Test-WsConnect "userId=$own") $denied
    Add-Case 'd1' 'refresh token (typ=JWTRefresh)' (Test-WsConnect "userId=$own&token=$refresh") $denied
    Add-Case 'e1' 'token vencido' (Test-WsConnect "userId=$own&token=$expired") $denied
    Add-Case 'f1' 'token basura' (Test-WsConnect "userId=$own&token=abc.def.ghi") $denied

    Start-Sleep -Seconds 1
    $viol = Get-Violations $since
    $reasons = @($viol | ForEach-Object { if ($_ -match 'reason=(\w+)') { $Matches[1] } } | Group-Object | ForEach-Object { "$($_.Name)=$($_.Count)" })
    Write-Host "WS_AUTH_VIOLATION en logs: $($viol.Count) ($($reasons -join ', '))"
    # Los tokens nunca deben aparecer en los logs.
    $leak = @(docker logs --since $since $Container 2>&1 | Select-String -SimpleMatch -Pattern @($owner, $other, $refresh, $expired)).Count
    Add-Case 'g1' 'ningun token aparece en los logs del dispatcher' "$leak" @('0')
    Add-Case 'g2' 'se registran violaciones para los casos b1,b2,c1,d1,e1,f1' $(if ($viol.Count -ge 6) { 'OK' } else { "solo $($viol.Count)" }) @('OK')

    $results | Format-Table -AutoSize | Out-Host
    $failed = @($results | Where-Object Resultado -eq 'FAIL').Count
    Write-Host "Modo=$Mode  PASS=$(($results.Count - $failed))  FAIL=$failed"
    if ($failed -gt 0) { exit 1 }
} finally {
    if ($Apply -and $Mode -ne 'LOG') {
        Write-Host "== Restaurando $Service a LOG (valor del compose) =="
        Push-Location $repoRoot
        try { docker compose up -d --no-deps $Service | Out-Host } finally { Pop-Location }
    }
}
