# Validación rápida del correo

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
en produccion. Guardar ingreso completo primero: el movil solicita ambos canales
en paralelo, sin esperar SMTP/Meta para confirmar el registro. Si el proceso movil
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
