# Tareas - Base Android

- [x] T001 Inicializar Spec Kit oficial para Copilot y PowerShell.
- [x] T002 Crear configuracion Gradle y modulo Android.
- [x] T003 Crear Activity, pantalla inicial, recursos y tema.
- [x] T004 Agregar prueba instrumentada de arranque.
- [x] T005 Documentar constitucion, especificacion, plan y contrato inicial.
- [x] T006 Descargar Gradle Wrapper oficial y verificar SHA-256 del JAR.
- [x] T007 Compilar APK y ejecutar Android Lint con JDK 17 y SDK 35.
- [x] T008 Ejecutar prueba instrumentada en dispositivo o emulador.
- [ ] T009 Revisar rotacion, fuente grande y temas en dispositivo.

## Evidencia

- `specify check`: correcto el 2026-10-28.
- Cuatro archivos XML Android: sintaxis valida.
- Cinco skills del flujo Spec Kit documentado: presentes.
- Constitucion: sin marcadores de plantilla pendientes.
- Gradle Wrapper 8.11.1: JAR verificado contra SHA-256 oficial de Gradle.
- Bloqueo inicial de Java resuelto: JDK Temurin 17 instalado con SHA-256 verificado.
- SDK Platform 35, Build-Tools 35.0.0 e imagen Google APIs x86_64 instalados.
- Emulador `CarWash_API_35`, perfil Pixel 6, Android 15/API 35; WHPX disponible.
- `JAVA_HOME`, `ANDROID_HOME` y PATH configurados para el usuario, sin rutas en Git.
- Error inicial PKIX resuelto en la sesion con el almacen Windows-ROOT de Java;
	validacion TLS conservada. Comando documentado en README.
- `gradlew.bat :app:assembleDebug :app:lintDebug`: BUILD SUCCESSFUL.
- APK generado: `app/build/outputs/apk/debug/app-debug.apk` (10 075 216 bytes).
- `gradlew.bat :app:installDebug :app:connectedDebugAndroidTest`: BUILD SUCCESSFUL;
	1 prueba ejecutada, 0 omitidas y 0 fallos.
- APK reinstalado tras la limpieza de pruebas y Activity abierta mediante ADB.
- Inspeccion visual: icono, CarWash e Inicio visibles en vertical y tema claro.
- Captura local generada: `app/build/reports/carwash-home.png` (no versionada).
- T009 permanece pendiente; no se ha verificado un celular fisico ni API 26.