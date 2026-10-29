# Implementation Plan: Confirmación de ingreso por WhatsApp

**Branch**: `004-confirmacion-whatsapp` | **Date**: 2026-10-28 | **Spec**: [spec.md](./spec.md)

## Summary

Después de confirmar un ingreso y sus servicios, `CarWashApi` preparará y enviará
una plantilla aprobada de WhatsApp mediante Meta Cloud API. La app móvil solo
consumirá el resultado de la API. La API persistirá una notificación por ingreso
con una clave única, estados observables y reintentos acotados; un fallo de
mensajería nunca revierte el ingreso. Las credenciales y la configuración de
plantilla permanecerán fuera del APK y del repositorio.

## Technical Context

**Language/Version**: Kotlin/JVM 17 (Android); C#/.NET 9 (API existente)
**Primary Dependencies**: Jetpack Compose, Retrofit/Gson, ASP.NET Core, EF Core SQLite, Meta Graph API
**Storage**: SQLite existente de CarWashApi; nueva tabla de notificaciones
**Testing**: JUnit/MockWebServer, pruebas de servicio/controlador .NET, mocks Graph API y webhooks
**Target Platform**: Android API 26+ y servidor IIS Windows en red local
**Project Type**: Aplicación móvil + API web
**Performance Goals**: Estado de notificación visible en menos de 10 segundos sin esperar indefinidamente a Meta
**Constraints**: No secretos en móvil/repositorio, no duplicados, no mensajes reales en pruebas
**Scale/Scope**: Ingresos diarios del autolavado; una notificación por ingreso confirmado

## Constitution Check

*GATE: PASS.* La propuesta respeta especificación previa, mantiene Android nativo,
añade pruebas, no introduce autenticación/pagos/reservas, no guarda secretos y
mantiene la API como autoridad del ingreso. El envío se ejecuta en servidor para
no exponer credenciales en el APK.

## Project Structure

```text
CarWash.Mobile/
├── app/src/main/java/com/carwash/mobile/ingreso/
├── app/src/main/res/values/
└── app/src/test/

CarWashApi/ (repositorio de API existente)
├── Controllers/
├── Services/
├── Modelos/
├── Context/
└── Tests/ (o proyecto de pruebas existente)
```

**Structure Decision**: Mantener la app móvil como cliente del flujo de ingreso y
extender `C:\Work\CarWashApi` con servicio, persistencia, endpoints de estado y
webhook de Meta. La API compone el mensaje a partir del ingreso confirmado y la
app solo presenta el estado devuelto.

## Phases

### Phase 0: Research

Documentar Meta Cloud API, plantillas, tokens, errores, webhooks, idempotencia y
pruebas simuladas en [research.md](./research.md).

### Phase 1: Design

Definir entidades y transiciones en [data-model.md](./data-model.md), contratos
en [contracts/api.md](./contracts/api.md) y validación en [quickstart.md](./quickstart.md).

### Phase 2: Implementation

Se generará con `/speckit-tasks` después de revisar este diseño. Incluirá primero
la persistencia/servicio en la API, luego contratos y UI móvil, y finalmente
pruebas y configuración segura.

## Complexity Tracking

No hay violaciones constitucionales que justificar.
