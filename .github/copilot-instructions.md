# CarWash.Mobile

- Android nativo: Kotlin, Jetpack Compose y Material 3. Android minimo: API 26.
- Consultar `.specify/memory/constitution.md` y la especificacion activa en `specs/` antes de implementar.
- Actualizar especificacion, contratos afectados y tareas junto con cada cambio funcional.
- No inventar reglas de negocio, endpoints, autenticacion, reservas ni pagos.
- Mantener textos visibles en recursos Android y pruebas para el comportamiento modificado.
- No guardar secretos, archivos de firma ni rutas locales del SDK en Git.
- Compilar con `./gradlew.bat :app:assembleDebug :app:lintDebug` en Windows.
- Ejecutar `./gradlew.bat :app:connectedDebugAndroidTest` con un emulador o celular conectado.
- No declarar una prueba o compilacion exitosa sin ejecutarla; documentar bloqueos.