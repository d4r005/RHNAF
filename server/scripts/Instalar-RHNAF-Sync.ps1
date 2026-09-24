# Instalador del Sincronizador de Asistencia RHNAF para Windows
# ---------------------------------------------------------------
# Uso: clic derecho sobre este archivo -> "Ejecutar con PowerShell"
# (o desde PowerShell: powershell -ExecutionPolicy Bypass -File .\Instalar-RHNAF-Sync.ps1)
#
# Que hace:
#   1. Verifica que exista Python (si falta, indica como instalarlo).
#   2. Instala la dependencia "requests" si hace falta.
#   3. Ejecuta "sync_all.py --install", que registra el arranque
#      automatico (tarea programada al iniciar sesion + clave de
#      registro Run) y arranca la sincronizacion en segundo plano.
#
# A partir de ese momento, cada vez que enciendan la PC el
# sincronizador empieza solo: jala empleados y checadas de la
# lectora Hikvision y las sube al servidor en la nube cada 5 minutos.
# La bitacora queda en sync_log.txt junto a este archivo.

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here

Write-Host "== Instalador del Sincronizador de Asistencia RHNAF ==" -ForegroundColor Cyan

# 1. Localizar Python (py launcher o python en PATH)
$py = $null
foreach ($cmd in @("py", "python")) {
    try {
        $v = & $cmd --version 2>&1
        if ($LASTEXITCODE -eq 0 -and "$v" -match "Python") { $py = $cmd; break }
    } catch { continue }
}
if (-not $py) {
    Write-Host "No se encontro Python." -ForegroundColor Red
    Write-Host "Descargalo de https://www.python.org/downloads/ (marca 'Add python.exe to PATH' durante la instalacion) y vuelve a ejecutar este instalador."
    Read-Host "Pulsa Enter para salir"
    exit 1
}
Write-Host "Python encontrado: $py"

# 2. Instalar requests si falta
& $py -m pip install --quiet requests 2>$null
if ($LASTEXITCODE -ne 0) { & $py -m pip install requests }
Write-Host "Dependencias listas."

# 3. Registrar el arranque automatico y arrancar ahora
Write-Host "Registrando arranque automatico..."
& $py (Join-Path $here "sync_all.py") --install

Write-Host ""
Write-Host "Instalacion terminada. El sincronizador correra en segundo plano" -ForegroundColor Green
Write-Host "y se activara solo cada vez que se encienda la PC."
Write-Host "Para quitarlo: python sync_all.py --uninstall"
Read-Host "Pulsa Enter para cerrar"
