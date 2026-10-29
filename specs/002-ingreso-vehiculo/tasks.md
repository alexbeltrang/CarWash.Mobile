# Tareas - Ingreso de vehiculos

- [x] T001 Revisar formulario original, Designer y API en ejecucion.
- [x] T002 Crear especificacion, plan y contrato verificable.
- [x] T003 Implementar modelos, validacion y repositorio HTTP con pruebas JVM.
- [x] T004 Implementar estado del formulario, conexion persistida y proteccion de envios.
- [x] T005 Crear formulario Compose, seleccion, busqueda, historial y mensajes.
- [x] T006 Probar formulario y estados con dobles de API, sin escrituras reales.
- [x] T007 Ejecutar build, lint y pruebas instrumentadas.
- [x] T008 Mostrar app con catalogos reales y capturar pantalla en emulador.
- [x] T009 Documentar configuracion Wi-Fi y limitaciones heredadas.
- [x] T010 Habilitar escucha LAN de la API con autorizacion explicita del usuario.
- [ ] T011 Verificar conexion desde celular fisico en la misma Wi-Fi.
- [ ] T012 Revisar visualmente rotacion, modo oscuro y fuente grande del nuevo formulario.
- [ ] T013 Usuario revisa perfil de red confiable y permisos necesarios de firewall privado.
- [x] T014 Corregir desfase de zona horaria usando la fecha operativa de la API.

## Evidencia

- OpenAPI y tres lecturas de catalogos/caja respondieron 200 en localhost:5197.
- Servidor activo: CarWashApi; inicialmente solo en loopback, luego LAN autorizada.
- IP Wi-Fi observada: 192.168.1.15 (dinamica, no fijar en la app).
- No se generaron ingresos ficticios en datos reales; no hubo celular fisico conectado.
- Constitucion ampliada a 1.1.0 para integracion de red y privacidad.
- `:app:testDebugUnitTest`: 14 pruebas sin fallos (8 dominio/HTTP y 6 ViewModel).
- `:app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest`: BUILD SUCCESSFUL.
- 7 pruebas instrumentadas en CarWash_API_35/API 35 sin fallos ni omisiones.
- Cobertura: campos obligatorios, totales/base, cambio de tipo, placa encontrada
	y ausente, caja cerrada, guardado completo/parcial, doble envio, respuesta
	obsoleta, persistencia de servidor, marcador de envio incierto y redirecciones.
- Capturas generadas en `app/build/reports/ingreso-vehiculo.png` e
	`app/build/reports/ingreso-servicios.png`, inspeccionadas sin solapamientos.
- Conexion real desde Android: Conectado y Caja abierta, catalogos reales.
- Tipo Automoviles y Lavado Exterior y Aspirado seleccionados localmente:
	tarifa y total $30.000,00, sin pulsar Guardar ni escribir datos reales.
- El usuario autorizo la configuracion LAN y reinicio de la API mediante pregunta
	de confirmacion. Solo se modifico launchSettings del proyecto externo:
	nuevo perfil wifi, perfiles anteriores conservados, sin endpoints nuevos.
- API reiniciada con `dotnet run --no-build --launch-profile wifi`.
- Escucha confirmada en 127.0.0.1, ::1 y 192.168.1.15, puerto 5197.
- GET tipos por localhost y direccion Wi-Fi: 200 y 6 tipos.
- Android se conecto usando la IP Wi-Fi; direccion persistida en el emulador.
- Captura adicional: `app/build/reports/ingreso-conexion-wifi.png`.
- Wi-Fi clasificada Publica en Windows. No se modifico el perfil ni el firewall;
	T013 requiere revision/accion manual del usuario, con elevacion si corresponde.
- T011 requiere celular fisico y revision de T013; la conexion emulada no lo sustituye.
- La API devuelve `X-Server-Date` en `ByFecha` sin parámetro; el móvil usa esa fecha
  para evitar generar turnos del día UTC cuando el servidor aún está en el día local.