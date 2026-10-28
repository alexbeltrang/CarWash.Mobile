<!--
Sync Impact Report
Version: 1.0.0 -> 1.1.0 (ampliacion de guia de integracion y privacidad).
Principio modificado: IV. Privacidad y seguridad (mismo titulo).
Secciones agregadas/eliminadas: ninguna.
Impacto: specs/002-ingreso-vehiculo autoriza API existente y permiso INTERNET.
Pendientes de plantilla: ninguno.
-->
# Constitucion de CarWash.Mobile

## Principios

### I. Especificacion antes de funcionalidad
Cada funcionalidad debe tener criterios de aceptacion en `specs/` antes de su
implementacion. Todo cambio funcional actualiza especificaciones, contratos
afectados y tareas en el mismo trabajo.

### II. Android nativo y simplicidad
Usar Kotlin, Jetpack Compose y Material 3, con Android minimo API 26.
Agregar capas y dependencias solo cuando un requisito aprobado lo necesite.

### III. Calidad verificable
Verificar compilacion y lint. Agregar pruebas para el comportamiento modificado.
Registrar evidencia real y distinguir pruebas pendientes de pruebas exitosas.

### IV. Privacidad y seguridad
Solicitar solo permisos necesarios. No guardar secretos ni claves de firma en
el repositorio. El formulario de ingreso puede usar INTERNET para consumir la
API especificada. Guardar localmente solo la direccion del servidor y el marcador
de envio pendiente; no persistir clientes ni registrar cuerpos HTTP en logs.
Limitar HTTP sin cifrar a servidores locales configurados y redes privadas
confiables. No escribir registros ficticios en la base real para ejecutar pruebas.

### V. Experiencia movil accesible
Externalizar textos, respetar areas seguras, fuente y tema del sistema.
No bloquear la orientacion sin justificacion.

## Restricciones de plataforma

La base usa JDK 17 y SDK Android 35. El identificador `com.carwash.mobile` es
provisional hasta confirmar el dominio de la organizacion antes de publicar.
No incorporar backend, reservas, pagos ni autenticacion sin especificacion.

## Flujo de trabajo

Usar `/speckit-specify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement`
y `/speckit-converge` en Copilot Chat, revisando resultados entre etapas.
Modificar principios con `/speckit-constitution`. No crear ramas ni commits
automaticamente sin autorizacion.

## Gobierno

Esta constitucion gobierna las decisiones del proyecto. Los cambios requieren
actualizar su version y describir el impacto en especificaciones existentes.
Usar versionado semantico: cambios incompatibles de principios incrementan MAJOR,
guia ampliada incrementa MINOR y aclaraciones incrementan PATCH. Cada revision
de cambios debe comprobar su cumplimiento y registrar excepciones.

**Version**: 1.1.0 | **Ratificacion**: 2026-10-28 | **Ultima modificacion**: 2026-10-28
