# Plan tecnico - Base Android

## Plataforma

- Modulo `app`, applicationId provisional `com.carwash.mobile`.
- Kotlin 2.1.20 y plugin Compose de la misma version.
- Android Gradle Plugin 8.9.2, Gradle 8.11.1, JDK 17.
- compileSdk/targetSdk 35; minSdk 26; Compose BOM 2025.04.01.
- Activity Compose, Material 3 y AndroidX Test.

## Diseno

Una Activity y una pantalla Compose sin estado de negocio. Recursos para textos
e icono. Tema claro/oscuro y soporte edge-to-edge. Sin repositorios, navegacion,
inyeccion de dependencias ni red hasta tener requisitos para esas capacidades.

## Contratos

Consultar `contracts/app.md`. No existen contratos HTTP ni modelo persistente.

## Validacion

1. Comprobar estructura Spec Kit y XML de recursos.
2. Verificar integridad del Gradle Wrapper descargado de la fuente oficial.
3. Ejecutar `:app:assembleDebug :app:lintDebug` con JDK 17 y SDK 35.
4. Ejecutar `:app:connectedDebugAndroidTest` con dispositivo API 26 o superior.
5. Inspeccionar rotacion, fuente grande y temas en el dispositivo.