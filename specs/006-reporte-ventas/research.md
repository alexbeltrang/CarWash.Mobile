# Research: Reporte de ventas por fechas

**Date**: 2026-11-03
**Status**: Complete. Ampliacion de login autorizada por el usuario el 2026-11-03; sin decisiones pendientes para Phase 1. Las decisiones son de diseno, no funcionalidad implementada.

## R1 - Fuente y unidad del reporte

**Decision**: partir de TurnosMovimientos, con una fila por IdTurnoMovimientos,
importe MontoPagado, fecha FechaMovimiento, IdTurno e IdFormaPago.

**Rationale**: coincide con la fuente confirmada por el usuario y el modelo
`../CarWashApi/Modelos/TurnosMovimientos.cs`. No hay campos de tipo de movimiento
o anulacion en ese modelo. Esa ausencia no autoriza a inventar reglas para signos,
devoluciones o eliminaciones: conservar los importes registrados sin nuevos filtros.
`../CarWash/Presentacion/Operacion/FrmGestionDetalleVehiculo.cs` crea el movimiento
con MontoPagado = valorPagar, FechaMovimiento = DateTime.Now e IdFormaPago elegido.
El flujo admite pagos parciales y la forma Credito: no excluirlos por una regla nueva.
`../CarWashApi/Context/AppDbContext.cs` convierte DateTime a texto local con formato
`yyyy-MM-dd HH:mm:ss.fff`; no migrar registros historicos a UTC. API y escritorio
deben tener la misma zona operativa. La consulta preserva la fecha local registrada.

**Alternatives considered**: sumar Turnos.Valor o filtrar por Pagado descarta cobros
individuales o utiliza el valor presupuestado; se rechaza por FR-003.

## R2 - Fecha y filtros del reporte

**Decision**: intervalo semiabierto desde fechaInicial a medianoche hasta el inicio
del dia posterior a fechaFinal sobre FechaMovimiento, en la convencion temporal
existente del negocio. Opcionales ausentes no agregan condiciones; los presentes
se aplican por identificador y se combinan con AND.

**Rationale**: `../CarWashApi/Controllers/TurnosMovimientosController.cs`, metodo
GetHistoricoMovimientos, usa FechaHoraIngreso, joins internos y filtros por nombres
de formas de pago y operarios. No satisface fechas de cobro, valores desconocidos
ni separacion inequivoca de registros con nombres iguales.

**Alternatives considered**: reutilizar el endpoint historico cambiando su
significado afectaria al consumidor de escritorio. Agregar GET `api/Reportes/Ventas`
y GET `api/Reportes/Ventas/Filtros` en controller propio y conservar el contrato
heredado, excepto su autenticacion obligatoria en adelante.

## R3 - Joins, totales y precision

**Decision**: proponer joins izquierdos hacia datos descriptivos y no unir servicios
al conjunto monetario. Filtrar sobre las claves de movimiento/turno, incluir valores
historicos y conservar un movimiento por clave. Agregar y paginar en servidor.

**Rationale**: los joins internos de GetHistoricoMovimientos pueden eliminar cobros
sin operario o relaciones descriptivas. Un turno puede tener varios servicios y
varios movimientos sin que deban multiplicarse sus importes.

**Alternatives considered**: sumar en el movil un detalle descargado parcialmente
produce un total incompleto. `../CarWashApi/Carwash.API.csproj` confirma net9.0 y
EF Core/SQLite 9.0.14. El proveedor traduce Sum(decimal) a ef_sum desde EF Core 9.0:
usar ese agregado sin convertir a double ni modificar las columnas. La prueba
SQLite temporal debera demostrar traduccion y precision; no se declara ejecutada.
Los importes del contrato nuevo son cadenas decimales invariantes mapeadas a
BigDecimal en Android, sin cambiar el DTO historico del escritorio.

**References**:

- https://learn.microsoft.com/en-us/ef/core/providers/sqlite/functions
- https://learn.microsoft.com/en-us/ef/core/providers/sqlite/limitations

## R4 - Identidad y perfil Administrador: ampliacion autorizada

**Evidence**:

- `../CarWashApi/Controllers/AuthController.cs`: POST login compara credenciales
  existentes y devuelve EsValido y Usuario, con EsAdministrador calculado en servidor.
  No emite token ni establece una cookie autenticada.
- `../CarWashApi/Program.cs`: no registra un esquema de autenticacion ni llama
  UseAuthentication; UseAuthorization por si solo no crea una identidad autenticada.
- `../CarWashApi/Controllers/TurnosMovimientosController.cs`: GetAll, GetById y
  GetHistoricoMovimientos permiten leer movimientos sin validar un usuario.
- `app/src/main/java/com/carwash/mobile/auth/AuthRepository.kt`: Session contiene
  userId, displayName, isAdmin y baseUrl, no una credencial verificable.
- `app/src/main/java/com/carwash/mobile/auth/SessionStore.kt`: persiste esos datos
  en SharedPreferences sin usuario ni contrasena. Son datos locales de navegacion,
  no evidencia de autenticacion ante la API.

**Decision**: el usuario autorizo ampliar el login existente. Agregar accessToken,
tokenType y expiresAt a su respuesta exitosa sin quitar campos. Emitir token opaco
de 32 bytes de RandomNumberGenerator, Base64Url; no usar Guid como secreto.
Guardar solo SHA-256 del token en registro singleton de sesiones en memoria de API.
Token en memoria del cliente, nunca en preferencias, SavedState, URL o logs.
Revision T058: configurar 9 horas absolutas desde cada login, sin caducidad por
inactividad ni renovacion por uso, con TimeProvider en UTC
para comprobar limites sin esperas. Limpiar sesiones expiradas y limitar a 10000
sesiones vigentes; al alcanzar capacidad devolver 503 sin expulsar sesiones vivas.

Autenticar con esquema ASP.NET Core antes de autorizar. Consultar usuario vigente
y PerfilAdministrador en cada solicitud protegida, no confiar en flags del cliente
ni rol almacenado al iniciar sesion. Ausente, expirado o revocado: 401. Perfil no
administrador con sesion valida: 403. Usuario eliminado invalida sesion con 401.

POST `api/Auth/logout` revoca solo el token presentado. Plazo absoluto,
logout confirmado o reinicio de API invalidan sesiones. Un cambio de perfil se
aplica en la siguiente solicitud. Cierre sin red descarta token local sin afirmar
revocacion remota. Restaurar metadatos de sesion movil no restaura token: se requiere
login general. Ventas comparte esa sesion sin login propio; la app cierra toda su
sesion a las 9 horas y comprueba el plazo al volver de segundo plano. No cierra
sesiones de otros usuarios.

Ligar token al origen autenticado (esquema, host y puerto), sin reenviarlo al backup
ni en redirecciones de otro origen. Mantener restricciones existentes de transporte;
HTTP privado sigue expuesto a escucha en LAN, por lo que HTTPS confiable se recomienda
sin desactivar validacion TLS. No se afirma que un token cifre el transporte.

**Scope consideration**: proteger todas las acciones GET de TurnosMovimientos y las
rutas nuevas de reporte/filtros; no solo la nueva pantalla. Consumidores confirmados:
`../CarWash/Controladores/TurnosMovimientosController.cs`, GetHistoricoMovimientos,
usado por `Presentacion/Consultas/FrmConsultaMovimientosCaja.cs` y
`Presentacion/Consultas/FrmReporteDiarioVentas.cs`. Adaptar
`../CarWash/Controladores/LoginController.cs` para conservar token solo en memoria,
adjuntarlo por solicitud al origen correcto y tratar 401/403 como errores de acceso,
no como listas vacias. Mantener respuesta y fechas heredadas del historico, sin
modificar escrituras de caja/cobro. Publicar API y clientes en ventana coordinada:
clientes viejos reciben 401 en esas lecturas hasta actualizarse; no hay bypass anonimo.
Ocultar pantallas de ventas a no administradores no cambia sus operaciones de cobro.
La seguridad global de otras rutas operativas o de escritura queda fuera de alcance;
este reporte no afirma eliminar toda exposicion monetaria de la API heredada.

**Alternatives considered**:

- Solo ocultar menu: incumple FR-004 ante acceso directo.
- Verificar que idUsuario corresponde a administrador: cualquiera puede suplantar
  ese identificador; cambiar query por header no soluciona el problema.
- Enviar o persistir credenciales en cada consulta: aumenta exposicion y contradice
  las restricciones actuales; no se adopta como solucion implicita.
- Cambiar toda la API a un nuevo sistema de identidad: excede el alcance del reporte.
- Persistir tokens en SharedPreferences o usar JWT/proveedor nuevo: innecesario para
  una API unica y contrario al alcance de credenciales solo en memoria.

## R5 - Navegacion movil

**Decision**: proponer destinos Ingreso, Vehiculos en proceso y Reporte de ventas,
este ultimo visible solo para Administrador. Mantener Material 3, repositorio,
ViewModel y recursos de texto existentes, sin agregar frameworks de reportes.

**Rationale**: MainActivity actualmente alterna ingreso/asignacion con showVehicles;
un destino explicito evita acumular booleanos y conserva los flujos de trabajo.

**Alternatives considered**: una pantalla inicial de marketing o un reporte incrustado
en ingreso no corresponde al uso operativo; no agregar esas superficies.

## R6 - Filtros, paginas y consistencia

**Decision**: fechas DateOnly obligatorias y opcionales null sin restriccion,
interseccion AND y comparacion por ids. Rango local [inicio, dia posterior a fin).
Las opciones provienen del conjunto del periodo, sin excluir catalogos inactivos
ni aplicar otros opcionales. Placa completa con Trim y mayusculas tanto en consulta
como en dato registrado. Una relacion descriptiva faltante se devuelve null.

Pagina inicial 50, maximo 200. Orden FechaMovimiento DESC, IdTurnoMovimientos DESC.
Cursor Base64Url con fecha local, id y huella de criterios; validar formato y huella,
parametrizar SQL y nunca usar cursor como credencial. Count/Sum no reciben cursor.
Usar transaccion de lectura corta para resumen y pagina coherentes por respuesta.

Reporte vivo: no mantener transaccion entre solicitudes ni prometer snapshot de
toda la navegacion. Cada respuesta incluye consultadoEn y criterios. El cliente
reemplaza el resumen al paginar; si cambia cantidad o total, exige actualizar desde
la primera pagina sin mezclar el recorrido. Ediciones concurrentes que no alteran
esos agregados no se detectan de forma garantizada. Actualizar inicia un recorrido
nuevo. La cobertura de recorrido completo usa datos estables; no es cierre contable.

**Rationale**: orden por dos claves evita empates; cursor no desplaza filas por
inserciones mas recientes. Las fechas del DTO se presentan con offset de la zona
operativa, pero se comparan como DateTime local persistido, sin conversion UTC.

**Alternatives considered**: offset desplaza filas al insertar; cursor solo por id
no respeta fecha. Cachear todo el reporte agrega almacenamiento innecesario.

## R7 - Estado movil y verificacion

**Decision**: destino explicito Ingreso/Vehiculos/Reporte; iconos y Material 3 existentes,
fechas visibles y opcionales plegables, listado vertical y resumen sin graficos.
ViewModel separa controles editados de criterios aplicados y usa Job cancelable mas
numero de consulta para ignorar respuestas antiguas. Token y ventas no se guardan
en SavedState. Rotacion conserva ViewModel; muerte de proceso pierde resultados y
credencial. 401 pide reingreso; 403 retira acceso y limpia datos de reporte.

**Rationale**: Compose, ViewModel, Retrofit, Gson, JUnit y MockWebServer ya existen.
No se localizo proyecto de pruebas API en su raiz: proponer proyecto hermano
CarWashApi.Tests con pruebas HTTP/SQLite temporal y reloj sustituible.

**Alternatives considered**: acumular booleanos de navegacion crea estados ambiguos;
persistir resultados en preferencias contradice privacidad. Las pruebas nuevas de
API deben excluir acceso al appsettings y a la base real durante el arranque.

## Resultado

Phase 0 completa; sin decisiones de negocio pendientes. La autorizacion y FR-017
resuelven el gate sin cambiar principios de constitucion: autenticacion especificada
y credenciales temporales no persistidas. Phase 1 se documenta en data-model.md,
contracts/ y quickstart.md. No se modifica la convencion temporal de cobros.

No se modificaron codigo, base de datos, credenciales, reglas de cobro ni servidor.
No se ejecutaron builds o pruebas de la funcionalidad.