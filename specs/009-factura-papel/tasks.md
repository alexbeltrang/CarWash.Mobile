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

## Phase 9: Estilo Y Menu Escritorio

- [X] T021 Reutilizar estilo de Apertura/Cierre caja en Empresa, Configuracion facturacion, Facturas, Retiro y Reporte diario mediante UIHelper.
- [X] T022 Agrupar opciones nuevas en Administracion conservando permisos existentes.
- [X] T023 Compilar y probar formularios/menus con API aislada, capturas y tamanos reducidos; 20 pruebas enfocadas aprobadas y capturas minimas inspeccionadas.
- [X] T024 Actualizar escritorio con nueva autorizacion y respaldo before-estilo-caja-20261109-105129; PID12484 responde, configuracion/API conservadas y celular sin cambios.

## Phase 10: Disenador WinForms

- [X] T025 Convertir cinco formularios a partial/Designer.cs/resx; controles/estilo declarativos y eventos separados sin API en diseno.
- [X] T026 Declarar entradas nuevas del menu principal en Designer.cs y eliminar iconos de opciones.
- [X] T027 Compilar y probar carga de diseno, edicion/serializacion, aspecto, menus/permisos y operaciones aisladas; 20 ejecutadas/20 aprobadas.
- [X] T028 Actualizar escritorio con nueva aprobacion del usuario y respaldo before-disenador-x64-20261109-120330, sin reiniciar API ni cambiar celular; completado junto con T031.

## Phase 11: Barras Y Compilacion SQLite

- [X] T029 Eliminar barras duplicadas de Facturas/Reporte diario y probar una barra nativa por eje y desplazamiento de filas.
- [X] T030 Corregir arquitectura x64 y copia nativa en proyecto/solucion; cuatro compilaciones Debug/Release sin parametros Platform/RID y ejecutable/SQLite AMD64 verificados.
- [X] T031 Reemplazar escritorio con autorizacion del usuario 2026-11-09 y respaldo; 18 archivos verificados, appSettings conservados, PID61936 respondio y luego se cerro sin fallos recientes registrados; API29316 sin reinicio y sin datos operativos de prueba.

## Phase 12: Carga Del Disenador x64

- [X] T032 Activar UseWinFormsOutOfProcDesigner para .NET Framework x64 sin cambiar formularios editados por el usuario.
- [X] T033 Verificar propiedad activa, resolucion de referencias de diseno y solucion Debug con MSBuild de Visual Studio Professional18.8; FxDesignToolsServer x64 instalado. Apertura manual pendiente de recargar proyecto en Visual Studio.

## Phase 13: Instalador Seleccionable

- [X] T034 Integrar proyecto generador y compilacion aislada de escritorio/API; proyecto Release genera setup y solucion Debug no empaqueta.
- [X] T035 Incluir runtime offline verificado y esquema sin datos reales; firmas Microsoft y manifiesto SHA256 comprobados.
- [X] T036 Agregar seleccion escritorio/API/base nueva, configuracion y usuario sin clave predeterminada; rechazar base incompatible en solo lectura.
- [X] T037 Probar paquete, seleccion/configuracion/base en temporal y documentar alcance pendiente en Windows limpio; API autocontenida autentica administrador sintetico y probe compilado aprobado.
- [ ] T038 [Validacion externa] Instalar/actualizar/desinstalar en VM Windows limpia con UAC y reinicio; comprobar tarea SYSTEM, firewall, permisos, conservacion al actualizar y borrado completo confirmado al desinstalar. No ejecutar sobre produccion como prueba.
- [X] T039 Corregir colision de variable XML/JSON en instalacion conjunta escritorio/API, probar flujo completo con base existente/nueva en PowerShell7 y Windows PowerShell5.1 con tareas/firewall simulados y generar instalador1.0.1.
- [X] T040 Implementar FR019 en instalador1.0.2: metadatos sin secretos, confirmacion de perdida, limpieza de archivos/datos/perfiles, rechazo de enlaces/otra instalacion y pruebas temporales con tareas/procesos/firewall simulados en Windows PowerShell5.1; compilar paquete sin ejecutar borrado real.
- [X] T041 Reutilizar instalacion del usuario inicial del API para base nueva/existente sin usuarios, conservar poblada, mostrar credenciales al seleccionar API y mantener tarea/Kestrel sin IIS. Tres recorridos conjuntos y login de base existente limpia aprobados en Windows PowerShell5.1; generar1.0.3 sin alterar API operativa.
- [X] T042 Implementar FR021: selector de carpeta externa dedicada, motor SQLite BackupDatabase horario con integridad/SHA256/retencion24-7-4/estado/bloqueo, tarea SYSTEM y retiro conservando copias; pruebas temporales WAL/restauracion/fallos y PowerShell5.1 con tareas/firewall simulados. Registro real SYSTEM/ACL/reinicio siguen pendientes en T038.
- [X] T043 Sustituir retencion24/7/4 de FR021 por todas las copias de hoy y la ultima por cada uno de los 30 dias calendario anteriores (hora local). Prueba temporal con duplicados en 45 dias, extremos de dia local, limite inclusivo30/exclusion31, copias de hoy y archivos ajenos conservados; no ejecutar limpieza operativa.