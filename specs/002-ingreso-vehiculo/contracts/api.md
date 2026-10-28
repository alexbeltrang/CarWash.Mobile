# Contrato API verificado - 2026-10-28

Fuente: OpenAPI del servidor en puerto 5197 y controladores de CarWashApi.
No se modifican endpoints ni autenticacion.

La captura de 003-captura-placa no agrega endpoints ni envia fotografias:
solo una placa revisada alimenta las lecturas por placa aqui descritas.
Confirmar una lectura no inicia la secuencia de escrituras.

## Lecturas

- GET `/api/TipoVehiculo/GetAll`: lista {idTipoVehiculo, nombre, isDelete}.
  Excluir isDelete=true.
- GET `/api/Servicios/GetListaCompletaServicios`: lista {idServicio, nombre,
  idTipoVehiculo, precio, precioBaseComision}. Precios nulos equivalen a cero.
- GET `/api/CajaDiaria/getCajaActiva`: {idCaja, estado, ...}; 404 sin caja.
  El servidor usa estado=true, sin comprobar fecha; el movil respeta este criterio.
- GET `/api/Turnos/BuscarPorPlaca?placa=...`: ultimo vehiculo
  {idTipoVehiculo, nombreCliente, numeroCelular, marca, ...}; 404 si no existe.
- GET `/api/Turnos/HistorialPorPlaca?placa=...`: lista {idTurno,
  fechaHoraIngreso, nombreCliente, placa, tipoVehiculo, valorPagado, marca}.
  404 equivale a historial vacio. Este servidor no entrega servicio; mostrarlo
  solo si aparece, sin inventar ni consultar otros registros.
- GET `/api/TurnosDiarios/ByFecha?fecha=yyyy-MM-dd`: lista de {id, fecha, consecutivo}.

## Escrituras existentes

1. Reconsultar caja; si no existe no escribir.
2. Consultar consecutivo del dia local, usar primero como el formulario original.
3. Sin fila: POST `/api/TurnosDiarios/RegistrarTurnoDiario` con
   {id:0, fecha: fecha local a medianoche, consecutivo:1}; devuelve 201 y entidad.
4. Con fila: PUT `/api/TurnosDiarios/ActualizarTurnoDiario/{id}` con id, fecha
   del dia y consecutivo+1; devuelve 204.
5. POST `/api/Turnos/RegistrarTurno` con numeroTurno T-yyyyMMdd-NNN,
   nombreCliente, numeroCelular, placa, fechaHoraIngreso local ISO, marca,
   numeroOrden, valor, valorBaseComision, observaciones, idTipoVehiculo,
   idCajaDiaria, pagado=false, estado=false y operadorOcupado=false.
   Devuelve 201 y objeto {idTurno, numeroTurno, ...}.
6. Por servicio POST `/api/TurnoServicios/RegistrarTurnoServicio` con
   {idServicios, idTurno, isDeleted:false}; devuelve 201.

Importes JSON numericos decimales, no cadenas con formato monetario.
El registro solo es completo despues de todos los pasos.

## Riesgos heredados y seguridad

- No hay transaccion ni idempotencia entre solicitudes; pueden quedar
  consecutivos consumidos, turnos parciales o duplicados por clientes concurrentes.
- No reintentar escrituras automaticamente, ni borrar datos como compensacion.
  Ante fallo posterior a inicio de escritura, bloquear envio hasta revision por
  el usuario y limpieza explicita; guardar marcador local para reinicio del proceso.
- No seguir redirecciones HTTP: el servidor debe responder directamente en la URL configurada.
- Guardados de pruebas solo en servidor simulado. No escribir en base real.
- HTTP local no cifra datos personales. Usar solo red privada confiable;
  HTTPS y una operacion atomica en servidor son recomendaciones, no requisitos
  implementados ni garantias que tenga la API actual.

## Red

- localhost:5197 funciona solo en el computador servidor.
- 10.0.2.2:5197 representa el host desde Android Emulator.
- Celular: URL con IP Wi-Fi del servidor, mismo segmento y sin aislamiento AP.
- Servidor inicialmente en loopback. Tras autorizacion del usuario se agrego
  perfil wifi (localhost y 192.168.1.15:5197) y se reinicio la API existente.
  Ambas direcciones devolvieron 200; Android conecto usando la direccion LAN.
- El perfil de la Wi-Fi en Windows es Publico; no se modifico ni se altero el
  firewall. El usuario debe revisar confianza de la red y permiso TCP 5197 en
  perfil privado. No se ha verificado acceso desde un celular fisico.
- Configuracion manual persistida, no descubrimiento automatico ni barrido de red.