# Implementation Plan: Reporte de ventas por fechas

**Branch**: `main` (rama real; setup-plan devuelve `006-reporte-ventas` como contexto de funcionalidad, sin crear ni cambiar una rama) | **Date**: 2026-11-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-reporte-ventas/spec.md`.

**Status**: Implementacion y gates T001-T060 completados en aislamiento, incluida revision de sesion general de 9 horas y combos; evidencia en quickstart. Sin despliegue ni auditoria global.

## Summary

Consultar cobros de TurnosMovimientos por FechaMovimiento y sumar MontoPagado,
con fechas obligatorias y forma de pago, operario, placa y tipo de vehiculo
opcionales. Agregar Reporte de ventas a la navegacion de la app para el perfil
Administrador, manteniendo ingreso y vehiculos en proceso.

La consulta historica existente no satisface el contrato: filtra por fecha de
ingreso, usa joins que pueden excluir movimientos y no valida identidad.
Se disena una consulta separada para preservar el contrato historico de escritorio.
El usuario autorizo ampliar el login con sesion temporal verificable por el servidor
y contemplar compatibilidad de escritorio; spec.md registra esa autorizacion y FR-017.
Proteger tambien todas las lecturas heredadas de TurnosMovimientos para no dejar
un acceso alterno anonimo a los mismos cobros.

## Technical Context

**Language/Version**: Kotlin 2.1.20/JVM 17, AGP 8.9.2; C#/.NET 9 en API.

**Primary Dependencies**: Jetpack Compose, Material 3, ViewModel, coroutines, Retrofit 2.11.0, Gson 2.13.1; ASP.NET Core 9 y EF Core SQLite 9.0.14. Esquema de autenticacion ASP.NET Core con registro opaco en memoria; sin proveedor de identidad nuevo.

**Storage**: SQLite existente en API, lectura de cobros; resultados y filtros en memoria movil, sin persistir datos de ventas ni credenciales.

**Testing**: JUnit 4.13.2, MockWebServer 4.12.0, coroutines-test 1.10.2 y pruebas Compose; pruebas de integracion API contra SQLite temporal, nunca contra datos reales.

**Target Platform**: Android API 26 minimo, compile/target SDK 35; API existente en Windows.

**Project Type**: App Android nativa y ampliacion de consulta en API existente, sin cambios de cobro en escritorio.

**Performance Goals**: Al menos 19 de 20 consultas de hasta 1000 cobros muestran resumen y primer detalle en menos de 5 segundos en conexion local estable.

**Constraints**: Solo Administrador validado en servidor; no persistir tokens/contrasenas; no modificar escrituras de negocio. Historico de escritorio conserva parametros, respuesta y fechas, pero exige sesion. No hay bypass anonimo para clientes viejos. HTTPS recomendado; HTTP privado conserva el riesgo de escucha en LAN.

**Scale/Scope**: Un reporte, cuatro filtros opcionales, totales del conjunto completo y detalle paginado sin limite arbitrario de fechas.

**Authentication gate**: Resuelto con autorizacion explicita del usuario. Token opaco de 32 bytes aleatorios, hash en memoria de API, token solo en memoria de clientes y perfil vigente comprobado en cada lectura. Una sesion general por login, con cierre absoluto a las 9 horas, sin vencimiento por inactividad ni renovacion por uso. No hay login exclusivo de Ventas ni cierre simultaneo de otros usuarios; logout confirmado y reinicio API invalidan sesiones. Tras muerte del proceso se requiere login general.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Antes de Phase 0 | Resultado de investigacion |
|------|-----------------|----------------------------|
| I. Especificacion antes de funcionalidad | PASS: spec 006 y checklist completos | PASS: autorizacion registrada y FR-017 cubre sesion temporal y escritorio |
| II. Android nativo y simplicidad | PASS: reutilizar dependencias y arquitectura locales | Sin necesidad de frameworks de reportes ni nueva base de datos |
| III. Calidad verificable | PASS: planificar pruebas por comportamiento | No se han ejecutado pruebas de una funcion que aun no existe |
| IV. Privacidad y seguridad | Bloqueo inicial de login sin credencial | PASS de diseno: ampliacion autorizada, token no persistido, revocacion y politica en todas las lecturas de movimientos, sin nuevas reglas de cobro |
| V. Experiencia accesible | PASS: controles Material 3 y recursos existentes | Validar orientacion, tema y fuente durante implementacion |

**Decision de gate**: PASS despues de autorizar y especificar la ampliacion. No se
modifica la constitucion: permite autenticacion especificada; la sesion recordada de
005 sigue siendo metadato local sin credencial, y el reporte requiere reingreso si
el token temporal no existe. No se afirma corregir la seguridad global de la API.

**Post-design check**: PASS documental. Modelo y contratos separan login aditivo,
lecturas protegidas, precision monetaria, controles de UI y compatibilidad. Gates
de ejecucion y metricas reales quedan para implementacion, no se declaran exitosos.

## Project Structure

### Documentation (this feature)

```text
specs/006-reporte-ventas/
  spec.md
  checklists/requirements.md
  plan.md
  research.md
  data-model.md
  contracts/api.md
  contracts/ui.md
  quickstart.md
```

`tasks.md` corresponde a `/speckit-tasks`, no a esta fase.

### Source Code (repository root)

```text
app/src/main/java/com/carwash/mobile/
  MainActivity.kt
  auth/
  ingreso/
  asignacion/
  reportes/                    (propuesto: modelos, repositorio, ViewModel y pantalla)
app/src/main/res/values/strings.xml
app/src/test/java/com/carwash/mobile/
app/src/androidTest/java/com/carwash/mobile/
```

**Structure Decision**: mantener el modulo `app` y agregar un paquete de reporte
con modelos, repositorio, ViewModel y pantalla. Backend externo inspeccionado:
repositorio hermano `../CarWashApi`, con `Controllers/AuthController.cs`,
`Controllers/TurnosMovimientosController.cs`, `Modelos/TurnosMovimientos.cs` y
`Program.cs`. Se reutilizo `../CarWashApi.Tests/Carwash.API.Tests.csproj` como proyecto
hermano de pruebas HTTP/SQLite, sin fuentes de test recursivas en el proyecto web.
Escritorio externo: `../CarWash`, login, controlador de movimientos y sus dos
pantallas de consulta. La fase de diseno solo modifico documentos; implementacion
coordinada incluyo API, escritorio y pruebas hermanas conforme al alcance autorizado.

## Phase 0 - Research Complete

[research.md](research.md) cierra fuente y escritura real de cobros, DateTime local,
traduccion decimal ef_sum, login sin credencial, compatibilidad, paginacion y estados.
Las decisiones se basan en lecturas de codigo y documentacion oficial; no en pruebas
ejecutadas. No quedan decisiones de negocio pendientes.

## Phase 1 - Design

- [data-model.md](data-model.md): movimientos unicos, relaciones opcionales, criterios,
  DTO monetario, pagina y sesion efimera; no crear tablas ni migraciones de negocio.
- [contracts/api.md](contracts/api.md): login aditivo, logout, politica de lecturas,
  GET de reporte/filtros, errores, importes decimales invariantes y despliegue.
- [contracts/ui.md](contracts/ui.md): navegacion por destino, filtros, controles,
  resultados, manejo de sesion, accesibilidad y alcance de escritorio.
- [quickstart.md](quickstart.md): comprobaciones ejecutables tras implementar,
  datos sinteticos, seguridad, regresion y criterios de rendimiento.

### Secuencia de implementacion para derivar tareas

1. Preparar pruebas API aisladas antes de arrancar la API: su Program actual ejecuta
   DDL al inicio y debe usar exclusivamente SQLite temporal en pruebas.
2. Agregar registro de sesiones, esquema de autenticacion, politica y login/logout
   aditivos; probar token invalido, caducidad, revocacion y perfil vigente con reloj falso.
3. Implementar consulta de cobros con filtros por ids/fechas, LEFT JOIN, Sum(decimal),
   opciones historicas y cursor; demostrar que no se multiplica por servicios.
4. Adaptar login y lecturas del escritorio, distinguir 401/403 de datos vacios y
   proteger las lecturas heredadas de movimientos de forma coordinada.
5. Agregar modelos/repositorio/ViewModel/pantalla Android; integrar destino explicito,
   reingreso para token ausente y manejo de respuestas obsoletas sin persistir datos.
6. Completar regresion Android/API/escritorio y actualizar README, instrucciones de
   instalacion y paquete API mediante installer/build.ps1; desplegar sin bypass anonimo.

### Riesgos y limites aceptados del diseno

- Sesiones en memoria corresponden a una instancia API; reinicios exigen login.
  No se introduce cache distribuida ni refresh token para esta instalacion.
- Reporte vivo, no snapshot entre paginas; agregado y pagina de una respuesta usan
  la misma lectura. Cambios detectados de cantidad/total exigen actualizar. Una
  correccion concurrente que conserve agregados puede no detectarse.
- Operario proviene del turno actual; no se inventa historial de asignacion al cobrar.
- HTTP privado no cifra el token; HTTPS confiable recomendado. No desactivar TLS.
- Las lecturas de movimientos pasan a ser solo Administrador. Clientes viejos
  requieren actualizacion y las dos pantallas historicas deben respetar ese permiso.
- Otras consultas operativas y escrituras heredadas no se auditan ni endurecen
  globalmente en este reporte; no prometer seguridad total del sistema.
- No agregar indices o migraciones por anticipado. Si la medicion con datos sinteticos
  no cumple SC-005, medir el plan SQL y proponer un indice acotado antes de ampliar.

### Cobertura y gates de implementacion

| Requisitos | Validacion prevista |
|------------|--------------------|
| FR-001, FR-015 | Navegacion Compose, perfil, volver, tema, fuente y orientacion |
| FR-002, FR-003 | Limites de fecha de cobro, mismo dia, zona distinta del movil, pagos parciales |
| FR-004, FR-017 | 401/403 en rutas nuevas y GET heredados, revocacion, reinicio y escritorio actualizado |
| FR-005 a FR-008 | Opcionales solos/combinados, vacios, ids historicos, placa y filtros sin coincidencias |
| FR-009 a FR-011 | Filas por cobro, nulls, dinero exacto, resumen global y recorrido de paginas |
| FR-012, FR-013 | Red/error/pagina fallida, reintento, edicion sin consulta y respuesta antigua |
| FR-014, FR-016 | Sin caja abierta, sin escrituras y sin PII adicional en DTO/logs |

SC-001 a SC-004 y SC-006 a SC-008 se verifican con escenarios de quickstart;
SC-005 requiere 20 mediciones reales, al menos 19 inferiores a 5 segundos.
Build/lint/JVM, pruebas instrumentadas con dispositivo y pruebas API/desktop
ejecutadas correctamente; SC-001 9808 ms, SC-005 20/20 bajo 5 segundos con 1000
cobros. Paquete generado y E2E con API publicada/APK/escritorio correctos.
Ver comandos, conteos y limites en quickstart; no sustituirlos por supuestos de
produccion, Wi-Fi real o auditoria global.

## Complexity Tracking

Sin violaciones justificadas ni frameworks adicionales. La unica ampliacion transversal
es la sesion temporal autorizada para poder cumplir FR-004 y proteger la lectura de
movimientos sin confiar en flags locales. Se reutilizan perfiles y cifrado de login;
no se introduce JWT, nueva base de datos o persistencia de credenciales.
