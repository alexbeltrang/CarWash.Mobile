# Data Model: Asignación de operario

Sin tablas ni columnas nuevas: se usan las existentes del API.

## Turno (`Turnos`)
| Campo | Uso |
|-------|-----|
| `IdTurno`, `Placa`, `NumeroTurno`, `FechaHoraIngreso` | Identificación y lista |
| `Estado` | `false` = en proceso; `true` = cerrado. Solo en proceso admite asignar |
| `idOperario` (nullable) | Operario asignado |
| `FechaHoraAsignacionOperario` (nullable) | Fecha y hora de la asignación |
| `OperadorOcupado` (nullable) | `true` mientras el operario atiende este turno |

Transiciones: sin operario → asignado (asignar) → asignado a otro (reasignar). Un turno cerrado no cambia.

## Operario (`Operarios`)
`idOperario`, `Nombres`, `Apellidos`, `isDelete`. Disponible = activo y sin turno con `OperadorOcupado = true`.

## Usuario (`Usuarios`) y Perfil (`Perfiles`)
Existentes. `PerfilId` determina si es Administrador.

## Estado de sesión (solo memoria en la app)
`IdUser`, `DisplayName`, `PerfilId`, `esAdministrador`. Se descarta al cerrar la app. No se guarda contraseña.

## Reglas de validación
- Asignar: turno en proceso; operario activo y libre (o ya asignado a ese mismo turno); usuario administrador activo.
- Reasignar: igual; el operario anterior queda libre.
