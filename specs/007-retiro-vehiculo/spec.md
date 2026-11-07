# Feature Specification: Comprobante y retiro del vehiculo

**Feature Branch**: `007-retiro-vehiculo` (directorio de especificacion; sin crear rama)
**Created**: 2026-11-06
**Status**: Implementado y desplegado con autorizacion; HP validada, termica real pendiente
**Input**: Implementar retiro con comprobante impreso y digital. Aclaraciones:
impresora termica USB de 80 mm; impresion automatica del documento al ingreso
desde Android o escritorio; entrega fisica confirmada por el encargado, sin
codigo ni escaneo y sin bloqueo por pago registrado (ultima decision del usuario).
Revision posterior: en Android, sustituir Retiro por Reimpresion del recibo y
eliminar la entrega de esta seccion; prevalece sobre el flujo de entrega descrito abajo.

## User Scenarios & Testing

### Revision Android: Reimpresion del recibo (Priority: P1)

La navegacion muestra Reimprimir con icono de impresora. La pantalla muestra
Reimpresion del recibo y permite consultar por placa, actualizar el recibo y
solicitar copias al servidor de impresion. No hay entrega en esta seccion.

**Acceptance Scenarios**:
1. **Given** un recibo consultado, **When** se abre la seccion, **Then** no
  muestra revision, confirmacion, fecha, encargado ni estado de entrega.
2. **Given** datos legados de entrega, **When** consulta o reimprime, **Then**
  no muestra esa entrega ni invoca su confirmacion y permite imprimir el recibo.
3. **Given** una impresion previa, **When** solicita otra copia, **Then**
  conserva el dialogo de confirmacion de copia y la recuperacion ante fallos.

### User Story 1 - Recibir comprobante (Priority: P1)

El cliente recibe un documento con placa, marca, servicios
y total confirmado por WhatsApp/correo o impreso, sin duplicar su ingreso.

**Why this priority**: Identifica el mismo ingreso en papel y digital.
**Independent Test**: Un ingreso confirmado conserva el mismo documento al consultar,
reimprimir y generar sus mensajes; un error de impresion no modifica el turno.
**Acceptance Scenarios**:
1. **Given** ingreso y servicios confirmados, **When** se solicita el comprobante,
   **Then** se obtiene el documento del ingreso confirmado.
2. **Given** el comprobante existente, **When** se consulta o reimprime,
  **Then** muestra datos y total confirmado actuales del mismo ingreso.
3. **Given** correo o WhatsApp manual, **When** se prepara la confirmacion,
  **Then** conserva el flujo digital existente sin enviar por si solo.

### User Story 2 - Imprimir comprobante (Priority: P1)

Al confirmar un nuevo ingreso y guardar todos sus servicios desde Android o
escritorio, se solicita automaticamente una unica impresion de 80 mm a la API
del PC Windows. Se conserva la reimpresion explicita y la vista previa.

**Why this priority**: Atiende clientes sin acceso al comprobante digital.
**Independent Test**: Vista previa y cancelacion no generan entrega ni otro codigo.
**Acceptance Scenarios**:
1. **Given** termica instalada, **When** se confirma imprimir,
  **Then** la API encola un comprobante para el controlador configurado y muestra
  su estado, sin asumir que enviado al controlador significa papel verificado.
2. **Given** cancelacion o impresora no disponible, **When** termina el intento,
   **Then** el comprobante sigue disponible y no se marca el vehiculo entregado.

### User Story 3 - Confirmar entrega (Priority: P1)

El encargado autenticado busca la placa, revisa la entrega fisica del vehiculo
a la persona y confirma expresamente, sin codigo, QR ni camara de retiro.

**Why this priority**: Evita entregar dos veces el mismo vehiculo.
**Independent Test**: Revision confirmada registra encargado y fecha una sola vez,
sin codigo y aun con solicitudes dobles; sin revision no registra entrega.
**Acceptance Scenarios**:
1. **Given** vehiculo y entrega fisica revisados, **When** confirma la revision,
   **Then** registra fecha/encargado y muestra entregado en ambas aplicaciones.
2. **Given** pago no registrado, **When** la revision esta confirmada,
   **Then** permite entregar sin crear pagos ni cambiar importes o banderas financieras.
3. **Given** revision no confirmada, **When** solicita entregar,
   **Then** rechaza la entrega sin cambiar datos.
4. **Given** vehiculo ya entregado, **When** se repite la confirmacion,
   **Then** conserva el registro original y no permite otra entrega.

### Edge Cases

- Fallo de conexion durante confirmacion: consultar el estado antes de reintentar;
  no asumir entrega correcta ni generar un nuevo codigo.
- Placa inexistente, sesion vencida o ingreso sin servicios: informar sin crear
  comprobantes incompletos ni exponer datos a usuarios no autenticados.
- Dos encargados confirman a la vez: una sola entrega persistida.
- Impresora sin controlador compatible: informar; conservar digital y vista previa.
- Comprobante perdido: la entrega no exige documento, codigo ni escaneo.
- Nuevos ingresos de la misma placa tienen comprobantes diferentes; mostrar el
  ingreso mas reciente y su fecha para revision del encargado.

## Requirements

### Functional Requirements

- **FR-001**: Emitir un solo comprobante por ingreso confirmado con servicios.
- **FR-002**: Conservar identificadores internos existentes por compatibilidad;
  no exigir codigo al entregar ni mostrar codigo o QR en el documento impreso.
- **FR-003**: Usar datos y total confirmado del mismo ingreso al reimprimir.
- **FR-004**: Mantener WhatsApp manual y correo existentes; no activar Meta.
- **FR-005**: Impresion USB centralizada en la API Windows mediante controlador
  configurado para papel nominal de 80 mm, limitada al area imprimible
  del papel del controlador; cancelacion/fallo no cambia comprobante o entrega.
- **FR-006**: Buscar por placa y confirmar retiro desde Android y escritorio con
  sesion existente y confirmacion explicita de entrega fisica por el encargado.
- **FR-007**: Registrar una sola fecha y encargado de entrega, sin borrar historial.
- **FR-008**: No exigir pago, cobrar, cerrar financieramente ni modificar las
  banderas actuales de pago/cierre/asignacion al entregar.
- **FR-009**: No enviar codigo introducido en URL ni registrar codigo o clientes
  en logs. Respuestas con datos de comprobante no se almacenan en cache.
- **FR-010**: Probar duplicados, concurrencia, errores, sesion, revision ausente,
  total confirmado y aislamiento de pagos; no usar base/clientes reales en pruebas.

### Key Entities

- **Comprobante de retiro**: ingreso unico, codigo, fecha de emision y estado.
- **Entrega**: fecha, encargado autenticado y revision confirmada; un solo registro
  asociado al comprobante, separado del estado de pago.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Todas las copias de un ingreso muestran su total
  confirmado vigente; sin cambios al turno, los valores coinciden entre copias.
- **SC-002**: Cero entregas sin revision confirmada del encargado.
- **SC-003**: Solicitudes simultaneas/repetidas conservan exactamente una entrega.
- **SC-004**: Entregar sin pago deja sin cambios todos los importes y pagos previos.
- **SC-005**: El encargado consulta y confirma entrega desde ambas aplicaciones.

## Revision historica de impresion central (2026-11-06)

El usuario confirmo boton Imprimir en Android y escritorio, con gestion de la
API en este PC Windows conectado a la impresora; no impresion automatica al
guardar ingresos. Autorizo una hoja diagnostica en HP Ink Tank 310 series USB001.
No hay termica instalada: esta prueba no acredita papel termico de 58/80 mm.

- Consultar comprobante permite imprimir cualquier ingreso confirmado,
  independientemente de si fue registrado desde movil o escritorio.
- Ambas aplicaciones solicitan impresion a la API autenticada. El servidor usa
  la impresora configurada, no una ruta o impresora arbitraria recibida del movil.
- Solicitud con UUID persistido: repetir la misma solicitud no crea otro trabajo.
  Conservar codigo y una copia del texto/valores al solicitar, sin modificar turno.
- Cola independiente del registro y retiro. Estados PENDING, SUBMITTING,
  SUBMITTED, FAILED y UNKNOWN; SUBMITTED significa enviado a Windows, no papel
  verificado. UNKNOWN exige revision del encargado, sin reenvio automatico.
- Si la API se muda a otro equipo, USB no se vuelve remoto por si solo: sera
  necesario mover el trabajador o compartir la impresora con permisos apropiados.
- Prueba HP ejecutada y confirmada por el usuario: una sola hoja completa y
  legible. Version central desplegada con autorizacion, configuracion local
  preservada y pruebas de API/escritorio/Android; termica fisica pendiente.
- El formulario libera el foco antes de consultar/confirmar para no ocultar
  avisos por desplazamiento automatico al campo de codigo.

## Assumptions

- La termica USB se conecta al PC y usa un controlador de Windows; no se asume
  protocolo ESC/POS, Bluetooth ni conexion USB directa al celular sin modelo.
- El administrador configura el destino en el servidor; las nuevas copias usan
  80 mm. Se respeta papel del controlador: HP usa hoja normal, no
  rollo termico. Vista previa no acredita salida fisica ni disponibilidad remota.
- No es una instantanea fiscal: consultar vuelve a leer datos/valores confirmados
  del turno; una modificacion posterior autorizada no regenera el codigo.
- Sesion y servidor son los existentes; no se incorporan pagos, excepciones
  por perdida ni cambios a la plantilla Meta en esta version.
- El despliegue/reinicio de la API operativa requiere autorizacion especifica
  para esta nueva funcionalidad; los binarios de prueba se generan separados.

## Revision vigente: solo documento y entrega fisica (2026-11-06)

Esta revision sustituye la decision previa de imprimir solo mediante boton y
retira la propuesta posterior de QR/codigo, cancelada expresamente por el usuario.

- **FR-011**: Ambos clientes solicitan automaticamente impresion tras confirmar
  un nuevo ingreso con todos sus servicios; nunca en actualizaciones ni antes
  de terminar el guardado. La API asigna una solicitud estable por turno para
  impedir copias automaticas duplicadas ante concurrencia o respuesta perdida.
- **FR-012**: Un fallo de impresion mantiene el ingreso confirmado y muestra
  aviso independiente; consultar o reimprimir no vuelve a registrar el vehiculo.
- **FR-013**: El documento no incluye QR ni codigo de retiro; se elimina el
  campo de codigo de Android/escritorio y la API solo exige revision confirmada
  para registrar entrega fisica. No incorporar nuevos datos de destinatario.
- **FR-014**: Impresion automatica y nuevas copias usan papel nominal de 80 mm;
  la termica usa un formato del controlador de ese ancho. En la HP actual se
  imprime una franja equivalente sobre hoja normal, sin fingir papel termico.
- **FR-015**: Android no muestra el aviso rutinario de comprobante de 80 mm
  solicitado en el formulario de ingreso. Conserva la confirmacion de ingreso,
  la impresion automatica y solo muestra aviso independiente para FAILED/UNKNOWN.
- **FR-016**: La seccion Android antes llamada Retiro se dedica a reimpresion
  del recibo. Eliminar controles, estado visible y accion de entrega de su modelo;
  conservar busqueda por placa, datos del recibo, impresion de 80 mm y consulta
  del trabajo. No borrar registros ni modificar contratos legados del servidor.
- **SC-006**: Ingresos confirmados de ambos clientes generan una sola solicitud
  automatica con todos los servicios; repetirla mantiene solicitud y codigo.
- **SC-007**: Confirmar entrega sin codigo funciona desde ambos clientes,
  conservando sesion, revision, fecha/encargado unicos y pagos intactos.
- El usuario autoriza una nueva hoja diagnostica en la HP disponible. No se
  crean turnos ficticios en la base operativa; se solicita confirmacion de papel
  y legibilidad y se mantiene pendiente la validacion con termica real.