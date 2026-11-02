param(
    [Parameter(Mandatory)] [string] $InstallDir,
    [int] $Port = 5197,
    [switch] $PublicFirewall
)
$ErrorActionPreference = 'Stop'
$taskName = 'CarWashApi'
$log = Join-Path $InstallDir 'install.log'
Start-Transcript -Path $log -Force | Out-Null

$action = New-ScheduledTaskAction -Execute 'powershell.exe' -WorkingDirectory $InstallDir `
    -Argument "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$InstallDir\run-api.ps1`" -InstallDir `"$InstallDir`" -Port $Port"
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal `
    -Settings $settings -Description 'CarWash API (inicia con Windows)' -Force | Out-Null

Get-NetFirewallRule -DisplayName 'CarWash API LAN*' -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName 'CarWash API LAN' -Direction Inbound -Action Allow -Protocol TCP `
    -LocalPort $Port -Profile Private -RemoteAddress LocalSubnet | Out-Null
if ($PublicFirewall) {
    New-NetFirewallRule -DisplayName 'CarWash API LAN (Publico)' -Direction Inbound -Action Allow -Protocol TCP `
        -LocalPort $Port -Profile Public -RemoteAddress LocalSubnet | Out-Null
}

Start-ScheduledTask -TaskName $taskName
$ok = $false
for ($i = 0; $i -lt 40 -and -not $ok; $i++) {
    Start-Sleep -Seconds 1
    try { $ok = (Invoke-WebRequest "http://localhost:$Port/api/TipoVehiculo/GetAll" -UseBasicParsing -TimeoutSec 3).StatusCode -eq 200 } catch { }
}

$addresses = Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -match '^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)' } |
    ForEach-Object { "http://$($_.IPAddress):$Port" }
$state = if ($ok) { 'La API responde correctamente.' } else { "La API no respondio aun; revise $log y la tarea CarWashApi." }
@(
    'CarWash API',
    $state,
    '',
    'Escriba una de estas direcciones en el engranaje de la app movil:',
    $addresses,
    '',
    'Si el celular no conecta: la Wi-Fi del PC debe ser de perfil Privado y el router',
    'no debe tener aislamiento de clientes. Reserve esta IP en el router (DHCP).'
) | Set-Content (Join-Path $InstallDir 'direcciones.txt') -Encoding UTF8
Stop-Transcript | Out-Null
