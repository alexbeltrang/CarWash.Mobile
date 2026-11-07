# Decisiones: empresa y recibos

- Decision: ficha unica en API con Revision y actualizacion condicional.
  Motivo: todos los clientes usan mismos datos; evita sobrescritura concurrente.
  Alternativa descartada: preferencias separadas por equipo.
- Decision: logo PNG/JPEG hasta 1 MB, dimensiones hasta 4096 px; normalizar a
  PNG de hasta 512 px. Motivo: misma imagen en Android, escritorio y papel.
  Alternativa descartada: rutas locales y URL remotas arbitrarias.
- Decision: politica administrativa existente protege PUT; GET exige sesion.
  Motivo: reutilizar autenticacion real sin autenticar por cabecera de dispositivo.
- Decision: snapshot JSON versionado en Texto con lectura de texto antiguo.
  Motivo: conservar logo/datos de trabajos pendientes sin migrar cada trabajo.
- Decision: electronica diferida; datos tributarios solo informativos segun RUT.
  Motivo: ultima instruccion del usuario. No quedan aclaraciones fiscales activas.