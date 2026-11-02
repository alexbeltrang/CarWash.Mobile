# Contrato de API: Asignación de operario

Base: `http://<servidor>:5197/`. Datos de operario y turno en JSON.

## POST api/Auth/login (existente)
Cuerpo: `{ "UserName": "<cifrado>", "Password": "<cifrado>" }`.
Respuesta 200: `{ EsValido, Respuesta, Usuario: { IdUser, DisplayName, Email, PerfilId, EsAdministrador } }`. `EsAdministrador` (nuevo) es true si el perfil activo se llama Administrador. UserName y Password viajan cifrados con el mismo cifrado del WinForm. Credenciales inválidas devuelven 200 con `EsValido = false`.

## GET api/Turnos/VehiculosEnProceso (existente, se amplía)
200: lista de `{ IdTurno, Placa, Marca, NumeroTurno, FechaHoraIngreso, TipoVehiculo, OperadorAsignado, idOperario, OperadorOcupado }`.
Cambio: se agrega `idOperario` y `OperadorOcupado` pasa a tolerar valores nulos. `OperadorAsignado` vacío significa sin asignar.

## GET api/Operarios/OperadoresDisponibles (existente)
200: lista de `{ idOperario, Nombres, Apellidos }`. Lista vacía = no hay disponibles.

## PUT api/Turnos/{id}/Operario (nuevo)
Cuerpo: `{ "idOperario": 3, "idUsuario": 1 }`.

| Código | Significado |
|--------|-------------|
| 204 | Asignado (también si ya estaba asignado a ese operario) |
| 400 | Datos inválidos |
| 403 | El usuario no es administrador activo |
| 404 | Turno u operario inexistente o eliminado |
| 409 | Turno cerrado, operario ocupado en otro turno u operario sin comisión mayor a 0 para el día de hoy (misma regla del WinForm) |

Efecto: guarda `idOperario`, `FechaHoraAsignacionOperario = ahora`, `PorcentajeComision` (del día), `OperadorOcupado = true` y libera al operario anterior, todo en una transacción.

## Errores en la app
Fallos de red o 5xx: mensaje recuperable con reintento. 409: informa y refresca. 403: informa que se requiere perfil Administrador.
