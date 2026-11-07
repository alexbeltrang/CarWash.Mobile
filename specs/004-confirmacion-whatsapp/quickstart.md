# Validación rápida del correo

## WhatsApp manual y automatico (2026-11-06)

Manual es el modo inicial mientras se resuelve el registro de Meta. No necesita
configurar WhatsApp en el servidor y conserva el correo automatico existente.

1. En Conexion, revisar Prefijo de pais WhatsApp (inicial +57) y pulsar Conectar
	para guardar la configuracion. Se recuerda entre ingresos y reinicios.
2. Buscar/capturar placa y completar el ingreso. Escribir celular local normalmente;
	si ya incluye + y otro prefijo internacional, se conserva. Dejar Manual y guardar
	el turno completo. El telefono almacenado en el turno no se cambia por el prefijo.
3. WhatsApp se abre automaticamente una sola vez, con destinatario y texto. No hay
	que pulsar un boton de apertura. Se intenta WhatsApp normal y luego Business.
	Se utiliza whatsapp://send con phone/text codificados, sin depender de que los
	enlaces web wa.me esten habilitados en los ajustes Android del telefono.
	Revisar destinatario y pulsar Enviar dentro de esa aplicacion. No se pulsa Enviar
	automaticamente ni se declara aceptacion o entrega por abrir una app externa.
4. Si no hay aplicacion compatible o el numero es invalido, el ingreso sigue registrado
	y el mensaje queda disponible para corregir y Reintentar apertura de WhatsApp.
	No se abre un navegador ni se vuelve a registrar el ingreso. Regresar a CarWash
	no abre WhatsApp de nuevo. La copia se conserva solo si no se pudo abrir la app.
5. Al abrir WhatsApp correctamente se limpian inmediatamente la copia y el
	destinatario anterior. Al regresar, el formulario queda listo para otra placa,
	sin vista previa del mensaje anterior. Tambien se limpia tras reintento exitoso.

### Correo fallido o turno existente

- Si falla el correo, se muestra su estado por separado y WhatsApp se abre igual
	tras confirmar el ingreso. No es necesario guardar nuevamente para abrir el mensaje.
- El aviso de revision requerida significa que no se pudo confirmar el registro
	del vehiculo o sus servicios, no que haya fallado el correo. Tras una respuesta
	fallida, la app intenta verificar por lectura si todos los datos y servicios
	quedaron guardados; nunca repite escrituras para esa comprobacion.
- Para un turno que ya existe, revisar sus servicios en el computador antes de
	registrar otra vez. Buscar la placa, revisar los datos cargados, dejar Manual y
	pulsar Actualizar. Una actualizacion confirmada abre WhatsApp sin crear otro
	turno ni reenviar correo. No confirmar al cliente servicios que no quedaron guardados.

### Evidencia de la correccion de correo y WhatsApp (2026-11-06)

- 93 pruebas JVM correctas, sin fallos ni errores. Incluye fallo de correo sin
	bloquear WhatsApp, confirmacion de registro por GET sin repetir escrituras,
	rechazo de servicios/datos incompletos y WhatsApp tras actualizacion confirmada.
- 17 pruebas MainActivityTest correctas, sin omisiones: apertura manual aun si
	correo lanza excepcion y actualizacion existente sin nuevo registro/correo.
- assembleDebug/lintDebug correctos. El primer intento de instrumentacion se
	interrumpio antes de ejecutar casos por inestabilidad del AVD; la ejecucion final
	termino correctamente tras desactivar Bluetooth virtual solo en el emulador.
- ANDROID_SERIAL=emulator-5554 limito las pruebas al AVD; se restauro la variable
	al terminar. No se instalaron pruebas ni se borraron datos del telefono real.
- APK corregido instalado en el celular autorizado con adb install -r, Success;
	inicio de MainActivity con Status: ok. No se enviaron correos/WhatsApp reales ni
	se escribieron registros ficticios en la API operativa. Prueba real de envio manual
	dentro de WhatsApp pendiente del usuario.

Una vez completada el alta de Meta, actualizar/configurar la API segun la seccion
siguiente y elegir Automatico antes de guardar un nuevo ingreso. Ese modo solicita
correo y WhatsApp independientemente, sin abrir la aplicacion del telefono. No
ejecutar ambos modos sobre un mismo ingreso para evitar mensajes duplicados al cliente.
No se modificaron ni desplegaron API, instalador o configuracion de Meta en esta revision.

Validacion de apertura con intents simulados; no envia mensajes reales ni confirma
funcionamiento con una cuenta WhatsApp real. La prueba manual en celular queda pendiente.

### Evidencia ejecutada 2026-11-06

- 89 pruebas JVM aprobadas; sin fallos, errores ni omisiones.
- 15 pruebas instrumentadas MainActivityTest aprobadas en AVD API 35, sin omisiones.
	Incluye mensaje con ambos servicios y total editado, correccion del destinatario,
	no llamar a Meta en manual, ausencia de app, alternativa Business, codificacion
	de caracteres y regresion del modo automatico/reintento seguro.
- assembleDebug/lintDebug correctos: 0 errores y 6 advertencias existentes.
- APK generado en app/build/outputs/apk/debug/app-debug.apk; no instalado en celular.
- Pruebas contra repositorios e intents simulados; no se envio ningun mensaje real.

Comando ejecutado:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.carwash.mobile.MainActivityTest' --console=plain
```

## WhatsApp adicional: alta Meta (2026-11-05)

Estado: implementado con dobles de Meta; numero, plantilla y prueba real pendientes.
El correo se conserva. No se ha activado WhatsApp ni enviado mensajes reales.

### 1. Preparar la cuenta y el numero

1. Entrar con la cuenta del propietario en Meta for Developers y crear/configurar
	una app para WhatsApp Business Platform vinculada al portfolio del negocio.
2. Empezar con el numero de prueba de Meta y destinatarios de prueba autorizados.
	No migrar ni borrar la cuenta WhatsApp actual del negocio para probar; para
	produccion revisar los requisitos de numero dedicado/coexistencia en el panel.
3. Obtener Phone Number ID (no es el telefono ni el WhatsApp Business Account ID)
	y un token autorizado con whatsapp_business_messaging. Para produccion seguir
	el proceso de token de sistema y permisos del negocio; el token temporal del
	panel no sirve como configuracion permanente. Revisar verificacion, limites y
	facturacion que Meta indique para esa cuenta; no se presupone envio gratuito.
4. Recabar autorizacion del cliente para contactarlo por WhatsApp mediante el
	proceso del negocio. Esta revision no inventa ni persiste un consentimiento.

Documentacion oficial: https://developers.facebook.com/docs/whatsapp/cloud-api/

### 2. Aprobar la plantilla

Crear una plantilla de recepcion transaccional, por ejemplo recepcion_vehiculo,
con idioma exacto es (si Meta aprueba es_CO, configurar es_CO). Elegir la categoria
que corresponda en Meta; su aprobacion no esta garantizada por esta integracion.
El cuerpo debe tener seis parametros posicionales, en este orden:

1. Nombre del cliente.
2. Placa.
3. Marca.
4. Servicios autorizados con valores del catalogo, solo relaciones no eliminadas.
5. Valor total confirmado del turno, no suma recalculada de tarifas actuales.
6. Nombre comercial del autolavado configurado en servidor.

Ejemplo de cuerpo a registrar en Meta, con muestras sinteticas en su editor:

```text
Hola {{1}}. Recibimos tu vehiculo de placa {{2}}, marca {{3}}. Servicios autorizados: {{4}}. Total a pagar: ${{5}}. {{6}}.
```

No agregar variables, cabecera o botones que requieran parametros adicionales
sin adaptar el contrato. No se requiere color del vehiculo. El celular del turno
debe estar en formato internacional E.164, por ejemplo +573000000000 (sintetico).
No se adivina prefijo de pais para numeros locales. Datos incompletos no impiden
el ingreso, pero WhatsApp puede devolver NOT_APPLICABLE o rechazo de Meta.

### 3. Configurar solo el servidor

Usar gestor de secretos/variables de entorno del proceso API o configuracion
segura de IIS/servicio; nunca appsettings con valores reales dentro de Git, APK,
chat, capturas, argumentos de comandos o logs. El ejemplo API deja todo deshabilitado.

| Clave ASP.NET Core | Variable de entorno | Valor |
|---|---|---|
| WhatsApp:Enabled | WhatsApp__Enabled | true solo despues de preparar la cuenta |
| WhatsApp:GraphVersion | WhatsApp__GraphVersion | Version admitida por Meta; default v23.0 |
| WhatsApp:PhoneNumberId | WhatsApp__PhoneNumberId | ID del numero emisor |
| WhatsApp:AccessToken | WhatsApp__AccessToken | Secreto de Meta con permiso de envio |
| WhatsApp:TemplateName | WhatsApp__TemplateName | Nombre exacto aprobado |
| WhatsApp:TemplateLanguage | WhatsApp__TemplateLanguage | Codigo exacto aprobado |
| WhatsApp:CarWashName | WhatsApp__CarWashName | Nombre comercial real |
| WhatsApp:MaxAttempts | WhatsApp__MaxAttempts | 3 por defecto; incluye primer intento |
| WhatsApp:WebhookVerifyToken | WhatsApp__WebhookVerifyToken | Secreto elegido para challenge |
| WhatsApp:AppSecret | WhatsApp__AppSecret | Secreto de la app Meta, no access token |

Aplicar cambios al proceso API en una ventana autorizada; no se reinicia aqui el
servidor ni se modifica la base real. Sin configuracion completa se devuelve
NOT_CONFIGURED sin salida a Meta ni fila de envio irrecuperable. Cambiar solo el
APK no actualiza la API instalada. Las reglas de correo y captura no cambian.

### 4. Webhook y entrega

Configurar callback HTTPS con certificado confiable a /api/webhooks/whatsapp,
challenge con WebhookVerifyToken y suscripcion al campo messages de la cuenta.
192.168.x.x no es accesible para Meta. Si se publica callback, restringir proxy
a esa ruta; no publicar login, turnos ni resto de API en Internet. El control de
la firma usa AppSecret y el cuerpo original; el proxy no debe reescribirlo.
Sin callback funciona aceptacion del envio, no confirmacion de entrega.

### 5. Validar sin duplicados

Solicitar autorizacion explicita antes de un envio real, usando un destinatario
de prueba y un ingreso controlado en una base aislada. No registrar turnos falsos
en produccion. Guardar ingreso completo primero: en modo Automatico el movil solicita
ambos canales en paralelo, sin esperar SMTP/Meta para confirmar el registro. Si el proceso movil
termina antes de solicitar notificacion no hay cola duradera ni entrega garantizada.

ACCEPTED no significa entregado. DELIVERED solo tras callback firmado; el movil
muestra resultado de solicitud y el endpoint GET permite consultar estado posterior.
UNKNOWN o SENDING tras interrupcion requiere conciliar con Meta antes de reenviar.
No borrar filas para forzar reintento; mantener MessageIdExterno y unicidad por turno.
Los reintentos son manuales y limitados para rechazos seguros; no hay worker automatico.

Pruebas reproducibles desde Mobile, sin credenciales ni mensajes reales:

```powershell
dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path "$env:TEMP/CarWash.ReporteVentas.TestArtifacts" --filter 'FullyQualifiedName~WhatsApp' --verbosity minimal
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

Las secciones siguientes conservan la guia del canal SMTP existente.

### Evidencia ejecutada 2026-11-05

- 19 pruebas enfocadas de WhatsApp correctas: plantilla, configuracion deshabilitada,
	idempotencia, envio pendiente, limite de intentos, timeout/cancelacion inciertos,
	servicios eliminados, payload HTTP con seis parametros y firma de webhook.
- Suite completa API: 79/79 correctas con --artifacts-path temporal, sin detener
	servidor real. SQLite temporal y dobles de HTTP; no se llamo a Meta ni SMTP.
- Android: 84 JVM y 45 instrumentadas correctas, 0 fallos. Una prueba opcional de
	integracion del reporte omitida porque no se suministro URL de fixture. Conteos
	contrastados con XML, AVD API 35. Incluye ingreso, OCR/zoom, sesion y reporte.
- assembleDebug/lintDebug correctos: 0 errores, 6 advertencias. APK generado en
	app/build/outputs/apk/debug/app-debug.apk, sin firma de produccion.
- No se regenero instalador API en esta revision: no usar el paquete anterior para
	afirmar que contiene WhatsApp actualizado. Actualizar API y APK coordinadamente.
- Sin instalacion en celular, cambios de firewall, reinicio de API, modificacion
	de base operativa ni envios reales. El propietario debe completar alta Meta y
	autorizar una prueba real antes de habilitar produccion.

### Evidencia de apertura nativa 2026-11-06

- Sustituido wa.me por whatsapp://send con phone/text codificados. En el Xiaomi,
	cmd package resolve-activity confirma com.whatsapp/.TextAndDirectChatDeepLink
	sin abrir conversaciones ni enviar mensajes.
- Cinco pruebas enfocadas de MainActivityTest correctas en celular fisico mediante
	adb am instrument: contenido/numero, caracteres especiales y alternativa Business,
	app ausente, correo fallido y actualizacion de turno existente. Usan repositorios
	en memoria y capturan Intent; no prueban entrega ni vista previa en WhatsApp real.
- JVM, assembleDebug, assembleDebugAndroidTest y lintDebug correctos. Actualizacion
	de APK y APK de pruebas con adb install -r -t, sin desinstalar ni borrar datos.
	MainActivity iniciada con Status: ok al finalizar. Sin escrituras API ni envios.
- Por indicacion del usuario, no se ejecutaron pruebas en emulador; se cerro el AVD.
	Falta confirmar visualmente el mensaje preparado dentro de WhatsApp real antes
	de afirmar solucion completa del comportamiento externo. El envio sigue siendo manual.

### Diagnostico de registro incierto 2026-11-06

La version instalada agrega al aviso una linea Etapa sin confirmar y un codigo
HTTP/tipo de excepcion; no muestra cuerpos HTTP ni mensajes del servidor. Distingue
consecutivo, vehiculo, servicios y confirmacion local. No desactiva la proteccion
contra duplicados ni autoriza WhatsApp para un guardado sin confirmar.

- 94 pruebas JVM y seis MainActivityTest correctas en Xiaomi fisico, sin omisiones.
	Incluye diagnostico visible, ausencia de cuerpo HTTP y ausencia de WhatsApp para
	registro incierto; las otras cinco cubren el flujo manual anterior.
- assembleDebug, assembleDebugAndroidTest y lintDebug correctos. APK y paquete de
	pruebas instalados con install -r -t; MainActivity Status: ok. Sin emulador,
	escrituras API, modificaciones de turnos pagados ni mensajes reales.
- La causa del fallo operativo sigue pendiente: las pruebas usan repositorios
	simulados y no demuestran que el registro real ya funcione. Para continuar,
	capturar solo la linea de etapa/codigo si falla un ingreso legitimo autorizado,
	antes de cerrar la app; no crear turnos ficticios ni repetir uno ya guardado.

### Causa real reparada y WhatsApp verificado 2026-11-06

- Aviso fisico identifico servicios/HTTP500. La base activa tenia idTunoServicios
	sin PRIMARY KEY: la relacion se insertaba, pero EF Core fallaba al leer su ID NULL.
	No era un fallo de correo ni ausencia de WhatsApp.
- Regresion nueva en CarWashApi.Tests reproduce ese 500 y verifica reparacion sin
	repetir la relacion, conservacion de IDs y POST201 posterior. Tres casos xUnit
	correctos. Script Test-TurnoServiciosIdentity.ps1 tambien correcto en base temporal;
	su error CHECK esperado corresponde al rechazo de reejecucion sobre esquema sano.
- Con autorizacion explicita, se detuvo la API, se verifico respaldo SQLite
	CarWash.before-service-identity-20261106-144105.db y se aplico el SQL existente.
	Se conservaron 404 relaciones y todos los IDs validos; ocho NULL recibieron ID.
	Comparacion bidireccional: asociaciones sin perdida, turnos y pagos sin cambios.
- API original reiniciada con perfil wifi y --no-build. GET TurnoServicios y consulta
	en proceso responden HTTP200. Turno004 mantiene una relacion, sin repetir POST.
- Prueba directa de enlace nativo en Xiaomi, sin otro ingreso: borrador de 159
	caracteres igual al texto preparado, con placa, servicios y total correctos.
	Prefijo +57 aprobado por el usuario solo para el destinatario manual. No se
	modifico su celular almacenado ni se pulso Enviar. El usuario habia enviado
	manualmente el mensaje anterior; esta repeticion dejo un borrador sin enviar.
- No se reprodujo un guardado nuevo desde CarWash. Un aviso pendiente ya visible
	no se limpia automaticamente al reparar SQLite: revisar primero el turno en
	computador; no registrarlo otra vez. Para retomarlo, usar Cancelar/Limpiar,
	buscar su misma placa y revisar el turno en proceso antes de Actualizar.
	El modo Manual ofrece el mensaje tras esa actualizacion confirmada; corregir
	ahi el destinatario internacional sin cambiar el turno guardado.

### Prefijo configurable y apertura automatica 2026-11-06

- Prefijo +57 inicial configurable en Conexion, persistido en preferencias. Numeros
	locales usan ese valor; los internacionales conservan su propio prefijo. No cambia
	el celular del turno ni configura Meta/servidor.
- Apertura unica en Manual despues de guardar/actualizar correctamente, consumida
	antes del Intent. No reapertura al regresar/remontar pantalla, ni en Automatico,
	registro incierto o actualizacion fallida. Boton solo para correcciones o fallos.
- Regresion JVM completa correcta. Las 21 MainActivityTest pasaron en Xiaomi fisico,
	incluida configuracion +52, numero guardado intacto, apertura sin boton, remontaje,
	correccion de telefono invalido, correo fallido, app ausente y modo Automatico.
- assembleDebug, assembleDebugAndroidTest y lintDebug correctos. APK y paquete de
	pruebas instalados con install -r -t sin borrar datos. Sin emulador, turnos ficticios,
	escrituras API ni mensajes reales. Aperturas interceptadas en estas 21 pruebas;
	no se reprodujo un ingreso nuevo contra la API real para esta revision.

### Limpieza tras apertura de WhatsApp 2026-11-06

- Limpiar la confirmacion manual al abrir correctamente la app, sin esperar el
	regreso ni otro guardado. Conservarla si falla apertura/numero; limpiar tambien
	tras un reintento exitoso. No modifica turnos, configuracion o notificaciones API.
- APK y paquete de pruebas actualizados con install -r -t sin borrar datos.
	assembleDebug, assembleDebugAndroidTest, regresion JVM y lintDebug correctos.
- MainActivityTest: 23 casos ejecutados en Xiaomi; 22 correctos y uno fallo al
	cerrar ActivityScenario, despues de sus comprobaciones, esperando DESTROYED
	desde STOPPED. Ese caso (limpieza y siguiente placa) se repitio aislado y paso.
	Incluye reintento exitoso que limpia y fallo de apertura que conserva la copia.
- Sin emulador, escrituras API ni mensajes enviados por el agente. El usuario
	confirmo que el flujo de envio ya funciona y solicito esta limpieza posterior.

### Formato visual del mensaje manual 2026-11-06

- Encabezado de recepcion con icono y negrita de WhatsApp, saludo, placa y marca
	en lineas separadas, servicios con icono individual, total en negrita y cierre
	de agradecimiento. Lineas en blanco separan secciones; sin nuevos datos de negocio.
- Se conserva el total confirmado y los precios de los servicios del mensaje
	anterior. No modifica la plantilla aprobada de Meta ni activa envios automaticos.
- Dos pruebas enfocadas correctas en Xiaomi: texto real del Intent con formato,
	ambos servicios y total editado; emojis/acentos/caracteres especiales codificados
	y alternativa Business sin navegador. Usan aperturas interceptadas, no envios.
- assembleDebug, assembleDebugAndroidTest, JVM y lintDebug correctos. APK actualizado
	con install -r -t sin borrar datos. Sin registros ficticios ni mensajes reales.
	La apariencia final dentro de una burbuja enviada no se verifico en esta revision.

### Correo HTML de recepcion 2026-11-06

- HTML con encabezado e iconos, saludo, placa/marca, tabla de servicios y bloque
	verde para el total confirmado. Mantiene aviso de aprobacion de adicionales.
	Estilos inline y ancho maximo adaptable de 600 px, sin imagenes remotas/rastreo.
- Los datos variables se escapan para HTML; importes es-CO y total del turno,
	no recalculado desde catalogo. SMTP envia multipart/alternative con texto
	plano y HTML UTF-8. La firma de texto anterior sigue disponible.
- Ejecutado desde CarWash.Mobile:

	```powershell
	dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path "$env:TEMP/CarWash.ReporteVentas.TestArtifacts" --filter 'FullyQualifiedName~EmailNotificationServiceTests' --verbosity minimal -p:WarningLevel=0
	```

- Resultado: cuatro casos xUnit correctos, cero fallos/omisiones; compilacion
	API/pruebas correcta. Verificados datos escapados, dos servicios, total
	confirmado distinto del catalogo, idempotencia, compatibilidad de clientes
	de texto y MIME real en SMTP loopback con/sin HTML, incluidos acentos/iconos.
- Sin credenciales, correos reales ni cambios en base operativa. API existente
	seguia usando el binario anterior en esta primera validacion; no se reinicio
	ni reemplazo hasta la autorizacion y despliegue descritos a continuacion.
	Apariencia final en un buzon real pendiente; no requiere actualizar el APK.

### Despliegue autorizado del correo HTML 2026-11-06

- Usuario autorizo desplegar y reiniciar. Se repitieron las cuatro pruebas de
	correo: todas correctas. dotnet publish Debug ejecutado con artifacts-path
	aislado y salida temporal antes de detener la instancia identificada.
- Respaldo del directorio operativo en
	`%LOCALAPPDATA%/CarWash/ApiBackups/before-email-html-20261106-160237`, fuera de
	Git. Se preservaron archivos appsettings del destino, verificados por hash.
	No se modifico la configuracion fuente, credenciales ni datos de negocio.
- Binario desplegado en el directorio operativo bin/Debug/net9.0 de CarWashApi.
	SHA-256 de Carwash.API.dll igual al publicado:
	`E3D38D4B03EAA46DCC5C75AFC228A74E1E229F39B49BB91ECB2270C096DC3CCF`.
- Reinicio con `dotnet run --no-build --project ../CarWashApi/Carwash.API.csproj --launch-profile wifi`.
	Proceso PID 23956, inicio 16:03:55; content root CarWashApi y escucha 0.0.0.0:5197.
	Swagger HTTP 200 tanto en 127.0.0.1:5197 como en 192.168.1.5:5197.
- Reiniciar con --no-build no compila cambios pendientes: por eso se requiere
	publicar/desplegar primero. El contenido nuevo aplica a proximos correos;
	no modifica mensajes recibidos ni reenvia notificaciones ya aceptadas.
- Sin correos reales ni registros ficticios del agente. No se ejecutaron
	endpoints de escritura ni de envio durante el despliegue. Apariencia del
	proximo correo legitimo en buzon real pendiente de confirmacion del usuario.

## Prerrequisitos

1. API CarWash compilable y SQLite de pruebas.
2. Valores ficticios para plantilla y nombre del autolavado.
3. Credenciales SMTP ausentes en pruebas automatizadas.

## Pruebas automatizadas

En `CarWashApi`, ejecutar el proyecto de pruebas existente con Graph API
reemplazado por un mock:

```powershell
dotnet test
```

Comprobar creación única, payload con placa/servicios/total, conservación del
ingreso ante timeout, reintentos acotados, errores permanentes, idempotencia y
actualización por webhook. No registrar teléfonos completos, cuerpos ni tokens.

En la app móvil:

```powershell
.\gradlew.bat :app:test
```

Comprobar que la UI exige correo, conserva el éxito del ingreso y muestra por
separado `NOT_APPLICABLE`, `PENDING`, `ACCEPTED`, `DELIVERED` y `FAILED`.

## Prueba viva autorizada

Solo después de configurar un buzón de prueba y credenciales SMTP seguras.
Verificar el correo con un ingreso controlado y no usar datos de clientes sin
autorización.
