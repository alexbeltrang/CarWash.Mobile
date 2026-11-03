# Feature Specification: Reporte de ventas por fechas

**Feature Branch**: Sin crear (no solicitada).

**Created**: 2026-11-03

**Status**: Implementada y verificada en aislamiento - T001-T056 completadas. APK y paquete API generados; metricas y E2E con escritorio verificados. Alcance/limites en quickstart. No desplegado.

**Input**: User description: "Reporte de ventas en la app movil con acceso desde un menu o navegacion apropiada. Filtrar por rango de fechas obligatorio, forma de pago y operario opcionales; evaluar otros filtros utiles. Los filtros no seleccionados incluyen todos sus valores y los seleccionados deben determinar la informacion mostrada."

## Clarifications

### Session 2026-11-03

- Q: Que se considera una venta? -> A: "Ventas significa cobros, esos cobros quedan en la tabla TurnosMovimientos". El reporte consulta los movimientos de cobro y aplica el periodo a su fecha de cobro, no a la fecha de ingreso del turno.
- Q: Quien puede consultar el reporte? -> A: Solo el perfil Administrador.
- Q: Se autoriza ampliar el login existente con una sesion temporal validada por el servidor, sin guardar contrasenas, y contemplar compatibilidad con escritorio? -> A: Si, autorizado por el usuario el 2026-11-03.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar ventas de un periodo (Priority: P1)

Una persona autorizada abre Reporte de ventas desde la navegacion de la app,
selecciona fecha inicial y final y consulta el detalle y el total del periodo.
No necesita elegir forma de pago ni operario para obtener resultados.

**Why this priority**: Permite conocer las ventas del periodo sin depender del computador.

**Independent Test**: Preparar registros conocidos dentro y fuera de un periodo,
consultar solo con fechas y comparar detalle, cantidad y total con los esperados
segun la definicion de venta acordada en FR-003.

**Acceptance Scenarios**:

1. **Given** una sesion con permiso, **When** abre la navegacion principal, **Then** encuentra Reporte de ventas junto con los accesos existentes de ingreso y vehiculos en proceso.
2. **Given** fechas validas y ningun filtro opcional, **When** consulta, **Then** obtiene todos los registros elegibles del periodo, cualquiera que sea su forma de pago u operario.
3. **Given** registros al inicio y al final del ultimo dia seleccionado, **When** consulta, **Then** ambos se incluyen y los del dia siguiente se excluyen.
4. **Given** una fecha vacia o una fecha inicial posterior a la final, **When** intenta consultar, **Then** se indica el error y no se ejecuta la consulta.
5. **Given** un periodo sin ventas, **When** consulta, **Then** ve un estado sin resultados, cantidad cero y total cero.
6. **Given** un usuario con perfil distinto de Administrador, **When** intenta acceder al reporte, incluso por una solicitud directa, **Then** no obtiene datos ni totales y no ve su acceso en la navegacion.
7. **Given** un turno ingresado fuera del periodo y cobrado dentro de el, **When** consulta, **Then** incluye el cobro; un turno ingresado dentro del periodo sin cobros en el periodo no agrega ventas ni importes.
8. **Given** una sesion temporal ausente, vencida o invalidada, **When** intenta consultar, **Then** no obtiene informacion de ventas y se solicita iniciar sesion de nuevo sin guardar su contrasena.
9. **Given** el escritorio actualizado junto con la API, **When** inicia sesion y realiza sus operaciones existentes, **Then** conserva sus flujos permitidos y las consultas de ventas respetan el perfil Administrador.

---

### User Story 2 - Combinar filtros opcionales (Priority: P2)

La persona delimita el periodo por forma de pago, operario, placa o tipo de vehiculo.
Puede combinar filtros, retirar uno sin perder los otros y volver a incluir todos
los valores opcionales sin cambiar las fechas.

**Why this priority**: Permite revisar recaudos por medio de pago y ventas asociadas
a un operario o vehiculo sin alterar el significado de la consulta general.

**Independent Test**: Con registros de distintas formas de pago, operarios y tipos,
comparar cada filtro individual y sus combinaciones con un conjunto esperado.

**Acceptance Scenarios**:

1. **Given** un periodo con varias formas de pago, **When** selecciona una, **Then** solo aparecen ventas de esa forma de pago dentro del periodo.
2. **Given** un periodo con varios operarios, **When** selecciona uno y deja forma de pago en Todas, **Then** aparecen sus ventas de cualquier forma de pago.
3. **Given** forma de pago y operario seleccionados, **When** consulta, **Then** cada registro satisface ambos filtros y las fechas, no solo alguno de ellos.
4. **Given** filtros opcionales seleccionados, **When** limpia los filtros, **Then** conserva las fechas y la siguiente consulta incluye todos los valores opcionales.
5. **Given** una placa o tipo de vehiculo seleccionado, **When** consulta, **Then** solo aparecen coincidencias dentro del periodo y de los demas filtros activos.
6. **Given** un registro sin operario o sin forma de pago informados, **When** consulta sin esos filtros, **Then** se incluye si cumple la definicion de venta y se muestra el dato como Sin informar.

---

### User Story 3 - Interpretar resultados y recuperarse de errores (Priority: P3)

La persona identifica el periodo y los filtros aplicados, revisa los importes y
puede reintentar una consulta fallida sin volver a completar los filtros.

**Why this priority**: Evita confundir resultados anteriores con una nueva consulta
y permite utilizar el reporte de forma confiable en la operacion diaria.

**Independent Test**: Consultar un conjunto con varios cobros por turno y varios servicios, cambiar
filtros y simular perdida de conexion; comprobar los totales y la identificacion
de la consulta a la que pertenecen los resultados.

**Acceptance Scenarios**:

1. **Given** un cobro de un turno con varios servicios, **When** consulta, **Then** el cobro aparece una sola vez y su importe no se suma por cada servicio.
2. **Given** resultados visibles, **When** modifica filtros sin consultar, **Then** se distingue que los resultados pertenecen a la consulta anterior y no a los nuevos filtros.
3. **Given** una consulta que falla, **When** termina el intento, **Then** ve un error recuperable, no un falso resultado vacio, y puede reintentar con los mismos filtros.
4. **Given** resultados que necesitan varias tandas para revisarse, **When** recorre el detalle, **Then** puede acceder a todas las ventas y la cantidad y el total corresponden al conjunto completo filtrado.
5. **Given** un turno con dos cobros en el periodo y otro fuera de el, **When** consulta sin filtros opcionales, **Then** aparecen los dos movimientos del periodo, la cantidad es dos y el total suma solo sus importes.

### Edge Cases

- Fecha inicial igual a la final: se incluye ese dia completo segun la fecha operativa del negocio.
- Ingreso y cobro en dias distintos: se aplica exclusivamente la fecha del cobro, sin sustituirla silenciosamente por la fecha de ingreso del turno.
- Varios cobros de un turno, incluso con distintas formas de pago: cada movimiento se filtra por su fecha, forma de pago e importe propios, sin repetir el valor completo del turno.
- Venta sin fecha o importe necesarios para clasificarla: no se inventan valores; se informa la limitacion si impide un resultado confiable.
- Operario inactivo o forma de pago retirada con ventas historicas: deben poder consultarse los valores presentes en el periodo, no solo los disponibles para nuevas operaciones.
- Combinacion sin coincidencias: no se ignora ningun filtro para mostrar resultados.
- Cambio rapido de filtros: una consulta anterior no reemplaza resultados de una consulta posterior.
- Importes y precision: se conservan los importes registrados y no se duplican ni se recalculan con las tarifas actuales.
- La consulta no depende de que haya caja abierta y nunca modifica ventas, pagos o asignaciones.
- Cierre de sesion sin conexion: se descarta inmediatamente la credencial local; la revocacion remota requiere respuesta del servidor y no se declara confirmada sin ella. La credencial remota sigue limitada por su vencimiento.
- El reporte es una consulta viva, no un cierre contable congelado: si se registran o corrigen cobros durante el recorrido, se debe actualizar la consulta para revisar el nuevo conjunto completo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST ofrecer Reporte de ventas como destino de la navegacion principal para personas autorizadas, conservando el acceso a ingreso y vehiculos en proceso y una vuelta predecible entre destinos.
- **FR-002**: El reporte MUST exigir solo fecha inicial y fecha final, aceptar un mismo dia e incluir ambos dias completos usando la fecha operativa del negocio; MUST impedir consultas con fechas ausentes o rango invertido.
- **FR-003**: El reporte MUST considerar ventas exclusivamente los movimientos de cobro registrados y aplicar el rango a la fecha del cobro. MUST usar el importe y la forma de pago de cada cobro, no el valor presupuestado ni la fecha de ingreso del turno; los turnos sin cobros en el periodo MUST no aportar ventas ni importes.
- **FR-004**: Solo el perfil Administrador MUST poder acceder al reporte, sus opciones de filtros, detalle y totales. El sistema MUST rechazar consultas de otros perfiles incluso por acceso directo; ocultar el menu MUST no ser la unica restriccion.
- **FR-005**: Forma de pago y operario MUST ser filtros opcionales con Todas/Todos como estado inicial; no seleccionar un valor MUST significar ausencia de restriccion de ese campo, incluso para registros con ese dato sin informar.
- **FR-006**: El reporte MUST ofrecer placa y tipo de vehiculo como filtros adicionales opcionales. Placa MUST buscar coincidencia completa ignorando mayusculas y espacios exteriores; vacia MUST no restringir. Tipo de vehiculo MUST permitir Todos o un tipo especifico.
- **FR-007**: Todos los filtros seleccionados MUST combinarse por interseccion con el rango de fechas. Limpiar filtros MUST retirar solo las restricciones opcionales y conservar las fechas.
- **FR-008**: Las opciones de forma de pago, operario y tipo de vehiculo MUST cargarse completas desde las tablas de formas de pago, operarios y tipos de vehiculo, sin depender de las fechas elegidas ni de que existan ventas en el periodo, e incluir valores historicos aunque ya no esten activos.
- **FR-009**: El detalle MUST mostrar una fila por movimiento de cobro con identificador de movimiento, identificador de turno, fecha de cobro, placa, tipo de vehiculo, operario, forma de pago e importe cobrado; los datos descriptivos ausentes MUST mostrarse como Sin informar y no provocar que se pierdan cobros elegibles.
- **FR-010**: El reporte MUST mostrar cantidad de cobros e importe total de todos los movimientos filtrados, no solo del detalle visible; MUST mantener la precision monetaria registrada, contar cada cobro una sola vez aunque el turno tenga varios servicios y conservar por separado los distintos cobros de un mismo turno.
- **FR-011**: El detalle MUST ordenarse por fecha de cobro de mas reciente a mas antiguo y desempatar por identificador de movimiento descendente; todos los registros coincidentes MUST ser accesibles sin truncamiento silencioso.
- **FR-012**: El reporte MUST distinguir cargando, resultados, sin resultados y error; MUST permitir reintentar conservando filtros y no presentar una consulta fallida como total cero.
- **FR-013**: Cada resultado MUST identificar las fechas y filtros con los que fue obtenido; cambiar controles sin consultar MUST marcar el resultado como anterior, y una respuesta antigua MUST no sobrescribir la consulta mas reciente.
- **FR-014**: El reporte MUST ser de solo lectura y funcionar sin caja abierta; MUST no registrar cobros, cambiar turnos, recalcular comisiones ni alterar los flujos existentes.
- **FR-015**: La vista MUST permitir consultar y leer resultados con orientacion vertical u horizontal, tema claro u oscuro y fuente ampliada sin ocultar filtros o acciones esenciales.
- **FR-016**: El reporte MUST limitar los datos personales mostrados a los necesarios para revisar ventas; no MUST incluir telefono, correo ni credenciales.
- **FR-017**: El login existente MUST poder establecer una sesion temporal verificable por el servidor, sin nuevos usuarios o proveedores de identidad ni almacenamiento de contrasenas. Las consultas protegidas MUST rechazar sesiones ausentes, vencidas o revocadas, comprobar el perfil vigente y permitir reingresar; cerrar sesion MUST descartar la credencial local inmediatamente y revocarla en servidor al confirmar la solicitud. Sin conexion MUST no afirmarse una revocacion remota y la credencial MUST seguir limitada por su vencimiento. Los cambios MUST contemplar la actualizacion coordinada de escritorio y conservar sus flujos permitidos sin dejar accesos alternos de consulta del reporte sin proteccion.
- **FR-018**: Con resultados, el reporte MUST mostrar una tabla de resumen agrupada por forma de pago con cantidad de cobros e importe por grupo y una fila de total, calculada sobre todos los movimientos filtrados (no solo la pagina cargada), de modo que la suma de los grupos coincida con cantidad e importe totales.

### Key Entities

- **Venta reportada o cobro**: movimiento de cobro unico asociado a un turno, con fecha de cobro, importe y forma de pago propios; el turno aporta operario, placa y tipo de vehiculo. Un turno puede tener varios cobros sin que se fusionen o dupliquen sus importes.
- **Criterios de consulta**: fecha inicial y final obligatorias y selecciones opcionales de forma de pago, operario, placa y tipo de vehiculo.
- **Resultado de ventas**: detalle ordenado de movimientos de cobro, cantidad de cobros, importe total cobrado y criterios efectivamente aplicados.
- **Usuario autorizado**: persona con sesion existente y perfil Administrador.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona autorizada puede abrir el reporte, indicar un periodo y solicitar la consulta en menos de 30 segundos, sin completar filtros opcionales.
- **SC-002**: En los conjuntos de comprobacion, el 100% de las ventas mostradas cumplen las fechas y todos los filtros activos, y ninguna venta elegible se pierde por dejar un filtro vacio.
- **SC-003**: En el 100% de los conjuntos de comprobacion, la cantidad y el importe total coinciden con los movimientos de cobro unicos elegibles, incluidos casos con multiples servicios, varios cobros de un mismo turno y varios grupos de resultados.
- **SC-004**: El 100% de las consultas con fechas ausentes o rango invertido se detienen antes de solicitar resultados y explican el dato que debe corregirse.
- **SC-005**: En al menos el 95% de 20 consultas de comprobacion con hasta 1000 ventas elegibles y conexion local estable, la persona ve el resumen y el primer detalle en menos de 5 segundos.
- **SC-006**: Ningun usuario con perfil distinto de Administrador obtiene opciones de filtros, detalle o totales, incluso por acceso directo, y ninguna consulta modifica datos de negocio.
- **SC-007**: En todos los escenarios de perdida de conexion y cambio de filtros, la persona puede distinguir error, ausencia real de ventas y resultados de una consulta anterior.
- **SC-008**: El 100% de los intentos con sesion ausente, vencida o revocada se rechazan sin datos de ventas; el cierre de sesion confirmado por el servidor impide reutilizar la credencial y los flujos existentes del escritorio actualizado pasan sus escenarios de regresion. Sin conexion, la app descarta la credencial local sin afirmar confirmacion remota.

## Assumptions

- Se reutilizan usuarios, validacion de credenciales y perfiles existentes. El usuario autorizo ampliar ese login con una sesion temporal verificable por el servidor y contemplar compatibilidad con escritorio; no se crean usuarios ni proveedores de identidad nuevos, no se guardan contrasenas y no se cambia el cifrado heredado como parte del reporte.
- El movil registra ingresos y el cobro se mantiene en escritorio; esta funcion solo consulta y no incorpora pagos al movil.
- Las fechas se presentan inicialmente con el dia operativo actual en ambos extremos y pueden editarse antes de consultar; no se impone un limite arbitrario al rango.
- Operario significa la persona asignada al lavado, no quien recibe el vehiculo ni quien cobra.
- Se eligen placa y tipo de vehiculo por su utilidad para localizar una venta y comparar categorias ya existentes. No se incluyen filtros por cliente, telefono o correo.
- La fuente confirmada por el usuario es TurnosMovimientos. La planificacion debe verificar como se identifican sus movimientos de cobro, su fecha, importe, forma de pago y relacion con el turno, respetando las reglas existentes sin inventar estados o tratamientos de anulaciones; no se presume que ya existe una consulta de reporte.
- Fuera de alcance: exportacion, impresion, graficos, programacion de reportes, cambios de ventas, nuevos medios de pago, filtros por comision o nomina y modificaciones de los cobros de escritorio.
- Las comprobaciones utilizaran datos controlados sin crear ventas ficticias en la operacion real.