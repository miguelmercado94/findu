<#
.SYNOPSIS
  E2E de autorizacion por propietario (D9) contra findu-core en Docker.
.DESCRIPTION
  Forja tokens HS256 con el JWT_SECRET del compose y llama a core con `docker exec findu-core curl`
  (core no publica puerto). -Mode indica el modo esperado (LOG | ENFORCE) para evaluar resultados.
  Con -Apply recrea SOLO findu-core con FINDU_AUTHZ_MODE=<Mode> (override temporal por entorno, sin tocar el compose).
  Sin -Apply asume que core ya corre en ese modo. Sale con codigo 1 si algun caso falla.
.EXAMPLE
  ./scripts/authz-e2e.ps1 -Mode ENFORCE -Apply
  ./scripts/authz-e2e.ps1 -Mode LOG -Apply
#>
param(
    [ValidateSet('LOG', 'ENFORCE')][string]$Mode = 'LOG',
    [switch]$Apply,
    [string]$Container = 'findu-core',
    [string]$BaseUrl = 'http://localhost:8082/findu-core',
    [string]$JwtSecret = 'ZmluZHUtZGVmYXVsdC1zZWNyZXQta2V5LWF0LWxlYXN0LTI1Ni1iaXRzLWZvci1oczI1Ng==',
    [string]$InternalKey = 'findu-internal-saga-secret-key-2026',
    [long]$OwnerUserId = 2,      # perfil_proveedor id=1 tiene auth_user_id=2 en el stack local
    [long]$PerfilId = 1,
    [long]$OtherUserId = 999
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

function ConvertTo-B64Url([byte[]]$bytes) {
    [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function Get-KeyBytes([string]$secret) {
    # Misma regla que core/security: primero Base64, si falla UTF-8
    try { return [Convert]::FromBase64String($secret) } catch { return [Text.Encoding]::UTF8.GetBytes($secret) }
}

function New-Jwt([long]$userId, [string]$typ = 'JWT', [int]$expOffsetSec = 3600, [string]$role = 'ROLE_PROVEEDOR') {
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $header = ConvertTo-B64Url ([Text.Encoding]::UTF8.GetBytes((@{ alg = 'HS256'; typ = $typ } | ConvertTo-Json -Compress)))
    $payload = ConvertTo-B64Url ([Text.Encoding]::UTF8.GetBytes((@{
                    userId = $userId; sub = "e2e-user-$userId"; role = $role; iss = 'findu-spring-security'
                    iat = ($now - 10); exp = ($now + $expOffsetSec)
                } | ConvertTo-Json -Compress)))
    $hmac = New-Object Security.Cryptography.HMACSHA256 (, (Get-KeyBytes $JwtSecret))
    $sig = ConvertTo-B64Url ($hmac.ComputeHash([Text.Encoding]::ASCII.GetBytes("$header.$payload")))
    return "$header.$payload.$sig"
}

function Invoke-Core([string]$Method, [string]$Path, [string]$Token = $null, [string]$Key = $null, [string]$Body = $null) {
    # El cuerpo va por stdin (-d @-) para evitar que PowerShell 5 pierda las comillas dobles de los argumentos nativos
    $a = @('exec')
    if (-not [string]::IsNullOrEmpty($Body)) { $a += '-i' }
    $a += @($Container, 'curl', '-s', '-o', '/dev/null', '-w', '%{http_code}', '-X', $Method, "$BaseUrl$Path")
    if ($Token) { $a += @('-H', "Authorization: Bearer $Token") }
    if ($Key) { $a += @('-H', "X-Internal-Service-Key: $Key") }
    if (-not [string]::IsNullOrEmpty($Body)) { $a += @('-H', 'Content-Type: application/json', '--data-binary', '@-') }
    $out = if (-not [string]::IsNullOrEmpty($Body)) { $Body | & docker @a } else { & docker @a }
    return "$out".Trim()
}

function Get-ViolationCount { return @(docker logs $Container 2>&1 | Select-String 'AUTHZ_VIOLATION').Count }

if ($Apply) {
    Write-Host "== Recreando $Container con FINDU_AUTHZ_MODE=$Mode (override temporal) =="
    $env:FINDU_AUTHZ_MODE = $Mode
    Push-Location $repoRoot
    try { docker compose up -d --no-deps $Container | Out-Host } finally { Pop-Location; Remove-Item Env:FINDU_AUTHZ_MODE -ErrorAction SilentlyContinue }
}

Write-Host "== Esperando health de $Container =="
for ($i = 0; $i -lt 60; $i++) {
    if ((docker inspect -f '{{.State.Health.Status}}' $Container 2>$null) -eq 'healthy') { break }
    Start-Sleep -Seconds 3
}
$actualMode = (docker exec $Container printenv FINDU_AUTHZ_MODE).Trim()
Write-Host "Modo real del contenedor: $actualMode (esperado: $Mode)"
if ($actualMode -ne $Mode) { Write-Error "El contenedor no corre en $Mode. Use -Apply."; exit 2 }

$enforce = $Mode -eq 'ENFORCE'
$owner = New-Jwt $OwnerUserId
$other = New-Jwt $OtherUserId
$refresh = New-Jwt $OwnerUserId 'JWTRefresh'
$expired = New-Jwt $OwnerUserId 'JWT' -3600

$results = New-Object System.Collections.Generic.List[object]
function Test-Case([string]$Id, [string]$Desc, [string]$Actual, [string[]]$Expected) {
    $ok = $Expected -contains $Actual
    $results.Add([pscustomobject]@{ Caso = $Id; Descripcion = $Desc; Esperado = ($Expected -join '|'); Obtenido = $Actual; Resultado = $(if ($ok) { 'PASS' } else { 'FAIL' }) })
}

$perfil = "/api/v1/perfil-proveedor/$PerfilId"
$byAuth = "/api/v1/perfil-proveedor/auth/$OwnerUserId"
$foto = "$perfil/foto-perfil"
$denied = if ($enforce) { '403' } else { '200' }
$noTok = if ($enforce) { '401' } else { '200' }

$before = Get-ViolationCount

Test-Case 'a1' 'propietario GET perfil/{id}' (Invoke-Core GET $perfil $owner) @('200')
Test-Case 'a2' 'propietario GET perfil/auth/{authUserId}' (Invoke-Core GET $byAuth $owner) @('200')
Test-Case 'a3' 'propietario GET cobertura' (Invoke-Core GET "$perfil/cobertura" $owner) @('200')
Test-Case 'b1' 'otro userId GET perfil/{id}' (Invoke-Core GET $perfil $other) @($denied)
Test-Case 'b2' 'otro userId GET perfil/auth/{authUserId}' (Invoke-Core GET $byAuth $other) @($denied)
Test-Case 'b3' 'otro userId PUT foto-perfil (json, body invalido)' (Invoke-Core PUT $foto $other $null '{"imagenBase64":""}') $(if ($enforce) { @('403') } else { @('400', '422') })
Test-Case 'c1' 'sin token GET perfil/{id}' (Invoke-Core GET $perfil) @($noTok)
Test-Case 'c2' 'sin token GET perfil/auth/{authUserId}' (Invoke-Core GET $byAuth) @($noTok)
Test-Case 'd1' 'refresh token (typ=JWTRefresh) GET perfil/{id}' (Invoke-Core GET $perfil $refresh) @($noTok)
Test-Case 'e1' 'token expirado GET perfil/{id}' (Invoke-Core GET $perfil $expired) @($noTok)
$fotoNoToken = Invoke-Core GET $foto
Test-Case 'f1' 'GET foto-perfil sin token (publico: 200, o 404 si no hay foto)' $fotoNoToken @('200', '404')
Test-Case 'g1' 'GET perfil/{id}/publico sin token' (Invoke-Core GET "$perfil/publico?servicioId=1") @('200')
Test-Case 'g2' 'GET /api/v1/categorias sin token' (Invoke-Core GET '/api/v1/categorias') @('200')
Test-Case 'g3' 'GET /api/v1/servicios sin token' (Invoke-Core GET '/api/v1/servicios') @('200')
Test-Case 'g4' 'GET /actuator/health sin token' (Invoke-Core GET '/actuator/health') @('200')
$intNoKey = Invoke-Core POST '/internal/perfiles/999999/cobertura' $null $null '{}'
$intKey = Invoke-Core POST '/internal/perfiles/999999/cobertura' $null $InternalKey '{}'
Test-Case 'h1' '/internal sin llave NO pasa' $intNoKey @('401', '403')
Test-Case 'h2' '/internal con llave pasa (no 401/403)' $(if ($intKey -in @('401', '403')) { $intKey } else { 'PASA' }) @('PASA')
Test-Case 'h3' 'llave interna en endpoint de perfil sin JWT cuenta como servicio' (Invoke-Core GET $byAuth $null $InternalKey) @('200')

Start-Sleep -Seconds 1
$after = Get-ViolationCount
$delta = $after - $before
# Violaciones esperadas: b1,b2,b3,c1,c2,d1,e1 = 7 (en ambos modos se registran)
Test-Case 'i1' "AUTHZ_VIOLATION registradas en logs de $Container (esperado >= 7)" $(if ($delta -ge 7) { 'OK' } else { "solo $delta" }) @('OK')

Write-Host ""
$results | Format-Table -AutoSize | Out-Host
$fail = @($results | Where-Object Resultado -eq 'FAIL').Count
Write-Host "Modo=$Mode  PASS=$(@($results | Where-Object Resultado -eq 'PASS').Count)  FAIL=$fail  (f1 obtuvo $fotoNoToken; AUTHZ_VIOLATION nuevas=$delta)"
if ($fail -gt 0) { exit 1 }
exit 0



