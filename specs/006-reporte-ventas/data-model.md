# Data Model: Reporte de ventas por fechas

**Date**: 2026-11-03
**Scope**: modelo de lectura y sesion temporal; sin migracion de tablas de negocio.

## Entidades existentes y relaciones

| Fuente | Campos usados | Relacion y regla |
|--------|---------------|------------------|
| TurnosMovimientos | IdTurnoMovimientos, IdTurno, IdFormaPago, FechaMovimiento, MontoPagado | Una fila por cobro; entidad raiz, sin filtrar por estado del turno |
| Turnos | IdTurno, Placa, IdTipoVehiculo, idOperario | LEFT JOIN desde IdTurno; no sustituir importe ni fecha de movimiento |
| FormaPago | IdFormaPago, Nombre | LEFT JOIN por id; conservar historicos aunque inactivos |
| Operarios | idOperario, Nombres, Apellidos | LEFT JOIN por idOperario nullable del turno; no restringir a disponibles |
| TipoVehiculo | IdTipoVehiculo, Nombre | LEFT JOIN por IdTipoVehiculo; no excluir tipos inactivos |

No unir TurnoServicios al conjunto monetario. Un turno puede tener varios cobros y
varios servicios; no se colapsan cobros ni se multiplican sus importes. Si falta el
turno o un catalogo, mantener el movimiento con sus claves e informacion descriptiva
null. No agregar estados de anulacion ni interpretar Credito como ausencia de cobro.
Operario es el asignado actualmente al turno: el modelo no conserva una fotografia
historica de asignacion al cobrar y este reporte no inventa ese historial.

## Criterios de consulta

| Campo | Tipo logico | Validacion |
|-------|-------------|------------|
| fechaInicial | DateOnly / LocalDate | Obligatorio, ISO yyyy-MM-dd |
| fechaFinal | DateOnly / LocalDate | Obligatorio, mayor o igual a inicial; rechazar extremos no representables al calcular el dia siguiente |
| idFormaPago | int? | Omitido = todas; si presente, positivo |
| idOperario | int? | Omitido = todos; si presente, positivo |
| placa | string? | Trim y mayusculas; blanco = null; coincidencia completa con dato igualmente normalizado |
| idTipoVehiculo | int? | Omitido = todos; si presente, positivo |
| pageSize | int | Predeterminado 50, de 1 a 200; es paginacion, no filtro de negocio obligatorio |
| cursor | string? | Ausente en primera pagina; fecha local, id y huella de criterios validos |

Fechas comparadas sobre FechaMovimiento local con intervalo [inicio, fin + 1 dia),
sin conversion por zona del telefono. Todas las restricciones presentes se combinan
con AND. Un id positivo inexistente produce un resultado vacio, no se ignora.
No imponer un limite arbitrario al numero de dias. Limpiar opcionales no cambia fechas.

## MovimientoVenta: proyeccion de lectura

| Campo de respuesta | Tipo logico | Origen |
|--------------------|-------------|--------|
| idMovimiento | int | IdTurnoMovimientos |
| idTurno | int | IdTurno del movimiento |
| fechaCobro | DateTimeOffset / string ISO con offset | FechaMovimiento interpretada en zona operativa configurada |
| placa | string? | Turnos.Placa |
| idTipoVehiculo, tipoVehiculo | int?, string? | Clave del turno y descripcion opcional |
| idOperario, operario | int?, string? | Clave del turno; nombre y apellidos sin espacios sobrantes |
| idFormaPago, formaPago | int, string? | Clave de movimiento y descripcion opcional |
| montoPagado | decimal / BigDecimal | MontoPagado registrado, sin recalcular tarifas |

En el JSON nuevo, montoPagado e importeTotal se transmiten como cadenas decimales
invariantes, sin agrupadores ni simbolo monetario y sin redondear precision almacenada.
La presentacion usa formato monetario existente. No agregar float/double a la ruta
de dinero ni conversion de almacenamiento. Descripciones null se muestran Sin informar.

La fecha almacenada permanece DateTime local con precision de milisegundos. Solo el
DTO expone offset, calculado para esa fecha en la zona operativa de la API. Configurar
BusinessTimeZoneId no secreto o usar zona local del servidor alineada con escritorio;
no codificar un offset fijo ni aplicar la zona del telefono. Si un dato temporal es
invalido o ambiguo sin convencion resoluble, devolver problema explicito de datos,
no atribuirle una fecha inventada.

## ResultadoVentas

- criterios: fechas y opcionales normalizados efectivamente usados, sin cursor.
- cantidadCobros: long del conjunto completo filtrado, antes de paginar.
- importeTotal: suma decimal de esos mismos movimientos; conjunto vacio = cero.
- items: pagina ordenada por FechaMovimiento DESC, IdTurnoMovimientos DESC.
- nextCursor: cursor de la ultima fila si existen mas filas; null al finalizar.
- pageSize: tamano aplicado.
- consultadoEn: DateTimeOffset UTC del resultado; no es una version de snapshot.
- fechaOperativa: fecha local del negocio para el contexto de la consulta.

Cada respuesta obtiene resumen y pagina en una transaccion SQLite corta de lectura.
Se pueden consultar los agregados en SQL usando Sum(decimal), traducido a ef_sum
por EF Core 9.0.14. El cursor solo restringe items, no cantidadCobros ni importeTotal.
El cursor codifica la fecha local original con milisegundos, id de movimiento y
huella de criterios; no contiene credenciales ni concede permisos.

Entre paginas el reporte es vivo: si cambia cantidad o importe, el cliente descarta
la continuacion y pide Actualizar. Una edicion concurrente que conserve agregados
puede no detectarse; no se promete recorrido congelado ni cierre contable. Con datos
estables el recorrido devuelve todos los movimientos una sola vez.

## Opciones de filtros

Tres listas de pares id/nombre nullable: formasPago, operarios y tiposVehiculo.
Proceden de movimientos del periodo antes de aplicar opcionales y de sus relaciones
descriptivas, sin excluir catalogos retirados. Claves historicas positivas se pueden
conservar aunque falte la descripcion. No se inventan claves para datos ausentes.
Seleccion opcional no disponible tras cambiar periodo se mantiene hasta que el usuario
la quite: nunca ampliar silenciosamente una consulta eliminando su filtro.

## Sesion temporal de API

Registro efimero, sin tabla ni migracion:

| Campo interno | Tipo | Regla |
|---------------|------|-------|
| tokenHash | SHA-256 | Clave de 32 bytes aleatorios emitidos; no conservar token plano en registro |
| userId | int | Identidad obtenida de login, no del cliente de reporte |
| createdAtUtc | DateTimeOffset | Reloj TimeProvider; origen del limite absoluto |
| lastSeenAtUtc | DateTimeOffset | Solo se actualiza al validar sesion no expirada |
| absoluteExpiresAtUtc | DateTimeOffset | createdAt + 8 horas por defecto |

Limite inactivo: 30 minutos por defecto; comparar ambos limites antes de actualizar
lastSeen. Limpieza periodica y operaciones concurrentes seguras. Maximo 10000 sesiones
vivas; rechazar nuevo login con 503 si la capacidad esta ocupada, sin expulsar sesiones
vigentes. Reiniciar API elimina sesiones y obliga a reingresar. Usuario eliminado
invalida la sesion; rol vigente se consulta en cada solicitud y no se toma del cache.

Los clientes guardan el token solo en memoria junto con su origen autenticado;
AndroidSessionStore conserva sus metadatos existentes pero no el token. No hay
refresh token ni login automatico con contrasenas guardadas. HTTPS recomendado;
el transporte HTTP privado permitido sigue siendo un riesgo de escucha en LAN.

## Transiciones de estado

Sesion: sin token -> login valido -> activa -> expirada/revocada -> reingreso.
Logout local elimina token inmediatamente. Logout recibido por API revoca; sin red
no se declara revocacion remota. Cambio a perfil no administrador produce 403 y limpia
el reporte del cliente; no convierte sus datos locales en permiso del servidor.

Pantalla: SinConsulta -> Cargando -> Resultados o Vacio o Error. Editar controles
marca resultados anteriores, pero no cambia sus criterios aplicados. Cada consulta
tiene numero de generacion y Job; respuesta de otra generacion se ignora. Paginando
no duplica llamadas ni filas por idMovimiento. 401 -> Reingreso; 403 -> SinPermiso;
logout/cambio de usuario/origen limpian estado del reporte. Rotacion conserva ViewModel;
no se guardan token ni resultados en preferencias o SavedState.