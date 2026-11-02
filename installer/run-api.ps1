# Lanzador de la API: carga el correo desde un archivo protegido (solo SYSTEM y
# Administradores) y arranca la API. No guarda secretos en el registro ni en la tarea.
param(
    [string] $InstallDir = $PSScriptRoot,
    [int] $Port = 5197
)
$ErrorActionPreference = 'Stop'
$emailFile = Join-Path $env:ProgramData 'SplashCar\email.json'
if (Test-Path $emailFile) {
    $email = Get-Content $emailFile -Raw | ConvertFrom-Json
    foreach ($property in $email.PSObject.Properties) {
        if ($property.Value -ne $null -and "$($property.Value)" -ne '') {
            Set-Item -Path "Env:Email__$($property.Name)" -Value "$($property.Value)"
        }
    }
}
Set-Location $InstallDir
$log = Join-Path $env:ProgramData 'SplashCar\arranque.log'
# Reinicia la API si se cae y deja constancia de cada arranque para medir cuanto tarda.
while ($true) {
    "$(Get-Date -Format 's') iniciando API" | Add-Content $log
    & (Join-Path $InstallDir 'Carwash.API.exe') --urls "http://0.0.0.0:$Port"
    "$(Get-Date -Format 's') la API termino con codigo $LASTEXITCODE; reintento en 5 s" | Add-Content $log
    Start-Sleep -Seconds 5
}
