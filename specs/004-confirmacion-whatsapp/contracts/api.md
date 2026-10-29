# Contrato API: notificación WhatsApp

La API CarWash sigue siendo autoridad del ingreso. Estos endpoints no crean ni
modifican turnos; operan sobre un turno confirmado.

## Crear o consultar notificación

`POST /api/Turnos/{idTurno}/notificacion-whatsapp`

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

`GET /api/Turnos/{idTurno}/notificacion-whatsapp`

Devuelve el último estado persistido sin volver a llamar a Meta.

## Reintento manual

`POST /api/Turnos/{idTurno}/notificacion-whatsapp/reintentar`

Solo permite reintentar `FAILED` sin `MessageIdExterno` aceptado y nunca crea
una segunda notificación.

## Webhook de Meta

`GET /api/webhooks/whatsapp` valida el challenge configurado.
`POST /api/webhooks/whatsapp` procesa `sent`, `delivered`, `read` y errores por
`message_id`, responde rápidamente y no registra datos personales.

## Configuración

`META_WA_GRAPH_VERSION`, `META_WA_PHONE_NUMBER_ID`, `META_WA_ACCESS_TOKEN`,
`META_WA_TEMPLATE_NAME`, `META_WA_TEMPLATE_LANGUAGE` y `CARWASH_NAME` son
configuración del servidor/secret store, nunca valores compilados en el APK.
