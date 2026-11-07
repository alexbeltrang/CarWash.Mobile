# Validacion: comprobante y retiro

## Reimpresion sin entrega en Android (2026-11-06)

- Navegacion Reimprimir con icono de impresora y pantalla Reimpresion del recibo.
	Sin casilla de revision, boton/accion de entrega ni estado/fecha/encargado de
	entrega. Consulta por placa, actualizacion y cola de copias de 80 mm conservadas.
- Ejecutado con exito `:app:testDebugUnitTest :app:assembleDebug
	:app:assembleDebugAndroidTest :app:lintDebug`; pruebas del modelo verifican
	consulta/actualizacion y recuperacion de impresion sin confirmar entregas.
- `:app:connectedDebugAndroidTest` filtrado a RetiroScreenTest: 3 aprobadas
	en Xiaomi fisico cmkrkjp7gexgs8ca. Navegacion/pantalla a 320 dp sin entrega,
	recibo con datos legados imprimible y confirmacion/cancelacion de otra copia.
	Repositorio/impresora simulados, sin turnos reales ni papel adicional.
- La captura temporal no estuvo disponible para recuperarla tras el comando
	conectado; no se declara inspeccion de esa imagen. Las aserciones Compose pasaron.
- APK anterior respaldado fuera de Git. APK actualizado instalado con
	`adb install -r -t` (Success) y abierto con `am start -W` (Status: ok).
	Sin cambios al servidor/escritorio, migraciones ni borrado intencional de datos.

## Ajuste de aviso en ingreso (2026-11-06)

- Se oculta el mensaje rutinario de comprobante de 80 mm solicitado para
	PENDING/SUBMITTING/SUBMITTED. Se conservan confirmacion de ingreso,
	impresion automatica y advertencias FAILED/UNKNOWN; sin cambios de API.
- Ejecutado con exito: `:app:testDebugUnitTest --tests com.carwash.mobile.ingreso.IngresoViewModelTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`.
- Ejecutado `:app:connectedDebugAndroidTest` filtrado a los dos nuevos casos
	de MainActivityTest: 2 pruebas aprobadas en Xiaomi fisico cmkrkjp7gexgs8ca.
	Repositorio e impresion simulados; no turnos reales ni hojas adicionales.
- APK previo respaldado fuera de Git en el respaldo local de esta revision.
	APK actualizado instalado con `adb install -r -t` (Success) y abierto con
	`am start -W` (Status: ok); sin borrar datos ni reiniciar el servidor.

## API

Usar SQLite temporal y fixture autenticada, nunca datos de clientes reales.

```powershell
dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path "$env:TEMP/CarWash.ReporteVentas.TestArtifacts" --filter 'FullyQualifiedName~RetiroVehiculosTests|FullyQualifiedName~EmailNotificationServiceTests' --verbosity minimal -p:WarningLevel=0
```

Verificar codigo estable/unico, HTML/texto, sesion, entrada invalida, revision,
codigo incorrecto, doble entrega/concurrencia y ausencia de cambios en pagos.

## Android

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console=plain
```

Instrumentacion exclusivamente en Xiaomi fisico, instalacion -r -t sin borrar
datos. Pruebas con repositorio simulado; no enviar mensajes reales.

## Escritorio e impresion

Compilar CarWash.csproj con MSBuild de Visual Studio, OutputPath/IntermediateOutputPath
temporales para no reemplazar el ejecutable operativo. Verificar vista previa,
saltos de linea y ancho elegido. Prueba fisica solo con impresora instalada y
autorizacion; no afirmar salida de papel sin ejecutarla.

## Estado

Implementacion desplegada el 2026-11-06 con autorizacion especifica del usuario
para respaldar/reiniciar API y actualizar/reabrir escritorio. APK instalada con
`adb install -r -t`, conservando datos; no se uso emulador.

### Evidencia ejecutada

- API: 11 casos enfocados de retiro/correo y regresion completa de 93 casos,
	cero fallos, SQLite temporal. Los casos enfocados estan incluidos en los 93.
- Android: 101 pruebas JVM sin fallos; assembleDebug, assembleDebugAndroidTest
	y lintDebug terminaron correctamente.
- Xiaomi fisico: `OK (5 tests)` de RetiroScreenTest, codigo en WhatsApp y
	navegacion de ambos roles. Primer intento bloqueado por pantalla en reposo;
	repeticion exitosa tras desbloqueo. Datos/repositorios e intents simulados,
	sin registrar turnos ni enviar mensajes reales. Captura revisada a 320 dp:
	servicios, codigo, total y entrega visibles, fechas locales, sin solapamiento.
- Escritorio: MSBuild aislado correcto y una prueba CompiledDesktop exitosa
	contra API temporal, incluyendo codigo estable, doble entrega rechazada,
	datos financieros intactos y renderizado no vacio/multipagina a 58 y 80 mm.

### Despliegue verificado

- API perfil wifi en `http://192.168.1.5:5197/`; Swagger HTTP 200 local y Wi-Fi,
	GET anonimo de retiro HTTP 401. Proceso de esta revision: PID 57496.
- Respaldo de binarios API/escritorio y SQLite bajo
	`%LOCALAPPDATA%/CarWash/ApiBackups/before-retiro-20261106-170304/`.
- Configuraciones API y escritorio conservan sus hashes anteriores. DLL API y
	EXE escritorio coinciden SHA-256 con sus salidas aisladas validadas:
	API `70A5B35BF90D8AA897270FC3D8236392CD138E3E289552A8AB7FCC839086D430`;
	escritorio `340571D9EF4EDB5CFA290D2B0D7E2354508BECE7F6DA5E349B4488AB5AEE7F10`.
- SQLite consultada en solo lectura: tabla/indice de comprobantes presentes,
	cero comprobantes creados como prueba. Comparacion bidireccional de todas las
	columnas de Turnos, TurnosMovimientos y TurnoServicios frente al respaldo:
	cero diferencias. No se entregaron vehiculos reales durante la validacion.
- Escritorio reabierto y respondiendo (PID 61336); app relanzada `Status: ok`.
	Reiniciar API invalida sesiones en memoria: volver a iniciar sesion en ambos.

### Acceso y limites

Android: destino Retiro. Escritorio: Operacion > Comprobante y retiro; consultar
placa, seleccionar 58/80 mm y controlador Windows para vista previa/impresion.
Codigo presentado y revision explicita confirman una sola entrega, sin exigir
ni modificar pagos. El digital conserva correo y WhatsApp manual existentes.

Pendiente T013: salida real de papel/seleccion del controlador termico USB.
No se enviaron trabajos fisicos ni correos/WhatsApp a clientes; presentacion
en buzon real y siguiente operacion legitima requieren comprobacion del usuario.
Los correos ya aceptados no se reenvian para incorporar el codigo nuevo.

## Revision central USB (2026-11-06)

El usuario confirmo boton Imprimir en ambos clientes y autorizo HP USB como
destino actual. Movil/escritorio solicitan a la API; el trabajador de este PC
envia a la cola Windows y a la impresora. No imprimir automaticamente al guardar.
Android: Retiro > consultar placa > Imprimir. Escritorio: Operacion > Comprobante
y retiro > consultar placa > Imprimir. Consultar impresion recupera estado;
otra copia exige confirmacion. Vista previa de escritorio no envia trabajo local.

### Prueba fisica ejecutada

HP Ink Tank 310 series, controlador PCL-3, USB001, Spooler activo. Una sola hoja
diagnostica marcada PRUEBA USB - NO VALIDO PARA RETIRO, con acentos y total;
sin insertar turnos/comprobantes en SQLite real. Adaptador Windows de la API
ejecutado desde la prueba optativa: un caso correcto. Windows registro trabajo
3, TotalPages=1; el usuario confirmo una hoja completa y legible. La impresion
fisica acredita la HP, no una termica ni un ticket real de un cliente.

No repetir esta prueba en suites normales: la compilacion requiere
`-p:RunUsbPrintTests=true` y ejecucion filtrada explicitamente al caso
AuthorizedUsbDiagnosticSubmitsOneSyntheticSheet. Configurar CARWASH_USB_PRINTER
solo tras autorizacion. Las fixtures normales deshabilitan destino fisico siempre.

### Evidencia de clientes y cola

- Regresion completa: 102 casos API/escritorio compilado, cero fallos; incluye
	ocho casos de impresion y uno de compatibilidad. Tras reforzar aislamiento de
	fixtures, nueve casos enfocados de impresion/escritorio pasaron nuevamente.
- Android: 106 pruebas JVM, cero fallos; APK, APK de pruebas y lint correctos.
	Xiaomi fisico: `OK (3 tests)` del retiro, codigo incorrecto y boton de impresion
	con otra copia confirmada, usando repositorios simulados. Captura a 320 dp
	revisada, legible. Foco liberado antes de acciones para no desplazar el aviso.
- Primeros intentos ADB offline: reconexion dirigida del dispositivo recupero
	el enlace. APK anterior respaldada y nueva instalada -r -t sin borrar datos.
	Tras las pruebas el dispositivo se desconecto: relanzamiento final por ADB no
	disponible; abrir la app manualmente, sin repetir instalacion ni usar emulador.

### Configuracion y despliegue

Perfil local fuera de Git: `%PROGRAMDATA%/SplashCar/printing.json`:

```json
{ "Printing": { "Enabled": true, "PrinterName": "HP Ink Tank 310 series" } }
```

Cambiar nombre al controlador instalado y reiniciar API con autorizacion; cuenta
del proceso debe tener acceso. Ancho 58/80 limita contenido; se respeta papel del
controlador. HP usa hoja normal. API/PC e impresora deben permanecer disponibles.
Si la API se muda, instalar trabajador donde este USB o compartir impresora
Windows con permisos; la direccion API no hace remoto el USB automaticamente.

Despliegue autorizado aplicado: perfil wifi5197, API PID17132, escritorio reabierto
PID59548; tabla/indice de cola creados. Swagger HTTP200 local/LAN, rutas presentes,
GET anonimo de impresion HTTP401. Cola real cero trabajos de prueba.
Comparacion SQLite solo lectura frente al respaldo, todas las columnas de Turnos,
TurnosMovimientos, TurnoServicios y ComprobantesRetiro: cero diferencias.
Configuraciones API/escritorio conservaron hashes previos. Binarios coinciden con
publicacion/compilacion aisladas: API SHA256
`67C487DFE4F408D8FDA1C30C37D6631857C871515541D3491E00EBF27A2B1244`, escritorio
`95747FB7D5EED19E84C469FBDED07A8BB4A4C48E5455104B73AC88AF028A563F`.
Respaldo de API, escritorio, SQLite, configuracion de impresion y APK anterior en
`%LOCALAPPDATA%/CarWash/ApiBackups/before-central-print-20261106-174443/`.
Reinicio invalida sesiones: volver a iniciar sesion. T014-T019 completadas;
T013 sigue pendiente porque no hay termica instalada para validar papel 58/80 mm.

## Revision vigente: documento automatico de 80 mm y entrega fisica (2026-11-06)

Esta seccion sustituye las instrucciones historicas anteriores de boton manual,
selector de 58 mm y requisito de codigo. El usuario cancelo QR/lectura por
camara antes del despliegue; solo necesita el documento y entrega fisica.

- Nuevo ingreso desde Android o escritorio: guardar turno y todos sus servicios
	solicita automaticamente una copia de 80 mm a la API del PC de la impresora.
	Actualizar no imprime automaticamente. Doble solicitud/respuesta perdida
	conserva el mismo UUID del servidor; fallo de impresion no repite el ingreso.
- Documento: placa, marca, cliente, fecha, servicios y total confirmado, sin
	codigo ni QR. Termica selecciona formato de 80 mm del controlador instalado;
	HP usa contenido equivalente en hoja normal. No se acredita termica real.
- Retiro: consultar placa, revisar entrega fisica y confirmar. Sin codigo ni
	escaneo; requiere sesion y revision, entrega unica y pagos sin modificaciones.
	DTO/codigo legado y digital previo se conservan por compatibilidad, no como
	requisito ni credencial de entrega. Captura de placas permanece intacta.

### Validacion ejecutada de esta revision

- 105 casos API/escritorio: todos correctos, datos temporales e impresora
	simulada. Incluye 11 de impresion (80 mm, paginacion, servicios completos,
	idempotencia, fallos/reinicio), entrega sin codigo y escritorio compilado.
- 110 pruebas JVM correctas; assembleDebug, assembleDebugAndroidTest y lintDebug
	correctos. Xiaomi fisico: OK (3 tests) de entrega sin codigo, confirmacion
	fallida recuperable y otra copia con revision. Primera ejecucion: dos casos
	correctos y uno sin jerarquia Compose al iniciar; repeticion completa correcta
	sin cambiar codigo. Captura de 320 dp inspeccionada, sin campos codigo/QR.
- Una nueva hoja diagnostica autorizada en HP Ink Tank 310 series, USB001/PCL-3:
	trabajo Windows 4, documento de 80 mm sin codigo ni QR, TotalPages=1. Usuario
	confirmo una sola hoja completa y legible. Sin turnos/clientes reales ni envios
	de correo/WhatsApp. No repetir la prueba optativa sin nueva autorizacion.

### Despliegue y respaldo

Usuario autorizo actualizacion/reinicio y respaldo; no se creo rama ni commit.
Respaldo fuera de Git en
`%LOCALAPPDATA%/CarWash/ApiBackups/before-auto-entry-qr-20261106/`
(nombre conservado del inicio de trabajo, antes de cancelar QR): ApiBin,
DesktopBin, Database, printing-config.json y app-before-documento.apk.

API actual PID62948, terminal `eeed4c52-8d5d-4d4e-b2c5-51c0c92378dc`, perfil
wifi en `http://192.168.1.5:5197/`. Swagger HTTP200 local/LAN, ruta automatica
publicada y GET anonimo de retiro HTTP401. Comparacion SQLite en solo lectura,
bidireccional y todas las columnas: cero diferencias en Turnos,
TurnosMovimientos, TurnoServicios, ComprobantesRetiro e ImpresionesComprobante;
cola real cero trabajos. Configuraciones conservaron hashes anteriores.

Binarios coinciden con salidas aisladas:
- API SHA256 `56F16BD8DAFCE65104AE08B218617B354C03C35DEBB55426DA04B8A88414E1CB`.
- Escritorio SHA256 `E3EABE3755E4044762A954DFE26AFF9B4C929CA54F9F89EC0D136462B3A16732`.

Escritorio reabierto PID3848 y celular relanzado Status: ok. APK instalada -r -t
sin borrar datos. Volver a iniciar sesion tras reiniciar API. Proximo ingreso
legitimo puede comprobar el flujo real de guardado a papel; no se crearon ingresos
ficticios para probarlo. T020-T024 completos; solo T013 externa pendiente con
termica real de 80 mm, incluyendo controlador, largo/corte de papel y legibilidad.