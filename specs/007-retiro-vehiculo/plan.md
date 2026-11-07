# Implementation Plan: Comprobante y retiro del vehiculo

**Branch**: sin rama nueva | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

**Input**: `specs/007-retiro-vehiculo/spec.md`; decisiones confirmadas con usuario.

## Summary

Revision Android vigente: sustituir la seccion Retiro por Reimpresion del recibo,
con icono de impresora y etiqueta Reimprimir. Retirar revision, confirmacion y
estado de entrega de la pantalla y accion de entrega del ViewModel; conservar
consulta/actualizacion del recibo y cola de copias de 80 mm. Sin migracion,
cambios de servidor/escritorio ni borrado de datos legados en este ajuste movil.

Comprobante unico persistido en API y compartido en correo/WhatsApp manual.
Retiro autenticado desde Android y WinForms con revision de entrega fisica,
sin mutar pagos. Impresion USB por cola persistida y BackgroundService en API
Windows con System.Drawing.Common/PrintDocument; ambos clientes solicitan una
copia automatica de 80 mm al guardar nuevo ingreso y todos sus servicios, con
UUID estable asignado por servidor. Reimpresion explicita conserva UUID del
intento incierto. No codigo ni QR en el documento o como requisito de entrega.

## Technical Context

**Language/Version**: C# net9.0 API; WinForms net481; Kotlin/Compose Android API26+.

**Primary Dependencies**: EF Core SQLite, sesion API existente, Retrofit, System.Drawing.Common 9.0.14 para el trabajador Windows.

**Storage**: ComprobantesRetiro e ImpresionesComprobante en SQLite servidor; configuracion Printing local fuera de Git, sin datos nuevos en preferencias Android.

**Testing**: xUnit/SQLite temporal y HTTP con fixture; JVM/Compose en celular fisico; MSBuild Windows.

**Target Platform**: API Windows, Android fisico, escritorio Windows con controlador USB.

**Project Type**: Tres proyectos existentes; sin nueva aplicacion ni agente instalado, con adaptador de impresion Windows en la API actual.

**Performance Goals**: Consulta por placa e indices unicos por ingreso/codigo; una entrega atomica.

**Constraints**: Sin pagos nuevos, Meta, QR, envios reales, secretos en logs o pruebas en base operativa.

**Scale/Scope**: Un comprobante y a lo sumo una entrega por turno.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

PASS: especificacion previa; alcance API autorizado para retiro; Android nativo;
recursos de texto; datos solo en servidor; pruebas aisladas y documentacion.
No se cambia la constitucion. Despliegue posterior requiere autorizacion nueva.
Mismos resultados tras diseno; no hay aclaraciones pendientes ni hooks registrados.

## Project Structure

### Documentation (this feature)

```text
specs/007-retiro-vehiculo/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
```text
../CarWashApi/{Modelos,DTOs,Services,Controllers,Context}/
../CarWashApi.Tests/RetiroVehiculosTests.cs
app/src/main/java/com/carwash/mobile/retiro/
app/src/main/java/com/carwash/mobile/ingreso/
../CarWash/Presentacion/Operacion/FrmRetiroVehiculo.cs
../CarWash/Controladores/RetiroVehiculosController.cs
```

**Structure Decision**: Reutilizar sesiones y clientes HTTP de cada proyecto;
servicio API unico para emitir/consultar/entregar; pantalla compacta por cliente.
Revision central: servicio de solicitudes/estado, trabajador y adaptador Windows;
fixtures siempre deshabilitan impresora fisica, excepto hoja diagnostica optativa
autorizada. Un cambio futuro de equipo requiere revisar acceso USB/compartido.

Revision vigente: ruta de impresion de ingreso valida servicios completos dentro
de transaccion; fallo de impresion no bloquea ingreso confirmado. Controlador
termico debe ofrecer papel nominal de 80 mm; HP actual usa franja sobre hoja.
Se retiro la propuesta de QR/camara por decision del usuario antes de desplegar;
sin bibliotecas nuevas de QR y sin cambios a captura de placas o envios digitales.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Ninguna | Alcance solicitado | Sin nuevas capas o protocolos USB |
