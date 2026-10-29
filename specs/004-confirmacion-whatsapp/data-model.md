# Modelo de datos

## NotificaciónWhatsApp

Una notificación pertenece a un único `Turno` confirmado y se crea de forma
idempotente con una restricción única sobre `IdTurno` y el tipo de evento.

| Campo | Tipo | Reglas |
|---|---|---|
| Id | entero | clave primaria |
| IdTurno | entero | obligatorio, único por recepción |
| DestinatarioNormalizado | texto | E.164; no mostrar completo en logs |
| Estado | enum | `NOT_APPLICABLE`, `PENDING`, `RETRYING`, `ACCEPTED`, `DELIVERED`, `FAILED` |
| Intentos | entero | >= 0 y limitado |
| MessageIdExterno | texto nullable | ID devuelto por Meta |
| FbTraceId | texto nullable | diagnóstico no sensible |
| UltimoErrorCodigo | texto nullable | código estructurado de Meta |
| CreadoEn/ActualizadoEn | fecha | convención existente de API |
| ProximoIntentoEn | fecha nullable | solo para error transitorio |

No se almacenan tokens ni cuerpos completos. El contenido se reconstruye del
ingreso confirmado y de la plantilla configurada.

## Resultado y transiciones

La API devuelve `idTurno`, `estado` y `accion` (`NONE`, `RETRY_MANUAL` o
`CONFIGURE_DATA`), sin tokens, cuerpos de Meta ni número completo.

`NOT_APPLICABLE` es terminal por datos ausentes/no utilizables.
`PENDING -> RETRYING -> ACCEPTED -> DELIVERED`.
`PENDING/RETRYING -> FAILED` al agotar reintentos o recibir error permanente.
`FAILED -> RETRYING` solo por reintento manual sin `MessageIdExterno` aceptado.
