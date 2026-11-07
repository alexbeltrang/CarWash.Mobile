# Feature Specification: Empresa del autolavado y recibos

**Feature Branch**: `008-empresa-facturacion` (directorio; sin crear rama)
**Created**: 2026-11-06
**Status**: Implementada, validada y desplegada con autorizacion. Facturacion electronica diferida.
**Input**: Registrar y modificar desde escritorio los datos del autolavado y su
logo para todo el aplicativo, separando la empresa de la marca CarWash del
software. El usuario confirma una sola empresa, edicion solo por administradores
y difiere expresamente facturacion electronica DIAN. Esta fase solo configura
la empresa y personaliza las aplicaciones, recibos y comunicaciones existentes.

## User Scenarios & Testing

### User Story 1 - Administrar la empresa (Priority: P1)

Un administrador registra y modifica en el escritorio la identidad comercial,
los datos de contacto y los datos tributarios de una sola empresa/autolavado.
Los demas usuarios no pueden modificar esa informacion.

**Why this priority**: Los documentos deben identificar al negocio que presta
el servicio, no al fabricante del software.
**Independent Test**: Guardar una empresa de prueba, recuperarla y modificarla
como administrador; comprobar que un usuario no administrador no pueda guardarla.
**Acceptance Scenarios**:
1. **Given** un administrador autenticado, **When** guarda informacion valida,
   **Then** conserva una sola ficha de empresa y puede editarla desde escritorio.
2. **Given** un usuario no administrador, **When** intenta modificar la empresa,
   **Then** se rechaza el cambio sin alterar los datos anteriores.
3. **Given** datos incompletos o logo invalido, **When** intenta guardar,
   **Then** informa los campos a corregir sin perder la ficha vigente.

### User Story 2 - Mostrar la identidad del negocio (Priority: P1)

Android, escritorio, recibos impresos y documentos digitales usan el nombre y
logo del autolavado; CarWash se conserva como identificacion del software, no
como razon social ni nombre del emisor.

**Why this priority**: Mantener una identidad coherente en los puntos de atencion.
**Independent Test**: Modificar nombre y logo en escritorio y comprobar que se
reflejan en las aplicaciones y en un nuevo recibo simulado de 80 mm.
**Acceptance Scenarios**:
1. **Given** empresa configurada, **When** se consulta su informacion actual,
   **Then** ambas aplicaciones y nuevos documentos muestran esa identidad.
2. **Given** empresa sin configurar, **When** se usa el software,
   **Then** no inventa un negocio ni usa CarWash como emisor fiscal.
3. **Given** logo ausente, **When** se consulta o imprime un recibo,
   **Then** el nombre y los datos mantienen legibilidad sin una imagen rota.
4. **Given** Android, **When** se consulta la empresa, **Then** no ofrece una
   accion para registrar o modificar sus datos.

### User Story 3 - Personalizar recibos sin cambiar su naturaleza (Priority: P1)

Los recibos y comunicaciones existentes muestran la empresa y sus datos, sin
convertirse en factura electronica ni cambiar cobros o importes.

**Why this priority**: Personalizar la atencion sin inventar obligaciones fiscales.
**Independent Test**: Cambiar empresa y comprobar texto/logo de un nuevo recibo
simulado; un trabajo ya encolado conserva los datos y total originales.
**Acceptance Scenarios**:
1. **Given** empresa configurada, **When** imprime un nuevo recibo, **Then**
  incluye identidad y contacto de la empresa y el total confirmado del ingreso.
2. **Given** un trabajo encolado, **When** cambia la empresa, **Then** ese
  trabajo conserva la identidad original; una copia nueva usa los datos actuales.
3. **Given** recibo personalizado, **When** se imprime, **Then** no usa
  numeracion fiscal, CUFE ni mensajes que afirmen validacion DIAN.

### Edge Cases

- Dos administradores editan la ficha a la vez: evitar sobrescribir silenciosamente.
- NIT/digito de verificacion, correo, domicilio o datos tributarios invalidos.
- Logo demasiado grande, formato no permitido o contenido que no sea imagen.
- Empresa o logo no disponibles por falta de conexion: no mostrar otro emisor.
- Cambio de datos empresariales despues de encolar una impresion.
- Pruebas sin clientes reales ni envios reales.

## Requirements

### Functional Requirements

- **FR-001**: Mantener una sola empresa para esta instalacion, con fuente comun
  de datos para escritorio, Android y documentos.
- **FR-002**: Permitir registrar/modificar desde escritorio exclusivamente a
  administradores autenticados y aplicar el permiso tambien al guardar.
- **FR-003**: Registrar nombre comercial del autolavado, razon social o nombre
  legal del emisor, tipo de persona, NIT y digito de verificacion, direccion,
  pais/departamento/municipio, telefono y correo de contacto/facturacion.
- **FR-004**: Registrar responsabilidades tributarias como texto informativo
  opcional suministrado segun RUT; no asignar responsabilidades, tarifas de IVA
  u obligaciones por suposicion ni solicitar configuracion fiscal electronica.
- **FR-005**: Permitir cargar, reemplazar y retirar el logo desde escritorio;
  validar imagen y limites, conservar legibilidad y no guardar rutas locales
  como referencia compartida con Android.
- **FR-006**: Mostrar identidad del autolavado en las aplicaciones, recibos de
  80 mm y documentos digitales. CarWash identifica al software, no al emisor.
  Android y escritorio muestran nombre/logo despues de autenticarse; antes del
  acceso permanece la identificacion del software. WhatsApp manual incorpora
  datos empresariales en texto: su enlace no adjunta el logo como imagen.
- **FR-007**: Mantener Android como consumidor sin controles de edicion de empresa.
- **FR-008**: Conservar impresion automatica de ingreso y reimpresion explicita;
  no reintroducir entrega ni codigo de retiro en la pantalla Android.
- **FR-009**: Facturacion electronica, habilitacion DIAN, numeracion fiscal,
  certificados, CUFE, QR fiscal e impuestos automaticos quedan fuera de esta fase.
- **FR-010**: Guardar responsabilidades tributarias como informacion suministrada
  segun RUT, sin inferir efectos fiscales ni validar habilitacion DIAN.
- **FR-011**: No cambiar pagos, turnos, servicios ni el momento de impresion.
- **FR-012**: Congelar los datos de empresa y logo en el trabajo de impresion;
  una copia nueva toma la empresa vigente y conserva el total confirmado.
- **FR-013**: El recibo personalizado sigue siendo recibo operativo, no factura.
- **FR-014**: No recopilar secretos fiscales ni credenciales en esta fase.
- **FR-015**: Probar permisos, cambios concurrentes, validacion de datos/logo,
  consistencia entre clientes/documentos y errores de conexion con datos aislados;
  no emitir documentos ni notificaciones reales para probar.

### Key Entities

- **Empresa**: Identidad comercial, identidad legal, domicilio, contacto y datos
  tributarios vigentes del unico negocio configurado.
- **Logo**: Imagen compartida del negocio, asociada a la ficha y administrada
  con los mismos permisos.
- **Recibo**: Documento operativo del ingreso con identidad de empresa y total
  confirmado; el trabajo de impresion mantiene una copia de sus datos y logo.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cero cambios de empresa realizados por usuarios no administradores.
- **SC-002**: Un cambio de identidad confirmado se refleja al recuperar los
  datos actuales en ambos clientes y nuevos documentos, sin emisor contradictorio.
- **SC-003**: Cero recibos presentados como facturas electronicas validadas.
- **SC-004**: Cambiar empresa no vuelve a registrar ingresos ni imprimir por si solo.
- **SC-005**: Editar empresa no cambia trabajos de impresion ya encolados.
- **SC-006**: Recibo de 80 mm con nombre largo y logo permitido conserva todos
  los datos legibles; Android permanece sin edicion empresarial ni entrega.

## Assumptions

- Una sola empresa y permisos administrativos existentes, confirmados por usuario.
- Identidad empresarial y emision fiscal son funcionalidades relacionadas pero
  separadas; habilitar una no prueba cumplimiento de la otra.
- El usuario aporta datos reales segun RUT; esta fase no solicita ni configura
  secretos o credenciales fiscales.
- No se integra ni automatiza el portal DIAN en esta fase.
- No se cambian pagos, turnos ni reglas tributarias; facturacion electronica diferida.
- Cambio del nombre/logo dentro del producto; no implica cambiar paquete Android
  ni sustituir el nombre tecnico CarWash en archivos o instalaciones del software.

## Referencias Fiscales Futuras (Fuera De Esta Fase)

- [Portal oficial DIAN](https://www.dian.gov.co/impuestos/factura-electronica/Paginas/inicio.aspx).
- [Sistema de Factura Electronica DIAN](https://micrositios.dian.gov.co/sistema-de-facturacion-electronica/).
- Antes de implementar emision, contrastar normativa vigente, habilitacion y
  documentacion tecnica del canal seleccionado. No se declara cumplimiento fiscal
  ni integracion DIAN implementada en esta especificacion.