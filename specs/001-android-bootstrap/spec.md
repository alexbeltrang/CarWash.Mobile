# 001 - Base Android de CarWash.Mobile

Estado: implementada; APK, lint y prueba de arranque verificados en emulador API 35.
Fecha: 2026-10-28

## Solicitud

Crear en esta carpeta un proyecto que funcione en celulares Android y permita
trabajar con Spec Kit. Las funcionalidades del negocio aun no estan definidas.

## Alcance y criterios de aceptacion

- FR-001: el proyecto debe contener una app Android nativa en Kotlin y Compose.
- FR-002: debe soportar Android 8.0 (API 26) o superior.
- FR-003: al iniciar debe mostrar CarWash e Inicio, sin depender de un servidor.
- FR-004: debe conservar los comandos oficiales de Spec Kit para GitHub Copilot
  y scripts PowerShell, una constitucion y especificaciones versionables.
- FR-005: debe incluir Gradle Wrapper e instrucciones para producir un APK e
  instalarlo en un celular con depuracion USB.
- FR-006: la app debe respetar areas seguras, tema del sistema y textos escalables.

## Escenarios

1. Dado un dispositivo API 26 o superior, al abrir la app instalada se muestran
   CarWash e Inicio sin solicitar permisos ni acceso a red.
2. Dado JDK 17 y SDK 35 configurados, al ejecutar `:app:assembleDebug` se obtiene
   `app/build/outputs/apk/debug/app-debug.apk`.
3. Dada la carpeta abierta en VS Code con Copilot, los comandos `/speckit-*`
   permiten definir y desarrollar la siguiente funcionalidad.

## Fuera del alcance

Reservas, catalogo de servicios, clientes, autenticacion, pagos, persistencia,
backend, publicacion en Google Play y firma de produccion.

## Supuestos por confirmar

- Nombre visible inicial: CarWash; identificador provisional: com.carwash.mobile.
- Kotlin y Compose elegidos como base nativa; no se requiere iOS por ahora.
- El publico y los flujos de negocio se definiran en la siguiente especificacion.

## Verificacion

`specify check` valida la disponibilidad de la CLI, no la compilacion Android.
La prueba `MainActivityTest` verifica FR-003 en un dispositivo o emulador.
El 2026-10-28 se ejecutaron correctamente build, lint y la prueba instrumentada
en `CarWash_API_35` (Pixel 6, Android 15/API 35). La pantalla inicial se inspecciono
en orientacion vertical y tema claro. La evidencia esta en `tasks.md`.
Quedan pendientes rotacion, modo oscuro, fuente grande y dispositivo fisico;
no se consideran verificados por la prueba de arranque.