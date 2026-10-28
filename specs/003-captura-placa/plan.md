# Plan: Captura de placa

## Arquitectura

- Mantener Kotlin, Compose, Material 3, API 26 y el ingreso 002.
- CameraX 1.4.2 (camera-camera2, camera-lifecycle y camera-view), captura en memoria,
  PreviewView ajustado al ciclo de vida; permiso CAMERA y hardware opcional.
- ML Kit text-recognition 16.0.1 con modelo latino incluido, sin descarga inicial.
- Separar sugerencias/revision puras de captura hardware. Ningun archivo de foto,
  permiso de almacenamiento, envio de imagen o cambio en servidor.
- Revision siempre editable y explicita. Heuristica alfanumerica de 5 a 10 caracteres,
  con letras y numeros, solo para sugerir; no cambia validacion manual del ingreso.
- Conectar confirmacion a edit/lookup existentes; preservar guardado y protecciones.
- Limpiar datos del vehiculo anterior al confirmar otra placa; conservar orden y notas.
- Ajustar contraste oscuro e iconos de sistema ante cambio de tema observado en captura.
- La captura queda ligada al ciclo de vida; cerrar ImageProxy y reconocedor incluso
  en error, salir o recrear. Cancelar descarta resultados sin tocar borrador.

## Verificacion

- JVM: sugerencias, separadores, ambiguedad, resultados vacios, seleccion/correccion,
  confirmacion sin escritura y rechazo de cambio durante guardado/revision incierta.
- Android: OCR real de bitmap sintetico, revision/cancelacion/manual con fuentes
  grandes y orientacion; regresion de formulario con repositorio simulado.
- Ejecutar assembleDebug, lintDebug, testDebugUnitTest y connectedDebugAndroidTest.
- Inspeccionar preview y fallback en emulador sin escribir ingresos reales.
- Pendiente separado: precision, reflejos, baja luz y autofocus en celular fisico.

## Constitucion

Permiso de camara autorizado por el requisito del usuario; imagenes exclusivamente
en memoria. No modifica persistencia de clientes, API ni reglas de negocio.