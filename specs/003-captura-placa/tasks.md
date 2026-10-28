# Tasks: Captura de placa

- [x] T001 Especificar captura, revision obligatoria y respaldo manual sin cambios API.
- [x] T002 Implementar y probar sugerencias y estado de revision.
- [x] T003 Agregar dependencias, permiso opcional y captura CameraX en memoria.
- [x] T004 Implementar OCR local, lifecycle, enfoque, linterna y errores.
- [x] T005 Integrar confirmacion con formulario y busqueda sin escrituras automaticas.
- [x] T006 Probar permiso/manual, revision, cancelacion, OCR real y regresion.
- [x] T007 Ejecutar compilacion, lint, pruebas JVM e instrumentadas y revisar emulador.
- [x] T008 Actualizar documentacion y registrar evidencia y bloqueos reales.
- [ ] T009 Validar placas reales, luz/reflejos, distancia y enfoque en celular fisico.
- [x] T010 Reparar identidad SQLite de TurnoServicios con autorizacion, pruebas y respaldo.
- [x] T011 Verificar guardado completo y formulario listo para el proximo ingreso.
- [x] T012 Quitar numero de orden del formulario y enviar siempre cero sin modificar base.

## Evidencia

Verificado el 2026-10-28 en CarWash_API_35, Android 15/API 35:

- Gate final `:app:assembleDebug :app:lintDebug :app:testDebugUnitTest
	:app:connectedDebugAndroidTest --console=plain`: BUILD SUCCESSFUL.
- JVM: 23 pruebas, 0 fallos; Android: 15 pruebas, 0 fallos, 0 omitidas.
- OCR real incluido: ABC123 sobre bitmap sintetico, rotacion 180 y fondo vacio.
- Revision: corregir AOI012 sin sustituciones, placa manual sin restriccion de
	pais, multiples sugerencias/reintento sin confirmar, fuente 1.8 en pruebas UI.
- ViewModel: confirmar solo busca, sin register; funciona sin conexion; bloquea
	cambio durante envio o revision incierta; otra placa limpia datos anteriores
	incluso cuando falla la consulta. Regresion del ingreso existente conservada.
- CameraX real en emulador: preview no vacio, captura y lectura de escena virtual
	sin placa; muestra revision vacia con alternativas, sin guardar ingresos.
- Permiso revocado/bloqueado con herramientas del emulador: aparece estado
	denegado, ingreso manual devuelve al formulario conservando ABC123.
	Flags de bloqueo retirados y permiso de prueba restaurado al finalizar.
- Rotacion real de revision conserva AOI012. Modo oscuro y fuente 1.5 conservan
	texto y permiten revisar; preferencias del emulador restauradas al finalizar.
- Ajustado contraste oscuro de botones e iconos de barras del sistema tras
	inspeccion; gate completo ejecutado otra vez despues del ultimo cambio de codigo.
- Capturas en app/build/reports/: captura-estado.png, captura-revision.png,
	captura-permiso.png, captura-manual.png, captura-horizontal.png,
	captura-accesible.png. Artefactos generados e ignorados por Git.
- No se generaron ingresos ficticios en datos reales. La captura no modifica
	endpoints; la reparacion autorizada posterior se detalla a continuacion.

T009 pendiente: no hay celular fisico conectado para certificar precision con
placas reales, reflejos, baja luz o enfoque. La escena virtual y los bitmaps no
sustituyen esta comprobacion; no se declara un porcentaje de exactitud.

El usuario confirmo lectura correcta de una placa en su celular. T009 sigue
pendiente como verificacion de distintas condiciones, no como fallo de lectura.
T010 completada: TurnoServicios tenia 395 IDs NULL y carecia de PRIMARY KEY.
Prueba aislada exitosa antes de intervenir; respaldo consistente en
%LOCALAPPDATA%/CarWash/Backups antes de reparar. Se conservaron las 395 asociaciones
y sus datos de negocio, con 0 IDs NULL y quick_check=ok. Tras reiniciar la API,
GET de servicios devuelve HTTP 200 con 395 IDs validos en localhost y por Wi-Fi.

T011 completada en pruebas: respuestas HTTP 201 completas de ASP.NET aceptadas;
dos guardados consecutivos limpian formulario y marcador en ViewModel y Compose.
El gate completo se ejecuto nuevamente con 23 pruebas JVM y 15 Android exitosas.
Se mantienen mensajes y bloqueo ante errores reales. Esa reparacion de base no
cambio codigo productivo Android ni requirio reinstalar APK.
Pendiente observar el proximo ingreso real del usuario en el celular; no se
crearon turnos de prueba en produccion para comprobarlo. El turno anterior puede
tener servicios faltantes: revisar en computador y limpiar explicitamente el
marcador pendiente antes de continuar, sin reenviar el mismo ingreso.

T012 completada el 2026-10-28: eliminado control, estado, etiqueta y validacion de
orden; celular ocupa el ancho disponible. El JSON mantiene numeroOrden como cadena
"0" sin modificar API ni base de datos. MockWebServer comprueba valor y tipo;
Compose comprueba ausencia del control y dos guardados sin completar orden.
Gate completo nuevamente exitoso: compilacion, lint, 23 pruebas JVM y 15 Android.
Este cambio de formulario si requiere instalar el APK actualizado.