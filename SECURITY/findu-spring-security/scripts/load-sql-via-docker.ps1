# Levanta PostgreSQL con docker-compose (repo raíz FINDU-PROJECT) y aplica schema.sql + data.sql
# por stdin a psql dentro del contenedor (sin necesidad de psql en Windows).
#
# Requisito: Docker Desktop en ejecución.
# Uso (desde esta carpeta):  .\load-sql-via-docker.ps1
# O desde FINDU-PROJECT:      .\SECURITY\findu-spring-security\scripts\load-sql-via-docker.ps1

$ErrorActionPreference = "Stop"

$moduleRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = (Resolve-Path (Join-Path $moduleRoot "..\..")).Path
$composeFile = Join-Path $projectRoot "docker-compose.yml"
$sqlDir = Join-Path $moduleRoot "src\main\resources"
$schema = Join-Path $sqlDir "schema.sql"
$data = Join-Path $sqlDir "data.sql"

foreach ($f in @($schema, $data)) {
    if (-not (Test-Path $f)) { Write-Error "No existe: $f" }
}

if (-not (Test-Path $composeFile)) { Write-Error "No existe docker-compose.yml en: $projectRoot" }

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "docker no está en PATH. Instala Docker Desktop y vuelve a intentar."
}

Write-Host "Iniciando contenedor findu-postgres..." -ForegroundColor Cyan
Push-Location $projectRoot
try {
    if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        & docker-compose -f $composeFile up -d postgres
    } else {
        & docker compose -f $composeFile up -d postgres
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Error "docker compose up falló (¿Docker Desktop arrancado?). Código: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    & docker exec findu-postgres pg_isready -U findu -d findu 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 2
}
if ($LASTEXITCODE -ne 0) {
    Write-Error "PostgreSQL no respondió a tiempo. Revisa: docker logs findu-postgres"
}

function Send-SqlFile {
    param([string]$Path, [string]$Label)
    Write-Host "Aplicando $Label..." -ForegroundColor Cyan
    $text = Get-Content -Path $Path -Raw -Encoding UTF8
    $text | & docker exec -i findu-postgres psql -U findu -d findu -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { Write-Error "Fallo al ejecutar $Label" }
}

Send-SqlFile -Path $schema -Label "schema.sql"
Send-SqlFile -Path $data -Label "data.sql"

Write-Host "OK: tablas y datos aplicados. Prueba: docker exec -it findu-postgres psql -U findu -d findu -c `"SELECT COUNT(*) FROM module;`"" -ForegroundColor Green
