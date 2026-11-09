# Feature Specification: Factura de venta en papel

**Feature Branch**: `009-factura-papel` (directorio, sin rama nueva)
**Created**: 2026-11-06
**Status**: Alcance confirmado
**Input**: Implementar facturacion normal no electronica. Usuario confirma factura
de venta en papel con numeracion autorizada, preparada al cobrar/finalizar desde
escritorio, y configuracion de impuestos/numeracion desde escritorio.

## Clarifications

- Una empresa existente, configurada por administrador en escritorio.
- Factura en papel, no cuenta de cobro ni factura electronica.
- Emision desde escritorio al finalizar el cobro; abonos no generan factura final.
- Administrador configura autorizacion, rango y tasa de IVA; no valores asumidos.
- No modificar cobros: el IVA configurado se desglosa dentro del valor confirmado.
- Revision 2026-11-06: usuario reemplaza el check automatico por preguntar
  siempre tras cobro completo confirmado si desea generar factura, Si/No con
  No predeterminado. No cierra sin factura; Si abre formulario del comprador.
- Nombre y tipo/numero de documento requeridos para emitir; direccion opcional.
  La eleccion Si sustituye la segunda confirmacion de emision para ese turno.
  Impresion permanece explicita, sin impresion automatica al cobrar.

## User Scenarios & Testing

### User Story 1 - Configurar facturacion (Priority: P1)

Administrador registra autorizacion para facturar en papel, prefijo/rango,
vigencias, siguiente numero y tasa de IVA incluida en el importe cobrado.
**Why this priority**: No emitir con datos fiscales inventados o fuera de rango.
**Independent Test**: Guardar configuracion valida como administrador y rechazar
perfil operativo, revision antigua y siguiente numero ya utilizado.
**Acceptance Scenarios**:
1. **Given** administrador, **When** configura datos validos, **Then** se conservan
   y pueden editarse sin renumerar facturas ya emitidas.
2. **Given** configuracion incompleta, vencida o rango agotado, **When** emite,
   **Then** no se crea factura ni se consume consecutivo.
3. **Given** operativo, **When** modifica configuracion, **Then** se rechaza.

### User Story 2 - Facturar al finalizar (Priority: P1)

Despues del cobro completo, escritorio prepara factura y solicita identificacion
del comprador. Empresa, autorizacion y valores se confirman antes de emitir.
**Why this priority**: Vincular documento con el servicio realmente cobrado.
**Independent Test**: Finalizar turno sintetico y emitir una factura con empresa,
comprador, servicios, base/IVA/total y consecutivo unico sin modificar caja.
**Acceptance Scenarios**:
1. **Given** turno pagado/finalizado y comprador identificado, **When** confirma,
   **Then** emite una factura unica con el total confirmado y desglose de IVA.
2. **Given** abono o turno no finalizado, **When** intenta emitir, **Then** rechaza
   sin crear documento ni alterar el cobro.
3. **Given** dos solicitudes o respuesta perdida, **When** reintenta, **Then** no
   duplica factura del turno ni asigna el mismo numero a dos facturas.
4. **Given** datos de empresa/configuracion cambiados durante la revision,
   **When** confirma, **Then** solicita refrescar antes de emitir.

### User Story 3 - Consultar y reimprimir (Priority: P2)

Personal autenticado consulta facturas y prepara otra impresion explicita.
**Why this priority**: Recuperar documento sin emitir otra factura ni cobrar otra vez.
**Independent Test**: Editar empresa/configuracion tras emitir y comprobar que
consulta y reimpresion conservan empresa/logo, comprador, numero y valores originales.
**Acceptance Scenarios**:
1. **Given** factura existente, **When** consulta/reimprime, **Then** conserva snapshot.
2. **Given** error de impresora, **When** reintenta consultar, **Then** factura y
   cobro permanecen intactos; no declara salida fisica confirmada.

### Edge Cases

Comprador incompleto, empresa ausente, rango/vigencia invalidos, configuracion
concurrente, numeros ya usados, abonos, perdida de respuesta, impresion incierta,
nombre/domicilio largos y datos antiguos sin factura.

## Requirements

- **FR-001**: Configuracion de facturacion unica administrada exclusivamente por
  administrador en escritorio, con permiso aplicado tambien al guardar.
- **FR-002**: Registrar numero/fecha de autorizacion, prefijo, rango, vigencia,
  siguiente consecutivo, tasa de IVA explicita y habilitacion para papel. No
  asumir tasa, numeros ni validar remotamente habilitacion DIAN.
- **FR-003**: Rechazar emision sin empresa/configuracion vigente, comprador
  identificado o turno completamente pagado/finalizado.
- **FR-004**: Usar importe confirmado del turno, sin sumar precios de catalogo
  que puedan diferir del valor acordado. Concepto agrupado de servicios realizados,
  cantidad uno; total incluye IVA configurado, con base e IVA desglosados.
- **FR-005**: Persistir una factura por turno y numeros unicos por prefijo,
  asignados atomicamente; repetir no consume otro consecutivo.
- **FR-006**: Congelar empresa/logo, autorizacion, comprador, servicios, fecha,
  base, tasa, IVA y total; no modificar facturas emitidas al editar configuracion.
- **FR-007**: Preparar facturacion tras confirmar finalizacion del cobro en escritorio;
  cancelar o fallar la factura no vuelve a cobrar ni deshace el pago confirmado.
  Preguntar Si/No con No predeterminado. No cierra sin emitir ni imprimir;
  Si solicita los datos del comprador antes de emitir.
- **FR-008**: Consultar facturas y reimprimir mediante cola de impresora existente
  de 80 mm, con confirmacion para otra copia e idempotencia de cada solicitud.
- **FR-009**: No emitir factura electronica, CUFE, QR fiscal ni enviar a DIAN.
  No presentar configuracion del software como certificacion de cumplimiento legal.
- **FR-010**: No introducir cobros, factura ni editor fiscal en Android.
- **FR-011**: Probar configuracion/permisos, concurrencia, impuestos, snapshots,
  conservacion de cobros e impresion simulada; sin facturas ficticias reales ni papel.
- **FR-012**: Tras confirmar pago completo, preguntar si desea generar factura.
  No (predeterminado) no abre formulario, no consume numeracion y cierra registro.
  Si abre comprador: nombre/tipo/numero requeridos, direccion opcional. Emitir
  para el mismo turno no vuelve a preguntar. Consulta manual conserva confirmacion.
- **FR-013**: Entregar scripts de creacion de estructura vacia SQLite y SQL Server
  incluyendo tablas del escritorio, notificaciones, empresa, impresion y factura.
  Sin copiar registros, usuarios, secretos ni configuracion fiscal. SQLite se
  verifica en base temporal; SQL Server no implica migrar ni cambiar el proveedor
  SQLite de la API. Documentar requisitos y limites de verificacion del servidor.
- **FR-014**: Formularios nuevos de escritorio (Empresa, Configuracion de
  facturacion, Facturas, Comprobante y retiro, Reporte diario) usan el estilo de
  Apertura/Cierre de caja: encabezado azul, fondo claro, Segoe UI y botones planos
  verde/azul, con cierre accesible y contenido sin solaparse al redimensionar.
  Sus opciones se agrupan en Administracion, conservando permisos existentes;
  mover una opcion no otorga permisos administrativos ni modifica operaciones.
- **FR-015**: Formularios nuevos editables en el disenador WinForms mediante
  clases parciales, InitializeComponent, Designer.cs y resx registrados en el
  proyecto. Declarar controles, contenedores y estilo en Designer.cs, no construir
  ni reordenar su estructura desde constructores/helpers. Conservar eventos,
  validaciones y permisos; no consultar API al abrir en modo diseno. Opciones
  nuevas del menu tambien declaradas en disenador y sin iconos a la izquierda.
  Para el proyecto .NET Framework x64, activar el disenador WinForms fuera de
  proceso en Visual Studio; conservar x64 de ejecucion y referencias del framework.
- **FR-016**: Tablas WinForms usan sus barras de desplazamiento nativas, sin
  duplicarlas como controles independientes en Designer.cs. Compilar proyecto y
  solucion sin argumentos de arquitectura adicionales debe seleccionar binario
  x64 y bibliotecas SQLite nativas compatibles, tanto en Debug como Release.

### Instalador Seleccionable De Escritorio

- **FR-017**: Proyecto generador de instalador en la solucion CarWash para
  Windows10 22H2/Windows11 x64, con seleccion independiente de escritorio y API
  local; base nueva disponible solo junto a API. Incluir .NET Framework4.8.1,
  redistribuible Visual C++ x64 firmado por Microsoft,
  DLL administradas/SQLite x64 y API autocontenida para instalacion sin Internet,
  Visual Studio ni SDK en destino. Solicitar elevacion solo al instalar.
- **FR-018**: No copiar bases, usuarios, SMTP, tokens ni configuracion de produccion.
  Base nueva vacia desde esquema versionado, usuario administrador elegido
  durante instalacion sin clave predeterminada. Rechazar creacion si ya existe
  base; API sin base nueva exige base existente compatible. Escritorio solo
  solicita URL de API externa. Configuracion y datos existentes se conservan
  al actualizar/desinstalar. No instalar componentes reales en el equipo de
  desarrollo como parte de las pruebas; documentar limites de maquina limpia.
  En instalaciones nuevas, archivo local heredado del escritorio en
  %LOCALAPPDATA%/CarWash, separado de la base API y sin requerir administrador
  para ejecutar el cliente. Expandir variables de NameLDB, conservando rutas
  existentes cuando no contienen variables. Verificar base existente en solo
  lectura (integridad SQLite y tablas principales), sin importar registros ni
  ejecutar migraciones destructivas. Crear directorio de logs del lanzador
  tambien cuando se elija una ruta personalizada para la base.

### Key Entities

Configuracion de facturacion: datos autorizados y siguiente numero.
Factura: documento inmutable asociado a turno pagado, con comprador y snapshot.
Comprador: nombre, tipo/numero de documento y contacto suministrados para esa factura.

## Success Criteria

- **SC-001**: Cero documentos/consecutivos duplicados al repetir o emitir concurrentemente.
- **SC-002**: Base mas IVA coincide exactamente con total confirmado.
- **SC-003**: Facturar/reimprimir no modifica turnos, pagos, comisiones ni caja.
- **SC-004**: Cero emisiones con autorizacion vencida, rango agotado o datos obligatorios ausentes.
- **SC-005**: Factura/logo legibles en vista previa de 80 mm y snapshot intacto tras editar empresa.

## Assumptions

Operativos autenticados pueden emitir/consultar; solo administrador configura.
El usuario verifica con su asesor la habilitacion para usar factura en papel y
aporta autorizacion real. Tasa cero debe configurarse expresamente; sin configuracion
no se emite. No se agregan impuestos al cobro ni se reconstruyen descuentos por
servicio: se respeta el importe confirmado. Identificacion del comprador no se inventa.
Correccion de sesion movil se documenta separadamente en specs/005-asignacion-operario.