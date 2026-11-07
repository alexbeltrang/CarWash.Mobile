# Feature Specification: Asignacion de operario desde el movil

**Feature Branch**: `005-asignacion-operario`

**Created**: 2026-05-17

**Status**: Draft

**Input**: User description: "El administrador asigna el operario a los vehiculos recibidos desde el movil. El pago se cobra al entregar el vehiculo y se mantiene en la aplicacion de escritorio. Quien recibe y quien cobra pueden ser personas distintas o la misma."

## Clarifications

### Session 2026-11-02

- Q: Como se identifica al administrador en el movil? -> A: Inicio de sesion con el usuario existente (perfil Administrador)
- Q: Que pasa con la comision al reasignar? -> A: Solo reemplaza; la comision va al ultimo operario
- Q: Que puede hacer un perfil no administrador? -> A: Ingreso y consulta; sin asignar
- Q: Se recuerda la sesion? -> A: Decision inicial reemplazada el 2026-11-06: conservar sesion al volver o recrear la app, sin guardar usuario ni contrasena.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Asignar operario a un vehiculo en proceso (Priority: P1)

El administrador ve la lista de vehiculos recibidos que aun no tienen operario, elige uno y le asigna un operario disponible. El vehiculo queda registrado con ese operario y con la fecha y hora de la asignacion.

**Why this priority**: Es el paso que falta entre recibir el vehiculo y empezar el lavado; sin el, el trabajo no se reparte desde el movil.

**Independent Test**: Con un turno en proceso sin operario y al menos un operario disponible, asignar el operario y comprobar que el turno aparece con ese operario en la lista y en la aplicacion de escritorio.

**Acceptance Scenarios**:

1. **Given** un turno en proceso sin operario y operarios disponibles, **When** el administrador elige un operario y confirma, **Then** el turno muestra al operario asignado y el operario deja de aparecer como disponible.
2. **Given** un turno que otro usuario ya cerro o asigno mientras se editaba, **When** el administrador confirma, **Then** la app informa el conflicto, no sobrescribe en silencio y refresca la lista.
3. **Given** que no hay operarios disponibles, **When** el administrador abre la asignacion, **Then** la app lo indica y no permite confirmar.

---

### User Story 2 - Consultar vehiculos en proceso y su operario (Priority: P2)

El administrador y el recepcionista ven los vehiculos en proceso con placa, tipo, hora de ingreso y operario asignado (o "sin asignar").

**Why this priority**: Da visibilidad del trabajo pendiente y permite decidir a quien asignar.

**Independent Test**: Con turnos en proceso asignados y sin asignar, abrir la lista y verificar que cada uno muestra su estado correcto.

**Acceptance Scenarios**:

1. **Given** turnos en proceso, **When** se abre la lista, **Then** cada turno indica su operario o "sin asignar".
2. **Given** que no hay turnos en proceso, **When** se abre la lista, **Then** se muestra un estado vacio claro.
3. **Given** que el servidor no responde, **When** se abre la lista, **Then** se muestra un error recuperable con opcion de reintentar.

---

### User Story 3 - Reasignar el operario (Priority: P3)

El administrador cambia el operario de un turno que sigue en proceso.

**Why this priority**: Cubre ausencias o errores, pero no bloquea el flujo principal.

**Independent Test**: Reasignar un turno ya asignado y comprobar que el operario anterior queda disponible y el nuevo ocupado.

**Acceptance Scenarios**:

1. **Given** un turno en proceso con operario, **When** el administrador elige otro operario disponible, **Then** el turno queda con el nuevo operario y el anterior queda disponible.

---

### Edge Cases

- Un turno finalizado o entregado no se puede asignar ni reasignar.
- Dos administradores asignan el mismo operario a la vez: solo uno lo logra y el otro recibe un aviso.
- Perdida de conexion durante la confirmacion: la app no debe indicar exito sin respuesta del servidor y debe permitir reintentar sin duplicar.
- Un usuario sin permiso de administrador no ve la accion de asignar.
- Credenciales incorrectas: la app informa el error sin revelar cual dato fallo y permite reintentar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST listar los turnos en proceso con placa, hora de ingreso y operario asignado o "sin asignar".
- **FR-002**: La app MUST listar los operarios activos disponibles (sin vehiculo en curso) para asignar.
- **FR-003**: El administrador MUST poder asignar un operario disponible a un turno en proceso sin operario.
- **FR-004**: El sistema MUST registrar la fecha y hora de la asignacion junto con el operario y, igual que el WinForm, rechazar la asignacion si el operario no tiene comision mayor a 0 configurada para el dia de hoy.
- **FR-005**: La asignacion MUST ser visible de inmediato en la aplicacion de escritorio, porque ambas comparten los mismos datos.
- **FR-006**: La app MUST rechazar la asignacion de un turno ya cerrado o de un operario ocupado, e informar el motivo.
- **FR-007**: La app MUST pedir inicio de sesion con el usuario y perfil que ya existen en el sistema; solo el perfil Administrador MUST poder asignar o reasignar. Otros perfiles (por ejemplo recepcionista) conservan el ingreso y la consulta y no ven la accion de asignar.
- **FR-012**: La app MUST restaurar la sesion vigente al volver o recrearse,
  cifrando token y plazo mediante Android Keystore AES-GCM; nunca guardar usuario
  de acceso ni contrasena. Restaurar no reinicia el plazo de nueve horas ni
  extiende el vencimiento del servidor. Cierre explicito, vencimiento o 401
  requieren acceso nuevamente; datos antiguos sin token no habilitan sesion.
- **FR-008**: La app NO MUST registrar pagos, caja, nomina ni comisiones; el cobro al entregar el vehiculo queda en la aplicacion de escritorio.
- **FR-009**: Quien recibe y quien cobra MUST poder ser personas distintas o la misma, sin que la app los vincule ni exija que coincidan.
- **FR-010**: Los textos MUST estar en recursos y la app MUST seguir las reglas de conexion existentes (servidor, HTTP solo en red privada).
- **FR-011**: La reasignacion MUST ser posible solo mientras el turno esta en proceso y solo reemplaza al operario; la comision y la nomina van al ultimo operario asignado y no se guarda historial.

### Key Entities

- **Turno**: vehiculo recibido; tiene placa, hora de ingreso, estado (en proceso o cerrado), operario asignado y hora de asignacion.
- **Operario**: persona que lava; activo o no, disponible u ocupado.
- **Administrador**: usuario con perfil que puede asignar operarios.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El administrador asigna un operario a un vehiculo en menos de 30 segundos desde que abre la lista.
- **SC-002**: El 100% de las asignaciones hechas en el movil aparecen en la aplicacion de escritorio sin pasos manuales.
- **SC-003**: Ninguna asignacion se reporta como exitosa si el servidor no la confirmo.
- **SC-004**: Un operario ocupado nunca queda asignado a dos vehiculos en curso por esta funcion.

## Assumptions

- La app movil y la aplicacion de escritorio usan la misma API y base de datos; el cobro se mantiene en escritorio.
- El API ya guarda operario, hora de asignacion y ocupacion en el turno, y expone la lista de operarios disponibles y de vehiculos en proceso; la fase de plan confirmara si hace falta un endpoint nuevo para asignar, que hoy no existe de forma dedicada.
- El administrador asigna el operario; el recepcionista no.
- El cobro ocurre al entregar el vehiculo y es ajeno a esta funcion.
- Fuera de alcance: inicio de turno/lavado, finalizacion, entrega, pagos, caja, nomina y notificaciones de asignacion.

