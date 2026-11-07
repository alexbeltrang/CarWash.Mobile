# Datos y estados

ComprobantesRetiro: IdTurno (PK), Codigo (unico), CreadoEn (UTC),
EntregadoEn (UTC nullable), EntregadoPor (usuario nullable).
Relacion logica uno a uno con Turnos; no modificar estructura de Turnos/pagos.

SIN_COMPROBANTE -> VIGENTE tras confirmar turno y servicios.
VIGENTE -> ENTREGADO con revision de la entrega fisica por el encargado autenticado,
sin codigo ni escaneo. Codigo existente es legado interno/digital por compatibilidad,
no una credencial de entrega; no se imprime ni se muestra en el formulario de retiro.
ENTREGADO es terminal; no borrar ni regenerar por reimpresion/reintento.
Generacion insert-on-conflict por IdTurno y entrega condicional atomicas.
Datos del comprobante corresponden al turno y catalogo actuales, no un documento
fiscal ni una factura; el total es Turnos.Valor, no suma de catalogo.

El cliente no envia fecha ni usuario de entrega. Codigo no se registra en logs,
URL, preferencias ni almacenamiento del celular. Datos personales solo en memoria
de la vista, el servidor existente y el canal de comprobante solicitado.

## Cola de impresion central

ImpresionesComprobante: SolicitudId (UUID canonico, PK), IdTurno, AnchoMm,
SolicitadoPor (usuario de sesion), Impresora (destino configurado), Texto (copia
del comprobante al solicitar), Estado, CreadoEn/ActualizadoEn UTC y ErrorCodigo
seguro. Indice por IdTurno/CreadoEn; ultimo trabajo con desempate por rowid.
Texto solo en SQLite servidor; DTO de estado no expone texto ni codigo/cliente.
La misma solicitud no altera la copia aunque despues cambie el turno/catalogo.

PENDING -> SUBMITTING mediante actualizacion condicional atomica.
SUBMITTING -> SUBMITTED si retorna el controlador, FAILED ante rechazo seguro
previo o UNKNOWN si el resultado es incierto. Recuperacion de SUBMITTING tras
reinicio -> UNKNOWN, sin reenvio. Nueva copia exige accion explicita y UUID nuevo.
SUBMITTED no es confirmacion fisica de papel. No afecta entrega ni pagos.

## Solicitud automatica al ingreso

Al confirmar turno y todos sus servicios, Android/escritorio llaman EntryAsync
con los identificadores de servicios confirmados. Se verifica el conjunto activo
en una transaccion y se calcula un UUID estable desde SHA256 de un prefijo de
version y el IdTurno, solo en servidor. La PK existente asegura una copia
automatica por turno sin agregar columnas ni modificar tablas de negocio.
AnchoMm=80 para nuevas solicitudes; Texto conserva datos y total confirmados,
sin codigo ni QR. Repetir una solicitud automatica devuelve estado existente,
sin reenvio ni cambio del texto. Actualizaciones no generan copia automatica.