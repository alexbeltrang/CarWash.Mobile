# Contract: Factura en papel

Todos los endpoints Bearer ApiSession existente, Cache-Control:no-store.
GET /api/Facturas/configuracion: 200 DTO o 204 sin configuracion.
PUT /api/Facturas/configuracion: administrador, DTO completo/revision; 200.
Comprador exige nombre/tipo/numero documento; direccion/contacto opcionales.

Provisionamiento: scripts SQL separados para SQLite y SQL Server, solo esquema,
sin INSERT de usuarios/empresa/autorizacion/facturas. Incluyen restricciones e
indices del esquema existente. No alteran la base operativa ni habilitan SQL
Server en la API, cuyo inicio/consultas siguen siendo especificos de SQLite.
GET /api/Facturas/preparacion?idTurno=N o placa=ABC123: 200 turno finalizado,
datos sugeridos comprador, total/concepto y revisiones. Requiere un selector.
GET /api/Facturas: hasta100 documentos mas recientes; placa opcional filtra antes
del limite para recuperar facturas anteriores incluso con autorizacion vencida.
Sin cuerpos en logs.
GET /api/Facturas/turnos/{id}: 200 snapshot o 204 si no emitida.
POST /api/Facturas/turnos/{id}: solicitudId UUID, comprador, empresaRevision,
configuracionRevision; 201 documento, repetir UUID/turno devuelve documento original.
Otro UUID sobre turno ya facturado:409, consultar original. No alterar factura.
POST /api/Facturas/{id}/impresiones: solicitudId UUID/anchoMm80; 202 job existente
de impresora Windows, reutilizar snapshot de factura. Recuperacion via GET
/api/RetiroVehiculos/impresiones/{uuid}; ninguna credencial/printer cliente.
GET /api/Facturas/{id}/impresiones/ultima: 200 ultimo job exclusivo de factura
o 204; no mezcla impresiones de ingreso. Identidad de factura incluida en snapshot.

400 validacion;401 sin sesion;403 configuracion no administrador;404 turno/factura
ausente;409 pendiente de pago, revision, autorizacion/vigencia/rango o duplicado;
503 dependencia no disponible. Errores seguros, sin datos de compradores.

Escritorio: tras cobro completo confirmado pregunta "Desea generar la factura?",
Si/No y Button2 (No) predeterminado. No cierra registro sin formulario/factura/
impresion. Si abre formulario comprador; nombre/tipo/numero requeridos, direccion
opcional. No hay segunda confirmacion de emision para el turno aceptado. Impresion
explicita desde factura, sin check ni impresion automatica. Menu Facturas para
personal autenticado; Configuracion facturacion
solo administrador. Al finalizar cobro abrir preparacion con IdTurno confirmado.
Cancelar/errores de facturacion no repiten pago. Emision manual explicita tras comprador,
historial y vista previa; imprimir/otra copia con revision de estado y confirmacion.
Android no factura. Contrato de recibos automaticos de ingreso permanece intacto.