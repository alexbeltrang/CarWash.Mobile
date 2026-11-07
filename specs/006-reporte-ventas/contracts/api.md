# API Contract: Reporte de ventas y sesion temporal

**Status**: Implementado y verificado: US1-US3, cinco GET protegidos, privacidad acotada, metricas, paquete y E2E aislado. Evidencia y limites en quickstart. Sin despliegue ni auditoria global.

Las claves esenciales se validan sobre el conjunto filtrado antes de paginar;
un cursor presente pero vacio es invalido, no equivale a omitirlo.
Los diagnosticos informativos de peticiones del host se filtran para no registrar
placas contenidas en query strings; no habilitar logs de cuerpos ni datos sensibles EF.
ToString diagnostico de query/criterios/movimiento no incluye valores. Regresion
con logs Trace verifica ausencia de token, contrasena y placa en el proceso API;
no comprende proxies ni sistemas externos de registro.
**Date**: 2026-11-03
**Source**: [spec.md](../spec.md), [data-model.md](../data-model.md).

Controllers ASP.NET Core existentes; nuevos DTO dedicados e inmutables (sealed record).
No cambiar serializacion global ni devolver entidades EF desde las rutas nuevas.
Mantener compatibilidad de login y del historico, excepto el acceso anonimo a lecturas
de movimientos, que deja de permitirse durante el despliegue coordinado.

## Sesion y autorizacion

### POST /api/Auth/login

Request sin cambios: userName y password con el cifrado heredado validado por el
login existente. No enviar credenciales en URL ni registrarlas. Mantener campos
esValido, respuesta y usuario; agregar a respuesta exitosa:

| Campo nuevo | Tipo | Significado |
|-------------|------|-------------|
| accessToken | string | Token opaco aleatorio; memoria de cliente y en Android persistencia cifrada Keystore para restaurar sesion vigente (revision 2026-11-06, spec005) |
| tokenType | string | Bearer |
| expiresAt | string ISO 8601 UTC | Cierre absoluto, por defecto 9 horas desde este login |
| idleTimeoutSeconds | int | Campo heredado: 32400 por defecto; sin vencimiento por inactividad |

Login exitoso: 200 con campos anteriores y los nuevos. Credenciales invalidas:
mantener 200/esValido=false, sin token. Capacidad de sesiones agotada o dependencia
no disponible: 503 ProblemDetails, sin informacion sensible. No reducir las
comprobaciones de usuario ni generar tokens para respuestas invalidas.

Agregar X-Server-Date (yyyy-MM-dd local del negocio) al login exitoso y respuestas
del reporte para inicializar fechas sin usar dia UTC del telefono. Android obtiene
headers mediante Response<LoginResponse>; no modifica el cuerpo del request.

### POST /api/Auth/logout

Authorization: Bearer con token de esta sesion; sin body.

- 204: credencial de sesion revocada; repetir con el mismo token ya revocado devuelve
  204 sin ampliar permisos ni revelar otros usuarios.
- 401: header ausente o formato invalido.
- Un token desconocido o expirado con formato valido puede devolver 204: no existe
  una sesion activa que conservar. Esta excepcion idempotente solo aplica a logout.

El cliente descarta token local en todos los casos. Sin respuesta no declara revocacion
remota; el servidor aplica vencimiento absoluto sin renovar por actividad. Cada
sesion vence por su propio login, sin cierre simultaneo de otros usuarios.

### Politica Administrador

Aplicar un esquema de autenticacion para token opaco y una politica que recupere el
usuario vigente y valide PerfilAdministrador en servidor, nunca flags locales.
Registrar UseAuthentication antes de UseAuthorization. Politica en:

- GET /api/Reportes/Ventas
- GET /api/Reportes/Ventas/Filtros
- GET /api/TurnosMovimientos
- GET /api/TurnosMovimientos/{id}
- GET /api/TurnosMovimientos/GetHistoricoMovimientos

401 para sesion ausente/expirada/revocada o usuario eliminado. 403 para sesion valida
de perfil distinto de Administrador. El error no devuelve catalogos, conteos ni datos
de cobros. El identificador de usuario no es parametro de autenticacion del reporte.

Los GET heredados conservan sus respuestas exitosas y semantica de fecha; se actualiza
el escritorio para enviar Bearer. No se cambian permisos ni comportamiento de POST,
PUT o DELETE heredados de movimientos como parte de este contrato. Tampoco se aplica
una politica global que bloquee otros controllers de forma incidental.

## GET /api/Reportes/Ventas

### Query

| Parametro | Requerido | Regla |
|-----------|-----------|-------|
| fechaInicial | Si | yyyy-MM-dd valido |
| fechaFinal | Si | yyyy-MM-dd, mayor o igual a inicial; extremo representable para dia siguiente |
| idFormaPago | No | Entero positivo; omitir = todas |
| idOperario | No | Entero positivo; omitir = todos |
| placa | No | Coincidencia completa, Trim y mayusculas; blanco = omitir |
| idTipoVehiculo | No | Entero positivo; omitir = todos |
| pageSize | No | 1..200, default 50 |
| cursor | No | Base64Url, fecha local/id/huella de los criterios, sin credenciales |

Ejemplo sin filtros opcionales: GET
`/api/Reportes/Ventas?fechaInicial=2026-11-01&fechaFinal=2026-11-03`.
Los clientes no envian 0, -1 ni etiquetas Todas/Todos como ids.
Los parametros presentes se intersectan; no aplicar OR entre opcionales.

### Respuesta 200

```json
{
  "criterios": {
    "fechaInicial": "2026-11-01",
    "fechaFinal": "2026-11-03",
    "idFormaPago": null,
    "idOperario": null,
    "placa": null,
    "idTipoVehiculo": null
  },
  "cantidadCobros": 2,
  "importeTotal": "30000.00",
  "items": [
    {
      "idMovimiento": 102,
      "idTurno": 42,
      "fechaCobro": "2026-11-03T10:30:00.000-05:00",
      "placa": "ABC123",
      "idTipoVehiculo": 1,
      "tipoVehiculo": "Automovil",
      "idOperario": null,
      "operario": null,
      "idFormaPago": 1,
      "formaPago": "Efectivo",
      "montoPagado": "15000.00"
    },
    {
      "idMovimiento": 101,
      "idTurno": 42,
      "fechaCobro": "2026-11-03T10:00:00.000-05:00",
      "placa": "ABC123",
      "idTipoVehiculo": 1,
      "tipoVehiculo": "Automovil",
      "idOperario": null,
      "operario": null,
      "idFormaPago": 2,
      "formaPago": "Transferencia",
      "montoPagado": "15000.00"
    }
  ],
  "nextCursor": null,
  "pageSize": 50,
  "consultadoEn": "2026-11-03T15:40:00Z",
  "fechaOperativa": "2026-11-03"
}
```

Datos ilustrativos, no ids ni registros reales; el offset depende de la zona operativa,
no se codifica -05:00 como regla. Sin coincidencias: items=[], cantidadCobros=0,
importeTotal="0.00", nextCursor=null; HTTP 200, no 404.

Los importes son cadenas decimales invariantes conservando la precision almacenada,
sin notacion exponencial ni simbolos. Nunca sumar Turnos.Valor ni repetir por servicios.
FechaMovimiento se compara localmente por rango semiabierto; fechaCobro del DTO
incluye el offset correspondiente a esa fecha en la zona operativa del servidor.
No se devuelven nombre de cliente, correo, telefono, observaciones o comisiones.

### Paginacion y consistencia

Orden FechaMovimiento DESC y IdTurnoMovimientos DESC. El cursor contiene la ultima
clave y huella de fechas/opcionales normalizados. La pagina siguiente usa las mismas
restricciones mas la comparacion lexicografica estricta de esa clave. Un cursor de
otros criterios o invalido da 400, nunca amplia la consulta. Su codificacion no es
autorizacion; el servidor valida y parametriza todos sus campos.

resumen agrupa todo el conjunto filtrado por forma de pago (ordenado por nombre); la suma de cantidad e importe coincide con cantidadCobros e importeTotal. Sin coincidencias: resumen=[].

cantidadCobros e importeTotal siempre describen todo el conjunto filtrado actual,
no la pagina ni las filas posteriores al cursor. Resumen y pagina se leen en una
transaccion corta por solicitud. ConsultadoEn no promete snapshot entre solicitudes.
Si el cliente observa cambios en cantidad/total al continuar, descarta esa continuacion
y pide actualizar desde la primera pagina; no mezcla resultados como si fueran fijos.
Ediciones concurrentes con agregados iguales pueden no detectarse: reporte vivo.

## GET /api/Reportes/Ventas/Filtros

Solo fechaInicial y fechaFinal obligatorias, con las mismas reglas de rango y perfil.
No acepta filtros opcionales ni consulta catalogos de operarios disponibles para
lavado. Obtiene catalogos completos independientemente de las fechas y cobros,
ordenados por nombre, con IDs positivos y sin operarios isDelete ni tipos IsDelete.
FormaPago no tiene indicador de eliminacion en el modelo actual:

```json
{
  "fechaInicial": "2026-11-01",
  "fechaFinal": "2026-11-03",
  "fechaOperativa": "2026-11-03",
  "formasPago": [{ "id": 1, "nombre": "Efectivo" }],
  "operarios": [{ "id": 7, "nombre": "Operario de prueba" }],
  "tiposVehiculo": [{ "id": 1, "nombre": "Automovil" }]
}
```

Un periodo sin cobros no vacia estos catalogos. Nombre nullable si falta descripcion;
no inventar IDs para relaciones ausentes. La UI muestra solo nombres y conserva
IDs para enviar filtros. Todas/Todos y Sin informar son textos de UI, no filas
ficticias. La exclusion de eliminados solo afecta opciones: no quita cobros historicos
del reporte sin filtros ni impide consultar un ID historico positivo explicitamente.

## Errores y operacion

Rutas nuevas usan ProblemDetails/ValidationProblemDetails (application/problem+json),
con status, title, detail seguro, instance y extension code; sin stack, SQL, tokens
o datos de clientes. Documentar respuestas en OpenAPI mediante metadatos de controller.

| HTTP | code | Resultado esperado |
|------|------|--------------------|
| 400 | INVALID_FILTERS / INVALID_CURSOR | Falta fecha, rango invertido, id no positivo, pageSize invalido o cursor incompatible; errores por campo |
| 401 | SESSION_REQUIRED | Solicitar reingreso; no reintentar con credenciales almacenadas |
| 403 | ADMIN_REQUIRED | Retirar acceso y limpiar datos; no convertir a consulta vacia |
| 503 | REPORT_UNAVAILABLE / INVALID_SOURCE_DATA | Dependencia no disponible, limite operativo o dato esencial que no puede interpretarse; reintento explicito |
| 500 | UNEXPECTED_ERROR | Error no esperado sanitizado; no reportar total cero |

Validar acceso antes de consultar opciones, detalle o agregados. Cancelar consultas
con RequestAborted y devolver errores controlados para timeout propio; no transformar
desconexion del cliente en una respuesta exitosa. Valores positivos inexistentes de
catalogo son filtros validos que pueden producir cero coincidencias.

Headers de login y respuestas protegidas: Cache-Control: no-store; no registrar
Authorization ni cuerpos del login. HTTP solo en red privada ya autorizada, HTTPS
con certificado confiable recomendado. Nunca adjuntar Bearer a otro origen, backup
o redireccion externa, ni aceptar un bypass por version antigua del escritorio.

## Despliegue compatible

Actualizar API, APK y escritorio en ventana coordinada. Login conserva campos
heredados; los clientes nuevos reciben/adoptan el token y manejan 401/403. Clientes
antiguos no podran consultar movimientos hasta actualizarse; sus credenciales no
se cambian. Las dos pantallas historicas de escritorio conservan parametros/DTO,
pero exigen Administrador y no interpretan un error de acceso como cero ventas.
Empaquetar la nueva API en el instalador existente despues de las pruebas, sin
introducir rutas locales de SDK ni secretos. Seguridad de otras rutas de API queda
fuera de este contrato; no se declara una auditoria global del sistema.