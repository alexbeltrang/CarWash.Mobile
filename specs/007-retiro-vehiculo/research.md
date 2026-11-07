# Decisiones

- Decision: codigo criptograficamente aleatorio de 16 digitos hexadecimales,
  persistido una vez por turno. Motivo: no deducible de consecutivo/placa, admite
  ingreso manual y reimpresion. Descartado: consecutivo como autorizacion.
- Decision: sesion API existente y confirmacion explicita del encargado.
  Motivo: usuario eligio revision manual sin requisito de pago. Descartado:
  cierre/pago automatico y retiro anonimo.
- Decision: entrega con UPDATE condicional y fecha/usuario solo si no existe
  entrega. Motivo: resolver dobles clics y concurrencia en servidor.
- Decision: PrintDocument de Windows, papel del controlador y vista previa.
  Motivo: termica USB sin modelo/protocolo confirmado. Descartado: ESC/POS crudo,
  nuevo permiso USB Android o una libreria de hardware inventada.
- Decision: no QR ni excepcion por codigo perdido en esta revision.
- No se requiere investigacion delegada; decisiones resueltas con codigo local.