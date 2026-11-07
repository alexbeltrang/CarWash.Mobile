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