# Research: Asignación de operario

## D1. Inicio de sesión
- **Decision**: Reutilizar `POST api/auth/login`. La app cifra usuario y contraseña con el mismo algoritmo del WinForm (RC2-CBC, relleno PKCS7, Base64) antes de enviarlos. La sesión (`IdUser`, `DisplayName`, `PerfilId`) vive solo en memoria.
- **Rationale**: El API compara los textos cifrados; no se inventa autenticación nueva y el instalador ya crea usuarios compatibles.
- **Alternatives**: Endpoint nuevo con texto plano (rechazado: contraseña en claro por HTTP); PIN local (rechazado en la clarificación).
- **Riesgos a verificar primero**: (1) VERIFICADO en un teléfono Android 16: `Cipher.getInstance("RC2/CBC/PKCS7Padding")` lanza `NoSuchAlgorithmException`. Se implementó RC2-CBC en Kotlin puro (`auth/CredentialCipher.kt`, sin dependencias nuevas), validado con vectores generados en .NET 9 (clave efectiva de 56 bits). (2) La clave y el IV del cifrado quedan dentro del APK; el cifrado es ofuscación, no protección real. Se acepta por compatibilidad y se recomienda a futuro tokens con HTTPS.

## D2. Identificar al administrador
- **Decision**: El login devuelve `PerfilId`; la app muestra la acción de asignar solo si el perfil es Administrador (`Perfiles.Nombre = "Administrador"`, id 1 en la base actual). El API vuelve a validar en el endpoint de asignación con el `idUser` enviado.
- **Rationale**: La regla vive en el servidor, no solo en la interfaz.
- **Limitación**: No hay tokens; quien conozca un `idUser` administrador podría llamar al endpoint. Riesgo aceptado en red local; mitigación futura: tokens.
- **Alternatives**: Validar solo en la app (rechazado: trivialmente evitable).

## D3. Asignar operario
- **Decision**: Nuevo `PUT api/Turnos/{id}/Operario` con `{ idOperario, idUsuario }`. En una transacción: turno existe y está en proceso (`Estado = false`), usuario activo con perfil Administrador, operario activo y no ocupado en otro turno (`OperadorOcupado`). Guarda `idOperario`, `FechaHoraAsignacionOperario` y `OperadorOcupado = true`; al reasignar libera el anterior.
- **Rationale**: No existe endpoint dedicado; `ActualizarTurno` es genérico y no valida estas reglas. La transacción cubre dos administradores a la vez (devuelve 409).
- **Alternatives**: Reutilizar `ActualizarTurno/{id}` (rechazado: sin validaciones, permite sobrescribir datos).

## D4. Lista de vehículos y operarios
- **Decision**: Reutilizar `GET api/Turnos/VehiculosEnProceso` y `GET api/Operarios/OperadoresDisponibles`. Se corrige `VehiculosEnProceso` para devolver `idOperario` y tolerar `OperadorOcupado` nulo (hoy se convierte con `(bool)` y fallaría).
- **Rationale**: Mínimo cambio, compatible con el WinForm.

## D5. Reasignación y comisión
- **Decision**: Solo reemplaza; no hay historial. La nómina usa el `idOperario` final del turno (ya lo hace `PendientesNomina`).
- **Alternatives**: Tabla de historial (rechazada en la clarificación).

## D6. Concurrencia y reintentos
- **Decision**: 409 si el turno cerró o el operario ya está ocupado; la app refresca la lista. La asignación es idempotente: repetir la misma asignación devuelve 204. La app no marca éxito sin respuesta 2xx.

## D7. Consistencia con el WinForm
- **Verificado** en `FrmGestionDetalleVehiculo.cs`: al asignar, el WinForm guarda `idOperario`, `PorcentajeComision` (de `OperarioComisiones` para el día de hoy; debe ser mayor a 0 o rechaza), `FechaHoraAsignacionOperario = ahora` y `OperadorOcupado = true`. Al finalizar pone `Estado = true`, `Pagado = true` y `OperadorOcupado = false`. "Liberar operador" solo pone `OperadorOcupado = false` y conserva `idOperario`. El endpoint nuevo replica esa convención y responde 409 si no hay comisión mayor a 0 para hoy.
