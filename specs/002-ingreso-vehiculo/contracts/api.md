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
  numeroOrden siempre es la cadena "0": el formulario no lo muestra ni lo exige.
  Se mantiene el tipo y nombre del contrato, sin cambios en API o base por esta
  simplificacion solicitada el 2026-10-28.
6. Por servicio POST `/api/TurnoServicios/RegistrarTurnoServicio` con
   {idServicios, idTurno, isDeleted:false}; devuelve 201.

Importes JSON numericos decimales, no cadenas con formato monetario.
El registro solo es completo despues de todos los pasos.

## Correccion de identidad de servicios - 2026-10-28

El usuario reporto turno visible con advertencia de envio incompleto. La tabla
SQLite TurnoServicios tenia idTunoServicios sin PRIMARY KEY y 395 IDs NULL.
EF espera una clave generada al insertar: la asociacion puede persistirse pero
fallar la lectura de INSERT RETURNING; GET de servicios devolvia HTTP 500.
El usuario autorizo corregir API/base, respaldar y reiniciar si es necesario.

Reparacion externa completada en CarWashApi/Database/Repairs mediante SQL y prueba
PowerShell aislada. Tras respaldo SQLite consistente fuera de Git, se reconstruyo
la tabla con INTEGER PRIMARY KEY AUTOINCREMENT y el indice TurnoServicios_IdTurno.
Se conservaron las 395 asociaciones, sin IDs nulos; sus campos de negocio y
cantidades coinciden con el respaldo. PRAGMA quick_check devolvio ok.
La prueba aislada conserva IDs existentes, genera IDs mediante INSERT RETURNING
y rechaza una segunda reparacion. API reiniciada con perfil wifi: GET de servicios
devuelve HTTP 200 y 395 IDs validos en localhost y en la direccion LAN.
No se enviaron ingresos ficticios ni se modificaron endpoints o codigo productivo
Android. Pruebas JVM y Compose verifican dos guardados completos consecutivos,
limpieza del formulario y del marcador; los fallos reales conservan la proteccion.

La reparacion no agrega servicios que un envio interrumpido no alcanzo a enviar.
Revisar el turno anterior y sus servicios en el computador antes de limpiar
explicitamente el marcador pendiente; no reenviar ese ingreso desde el movil.
El movil mantiene limpieza al completar todas las respuestas correctas y mensajes
solo de error/incompleto cuando hay fallo real; no esconder errores del servidor.

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
- El perfil de la Wi-Fi era Publico durante el diagnostico; el agente no modifico
  perfil ni firewall. El usuario posteriormente confirmo JSON desde el navegador
  del celular y un turno registrado desde la app. No se certifica el estado actual
  del firewall o perfil; mantener acceso limitado a red privada confiable.
- Configuracion manual persistida, no descubrimiento automatico ni barrido de red.