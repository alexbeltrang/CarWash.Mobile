PRAGMA foreign_keys = ON;
BEGIN IMMEDIATE;

CREATE TABLE IF NOT EXISTS "AsistenciaOperario" (
    "idAsistenciaOperario" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "idOperario" INTEGER,
    "Fecha" BIGINT,
    "Asistio" INTEGER,
    "Autorizado" INTEGER,
    "Observacion" VARCHAR
);
CREATE TABLE IF NOT EXISTS "CajaDiaria" (
    "idCaja" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "FechaApertura" BIGINT,
    "FechaCierre" BIGINT,
    "MontoInicial" FLOAT,
    "TotalIngresosEfectivo" FLOAT,
    "TotalIngresosTransferencias" FLOAT,
    "TotalIngresosDatafono" FLOAT,
    "TotalIngresosCredito" FLOAT,
    "TotalEgresos" FLOAT,
    "TotalValesOperarios" FLOAT,
    "TotalFinal" FLOAT,
    "Estado" INTEGER
);
CREATE TABLE IF NOT EXISTS "ClienteCredito" (
    "idClienteCredito" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Nombre" VARCHAR,
    "Telefono" VARCHAR,
    "Direcion" VARCHAR,
    "Correo" VARCHAR,
    "ciudad" VARCHAR,
    "Estado" INTEGER
);
CREATE TABLE IF NOT EXISTS "ComprobantesRetiro" (
    "IdTurno" INTEGER NOT NULL CONSTRAINT "PK_ComprobantesRetiro" PRIMARY KEY,
    "Codigo" TEXT NOT NULL,
    "CreadoEn" TEXT NOT NULL,
    "EntregadoEn" TEXT NULL,
    "EntregadoPor" INTEGER NULL
);
CREATE TABLE IF NOT EXISTS "ConfiguracionFacturacion" (
    "Id" INTEGER NOT NULL CONSTRAINT "PK_ConfiguracionFacturacion" PRIMARY KEY CHECK (Id = 1),
    "DatosJson" TEXT NOT NULL,
    "Revision" INTEGER NOT NULL,
    "SiguienteNumero" INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS "EmailNotifications" (
    "Id" INTEGER NOT NULL CONSTRAINT "PK_EmailNotifications" PRIMARY KEY AUTOINCREMENT,
    "IdTurno" INTEGER NOT NULL,
    "Destinatario" TEXT NOT NULL,
    "Estado" TEXT NOT NULL,
    "Intentos" INTEGER NOT NULL,
    "UltimoErrorCodigo" TEXT NULL,
    "CreadoEn" TEXT NOT NULL,
    "ActualizadoEn" TEXT NOT NULL,
    "ProximoIntentoEn" TEXT NULL
);
CREATE TABLE IF NOT EXISTS "Empresa" (
    "Id" INTEGER NOT NULL CONSTRAINT "PK_Empresa" PRIMARY KEY CHECK (Id = 1),
    "DatosJson" TEXT NOT NULL,
    "Revision" INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS "Facturas" (
    "Id" TEXT NOT NULL CONSTRAINT "PK_Facturas" PRIMARY KEY,
    "IdTurno" INTEGER NOT NULL,
    "Prefijo" TEXT NOT NULL,
    "Numero" INTEGER NOT NULL,
    "Fecha" TEXT NOT NULL,
    "Placa" TEXT NOT NULL,
    "DatosJson" TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS "Festivos" (
    "IdFestivo" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Fecha" BIGINT,
    "Descripcion" VARCHAR
);
CREATE TABLE IF NOT EXISTS "FormaPago" (
    "IdFormaPago" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Nombre" VARCHAR
);
CREATE TABLE IF NOT EXISTS "GastosCaja" (
    "IdGasto" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "idCaja" INTEGER,
    "FechaRegistro" BIGINT,
    "Concepto" VARCHAR,
    "Valor" FLOAT,
    "Observaciones" VARCHAR
);
CREATE TABLE IF NOT EXISTS "ImpresionesComprobante" (
    "SolicitudId" TEXT NOT NULL CONSTRAINT "PK_ImpresionesComprobante" PRIMARY KEY,
    "IdTurno" INTEGER NOT NULL,
    "AnchoMm" INTEGER NOT NULL,
    "SolicitadoPor" INTEGER NOT NULL,
    "Impresora" TEXT NOT NULL,
    "Estado" TEXT NOT NULL,
    "Texto" TEXT NOT NULL,
    "CreadoEn" TEXT NOT NULL,
    "ActualizadoEn" TEXT NOT NULL,
    "ErrorCodigo" TEXT NULL
);
CREATE TABLE IF NOT EXISTS "JornadaOperario" (
    "idJornada" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "HoraIngreso" BIGINT,
    "HoraSalida" BIGINT,
    "Estado" INTEGER,
    "idCaja" INTEGER,
    "idOperario" INTEGER
);
CREATE TABLE IF NOT EXISTS "LiquidacionOperario" (
    "idLiquidacionOperario" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "idOperario" INTEGER,
    "FechaInicial" BIGINT,
    "FechaFinal" BIGINT,
    "TotalServicios" INTEGER,
    "TotalFacturado" FLOAT,
    "Comision" FLOAT,
    "Vales" FLOAT,
    "TotalPagado" FLOAT,
    "FechaLiquidacion" BIGINT
);
CREATE TABLE IF NOT EXISTS "Modulos" (
    "ModuloId" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Descripcion" VARCHAR(150),
    "IsDelete" INTEGER,
    "ModuloPadre" INTEGER,
    "OrdenPresentacion" INTEGER,
    "NombreMenuApp" VARCHAR(300)
);
CREATE TABLE IF NOT EXISTS "OperarioComisiones" (
    "IdOperarioComision" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "idOperario" INTEGER,
    "Porcentaje" FLOAT,
    "DiaSemana" INTEGER,
    "FechaInicio" BIGINT,
    "FechaFin" BIGINT
);
CREATE TABLE IF NOT EXISTS "Operarios" (
    "idOperario" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Nombres" VARCHAR(80),
    "Apellidos" VARCHAR(80),
    "Email" VARCHAR(150),
    "Celular" VARCHAR(20),
    "isDelete" INTEGER,
    "UltimaAsignacion" BIGINT,
    "CargaTrabajo" INTEGER
);
CREATE TABLE IF NOT EXISTS "PerfilModulo" (
    "PerfilModuloId" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "ModuloId" INTEGER,
    "PerfilId" INTEGER,
    "isDeleted" INTEGER
);
CREATE TABLE IF NOT EXISTS "Perfiles" (
    "PerfilId" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "NombrePerfil" VARCHAR(150),
    "isDelete" INTEGER
);
CREATE TABLE IF NOT EXISTS "PrecioServicioVehiculo" (
    "IdServicioVehiculo " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "idServicio" INTEGER,
    "IdTipoVehiculo" INTEGER,
    "Precio" FLOAT,
    "PrecioBaseComision" FLOAT
);
CREATE TABLE IF NOT EXISTS "RegistroPropinasOperarios" (
    "idPropina" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "idOperario" INTEGER,
    "fechaRegistro" BIGINT,
    "valorPropina" FLOAT,
    "observaciones" VARCHAR,
    "idCaja" INTEGER,
    "isDelete" INTEGER
);
CREATE TABLE IF NOT EXISTS "Servicios" (
    "idServicio" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Nombre" VARCHAR,
    "IsDelete" INTEGER
);
CREATE TABLE IF NOT EXISTS "TipoVehiculo" (
    "idTipoVehiculo" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Nombre" VARCHAR(50),
    "IsDelete" INTEGER
);
CREATE TABLE IF NOT EXISTS "TurnoServicios" (
    "idTunoServicios" INTEGER PRIMARY KEY AUTOINCREMENT,
    "idServicios" INTEGER,
    "IdTurno" INTEGER,
    "IsDeleted" INTEGER
);
CREATE TABLE IF NOT EXISTS "Turnos" (
    "IdTurno" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "NumeroTurno" VARCHAR,
    "NombreCliente" VARCHAR,
    "NumeroCelular" VARCHAR,
    "CorreoElectronico" VARCHAR,
    "Placa" VARCHAR,
    "FechaHoraIngreso" BIGINT,
    "FechaHoraFin" BIGINT,
    "Marca" VARCHAR,
    "NumeroOrden" VARCHAR,
    "Valor" FLOAT,
    "ValorBaseComision" FLOAT,
    "PorcentajeComision" FLOAT,
    "ValorComision" FLOAT,
    "Pagado" INTEGER,
    "PagadoNomina" INTEGER,
    "Observaciones" VARCHAR,
    "FechaHoraAsignacionOperario" BIGINT,
    "Estado" INTEGER,
    "IdTipoVehiculo" INTEGER,
    "idOperario" INTEGER,
    "OperadorOcupado" INTEGER,
    "idCajaDiaria" INTEGER,
    "idClienteCredito" INTEGER
);
CREATE TABLE IF NOT EXISTS "TurnosDiarios" (
    "Id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "Fecha" VARCHAR,
    "Consecutivo" INTEGER
);
CREATE TABLE IF NOT EXISTS "TurnosMovimientos" (
    "IdTurnoMovimientos" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "MontoPagado" FLOAT,
    "FechaMovimiento" BIGINT,
    "IdTurno" INTEGER,
    "IdFormaPago" INTEGER
);
CREATE TABLE IF NOT EXISTS "Usuarios" (
    "idUser" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "UserName" VARCHAR,
    "password" VARCHAR,
    "Nombres" VARCHAR(80),
    "Apellidos" VARCHAR(80),
    "DisplayName" VARCHAR(150),
    "Email" VARCHAR(500),
    "isDelete" INTEGER,
    "PerfilId" INTEGER
);
CREATE TABLE IF NOT EXISTS "ValesOperarios" (
    "IdValeOperario" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "idOperario" INTEGER,
    "idCaja" INTEGER,
    "FechaRegsitro" BIGINT,
    "Valor" FLOAT,
    "Motivo" VARCHAR,
    "Descontado" INTEGER,
    "FechaDescuento" BIGINT
);
CREATE TABLE IF NOT EXISTS "WhatsAppNotifications" (
    "Id" INTEGER NOT NULL CONSTRAINT "PK_WhatsAppNotifications" PRIMARY KEY AUTOINCREMENT,
    "IdTurno" INTEGER NOT NULL,
    "DestinatarioNormalizado" TEXT NOT NULL,
    "Estado" TEXT NOT NULL,
    "Intentos" INTEGER NOT NULL,
    "MessageIdExterno" TEXT NULL,
    "FbTraceId" TEXT NULL,
    "UltimoErrorCodigo" TEXT NULL,
    "CreadoEn" TEXT NOT NULL,
    "ActualizadoEn" TEXT NOT NULL,
    "ProximoIntentoEn" TEXT NULL
);

CREATE INDEX IF NOT EXISTS "AsistenciaOperario_idOperario" ON "AsistenciaOperario"("idOperario");
CREATE INDEX IF NOT EXISTS "GastosCaja_idCaja" ON "GastosCaja"("idCaja");
CREATE UNIQUE INDEX IF NOT EXISTS "IX_ComprobantesRetiro_Codigo" ON "ComprobantesRetiro"("Codigo");
CREATE UNIQUE INDEX IF NOT EXISTS "IX_EmailNotifications_IdTurno" ON "EmailNotifications"("IdTurno");
CREATE UNIQUE INDEX IF NOT EXISTS "IX_Facturas_IdTurno" ON "Facturas"("IdTurno");
CREATE UNIQUE INDEX IF NOT EXISTS "IX_Facturas_Prefijo_Numero" ON "Facturas"("Prefijo", "Numero");
CREATE INDEX IF NOT EXISTS "IX_ImpresionesComprobante_IdTurno_CreadoEn" ON "ImpresionesComprobante"("IdTurno", "CreadoEn");
CREATE UNIQUE INDEX IF NOT EXISTS "IX_WhatsAppNotifications_IdTurno" ON "WhatsAppNotifications"("IdTurno");
CREATE INDEX IF NOT EXISTS "JornadaOperario_idCaja" ON "JornadaOperario"("idCaja");
CREATE INDEX IF NOT EXISTS "JornadaOperario_idOperario" ON "JornadaOperario"("idOperario");
CREATE INDEX IF NOT EXISTS "LiquidacionOperario_idOperario" ON "LiquidacionOperario"("idOperario");
CREATE INDEX IF NOT EXISTS "PerfilModulo_ModuloId" ON "PerfilModulo"("ModuloId");
CREATE INDEX IF NOT EXISTS "PerfilModulo_PerfilId" ON "PerfilModulo"("PerfilId");
CREATE INDEX IF NOT EXISTS "PrecioServicioVehiculo_IdTipoVehiculo" ON "PrecioServicioVehiculo"("IdTipoVehiculo");
CREATE INDEX IF NOT EXISTS "PrecioServicioVehiculo_idServicio" ON "PrecioServicioVehiculo"("idServicio");
CREATE INDEX IF NOT EXISTS "RegistroPropinasOperarios_idCaja" ON "RegistroPropinasOperarios"("idCaja");
CREATE INDEX IF NOT EXISTS "RegistroPropinasOperarios_idOperario" ON "RegistroPropinasOperarios"("idOperario");
CREATE INDEX IF NOT EXISTS "TurnoServicios_IdTurno" ON "TurnoServicios"("IdTurno");
CREATE INDEX IF NOT EXISTS "TurnosMovimientos_IdFormaPago" ON "TurnosMovimientos"("IdFormaPago");
CREATE INDEX IF NOT EXISTS "TurnosMovimientos_IdTurno" ON "TurnosMovimientos"("IdTurno");
CREATE INDEX IF NOT EXISTS "Turnos_IdTipoVehiculo" ON "Turnos"("IdTipoVehiculo");
CREATE INDEX IF NOT EXISTS "Turnos_idCajaDiaria" ON "Turnos"("idCajaDiaria");
CREATE INDEX IF NOT EXISTS "Turnos_idClienteCredito" ON "Turnos"("idClienteCredito");
CREATE INDEX IF NOT EXISTS "Turnos_idOperario" ON "Turnos"("idOperario");
CREATE INDEX IF NOT EXISTS "Usuarios_PerfilId" ON "Usuarios"("PerfilId");
CREATE INDEX IF NOT EXISTS "ValesOperarios_idCaja" ON "ValesOperarios"("idCaja");
CREATE INDEX IF NOT EXISTS "ValesOperarios_idOperario" ON "ValesOperarios"("idOperario");

COMMIT;