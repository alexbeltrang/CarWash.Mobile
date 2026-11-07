# Data Model: Factura

ConfiguracionFacturacion Id=1, DatosJson, Revision, SiguienteNumero.
DTO: habilitada, prefijo (hasta20), numeroResolucion (hasta80), fechaResolucion,
vigenteDesde/vigenteHasta ISO fecha, desdeNumero/hastaNumero/siguienteNumero
1..999999999, tasaIva nullable obligatoria 0..100, revision.
No retroceder consecutivo sobre numeros ya emitidos para el mismo prefijo.
Cada edicion incrementa revision; emision incrementa solo contador.

Factura: Id UUID PK, IdTurno UNIQUE, Prefijo+Numero UNIQUE, Fecha/Placa congeladas
para consulta y DatosJson snapshot.
Snapshot incluye empresa/logo, configuracion, comprador, fecha operativa,
placa, concepto de servicios, base, IVA y total, texto imprimible.
Inmutable; una factura por turno. No editar ni eliminar documentos emitidos.
Snapshot de impresion CARWASH-INVOICE-1 incluye identidad UUID, texto y logo;
ultima impresion solo considera trabajos de factura, no recibos de ingreso.

Comprador: nombre obligatorio hasta160, tipoDocumento CC/NIT/CE/TI/PAS/PEP/PPT,
numeroDocumento obligatorio hasta30; domicilio/telefono/correo opcionales limitados.
Texto sin controles, correo valido cuando informado. Sin identidad inventada.

Preparacion: turno pagado/finalizado, nombre/telefono/correo existentes como
sugerencia, servicios realizados y total, revisiones de empresa/configuracion.
Emision exige revisiones coincidentes para evitar cambios fiscales silenciosos.
Base=Round(total/(1+tasa/100),2,AwayFromZero); IVA=total-base, total confirmado.