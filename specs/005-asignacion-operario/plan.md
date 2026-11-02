# Implementation Plan: Asignación de operario desde el móvil

**Branch**: `005-asignacion-operario` | **Date**: 2026-11-02 | **Spec**: [spec.md](./spec.md)

## Summary

La app móvil agrega inicio de sesión (usuario y perfil existentes, vía
`api/auth/login`), una lista de vehículos en proceso con su operario y, solo para
el perfil Administrador, la asignación y reasignación de operario. El API
existente se extiende con un endpoint transaccional de asignación y con el
`idOperario` en la lista de vehículos en proceso. El cobro, la caja y la nómina
permanecen en el WinForm.

## Technical Context

**Language/Version**: Kotlin/JVM 17 (Android); C#/.NET 9 (API existente)
**Primary Dependencies**: Jetpack Compose, Retrofit/Gson (ya presentes); ASP.NET Core, EF Core SQLite (API)
**Storage**: SQLite existente de CarWashApi (`Turnos`, `Operarios`, `Usuarios`); la app no persiste sesión ni credenciales
**Testing**: JUnit/MockWebServer en la app; pruebas Compose instrumentadas; pruebas de controlador del API sobre SQLite temporal
**Target Platform**: Android API 26+ y API en Windows (red local, puerto 5197)
**Project Type**: Aplicación móvil + API web
**Performance Goals**: Asignar en menos de 30 s desde abrir la lista (SC-001); respuesta del API en menos de 2 s en red local
**Constraints**: Sin sesión ni contraseña guardadas (FR-012); HTTP solo en red privada; sin registros ficticios en la base real; asignación atómica
**Scale/Scope**: Decenas de vehículos y operarios al día; 3 pantallas nuevas (login, lista, asignación)

## Constitution Check

*GATE: PASS con una excepción registrada.*

- I. Especificación antes de funcionalidad: PASS (spec 005 clarificada).
- II. Android nativo y simplicidad: PASS; sin dependencias nuevas.
- III. Calidad verificable: PASS; pruebas por historia y evidencia real en `quickstart.md`.
- IV. Privacidad y seguridad: PASS con cuidado. No se persisten contraseña ni sesión; no se registran cuerpos HTTP. La constitución prohíbe autenticación sin especificación: esta spec la autoriza (FR-007, FR-012), y debe actualizarse la constitución a 1.2.0 para permitir el inicio de sesión con la API existente.
- V. Experiencia móvil accesible: PASS; textos en recursos.
- Restricción "sin pagos": PASS; FR-008 deja el cobro fuera.

Reevaluación tras el diseño: sin violaciones nuevas. Riesgo residual documentado en [research.md](./research.md) (no hay tokens; la autorización del API se basa en el usuario enviado).

## Project Structure

```text
specs/005-asignacion-operario/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
└── tasks.md            # lo genera /speckit-tasks

CarWash.Mobile/
├── app/src/main/java/com/carwash/mobile/
│   ├── auth/           # sesión en memoria, cifrado compatible, repositorio de login
│   ├── asignacion/     # lista de vehículos, operarios, ViewModel, pantallas
│   └── ingreso/        # conexión existente reutilizada
├── app/src/main/res/values/strings.xml
├── app/src/test/
└── app/src/androidTest/

CarWashApi/ (C:\Work\CarWashApi)
├── Controllers/TurnosController.cs     # nuevo PUT {id}/Operario; idOperario en VehiculosEnProceso
├── Controllers/OperariosController.cs  # OperadoresDisponibles (existente)
└── DTOs/
```

**Structure Decision**: La app consume la misma API que el WinForm. La regla de
asignación (turno abierto, operario activo y libre, perfil Administrador) vive en
el API para que el WinForm y el móvil compartan el mismo resultado.

## Phases

### Phase 0: Research
Ver [research.md](./research.md).

### Phase 1: Design
[data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md) y [quickstart.md](./quickstart.md).

## Complexity Tracking

| Violación | Por qué es necesaria | Alternativa más simple rechazada |
|-----------|----------------------|----------------------------------|
| Cifrado RC2 compatible en la app | El API compara usuario y contraseña ya cifrados por el WinForm | Enviar texto plano exigiría un login nuevo y exponer la clave en tránsito sin cifrar |
