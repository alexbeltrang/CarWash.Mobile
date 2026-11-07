# Implementation Plan: Empresa y recibos

**Branch**: sin rama nueva | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)
**Input**: `specs/008-empresa-facturacion/spec.md`; electronica diferida por usuario.

## Summary

Ficha unica Empresa en SQLite API, GET autenticado para clientes y PUT protegido
por politica Administrador. Edicion WinForms solamente; Android consulta en memoria.
Logo PNG normalizado, limitado y compartido como base64. Snapshot JSON de texto
y logo dentro de Texto del trabajo existente, con lectura compatible de texto legado.
La impresora Windows dibuja logo y texto dentro del ancho actual de 80 mm.
Comunicaciones y encabezados usan la identidad empresarial; CarWash sigue siendo software.
 
## Technical Context

**Language/Version**: C# net9.0, WinForms net481, Kotlin/Compose API26+.

**Primary Dependencies**: EF Core SQLite, System.Drawing.Common, Newtonsoft.Json, Retrofit/Gson.
**Storage**: Empresa: Id=1, JSON de datos, Revision; trabajo conserva snapshot.

**Testing**: xUnit/fixture temporal, JVM/Compose en celular fisico, MSBuild aislado.
**Target Platform**: Windows API/escritorio y Android fisico.

**Project Type**: Clientes existentes y API compartida.
**Performance Goals**: Una consulta de ficha por refresco; imagen limitada a 512 px.

**Constraints**: Sin datos ficticios reales, electronica, pagos, envios o papel de prueba.
**Scale/Scope**: Una empresa y edicion administrativa desde escritorio.

## Constitution Check

PASS: especificacion antes de codigo, recursos Android, perfiles existentes,
sin persistencia local nueva en Android, pruebas aisladas. No nuevos permisos
ni reglas fiscales. Despliegue API requiere autorizacion y respaldo posteriores.

## Project Structure

**Structure Decision**: Implementar en `../CarWashApi/{DTOs,Modelos,Services,Controllers}`,
`../CarWash/Presentacion/Administracion`, clientes Android y pruebas existentes.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Ninguna | Reutilizar proyectos y almacenamiento existentes | Sin plataforma nueva |
