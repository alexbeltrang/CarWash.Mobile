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
Desinstalar elimina tarea propia/regla LAN, no las bases ni configuraciones.
No incluir usuarios de trabajo, bases reales, SMTP ni secretos. WhatsApp e
impresion del paquete deshabilitados; datos fiscales/empresa/servicios se
configuran con valores reales despues. No modificar endpoints ni reglas fiscales.

Generar/probar el paquete no autoriza instalarlo sobre el equipo operativo.
Instalacion limpia/UAC/reinicio/upgrade/desinstalacion y firma del ejecutable
siguen siendo validaciones externas; no confundir API temporal con VM limpia.