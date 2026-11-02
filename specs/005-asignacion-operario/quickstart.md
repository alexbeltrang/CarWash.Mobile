# Quickstart: validar la asignación de operario

## Prerrequisitos
- API con el endpoint nuevo en ejecución (por ejemplo en `http://localhost:5197/`) sobre una **copia** de la base, no la real.
- Usuario administrador y un usuario recepcionista de prueba; al menos dos operarios activos.
- Teléfono conectado y desbloqueado.

## Comandos
```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot'; $env:ANDROID_HOME='C:\Android'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

## Escenarios
1. Inicio de sesión del administrador: aparece la lista y la acción de asignar. Credenciales erróneas: error genérico y reintento.
2. Asignar un operario libre a un turno sin operario: la lista lo muestra asignado; el WinForm lo ve igual (US1).
3. Intentar asignar un operario ocupado o un turno cerrado: mensaje de conflicto y lista refrescada.
4. Sin operarios disponibles: aviso y botón de confirmar deshabilitado.
5. Reasignar: el operario anterior vuelve a aparecer como disponible (US3).
6. Recepcionista: ve ingreso y lista, sin acción de asignar. Llamar al endpoint con su `idUsuario` devuelve 403.
7. Cerrar y abrir la app: pide inicio de sesión; no queda contraseña ni sesión guardadas.
8. Sin conexión: error recuperable; nunca se muestra éxito sin 2xx.

## Resultados reales
- Unit tests (cifrado vs .NET, login, asignación, repositorio con MockWebServer), `assembleDebug` y `lintDebug`: BUILD SUCCESSFUL.
- `connectedDebugAndroidTest` en el teléfono 25100RA69G (Android 16): 19/19. Un test existente falló una vez al cerrar la actividad (pantalla del equipo) y pasó al repetirlo; no es un fallo funcional.
- Endpoint contra copia temporal de la base (API en 127.0.0.1:5299): escenarios 2, 3, 5 y 6 (204 / 409 ocupado, sin comisión y cerrado / reasignar libera al anterior / 403 no admin) y login con `esAdministrador` verificados.
- Escenarios 1, 4, 7 y 8: cubiertos por pruebas unitarias y Compose (fake); NO recorridos manualmente en el teléfono contra el API real porque el API en 5197 aún no tiene el endpoint nuevo (hay que reinstalar con el nuevo `CarWashApi-Setup.exe`).

Registrar resultados reales; documentar como bloqueo cualquier prueba no ejecutada.
