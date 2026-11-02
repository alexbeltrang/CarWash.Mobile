#requires -Version 5.1
# Genera installer\output\CarWashApi-Setup.exe a partir de la API y la base de esquema.
# Requisitos en esta maquina: .NET SDK 9 e Inno Setup 6 (winget install JRSoftware.InnoSetup).
param(
    [string] $ApiPath = 'C:\Work\CarWashApi',
    [string] $SchemaDb = "$PSScriptRoot\CarWash.schema.db"
)
$ErrorActionPreference = 'Stop'
$staging = "$PSScriptRoot\staging\api"
if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
dotnet publish "$ApiPath\Carwash.API.csproj" -c Release -r win-x64 --self-contained true -p:PublishReadyToRun=true -o $staging
if ($LASTEXITCODE) { throw 'Fallo dotnet publish' }

# La API publicada puede arrastrar su carpeta publish anterior y su appsettings con secretos.
Remove-Item "$staging\publish" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$staging\appsettings.Development.json" -Force -ErrorAction SilentlyContinue
Copy-Item "$PSScriptRoot\appsettings.json" "$staging\appsettings.json" -Force

# Sembrador del usuario inicial: comparte el runtime autocontenido de la API.
dotnet publish "$PSScriptRoot\seeder\CarWashSeeder.csproj" -c Release -r win-x64 --self-contained true -o $staging
if ($LASTEXITCODE) { throw 'Fallo dotnet publish del sembrador' }
$leaks = Get-ChildItem $staging -Recurse -Filter *.json | Select-String -Pattern '"Password"\s*:\s*"[^"]+"'
if ($leaks) { throw "Se encontraron credenciales en: $($leaks.Path -join ', ')" }
if (-not (Test-Path $SchemaDb)) { throw "Falta la base de esquema: $SchemaDb" }

$iscc = @("$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe", "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe") |
    Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $iscc) { throw 'Inno Setup 6 no esta instalado.' }
& $iscc "$PSScriptRoot\CarWashApi.iss"
if ($LASTEXITCODE) { throw 'Fallo ISCC' }
Write-Host "Instalador: $PSScriptRoot\output\CarWashApi-Setup.exe"
