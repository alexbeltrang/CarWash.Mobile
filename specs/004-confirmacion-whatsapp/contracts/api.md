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
