# Investigación: correo de confirmación

## Decisiones

- El API envía el correo mediante SMTP; Android y escritorio nunca almacenan
  credenciales SMTP.
- El correo incluye placa, marca, servicios autorizados con valor individual,
  total y aviso sobre servicios adicionales.
- La notificación se persiste de forma idempotente por `IdTurno`.
- Los errores transitorios tienen reintentos limitados y los permanentes quedan
  visibles para reintento manual.
- Las pruebas usan un cliente SMTP simulado y no envían correos reales.

## Restricciones

El correo del cliente es obligatorio para completar el ingreso. No se inventan
datos que no estén en el turno confirmado y no se registran destinatarios,
cuerpos ni contraseñas en logs.
