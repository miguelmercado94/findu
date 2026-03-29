# Mata el proceso que usa el puerto 8081 (útil cuando bootRun dice "port already in use")
$port = 8081
$conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
if ($conn) {
    $conn | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
    Write-Host "Proceso en puerto $port terminado."
} else {
    Write-Host "Ningun proceso usa el puerto $port."
}
