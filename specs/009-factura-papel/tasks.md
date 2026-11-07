# Tasks: Factura de venta en papel

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/factura.md.

## Phase 1: Setup

- [X] T001 Definir alcance confirmado y diseno en `specs/009-factura-papel/`.

## Phase 2: Foundation

- [X] T002 Probar configuracion y emision aisladas en `../CarWashApi.Tests/FacturacionTests.cs`.
- [X] T003 Implementar entidades/DTO/API y transaccion en `../CarWashApi/{Modelos/Factura.cs,DTOs/FacturaDto.cs,Services/FacturacionService.cs,Controllers/FacturasController.cs,Context/AppDbContext.cs,Program.cs}`.

## Phase 3: US1 Configurar Facturacion

Prueba independiente: administrador guarda y operativo no; revision/rango no sobrescriben.

- [X] T004 [US1] Agregar cliente/DTO y editor fiscal en `../CarWash/{DTOs/FacturaDTO.cs,Controladores/FacturasController.cs,Presentacion/Administracion/FrmFacturacion.cs}` y menu `Presentacion/Principal/FrmPrincipal.cs`.
- [X] T005 [US1] Probar formulario/configuracion compilados en `../CarWashApi.Tests/Fixtures/DesktopSessionProbe.cs`.

## Phase 4: US2 Emitir Al Cobrar

Prueba independiente: turno finalizado produce factura unica, sin cambios de pago.

- [X] T006 [US2] Agregar preparacion/comprador/emision en `../CarWash/Presentacion/Operacion/FrmFactura.cs`.
- [X] T007 [US2] Abrir factura tras cobro final confirmado en `../CarWash/Presentacion/Operacion/FrmGestionDetalleVehiculo.cs`, sin repetir pago ante error fiscal.

## Phase 5: US3 Consultar Y Reimprimir

Prueba independiente: factura/logo originales despues de editar empresa/configuracion.

- [X] T008 [US3] Integrar cola de snapshot y consulta/impresion en `../CarWashApi/Services/ImpresionComprobantesService.cs`, `FacturacionService.cs`, `FacturasController.cs` y `../CarWash/Presentacion/Operacion/FrmFactura.cs`.
- [X] T009 [US3] Probar snapshots, cola falsa y layout 80 mm en `../CarWashApi.Tests/FacturacionTests.cs` y probe compilado existente.

## Phase 6: Verification

- [X] T010 Ejecutar suite API/MSBuild y registrar evidencia en `specs/009-factura-papel/quickstart.md`.
- [X] T011 Desplegar con autorizacion nueva/respaldo conservando configuracion/datos y documentar en `specs/009-factura-papel/quickstart.md`.
- [X] T012 Contrastar especificacion/contrato/codigo y cerrar tareas en `specs/009-factura-papel/tasks.md`.

## Dependencies

T001 -> T002 -> T003 -> US1 -> US2 -> US3 -> T010 -> T011/T012.
Pruebas solicitadas en FR011 y constitucion. Sin delegacion ni ramas/commits.
Clientes/configuracion y emision usan mismos DTO; validar cada tramo antes del siguiente.

## Implementation Strategy

Primero configuracion/API con tests rojos; despues editor y emision, luego
impresion/historial y gates. MVP US1 no se confunde con facturacion completa.
Trabajo independiente posible: pruebas de render despues del contrato mientras
se escribe editor; se ejecuta secuencialmente en esta sesion para mantener validacion.

## Phase 7: Factura Opcional Al Cobrar

- [X] T013 [US1] Retirar preferencia automatica sustituida por ultima indicacion del usuario; mantener habilitacion fiscal existente.
- [X] T014 [US2] Preguntar tras cobro completo confirmado con No predeterminado; No cierra sin factura, Si abre datos sin segunda confirmacion para ese turno; direccion opcional.
- [X] T015 [US3] Probar respuestas Si/No/cancelacion, comprador requerido sin direccion y emision/impresion explicita aisladas.
- [X] T016 Ejecutar gates API/escritorio y documentar evidencia: MSBuild aislado y 20 pruebas enfocadas aprobadas.
- [X] T017 Actualizar solo escritorio con respaldo y aprobacion nueva; no reiniciar API ni modificar celular. PID61156 responde; configuracion conservada.

## Phase 8: Scripts De Creacion De Base

- [X] T018 Obtener esquema actual en solo lectura y entregar script SQLite sin datos en docs/database/carwash-sqlite.sql.
- [X] T019 Entregar script equivalente SQL Server con tipos/identidades/indices compatibles en docs/database/carwash-sqlserver.sql.
- [X] T020 Verificar SQLite temporal y SQL Server temporal (30 tablas/27 indices), rechazo de base existente y documentar uso/limitaciones sin tocar produccion en docs/database/README.md.