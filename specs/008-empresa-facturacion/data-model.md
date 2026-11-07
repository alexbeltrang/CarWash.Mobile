# Modelo: Empresa

Empresa: Id=1 (PK y CHECK), DatosJson TEXT, Revision INTEGER positiva.
Sin borrado de clientes, turnos o comprobantes. No contiene credenciales.

DTO datos: nombreComercial, razonSocial, tipoPersona, nit, digitoVerificacion,
direccion, municipio, departamento, pais, telefono, correo, responsabilidadesTributarias,
logoBase64 opcional. Identidad, NIT/contacto requeridos; tipo persona Natural/Juridica.
Campos limitados; NIT numerico 5-15 digitos y DV calculado con pesos modulo 11.
Texto de una linea, sin controles. Responsabilidades libres informativas segun RUT.
Logo PNG/JPEG valido, limites bytes/dimensiones y PNG normalizado por servidor.

PUT: revision=0 crea; revision vigente modifica; conflicto devuelve 409.
Cada cambio incrementa Revision atomicamente. DTO de lectura incluye revision.

Snapshot impresion: version=1, texto, logoBase64; contiene datos vigentes al
encolar. Texto legado sigue legible. Cambios de empresa no modifican trabajos anteriores.