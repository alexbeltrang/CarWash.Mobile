---
description: "Tareas de implementación de confirmación por WhatsApp"
---

# Tasks: Confirmación de ingreso por WhatsApp

**Input**: Design documents from `specs/004-confirmacion-whatsapp/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/api.md`, `quickstart.md`

**Organization**: Las tareas están agrupadas por historia para permitir entregas y validación independientes.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar configuración segura y estructura común sin introducir secretos.

- [X] T001 Documentar en `C:\Work\CarWashApi\appsettings.example.json` las claves no secretas de Meta, nombre del autolavado y límites de reintento, sin valores reales
- [X] T002 [P] Crear la abstracción `IWhatsAppClient` en `C:\Work\CarWashApi\Services\IWhatsAppClient.cs` para aislar Meta Cloud API de la lógica de negocio
- [X] T003 [P] Crear el cliente HTTP tipado y su registro DI en `C:\Work\CarWashApi\Services\MetaWhatsAppClient.cs` y `C:\Work\CarWashApi\Program.cs`, leyendo token y phone number ID solo desde configuración del servidor
- [X] T004 [P] Crear el modelo de respuesta común `WhatsAppNotificationResult` en `C:\Work\CarWashApi\DTOs\WhatsAppNotificationResult.cs` con estados y acciones del contrato

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear la persistencia, configuración y reglas compartidas que bloquean ambas historias.

**⚠️ CRITICAL**: Ninguna historia puede iniciar hasta completar esta fase.

- [X] T005 Crear la entidad `WhatsAppNotification` en `C:\Work\CarWashApi\Modelos\WhatsAppNotification.cs` con estados, intentos, identificador externo, código de error y fechas, sin almacenar tokens ni cuerpos completos
- [X] T006 Registrar `DbSet<WhatsAppNotification>` y la restricción única por `IdTurno` en `C:\Work\CarWashApi\Context\AppDbContext.cs`
- [X] T007 Crear la migración SQLite idempotente de `WhatsAppNotification` en `C:\Work\CarWashApi\Migrations\`; su aplicación automática al arranque evita modificar o borrar datos existentes
- [X] T008 [P] Implementar `WhatsAppOptions` y validación de configuración en `C:\Work\CarWashApi\Configuration\WhatsAppOptions.cs`, distinguiendo configuración faltante de secreto inválido
- [X] T009 [P] Implementar normalización y validación E.164 sin registrar el número completo en `C:\Work\CarWashApi\Services\WhatsAppPhoneNumberValidator.cs`
- [X] T010 Implementar el constructor de plantilla y validación de campos obligatorios en `C:\Work\CarWashApi\Services\WhatsAppTemplateBuilder.cs`, rechazando color, marca/modelo o nombre comercial ausentes
- [ ] T011 [P] Crear pruebas unitarias de validación y plantilla en `C:\Work\CarWashApi.Tests\Services\WhatsAppTemplateBuilderTests.cs` usando números y datos no reales

**Checkpoint**: La API puede validar datos y persistir la notificación sin exponer secretos.

---

## Phase 3: User Story 1 - Notificar recepción del vehículo (Priority: P1) 🎯 MVP

**Goal**: Tras confirmar un ingreso completo, preparar un único mensaje con placa, servicios autorizados y total, conservar el ingreso ante cualquier fallo y evitar duplicados.

**Independent Test**: Con un cliente Meta simulado, confirmar un turno con servicios y verificar que se construye un solo payload correcto; simular destinatario inválido o timeout y comprobar que el turno permanece confirmado sin un segundo envío.

### Tests for User Story 1

- [ ] T012 [P] [US1] Crear pruebas de contrato para `POST /api/Turnos/{idTurno}/notificacion-whatsapp` en `C:\Work\CarWashApi.Tests\Controllers\WhatsAppNotificationControllerTests.cs`, cubriendo turno confirmado, inexistente y no confirmado
- [ ] T013 [P] [US1] Crear pruebas de integración con cliente Meta mock en `C:\Work\CarWashApi.Tests\Services\WhatsAppNotificationServiceTests.cs`, cubriendo payload, destinatario inválido, timeout, error permanente e idempotencia

### Implementation for User Story 1

- [X] T014 [US1] Implementar `WhatsAppNotificationService` en `C:\Work\CarWashApi\Services\WhatsAppNotificationService.cs` para cargar el turno confirmado, servicios y valores finales antes de enviar
- [X] T015 [US1] Implementar creación idempotente de `WhatsAppNotification` en `C:\Work\CarWashApi\Services\WhatsAppNotificationService.cs`, conservando el turno aunque Meta falle
- [X] T016 [US1] Implementar clasificación de respuestas Meta, captura de `message_id`/`fbtrace_id` y reintentos limitados solo para errores transitorios en `C:\Work\CarWashApi\Services\MetaWhatsAppClient.cs`
- [ ] T017 [US1] Implementar `POST /api/Turnos/{idTurno}/notificacion-whatsapp` en `C:\Work\CarWashApi\Controllers\WhatsAppNotificationController.cs` con respuestas `200`, `404` y `409` del contrato
- [X] T018 [US1] Encadenar la llamada de notificación después del guardado completo de turno y servicios en `C:\Work\CarWash.Mobile\app\src\main\java\com\carwash\mobile\ingreso\IngresoRepository.kt` sin hacer que un fallo de WhatsApp revierta el ingreso
- [X] T019 [US1] Añadir al contrato Retrofit y al flujo de registro la lectura del resultado de notificación en `app/src/main/java/com/carwash/mobile/ingreso/IngresoRepository.kt` y `IngresoViewModel.kt`

**Checkpoint**: Un ingreso confirmado genera como máximo una notificación y queda conservado aunque el envío sea rechazado o incierto.

---

## Phase 4: User Story 2 - Informar el resultado de la notificación (Priority: P2)

**Goal**: El recepcionista distingue estado aceptado, entregado, pendiente, fallido o no aplicable, y puede solicitar un reintento manual seguro.

**Independent Test**: Simular aceptación, entrega, error permanente, error transitorio agotado y webhook; verificar el estado mostrado sin modificar el turno confirmado.

### Tests for User Story 2

- [ ] T020 [P] [US2] Crear pruebas de transición de estados y reintento manual en `C:\Work\CarWashApi.Tests\Services\WhatsAppNotificationStatusTests.cs`
- [ ] T021 [P] [US2] Crear pruebas del webhook GET/POST con eventos `sent`, `delivered`, `read` y error en `C:\Work\CarWashApi.Tests\Controllers\WhatsAppWebhookControllerTests.cs`
- [ ] T022 [P] [US2] Crear pruebas de presentación de estados y reintento en `app/src/test/java/com/carwash/mobile/ingreso/WhatsAppNotificationUiTest.kt`

### Implementation for User Story 2

- [X] T023 [US2] Implementar `POST /api/Turnos/{idTurno}/notificacion-whatsapp/reintentar` en `C:\Work\CarWashApi\Controllers\WhatsAppNotificationController.cs`, bloqueando duplicados con `MessageIdExterno`
- [X] T024 [US2] Implementar `GET` y `POST /api/webhooks/whatsapp` en `C:\Work\CarWashApi\Controllers\WhatsAppWebhookController.cs` con verificación de challenge y actualización por `message_id`
- [X] T025 [US2] Implementar endpoint de consulta de estado en `C:\Work\CarWashApi\Controllers\WhatsAppNotificationController.cs` para que el computador no dependa de logs
- [X] T026 [US2] Añadir modelos Retrofit y método de consulta inicial en `app/src/main/java/com/carwash/mobile/ingreso/IngresoModels.kt` y `IngresoRepository.kt`
- [X] T027 [US2] Mostrar textos externalizados para `NOT_APPLICABLE`, `PENDING`, `ACCEPTED`, `DELIVERED` y `FAILED` en `app/src/main/res/values/strings.xml` y `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt`
- [X] T028 [US2] Añadir acción de reintento manual desde el computador en `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt` y `IngresoViewModel.kt`, sin reintentar automáticamente errores permanentes
- [X] T029 [US2] Ocultar teléfonos, cuerpos, tokens y datos personales de logs en `C:\Work\CarWashApi\Services\WhatsAppNotificationService.cs` y `MetaWhatsAppClient.cs`

**Checkpoint**: El operador ve el estado en menos de 10 segundos y puede reintentar solo cuando el contrato lo permite.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validación final, documentación y protección operativa.

- [X] T030 [P] Actualizar `specs/004-confirmacion-whatsapp/contracts/api.md` con el endpoint de consulta de estado descubierto durante la implementación
- [X] T031 [P] Documentar variables de entorno, permisos Meta, plantilla aprobada y configuración del webhook en `C:\Work\CarWashApi\README.md` sin incluir valores secretos
- [ ] T032 Ejecutar las validaciones de `specs/004-confirmacion-whatsapp/quickstart.md` usando mocks y confirmar que no se envían mensajes reales
- [ ] T033 Ejecutar `.\gradlew.bat :app:assembleDebug :app:lintDebug` en `C:\Work\CarWash.Mobile` y resolver fallos introducidos por la funcionalidad
- [X] T034 Ejecutar `dotnet test` y `dotnet build --no-restore` en `C:\Work\CarWashApi`, registrando bloqueos si no existe proyecto de pruebas
- [ ] T035 Ejecutar `.\gradlew.bat :app:connectedDebugAndroidTest` con un dispositivo autorizado y verificar los estados visibles sin usar datos de clientes reales

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no depende de otra fase.
- **Foundational (Phase 2)**: depende de Setup y bloquea las historias.
- **User Story 1 (Phase 3)**: depende de Foundational.
- **User Story 2 (Phase 4)**: depende de Foundational y de los contratos de US1 para mostrar el resultado del envío; el webhook puede desarrollarse en paralelo con la UI.
- **Polish (Phase 5)**: depende de las historias que se decida entregar.

### User Story Dependencies

- **US1 (P1)**: puede comenzar después de Phase 2; es el MVP.
- **US2 (P2)**: requiere el resultado persistido y los estados de US1, pero sus pruebas de webhook y UI pueden prepararse en paralelo con la implementación del servicio de US1.

### Parallel Opportunities

- **Setup**: T002, T003 y T004 pueden ejecutarse en paralelo después de T001.
- **Foundational**: T008, T009 y T011 pueden ejecutarse en paralelo; T005-T007 y T010 dependen de la estructura de datos/configuración correspondiente.
- **US1**: T012 y T013 pueden ejecutarse en paralelo antes de T014; T017 y T019 requieren el servicio/contrato definido por T014-T016.
- **US2**: T020, T021 y T022 pueden ejecutarse en paralelo; T023/T024/T025 pueden dividirse por archivo; T026-T028 dependen de los contratos.
- **Polish**: T030 y T031 pueden ejecutarse en paralelo; T033 y T034 pueden ejecutarse en paralelo si las implementaciones están completas.

## Implementation Strategy

1. **MVP**: completar Phase 1, Phase 2 y US1 con Meta mock; validar idempotencia y conservación del ingreso.
2. **Entrega operativa**: completar US2 para estados, webhooks y reintento manual.
3. **Cierre**: ejecutar documentación, build, lint, pruebas unitarias e instrumentadas y solo después configurar una prueba viva autorizada.

## Format Validation

Todas las tareas usan `- [ ]`, identificador secuencial, marcador `[P]` solo
cuando aplica, etiqueta `[US1]`/`[US2]` en fases de historia y una ruta de
archivo explícita.
