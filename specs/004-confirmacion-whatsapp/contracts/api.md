# Contrato API: notificación por correo

La API CarWash sigue siendo autoridad del ingreso. Estos endpoints no crean ni
modifican turnos; operan sobre un turno confirmado.

El asunto del correo incluye la placa y la marca del vehículo y no incluye el
identificador interno ni el número visible del turno.

## Presentacion del correo (revision 2026-11-06)

- Sin cambios en rutas, DTO, destinatario, configuracion SMTP, idempotencia ni
  reintentos. La notificacion incluye alternativa text/plain y text/html en
  UTF-8 mediante MIME multipart/alternative; clientes sin HTML leen texto plano.
- HTML con encabezado de recepcion, iconos Unicode, cliente, placa, marca,
  tabla de servicios/precios y total a pagar destacado. Agradecimiento y aviso
  de que adicionales requieren aprobacion. Estilos inline, ancho adaptable,
  sin imagenes remotas, rastreo ni enlaces externos.
- Codificar cliente, placa, marca y nombres de servicios antes de interpolar
  HTML. Formatear dinero en es-CO; conservar turn.Valor confirmado aunque difiera
  de la suma de precios de catalogo. Mismo contenido al reintentar.
- IEmailClient conserva la firma de texto existente; la sobrecarga EmailBody
  permite enviar ambas alternativas, con degradacion a texto para clientes
  existentes. SmtpEmailClient implementa ambas y no interpreta texto previo
  como HTML. No cambia la plantilla de WhatsApp ni requiere actualizar el APK.
- Validar con SQLite temporal, destinatarios example.invalid y SMTP loopback;
  nunca enviar a clientes reales para probar. Despliegue de API/reinicio y prueba
  viva del correo requieren autorizacion separada.

Despliegue autorizado por el usuario y ejecutado el 2026-11-06: API actualizada
y reiniciada con perfil wifi, configuracion conservada, copia previa del binario
y verificacion SHA-256 contra la publicacion. HTTP 200 en Swagger local y en
192.168.1.5:5197. No se repiten notificaciones aceptadas ni se envian correos de
prueba reales. La plantilla nueva aplica a proximos envios; no altera correos
ya recibidos. Presentacion final en buzon real aun no verificada.

## Crear o consultar notificación

`POST /api/Turnos/{idTurno}/notificacion-email`

La operación es idempotente por `idTurno`. Si ya existe una notificación
aceptada o entregada, devuelve el estado existente sin llamar nuevamente a Meta.

Respuesta `200`:

```json
{
  "idTurno": 123,
  "estado": "ACCEPTED",
  "accion": "NONE"
}
```

Un teléfono/dato no utilizable devuelve `NOT_APPLICABLE` sin invalidar el turno.
Un turno no confirmado devuelve `409`; uno inexistente devuelve `404`.

## Consultar estado

`GET /api/Turnos/{idTurno}/notificacion-email`

Devuelve el último estado persistido sin volver a llamar a Meta.

## Reintento manual

`POST /api/Turnos/{idTurno}/notificacion-email/reintentar`

Solo permite reintentar `FAILED` sin `MessageIdExterno` aceptado y nunca crea
una segunda notificación.

## Configuración

`Email:Host`, `Email:Port`, `Email:Username`, `Email:Password`,
`Email:FromAddress`, `Email:FromName`, `Email:EnableSsl` y `Email:MaxAttempts`
son configuración del servidor/secret store, nunca valores compilados en el APK.

## Canal adicional WhatsApp (revision 2026-11-05)

Conservar los endpoints email. Reutilizar POST/GET
`/api/Turnos/{idTurno}/notificacion-whatsapp` y POST de su subruta `reintentar`.
DTO: `{ "idTurno": 123, "estado": "ACCEPTED", "accion": "NONE" }`.
En modo automatico el movil solicita ambos canales solo despues de confirmar
turno y servicios, sin bloquear ni revertir el registro; cada canal mantiene su
propio estado. En modo manual no solicita notificacion WhatsApp a la API.

Sin Meta configurado: NOT_CONFIGURED, accion NONE, sin salida de red ni consumo
de intentos. Un envio aceptado no implica entrega: solo webhook verificado puede
confirmar DELIVERED. No reintentar resultados inciertos automaticamente.

Configuracion exclusiva del servidor: WhatsApp:Enabled, GraphVersion,
PhoneNumberId, AccessToken, TemplateName, TemplateLanguage, CarWashName y MaxAttempts.
La plantilla aprobada recibe seis parametros: cliente, placa, marca, servicios
con sus valores, total confirmado y nombre del autolavado. No requiere color.

Estados WhatsApp: PENDING/SENDING, ACCEPTED, DELIVERED, RETRYING, FAILED,
NOT_APPLICABLE, NOT_CONFIGURED y UNKNOWN. RETRY_MANUAL solo si hay rechazo seguro,
sin MessageIdExterno, ni limite agotado. UNKNOWN/SENDING/aceptados no se reenvian.
No hay reintentos automaticos ni promesa de entrega si el movil se cierra antes
de solicitar la notificacion. Cada canal es independiente; un error no revierte ingreso.

GET /api/webhooks/whatsapp verifica hub.mode/verify_token/challenge contra
WhatsApp:WebhookVerifyToken. POST requiere X-Hub-Signature-256 (HMAC-SHA256
del cuerpo sin modificar con WhatsApp:AppSecret), maximo 64 KiB; firma invalida:
401, JSON invalido: 400, secreto ausente: 503. Actualizar solo message_id conocido.
La interfaz actual muestra resultado de la solicitud, no consulta entrega en bucle;
GET de notificacion permite consultar el ultimo estado persistido posteriormente.

## Contrato movil manual (revision 2026-11-06)

- Selector Manual/Automatico antes de guardar; Manual es el valor inicial de cada
  instancia del formulario. No persistir el modo ni credenciales nuevas.
- Configurar prefijo de pais en Conexion; persistir whatsAppCountryCode en las
  preferencias locales, inicialmente +57. Validar + seguido de 1 a 3 digitos con
  primero 1-9. Aplicarlo solo a destinatarios sin +; un numero internacional
  existente conserva su propio prefijo. No cambiar el telefono del turno/API.
- Manual conserva correo y no invoca POST de WhatsApp ni su reintento.
- Solo tras register completo conservar una copia en memoria: cliente, placa,
  marca, servicios seleccionados con precios de catalogo y total confirmado.
  Tambien preparar la copia tras PUT Turnos/{id}/EnProceso confirmado, sin crear
  otro turno ni solicitar notificaciones automaticas. No preparar ante error o
  ingreso/actualizacion parcial sin confirmar.
- Tras registro/actualizacion confirmado en Manual, solicitar una unica apertura
  de WhatsApp. Consumir esa solicitud en el ViewModel antes de abrir la actividad,
  sin repetir por recomposicion, remontaje o regreso desde WhatsApp. No requiere
  boton en el flujo normal. No abrir en Automatico, error o registro incierto.
- Permitir corregir telefono exclusivamente en la copia del mensaje; exigir +,
  primer digito 1-9 y de 8 a 15 digitos, admitiendo espacios, parentesis, punto y
  guion como separadores. Solo mostrar boton de reintento para error de apertura,
  numero invalido o destinatario corregido; no repetir escrituras/notificaciones.
- Un error de registro conserva solo etapa (consecutivo, vehiculo, servicios o
  confirmacion local) y codigo HTTP/tipo de excepcion en memoria para el aviso.
  No mostrar cuerpo HTTP, mensaje del servidor ni datos personales. No alterar
  la conciliacion estricta ni habilitar WhatsApp por disponer de un diagnostico.
- Prerrequisito SQLite de RegistrarTurnoServicio: idTunoServicios debe ser
  INTEGER PRIMARY KEY AUTOINCREMENT. El esquema antiguo sin clave puede insertar
  la relacion pero devolver HTTP 500 al leer un identificador NULL. No reenviar
  ciegamente: respaldar, validar esquema y aplicar la reparacion existente
  Database/Repairs/20261028_TurnoServiciosIdentity.sql de la API con autorizacion.
  Conservar asociaciones e IDs validos, asignar identidad a NULLs y verificar
  turnos/pagos antes de reanudar. No modificar rutas ni cuerpos HTTP del contrato.
- Construir ACTION_VIEW whatsapp://send?phone={digitos}&text={texto codificado}
  limitado a com.whatsapp; si no existe o rechaza apertura, intentar com.whatsapp.w4b.
  Construir parametros con Uri.Builder, preservando saltos de linea y caracteres
  especiales. No depender de enlaces web wa.me habilitados en Android.
  Nunca usar navegador u otro paquete como alternativa ni solicitar permisos.
- Texto visible en recursos Android: cliente, placa, marca, servicios y total.
  No inventar nombre comercial cuando no esta disponible en el movil.
- Formato manual: encabezado RECEPCION DE VEHICULO, etiquetas y total con *negrita*
  de WhatsApp, iconos Unicode, lineas en blanco entre secciones, una linea por
  servicio con icono y precio y agradecimiento final. Plantilla y formato de cada
  servicio en recursos Android. Preservar emojis, acentos, importes y saltos de
  linea al codificar text; no cambiar datos del turno ni la plantilla Meta.
- El usuario revisa y pulsa Enviar dentro de WhatsApp; abrir no significa enviado,
  aceptado o entregado y no crea ni modifica el estado de la notificacion API.
- Si startActivity finaliza correctamente, descartar inmediatamente la copia del
  mensaje y destinatario mediante clearManualWhatsApp, tanto en apertura automatica
  como tras reintento. El formulario ya vaciado al confirmar el guardado queda
  listo para otra placa; no conservar la confirmacion anterior al regresar.
  No limpiar si ambos paquetes fallan o el telefono es invalido. No borrar turnos,
  configuracion, estados de correo ni datos nuevos escritos en otro formulario.
- Conservar copia si no hay aplicacion; permitir descartarla. Limpiar al iniciar
  otro guardado, limpiar formulario o cambiar servidor. No guardar datos en disco
  ni cuerpos/destinatarios en logs; no se garantiza recuperacion al cerrar proceso.
- Automatico conserva contrato Meta existente y requiere API actualizada, emisor,
  token y plantilla aprobada. No activar Meta ni realizar pruebas reales aqui.

## Independencia del correo y confirmacion del registro (2026-11-06)

### Vinculo con comprobante de retiro (007)

La revision [007-retiro-vehiculo](../../007-retiro-vehiculo/contracts/api.md)
agrega a ambas alternativas del correo y al WhatsApp manual el mismo codigo
aleatorio persistido por la API, nunca generado por el movil. Produccion exige
sesion existente y emision correcta del comprobante antes de abrir el mensaje
manual; un error conserva el ingreso confirmado y muestra un aviso independiente,
sin preparar un comprobante incompleto ni repetir escrituras del turno.
El correo obtiene el codigo antes de preparar su envio/reintento; servicios sin
precios completos producen conflicto. No reenviar correos aceptados ni modificar
mensajes ya recibidos para agregarlo. La plantilla Meta de seis parametros sigue
intacta. Esta integracion si requiere API y APK actualizadas, a diferencia del
cambio previo limitado a presentacion HTML.

### Conciliacion sin cambios

- notifyEmail se ejecuta fuera del registro; excepcion, respuesta ausente o ID
  incorrecto dejan UNKNOWN de correo, sin bloquear WhatsApp manual ni volver a
  marcar el ingreso como incierto. Cancelacion se propaga. Ignorar respuestas
  tardias si se descarto/cambio el turno o se cambio el repositorio.
- Ante fallo posterior al inicio de escrituras, efectuar solo GET
  Turnos/EnProcesoPorPlaca. Confirmar exclusivamente si el ID es positivo y
  coinciden numero de turno, tipo, cliente, telefono, correo, marca, observaciones,
  valor, base y conjunto completo de servicios con el formulario enviado.
- Si coincide, retornar SavedTurn y continuar notificaciones normales. Nunca
  repetir POST/PUT del registro durante esa verificacion. Si la lectura falla,
  no existe o no coincide, conservar RegistrationException y revision requerida.
- Revision requerida describe un registro sin confirmar, no un fallo de correo.
  No se preparan mensajes de recepcion para registros realmente incompletos.
