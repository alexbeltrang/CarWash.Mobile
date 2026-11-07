# Tasks: Empresa y recibos

**Input**: spec.md, plan.md, research.md, data-model.md y contracts/empresa.md.

## Phase 1: Setup

- [X] T001 Definir alcance sin electronica y diseno en `specs/008-empresa-facturacion/`.

## Phase 2: Foundation

- [X] T002 Probar contrato, permisos, revision y logo en `../CarWashApi.Tests/EmpresaTests.cs`.
- [X] T003 Agregar ficha unica y API en `../CarWashApi/{Modelos/Empresa.cs,DTOs/EmpresaDto.cs,Services/EmpresaService.cs,Controllers/EmpresaController.cs,Context/AppDbContext.cs,Program.cs}`.

## Phase 3: US1 Administrar empresa

Prueba independiente: administrador guarda/modifica, operativo no puede; conflicto no sobrescribe.

- [X] T004 [US1] Implementar cliente y formulario de empresa en `../CarWash/{DTOs/EmpresaDTO.cs,Controladores/EmpresaController.cs,Presentacion/Administracion/FrmEmpresa.cs}` y menu en `Presentacion/Principal/FrmPrincipal.cs`.
- [X] T005 [US1] Probar formulario/cliente compilados en `../CarWashApi.Tests/` y compilar `../CarWash/CarWash.csproj` con salida aislada.

## Phase 4: US2 Identidad en aplicaciones

Prueba independiente: ambos clientes muestran nombre/logo actuales; Android no edita.

- [X] T006 [US2] Agregar lectura e identidad compartida en `app/src/main/java/com/carwash/mobile/empresa/` e integrar `MainActivity.kt` y encabezados existentes.
- [X] T007 [US2] Probar identidad y fallos en pruebas JVM/Compose bajo `app/src/{test,androidTest}/`.

## Phase 5: US3 Recibos y comunicaciones

Prueba independiente: recibo/logo legibles y snapshot anterior intacto tras editar empresa.

- [X] T008 [US3] Integrar empresa y snapshot en `../CarWashApi/Services/{ReceiptPrintLayout.cs,ImpresionComprobantesService.cs,WindowsReceiptPrinter.cs,EmailNotificationService.cs}` y comunicaciones manuales Android/escritorio.
- [X] T009 [US3] Probar contenido, logo, snapshot y ancho 80 mm en `../CarWashApi.Tests/EmpresaTests.cs` y pruebas existentes de impresion/correo.

## Phase 6: Validation

- [X] T010 Ejecutar API/MSBuild/Gradle/lint y Compose en celular fisico; registrar resultados en `specs/008-empresa-facturacion/quickstart.md`.
- [X] T011 Contrastar implementacion con contratos/especificacion y cerrar tareas en `specs/008-empresa-facturacion/`.
- [X] T012 Desplegar con autorizacion de reinicio y respaldo, conservando datos/configuracion; documentar en `specs/008-empresa-facturacion/quickstart.md`.

## Dependencies

T001 -> T002 -> T003 -> US1/US2/US3 -> T010 -> T011 -> T012.
Sin delegacion. Clientes independientes tras contrato; cambios de impresion se
validan antes de comunicaciones. MVP ficha empresarial no sustituye alcance completo.

## Implementation Strategy

Pruebas acotadas antes del codigo; incrementar API, escritorio, Android y
documentos con validacion por tramo. No facturacion, papel ni mensajes reales.