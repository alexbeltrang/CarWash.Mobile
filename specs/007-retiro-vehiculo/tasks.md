# Tasks: Comprobante y retiro del vehiculo

## Phase 1: Setup

- [X] T001 Especificar aclaraciones, plan y contratos en `specs/007-retiro-vehiculo/`.

## Phase 2: Foundation

- [X] T002 Agregar tabla/modelo e indices de comprobante en `../CarWashApi/Context/AppDbContext.cs` y `../CarWashApi/Program.cs`.

## Phase 3: US1 Comprobante digital

Prueba independiente: codigo estable, servicios y total confirmado en comprobante.

- [X] T003 [US1] Probar emision/consulta y sesion en `../CarWashApi.Tests/RetiroVehiculosTests.cs`.
- [X] T004 [US1] Implementar servicio y rutas en `../CarWashApi/Services/RetiroVehiculosService.cs` y `../CarWashApi/Controllers/RetiroVehiculosController.cs`.
- [X] T005 [US1] Integrar codigo en `../CarWashApi/Services/EmailNotificationService.cs`, `app/src/main/java/com/carwash/mobile/ingreso/IngresoViewModel.kt` y recursos de WhatsApp.

## Phase 4: US2 Impresion

Prueba independiente: vista previa, papel y cancelacion no entregan ni regeneran.

- [X] T006 [US2] Implementar cliente y formulario con vista previa local e impresion central en `../CarWash/Controladores/RetiroVehiculosController.cs` y `../CarWash/Presentacion/Operacion/FrmRetiroVehiculo.cs`.
- [X] T007 [US2] Integrar acceso al formulario y compilar `../CarWash/CarWash.csproj` con salida aislada.

## Phase 5: US3 Retiro

Prueba independiente: codigo/revision correctos, una sola entrega y pagos intactos.

- [X] T008 [US3] Probar errores, concurrencia, entrega unica y pagos en `../CarWashApi.Tests/RetiroVehiculosTests.cs`.
- [X] T009 [US3] Implementar consulta y confirmacion Android en `app/src/main/java/com/carwash/mobile/retiro/` y navegacion en `app/src/main/java/com/carwash/mobile/MainActivity.kt`.
- [X] T010 [US3] Verificar flujo y errores en pruebas JVM/Compose existentes bajo `app/src/test/` y `app/src/androidTest/`.

## Phase 6: Validation

- [X] T011 Ejecutar API, MSBuild, Gradle/lint y pruebas en celular fisico; registrar evidencia y bloqueos en `specs/007-retiro-vehiculo/quickstart.md`.
- [X] T012 Contrastar implementacion y contrato, completar tareas/documentacion en `specs/007-retiro-vehiculo/` sin desplegar servicio ni imprimir/enviar en real sin autorizacion.
- [ ] T013 [US2] Validar salida de papel en la termica USB instalada con autorizacion para imprimir; registrar ancho/controlador y legibilidad en `specs/007-retiro-vehiculo/quickstart.md`. Bloqueo externo: no hay termica instalada; HP USB si validada fisicamente.

## Revision central de impresion

- [X] T014 [US2] Implementar formato, cola persistida, rutas autenticadas e idempotencia en API; probar con impresora simulada.
- [X] T015 [US2] Implementar trabajador Windows y prueba USB optativa, sin envios fisicos en pruebas normales.
- [X] T016 [US2] Conectar botones Imprimir/Consultar impresion en Android y escritorio con UUID estable ante respuesta incierta.
- [X] T017 [US2] Ejecutar una sola hoja diagnostica autorizada en HP USB001; verificar salida con el usuario, sin turnos ficticios reales.
- [X] T018 Validar API, escritorio, JVM/lint y UI en celular fisico; documentar configuracion y limites del servidor de impresion.
- [X] T019 Desplegar impresion central con respaldo y autorizacion especifica de reinicio, conservando configuraciones y datos.

## Dependencies

## Revision vigente: documento automatico y entrega sin codigo

- [X] T020 [US2] Agregar solicitud automatica idempotente de 80 mm al cierre de ingreso en ambos clientes; probar duplicados y fallo independiente del guardado.
- [X] T021 [US2] Imprimir documento sin codigo ni QR con formato de papel 80 mm y comprobar ancho/paginacion sin impresora fisica.
- [X] T022 [US3] Eliminar requisito/campo de codigo y escaneo; registrar entrega fisica revisada desde ambos clientes, probar revision obligatoria y entrega unica.
- [X] T023 Ejecutar gates API/escritorio/Android, pruebas en celular fisico y una hoja HP autorizada; documentar evidencia y bloqueos.
- [X] T024 Actualizar contrato, modelo y plan; desplegar solo con autorizacion de reinicio y respaldo.
- [X] T025 Ocultar aviso rutinario de impresion en ingreso Android, conservar errores e impresion automatica; probar en celular fisico y actualizar APK.
- [X] T026 Sustituir Retiro por Reimpresion del recibo en Android; retirar entrega, probar navegacion/copias en celular fisico y actualizar APK conservando datos.

T001 -> T002 -> US1 (T003-T005). US2 y US3 dependen del contrato US1.
T008 valida entrega antes de T009. T011-T012 despues de integraciones.

## Parallel Opportunities

Sin delegacion; clientes Android y escritorio son archivos independientes despues
del contrato/API. Ejemplos: T006 con T009; pruebas T003 con formulario T006.

## Implementation Strategy

Incrementos verificados: API con pruebas aisladas, digital/Android, escritorio e
impresion, luego gates y evidencia. MVP US1 no sustituye la entrega total solicitada.