# Contrato: Empresa y logo

GET /api/Empresa: Bearer sesion existente; 200 DTO o 204 si no configurada.
PUT /api/Empresa: Bearer y politica Administrador; 200 DTO actualizado.
JSON datos de empresa y revision segun data-model.md. logoBase64=null elimina
logo; la ficha completa se reemplaza condicionalmente por revision.
400 datos/logo invalidos; 401 sin sesion; 403 no administrador; 409 revision
conflictiva; 503 almacenamiento o procesamiento de imagen no disponible. Cache-Control:no-store.
No aceptar Id de empresa ni usuario proporcionado por cliente. Limite cuerpo 2 MB.

Escritorio: menu Empresa solo administrador, cargar/guardar ficha, cargar/quitar
logo y vista previa. En conflicto solicitar recarga, nunca sobrescribir en silencio.
Android: consulta en memoria por sesion al navegar, volver a primer plano o
refrescar ingreso; identidad visible comun, sin edicion. Fallo limpia identidad;
401 vuelve al acceso. No persistir ficha ni logo en preferencias.
Recibos/correos/WhatsApp manual usan datos actuales; impresion conserva snapshot.
WhatsApp manual congela nombre, NIT, domicilio/contacto al preparar el borrador;
el enlace admite texto, no adjunta imagen. Meta conserva su contrato existente.
Logo: PNG/JPEG hasta 1 MB y 4096 pixeles por lado; servidor normaliza PNG hasta
512 pixeles conservando proporcion. null retira el logo. NIT 5-15 digitos ASCII,
DV calculado; responsabilidades opcionales informativas segun RUT.
Sin empresa, mostrar CarWash solo como software y nunca como emisor del recibo.
No cambiar endpoints fiscales, notificacion Meta ni autenticacion existente.