# Modelo de datos: confirmación por correo

## WhatsApp adicional (2026-11-05)

Reutilizar WhatsAppNotifications existente, sin tabla ni migracion nueva. Indice
unico por IdTurno; MessageIdExterno impide segundo envio tras aceptacion. Intentos
se incrementa atomicamente al reclamar envio, hasta MaxAttempts. No guardar token
ni cuerpos completos. Estado NOT_CONFIGURED es una respuesta sin fila nueva.

Transiciones: PENDING -> SENDING -> ACCEPTED -> DELIVERED; SENDING -> RETRYING
para rechazo transitorio o FAILED para rechazo definitivo/limite. Reintento manual
seguro: RETRYING/FAILED -> SENDING si no hay MessageIdExterno ni limite agotado.
Timeout, cancelacion o respuesta ambigua -> UNKNOWN; no se reenvia automaticamente
ni se ofrece reintento sin conciliar. SENDING que sobreviva a un reinicio tambien
requiere conciliacion: no se presume fallo para reenviar. Datos no utilizables ->
NOT_APPLICABLE. Entrega solo se confirma con webhook firmado y message_id conocido.

Android mantiene whatsAppTurnId/Status/CanRetry/Sending separados del correo y
descarta respuestas de otra identidad de repositorio o turno. Ocultar estado no
cancela un envio aceptado; reintentar nunca registra otro turno. No persistir esos
datos en preferencias ni introducir una restriccion de celular para guardar ingreso.

## NotificaciónEmail

Una notificación pertenece a un único `Turno` confirmado y se crea de forma
idempotente con una restricción única sobre `IdTurno`.

| Campo | Tipo | Reglas |
|---|---|---|
| Id | entero | clave primaria |
| IdTurno | entero | obligatorio, único por recepción |
| Destinatario | texto | correo validado; no mostrar completo en logs |
| Estado | enum | `NOT_APPLICABLE`, `PENDING`, `RETRYING`, `ACCEPTED`, `DELIVERED`, `FAILED` |
| Intentos | entero | >= 0 y limitado |
| UltimoErrorCodigo | texto nullable | código estructurado de Meta |
| CreadoEn/ActualizadoEn | fecha | convención existente de API |
| ProximoIntentoEn | fecha nullable | solo para error transitorio |

No se almacenan tokens ni cuerpos completos. El contenido se reconstruye del
ingreso confirmado y de la plantilla configurada.

## Resultado y transiciones

La API devuelve `idTurno`, `estado` y `accion` (`NONE`, `RETRY_MANUAL` o
`CONFIGURE_DATA`), sin tokens, cuerpos de Meta ni número completo.

`NOT_APPLICABLE` es terminal por correo ausente/no utilizable.
`PENDING -> RETRYING -> ACCEPTED -> DELIVERED`.
`PENDING/RETRYING -> FAILED` al agotar reintentos o recibir error permanente.
`FAILED -> RETRYING` solo por reintento manual sin `MessageIdExterno` aceptado.
