<#
Instala la API CarWash en IIS (puerto 5197) en esta maquina.
Ejecutar en PowerShell como administrador, con la carpeta publicada de la API:
  .\instalar-api.ps1 -Origen C:\Work\CarWashApi\publish
Requisito previo: .NET 9 Hosting Bundle instalado (ver docs\instalacion-api.md).
No configura correo ni secretos; ver la seccion 5 de la guia.
#>
param(
    [Parameter(Mandatory)] [string] $Origen,
    [string] $Destino = 'C:\inetpub\CarWashApi',
    [int] $Puerto = 5197,
    [string] $DatosDb = 'C:\ProgramData\SplashCar',
    [switch] $PerfilPrivado
)
$ErrorActionPreference = 'Stop'
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole('Administrator')) {
    throw 'Ejecute este script como administrador.'
}
if (-not (Test-Path "$Origen\Carwash.API.dll")) { throw "No se encontro Carwash.API.dll en $Origen" }

Enable-WindowsOptionalFeature -Online -FeatureName IIS-WebServerRole, IIS-WebServer, IIS-ManagementConsole -All -NoRestart | Out-Null
Import-Module WebAdministration

New-Item -ItemType Directory -Force $Destino, $DatosDb | Out-Null
Copy-Item "$Origen\*" $Destino -Recurse -Force

if (-not (Test-Path IIS:\AppPools\CarWashApi)) { New-WebAppPool -Name CarWashApi | Out-Null }
Set-ItemProperty IIS:\AppPools\CarWashApi managedRuntimeVersion ''
Set-ItemProperty IIS:\AppPools\CarWashApi startMode AlwaysRunning
Set-ItemProperty IIS:\AppPools\CarWashApi processModel.idleTimeout ([TimeSpan]::Zero)

if (-not (Get-Website -Name CarWashApi -ErrorAction SilentlyContinue)) {
    New-Website -Name CarWashApi -PhysicalPath $Destino -ApplicationPool CarWashApi -Port $Puerto -IPAddress '*' | Out-Null
}
icacls $Destino /grant 'IIS AppPool\CarWashApi:(OI)(CI)RX' | Out-Null
icacls $DatosDb /grant 'IIS AppPool\CarWashApi:(OI)(CI)M' | Out-Null

if (-not (Get-NetFirewallRule -DisplayName 'CarWash API LAN' -ErrorAction SilentlyContinue)) {
    New-NetFirewallRule -DisplayName 'CarWash API LAN' -Direction Inbound -Action Allow -Protocol TCP `
        -LocalPort $Puerto -Profile Private -RemoteAddress LocalSubnet | Out-Null
}
if ($PerfilPrivado) {
    Get-NetConnectionProfile | Where-Object IPv4Connectivity -eq 'Internet' |
        Set-NetConnectionProfile -NetworkCategory Private
}

Start-WebAppPool CarWashApi -ErrorAction SilentlyContinue
Start-Website CarWashApi
Write-Host "Listo. Pruebe: curl.exe -i http://localhost:$Puerto/api/TipoVehiculo/GetAll"
Get-NetIPAddress -AddressFamily IPv4 | Where-Object IPAddress -match '^(10|192\.168|172\.(1[6-9]|2\d|3[01]))\.' |
    ForEach-Object { Write-Host "Direccion para la app: http://$($_.IPAddress):$Puerto" }
