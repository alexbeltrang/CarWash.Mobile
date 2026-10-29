# Plan - Ingreso de vehiculos

- Mantener Kotlin/Compose/Material 3 y minSdk 26.
- Retrofit/Gson y OkHttp para contratos JSON existentes, BigDecimal para importes.
- ViewModel conserva formulario durante rotacion; coroutines para red fuera del hilo UI.
- Preferencias locales guardan solo direccion del servidor y marcador de envio incierto,
  no datos de clientes. Marcador previo a escrituras; sin reintentos automaticos.
- Vista desplazable con placa/busqueda, tipo, datos, servicios, valores, notas,
  historial y barra inferior de guardar/cancelar. Ajustes de servidor en dialogo.
- INTERNET y HTTP local necesarios para la API existente; validar URL de red local
  o HTTPS sin credenciales embebidas. No registrar cuerpos HTTP en logs.
- Emulador: 10.0.2.2:5197. Celular: IP LAN/nombre del servidor configurable.
- El consecutivo y la fecha del turno usan la fecha operativa que informa la API,
  no la zona horaria local del dispositivo.
- Tras autorizacion explicita, agregar perfil wifi a launchSettings de la API
  externa y reiniciar su instancia; no cambiar endpoints, reglas ni datos.
  Lecturas reales para integracion; guardados solo contra MockWebServer.

## Verificacion

1. Pruebas JVM de importes, validacion, contratos JSON y secuencia de guardado.
2. Pruebas de ViewModel de estados, doble envio y consultas obsoletas.
3. Pruebas Compose con repositorio simulado, sin escrituras reales.
4. assembleDebug, lintDebug y connectedDebugAndroidTest.
5. Captura en emulador usando catalogos reales y ningun guardado de prueba real.