# Contract: Factura en papel

Todos los endpoints Bearer ApiSession existente, Cache-Control:no-store.
GET /api/Facturas/configuracion: 200 DTO o 204 sin configuracion.
PUT /api/Facturas/configuracion: administrador, DTO completo/revision; 200.
Comprador exige nombre/tipo/numero documento; direccion/contacto opcionales.

Provisionamiento: scripts SQL separados para SQLite y SQL Server, solo esquema,
sin INSERT de usuarios/empresa/autorizacion/facturas. Incluyen restricciones e
indices del esquema existente. No alteran la base operativa ni habilitan SQL
Server en la API, cuyo inicio/consultas siguen siendo especificos de SQLite.

Revision visual 2026-11-09: menus Empresa, Configuracion facturacion, Facturas,
Comprobante y retiro y Reporte Diario bajo Administracion. Mismos permisos por
opcion que antes; administracion de empresa/fiscal y reporte solo administrador,
factura/retiro para personal autenticado. Formularios nuevos reutilizan estilo
de Apertura/Cierre caja sin modificar contratos HTTP, pagos ni impresion.

Revision disenador 2026-11-09: las cinco pantallas y entradas nuevas de menu se
declaran en InitializeComponent/Designer.cs, con recursos asociados y constructor
sin parametros disponible. Diseno no inicializa datos ni conecta a API. Quitar
iconos de opciones, conservar agrupacion Administracion y todos los permisos.
GET /api/Facturas/preparacion?idTurno=N o placa=ABC123: 200 turno finalizado,
datos sugeridos comprador, total/concepto y revisiones. Requiere un selector.
GET /api/Facturas: hasta100 documentos mas recientes; placa opcional filtra antes
del limite para recuperar facturas anteriores incluso con autorizacion vencida.
Sin cuerpos en logs.
GET /api/Facturas/turnos/{id}: 200 snapshot o 204 si no emitida.
POST /api/Facturas/turnos/{id}: solicitudId UUID, comprador, empresaRevision,
configuracionRevision; 201 documento, repetir UUID/turno devuelve documento original.
Otro UUID sobre turno ya facturado:409, consultar original. No alterar factura.
POST /api/Facturas/{id}/impresiones: solicitudId UUID/anchoMm80; 202 job existente
de impresora Windows, reutilizar snapshot de factura. Recuperacion via GET
/api/RetiroVehiculos/impresiones/{uuid}; ninguna credencial/printer cliente.
GET /api/Facturas/{id}/impresiones/ultima: 200 ultimo job exclusivo de factura
o 204; no mezcla impresiones de ingreso. Identidad de factura incluida en snapshot.

400 validacion;401 sin sesion;403 configuracion no administrador;404 turno/factura
ausente;409 pendiente de pago, revision, autorizacion/vigencia/rango o duplicado;
503 dependencia no disponible. Errores seguros, sin datos de compradores.

Escritorio: tras cobro completo confirmado pregunta "Desea generar la factura?",
Si/No y Button2 (No) predeterminado. No cierra registro sin formulario/factura/
impresion. Si abre formulario comprador; nombre/tipo/numero requeridos, direccion
opcional. No hay segunda confirmacion de emision para el turno aceptado. Impresion
explicita desde factura, sin check ni impresion automatica. Menu Facturas para
personal autenticado; Configuracion facturacion
solo administrador. Al finalizar cobro abrir preparacion con IdTurno confirmado.
Cancelar/errores de facturacion no repiten pago. Emision manual explicita tras comprador,
historial y vista previa; imprimir/otra copia con revision de estado y confirmacion.
Android no factura. Contrato de recibos automaticos de ingreso permanece intacto.

## Correccion Escritorio 2026-11-09

Facturas y Reporte diario conservan solo las barras nativas de DataGridView;
no se declaran barras independientes copiadas de sus controles internos.
Proyecto y solucion compilan Debug/Release con plataforma x64 por defecto y
copian SQLite nativo junto al ejecutable, sin modificar paquetes NuGet.
Sin cambios de endpoints, DTO, datos ni reglas de facturacion. Usuario autoriza
reemplazo del escritorio con respaldo y configuracion conservada; API sin reinicio.

## Carga Del Disenador .NET Framework

UseWinFormsOutOfProcDesigner=True habilita el disenador fuera de proceso para
el proyecto .NET Framework4.8.1 x64. No sustituir referencias del framework,
cambiar arquitectura de SQLite ni editar formularios para corregir el cargador.
Visual Studio debe recargar el proyecto y reabrir las pestanas de diseno.

## Distribucion Seleccionable Windows

Proyecto CarWash.Installer integrado en la solucion de escritorio. Release
genera Installer/output/CarWash-Setup-x64.exe; Debug no empaqueta. Destino
Windows10 22H2/11 x64, sin SDK, Visual Studio, IIS ni SQL Server. Incluye
redistribuibles Microsoft firmados (.NET Framework4.8.1/VC++ x64), dependencias
WinForms/SQLite x64 y API .NET9 autocontenida. Sin descargas durante instalacion.

Componentes independientes escritorio/API; crear base nueva depende de API y
esta desmarcado por defecto. Escritorio sin API pide URL base HTTP/HTTPS sin
credenciales ni parametros. API local usa puerto5197 y tarea SYSTEM al iniciar
Windows, con firewall solo Privado/subred local. No reemplazar otra tarea
CarWashApi de otra ubicacion ni una aplicacion ocupando el puerto.

Base nueva copia esquema versionado sin registros y crea solo el administrador
introducido en el asistente; usuario/clave4-64 ASCII imprimible, sin espacios
extremos, conservando el cifrado compatible existente. Nunca sobrescribir un
archivo existente. Base existente: apertura SQLite ReadOnly, quick_check y
tablas principales requeridas; no es una migracion ni verificacion exhaustiva
de versiones del esquema. Credenciales solo en JSON temporal protegido por
ACL SYSTEM/Administradores y eliminado al configurar; nunca en argumentos/logs.

Conservar appSettings y configuracion API existentes, salvo URL/ruta elegidas
explicitamente; actualizar binding redirects desde la distribucion compilada.
Cliente nuevo usa NameLDB=%LOCALAPPDATA%/CarWash/CarWash.db expandido al ejecutar;
configuraciones anteriores sin variables conservan su resolucion previa.
Preparar directorio protegido de logs aun si la base esta en otra ruta.
Desde1.0.2 desinstalar elimina tambien bases y configuraciones conforme FR019;
la conservacion anterior se aplica solo al actualizar, no al desinstalar.
No incluir usuarios de trabajo, bases reales, SMTP ni secretos. WhatsApp e
impresion del paquete deshabilitados; datos fiscales/empresa/servicios se
configuran con valores reales despues. No modificar endpoints ni reglas fiscales.

Generar/probar el paquete no autoriza instalarlo sobre el equipo operativo.
Instalacion limpia/UAC/reinicio/upgrade/desinstalacion y firma del ejecutable
siguen siendo validaciones externas; no confundir API temporal con VM limpia.

Revision 1.0.1: configuracion de escritorio XML y configuracion API JSON usan
variables independientes. No convertir JSON a XmlDocument al seleccionar ambos
componentes. Regresion conjunta con base existente/nueva, ajustes conservados
y tareas/firewall simulados; ejecutar tambien en Windows PowerShell5.1 usado
por el asistente. Sin cambio de endpoints ni datos operativos.

Revision 1.0.2 (sustituye conservacion al desinstalar): InitializeUninstall
exige confirmacion explicita, No predeterminado, avisando borrado permanente de
base API, configuraciones, logs y datos de todos los usuarios. Respaldo fuera
de carpetas CarWash; tambien se eliminan respaldos guardados dentro de ellas.
CurUninstallStepChanged ejecuta limpieza antes de retirar archivos y aborta con
error visible si falla. No desinstalar runtimes Microsoft compartidos.

installation.json sin secretos registra ruta instalada, componentes, base
validada y directorios creados, incluso para instalacion parcial. En versiones
anteriores sin registro, recuperar base del appsettings instalado con parser de
cadena de conexion, no regex. Eliminar DB/.db-wal/.db-shm/.db-journal; nunca
borrar recursivamente el directorio personalizado que contiene la base.
Eliminar arboles propios Api/Desktop/Templates, ProgramData/SplashCar para API
y AppData/Local/CarWash y AppData/Roaming/CarWash de perfiles para escritorio.
Directorios personalizados creados se retiran solo si vacios. Inno elimina
accesos, registro de desinstalacion y carpeta instalada al terminar.

Antes de borrar, rechazar enlaces/uniones y otras tareas/API activas; detener
solo procesos por ruta exacta y lanzador propio, tolerar proceso ya terminado,
retirar tarea/regla LAN propias. Escritorio solo no toca API externa. Probar
archivos reales exclusivamente en temporales con perfiles y comandos de Windows
simulados. No implica borrar trazas de auditoria/historial que mantiene Windows.

Revision 1.0.3: reutilizar CarWashSeeder del instalador API original tambien
para base existente compatible sin usuarios activos, no solo para base nueva.
Mostrar usuario/clave al seleccionar API; ambos elegidos por el usuario, sin
trasladar la clave fija del instalador anterior. Invocar sembrador despues de
configurar conexion y, si corresponde, copiar esquema. Usuarios activos
existentes implican no crear/restablecer ninguno. Credenciales temporales se
eliminan tanto al crear como al omitir usuario. No cambiar cifrado/login/API.

Mantener el hosting original Kestrel/run-api.ps1 mediante tarea CarWashApi
SYSTEM al iniciar, puerto5197, no IIS ni servicio Windows. Sitio/application
pool IIS no se crean. El instalador registra la tarea; un proceso API iniciado
manualmente no demuestra por si solo que esa tarea exista.

Revision 1.1.0 (FR021): seleccionar BackupDirectory local dedicado al instalar
API; predeterminado ProgramData/CarWashBackups. Rechazar raiz, UNC, enlaces,
destinos dentro de carpetas eliminables, antecesores de instalacion/SplashCar
y carpetas que contengan la base. Carpeta no vacia requiere backup-owner.json
de la misma instalacion; validar antes de detener/configurar componentes.
ACL para SYSTEM/Administradores; no incluir destino en directorios eliminables.

backup-settings.json instalado contiene InstallDir, DatabasePath,
BackupDirectory y ConfigFiles, sin copiar credenciales a argumentos. Tarea
CarWashBackup SYSTEM ejecuta Api/CarWashSeeder.exe --backup con ese archivo,
cada hora, StartWhenAvailable, IgnoreNew y limite de una hora. Bloqueo de
archivo adicional impide solapamiento. Verificar propiedad exacta antes de
reemplazar/detener/retirar la tarea; desinstalar conserva copias externas.

Motor SQLite BackupDatabase ReadOnly sobre fuente existente, destino temporal,
integrity_check y SHA256 antes de publicar por renombrado. Carpeta publicada
CarWash-<fecha UTC>-<identificador> contiene CarWash.db, configuraciones API,
correo e impresion existentes y backup.json con InstallDir, CreatedUtc y Files
(nombre a SHA256). No incluir binarios. Retencion por union de 24 mas recientes,
7 dias distintos y 4 semanas ISO distintas, solo marcadores de esta instalacion.
No borrar carpetas ajenas. Fallos devuelven 1 y exito 0; backup-status.json
incluye Success, AttemptUtc y LastSuccessUtc. Estado JSON truncado no impide
respaldos futuros. Sin alertas remotas, nube ni cifrado adicional automaticos.
Prueba/restauracion aislada con WAL activo; nunca sustituir base operativa
para probar. Registro efectivo SYSTEM/ACL y reinicio requieren VM Windows.