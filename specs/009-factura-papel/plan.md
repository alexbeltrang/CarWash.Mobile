# Implementation Plan: Factura de venta en papel

**Branch**: sin rama nueva | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

**Input**: `specs/009-factura-papel/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Configuracion unica administrativa y facturas inmutables en SQLite API.
Escritorio configura autorizacion/IVA, prepara comprador tras cobro final,
emite, consulta historial y solicita impresion central de 80 mm.
Transaccion SQLite asigna numero y factura unica por turno atomicamente.
Total del turno confirmado; concepto agrupado con base/IVA incluidos en ese total.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: C# net9.0 y WinForms net481.

**Primary Dependencies**: EF Core SQLite, Newtonsoft, impresora Windows existente.

**Storage**: ConfiguracionFacturacion Id1/JSON/Revision/SiguienteNumero; Factura
IdUUID/IdTurno/Prefijo/Numero/DatosJson con indices unicos turno y prefijo-numero.

**Testing**: xUnit con SQLite temporal, cliente/formulario escritorio compilados y renderer simulado.

**Target Platform**: Windows; Android sin emision fiscal.

**Project Type**: API y escritorio existentes.

**Performance Goals**: Transaccion corta por emision; historial acotado a 100 facturas.

**Constraints**: Sin electronica, tasas/numeracion ficticias, cambios de cobro ni pruebas reales de papel.

**Scale/Scope**: Una empresa, una configuracion y una factura por turno finalizado.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

PASS: alcance aprobado antes del codigo, permisos existentes, SQLite temporal,
sin cobros Android ni credenciales nuevas. Despliegue requiere aprobacion y respaldo.

## Project Structure

**Structure Decision**: API `../CarWashApi/{Modelos/Factura.cs,DTOs/FacturaDto.cs,
Services/FacturacionService.cs,Controllers/FacturasController.cs}` y contexto/startup;
escritorio `../CarWash/{DTOs/FacturaDTO.cs,Controladores/FacturasController.cs,
Presentacion/Administracion/FrmFacturacion.cs,Presentacion/Operacion/FrmFactura.cs}`;
enlace desde `FrmGestionDetalleVehiculo.cs` y menu `FrmPrincipal.cs`.
Pruebas `../CarWashApi.Tests/FacturacionTests.cs` y probe compilado existente.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Ninguna | Reutilizar API, WinForms y cola existentes | Sin plataforma ni motor fiscal externo |
