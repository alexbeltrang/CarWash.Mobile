# Feature Specification: Captura de placa con camara

**Feature Branch**: Sin crear rama; directorio 003-captura-placa.
**Created**: 2026-10-28
**Status**: Implementada y verificada en emulador; precision con placas reales pendiente en celular.
**Input**: Capturar la placa con la camara del celular y continuar con el ingreso
existente; permitir registro manual cuando no sea legible.

## User Scenarios & Testing

### User Story 1 - Capturar y confirmar placa (Priority: P1)

El recepcionista abre la camara desde el ingreso, captura una placa y revisa el
resultado antes de continuar con el formulario existente.

**Why this priority**: Reduce digitacion sin convertir una lectura erronea en un ingreso.
**Independent Test**: Capturar texto de prueba y confirmar la placa; verificar que
se consulta el vehiculo pero no se registra ningun ingreso.

**Acceptance Scenarios**:
1. **Given** permiso concedido, **When** abre captura, **Then** ve la camara trasera
   y puede capturar, enfocar, iluminar si existe flash, o volver al ingreso manual.
2. **Given** una lectura con posibles placas, **When** revisa el resultado,
   **Then** puede elegir y corregir la placa; aun no cambia el formulario ni guarda.
3. **Given** placa revisada, **When** confirma, **Then** completa placa y busca sus
   datos por el flujo existente; servicios, validaciones y guardado siguen vigentes.
4. **Given** una captura cancelada, **When** vuelve, **Then** conserva el borrador anterior.

### User Story 2 - Continuar manualmente (Priority: P1)

El recepcionista puede escribir la placa sin depender de camara o reconocimiento.

**Why this priority**: Una placa deteriorada o un permiso denegado no deben impedir operar.
**Independent Test**: Denegar permiso o simular lectura vacia y completar ingreso manual.

**Acceptance Scenarios**:
1. **Given** permiso denegado o sin camara, **When** abre captura, **Then** informa
   el estado y ofrece ingreso manual; no vuelve a solicitar permiso automaticamente.
2. **Given** lectura vacia o error, **When** finaliza captura, **Then** ofrece
   reintentar o escribir la placa, sin vaciar el formulario anterior.
3. **Given** ingreso manual, **When** registra, **Then** usa las mismas validaciones
   y protecciones de envio de la funcionalidad 002.

### Edge Cases

- Lecturas multiples: no seleccionar silenciosamente otra placa; confirmar siempre.
- Caracteres ambiguos O/0, I/1: no sustituirlos automaticamente.
- Sin conexion: reconocer localmente; consultar y guardar requieren la API existente.
- Rotacion o segundo plano: liberar la camara al salir y no aplicar resultados tardios;
  mantener texto de revision durante rotacion, no persistir fotos.
- Doble toque de captura: una operacion a la vez; cancelar no registra.
- Fuente grande, modo oscuro y horizontal: contenido desplazable y controles accesibles.
- Guardado en curso o envio incierto: no permitir reemplazar placa mediante camara.

## Requirements

### Functional Requirements

- **FR-001**: Ofrecer captura de placa e ingreso manual en el formulario operativo,
  sin sustituir el formulario por una pantalla explicativa.
- **FR-002**: Solicitar camara solo al abrir captura; no solicitar almacenamiento,
  microfono ni ubicacion; informar permiso denegado y permitir abrir ajustes.
- **FR-003**: Mostrar vista previa y controles de captura, enfoque y linterna segun hardware.
- **FR-004**: Reconocer texto localmente sin subir, guardar en galeria ni registrar
  fotografias o textos reconocidos en logs propios.
- **FR-005**: Presentar hasta ocho sugerencias unicas y un campo editable; exigir
  confirmacion explicita antes de aplicar la placa al formulario.
- **FR-006**: La ausencia de sugerencias no debe bloquear placa manual, ni imponer
  un formato de un pais al registro ya existente.
- **FR-007**: Confirmar solo completa placa y consulta datos si hay conexion; nunca
  registra automaticamente. El boton Guardar existente conserva su responsabilidad.
- **FR-008**: Conservar borrador al cancelar y descartar resultados de una captura abandonada.
- **FR-009**: Ofrecer estados de permiso, carga, lectura vacia, error y revision;
  permitir reintentar o volver a manual en todos los fallos recuperables.
- **FR-010**: Conservar API, reglas, caja activa, servicios, valores y proteccion
  contra doble envio o registro incierto del ingreso existente.
- **FR-011**: Al confirmar una placa distinta, limpiar datos y servicios del vehiculo
  anterior antes de consultar; conservar orden y observaciones del ingreso.

### Key Entities

- **Captura temporal**: imagen solo en memoria durante reconocimiento.
- **Sugerencia de placa**: texto candidato, no validacion legal ni certeza de lectura.
- **Placa confirmada**: texto revisado que alimenta la placa del ingreso existente.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Capturar o confirmar una placa produce cero escrituras de ingreso.
- **SC-002**: En los casos permiso denegado, lectura vacia, fallo o cancelacion,
  el usuario puede continuar manualmente conservando el borrador.
- **SC-003**: La placa confirmada es exactamente la revisada, en mayusculas y sin
  reemplazos silenciosos entre letras y numeros.
- **SC-004**: El flujo de registro existente mantiene sus pruebas de validacion,
  guardado y bloqueo por envio incierto.
- **SC-005**: Una imagen de texto controlada permite verificar reconocimiento local;
  la precision con placas reales requiere validacion posterior en celular.

## Assumptions

- Se incorpora captura al ingreso existente, no un registro paralelo ni endpoints nuevos.
- El reconocimiento sugiere texto alfanumerico; no se presupone jurisdiccion,
  patron legal, exactitud garantizada ni reconocimiento automatico en movimiento.
- Una fotografia voluntaria por intento evita consumo continuo y resultados inestables.
- No se archivan imagenes, no se incorporan nube, analitica propia ni credenciales.
- No se escriben ingresos ficticios en datos reales para verificar la funcionalidad.