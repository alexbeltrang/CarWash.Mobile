# UI Contract: Reporte de ventas movil

**Status**: US1-US3, revision visual, accesibilidad automatizada y APK contra API publicada verificados en aislamiento; metricas SC-001/SC-005 correctas. Alcance/limites en quickstart. Sin despliegue.
**Source**: [spec.md](../spec.md), [api.md](api.md).

## Navegacion

- Inicio autenticado sigue abriendo Ingreso; no agregar pantalla de bienvenida.
- Navegacion Material 3 con destinos Ingreso, Vehiculos y Reporte de ventas. Usar
  NavigationBar en telefono y NavigationRail si el ancho lo permite, con iconos de
  Material existentes y etiquetas de recursos. El tercer destino solo existe para
  Administrador. Usar un destino explicito, no varios booleanos independientes.
- Volver desde Reporte regresa a Ingreso sin cerrar la sesion ni alterar un formulario
  existente. Cerrar sesion limpia el reporte, sus resultados y la credencial local.
- Ventas comparte el login general, sin autenticacion adicional. La sesion completa
  cierra a las 9 horas desde ese login, sin caducidad por inactividad ni renovacion
  por uso. Comprobar el plazo tambien al volver de segundo plano. No cerrar otros
  usuarios. Metadatos restaurados sin token requieren login general antes de entrar.
- Cambio de usuario o servidor descarta resultados y token anteriores. No copiar una
  credencial al servidor alternativo. Sesion no administradora no carga opciones.
- Un login nuevo tras logout inicializa fechas con el dia operativo y no recupera
  filtros descartados. No persistir credencial ni contrasena para evitar ese login.

## Filtros

- Fecha inicial y Fecha final visibles y obligatorias, con selectores Material 3.
  Inicializar ambas al dia operativo informado por servidor, no al dia UTC del movil.
  Si no hay fecha operativa disponible, permitir seleccion explicita sin inventarla.
- Opcionales en seccion plegable: Forma de pago (Todas), Operario (Todos),
  Tipo de vehiculo (Todos) y Placa (vacia). Usar menus de seleccion e input de placa;
  no botones decorativos para opciones. Seleccion unica por catalogo.
- Consultar ejecuta solo con fechas validas; muestra error junto al campo faltante o
  rango invertido. No exigir caja abierta, operario, medio de pago ni tipo.
- Limpiar filtros conserva fechas y quita las cuatro restricciones opcionales.
- Filtros devuelve catalogos completos independientes de fechas/cobros, sin operarios
  ni tipos eliminados. FormaPago no tiene indicador de eliminacion. Mostrar solo
  nombres, nunca sufijos #ID; conservar IDs internos para la solicitud. Si una
  seleccion deja de estar disponible, conservarla hasta que el usuario la quite;
  no ampliar consulta silenciosamente. Los cobros historicos siguen en resultados.
- Editar controles no consulta automaticamente ni cambia la etiqueta de los criterios
  aplicados a resultados existentes. Marcar esos resultados como anteriores hasta
  pulsar Consultar. Son etiquetas de estado, no instrucciones de uso.

## Resultados

- Encabezado compacto con periodo aplicado, filtros activos, cantidad de cobros,
  importe total y momento de consulta. No usar hero, graficos ni tarjetas anidadas.
- Tabla global por forma de pago con nombre, cantidad e importe, incluyendo total;
  sus valores describen todo el conjunto filtrado, no se acumulan al paginar.
- Listado vertical con fecha/hora del cobro, placa, tipo, operario, forma de pago e
  importe, sin datos personales adicionales. Identificador de movimiento y turno
  accesibles como referencia del registro. Un movimiento ocupa un item; varios
  cobros del mismo turno aparecen por separado.
- Descripciones null se presentan Sin informar, no se ocultan los movimientos.
- Mantener formato monetario de la app; aritmetica en BigDecimal. No recalcular
  tarifa, total ni comision desde servicios o el precio actual.
- Cargar mas solo si nextCursor existe; deshabilitar durante una carga. No repetir
  filas ni lanzar dos continuaciones simultaneas. El resumen no se suma por pagina.
- Si la continuacion informa una cantidad/total diferente, no mezclarla con el listado:
  mostrar cambio de datos y una accion Actualizar que reinicia la consulta.
- Reporte vivo, no cierre contable: no prometer snapshot entre paginas.

## Estados y errores

| Estado | Presentacion y acciones |
|--------|------------------------|
| SinConsulta | Controles disponibles; no inventar total cero como resultado |
| Cargando | Indicador y criterios de la consulta; cancelar o sustituir desde ViewModel |
| Resultados | Resumen global y detalle; Consultar, Actualizar y Cargar mas segun estado |
| Vacio | Sin resultados, cero cobros y cero importe para criterios aplicados |
| Error | Mensaje y Reintentar conservando criterios; no declarar conjunto vacio |
| 401 | Cerrar toda la sesion y limpiar reporte/credencial; volver al login general |
| 403 | Limpiar reporte y retirar destino; no permitir reintentar como administrador local |

Si falla una pagina adicional, conservar lo ya visto como parcial e indicar que el
recorrido esta incompleto. Reintentar usa el mismo cursor y los mismos criterios.
Una respuesta anterior o de otro usuario/origen no puede reemplazar el estado actual.

INVALID_CURSOR ofrece Actualizar desde la primera pagina con los criterios aplicados;
no reenvia indefinidamente una continuacion que el servidor ya rechazo.

## Accesibilidad y ciclo de vida

Textos en strings.xml; iconos con descripcion semantica cuando no hay etiqueta.
Controles tactiles con dimensiones Material, errores asociados a campos, contraste
claro/oscuro y lectura con fuente ampliada. Permitir desplazamiento sin superponer
filtros, resumen, barras de sistema o teclado. Orientacion libre.

Convertir DatePicker a LocalDate por su convencion UTC de seleccion de calendario,
sin desplazar el dia al aplicar la zona del telefono. Probar un telefono en otra
zona horaria y ambos extremos del rango. Fechas de cobro muestran zona del negocio.

ViewModel conserva filtros/resultados durante rotacion. No persistir resultados ni
token en preferencias, Bundle o SavedStateHandle; muerte del proceso pierde esos
datos y exige login general. Logout local sin red elimina la credencial y no
afirma revocacion remota. Respetar los flujos existentes de ingreso/asignacion.

## Escritorio

Las pantallas existentes FrmConsultaMovimientosCaja y FrmReporteDiarioVentas solo
se habilitan para Administrador. Se conservan sus filtros y contenido heredados;
401 pide login y 403 informa falta de permiso, sin devolver una lista vacia como
si no hubiera ventas. El cobro de FrmGestionDetalleVehiculo no cambia de reglas ni
requiere permiso de reporte para registrar movimientos.