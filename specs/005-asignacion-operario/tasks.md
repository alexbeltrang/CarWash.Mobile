# Ajuste De Sesion 2026-11-06

- [X] SES-001 Restaurar sesion cifrada y plazo original en SessionStore/LoginViewModel.
- [X] SES-002 Verificar restauracion, expiracion, logout y cifrado en JVM/celular fisico; compilar APK y lint.

Evidencia 2026-11-06: JVM/assembleDebug/assembleDebugAndroidTest/lintDebug
correctos. Xiaomi fisico: dos pruebas nuevas de Android Keystore/restauracion
y rechazo de contenido alterado correctas; prueba antigua de contrasena fallo
al arrancar Compose y paso aislada sin cambios. APK respaldado e instalado -r -t,
MainActivity Status:ok. Sin credenciales reales ni escrituras API en pruebas.
Acceso una vez tras actualizar: version antigua no tenia token recuperable.

---
description: "Tareas de implementación de asignación de operario desde el móvil"
---

# Tasks: Asignación de operario desde el móvil

**Input**: Design documents from `specs/005-asignacion-operario/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/api.md`, `quickstart.md`

**Organization**: Las tareas están agrupadas por historia (US1 asignar, US2 consultar, US3 reasignar). Las pruebas se incluyen porque la constitución exige pruebas del comportamiento modificado. Rutas de la app relativas a `C:\Work\CarWash.Mobile`; las del API en `C:\Work\CarWashApi`.

## Phase 1: Setup

- [X] T001 Revisar en `C:\Work\CarWash\` cómo el WinForm fija y libera `OperadorOcupado` y `FechaHoraAsignacionOperario` al asignar y finalizar un turno, y anotar la convención en `specs/005-asignacion-operario/research.md` (D7)
- [X] T002 [P] Verificar si RC2-CBC (clave "C4rW4sh", IV "20270430", Base64) funciona en Android API 26 con la plataforma o con BouncyCastle y registrar la decisión en `specs/005-asignacion-operario/research.md` (D1); si no es viable, detenerse y consultar al usuario sobre un login alterno en el API
- [X] T003 [P] Actualizar `.specify/memory/constitution.md` a 1.2.0 para permitir el inicio de sesión con la API existente (sin persistir credenciales) y registrar el impacto en su Sync Impact Report

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Inicio de sesión y base del API que necesitan todas las historias.

- [X] T004 [P] Crear `app/src/main/java/com/carwash/mobile/auth/CredentialCipher.kt` con el cifrado compatible con `FunctionsEncrip.Cifrado` y su prueba `app/src/test/java/com/carwash/mobile/auth/CredentialCipherTest.kt` con un vector real generado por el instalador o el sembrador
- [X] T005 [P] Crear `app/src/main/java/com/carwash/mobile/auth/AuthRepository.kt` (Retrofit `POST api/Auth/login`, modelos `LoginResponse` y `Usuario`) y `app/src/test/java/com/carwash/mobile/auth/AuthRepositoryTest.kt` con MockWebServer
- [X] T006 Crear `app/src/main/java/com/carwash/mobile/auth/SessionState.kt` y `LoginViewModel.kt` con la sesión solo en memoria (`IdUser`, `DisplayName`, `PerfilId`, `esAdministrador`), sin persistir contraseña (FR-012), y `app/src/test/java/com/carwash/mobile/auth/LoginViewModelTest.kt`
- [X] T007 Crear `app/src/main/java/com/carwash/mobile/auth/LoginScreen.kt` con textos en `app/src/main/res/values/strings.xml`, error genérico ante credenciales inválidas y reintento; integrarla en `app/src/main/java/com/carwash/mobile/MainActivity.kt` antes de la pantalla de ingreso, reutilizando el diálogo de conexión existente
- [X] T008 Corregir `C:\Work\CarWashApi\Controllers\TurnosController.cs` (`VehiculosEnProceso`): devolver `idOperario` y tolerar `OperadorOcupado` nulo; agregar `idOperario` a `C:\Work\CarWashApi\DTOs\GestionVehiculosDTO.cs` ya existente sin romper al WinForm

**Checkpoint**: se puede iniciar sesión y el API entrega la lista con `idOperario`.

---

## Phase 3: User Story 1 - Asignar operario (Priority: P1) 🎯 MVP

**Goal**: El administrador asigna un operario libre a un turno en proceso sin operario.

**Independent Test**: Escenarios 1, 2, 3 y 4 de `quickstart.md`.

- [X] T009 [US1] Crear `AsignarOperarioRequest` en `C:\Work\CarWashApi\DTOs\AsignarOperarioRequest.cs` y el endpoint `PUT api/Turnos/{id}/Operario` en `C:\Work\CarWashApi\Controllers\TurnosController.cs` con las reglas y códigos de `contracts/api.md` (204, 400, 403, 404, 409), en una transacción, estableciendo `idOperario`, `FechaHoraAsignacionOperario` y `OperadorOcupado` según la convención de T001
- [X] T010 [US1] Probar el endpoint contra una copia temporal de la base (no la real): asignación correcta, turno cerrado, operario ocupado, usuario no administrador, idempotencia y concurrencia; ubicar la prueba en el proyecto de pruebas del API o documentar el script usado
- [X] T011 [P] [US1] Ampliar `app/src/main/java/com/carwash/mobile/asignacion/AsignacionRepository.kt` con `operadoresDisponibles()` y `asignar(idTurno, idOperario, idUsuario)` y su prueba `app/src/test/java/com/carwash/mobile/asignacion/AsignacionRepositoryTest.kt` (204, 403, 404, 409 y red)
- [X] T012 [US1] Crear `app/src/main/java/com/carwash/mobile/asignacion/AsignacionViewModel.kt` con selección de operario, estado vacío sin operarios, conflicto 409 con refresco, y ausencia de éxito sin 2xx; pruebas en `app/src/test/java/com/carwash/mobile/asignacion/AsignacionViewModelTest.kt`
- [X] T013 [US1] Crear `app/src/main/java/com/carwash/mobile/asignacion/AsignacionScreen.kt` (diálogo de selección y confirmación, botón deshabilitado sin operarios) con textos en `strings.xml`; mostrar la acción solo si `esAdministrador`
- [X] T014 [US1] Agregar pruebas Compose en `app/src/androidTest/java/com/carwash/mobile/AsignacionScreenTest.kt` con repositorio simulado (sin escribir en la base real)

**Checkpoint**: MVP, el administrador asigna desde el móvil y el WinForm lo ve.

---

## Phase 4: User Story 2 - Consultar vehículos en proceso (Priority: P2)

**Goal**: Administrador y recepcionista ven la lista con operario o "sin asignar".

**Independent Test**: Escenarios 1, 6 y 8 de `quickstart.md`.

- [X] T015 [P] [US2] Crear `app/src/main/java/com/carwash/mobile/asignacion/VehiculosEnProcesoRepository.kt` (`GET api/Turnos/VehiculosEnProceso`) y su prueba con MockWebServer
- [X] T016 [US2] Crear `app/src/main/java/com/carwash/mobile/asignacion/VehiculosViewModel.kt` con estados cargando, vacío, error recuperable y reintento, más `VehiculosViewModelTest.kt`
- [X] T017 [US2] Crear `app/src/main/java/com/carwash/mobile/asignacion/VehiculosScreen.kt` mostrando placa, tipo, hora de ingreso y operario o "sin asignar"; navegación desde `MainActivity.kt` para todos los perfiles, con acción de asignar solo para administrador; textos en `strings.xml`
- [X] T018 [US2] Prueba Compose `app/src/androidTest/java/com/carwash/mobile/VehiculosScreenTest.kt` (lista, vacío, error, recepcionista sin acción de asignar)

---

## Phase 5: User Story 3 - Reasignar (Priority: P3)

**Goal**: El administrador cambia el operario de un turno en proceso.

**Independent Test**: Escenario 5 de `quickstart.md`.

- [X] T019 [US3] Extender el endpoint de T009 en `C:\Work\CarWashApi\Controllers\TurnosController.cs` para liberar al operario anterior al reasignar y validar que el nuevo esté libre; ampliar la prueba de T010
- [X] T020 [US3] Permitir reasignar en `AsignacionViewModel.kt` y `VehiculosScreen.kt` sobre turnos ya asignados en proceso, con pruebas en `AsignacionViewModelTest.kt`

---

## Phase 6: Polish & Cross-Cutting

- [X] T021 [P] Verificar que no se registren cuerpos HTTP ni credenciales en logs y que no se persista sesión (revisar `AndroidServerSettings` y el almacenamiento local)
- [X] T022 [P] Actualizar `README.md`, `docs/instalacion-api.md` (nuevo endpoint y su versión del API) y regenerar el instalador con `installer\build.ps1` para incluir el endpoint nuevo
- [X] T023 Ejecutar `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` y `:app:connectedDebugAndroidTest` con el teléfono conectado, y registrar la evidencia real en `specs/005-asignacion-operario/quickstart.md` (documentar como bloqueo lo no ejecutado)
- [X] T024 Recorrer los escenarios de `quickstart.md` contra una copia de la base y marcar los resultados

---

## Dependencies & Execution Order

- Setup (T001-T003) antes de Foundational; T002 puede cancelar el enfoque de login.
- Foundational (T004-T008) bloquea todas las historias; T004 → T005 → T006 → T007; T008 es independiente.
- US1 depende de T001 y de Foundational; US2 solo de Foundational; US3 depende de US1.
- Polish al final.

## Parallel Opportunities

- T002 y T003 en paralelo; T004 y T005 en paralelo; T008 en paralelo con T004-T007.
- T011 en paralelo con T009-T010; T015 en paralelo con T011.
- US1 y US2 pueden avanzar en paralelo tras Foundational (T013 y T017 comparten `MainActivity.kt`: coordinar).

## Implementation Strategy

1. Entregar MVP: Setup + Foundational + US1; validar escenarios 1-4.
2. Agregar US2 y validar; luego US3.
3. Ejecutar Polish y registrar evidencia real.
