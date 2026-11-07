# Decisions: Factura en papel

- Decision: total confirmado, no suma de catalogo. Rationale: cobro actual admite
  acuerdos y abonos; factura no debe reconstruir precios ni generar otro cobro.
  Alternative: distribuir diferencias entre servicios, rechazada por inventar descuentos.
- Decision: IVA incluido con tasa explicita administrativa; redondeo decimal
  a dos posiciones AwayFromZero y IVA como total menos base. No default de tasa.
- Decision: SQLite transaccion de escritura, indices unicos turno y prefijo-numero;
  contador separado del JSON. Settings revision cambia solo al editar configuracion.
- Decision: snapshot completo al emitir, impresion central reutiliza texto/logo
  congelados. No re-render con empresa vigente al reimprimir factura existente.
- Decision: autorizacion/vigencia/rango suministrados por administrador; software
  no acredita elegibilidad fiscal ni integra DIAN. No investigadores externos
  necesarios para stack local conocido; cumplimiento real depende del usuario/asesor.