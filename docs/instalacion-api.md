# Instalar CarWash API en otra maquina (Windows + IIS)

## Opcion rapida: instalador .exe

`installer\output\CarWashApi-Setup.exe` (se genera con `installer\build.ps1`) instala todo sin IIS ni .NET:
API autocontenida en `C:\Program Files\CarWashApi`, tarea de Windows que la inicia con el equipo
(reinicio automatico si falla), regla de firewall en perfil Privado puerto 5197 y base vacia en
`C:\ProgramData\SplashCar\CarWash.db` (solo esquema; si ya existe una base, no se toca).
Pide opcionalmente la cuenta y contrasena de aplicacion del correo; se guardan solo en
`C:\ProgramData\SplashCar\email.json` (acceso unicamente SYSTEM y Administradores).
**Usuario inicial:** el asistente pide usuario y contrasena (por defecto `admin` / `Admin2026*`,
editables). Se crean solo si la base no tiene usuarios activos; si ya hay usuarios, no se toca nada.
Se guardan igual que el WinForm (perfil `Administrador` y usuario/contrasena cifrados con
`FunctionsEncrip`); el WinForm no tiene modulo para crear usuarios. Cambie la clave por defecto
si el equipo es accesible a otras personas. Silenciosa: `/USUARIO=nombre /CLAVE=clave`.
Al terminar muestra las direcciones para escribir en la app. Instalacion silenciosa:
`CarWashApi-Setup.exe /VERYSILENT /SUPPRESSMSGBOXES`. Se desinstala desde Aplicaciones instaladas.
La base nueva no trae catalogos ni usuarios: copie `CarWash.db` con datos (servicio detenido:
`schtasks /End /TN CarWashApi`) o cargue los catalogos desde la aplicacion WinForms.
Las secciones siguientes son la alternativa manual con IIS.

Rutas de origen: API `C:\Work\CarWashApi`, WinForms `C:\Work\CarWash`,
Mobile `C:\Work\CarWash.Mobile`. La API es .NET 9 con SQLite.
Ejecutar todo en PowerShell **como administrador**. Reemplazar los valores en MAYUSCULAS.

> Version del API con asignacion de operario: incluye `PUT api/Turnos/{id}/Operario` y el campo `EsAdministrador` en `POST api/Auth/login`. La app movil lo requiere para asignar; una API anterior solo permite el ingreso.

## Actualizacion coordinada: reporte de ventas

Esta version agrega `GET /api/Reportes/Ventas` y
`GET /api/Reportes/Ventas/Filtros`. Ambos requieren `Authorization: Bearer` de
un Administrador vigente. Fecha inicial/final obligatorias `yyyy-MM-dd`; los
opcionales idFormaPago/idOperario/placa/idTipoVehiculo se intersectan. El segundo
GET recibe solo fechas y devuelve catalogos independientes del periodo, excluyendo
operarios/tipos eliminados. FormaPago no tiene indicador de eliminacion. Los
combos muestran nombres, conservando IDs internos. Total decimal textual y
cantidad son globales; detalle por fecha de cobro, no ingreso del turno.

Tambien se protegen los GET raiz, `{id}` y `GetHistoricoMovimientos` de
`/api/TurnosMovimientos`. POST/PUT/DELETE mantienen contratos y permisos previos.
Clientes antiguos sin Bearer reciben 401; no habilitar bypass. Antes de activar
esta API, preparar escritorio y APK compatibles en una ventana coordinada,
probarlos juntos y respaldar la base con el procedimiento operativo existente.
No se ejecuta aqui instalacion, reinicio de servicio ni cambio de firewall.

`POST /api/Auth/login` conserva usuario/contrasena cifrados actuales y agrega
accessToken/tokenType/expiresAt/idleTimeoutSeconds a respuestas validas.
`POST /api/Auth/logout` revoca la credencial enviada. Sesiones en memoria:
sin caducidad por inactividad, vida absoluta 9 horas desde cada login, maximo
10000 sesiones por proceso. El uso no renueva el plazo ni cierra otros usuarios.
La app usa el login general y cierra toda su sesion al vencer, incluso al volver
de segundo plano. Configuracion no secreta: Session:AbsoluteLifetimeHours=9 y
Session:MaxSessions=10000 (variables Session__...). IdleTimeoutMinutes se conserva
por compatibilidad pero no controla vencimiento; idleTimeoutSeconds=32400 es
un campo heredado que informa la duracion absoluta, no un limite de inactividad.
Un reinicio invalida todas las sesiones; no se usan refresh tokens ni se guardan
contrasenas para reingreso automatico. Logout offline limpia cliente pero no
garantiza revocacion remota.

BusinessTimeZoneId opcional fija la zona operativa; por defecto usa la zona local
del servidor. Elegir un identificador valido para la instalacion y conservar la
interpretacion local de fechas existentes; no convertir la base a UTC. El header
X-Server-Date informa el dia operativo a clientes. No imprimir tokens, cuerpos de
login, placas ni URLs de reporte; no activar logging sensible de EF.

Preferir HTTPS con certificado confiable. HTTP solo en red privada confiable,
con riesgo de interceptacion; no desactivar certificados ni publicar el puerto
en Internet. Los clientes no reenvian el token a alternativos/redirecciones;
escritorio rechaza lecturas HTTPS protegidas con ApiIgnorarSSL=true.
Pruebas, configuracion y comandos reproducibles:
[quickstart](../specs/006-reporte-ventas/quickstart.md).

### Paquete generado y comprobado

Se ejecuto `./installer/build.ps1 -ApiPath (Resolve-Path ../CarWashApi).Path`:
API y seeder Release win-x64 autocontenidos; Inno Setup 6.7.3 correcto.
Artefacto actualizado T057-T060: `installer/output/CarWashApi-Setup.exe` (41545444 bytes).
SHA256: `2D9F8482BAFF6633A0FC6C4B4A4C844AB7716633BF569C121E6B6A593EBBEDB4`.
No se ejecuto el instalador ni se modifico ningun servicio o firewall.

Esquema SQLite verificado en modo solo lectura, tablas de negocio vacias.
Staging sin bases operativas, appsettings.Development.json ni valores secretos.
El binario recien publicado se probo como proceso loopback con base temporal:
login/logout y reporte correctos; cinco GET rechazan cliente viejo (401) y no
administrador (403), que conserva sus operaciones permitidas. Consultas no alteran
cobros. Esas dos comprobaciones de paquete/esquema pasaron. Ademas, esta revision
paso 66 pruebas API, 77 JVM, 41 instrumentadas (una integracion opcional omitida),
compatibilidad de escritorio y compilacion/lint Android (0 errores, 6 advertencias).
Los E2E anteriores con ambos clientes quedan como evidencia historica en quickstart.
APK Debug: `app/build/outputs/apk/debug/app-debug.apk`; no es firma de produccion.

## 1. Publicar (en la maquina de desarrollo)

```powershell
cd C:\Work\CarWashApi
dotnet publish Carwash.API.csproj -c Release -o C:\Work\CarWashApi\publish
```

Copiar la carpeta `publish` a la maquina destino (USB o red), por ejemplo a `C:\inetpub\CarWashApi`.
No copiar `appsettings.Development.json` con datos reales ni secretos.

## 2. Requisitos en la maquina destino

```powershell
winget install Microsoft.DotNet.HostingBundle.9 --accept-package-agreements --accept-source-agreements
Enable-WindowsOptionalFeature -Online -FeatureName IIS-WebServerRole,IIS-WebServer,IIS-ManagementConsole -All
iisreset
```

Si `winget` no esta disponible, instalar el ".NET 9 Hosting Bundle" desde dotnet.microsoft.com.

## 3. Base de datos SQLite

`ConnectionStrings:DefaultConnection` usa `C:\ProgramData\SplashCar\CarWash.db`.
Crear la carpeta y dar permiso al pool (el pool se crea en el paso 4; ejecutar este
`icacls` despues de ese paso):

```powershell
New-Item -ItemType Directory -Force C:\ProgramData\SplashCar | Out-Null
icacls C:\ProgramData\SplashCar /grant "IIS AppPool\CarWashApi:(OI)(CI)M"
```

Para llevar los datos existentes, copiar `CarWash.db` a esa carpeta con el sitio detenido.

## 4. Sitio IIS en el puerto 5197

```powershell
Import-Module WebAdministration
New-WebAppPool -Name CarWashApi
Set-ItemProperty IIS:\AppPools\CarWashApi managedRuntimeVersion ""
Set-ItemProperty IIS:\AppPools\CarWashApi startMode AlwaysRunning
Set-ItemProperty IIS:\AppPools\CarWashApi processModel.idleTimeout 00:00:00
New-Website -Name CarWashApi -PhysicalPath C:\inetpub\CarWashApi -ApplicationPool CarWashApi -Port 5197 -IPAddress "*"
icacls C:\inetpub\CarWashApi /grant "IIS AppPool\CarWashApi:(OI)(CI)RX"
```

## 5. Correo (opcional, solo en el servidor)

```powershell
[Environment]::SetEnvironmentVariable("Email__Username", "CUENTA@gmail.com", "Machine")
[Environment]::SetEnvironmentVariable("Email__Password", "CONTRASENA_DE_APLICACION", "Machine")
[Environment]::SetEnvironmentVariable("Email__FromAddress", "CUENTA@gmail.com", "Machine")
iisreset
```

## 6. Firewall y red

Solo en una red propia y confiable. Marcar la Wi-Fi como Privada:

```powershell
Get-NetConnectionProfile
Set-NetConnectionProfile -InterfaceAlias "Wi-Fi" -NetworkCategory Private
New-NetFirewallRule -DisplayName "CarWash API LAN" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5197 -Profile Private -RemoteAddress LocalSubnet
```

## 7. IP fija (evita que la app deje de conectar)

Preferible: reserva DHCP en el router para la MAC del PC. Alternativa en Windows:

```powershell
Get-NetAdapter | Where Status -eq Up | Select Name, MacAddress
New-NetIPAddress -InterfaceAlias "Wi-Fi" -IPAddress 192.168.1.15 -PrefixLength 24 -DefaultGateway 192.168.1.1
Set-DnsClientServerAddress -InterfaceAlias "Wi-Fi" -ServerAddresses 192.168.1.1,8.8.8.8
```

Ajustar IP, prefijo y puerta de enlace a la red real. Si ya existe una IP en el adaptador,
quitarla antes con `Remove-NetIPAddress`.

## 8. Verificacion

```powershell
ipconfig
netstat -ano | findstr :5197
curl.exe -i http://localhost:5197/api/TipoVehiculo/GetAll
```

Desde el navegador del celular (misma Wi-Fi, sin aislamiento de clientes ni red de invitados):
`http://IP-DEL-PC:5197/swagger/index.html` y `http://IP-DEL-PC:5197/api/TipoVehiculo/GetAll`.
Luego escribir `http://IP-DEL-PC:5197` en el engranaje de la app.

## Respaldo por USB (sin Wi-Fi)

```powershell
adb reverse tcp:5197 tcp:5197
```

En la app usar `http://localhost:5197`.

## Alternativa sin IIS (pruebas)

```powershell
cd C:\inetpub\CarWashApi
dotnet Carwash.API.dll --urls "http://0.0.0.0:5197"
```

Se detiene al cerrar la terminal; para uso continuo preferir IIS.
