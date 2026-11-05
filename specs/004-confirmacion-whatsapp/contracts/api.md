# Contrato API: notificación por correo

La API CarWash sigue siendo autoridad del ingreso. Estos endpoints no crean ni
modifican turnos; operan sobre un turno confirmado.

El asunto del correo incluye la placa y la marca del vehículo y no incluye el
identificador interno ni el número visible del turno.

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
El movil solicita ambos canales solo despues de confirmar turno y servicios,
sin bloquear ni revertir el registro; cada canal mantiene su propio estado.

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
