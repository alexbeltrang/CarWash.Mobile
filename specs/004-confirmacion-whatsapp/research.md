# Investigación: WhatsApp de confirmación

## Decisiones

### Integración oficial y ubicación
- **Decisión:** usar Meta Cloud API desde `CarWashApi`, no desde Android.
- **Razón:** el token, el phone number ID y la plantilla son configuración de servidor. El APK solo recibe el resultado.
- **Alternativa descartada:** enviar directamente desde Android expone credenciales y dificulta idempotencia y auditoría.

### Plantilla
- **Decisión:** plantilla aprobada en español, categoría `utility`, con parámetros en el orden aprobado por Meta.
- **Restricción:** el modelo actual no tiene color. No se inventará ni se enviará una plantilla incompleta; se bloqueará la notificación como `NOT_APPLICABLE` hasta configurar una fuente legítima.

### Autenticación y endpoint
- **Decisión:** `POST https://graph.facebook.com/{version}/{PHONE_NUMBER_ID}/messages` con `Authorization: Bearer {token}` desde configuración segura del servidor.
- **Razón:** el phone number ID no es el número visible. Los tokens temporales sirven solo para pruebas iniciales; producción requiere un token de sistema con permisos asignados.

### Idempotencia y estados
- **Decisión:** clave única estable por ingreso confirmado, estado persistido, intentos, identificador de Meta y `fbtrace_id`, más webhooks de entrega/error.
- **Razón:** Messages API no documenta una clave de idempotencia del cliente ni garantía de deduplicación.
- **Timeout:** no repetir ciegamente una solicitud ambigua; reconciliar por webhook o intervención manual.

### Reintentos
- **Decisión:** reintentar solo errores transitorios, con backoff exponencial acotado y jitter. No reintentar permisos, autenticación, plantilla, consentimiento o destinatario inválido.
- **Razón:** clasificar por `error.code` y `error_data.details`, no solo por HTTP status o texto.

### Pruebas
- **Decisión:** mock de Graph API y webhooks; pruebas vivas únicamente con número de prueba y destinatario permitido, con autorización explícita.
- **Razón:** `/messages` no ofrece un modo documentado de dry-run.

## Fuentes

- [Template fundamentals](https://developers.facebook.com/documentation/business-messaging/whatsapp/templates/overview)
- [Send messages](https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/send-messages)
- [Messages API reference](https://developers.facebook.com/documentation/business-messaging/whatsapp/reference/whatsapp-business-phone-number/message-api)
- [Cloud API setup](https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started)
- [Access tokens](https://developers.facebook.com/documentation/business-messaging/whatsapp/access-tokens)
- [Error codes](https://developers.facebook.com/documentation/business-messaging/whatsapp/support/error-codes)
- [Webhooks](https://developers.facebook.com/documentation/business-messaging/whatsapp/webhooks/overview)
