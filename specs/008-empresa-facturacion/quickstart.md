# Validacion: empresa y recibos

Pruebas con SQLite temporal y clientes simulados, sin escribir empresa ficticia
en base real ni imprimir/enviar notificaciones reales.

API: casos EmpresaTests para permisos, revision, datos/logo y snapshot de recibo.
Android: JVM, assembleDebug, assembleDebugAndroidTest y lintDebug; Compose fisico.
Escritorio: MSBuild aislado y pruebas de formulario/cliente compilado existentes.
Verificar nombre largo/logo en 80 mm, fallo de lectura sin emisor incorrecto y
preservacion de trabajos antiguos. Electronica no implementada en esta fase.

Despliegue: autorizacion de reinicio, respaldo de API/escritorio/base/configuracion
y APK; no reemplazar configuraciones. Usuario administra datos reales en escritorio.

## Uso Despues Del Despliegue

Iniciar sesion administrativa en escritorio y abrir el menu Empresa. Registrar
nombre comercial, razon social, tipo de persona, NIT, domicilio, telefono/correo
y responsabilidades segun RUT (opcional). El digito de verificacion se calcula.
Cargar logo PNG/JPEG de hasta 1 MB y 4096 pixeles, quitarlo o reemplazarlo y guardar.
La API normaliza la imagen a PNG de hasta 512 pixeles. Recargar confirma descarte
de cambios; un conflicto de revision no sobrescribe la ficha de otro administrador.

Android muestra empresa/logo en los encabezados autenticados sin permitir editar;
actualiza al navegar, volver a primer plano o refrescar ingreso. Sin conexion,
retira identidad previa; sin empresa no inventa un emisor. Un 401 requiere acceso.
Correo lleva logo embebido CID y texto alternativo; WhatsApp manual lleva los
datos empresariales en texto, no un adjunto de imagen. Meta no cambia de contrato.
Impresion y vista previa usan nombre, NIT, contacto y logo. Trabajos encolados
conservan snapshot; solo una copia nueva usa la empresa actual. No factura DIAN.

## Evidencia Ejecutada 2026-11-06

- API: 121 pruebas ejecutadas, 121 correctas, cero omitidas/fallos, incluyendo
	cliente WinForms compilado, permisos/revision, normalizacion/retiro y limites
	de imagen, snapshot SQLite tras cambio, totales y MIME SMTP local con logo.
	Salida aislada `%TEMP%/CarWash.Empresa.TestArtifacts`; TRX bajo
	`%TEMP%/CarWash.Empresa.FinalResults`.
- Escritorio: MSBuild Debug con OutputPath/IntermediateOutputPath temporales
	`%TEMP%/CarWash.Empresa.Desktop/{bin,obj}`. Probe de formulario, guardado,
	consulta y conflicto correcto; captura de controles reales inspeccionada:
	`%TEMP%/CarWash.EmpresaFormulario.png`.
- Android: 115 pruebas JVM, cero fallos. Ejecutados
	`./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console=plain`.
	APK y lint correctos. Advertencia de menuAnchor obsoleto preexistente.
- Xiaomi fisico `cmkrkjp7gexgs8ca`, sin emulador: ocho casos en la corrida inicial,
	siete correctos y uno con fallo de cierre ActivityScenario PAUSED -> DESTROYED.
	Ese mismo caso de nombre largo/logo se repitio aislado sin cambiarlo y paso.
	Logo verde comprobado por pixel y geometria; ausencia de entrega/editor y
	recibo WhatsApp interceptado comprobados. No afirmar ocho exitosos en una sola corrida.
- Capturas Android 320 dp y recibo 80 mm con nombre largo/logo inspeccionadas:
	`%TEMP%/CarWash.EmpresaAndroid320dp.png` y `%TEMP%/CarWash.Empresa80mm.png`.
	Prueba acotada del nombre largo en recibo ejecutada despues de la suite: correcta.
- APK operativo respaldado antes de pruebas y restaurado con `adb install -r -t`;
	lanzamiento final `Status: ok`, sin borrar datos. Respaldo bajo
	`%LOCALAPPDATA%/CarWash/ApiBackups/before-auto-entry-qr-20261106/app-before-empresa-tests-*.apk`.
- No cambios de empresa ficticia en la base real, mensajes externos ni papel.
	Durante las pruebas la API y el escritorio operativos no se actualizaron.

## Despliegue Autorizado 2026-11-06

Usuario autorizo expresamente respaldar/reiniciar API y escritorio e instalar APK.
Respaldo nuevo `%LOCALAPPDATA%/CarWash/ApiBackups/before-empresa-20261106-205655`:
ApiBin, DesktopBin, Database/CarWash.db, ApiSourceConfig, printing-config.json
y app-before-empresa.apk. Cola activa cero; escritorio cerrado normalmente,
API anterior detenida, base copiada en reposo. Solo cuatro binarios diferentes
reemplazados; configuracion de origen/API/escritorio/impresora conservada por hash.

- API PID 57532, terminal persistente `540cfdb8-4a6c-411c-b3a2-ab9df62b7591`,
	perfil wifi, `http://192.168.1.5:5197/`; no detener como limpieza de pruebas.
- API DLL SHA256 `9406B69FCFC847A4C2B373E6265840A6E0939CED4A66F38D3A2D36B795D8090C`.
- Escritorio PID 54812, Responding=True, SHA256
	`F8BA76301A98512F98A5F406EEE5EB4F868A386D7A4A851F58275E8584196044`.
- Swagger local/LAN HTTP200 con GET/PUT Empresa; GET anonimo HTTP401.
- SQLite solo lectura: EXCEPT bidireccional de todas las columnas en Turnos,
	TurnosMovimientos, TurnoServicios, ComprobantesRetiro, ImpresionesComprobante,
	WhatsAppNotifications y EmailNotifications: cero diferencias en las siete tablas.
- Tabla Empresa creada con cero fichas: administrador ingresara datos reales.
- APK final instalado `-r -t` en Xiaomi, conservando datos; MainActivity
	`Status: ok`, lanzamiento COLD. Reinicio de API requiere acceso nuevamente.

## Pendientes Externos

Registro de la empresa real por administrador despues del despliegue.
Impresion fisica con logo no ejecutada; impresora termica 80 mm no instalada.
Facturacion electronica queda para una fase posterior.

## Convergencia

Revision final del codigo contra spec.md, plan.md, tasks.md y constitucion:
15 FR, 6 SC y 10 escenarios de aceptacion (31 referencias), ocho decisiones
tecnicas y cinco principios comprobados. Sin brechas funcionales ni tareas
adicionales de codigo. T001-T012 cerradas con evidencia; no nuevas ramas ni commits.
Los pendientes externos anteriores no se presentan como pruebas o datos realizados.