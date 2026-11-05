# Feature Specification: Confirmación de ingreso por correo electrónico y WhatsApp

**Feature Branch**: `004-confirmacion-whatsapp`

**Created**: 2026-10-28

**Status**: Correo conservado y WhatsApp adicional implementado/verificado con dobles el 2026-11-05. Alta Meta, actualizacion de API instalada y prueba real autorizada pendientes. Sin envios reales ni despliegue.

## Revision aprobada 2026-11-05: agregar WhatsApp

- Envio automatico mediante WhatsApp Business Platform de Meta al confirmar turno
	y todos sus servicios; conservar el correo existente como canal independiente.
- Mensaje con placa, servicios autorizados y total confirmado. Reutilizar saludo,
	marca y nombre comercial disponibles; no exigir un color inexistente en el modelo.
- Guardar/confirmar el ingreso no espera mensajeria ni se revierte si un canal falla.
- Configuracion incompleta se informa como NOT_CONFIGURED, sin llamar a Meta ni
	crear un envio irrecuperable; credenciales solo en servidor, nunca APK o Git.
- Identidad de notificacion por turno; repetir solicitud o recibir una aceptacion
	no debe reenviar. Un resultado incierto no se reintenta automaticamente.
- Mostrar estados separados por canal y permitir reintentar fallos seguros sin
	registrar nuevamente el ingreso. No enviar en guardados parciales/inciertos.
- El usuario aun no tiene numero habilitado ni plantilla aprobada en Meta:
	documentar el alta y validar con dobles, sin mensajes reales ni activar produccion.

**Input**: User description: "Cuando se haga el ingreso del vehículo, enviar por correo electrónico la recepción del vehículo, los servicios autorizados y el valor a pagar."

## User Scenarios & Testing

### User Story 1 - Notificar recepción del vehículo por correo (Priority: P1)

Después de completar un ingreso de vehículo, el cliente recibe por correo una confirmación con la placa, los servicios autorizados y el valor a pagar.

**Why this priority**: Reduce consultas y confirma al cliente los datos acordados en el momento de la recepción.

**Independent Test**: Completar un ingreso contra un cliente SMTP simulado y comprobar que el correo contiene exactamente los datos del ingreso confirmado, sin enviar mensajes reales.

**Acceptance Scenarios**:

1. **Given** un ingreso confirmado con número celular, servicios y valor válidos, **When** termina el registro, **Then** se prepara un único mensaje de confirmación con la información del vehículo, servicios y valor total.
2. **Given** el correo no es válido o el servidor SMTP falla, **When** se intenta notificar, **Then** el ingreso no se duplica ni se revierte y se informa el estado del correo.

### User Story 2 - Informar el resultado del correo (Priority: P2)

El recepcionista puede distinguir un ingreso confirmado de una notificación pendiente, fallida o entregada.

**Why this priority**: Evita que un problema de mensajería se interprete como un fallo del ingreso.

**Independent Test**: Simular respuestas exitosa, rechazada y timeout del servicio de mensajería y verificar estados y mensajes visibles.

**Acceptance Scenarios**:

1. **Given** el ingreso fue confirmado y el servicio acepta el mensaje, **When** finaliza el envío, **Then** se informa que el ingreso se completó y la notificación fue aceptada.
2. **Given** el ingreso fue confirmado pero el servicio no responde, **When** vence el tiempo de espera, **Then** el ingreso permanece confirmado y se informa la falla de notificación sin reintentos automáticos ilimitados.

## Edge Cases

- El correo está vacío o tiene formato inválido.
- El servidor SMTP está temporalmente indisponible o devuelve un límite de envío.
- El ingreso queda parcial o incierto: no se debe enviar una confirmación de ingreso completo.
- El mensaje contiene varios servicios, servicios con precio nulo o un valor editado manualmente.
- La notificación se procesa más de una vez: no debe generar mensajes duplicados para el mismo ingreso.
- El contenido incluye datos personales; no se deben escribir correos, cuerpos de mensajes ni credenciales en logs.

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST exigir un correo electrónico válido para completar el ingreso.
- **FR-002**: El correo MUST incluir placa, servicios autorizados y valor total a pagar del ingreso confirmado.
- **FR-003**: El sistema MUST conservar el ingreso confirmado aunque la notificación falle, quede pendiente o sea rechazada.
- **FR-004**: El sistema MUST mostrar al recepcionista si el correo fue aceptado, falló, quedó pendiente o no aplica.
- **FR-005**: El sistema MUST evitar mensajes duplicados para un mismo ingreso confirmado.
- **FR-006**: El sistema MUST validar el correo antes de intentar notificar y MUST impedir el ingreso si no es utilizable.
- **FR-007**: El sistema MUST notificar errores de mensajería sin ocultarlos ni marcarlos como ingreso fallido.
- **FR-008**: El sistema MUST NOT enviar mensajes reales durante pruebas automatizadas o de integración sin autorización explícita.
- **FR-009**: La integración MUST usar SMTP configurado de forma segura en el servidor, sin credenciales en la aplicación móvil ni en el repositorio.
- **FR-010**: El sistema MUST usar un correo en español con saludo, placa, marca, servicios autorizados con valor individual, total a pagar, aviso de aprobación de servicios adicionales y nombre del autolavado.
- **FR-011**: El formulario de recepción del aplicativo de escritorio MUST ocultar el campo de número de orden, enviar ese valor vacío y ubicar el correo electrónico en su espacio visual sin alterar las operaciones existentes.
- **FR-012**: El registro móvil MUST mostrar el ingreso confirmado sin esperar la respuesta SMTP; la notificación de correo debe ejecutarse después en segundo plano.
- **FR-013**: Al buscar una placa con un turno en proceso (`Turnos.Estado = false`), el aplicativo móvil MUST cargar sus datos y servicios, indicarlo al usuario y permitir actualizar datos del turno, agregar o quitar servicios y guardar los cambios sin crear un turno nuevo ni consumir consecutivo; los servicios quitados se marcan `IsDeleted`. Si cambia la placa, el formulario vuelve al modo de nuevo ingreso.
- **FR-011**: Ante una falla transitoria, el sistema MUST realizar reintentos automáticos limitados y después ofrecer un reintento manual desde el computador, sin duplicar mensajes.
- **FR-012**: Si la fuente de datos no dispone de marca o nombre comercial, el sistema MUST informar que la notificación no puede completarse; no debe inventar valores.

### Key Entities

- **Notificación de correo**: intento asociado a un ingreso confirmado, con destinatario, estado y fechas; no almacena credenciales ni cuerpos completos.
- **Mensaje de recepción**: representación del texto enviado, compuesto por placa, lista de servicios y valor total.
- **Preferencia de contacto**: indicación de si el cliente acepta o rechaza notificaciones, si el negocio la requiere.
- **Configuración SMTP**: servidor, puerto, remitente y límites de reintento, con contraseña solo en el almacén seguro del servidor.

## Success Criteria

### Measurable Outcomes

- **SC-001**: En pruebas controladas, el 100% de los ingresos confirmados con correo válido genera como máximo una notificación y el mensaje contiene placa, todos los servicios y valor correcto.
- **SC-002**: Un fallo del correo no causa pérdida, duplicación ni reversión del ingreso confirmado en el 100% de los casos probados.
- **SC-003**: El recepcionista puede identificar el estado del ingreso y de la notificación en menos de 10 segundos después de finalizar el registro.
- **SC-004**: Las pruebas automatizadas no envían correos reales ni exponen credenciales.

## Assumptions

- El cliente proporciona un correo válido y el negocio tiene autorización legal/operativa para contactarlo.
- La notificación es posterior al ingreso; el correo no reemplaza la confirmación del registro ni participa en el cálculo del valor.
- Los servicios y el valor se toman del ingreso finalmente confirmado, no de un borrador anterior.
- La integración no agregará reservas, pagos, autenticación de clientes ni reglas nuevas de negocio.
- La API CarWash seguirá siendo la autoridad del ingreso; el servicio de mensajería solo transporta la notificación.
- El negocio cuenta con un servidor SMTP y un mecanismo seguro de configuración de sus credenciales fuera del código y del APK.
- El nombre del autolavado será una configuración del negocio. La marca se enviará si existe en el registro confirmado.
- Los reintentos limitados se aplicarán solo a errores transitorios; errores de validación, consentimiento, plantilla o destinatario no se reintentarán automáticamente.
