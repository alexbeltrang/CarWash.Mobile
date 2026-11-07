---
description: "Tareas de implementación de confirmación por correo electrónico"
---

# Tasks: Confirmación de ingreso por correo electrónico

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
- [X] T011 [P] Crear pruebas unitarias de validación y plantilla en `C:\Work\CarWashApi.Tests\Services\WhatsAppTemplateBuilderTests.cs` usando números y datos no reales

**Checkpoint**: La API puede validar datos y persistir la notificación sin exponer secretos.

---

## Phase 3: User Story 1 - Notificar recepción del vehículo (Priority: P1) 🎯 MVP

**Goal**: Tras confirmar un ingreso completo, preparar un único mensaje con placa, servicios autorizados y total, conservar el ingreso ante cualquier fallo y evitar duplicados.

**Independent Test**: Con un cliente Meta simulado, confirmar un turno con servicios y verificar que se construye un solo payload correcto; simular destinatario inválido o timeout y comprobar que el turno permanece confirmado sin un segundo envío.

### Tests for User Story 1

- [X] T012 [P] [US1] No aplica: el canal WhatsApp fue sustituido por correo electrónico
- [X] T013 [P] [US1] No aplica: el canal WhatsApp fue sustituido por correo electrónico

### Implementation for User Story 1

- [X] T014 [US1] Implementar `WhatsAppNotificationService` en `C:\Work\CarWashApi\Services\WhatsAppNotificationService.cs` para cargar el turno confirmado, servicios y valores finales antes de enviar
- [X] T015 [US1] Implementar creación idempotente de `WhatsAppNotification` en `C:\Work\CarWashApi\Services\WhatsAppNotificationService.cs`, conservando el turno aunque Meta falle
- [X] T016 [US1] Implementar clasificación de respuestas Meta, captura de `message_id`/`fbtrace_id` y reintentos limitados solo para errores transitorios en `C:\Work\CarWashApi\Services\MetaWhatsAppClient.cs`
- [X] T017 [US1] Implementar `POST /api/Turnos/{idTurno}/notificacion-whatsapp` en `C:\Work\CarWashApi\Controllers\WhatsAppNotificationController.cs` con respuestas `200`, `404` y `409` del contrato
- [X] T018 [US1] Encadenar la llamada de notificación después del guardado completo de turno y servicios en `C:\Work\CarWash.Mobile\app\src\main\java\com\carwash\mobile\ingreso\IngresoRepository.kt` sin hacer que un fallo de WhatsApp revierta el ingreso
- [X] T019 [US1] Añadir al contrato Retrofit y al flujo de registro la lectura del resultado de notificación en `app/src/main/java/com/carwash/mobile/ingreso/IngresoRepository.kt` y `IngresoViewModel.kt`

**Checkpoint**: Un ingreso confirmado genera como máximo una notificación y queda conservado aunque el envío sea rechazado o incierto.

---

## Phase 4: User Story 2 - Informar el resultado de la notificación (Priority: P2)

**Goal**: El recepcionista distingue estado aceptado, entregado, pendiente, fallido o no aplicable, y puede solicitar un reintento manual seguro.

**Independent Test**: Simular aceptación, entrega, error permanente, error transitorio agotado y webhook; verificar el estado mostrado sin modificar el turno confirmado.

### Tests for User Story 2

- [X] T020 [P] [US2] No aplica: el canal WhatsApp fue sustituido por correo electrónico
- [X] T021 [P] [US2] No aplica: el canal WhatsApp fue sustituido por correo electrónico
- [X] T022 [P] [US2] Cubrir presentación de estado y reintento en `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt`

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
- [X] T032 No aplica: el quickstart fue actualizado para SMTP y no se realizan envíos reales durante la validación
- [X] T033 Ejecutar `.\gradlew.bat :app:assembleDebug :app:lintDebug` en `C:\Work\CarWash.Mobile` y resolver fallos introducidos por la funcionalidad
- [X] T034 Ejecutar `dotnet test` y `dotnet build --no-restore` en `C:\Work\CarWashApi`, registrando bloqueos si no existe proyecto de pruebas
- [X] T035 Ejecutar `.\gradlew.bat :app:connectedDebugAndroidTest` con un dispositivo autorizado y verificar los estados visibles sin usar datos de clientes reales (16 pruebas exitosas en `25100RA69G`)

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

## Cambio de canal: correo SMTP

- [X] T036 Agregar `CorreoElectronico` al modelo de turno, persistencia SQLite y contratos de búsqueda/registro en API, Android y escritorio
- [X] T037 Implementar cliente SMTP, persistencia idempotente y endpoints de notificación por correo en `CarWashApi`
- [X] T038 Reemplazar el contrato móvil de WhatsApp por correo, exigir correo válido y mostrar sus estados
- [X] T039 Agregar captura obligatoria de correo y envío posterior al registro desde el aplicativo de escritorio
- [X] T040 Cambiar la paleta visual de Android y escritorio de verde a azul
- [X] T041 Validar API, Android y escritorio sin enviar correos reales
- [X] T042 Actualizar el asunto del correo para mostrar placa y marca sin exponer códigos internos
- [X] T043 Modernizar el formulario WinForms, ocultar Orden y ubicar CorreoElectronico en su espacio sin cambiar operaciones
- [X] T044 Desacoplar el envío móvil del correo para no bloquear la confirmación del ingreso
- [X] T045 Cargar y editar desde Android el turno en proceso de una placa (API `GET Turnos/EnProcesoPorPlaca`, `PUT Turnos/{id}/EnProceso`) con pruebas unitarias

## WhatsApp adicional aprobado 2026-11-05

- [X] T046 Corregir plantilla y configurar Meta opcional sin secretos, sin exigir color inexistente; probar construccion y configuracion ausente en API.
- [X] T047 Proteger idempotencia, intentos y resultados inciertos del envio WhatsApp con pruebas aisladas; conservar correo e ingreso confirmado.
- [X] T048 Solicitar WhatsApp y correo independientemente tras guardado completo en Android; mostrar estados separados y reintento seguro, con pruebas JVM/UI.
- [X] T049 Documentar alta de Meta, plantilla, consentimiento y secretos del servidor; compilar/lint, ejecutar regresion y generar APK sin enviar mensajes reales ni desplegar API.

Evidencia T046-T049: ver quickstart, revision 2026-11-05. 79 pruebas API,
84 JVM y 45 instrumentadas correctas, 0 fallos; una integracion opcional del
reporte omitida sin URL de fixture. Compilacion/lint correctos (0 errores,
6 advertencias). Meta aun no configurado; no se enviaron mensajes reales.

## WhatsApp manual y automatico aprobado 2026-11-06

- [X] T050 [US1] Agregar selector manual/automatico con manual inicial y copia en memoria tras ingreso completo en `app/src/main/java/com/carwash/mobile/ingreso/IngresoViewModel.kt`; probar ausencia de llamadas Meta, total confirmado, limpieza y errores en `app/src/test/java/com/carwash/mobile/ingreso/IngresoViewModelTest.kt`.
- [X] T051 [US1] Mostrar vista previa y destinatario internacional corregible; abrir exclusivamente WhatsApp/Business sin afirmar entrega en `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt` y `app/src/main/res/values/strings.xml`.
- [X] T052 [US2] Verificar apertura simulada, destinatario, contenido, ausencia de app, no duplicacion y regresion del modo automatico en `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt`.
- [X] T053 Actualizar `specs/004-confirmacion-whatsapp/contracts/api.md` y `specs/004-confirmacion-whatsapp/quickstart.md`; ejecutar pruebas, assembleDebug/lintDebug y generar `app/build/outputs/apk/debug/app-debug.apk`, sin instalar ni enviar mensajes reales.

Evidencia T050-T053: 89 pruebas JVM y 15 instrumentadas de MainActivityTest
correctas, sin fallos ni omisiones. assembleDebug/lintDebug correctos, 0 errores
y 6 advertencias existentes. Aperturas WhatsApp simuladas; prueba en celular
real pendiente. Sin llamadas reales a Meta, despliegue API ni instalacion APK.

## Correccion de independencia de correo y WhatsApp 2026-11-06

- [X] T054 [US1] Separar errores y respuestas tardias de correo del ingreso confirmado en `app/src/main/java/com/carwash/mobile/ingreso/IngresoViewModel.kt`; confirmar guardados inciertos mediante GET sin repetir escrituras en `app/src/main/java/com/carwash/mobile/ingreso/IngresoRepository.kt`, con regresion JVM.
- [X] T055 [US2] Ofrecer WhatsApp manual tras actualizar turno existente y probar correo fallido/apertura sin duplicar ingreso en `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt`; aclarar revision requerida en `app/src/main/res/values/strings.xml`.
- [X] T056 Actualizar `specs/004-confirmacion-whatsapp/contracts/api.md` y `specs/004-confirmacion-whatsapp/quickstart.md`, validar APK/lint/pruebas e instalar actualizacion conservando datos si el celular sigue autorizado, sin envios reales.

Evidencia T054-T056: 93 pruebas JVM y 17 MainActivityTest correctas, 0 fallos
y 0 errores. assembleDebug/lintDebug correctos. Instrumentacion limitada al
AVD mediante ANDROID_SERIAL, sin instalar pruebas ni limpiar datos del celular.
APK corregido instalado con adb install -r y MainActivity iniciada con Status: ok.
Consultas de diagnostico solo lectura; sin turnos ficticios ni mensajes reales.

## Apertura nativa de WhatsApp 2026-11-06

- [X] T057 [US1] Reemplazar enlace web wa.me por whatsapp://send con phone y text codificados en `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt`; actualizar comprobaciones de URI y caracteres especiales en `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt`.
- [X] T058 [US2] Validar destinatario/texto, alternativa Business, app ausente y correo fallido en celular fisico; compilar/lint, actualizar `specs/004-confirmacion-whatsapp/quickstart.md` e instalar APK conservando datos del celular, sin enviar mensajes reales.

Evidencia T057-T058: cinco MainActivityTest correctas en Xiaomi fisico mediante
adb am instrument, usando MemorySettings/FakeRepository y aperturas interceptadas.
JVM, assembleDebug, assembleDebugAndroidTest y lintDebug correctos. Ambos APK
actualizados con install -r -t sin desinstalar CarWash; MainActivity Status: ok.
Enlace nativo resuelto por WhatsApp instalado. El usuario solicito no usar emulador;
se cerro sin ejecutar pruebas en el. Sin mensajes reales ni escrituras API.
La vista previa dentro de la aplicacion WhatsApp real aun requiere confirmacion visual.

## Diagnostico de registro incierto 2026-11-06

- [X] T059 [US1] Conservar etapa del registro y codigo seguro en `app/src/main/java/com/carwash/mobile/ingreso/IngresoRepository.kt` y `app/src/main/java/com/carwash/mobile/ingreso/IngresoViewModel.kt`; probar etapas y ausencia de cuerpos HTTP en `app/src/test/java/com/carwash/mobile/ingreso/IngresoRepositoryTest.kt`.
- [X] T060 [US2] Mostrar diagnostico mediante recursos en `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt`, verificarlo en celular fisico con `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt` e instalar APK conservando datos; actualizar `specs/004-confirmacion-whatsapp/quickstart.md`.
- [X] T061 [US1] Capturar etapa/codigo del fallo real, reparar con autorizacion la identidad SQLite de TurnoServicios mediante `Database/Repairs/20261028_TurnoServiciosIdentity.sql` de la API y verificar borrador en WhatsApp real; documentar en `specs/004-confirmacion-whatsapp/quickstart.md`, sin registros ficticios ni envios duplicados.
- [X] T062 [US1] Agregar regresion aislada en `TurnoServiciosTests.cs` del proyecto hermano CarWashApi.Tests: reproducir HTTP500, conservar fila insertada e IDs existentes, permitir siguiente POST201 y rechazar esquemas sanos/no compatibles; reutilizar el script SQL existente.

Evidencia T059-T060: 94 JVM y seis MainActivityTest en Xiaomi fisico correctas,
0 fallos/errores/omisiones. assembleDebug, assembleDebugAndroidTest y lintDebug
correctos. APK actualizado con install -r -t; MainActivity Status: ok. Sin API
modificada ni mensajes reales. El fallo operativo original no esta resuelto:
la excepcion original no estaba disponible y falta reproducirla con diagnostico.

Evidencia posterior T061-T062: aviso real del turno 004 identifico servicios/HTTP500.
Base operativa verificada: 404 relaciones, ocho IDs NULL y esquema sin clave.
Tres pruebas xUnit y script aislado existentes correctos. Con autorizacion explicita,
API detenida, respaldo SQLite verificado y reparacion aplicada. Resultado: 404 IDs
unicos, asociaciones e IDs validos conservados; turnos/pagos identicos al respaldo.
API original reiniciada sin recompilar y consultas reales responden HTTP200; turno004
con una relacion, sin duplicados. Borrador nativo directo en Xiaomi: 159 caracteres,
igualdad completa, placa/servicios/total correctos. +57 confirmado para destinatario
manual; sin modificar celular guardado y sin pulsar Enviar. Usuario informo envio
manual propio antes de repetir la prueba. Guardado nuevo desde CarWash no repetido.

## Prefijo configurable y apertura automatica 2026-11-06

- [X] T063 [US1] Persistir prefijo WhatsApp inicialmente +57 y normalizar solo destinatarios manuales locales en `app/src/main/java/com/carwash/mobile/ingreso/IngresoModels.kt` y `app/src/main/java/com/carwash/mobile/ingreso/IngresoViewModel.kt`, con regresion en `app/src/test/java/com/carwash/mobile/ingreso/IngresoViewModelTest.kt`.
- [X] T064 [US2] Agregar configuracion de pais, apertura unica automatica tras confirmacion y boton solo para fallos/correcciones en `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt`; textos en `app/src/main/res/values/strings.xml` y pruebas fisicas en `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt`.
- [X] T065 Actualizar `specs/004-confirmacion-whatsapp/contracts/api.md` y `specs/004-confirmacion-whatsapp/quickstart.md`; validar JVM, APK, lint y 21 MainActivityTest en celular fisico e instalar actualizacion conservando datos, sin mensajes reales.

Evidencia T063-T065: 96 JVM y 21 MainActivityTest correctas en Xiaomi fisico,
0 fallos/errores/omisiones. assembleDebug/assembleDebugAndroidTest/lintDebug
correctos; APK actualizado con install -r -t. Sin emulador, API modificada ni
mensajes reales. Flujo nuevo de guardado real pendiente de proximo ingreso legitimo.

## Limpieza tras apertura de WhatsApp 2026-11-06

- [X] T066 [US1] Descartar copia y destinatario tras apertura correcta automatica o reintento en `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt`, conservando mensaje ante fallos; probar siguiente placa y reintento sin duplicados en `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt`.
- [X] T067 [US2] Actualizar `specs/004-confirmacion-whatsapp/contracts/api.md` y `specs/004-confirmacion-whatsapp/quickstart.md`, validar APK/JVM/lint, ejecutar regresion fisica e instalar conservando datos.

Evidencia T066-T067: compilacion de ambos APK, JVM y lint correctos. En Xiaomi,
23 MainActivityTest ejecutados: 22 correctos, uno fallo en cierre de ActivityScenario
esperando DESTROYED; repeticion aislada del caso de siguiente placa correcta.
Actualizacion install -r -t sin borrar datos. Sin escrituras API ni envios reales.

## Formato visual del mensaje manual 2026-11-06

- [X] T068 [US1] Mejorar encabezado, iconos, secciones y total destacado en `app/src/main/res/values/strings.xml` y formatear servicios desde recursos en `app/src/main/java/com/carwash/mobile/ingreso/IngresoScreen.kt`, sin cambiar datos ni importes.
- [X] T069 [US2] Verificar contenido y codificacion Unicode en `app/src/androidTest/java/com/carwash/mobile/MainActivityTest.kt`, actualizar `specs/004-confirmacion-whatsapp/contracts/api.md` y `specs/004-confirmacion-whatsapp/quickstart.md`, validar JVM/APK/lint e instalar conservando datos.

Evidencia T068-T069: dos pruebas enfocadas correctas en Xiaomi fisico; formato
completo del Intent, emojis, negrita, servicios y total editado, codificacion y
alternativa Business. assembleDebug/assembleDebugAndroidTest/JVM/lintDebug
correctos. APK instalado con install -r -t. Sin mensajes reales; apariencia de
burbuja enviada no verificada en esta revision. Apertura y limpieza conservadas.

## Formato visual del correo de recepcion 2026-11-06

- [X] T070 [US1] Mejorar BuildBody en `../CarWashApi/Services/EmailNotificationService.cs` con HTML escapado, iconos, tabla de servicios, total confirmado es-CO y alternativa de texto; conservar idempotencia y aviso de aprobacion.
- [X] T071 [US1] Agregar EmailBody compatible con la firma anterior en `../CarWashApi/Services/IEmailClient.cs` y MIME alternativo UTF-8 en `../CarWashApi/Services/SmtpEmailClient.cs`, sin cambiar configuracion SMTP.
- [X] T072 [US2] Validar contenido, importes, escape HTML, idempotencia, compatibilidad de texto y MIME SMTP loopback en `../CarWashApi.Tests/Services/EmailNotificationServiceTests.cs`; documentar evidencia y despliegue pendiente en contrato y quickstart.

Evidencia T070-T072: cuatro casos xUnit correctos y compilacion API/pruebas
correcta con artifacts-path temporal; SQLite temporal y SMTP loopback sin
credenciales ni envios externos. Verificados HTML escapado, total confirmado
distinto de suma de catalogo, es-CO, idempotencia, alternativa de texto y MIME
UTF-8 con/sin HTML. API operativa no reemplazada/reiniciada; despliegue y
apariencia final en buzon real pendientes de autorizacion. Sin cambios Android,
emulador ni pruebas fisicas innecesarias para esta revision exclusiva de API.

## Despliegue autorizado del correo HTML 2026-11-06

- [X] T073 [US2] Con autorizacion explicita del usuario, repetir las cuatro pruebas de correo, publicar API en directorio temporal, respaldar binario previo, desplegar conservando configuracion y reiniciar perfil wifi; verificar SHA-256 e HTTP 200 local y por la IP usada por el celular.

Evidencia T073: cuatro casos xUnit correctos y dotnet publish correcto. Copia
previa before-email-html-20261106-160237 en LOCALAPPDATA/CarWash/ApiBackups,
fuera de Git. Detenido solo PID 30884 identificado por su ejecutable; proceso
nuevo PID 23956, inicio 16:03:55, bin/Debug/net9.0/Carwash.API.exe. Hash del DLL
desplegado igual a publicacion: E3D38D4B03EAA46DCC5C75AFC228A74E1E229F39B49BB91ECB2270C096DC3CCF.
Archivos appsettings del destino conservados y verificados por hash; mismo
content root y perfil wifi. Swagger HTTP 200 en 127.0.0.1:5197 y 192.168.1.5:5197.
No se ejecutaron endpoints de escritura ni de envio, ni se alteraron turnos/pagos.
API nueva permanece operativa; apariencia en buzon real pendiente.
