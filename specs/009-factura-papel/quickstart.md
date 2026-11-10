# Verification: Factura papel

SQLite temporal, empresa/comprador/autorizacion sinteticos exclusivamente en pruebas.
Probar permisos y revision de configuracion, vigencia/rango, IVA0 y fraccionario,
emision repetida/concurrente, rechazo de abonos, snapshots y cobros conservados.
Cola simulada; ningun documento real ni envio a DIAN, SMTP o impresora de prueba.

API: dotnet test con artifacts-path temporal y RunUsbPrintTests=false.
Escritorio: MSBuild con OutputPath/IntermediateOutputPath temporales y probe compilado;
habilitar RunDesktopCompatibilityTests=true (por defecto se excluye).
Recibo80mm: bitmap/paginacion con texto/logo largo; inspeccionar captura.
No certificar cumplimiento legal. Datos reales/autorizacion y asesor fiscal
corresponden al negocio. Despliegue posterior con aprobacion nueva y respaldo.

## Evidencia 2026-11-06

- API/WinForms: 138 ejecutadas, 138 aprobadas, 0 omitidas; TRX temporal
	`%TEMP%/CarWash.Factura.TestResults/factura-gates.trx`.
- 17 casos FacturacionTests y probe compilado: permisos, datos incompletos,
	abonos, IVA incluido 0/5/19, vigencia/rango, consecutivos concurrentes, UUID
	repetido/cruzado, snapshots, busqueda por placa original, cola falsa sin cobros.
- MSBuild net481 exitoso en `%TEMP%/CarWash.Empresa.Desktop/`.
- Capturas inspeccionadas `%TEMP%/CarWash.FacturaFormulario.png` y
	`CarWash.Factura80mm.png`; pixels no blancos/logo y progreso de paginacion.
- Android: 117 JVM/0 fallos, assembleDebug, assembleDebugAndroidTest y lintDebug
	BUILD SUCCESSFUL. Sesion cifrada ya instalada preservando datos en Xiaomi.
- Pruebas fisicas de sesion: dos casos AndroidKeyStore/restauracion y alteracion
	aprobados; el test existente de contrasena fallo por falta de jerarquia Compose
	en el primer lote y paso sin cambios aislado. No afirmar tres exitosas juntas.
- No se ejecutaron cobros/facturas reales, envios externos ni impresion fisica.
- Despliegue ejecutado con aprobacion nueva del usuario. Ninguna configuracion
	fiscal real fue creada automaticamente; empresa real existente conservada.

## Despliegue Inicial Autorizado

Respaldo `%LOCALAPPDATA%/CarWash/ApiBackups/before-factura-20261106-214525`:
ApiBin, DesktopBin, ApiSourceConfig, printing-config.json y Database/CarWash.db
via SQLite BackupDatabase. Solo cuatro binarios reemplazados; ocho hashes de
configuracion conservados. Comparacion de solo lectura EXCEPT bidireccional,
todas las columnas: 28 tablas previas identicas. Empresa1, configuracion0, factura0.

API PID29316, terminal `f42dda01-1d9d-4058-b649-273b4a06d85f`, perfil wifi
`http://192.168.1.5:5197/`; mantener en ejecucion. Swagger local/LAN200, rutas
de factura presentes, GET configuracion anonimo401. Escritorio PID53472 responde.
API SHA256 `67B6C0359E8FA68A7B7609C12BA7CF13A730E43148FF8AA469BE718D7AAE3F6F`.
Escritorio SHA256 `AFC27A751A0ACE9E2CC328390BFB7C619BE3CA6956A13FCCD340D01DAD3A09E1`.
El reinicio invalida sesiones del servidor; iniciar sesion una vez en ambos
clientes. Android recuerda sesion cifrada hasta el vencimiento original (max9h),
logout o revocacion, sin guardar usuario/contrasena del formulario de acceso.
App fisica reabierta Status:ok/HOT, sin emulador ni nuevas escrituras/mensajes.

## Convergencia

11 FR, 5 SC y 9 escenarios de aceptacion contrastados con codigo/pruebas y
contrato; decisiones de plan y cinco principios constitucionales revisados.
Sin brechas funcionales pendientes ni tareas adicionales de construccion.
Fuera de verificacion: habilitacion fiscal real y papel/termica80mm; no se
declara cumplimiento legal ni se ejecuta una factura ficticia de produccion.

## Uso Operativo

Administrador registra empresa real en Empresa y autorizacion/rango/vigencia/IVA
en Configuracion facturacion. No hay tasa asumida ni registro sintetico inicial.
Al terminar cobro completo confirmado pregunta si desea generar factura, con No
predeterminado. No cierra sin formulario, factura ni impresion; Si abre comprador.
Nombre/tipo/numero de documento requeridos; direccion opcional. Emitir tras esa
eleccion no vuelve a preguntar para el mismo turno; imprimir permanece explicito.
Cancelar no repite el pago. Facturas consulta por turno/placa y muestra historial hasta100.
Reimpresion consulta estado/confirmacion de otra copia; SUBMITTED no prueba papel.
La impresora configurada es HP Ink Tank 310 series; no hay termica80mm instalada.

## Revision Factura Opcional 2026-11-06

- Ultima indicacion sustituye el check automatico: siempre preguntar al cobrar.
- MSBuild escritorio net481 exitoso en salida temporal, sin reemplazar aplicativo.
- 20 ejecutadas/20 aprobadas/0 omitidas: FacturacionTests (19 casos) y probe
	WinForms compilado. TRX `%TEMP%/CarWash.FacturaOpcional.TestResults/flujo.trx`.
- Probe prueba Si/No/cancelacion, No predeterminado, formulario solo con Si,
	comprador sin direccion, emision real aislada sin segunda pregunta ni impresion
	automatica; conserva pruebas de permisos, IVA, historial, idempotencia y render.
- Sin errores de editor en formularios/pruebas ni conflictos de espacios escritorio.
- No cobros/facturas reales, mensajes externos ni papel. Android/API operativos
	sin cambios. Actualizacion escritorio autorizada y ejecutada con respaldo.

Respaldo `%LOCALAPPDATA%/CarWash/ApiBackups/before-factura-opcional-20261106/DesktopBin`,
binario anterior verificado por hash antes de cerrar ventana normalmente.
Solo CarWash.exe/CarWash.pdb reemplazados desde la salida MSBuild probada;
CarWash.exe.config conserva SHA256 anterior. Escritorio PID61156 responde.
SHA256 escritorio `C271788549F142C2B8A430053FA7D8F5C1AE1D3A020F1A677FEB692ECDE36FE5`.
API PID29316 permanece sin reiniciar, DLL con hash anterior y Swagger HTTP200.
No cambios al celular ni al esquema/base de datos; sin acciones operativas reales.

## Estilo Y Menu Escritorio 2026-11-09

- Empresa, Configuracion facturacion, Facturas, Comprobante y retiro y Reporte
	Diario reutilizan UIHelper.AplicarEstiloCaja: encabezado RGB15/76/129, fondo
	RGB240/245/250, Segoe UI, botones planos verde/azul y Cerrar accesible por Escape.
- Las cinco opciones quedan en Administracion; no quedan duplicadas en raiz,
	Operacion o Sistema. Permisos anteriores conservados: empresa/fiscal y reporte
	administrativos; factura/retiro operativos. API y operaciones sin cambios.
- MSBuild net481 exitoso en salida temporal. TRX
	`%TEMP%/CarWash.EstiloCaja.TestResults/estilo-final.trx`: 20 ejecutadas,
	20 aprobadas, 0 omitidas. Probe compilado verifica ambos perfiles, menus,
	paleta/tipografia, encabezado renderizado, cierre visible y documentos con alto
	minimo 100px en tamanos normal y minimo; facturacion/cola siguen aisladas.
- Diez capturas `%TEMP%/CarWash.EstiloCaja.Frm*.png` (normal y .min); inspeccionadas
	las cinco minimas. Se corrigieron margen de encabezado y altura de documento
	Retiro; tamano minimo600x720. Reporte usa contenedores redimensionables.
- Sin errores de editor ni conflictos de espacios en los archivos tocados.
- Instalacion autorizada y ejecutada. Sin cambios en Android, datos operativos,
  impresion fisica ni reinicio del servidor API.

Respaldo `%LOCALAPPDATA%/CarWash/ApiBackups/before-estilo-caja-20261109-105129`,
version instalada verificada por hash antes de sustituirla (escritorio ya cerrado).
Solo CarWash.exe/CarWash.pdb reemplazados; configuracion con hash conservado.
Escritorio PID12484 Responding=true; SHA256
`3C5C6D987DD6104A767C35092952257C156F214F67648FC9922BF42AE12052D0`.
API PID29316 sin reiniciar y DLL sin cambiar; Swagger local HTTP200.

## Disenador WinForms 2026-11-09

- Empresa, Configuracion facturacion, Facturas, Comprobante y retiro y Reporte
	Diario ahora son clases partial con InitializeComponent, Designer.cs y resx
	registrados mediante DependentUpon/SubType en el proyecto de escritorio.
- Controles, jerarquia y propiedades visuales persistidos en el disenador;
	eliminado UIHelper.AplicarEstiloCaja. Constructores conservan solo eventos y
	estado operativo, con proteccion de modo diseno antes de cargar datos.
- FrmFactura dispone de constructor publico sin parametros y conserva las
	variantes operativas; formulario principal conserva permisos y marca de empresa.
	Las opciones nuevas se declaran en su Designer y ya no tienen imagen izquierda.
- MSBuild net481 exitoso en `%TEMP%/CarWash.Diseno.Desktop/`. Se uso
	`/p:RuntimeIdentifier=win-x64` para satisfacer el paquete SQLite existente,
	sin modificar paquetes ni configuracion operativa.
- TRX `%TEMP%/CarWash.Diseno.TestResults/disenadores-final.trx`: 20 ejecutadas,
	20 aprobadas, 0 fallidas. Incluye FacturacionTests y probe WinForms compilado.
- Probe verifica carga de los seis tipos en DesignSurface/STA, constructor vacio,
	controles presentes, guardia de diseno y serializacion CodeDOM de un titulo
	editado. Tambien verifica opciones sin imagen, dos perfiles, aspecto normal y
	minimo y operaciones aisladas existentes, sin impresoras fisicas.
- Para editar visualmente abrir el proyecto CarWash en Visual Studio para Windows
	y seleccionar Ver disenador en el archivo principal de cada formulario.
	No se verifico una sesion manual del editor de Visual Studio; la validacion
	automatizada usa los servicios oficiales System.Design de WinForms.
- Validacion inicial sin cambios en API/Android ni datos reales y sin sustituir
	binarios operativos. Instalacion posteriormente autorizada y ejecutada segun
	la siguiente seccion (T028/T031).

## Barras Nativas, x64 Y Despliegue 2026-11-09

- vscrollbar20 y hscrollbar19 eran copias independientes de las barras internas
	de DataGridView, no controles enlazados que fueran a ajustarse en ejecucion.
	Retiradas de Facturas y las equivalentes de Reporte diario; ambas tablas
	conservan sus barras nativas y el desplazamiento gestionado por WinForms.
- CarWash.csproj selecciona x64 por defecto en Debug/Release, sin Prefer32Bit;
	CarWash.slnx declara y mapea explicitamente x64 al proyecto. Una regla Build
	copia e_sqlite3.dll x64 junto al ejecutable; paquetes NuGet sin modificaciones.
- MSBuild proyecto Debug/Release y solucion Debug/Release: cuatro compilaciones
	aprobadas sin /p:Platform ni /p:RuntimeIdentifier. Cabecera PE AMD64 verificada
	tanto en CarWash.exe como en e_sqlite3.dll en ambas salidas temporales.
- TRX `%TEMP%/CarWash.Diseno.TestResults/barras-x64-final.trx`: 20 ejecutadas,
	20 aprobadas, 0 fallidas. Regresion comprueba una barra nativa por eje y
	desplazamiento de filas cuando hay desbordamiento, ademas de diseno/operaciones.
- Instalacion autorizada por el usuario en esta revision. No habia escritorio
	abierto; bin/Debug solo contenia e_sqlite3.dll, sin ejecutable/configuracion.
	Respaldo `%LOCALAPPDATA%/CarWash/ApiBackups/before-disenador-x64-20261109-120330`
	guarda ese estado, App.config fuente y configuracion operativa del respaldo
	anterior. Se instalaron 18 archivos de la salida Debug probada y sus hashes
	se comprobaron. appSettings identicos al respaldo anterior; configuracion
	runtime/binding redirects corresponde a las dependencias actuales probadas.
- Escritorio iniciado en bin/Debug, PID61936 Responding=true, ventana
	AUTO SPLASH - CarWash. SHA256 ejecutable
	`5A6AF4D5B39325750E9B1F99B4E3752DFCF0ABCC278C66A4DE009550F6476BFA`.
	En la comprobacion posterior ya no habia instancia abierta ni eventos de fallo
	CarWash1000/1026 recientes; no se atribuye el cierre a una causa no verificada
	ni se vuelve a abrir automaticamente. Swagger HTTP200 confirmado.
	API PID29316 y hora de inicio conservados; no reinicio, cambios de Android,
	cobros/facturas reales ni impresion fisica. Recargar solucion en Visual Studio
	para actualizar la seleccion de plataforma a x64.

## Cargador Del Disenador 2026-11-09

- Ante error global de clase base System.Windows.Forms.Form no cargada en
	VSCodeDomDesignerLoader, referencias del proyecto presentes y resolucion
	de diseno sin errores. Proyecto x64 sin UseWinFormsOutOfProcDesigner definido.
- Activado UseWinFormsOutOfProcDesigner=True en CarWash.csproj para usar el
	disenador fuera de proceso; x64 y .NET Framework4.8.1 permanecen sin cambios.
- Verificado con MSBuild de Visual Studio Professional18.8.12023.21: propiedad
	True, Platform/PlatformTarget=x64, ResolveReferences con DesignTimeBuild=true
	y BuildingInsideVisualStudio=true sin errores; solucion Debug compilada en
	`%TEMP%/CarWash.DesignerFix/Debug/`, sin sustituir el escritorio operativo.
- FxDesignToolsServer.exe x64 presente en la instalacion de Visual Studio.
	No se verifico apertura manual en Visual Studio ni se declara resuelto el
	mensaje dentro de una sesion aun sin recargar: cerrar pestanas de diseno,
	recargar proyecto/solucion y volver a abrir Ver disenador. Reconstruir desde
	Visual Studio si conserva una salida previa.
- No se modificaron los Designer de Facturas/Retiro editados por el usuario,
	ni API, datos, paquetes o aplicacion instalada. No repetir las 20 pruebas de
	facturacion previas como evidencia nueva de este cambio de configuracion.

## Retencion De Respaldos 1.1.1 (2026-11-10)

Reemplaza la regla24/7/4 de1.1.0: conservar todas las copias del dia actual y
solo la ultima de cada uno de los 30 dias calendario anteriores, por hora
local del equipo. Dias sin copias no extienden el plazo; sin copias semanales
extra. Depurar propias anteriores/duplicadas solo tras publicar copia valida.
Actualizar el instalador/motor para que la siguiente ejecucion exitosa aplique
la regla a los respaldos propios existentes. No se ha limpiado produccion.

Prueba ScheduledBackupRestoresLiveWalDatabaseAndPreservesForeignFiles aprobada:
una ejecutada, cero fallos/omitidas; TRX
`%TEMP%/CarWash.Backup.TestResults/backup-retention30.trx`.
Incluye dos copias por dia durante45 dias a00:01/23:59 locales, tres copias de
hoy y respaldos WAL reales; verifica conservar la ultima de dias1-30 y borrar
ambas de dias31-45, conservar todas las de hoy y archivos ajenos.
No sustituir esto por una prueba de borrado sobre carpetas operativas.

Inno compilo ../CarWash/Installer/output/CarWash-Setup-x64-1.1.1.exe,
142302009 bytes, SHA256
`01B338D8AA8B1DBBF1D35A9449DDE7B50FAABA189F3400B1BB9D931E01CE3761`.
Misma prueba contra motor Release autocontenido: una aprobada, cero fallos ni
omitidas; TRX `%TEMP%/CarWash.Backup.TestResults/backup-retention30-packaged.trx`.
Windows PowerShell5.1 Installer/test.ps1 -VerifyPayload aprobado, con tareas,
firewall y operaciones de sistema simulados. Sin instalacion ni limpieza real;
UAC/registro SYSTEM/ACL/reinicio siguen pendientes en T038.

## Instalador Seleccionable 2026-11-09

Actualizacion 1.1.0: el asistente API agrega carpeta de respaldo local dedicada
(predeterminada ProgramData/CarWashBackups). CarWashBackup ejecuta cada hora
SQLite BackupDatabase con integridad y SHA256, configuraciones existentes y
retencion por union24/7/4. Conserva las copias externas al desinstalar; retira
la tarea propia. Destinos eliminables, enlaces y carpetas ajenas se rechazan
antes de configurar. Estado y ultimo resultado de tarea son diagnostico local,
no alertas remotas; no incluye nube ni cifrado adicional. Copiar tambien fuera
del equipo y proteger secretos de configuracion. Procedimiento de restauracion
y limites en ../CarWash/Installer/README.md.

Validacion aislada: ScheduledBackupRestoresLiveWalDatabaseAndPreservesForeignFiles
aprobada (una ejecutada, cero omitidas), cubriendo WAL activo, transaccion no
confirmada, integridad, hashes, retencion, archivo ajeno, lock concurrente,
fuente inexistente, destino inseguro y recuperacion de estado JSON truncado.
TRX `%TEMP%/CarWash.Backup.TestResults/backup-status.trx`. Windows PowerShell5.1
Installer/test.ps1 -VerifyPayload aprobado con tareas/firewall simulados,
incluyendo destinos invalidos, propiedad y preservacion al desinstalar.
Repetida contra el motor Release autocontenido incluido: una aprobada, cero
fallos/omitidas; TRX `%TEMP%/CarWash.Backup.TestResults/backup-packaged.trx`.
Inno compilo ../CarWash/Installer/output/CarWash-Setup-x64-1.1.0.exe,
142299372 bytes, SHA256
`C3EEC8B78E20B6F56F8ADC3DD6D03351E26B3D4DF0B360CD5C31ECF43C35D8AB`.
No se ha instalado la nueva tarea en el equipo operativo ni reiniciado su API.
T038 sigue pendiente para UAC, registro/ejecucion SYSTEM, ACL, reinicio y ciclo
completo en VM limpia; las evidencias siguientes corresponden a versiones previas.

- Proyecto ../CarWash/Installer/CarWash.Installer.csproj integrado en CarWash.slnx.
	Release genera el paquete; solucion Debug compilo aislada sin regenerarlo.
	MSBuild18.8/Framework4.8.1, API/seeder Release win-x64 autocontenidos.
- Entrega: ../CarWash/Installer/output/CarWash-Setup-x64.exe,
	142293162 bytes (135.70 MiB), SHA256
	`09D2C42EEBADEBC217790F70D16BDD34D1242B1040D96B2505205F571FE273B5`.
	Inno Setup compilo la version final, sin ejecutar el instalador.
- Redistribuibles oficiales .NET Framework4.8.1 y VC++ x64: firmas Authenticode
	Valid/Microsoft Corporation comprobadas. Manifiesto de archivos SHA256 aprobado.
	Scripts PowerShell sin errores de sintaxis; modulo Security/Utility del host
	cargado explicitamente para construir desde MSBuild/Windows PowerShell.
- Installer/test.ps1 -VerifyPayload: diez combinaciones de componentes/URL/base,
	rechazo de sobrescritura, configuracion cliente conservada, seleccion temporal
	eliminada, esquema vacio, base compatible/incompatible sin cambios de hash.
	API publicada arrancada en puerto loopback libre con SQLite temporal y
	administrador sintetico: login valido/token recibido. DOTNET_ROOT apunta al
	temporal; esto no equivale a certificar una maquina sin runtime instalado.
- Probe compilado del escritorio distribuido: una ejecutada/una aprobada,
	cero fallos/omitidas. TRX
	`%TEMP%/CarWash.Installer.TestResults/desktop-native-final.trx`.
	Incluye ruta NameLDB expandida en temporal y creacion real con SQLite nativo
	x64, ademas de compatibilidad de login/sesion/formularios existentes.
- Instrucciones de generacion/seleccion/offline/actualizacion en
	../CarWash/Installer/README.md. Base nueva es opt-in y exige administrador
	elegido por el usuario; no contiene empresa/servicios/tarifas ficticios.
- Sin instalacion de prerrequisitos, registro de tareas, firewall, cambios de
	bases reales, despliegue, reinicio de API operativa, mensajes ni impresiones.
	La carpeta de logs se prepara incluso con ruta personalizada de base;
	ese flujo SYSTEM/ACL requiere validacion real en VM junto con reinicio.
- Pendientes externos T038: instalacion limpia, UAC, actualizacion,
	desinstalacion/conservacion, tarea y firewall. Setup no firmado por la empresa;
	SmartScreen puede advertir editor desconocido. Drivers especificos de impresora
	no se incluyen; instalar los del fabricante cuando corresponda.

## Correccion Instalador Conjunto 1.0.1

- Usuario reporto error final al seleccionar escritorio y API. configure.ps1
	declaraba config como XmlDocument para el cliente y reutilizaba esa variable
	al asignar JSON de la API. PowerShell conserva la restriccion de tipo y falla
	antes de completar configuracion de la API. Variables desktopConfig/apiConfig
	separadas; no se modifican datos, contratos HTTP ni credenciales.
- Installer/test.ps1 -VerifyPayload aprobado en PowerShell7 y Windows
	PowerShell5.1: incluye dos recorridos nuevos de configuracion conjunta,
	uno con base existente y otro con base nueva. Desktop/XML y API/JSON conservan
	ajustes, base existente conserva SHA256 y seleccion temporal se elimina.
	Tarea programada y firewall simulados; nunca registrarlos como parte del test.
	Continuan aprobadas las diez selecciones y API temporal/login sintetico.
- Inno Setup compilo la version1.0.1 en archivo separado para no sobrescribir
	el instalador anterior abierto por el usuario:
	../CarWash/Installer/output/CarWash-Setup-x64-1.0.1.exe,
	142310692 bytes, SHA256
	`571233E936E28DCA8804645995EFC8B7031537D5AB970E2241F109E1EBEF15E2`.
	build.ps1 mantiene su nombre habitual CarWash-Setup-x64.exe para futuras
	compilaciones, ahora con version1.0.1. Cerrar asistentes antiguos antes de
	reinstalar y conservar la base existente; no marcar base nueva si ya existe.
- No se ejecuto el instalador nuevo, no se reparo la instalacion real ni se
	inicio su API. UAC/tarea SYSTEM/firewall/reinicio reales siguen pendientes
	de verificacion externa T038. No afirmar instalacion completa certificada.

## Desinstalacion Completa 1.0.2

- Ultima peticion cambia expresamente la politica: actualizar conserva datos,
	pero desinstalar borra completamente los archivos y datos propios de CarWash.
	El asistente advierte borrado permanente de todos los perfiles, con No
	predeterminado. Hacer respaldo FUERA de las carpetas CarWash.
- Quitados uninsneveruninstall de configuraciones; script de limpieza instalado
	tambien para escritorio solo. Evento de desinstalacion comprueba salida del
	script y no declara limpieza exitosa si falla. Inno completa retiro de
	accesos, registro de desinstalacion, su ejecutable y carpeta final vacia.
- Limpieza propia: DB y auxiliares en ruta elegida, logs/configuracion en
	ProgramData/SplashCar para API, AppData/Local y Roaming/CarWash de perfiles
	para escritorio, Api/Desktop/Templates y metadatos. Registrar directorios
	creados permite retirar antecesores vacios; conservar carpeta compartida con
	otros archivos. No eliminar runtimes Microsoft ni historial de Windows.
- Prueba Installer/test.ps1 -VerifyPayload aprobada en Windows PowerShell5.1:
	archivos temporales completos/escritorio solo; cuatro escenarios de limpieza
	(completa, escritorio, tarea ajena, junction); hashes/datos ajenos conservados,
	auxiliares DB eliminados, proceso ya finalizado tolerado, directorios vacios
	retirados, enlaces/tareas ajenos rechazados antes de efectos laterales.
	Todos los comandos de tarea/proceso/firewall y perfiles fueron simulados.
	Diez selecciones, configuracion conjunta y API temporal/login siguen aprobados.
- Inno Setup compilo la entrega
	../CarWash/Installer/output/CarWash-Setup-x64-1.0.2.exe,
	142308566 bytes, SHA256
	`4918BF5D9BA51C94305E412AC8F2DE693403778CAA35D0FD7CEE1F3C97F644F0`.
	No se ejecuto desinstalacion ni borrado real, ni se alteraron datos operativos.
- Instalaciones antiguas necesitan actualizar en la misma ruta a1.0.2 antes
	de desinstalar para usar el nuevo script/politica. Instalacion limpia,
	elevacion real, eventos Inno y limpieza en reinicio siguen pendientes T038;
	no se certifica el ciclo real completo solo por pruebas simuladas.

## Usuario Inicial Y Hosting 1.0.3

- Comparado con installer/CarWashApi.iss, install-service.ps1 y seeder/Program.cs
	existentes: el original tambien utiliza tarea SYSTEM/Kestrel, no IIS. El
	proyecto unificado ya publicaba ese mismo seeder; faltaba invocarlo al elegir
	una base existente sin usuarios. Ahora se pide usuario/clave siempre con API
	y se invoca para ambos tipos de base. No se traslada la clave predeterminada
	del instalador viejo ni se distribuyen credenciales fijas.
- Seeder omite creacion si hay usuarios activos y elimina JSON temporal. No
	restablecer usuarios existentes, duplicarlos ni cambiar el cifrado compatible.
- Windows PowerShell5.1 Installer/test.ps1 -VerifyPayload aprobado: diez
	selecciones, limpieza aislada, tres recorridos conjuntos (poblada, nueva,
	existente vacia), SHA256 de base poblada intacto, JSON de credenciales borrado.
	API temporal arranca contra la base existente vacia provisionada y autentica
	el usuario sintetico. Tareas/procesos/firewall de pruebas simulados.
- Estado observado en esta revision: PID58244 escucha 0.0.0.0:5197, padre
	PowerShell PID42380. Consulta no encontro tarea CarWashApi registrada; ruta
	del ejecutable no visible desde la sesion actual. No afirmar que esa instancia
	este gestionada por tarea/servicio ni confundirla con IIS. No se detuvo,
	reinicio ni reconfiguro el proceso; los instaladores registran la tarea en
	su instalacion real y ese ciclo sigue pendiente de verificacion externa.
- Inno Setup compilo
	../CarWash/Installer/output/CarWash-Setup-x64-1.0.3.exe,
	142305563 bytes, SHA256
	`3EF70EA6638007031A1359D465F4826DC1B821ECB33176B19938E3F4C98E5E9C`.
	Hosting permanece Kestrel/run-api.ps1, tarea CarWashApi en Programador de
	tareas, SYSTEM/al iniciar, puerto5197. No se agrega IIS ni Windows Service.