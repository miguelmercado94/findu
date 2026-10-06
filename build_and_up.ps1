$ErrorActionPreference = "Stop"

$root = "C:\Users\ADMIN\Documents\FIND-U-PROJECT\findu"

$projects = @(
  "ARCHITECTURE\findu-config",
  "ARCHITECTURE\findu-eureka-server",
  "ARCHITECTURE\findu-api-gateway",
  "SECURITY\findu-spring-security",
  "SECURITY\autorization-server-oauth2",
  "CORE\findu-core",
  "CORE\findu-transaction",
  "CORE\findu-help-v2",
  "CORE\findu-notification-processor",
  "CORE\LAMBDA\findu-notification-dispatcher",
  "CORE\LAMBDA\findu-transaction-daily-balance"
)

foreach ($proj in $projects) {
  $targetDir = Join-Path $root $proj
  Write-Host "==========================================" -ForegroundColor Cyan
  Write-Host "Compilando JAR local: $proj" -ForegroundColor Green
  Write-Host "==========================================" -ForegroundColor Cyan
  Push-Location $targetDir
  cmd /c "gradlew.bat bootJar -x test -x check --warning-mode none"
  if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo la compilacion en $proj"
    exit 1
  }
  Pop-Location
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Compilacion JAR completada. Levantando Docker Compose..." -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan

Push-Location $root
docker compose up -d --build
