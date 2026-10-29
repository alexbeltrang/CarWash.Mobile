# Feature Specification: Confirmación de ingreso por WhatsApp

**Feature Branch**: `004-confirmacion-whatsapp`

**Created**: 2026-10-28

**Status**: Draft

**Input**: User description: "Cuando se haga el ingreso del vehículo, enviar al número celular un mensaje a WhatsApp informando la recepción del vehículo, los servicios autorizados y el valor a pagar."

## User Scenarios & Testing

### User Story 1 - Notificar recepción del vehículo (Priority: P1)

Después de completar un ingreso de vehículo, el cliente recibe en WhatsApp una confirmación con la placa, los servicios autorizados y el valor a pagar.

**Why this priority**: Reduce consultas y confirma al cliente los datos acordados en el momento de la recepción.

**Independent Test**: Completar un ingreso contra un servicio de mensajería simulado y comprobar que el mensaje contiene exactamente los datos del ingreso confirmado, sin enviar mensajes reales.

**Acceptance Scenarios**:

1. **Given** un ingreso confirmado con número celular, servicios y valor válidos, **When** termina el registro, **Then** se prepara un único mensaje de confirmación con la información del vehículo, servicios y valor total.
2. **Given** el número celular no puede recibir WhatsApp, **When** se intenta notificar, **Then** el ingreso no se duplica ni se revierte y se informa que la notificación no fue entregada.

### User Story 2 - Informar el resultado de la notificación (Priority: P2)

El recepcionista puede distinguir un ingreso confirmado de una notificación pendiente, fallida o entregada.

**Why this priority**: Evita que un problema de mensajería se interprete como un fallo del ingreso.

**Independent Test**: Simular respuestas exitosa, rechazada y timeout del servicio de mensajería y verificar estados y mensajes visibles.

**Acceptance Scenarios**:

1. **Given** el ingreso fue confirmado y el servicio acepta el mensaje, **When** finaliza el envío, **Then** se informa que el ingreso se completó y la notificación fue aceptada.
2. **Given** el ingreso fue confirmado pero el servicio no responde, **When** vence el tiempo de espera, **Then** el ingreso permanece confirmado y se informa la falla de notificación sin reintentos automáticos ilimitados.

## Edge Cases

- El teléfono está vacío, tiene formato inválido o no está asociado a WhatsApp.
- El cliente no autorizó recibir mensajes o solicita no recibirlos.
- El servicio de WhatsApp está temporalmente indisponible o devuelve un límite de envío.
- El ingreso queda parcial o incierto: no se debe enviar una confirmación de ingreso completo.
- El mensaje contiene varios servicios, servicios con precio nulo o un valor editado manualmente.
- La notificación se procesa más de una vez: no debe generar mensajes duplicados para el mismo ingreso.
- El contenido incluye datos personales; no se deben escribir teléfonos, cuerpos de mensajes ni credenciales en logs.

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST intentar una notificación de WhatsApp únicamente después de confirmar completamente el ingreso y sus servicios.
- **FR-002**: El mensaje MUST incluir placa, servicios autorizados y valor total a pagar del ingreso confirmado.
- **FR-003**: El sistema MUST conservar el ingreso confirmado aunque la notificación falle, quede pendiente o sea rechazada.
- **FR-004**: El sistema MUST mostrar al recepcionista si la notificación fue aceptada, falló, quedó pendiente o no aplica.
- **FR-005**: El sistema MUST evitar mensajes duplicados para un mismo ingreso confirmado.
- **FR-006**: El sistema MUST validar el número celular antes de intentar notificar y MUST permitir continuar sin WhatsApp cuando el número no sea utilizable.
- **FR-007**: El sistema MUST notificar errores de mensajería sin ocultarlos ni marcarlos como ingreso fallido.
- **FR-008**: El sistema MUST NOT enviar mensajes reales durante pruebas automatizadas o de integración sin autorización explícita.
- **FR-009**: La integración MUST usar la API oficial de WhatsApp Business / Meta Cloud API y credenciales configuradas de forma segura, sin secretos en la aplicación móvil ni en el repositorio.
- **FR-010**: El sistema MUST usar una plantilla aprobada en español con el contenido proporcionado por el negocio: saludo, placa, marca y modelo, color, servicios autorizados con valor individual, total a pagar, aviso de aprobación de servicios adicionales y nombre del autolavado.
- **FR-011**: Ante una falla transitoria, el sistema MUST realizar reintentos automáticos limitados y después ofrecer un reintento manual desde el computador, sin duplicar mensajes.
- **FR-012**: Si la fuente de datos no dispone de color, marca/modelo o nombre comercial, el sistema MUST solicitar su configuración o informar que la notificación no puede completarse; no debe inventar valores ni enviar una plantilla incompleta como si fuera válida.

### Key Entities

- **Notificación de WhatsApp**: intento asociado a un ingreso confirmado, con destinatario, contenido resumido, estado, identificador externo y fechas de creación/entrega; no almacena credenciales.
- **Mensaje de recepción**: representación del texto enviado, compuesto por placa, lista de servicios y valor total.
- **Preferencia de contacto**: indicación de si el cliente acepta o rechaza notificaciones, si el negocio la requiere.
- **Plantilla aprobada**: contenido parametrizado en español y sus identificadores de plantilla de Meta, versión y variables autorizadas.

## Success Criteria

### Measurable Outcomes

- **SC-001**: En pruebas controladas, el 100% de los ingresos confirmados con teléfono válido genera como máximo una notificación y el mensaje contiene placa, todos los servicios y valor correcto.
- **SC-002**: Un fallo de WhatsApp no causa pérdida, duplicación ni reversión del ingreso confirmado en el 100% de los casos probados.
- **SC-003**: El recepcionista puede identificar el estado del ingreso y de la notificación en menos de 10 segundos después de finalizar el registro.
- **SC-004**: Las pruebas automatizadas no envían mensajes a números reales ni exponen credenciales.

## Assumptions

- El cliente proporciona un número celular válido y el negocio tiene autorización legal/operativa para contactarlo por WhatsApp.
- La notificación es posterior al ingreso; WhatsApp no reemplaza la confirmación del registro ni participa en el cálculo del valor.
- Los servicios y el valor se toman del ingreso finalmente confirmado, no de un borrador anterior.
- La integración no agregará reservas, pagos, autenticación de clientes ni reglas nuevas de negocio.
- La API CarWash seguirá siendo la autoridad del ingreso; el servicio de mensajería solo transporta la notificación.
- El negocio cuenta con una cuenta de WhatsApp Business en Meta Cloud API, una plantilla aprobada y un mecanismo seguro de configuración de sus credenciales fuera del código y del APK.
- El nombre del autolavado será una configuración del negocio. La marca/modelo y el color solo se enviarán si existen en el registro confirmado; actualmente el formulario de ingreso no solicita color.
- Los reintentos limitados se aplicarán solo a errores transitorios; errores de validación, consentimiento, plantilla o destinatario no se reintentarán automáticamente.
