# CarWash.Mobile

Base de una app Android nativa con Kotlin, Jetpack Compose, Material 3 y GitHub
Spec Kit integrado con Copilot. Compatible desde Android 8.0 (API 26).

La pantalla principal es el formulario de ingreso de vehiculos, basado en el
formulario FrmIngresoVehiculo del sistema existente. Consume la API existente;
no incluye un backend nuevo, autenticacion ni pagos. El paquete
`com.carwash.mobile` es provisional y debe confirmarse antes de publicar.

## Formulario de ingreso

- Placa, tipo de vehiculo, cliente, celular, marca, numero de orden y observaciones.
- Captura de placa con camara, reconocimiento local y revision editable obligatoria.
- Busqueda por placa con autocompletado e historial disponible en la API.
- Seleccion multiple de servicios segun el tipo, valores y base de comision
   calculados con BigDecimal y editables.
- Validacion de campos, caja activa, carga, errores y confirmacion de ingreso.
- Proteccion contra doble envio y marcador persistido ante un envio incierto.

El icono de engranaje permite configurar el servidor y comprobar la conexion
cargando tipos, servicios y caja. Se recuerda la direccion tras una conexion
correcta. No hay escaneo de la Wi-Fi ni descubrimiento automatico: la API actual
no publica un servicio de descubrimiento.

Si un envio falla despues de comenzar las escrituras, revisar el turno en el
computador antes de limpiar el formulario. La API guarda consecutivo, turno y
servicios en solicitudes separadas: no garantiza atomicidad ni evita carreras
entre varios dispositivos. No se hacen reintentos de escritura automaticos.

## Capturar placa con camara

1. Pulsar Capturar placa y permitir camara cuando Android lo solicite.
2. Encuadrar la placa y pulsar Capturar. Tocar la vista previa para enfocar;
   usar la linterna si el equipo dispone de ella.
3. Revisar la lectura, seleccionar otra sugerencia o corregir el texto y pulsar
   Usar placa. Esto completa la placa y consulta el vehiculo, no registra un ingreso.
4. Completar orden, tipo, servicios y demas campos; registrar con Guardar como antes.

Ingreso manual permanece disponible en la captura y en todos sus errores.
Si no hay lectura se puede escribir la placa en la revision o volver al formulario.
Cancelar o volver a manual conserva el borrador anterior. Si se confirma otra
placa, se limpian datos y servicios del vehiculo previo antes de consultar para
evitar mezclarlos si falla la red; orden y observaciones se conservan.

CameraX captura en memoria; ML Kit usa el modelo latino incluido en el APK.
No se envian fotografias al servidor ni se guardan en galeria/cache. Solo se pide
CAMERA al abrir captura, no almacenamiento, audio ni ubicacion. Si se deniega,
se puede continuar manualmente o abrir ajustes por accion explicita.
El reconocimiento no requiere servidor; las consultas y el registro si.

Las sugerencias no validan un formato legal ni garantizan exactitud; no se
sustituyen O/0 ni I/1. Es necesaria la revision humana antes de continuar.
La captura esta deshabilitada durante guardado o revision de un envio incierto.

## Preparar el equipo

1. Instalar Android Studio: https://developer.android.com/studio
2. Desde SDK Manager instalar Android SDK Platform 35, Build-Tools 35.0.0 y
   Android SDK Platform-Tools. Para emulacion, instalar Android Emulator y una
   imagen API 26 o superior.
3. Configurar JDK 17 y `JAVA_HOME`; agregar `%JAVA_HOME%\bin` a `PATH`.
4. Abrir esta carpeta desde Android Studio y sincronizar Gradle. Se creara
   `local.properties` con la ubicacion local del SDK; no subirlo a Git.
   Alternativamente, definir `ANDROID_HOME` apuntando al SDK instalado.

No hace falta instalar Gradle globalmente; se incluye Gradle Wrapper.
La primera compilacion requiere Internet para descargar Gradle y dependencias.

## Compilar en Windows

Desde la terminal PowerShell de esta carpeta:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Informe lint: `app/build/reports/lint-results-debug.html`.
Este APK usa firma de depuracion; no es una version de produccion.

Si Java muestra `PKIX path building failed` en Windows, usar los certificados
confiables del sistema en la terminal actual y reintentar el comando Gradle:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
```

Esto mantiene la validacion TLS activa; no desactivar la comprobacion de certificados.

## Emulador local configurado

Se instalo JDK 17 y se configuraron `JAVA_HOME`, `ANDROID_HOME` y el `PATH` de
usuario. Reiniciar VS Code para que sus nuevas terminales hereden estos cambios.
El SDK incluye Platform 35, Build-Tools 35.0.0 y una imagen Google APIs x86_64.
El AVD `CarWash_API_35` usa un perfil Pixel 6 con Android 15 (API 35) y WHPX.
Android Studio es opcional para usar esta instalacion desde la terminal.

Para iniciar el emulador sin bloquear la terminal:

```powershell
Start-Process -FilePath "$env:ANDROID_HOME\emulator\emulator.exe" -ArgumentList '-avd', 'CarWash_API_35', '-no-metrics'
```

Cuando termine de arrancar:

```powershell
adb devices
.\gradlew.bat :app:installDebug
adb shell am start -W -n com.carwash.mobile/.MainActivity
```

Las pruebas instrumentadas pueden desinstalar la app al terminar. Para dejarla
abierta, repetir `:app:installDebug` y el comando `adb shell am start` despues de
las pruebas.

## Ejecutar en un celular

Activar Opciones de desarrollador y Depuracion USB, conectar el celular y
autorizar este equipo en el dispositivo. En Windows puede requerirse el driver
USB del fabricante. Con Platform-Tools en `PATH`:

```powershell
adb devices
.\gradlew.bat :app:installDebug
adb shell am start -n com.carwash.mobile/.MainActivity
.\gradlew.bat :app:connectedDebugAndroidTest
```

Tambien se puede usar Run desde Android Studio. Para un emulador, iniciarlo desde
Device Manager antes de ejecutar los mismos comandos.

## Trabajar con Spec Kit

La CLI `specify` ya estaba instalada en el equipo y se inicializo con:

```powershell
specify init --here --integration copilot --script ps --non-interactive
specify check
```

Si se necesita instalar la CLI en otro equipo: `uv tool install specify-cli`.
No volver a inicializar esta carpeta: las plantillas oficiales ya estan incluidas.

Abrir Copilot Chat en modo agente. Los comandos se escriben en el chat, no en
PowerShell. La version instalada utiliza guiones, no puntos:

```text
/speckit-specify Describir la siguiente funcionalidad y sus usuarios
/speckit-plan Usar la base Android Kotlin y Compose existente
/speckit-tasks
/speckit-implement
/speckit-converge
```

Revisar cada resultado antes de continuar. Para modificar principios usar
`/speckit-constitution`; para aclarar requisitos usar `/speckit-clarify`.
Si los comandos no aparecen, recargar la ventana de VS Code y abrir un chat nuevo.

## Conectar la API por Wi-Fi

`localhost` en Android representa el propio dispositivo, no el computador.

| Dispositivo | Direccion del servidor |
| --- | --- |
| Emulador Android en el mismo equipo | `http://10.0.2.2:5197` (valor inicial) |
| Celular en la misma Wi-Fi | `http://IP-WIFI-DEL-COMPUTADOR:5197` |

La IP Wi-Fi observada al implementar fue `192.168.1.15`; confirmar con `ipconfig`
en el computador porque puede cambiar. Se puede reservar esa IP en DHCP.
No usar una IP de VPN ni asumir que compartir el nombre de Wi-Fi garantiza
acceso entre dispositivos: el router debe permitir comunicacion entre clientes.

### Habilitar el servidor

Inicialmente la API escuchaba solo en `127.0.0.1` y `::1`. Con autorizacion del
usuario se agrego un perfil `wifi` a `Properties/launchSettings.json` en el
proyecto externo CarWashApi y se reinicio su instancia. Ahora escucha tambien
en `192.168.1.15:5197`; ambas direcciones respondieron HTTP 200.

Para futuros arranques, desde la carpeta de la API:

```powershell
dotnet run --launch-profile wifi
```

El perfil conserva localhost y usa la IP Wi-Fi observada. Si DHCP la cambia,
actualizar la direccion del perfil y de la app o sobrescribirla al arrancar:

```powershell
dotnet run --no-launch-profile --urls "http://localhost:5197;http://192.168.1.15:5197"
```

No ejecutar otra instancia mientras el puerto 5197 siga ocupado. Los perfiles
http/https anteriores se conservaron. No se cambiaron endpoints, autenticacion,
reglas de negocio ni datos de la API.

### Perfil de red y firewall

Windows clasifica actualmente esta Wi-Fi como Publica. No se cambio ese perfil
ni el firewall. Si es una red propia y confiable, el usuario debe marcarla como
Privada en Configuracion de Windows, Red e Internet, propiedades de su Wi-Fi.
No marcar redes publicas o ajenas como privadas para ejecutar esta app.

Si Windows bloquea el puerto, el usuario debe autorizarlo en una red privada
confiable. Esta regla requiere una terminal de administrador y debe ejecutarla
el usuario, no la aplicacion:

```powershell
New-NetFirewallRule -DisplayName "CarWash API LAN" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5197 -Profile Private -RemoteAddress LocalSubnet
```

No desactivar el firewall ni habilitar la regla en redes publicas.
Desde el navegador del celular verificar primero:
`http://192.168.1.15:5197/api/TipoVehiculo/GetAll`.
Despues configurar `http://192.168.1.15:5197` en el engranaje de la app y conectar.
No se necesita configurar CORS para el cliente Android nativo.

HTTP transmite datos sin cifrar. Usar solo una red local confiable, sin exponer
el puerto a Internet. Para uso fuera de esa red o despliegue productivo, preparar
HTTPS y seguridad del servidor. La app admite HTTPS; rechaza HTTP a dominios
publicos, credenciales en la URL y redirecciones a otros destinos.

## Documentacion

- `.specify/memory/constitution.md`: principios del proyecto.
- `specs/001-android-bootstrap/`: especificacion, plan, tareas y contrato inicial.
- `specs/002-ingreso-vehiculo/`: ingreso existente, contrato HTTP, plan y pruebas.
- `specs/003-captura-placa/`: especificacion activa, contrato de captura y tareas.
- `.github/skills/`: comandos oficiales generados por Spec Kit.
- `.github/copilot-instructions.md`: reglas compartidas para Copilot.

## Estado de verificacion

Verificado el 2026-10-28 en el emulador `CarWash_API_35`:

- `:app:assembleDebug :app:lintDebug`: correctos; APK generado.
- `:app:testDebugUnitTest`: 22 pruebas, 0 fallos.
- `:app:connectedDebugAndroidTest`: 15 pruebas, 0 fallos, 0 omitidas.
- OCR real con imagen sintetica, imagen rotada y fondo vacio: correcto.
- Confirmacion sin escritura, correccion, sugerencias, reintento y fuente grande
   verificados junto con las pruebas del formulario existente.
- CameraX en emulador: vista previa y captura reales; la escena virtual sin placa
   conduce a revision vacia editable, no a un registro.
- Permiso bloqueado en emulador: pantalla de estado y salida manual conservan borrador.
- Revision conserva texto al rotar y cambiar tema/fuente; modo oscuro y fuente
   1.5 inspeccionados; contraste oscuro ajustado a la paleta existente.
- Guardados, errores y consultas por placa probados con dobles de servidor;
   no se crearon ingresos ficticios en la base real.
- Conexion real desde el emulador a `10.0.2.2:5197`: catalogos y caja activa recibidos.
- API reiniciada con perfil wifi autorizado; localhost e IP Wi-Fi responden 200.
- App conectada despues a `192.168.1.15:5197` y direccion guardada; esto no
   sustituye una comprobacion desde un celular fisico atravesando el firewall.
- Seleccion local de servicio real: tarifa y total visibles de $30.000,00.
- App reinstalada y abierta despues de las pruebas.
- Capturas locales: `app/build/reports/ingreso-vehiculo.png` e
   `app/build/reports/ingreso-servicios.png`; configuracion de conexion en
   `app/build/reports/ingreso-conexion-wifi.png` (generadas, no versionadas).
- Capturas de camara: `app/build/reports/captura-estado.png`,
  `captura-revision.png`, `captura-permiso.png`, `captura-manual.png`,
  `captura-horizontal.png` y `captura-accesible.png` en ese mismo directorio.

La revision visual se centro en la captura; no reemplaza la revision exhaustiva
del formulario completo de 002. Quedan pendientes celular fisico, precision con
placas reales, baja luz/reflejos, distancia/enfoque y conexion Wi-Fi atravesando
el firewall del computador. No se promete precision usando solo el emulador.
Evidencia detallada en `specs/003-captura-placa/tasks.md`.