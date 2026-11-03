# Quickstart: Validacion del reporte de ventas

**Date**: 2026-11-03
**Status**: T001-T060 implementadas/verificadas en aislamiento. Revision vigente
documentada primero; checkpoints anteriores conservan evidencia historica. Sin despliegue.

Referencias: [modelo](data-model.md), [API](contracts/api.md), [UI](contracts/ui.md).

## Revision vigente: sesion unica y combos (T057-T060)

Esta revision sustituye las politicas historicas de 30 minutos/8 horas y opciones
historicas por periodo descritas en checkpoints anteriores de este documento.
Ventas usa el login general: cierre de toda la sesion movil a las 9 horas desde
cada login, sin caducidad por inactividad ni renovacion por uso. La vuelta de
segundo plano comprueba el plazo; no cierra otros usuarios. Muerte del proceso
requiere login general, porque la credencial no se persiste.

Combos con nombres sin sufijos de ID, seleccion/envio por ID interno. Catalogos
independientes del periodo, excluyendo Operarios.isDelete/TipoVehiculo.IsDelete.
FormaPago no dispone de ese indicador. Los cobros historicos siguen consultables.

Verificacion ejecutada en aislamiento:

- Suite API: 66/66 correctas; incluye 29 pruebas de sesiones, limite de 9 horas,
   inactividad, actividad que no renueva y usuarios con plazos independientes.
- Android: 77 JVM y 41 instrumentadas correctas, 0 fallos; una integracion opcional
   omitida sin URL de fixture. assembleDebug y lintDebug correctos (0 errores,
   6 advertencias). Conteos contrastados con XML, AVD API 35.
- DesktopCompatibilityTests: 1/1 correcta con reloj de fixture de 9 horas;
   historico, cobro existente y revocacion, sin modificar servicio productivo.
- Comandos: gate Gradle completo :app:testDebugUnitTest :app:assembleDebug
   :app:lintDebug :app:connectedDebugAndroidTest; dotnet test con --artifacts-path
   temporal, y filtro FullyQualifiedName~DesktopCompatibilityTests con
   -p:RunDesktopCompatibilityTests=true.
- Paquete regenerado con installer/build.ps1, Inno Setup 6.7.3 correcto.
   InstallerSchemaContainsNoBusinessData y PackagedApiKeepsPermissionsAndQueriesReadOnly:
   2/2 correctas sobre el nuevo binario, base temporal y loopback. Cinco GET con
   401/403, lectura sin alterar cobros y logout revocado. No se repitieron los E2E
   completos de ambos clientes contra este paquete; sus resultados anteriores son historicos.
- Instalador: installer/output/CarWashApi-Setup.exe, 41545444 bytes,
   SHA256 2D9F8482BAFF6633A0FC6C4B4A4C844AB7716633BF569C121E6B6A593EBBEDB4.
- APK Debug: app/build/outputs/apk/debug/app-debug.apk, 64350672 bytes,
   SHA256 109073A2455D6E653F334EEDEF67A876871D05D90A17CFDB4E403339BEC6A59F.
- Staging sin bases operativas, Development ni valores secretos detectados.
   Instalador no ejecutado; servicio productivo, firewall y base real sin cambios.

## Prerrequisitos

- JDK 17 y SDK Android 35, Gradle Wrapper del repo y emulador o telefono autorizado
  API 26 o superior. No versionar local.properties ni rutas del SDK.
- .NET 9 SDK y fuente API en el repositorio hermano CarWashApi; fuente de escritorio
  CarWash y sus herramientas habituales de compilacion WinForms.
- Proyecto existente ../CarWashApi.Tests/Carwash.API.Tests.csproj reutilizado,
  con xUnit/VSTest, Microsoft.AspNetCore.Mvc.Testing y proveedor EF SQLite 9.0.14.
- API, movil y escritorio de prueba conectados al mismo servidor y zona operativa.
  HTTPS con certificado confiable recomendado; no desactivar TLS para enviar tokens.
- Usuario administrador y usuario no administrador sinteticos. No anotar contrasenas
  ni tokens en documentos, URLs, capturas, logs o historial de terminal.

## Aislamiento obligatorio

La fixture de API debe usar SQLite temporal o en memoria, crear esquema completo y
datos sinteticos antes de iniciar el host, sustituir conexion/servicios relevantes
y reloj, y comprobar que la conexion no apunta al archivo real. Program.cs ejecuta
DDL al arrancar: nunca lanzar sus escenarios con conexion productiva ni copiar una
base con datos de clientes. Deshabilitar clientes SMTP/WhatsApp en pruebas.

No probar escrituras autenticando contra produccion. Para inspeccion manual usar una
instancia de prueba en puerto libre; no reiniciar el servicio real. Al terminar,
eliminar solo archivos temporales creados por la fixture y cerrar esa instancia.

## Comandos posteriores a implementacion

Desde la raiz de CarWash.Mobile, en PowerShell, una vez que existan las pruebas:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path "$env:TEMP/CarWash.ReporteVentas.TestArtifacts"
dotnet build ../CarWashApi/Carwash.API.csproj --artifacts-path "$env:TEMP/CarWash.ReporteVentas.TestArtifacts" --no-restore
```

El segundo comando requiere dispositivo conectado. Las pruebas API deben restaurar
sus dependencias y referencia al proyecto API antes de usar --no-restore en build.
Compilar tambien el escritorio con su solucion/configuracion existente en Windows
durante la fase de compatibilidad; registrar comando, version y resultado reales,
sin inventar una herramienta de build si no esta disponible.

La fixture HTTP levanta su propio host aislado, por lo que `dotnet test` no requiere
iniciar el servicio real. Para validacion manual, iniciar la API de prueba solo
despues de configurar su SQLite temporal y comprobar el destino de conexion.

## Conjunto sintetico minimo

Preparar en la fixture turnos y cobros con identificadores de prueba estables:

| Caso | Datos | Comprobacion |
|------|-------|--------------|
| A | Ingreso dia anterior, cobro 100.10 en primer dia del rango | Se incluye por fecha de cobro |
| B | Mismo turno, segundo cobro 20.20 en ultimo dia y otra forma de pago | Cuenta como otro movimiento |
| C | Mismo turno con varios servicios | No multiplica A ni B |
| D | Cobro 30.30 justo fuera del rango | Se excluye |
| E | Ingreso dentro del rango sin movimiento | No agrega venta |
| F | Cobro 0.10 y otro 0.20, mismos criterios | Total exacto 0.30 sin double |
| G | Sin operario y con descripcion de catalogo ausente | Se conserva sin filtro y muestra Sin informar |
| H | Operario/tipo/forma historicos inactivos | Opciones presentes y filtro por id funciona |
| I | Cobros de igual fecha y distinto id, mas de 2 paginas | Orden estable, sin duplicados y recorrido completo |

En el conjunto A/B/C/D/E consultado aisladamente, cantidad = 2 e importe = 120.30.
No mezclar F/G/H/I en ese esperado sin recalcular la suma explicita de la fixture.
Usar clave del movimiento, no cantidad de turnos, para derivar conteos esperados.

## Escenarios ejecutables de aceptacion

1. **Fechas y cobros**: probar mismo dia, medianoche inicial, ultimo milisegundo
   del dia final y siguiente medianoche. Cambiar zona del telefono sin cambiar el
   conjunto. Fecha vacia/invalida o invertida produce 400 en API y bloqueo local.
   Confirmar que no se consulta por FechaHoraIngreso ni se excluyen abonos/Credito.
2. **Opcionales**: consultar solo fechas y luego cada filtro por separado y todas
   sus combinaciones. Comparar con interseccion de ids; limpiar devuelve al conjunto
   completo sin cambiar fechas. Id positivo inexistente produce vacio; id 0/-1 da
   400, nunca amplia resultados. Placa completa ignora mayusculas y espacios exteriores.
3. **Historicos y nulls**: catalogos inactivos aparecen entre opciones del periodo;
   movimiento sin relacion descriptiva no se pierde. Opciones no dependen de que
   operario este disponible ni de caja abierta. Cambiar periodo no elimina un filtro
   seleccionado silenciosamente.
4. **Dinero y paginacion**: demostrar traduccion decimal Sum a ef_sum con SQLite
   real del proveedor, no EF InMemory. Resumen igual al conjunto completo con pageSize
   1 y 50; recorrer todos los cursores sin duplicar ids con datos estables. Cursor
   malformado o de otros filtros da 400. Al cambiar cantidad/total entre paginas,
   mostrar cambio y reiniciar al actualizar; no afirmar snapshot contable.
5. **Acceso directo**: invocar reporte, opciones y todos los GET de TurnosMovimientos
   sin token, con token inventado, expirado y revocado: 401. Sesion valida no admin:
   403 sin datos, conteos ni catalogos. Manipular idUsuario/isAdmin local no sirve.
6. **Ciclo de sesion**: con TimeProvider de prueba avanzar 30 minutos sin actividad
   y 8 horas absolutas; ambas condiciones deben expirar. Solicitud valida renueva
   inactividad, no limite absoluto. Logout confirmado devuelve 204 y reutilizar token
   en GET da 401. Repetir logout con token revocado es 204. Reiniciar API pide login.
   Eliminar usuario o cambiar perfil se refleja en la siguiente solicitud.
7. **Cliente y origen**: rotar pantalla y verificar ViewModel; matar proceso y comprobar
   que token/resultados no estan en preferencias ni SavedState. Sesion restaurada
   requiere reingreso para reporte. No enviar token a backup u otro origen, incluso
   mediante redireccion. Logout sin red descarta token local sin afirmar revocacion remota.
8. **Errores y carreras**: simular timeout, perdida de red, 503 y error de pagina; no
   mostrar cero como exito. Reintentar conserva criterios/cursor. Respuesta antigua
   despues de otra consulta o login no reemplaza resultados. Editar sin consultar
   mantiene visibles los criterios aplicados anteriores.
9. **UI y regresion**: administrador ve tercer destino y demas perfiles no. Verificar
   filtros/tabla/resumen con fuente ampliada, claro/oscuro, vertical/horizontal y
   lector de pantalla. Volver conserva formulario de ingreso; asignacion sigue igual.
   Sin caja abierta el reporte funciona y no genera ninguna escritura.
10. **Escritorio coordinado**: login actualizado recibe token sin cambiar credenciales;
    las dos pantallas historicas adjuntan Bearer y muestran sus respuestas previas
    con perfil Administrador. Otros perfiles no consultan ventas, pero conservan
    sus operaciones permitidas de ingreso/cobro. 401/403 no se interpretan como cero
    ventas. Cliente antiguo no recibe bypass anonimo: debe actualizarse.

## Objetivos medibles

- SC-001: cronometrar apertura, fechas y solicitud; menos de 30 segundos sin opcionales.
- SC-002/003: todos los conjuntos de prueba coinciden por id de movimiento, importe
  decimal y cantidad; cubrir abonos, varios servicios y varias paginas.
- SC-004: todos los rangos invalidos se bloquean antes de pedir resultados desde UI.
- SC-005: preparar 1000 cobros sinteticos y ejecutar 20 consultas en conexion local
  estable, midiendo desde Consultar hasta resumen y primer detalle visibles. Al menos
  19 deben ser inferiores a 5 segundos. Documentar equipo, red, rangos y tiempos; no
  sustituir esta medicion por tiempo de una prueba unitaria.
- SC-006/008: todos los intentos sin permiso/sesion se rechazan; comparar datos antes
  y despues de consultas para confirmar que no alteran cobros ni estado de negocio.
- SC-007: comprobar cada estado de error/consulta anterior y ausencia de falsos vacios.

## Despliegue y evidencia

Despues de pasar gates, actualizar README y docs de instalacion, regenerar el paquete
API con el procedimiento existente de installer/build.ps1 y coordinar actualizacion
de API, APK y escritorio. Reautenticar clientes tras reiniciar API. No ejecutar el
instalador ni modificar firewall o servicio productivo como parte de una prueba.

Registrar comandos reales, cantidades de pruebas, resultados de metricas y capturas
sin datos personales ni secretos. Ausencia de dispositivo, SDK o herramienta se
registra como bloqueo; no marcar pruebas no ejecutadas como exitosas.

**Evidencia actual**: T001-T056 verificadas. Las secciones incrementales conservan
los conteos de cada corrida, no los sustituyen por ejecuciones supuestas.
Accesibilidad automatizada/visual, privacidad acotada y gates siguientes completos.
Sin despliegue ni auditoria global; escucha humana de TalkBack no realizada.

### Privacidad y gates de compilacion (T049-T051)

- T049: matriz de cinco GET protegidos con token ausente/inventado, vencimiento,
   revocacion, no Administrador, cambio de perfil y usuario eliminado: correcta.
   AuthSessionTests 28/28 correctas. API almacena solo hashes de tokens; clientes
   conservan credencial solo en memoria y la envian por peticion al origen emisor.
- Prueba con logs de diagnostico de todos los niveles detecto placa en mensajes
   del host y en ToString de argumentos MVC. Corregidos filtro informativo del host
   y representaciones diagnosticas sin valores para DTO de reporte. Repeticion
   correcta, sin token, contrasena ni placa. No habilitar logging de cuerpos ni EF
   SensitiveDataLogging; proxies/servicios externos al proceso quedan fuera de alcance.
- Preferencias Android reales: solo userId/displayName/isAdmin/baseUrl; elimina
   claves antiguas de accessToken/password/sales. Prueba instrumentada correcta.
   Reporte no usa preferencias, Bundle ni SavedStateHandle para ventas/credenciales.
- Aislamiento de origen comprobado por MockWebServer (sin redirecciones ni fallback)
   y escritorio compilado. HTTPS protegido del escritorio rechaza ApiIgnorarSSL=true.
   HTTP privado no cifra ni protege de interceptacion de un token: limitar a red
   confiable y preferir HTTPS con certificado confiable. No es una auditoria global.
- T050: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
   :app:connectedDebugAndroidTest --console=plain`: BUILD SUCCESSFUL, 4m43s.
   74 JVM y 40 instrumentadas correctas, 0 fallos/omitidas en CarWash_API_35 API 35;
   APK generado y lint correcto sin errores. Incluye regresion previa completa.
- T051: suite API ordinaria 65/65 correcta; `dotnet build
   ../CarWashApi/Carwash.API.csproj --artifacts-path
   $env:TEMP/CarWash.ReporteVentas.TestArtifacts --no-restore` correcto.
   MSBuild net481 Debug con OutputPath/IntermediateOutputPath temporales correcto.
   Integracion opcional DesktopCompatibilityTests 1/1 correcta, ampliada para
   registrar un cobro real del controlador compilado: 1.25 exactos, turno 10/medio 1,
   cantidad temporal pasa de 112 a 113. Login/cifrado/historico/401/403/logout correctos.
   No se reemplazaron ejecutables en uso ni se escribio en bases operativas.

### Medicion reproducible con API y UI reales (T052)

Windows x64, Intel Core Ultra 7 165U (14 procesadores logicos), JDK 17 y .NET 9;
AVD Pixel 6 CarWash_API_35, sdk_gphone64_x86_64 API 35. Conexion estable local
por adb reverse a Kestrel loopback y TestServer; SQLite temporal aislado.
Login con cifrado real del APK y usuario exclusivamente sintetico. 1000 cobros
elegibles, cantidad/importe verificados antes/despues (1000 y 1152.90 exactos).
No calentamiento artificial; la carga normal de opciones esta activa.

SC-001: desde Ingreso, abrir Ventas, usar ambos calendarios (1 al 8 noviembre
2026) y pulsar Consultar sin opcionales: **9808 ms**, menor de 30 segundos.
SC-005: desde Consultar hasta resumen y primera referencia del detalle visibles
(assertIsDisplayed de ambos y UI ociosa), incluye red/deserializacion/render y
desplazamiento si hace falta: **20/20 bajo 5 segundos** (min 345, max 1149 ms).
Muestras ms: 1149,736,660,692,595,496,497,379,361,431,424,680,475,426,479,657,605,541,472,345.
La prueba verifica suma exacta y conteo en cada consulta; no es microbenchmark
API ni garantia para Wi-Fi/dispositivos distintos. No se agregaron indices.

Repetir con emulador conectado desde Mobile (proyecto API.Tests real):

```powershell
dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path "$env:TEMP/CarWash.ReporteVentas.TestArtifacts" -p:RunReportIntegrationTests=true --filter 'FullyQualifiedName~AndroidReportIntegrationTests' --verbosity normal
```

El orquestador crea/retira su puente y adb reverse, ejecuta solo
ReporteVentasIntegrationTest con URL loopback explicita. Sin ese argumento la
instrumentada opcional se omite, nunca contacta la API operativa. El calendario
Material3 se selecciona por fecha accesible completa, no por el numero visible.
Salida generada report-metrics.txt bajo connected_android_test_additional_output;
Gradle puede reemplazarla en la siguiente corrida. Esta seccion conserva las
muestras. T053: README Mobile/API y guia de instalacion actualizados.
Lint T050: 0 errores y 6 advertencias. Paquete y recorrido final descritos abajo.

### Paquete y recorrido final (T054-T056)

- Inno Setup 6.7.3 instalado en ambito de usuario, sin elevacion. Prueba opcional
   InstallerSchemaContainsNoBusinessData correcta: SQLite ReadOnly, todas las tablas
   de negocio vacias. `./installer/build.ps1 -ApiPath (Resolve-Path ../CarWashApi).Path`
   correcto: publish API/seeder Release win-x64 autocontenido, ISCC correcto.
- Artefacto installer/output/CarWashApi-Setup.exe: 41545946 bytes;
   SHA256 AFB8DCC0747B7A99C1A11D48234BCDB07024E1A9E270190B7FD006B2303CD44F.
   Staging API sin .db/.sqlite/.sqlite3, appsettings.Development.json ni valores
   Password/AccessToken/ApiKey. Solo esquema vacio aparte; no bases operativas.
- PackagedApiHost inicia exclusivamente installer/staging/api/Carwash.API.exe
   con SQLite temporal explicito, zona UTC y puerto loopback dinamico. No instala
   ni registra servicio; mata solo su proceso hijo y elimina su fixture al terminar.
   No agrega rutas de prueba a la API; controles sinteticos viven en puente de tests.
- PackagedApiWorksWithCompiledDesktop: 1/1 correcta, login cifrado real, historico,
   origen ajeno rechazado, cambio de perfil, reingreso, cobro exacto 1.25 y logout
   revocado. La expiracion con reloj falso se verifica en la integracion de fixture,
   no se simula avanzar el reloj del proceso publicado.
- PackagedApiWorksWithCompiledAndroid: 1/1 correcta; APK actual con repositorio,
   login/cifrado, UI y calendarios reales; 1000 cobros, 20/20 consultas bajo 5 segundos,
   fechas/solicitud bajo 30 segundos, logout confirmado. Datos sin cambios tras leer.
- PackagedApiKeepsPermissionsAndQueriesReadOnly: 1/1 correcta; cinco GET anonimos
   devuelven 401 y con operativo 403, Administrador consulta 2/120.30 exactos sin
   modificar 112 cobros/264.90. Operativo conserva catalogo y ruta de cobro existente
   RegistrarTurnoMovimiento; su unica escritura sintetica lleva a 113/266.15. Otra
   consulta no altera eso; sin notificaciones, logout 204 y luego reporte 401.

Para repetir los tres escenarios del paquete, primero compilar escritorio temporal,
generar instalador y conectar el emulador; desde Mobile:

```powershell
dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path "$env:TEMP/CarWash.ReporteVentas.TestArtifacts" -p:RunDesktopCompatibilityTests=true -p:RunReportIntegrationTests=true -p:RunPackagedIntegrationTests=true --filter 'FullyQualifiedName~PackagedApi' --verbosity minimal
```

No se ejecutaron instalador, reinicio de servicio productivo ni reglas de firewall.
No se cambio rama ni se creo commit. Hooks posteriores omitidos: no existe
.specify/extensions.yml. Spec/plan/contratos/tareas reconciliados en T056.
Verificacion de privacidad limitada al codigo/procesos de esta funcionalidad;
HTTP privado sigue expuesto y las metricas no certifican otros equipos/redes.

### Accesibilidad (T048)

- 18 pruebas del reporte correctas en API 35 antes de ampliar la comprobacion del
   arbol Android; despues, pruebas enfocadas de titulo, visibilidad y acciones
   accesibles correctas. La suite final completa queda para T050.
- Fuente 200%, oscuro y formato compacto 360x320: se detecto recorte real del titulo;
   barra ajustada a la escala de fuente y prueba enfocada correcta. Desplazamiento
   probado mediante acciones LazyColumn, con resumen realmente visible.
- Capturas claro vertical, claro horizontal real (2400x1080) y oscuro compacto
   revisadas, sin superposiciones. Archivos sintéticos en la salida adicional de
   Gradle: app/build/outputs/connected_android_test_additional_output/. No versionados.
   Detector de captura vacia compara pixeles completos para evitar falsos positivos.
- Calendario conserva ambos extremos en Pacific/Kiritimati (UTC+14). Teclado/placa
   cubiertos por la prueba de cuatro filtros y contenido con imePadding.
- Etiquetas y acciones verificadas tambien en AccessibilityNodeInfo del arbol
   Android, incluidos grupos interactivos Compose. Esto comprueba exposicion a
   lectores de pantalla, no una revision humana auditiva con TalkBack.
- AVD girado con wm user-rotation lock 1; detalle realmente visible y captura
   horizontal verificados; restaurado a vertical con wm user-rotation lock 0.

### US3 verificada (T038-T047)

- API: 105 movimientos con fecha empatada recorren tres paginas sin duplicados,
   orden 204..100, resumen global 105/105.00. Cursor de otro criterio y malformado
   devuelven 400. Cambio entre paginas actualiza resumen sin prometer snapshot.
- Fase roja API: 27/29 correctas; cursor vacio convertido a null y clave esencial
   invalida fuera de primera pagina no se rechazaban. Corregidos model binding y
   validacion del conjunto completo. Repeticion: 29/29 correctas. Cancelacion se
   propaga; dependencia ausente devuelve 503 sin SQL/ruta/stack ni falso cero.
   America/Bogota conserva limites locales y devuelve offset -05:00.
- ViewModel: respuestas diferidas no cooperativas, doble continuacion, fallo/reintento
   con mismo cursor, ids unicos y resumen no acumulado, cambio de agregados exige
   Actualizar, edicion separada de criterios aplicados, 401/403 y cambio de identidad.
- Compose: primera ejecucion 14/15 correctas; cursor invalido carecia de Actualizar.
   Tras corregir la accion: 15/15 correctas en CarWash_API_35, 1m36s, mismo comando
   filtrado de US2. Errores distintos de vacio, pagina parcial y reingreso verificados.
- Ciclo de sesion: regresion de nuevo login del mismo usuario confirmo fechas vacias
   tras logout. Se distingue borrador de reingreso de sesion cerrada; repeticion JVM
   del reporte 19/19 correcta. onCleared libera repositorio y datos; ViewModel de
   Activity conserva estado en rotacion, sin SavedStateHandle/Bundle de resultados
   ni credenciales. La revision visual de orientacion sigue en T048.
- Suite JVM completa correcta (74 pruebas, a contrastar con XML); reporte incluye
   12 ViewModel y 7 repositorio. Prueba de login 403 conserva identidad operativa,
   retira Administrador y token sin persistir credencial.
- Suite API completa: 60/60 correctas mediante comando temporal habitual.

### US2 verificada (T029-T037)

- Matriz de 16 combinaciones: ids/importe/cantidad coinciden con el conjunto
   sintetico, incluso con nombres de forma de pago y operario repetidos. Placa
   normalizada completa; blancos equivalen a sin filtro, id inexistente devuelve
   vacio y 0/-1 son invalidos. US1 mantiene A-E sin opcionales.
- Catologo historico: ids de pago 1/2/999 (999 sin nombre), operarios 1/2 y tipos
   1/2, incluidos inactivos. Sin ids ficticios para ausentes. 401/403 antes de leer.
- Fase roja API: 18/19 correctas y GET Filtros 404. Tras implementar: 19/19.
   Fase roja JVM: 5/6 correctas y opciones UNAVAILABLE. Tras implementar: 6/6.
- ViewModel: 5/5 pruebas correctas, con seleccion conservada y limpiar sin consultar.
   Suite JVM completa correcta: 65 pruebas contrastadas con XML en ese checkpoint.
- `dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path
   $env:TEMP/CarWash.ReporteVentas.TestArtifacts --verbosity minimal`: 50/50 correctas.
- `.\gradlew.bat :app:connectedDebugAndroidTest
   '-Pandroid.testInstrumentationRunnerArguments.class=com.carwash.mobile.ReporteVentasScreenTest'`:
   8/8 correctas, 0 fallos/omitidas en CarWash_API_35, 1m21s. Incluye cuatro filtros,
   limpiar conservando fechas y seleccion historica ausente del siguiente periodo.
   En PowerShell el argumento con puntos debe ir entre comillas; primer intento
   sin ellas fue un error de invocacion, no una prueba ejecutada.

### US1 verificada (T017-T028)

- T017: pruebas HTTP del reporte compilan y fallan por 404 antes de crear la ruta.
   Ejecutadas con filtro FullyQualifiedName~ReporteVentasTests sobre SQLite temporal.
- T018/T019: contratos JVM y Compose escritos; primera comprobacion Android bloqueada
   por tipos de reporte aun inexistentes. Esto no cuenta como fase roja funcional.
   Tras agregar el contrato minimo, 4/4 JVM fallaron por comportamiento. Despues de
   implementar Retrofit, 4/4 correctas; el repositorio temporal ya fue reemplazado.
- T020-T023: DTO/servicio/ruta compilados con salida temporal. Primera ejecucion:
   11/13 correctas y dos fallos de limpieza SQLite por conexion abierta; corregido
   con cierre explicito en finally. Repeticion: 13/13 correctas. A-E: 2 cobros,
   120.30 exactos, ids 2 y 1, usando FechaMovimiento y sin multiplicar servicios.
   Medianoche final excluida, ultimo milisegundo incluido, decimal 0.10+0.20=0.30,
   relaciones faltantes conservadas y caja vacia sin impedir la consulta.
- T024-T027: repositorio sin redirecciones/fallback, ViewModel con validacion y
   control de generacion, pantalla y tres destinos compilados. 3 pruebas nuevas
   del ViewModel correctas; suite JVM completa correcta (61 pruebas).
- T028: `dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path
   $env:TEMP/CarWash.ReporteVentas.TestArtifacts --verbosity minimal`: 44/44 correctas.
   `.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`: correcto.
   `.\gradlew.bat :app:connectedDebugAndroidTest`: BUILD SUCCESSFUL, 26/26 correctas,
   0 fallos/omitidas en CarWash_API_35 (Android 15/API 35), 2m38s. Repositorios falsos;
   sin consultar ni escribir cobros operativos. Incluye seis pruebas del reporte y
   regresion instrumentada de ingreso/asignacion/captura.

## Ejecucion 2026-11-03

- T001: proyectos verificados en app/build.gradle.kts,
  ../CarWashApi/Carwash.API.csproj y ../CarWash/CarWash.slnx (CarWash.csproj).
   Se revisaron instrucciones, constitucion y estado Git de los tres repositorios.
   Se conservaron cambios previos, incluidos bin/obj ya rastreados por Git.
- Existe ../CarWashApi.Tests/Carwash.API.Tests.csproj: inspeccionar y reutilizar
   antes de crear otro proyecto. La ruta propuesta con otra capitalizacion no es
   la ruta real de su archivo de proyecto.
- Herramientas: Temurin JDK 17.0.20.1; SDK .NET 10.0.403 disponible, runtime
   ASP.NET Core/.NET 9 instalado; MSBuild de Visual Studio 18 Professional
   disponible para la solucion WinForms. No se requiere cambiar el target net9.0.
- `adb devices` no encontro dispositivos. El gate connectedDebugAndroidTest
   permanece bloqueado hasta disponer de emulador o telefono autorizado.
- Se verificaron ignores de Kotlin/.NET y se agregaron exclusiones ausentes de
   generados y archivos locales; no se retiraron del indice archivos preexistentes.
- No se inicio la API productiva, no se leyo appsettings con secretos y no se modificaron
   base de datos real, servicios, firewall ni configuracion de red.
- T002: `dotnet build ../CarWashApi.Tests/Carwash.API.Tests.csproj --verbosity minimal`
   correcto. Se reutilizo xUnit/VSTest existente y solo se agrego Mvc.Testing 9.0.14.
- T003: `dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path
   $env:TEMP/CarWash.ReporteVentas.TestArtifacts --filter
   FullyQualifiedName~CarWashApiFactoryTests --verbosity minimal`: 1/1 correcta.
   Se verifico el destino SQLite temporal usado por el arranque, esquema completo,
   datos A-I (112 cobros), reloj sustituido, eliminacion de la base al disponer
   y suma SQL ef_sum exacta de 120.30 y 0.30. Clientes externos sustituidos por fakes.
- La compilacion inicial de T003 sin artifacts-path fallo por ejecutable ocupado
   por una API existente (MSB3021/MSB3027). No se detuvo el proceso; se resolvio
   compilando fuera de su directorio. Usar artifacts-path en los siguientes comandos
   .NET de validacion. Las 44 advertencias nullable de la API son preexistentes.
- T004-T009/T015: pruebas rojas confirmaron ausencia de token/logout y lecturas
   anonimas. Despues de implementar, suite API completa: 31/31 correctas con
   `dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj --artifacts-path
   $env:TEMP/CarWash.ReporteVentas.TestArtifacts --verbosity minimal`.
   Cobertura ejecutada de inactividad/plazo absoluto, capacidad concurrente,
   logout, reinicio, perfil/usuario vigente, tres GET y contratos de escritura.
   Usuario eliminado se rechaza con 401, no 403. Historico conserva FechaHoraIngreso.
- T005/T010-T011: fase roja Android inicial 7 pruebas, 3 fallos funcionales.
   Corregidos token/fecha, metadatos sin credencial y carrera de login tras logout.
   Logout con desconexion no se confirma ni se envia al servidor alternativo.
- T012-T014: escritorio net481 compilado con MSBuild de VS 18, Configuration=Debug,
   OutputPath=$env:TEMP/CarWash.ReporteVentas.Desktop/bin/ e
   IntermediateOutputPath=$env:TEMP/CarWash.ReporteVentas.Desktop/obj/.
   No se reemplazo la aplicacion del usuario. Dos pantallas historicas restringidas,
   401 permite reingreso modal y errores no se convierten en ventas cero.
- T016: comando Gradle completo testDebugUnitTest/assembleDebug/lintDebug correcto:
   54 pruebas JVM, 0 fallos/errores/omitidas; lint 0 errores y 6 advertencias.
   Las pruebas instrumentadas y revision visual no se han ejecutado aun.
- T016: integracion opcional del escritorio real, sin ventanas ni base operativa:
   1/1 correcta ejecutando `dotnet test ../CarWashApi.Tests/Carwash.API.Tests.csproj
   --artifacts-path $env:TEMP/CarWash.ReporteVentas.TestArtifacts
   -p:RunDesktopCompatibilityTests=true --filter FullyQualifiedName~DesktopCompatibilityTests`.
   La prueba compila DesktopSessionProbe con csc de .NET Framework y usa un puente
   Kestrel solo loopback/puerto efimero hacia WebApplicationFactory/SQLite temporal.
   Las rutas __fixture existen solo en ese puente de pruebas, nunca en la API.
   Verificados cifrado real de login, historico, rechazo de origen ajeno, 401/403,
   reingreso, logout remoto y revocacion, sin imprimir credenciales ni tokens.
   La regresion agregada de reingreso fallido fallo antes de separar el borrado de
   token del identificador operativo; paso tras la correccion.
- La integracion de escritorio no se incluye en la suite API ordinaria. Requiere
   compilar primero el escritorio temporal; CARWASH_DESKTOP_EXE y CARWASH_CSC_EXE
   pueden indicar otras ubicaciones de artefacto/compilador sin versionar rutas locales.
- No se han ejecutado despliegue, instalador productivo, cambios de servicios o
   firewall, pruebas sobre cobros reales ni auditoria global. El acceso HTTPS protegido
   del escritorio rechaza ApiIgnorarSSL=true; se requiere certificado confiable.