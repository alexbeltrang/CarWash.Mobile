# Contrato: captura y confirmacion

## Entrada y salida

- Entrada: borrador de ingreso existente; camara solo al solicitar captura.
- Salida exitosa: placa revisada, trim y mayusculas, sin reemplazar O/0 o I/1.
- Cancelar/manual: no modifica borrador; devuelve foco al campo placa.
- Lectura vacia/error: no modifica ingreso; permite nueva captura o manual.
- Confirmacion: solo edit de placa y lookup existente cuando hay conexion.
  Cero llamadas de registro; guardar sigue siendo una accion separada.
- Si cambia la placa confirmada, descartar cliente, celular, marca, tipo,
  servicios y sus valores previos antes de consultar; conservar observaciones.
  Una consulta fallida no debe mezclar vehiculos. No existe campo de orden editable;
  el registro envia numeroOrden="0" conforme al contrato 002.

## Estados

Permiso pendiente/denegado -> manual, ajustes o nueva solicitud explicita.
Camara disponible -> captura -> reconocimiento -> revision editable.
Error/vacio -> reintento o manual. Salir cancela entrega de resultados tardios.
Una captura simultanea; liberar camara al mostrar revision o abandonar.

## Datos y red

Fotos solo en memoria, sin galeria ni archivos de cache. OCR incluido en APK.
No se envian imagenes ni se agregan rutas. Tras confirmar se reutilizan GET
api/Turnos/BuscarPorPlaca y api/Turnos/HistorialPorPlaca del contrato 002.
Guardar conserva sin cambios la secuencia no atomica y las advertencias de 002.
La fotografia y el reconocimiento no requieren servidor; consultar/registrar si.

## Permisos

CAMERA en tiempo de ejecucion; hardware android.hardware.camera.any opcional.
Sin READ_MEDIA_IMAGES, WRITE_EXTERNAL_STORAGE, audio o ubicacion.
No solicitar automaticamente de nuevo tras denegacion; ajustes solo por accion.