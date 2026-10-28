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

## Evidencia

Verificado el 2026-10-28 en CarWash_API_35, Android 15/API 35:

- Gate final `:app:assembleDebug :app:lintDebug :app:testDebugUnitTest
	:app:connectedDebugAndroidTest --console=plain`: BUILD SUCCESSFUL.
- JVM: 22 pruebas, 0 fallos; Android: 15 pruebas, 0 fallos, 0 omitidas.
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
- No se generaron ingresos ficticios en datos reales ni se modifico la API.

T009 pendiente: no hay celular fisico conectado para certificar precision con
placas reales, reflejos, baja luz o enfoque. La escena virtual y los bitmaps no
sustituyen esta comprobacion; no se declara un porcentaje de exactitud.