# Contrato: comprobante y retiro

Todas las rutas bajo /api/RetiroVehiculos requieren Bearer de la sesion API
existente; usuarios operativos y administradores pueden actuar como encargados.
Cache-Control: no-store. No confiar en usuario/fecha proporcionados por clientes.

## Emitir o recuperar

POST /api/RetiroVehiculos/comprobante
JSON: { "idTurno": 10 } o { "placa": "ABC123" }, exactamente uno.
Por placa selecciona el ingreso mas reciente y muestra su fecha para revision.
201 con Location a GET si se emite; 200 si ya existia.
GET /api/RetiroVehiculos/{idTurno} consulta existente sin crear.
DTO: idTurno, codigo, placa, marca, cliente, fechaIngreso, total, servicios
[{nombre,precio}], entregadoEn, entregadoPor. Fechas ISO8601 con offset UTC
para entrega; fechaIngreso preserva semantica local existente con offset servidor.
400 entrada invalida, 401 sesion requerida, 404 ingreso/comprobante inexistente,
409 ingreso sin servicios confirmados. Datos y precios del turno/catalogo actuales.
503 almacenamiento no disponible; ante respuesta incierta consultar antes de
repetir. Nunca mostrar el cuerpo HTTP como diagnostico al usuario.

## Entregar (legado; no usado por la pantalla Android)

POST /api/RetiroVehiculos/{idTurno}/entrega
JSON: { "revisionConfirmada": true }.
La revision previa elimino codigo y escaneo para la entrega fisica.
La pantalla Android vigente es solo de reimpresion y no invoca esta ruta ni
muestra entregadoEn/entregadoPor. Se conserva el contrato servidor por compatibilidad.
Codigo es un campo legado opcional ignorado, no una credencial de entrega.
200 devuelve comprobante entregado. 400 revision ausente/no confirmada;
401 sin sesion; 404 inexistente; 409 entrega previa.
Una actualizacion atomica evita doble entrega y conserva fecha/usuario iniciales.
El cliente consulta tras respuesta incierta antes de intentar nuevamente.
No exigir Pagado ni modificar Turnos.Estado, pagos, importes o asignacion.

## Digital e impresion

Correo HTML/texto y WhatsApp manual contienen el codigo emitido por servidor;
un error de comprobante es independiente del registro del turno y no habilita
un mensaje incompleto como comprobante de retiro. No cambiar plantilla Meta.
Escritorio consulta y muestra vista previa sin envio local. Imprimir en ambos
clientes solicita la cola central de la API Windows; cancelar/reimprimir no
ejecuta entrega. Pruebas normales simuladas; una hoja diagnostica HP USB fue
autorizada y confirmada por el usuario, sin usar turnos/clientes reales.
Android muestra solo la fecha de ingreso en la zona horaria local del dispositivo; el
servidor conserva la fecha de entrega UTC y el encargado de la primera entrega.

## Solicitar impresion central (revision 2026-11-06)

POST /api/RetiroVehiculos/{idTurno}/impresiones
JSON: { "solicitudId": "6b1f67d0-584a-4a95-88dd-c61c43d61fe5", "anchoMm": 80 }.
Bearer existente; nuevas solicitudes solo de 80 mm. No aceptar nombre/ruta de impresora ni texto del
cliente. 202 Accepted con Location a GET del trabajo, tambien para repeticion
de la misma solicitud compatible. UUID incompatible con otro turno/ancho: 409.
GET /api/RetiroVehiculos/impresiones/{solicitudId} devuelve trabajo existente.
GET /api/RetiroVehiculos/{idTurno}/impresiones/ultima devuelve ultimo trabajo,
200 con DTO o 204 sin trabajos. Desempate por orden de insercion, no UUID aleatorio.
DTO: solicitudId, idTurno, anchoMm, impresora, estado, creadoEn, actualizadoEn,
errorCodigo. Fechas UTC ISO8601. Cache-Control no-store; no incluir texto/codigo.
400 entrada invalida; 401 sin sesion; 404 inexistente; 503 impresion deshabilitada
o almacenamiento no disponible. No mutar entrega ni datos financieros.

Servidor Windows con impresora configurada procesa cola persistida; PENDING
puede procesarse una vez. SUBMITTING se cambia a UNKNOWN al recuperar un arranque
interrumpido; no reenviar automaticamente UNKNOWN/SUBMITTED. Errores seguros
previos al envio: FAILED. SUBMITTED solo acredita envio al controlador Windows.
Los clientes conservan UUID si se pierde respuesta y consultan/reutilizan esa
solicitud antes de crear otra. Reimpresion requiere nueva accion explicita.
Vista previa sigue local; Imprimir en ambos clientes solicita la cola del servidor.

Configuracion local de servidor fuera de Git: `%PROGRAMDATA%/SplashCar/printing.json`
con seccion Printing (Enabled, PrinterName). API debe reiniciarse para aplicarla.
No requiere credenciales nuevas de cliente. El proceso/usuario de la API debe
tener acceso al controlador Windows. AnchoMm limita contenido dentro del area
imprimible; usa el papel configurado en el controlador, sin asumir rollo termico.
Destino actual autorizado: HP Ink Tank 310 series, USB001.

## Impresion automatica de ingreso (revision vigente 2026-11-06)

POST /api/RetiroVehiculos/{idTurno}/impresiones/ingreso
JSON: { "idServicios": [1, 2] }. Bearer existente.
Ambos clientes llaman automaticamente solo despues de confirmar nuevo turno y
todos los servicios. La API compara servicios activos, asigna UUID estable por
turno y encola una sola copia de 80 mm. Repetir devuelve el mismo trabajo, aun
si ya termino; no reinicia FAILED/UNKNOWN/SUBMITTED. 202 con Location, 400 datos
invalidos, 409 servicios incompletos, 401 sin sesion, 503 impresora no configurada.
No bloquear ni repetir el guardado ante fallo de impresion; consultar estado o
solicitar otra copia expresamente. Actualizaciones no imprimen automaticamente.
Android oculta el aviso rutinario de impresion en el formulario; conserva la
confirmacion de ingreso y advertencias FAILED/UNKNOWN. Sin cambios al contrato HTTP.
Documento sin QR/codigo; texto y valores congelados al solicitar. Mantener
lectura de trabajos antiguos y DTO de comprobante legado por compatibilidad.
Termica usa formato nominal de 80 mm del controlador; HP imprime franja de
contenido de ese ancho en hoja normal, no acredita papel termico.