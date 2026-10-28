# Feature Specification: Ingreso de vehiculos

**Feature Branch**: Sin crear rama; directorio 002-ingreso-vehiculo.
**Created**: 2026-10-28
**Status**: Implementada y verificada en emulador; API con escucha LAN habilitada; celular fisico pendiente.
**Input**: Reproducir el ingreso de FrmIngresoVehiculo del sistema existente,
consumiendo su API y permitiendo conexion desde la misma red Wi-Fi.

**Evolucion**: La funcionalidad 003-captura-placa agrega captura/revision y respaldo
manual sobre este formulario. No cambia guardado ni reglas; confirmar otra placa
limpia datos del vehiculo previo antes de buscar para evitar mezclas ante fallo.

## User Scenarios & Testing

### User Story 1 - Registrar ingreso (Priority: P1)

El recepcionista registra placa, tipo, cliente, celular y marca,
servicios, valor, base de comision y observaciones sin volver al computador.

**Why this priority**: Es la operacion principal del formulario original.
**Independent Test**: Con servidor simulado y caja activa, seleccionar servicios
y guardar; comprobar los datos y confirmacion, sin escribir en la base real.

**Acceptance Scenarios**:
1. **Given** catalogos disponibles, **When** cambia el tipo, **Then** se muestran
   solo sus servicios y se reinician seleccion y valores anteriores.
2. **Given** varios servicios seleccionados, **When** cambia la seleccion,
   **Then** valor y base se recalculan con las tarifas del catalogo; son editables.
3. **Given** placa, tipo y servicios validos y caja activa, **When** guarda,
   **Then** se registran turno y servicios y se confirma solo al completar todos.
4. **Given** un campo obligatorio ausente o sin caja activa, **When** guarda,
   **Then** no se inicia el registro y se indica el problema.
5. **Given** formulario editado, **When** cancela, **Then** limpia ingreso e historial.
6. **Given** turno y todos sus servicios confirmados por el servidor, **When**
  finaliza guardar, **Then** limpia el formulario y permite el siguiente ingreso
  sin mostrar advertencia de registro incompleto.

### User Story 2 - Buscar por placa (Priority: P2)

El recepcionista recupera datos e historial del vehiculo antes de registrar.

**Why this priority**: Reduce errores y digitacion repetida.
**Independent Test**: Buscar una placa simulada y una inexistente.

**Acceptance Scenarios**:
1. **Given** placa existente, **When** busca, **Then** recupera tipo, cliente,
   celular y marca y muestra historial disponible ordenado como lo entrega el sistema.
2. **Given** placa inexistente, **When** busca, **Then** permite un ingreso nuevo
   sin conservar datos del vehiculo anterior.
3. **Given** edicion mientras hay consulta pendiente, **When** llega una respuesta
   anterior, **Then** no reemplaza los datos de la placa actual.

### User Story 3 - Conectar al computador por Wi-Fi (Priority: P1)

El usuario configura y comprueba la direccion del computador que presta el servicio.

**Why this priority**: Sin conexion al servidor no hay catalogos ni registros.
**Independent Test**: Cambiar de servidor, comprobar conexion y reabrir la app.

**Acceptance Scenarios**:
1. **Given** una direccion alcanzable, **When** conecta, **Then** carga catalogos
   y caja y recuerda esa direccion para el siguiente inicio.
2. **Given** servidor inaccesible, **When** conecta, **Then** informa el fallo,
   permite corregir direccion o reintentar y no muestra catalogos de otro servidor.
3. **Given** celular en la misma Wi-Fi y servidor accesible en la red local,
   **When** configura su direccion local, **Then** accede sin usar localhost del celular.

### Edge Cases

- Caja cerrada entre la carga de pantalla y guardar: volver a consultar antes de escribir.
- Interrupcion durante un registro: no reintentar escrituras automaticamente;
  advertir posible registro parcial y bloquear otro envio hasta revision y limpieza.
- La API existente no garantiza atomicidad ni exclusividad del consecutivo entre
  clientes; conservar el flujo existente e informar esta limitacion, no prometerla.
- Cero servicios o tarifas nulas: no seleccionar automaticamente; nulo equivale a
  cero como en el formulario original. No aceptar valores negativos ni no numericos.
- Respuesta vacia/404 de placa o historial: estado vacio, no error de conectividad.
- Cambiar servidor descarta catalogos y formulario anteriores para no mezclar IDs.
- Un timeout durante guardar no confirma exito ni implica que nada se registro.
- Un turno visible no garantiza que todos los servicios se confirmaron. Corregir
  el fallo del servidor, no ocultar HTTP 500 ni declarar exito de forma anticipada.

## Requirements

### Functional Requirements

- **FR-001**: Mostrar los campos del formulario original excepto numero de orden, seleccion multiple
  de servicios y acciones guardar/cancelar.
- **FR-002**: Placa, tipo real, al menos un servicio y valores
  numericos no negativos son obligatorios. Cliente, celular, marca y observaciones
  son opcionales segun las etiquetas del disenador original.
- **FR-003**: Normalizar placa, cliente, marca y observaciones a mayusculas.
- **FR-004**: Cargar tipos no eliminados, servicios por tipo, tarifa y base.
- **FR-005**: Consultar y exigir caja activa antes de registrar; no abrir cajas.
- **FR-006**: Conservar formato diario del turno, estados iniciales y relaciones
  de servicios del flujo original, sin incorporar pagos ni asignar operarios.
- **FR-007**: Buscar datos e historial por placa, sin mezclar consultas obsoletas.
- **FR-008**: Conservar datos ante fallos de lectura y evitar doble envio o reintentos
  de escritura automaticos; distinguir guardado completo de parcial o incierto.
- **FR-009**: Permitir configurar, probar y recordar el servidor local.
- **FR-010**: Mostrar carga, falta de conexion, caja cerrada, vacios, errores y exito.
- **FR-011**: Funcionar con teclado, desplazamiento, fuentes del sistema y recreacion
  por rotacion sin perder el formulario durante la vida del proceso.
- **FR-012**: No mostrar ni solicitar numero de orden. Enviar siempre cero en ese
  campo al registrar, conservando el contrato existente y sin modificar la base.

### Key Entities

- **Tipo de vehiculo**: identificador, nombre y marca de eliminacion.
- **Servicio por tipo**: servicio, tipo, tarifa y base de comision.
- **Ingreso**: placa, datos opcionales del cliente, orden fija en cero, valores, caja y servicios.
- **Historial**: fecha, placa, tipo, cliente, marca, valor pagado y servicio si existe.
- **Servidor**: direccion persistida para este dispositivo; sin credenciales nuevas.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Los campos de ingreso originales excepto numero de orden estan disponibles;
  el registro envia cero sin solicitarlo al usuario.
- **SC-002**: La seleccion de servicios produce exactamente la suma de las tarifas
  y bases obtenidas, incluida la eliminacion de una seleccion.
- **SC-003**: Los escenarios de validacion y caja cerrada producen cero escrituras.
- **SC-004**: Se muestra una sola confirmacion de ingreso completo y se limpia el
  formulario unicamente cuando finalizan todos los pasos.
- **SC-005**: La direccion configurada se conserva al volver a abrir la aplicacion.

## Assumptions

- Fuente funcional: FrmIngresoVehiculo.cs y su Designer; se corrigen las omisiones
  accidentales de validacion por recorrer solo controles superiores, no se cambian
  las reglas de negocio declaradas.
- El servidor existente no tiene autenticacion ni descubrimiento de red publicado;
  no se agregan mecanismos ficticios ni se escanea la red Wi-Fi.
- Encontrar el computador significa configurar su IP/nombre local alcanzable.
  Descubrimiento automatico requeriria una capacidad adicional del servidor.
- Compartir Wi-Fi no basta: el servidor debe escuchar en la interfaz LAN y permitir
  el puerto en firewall privado. El usuario autorizo configurar y reiniciar la
  API externa: solo se agrega perfil wifi, sin cambiar endpoints ni datos.
  No se modifica el firewall ni el perfil de red sin accion manual del usuario.
- No se enviaran ingresos ficticios a la base real para verificar el formulario.
- No incluye modo offline, edicion de turnos, apertura de caja, pagos ni usuarios.