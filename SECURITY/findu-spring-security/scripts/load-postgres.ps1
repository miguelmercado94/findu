# Carga schema.sql y data.sql en PostgreSQL local (psql).
# Prerrequisito: servicio PostgreSQL en marcha y base creada.
#
# Ejemplo (crear base y usuario una vez, como superusuario):
#   psql -U postgres -c "CREATE USER findu WITH PASSWORD 'findu';"
#   psql -U postgres -c "CREATE DATABASE findu OWNER findu;"
#
# Uso desde este directorio (scripts):
#   .\load-postgres.ps1
#   .\load-postgres.ps1 -Host localhost -Port 5432 -Database findu -User findu
#
# Variables de entorno: PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD

param(
    [string] $Host = "localhost",
    [int] $Port = 5432,
    [string] $Database = "findu",
    [string] $User = "findu"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sqlDir = Join-Path $root "src\main\resources"
$schema = Join-Path $sqlDir "schema.sql"
$data = Join-Path $sqlDir "data.sql"

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    Write-Error "psql no está en PATH. Instala PostgreSQL client o añade la carpeta bin al PATH."
}

foreach ($f in @($schema, $data)) {
    if (-not (Test-Path $f)) { Write-Error "No existe: $f" }
}

$env:PGHOST = $Host
$env:PGPORT = "$Port"
$env:PGDATABASE = $Database
$env:PGUSER = $User
if (-not $env:PGPASSWORD) {
    Write-Host "PGPASSWORD no definido; psql pedirá contraseña por consola." -ForegroundColor Yellow
}

Write-Host "Ejecutando schema.sql..." -ForegroundColor Cyan
& psql -v ON_ERROR_STOP=1 -f $schema
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Ejecutando data.sql..." -ForegroundColor Cyan
& psql -v ON_ERROR_STOP=1 -f $data
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Listo: tablas y datos cargados en ${Database}@${Host}:${Port}" -ForegroundColor Green
