:ON ERROR EXIT
USE [master];
GO
IF DB_ID(N'$(DatabaseName)') IS NOT NULL
    THROW 50001, N'La base ya existe. Este script solo crea bases nuevas; no migra ni reemplaza datos.', 1;
CREATE DATABASE [$(DatabaseName)];
GO
USE [$(DatabaseName)];
GO
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    CREATE TABLE dbo.AsistenciaOperario (
        idAsistenciaOperario INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idOperario INT NULL,
        Fecha DATETIME2(3) NULL,
        Asistio INT NULL,
        Autorizado INT NULL,
        Observacion NVARCHAR(MAX) NULL
    );
    CREATE TABLE dbo.CajaDiaria (
        idCaja INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        FechaApertura DATETIME2(3) NULL,
        FechaCierre DATETIME2(3) NULL,
        MontoInicial FLOAT(53) NULL,
        TotalIngresosEfectivo FLOAT(53) NULL,
        TotalIngresosTransferencias FLOAT(53) NULL,
        TotalIngresosDatafono FLOAT(53) NULL,
        TotalIngresosCredito FLOAT(53) NULL,
        TotalEgresos FLOAT(53) NULL,
        TotalValesOperarios FLOAT(53) NULL,
        TotalFinal FLOAT(53) NULL,
        Estado INT NULL
    );
    CREATE TABLE dbo.ClienteCredito (
        idClienteCredito INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Nombre NVARCHAR(MAX) NULL,
        Telefono NVARCHAR(MAX) NULL,
        Direcion NVARCHAR(MAX) NULL,
        Correo NVARCHAR(MAX) NULL,
        ciudad NVARCHAR(MAX) NULL,
        Estado INT NULL
    );
    CREATE TABLE dbo.ComprobantesRetiro (
        IdTurno INT NOT NULL CONSTRAINT PK_ComprobantesRetiro PRIMARY KEY,
        Codigo NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
        CreadoEn DATETIMEOFFSET(7) NOT NULL,
        EntregadoEn DATETIMEOFFSET(7) NULL,
        EntregadoPor INT NULL
    );
    CREATE TABLE dbo.ConfiguracionFacturacion (
        Id INT NOT NULL CONSTRAINT PK_ConfiguracionFacturacion PRIMARY KEY,
        DatosJson NVARCHAR(MAX) NOT NULL,
        Revision INT NOT NULL,
        SiguienteNumero BIGINT NOT NULL,
        CONSTRAINT CK_Facturacion_Unica CHECK (Id = 1)
    );
    CREATE TABLE dbo.EmailNotifications (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_EmailNotifications PRIMARY KEY,
        IdTurno INT NOT NULL,
        Destinatario NVARCHAR(MAX) NOT NULL,
        Estado NVARCHAR(MAX) NOT NULL,
        Intentos INT NOT NULL,
        UltimoErrorCodigo NVARCHAR(MAX) NULL,
        CreadoEn DATETIMEOFFSET(7) NOT NULL,
        ActualizadoEn DATETIMEOFFSET(7) NOT NULL,
        ProximoIntentoEn DATETIMEOFFSET(7) NULL
    );
    CREATE TABLE dbo.Empresa (
        Id INT NOT NULL CONSTRAINT PK_Empresa PRIMARY KEY,
        DatosJson NVARCHAR(MAX) NOT NULL,
        Revision INT NOT NULL,
        CONSTRAINT CK_Empresa_Unica CHECK (Id = 1)
    );
    CREATE TABLE dbo.Facturas (
        Id UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_Facturas PRIMARY KEY,
        IdTurno INT NOT NULL,
        Prefijo NVARCHAR(20) COLLATE Latin1_General_100_BIN2 NOT NULL,
        Numero BIGINT NOT NULL,
        Fecha DATE NOT NULL,
        Placa NVARCHAR(MAX) NOT NULL,
        DatosJson NVARCHAR(MAX) NOT NULL
    );
    CREATE TABLE dbo.Festivos (
        IdFestivo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Fecha DATETIME2(3) NULL,
        Descripcion NVARCHAR(MAX) NULL
    );
    CREATE TABLE dbo.FormaPago (
        IdFormaPago INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Nombre NVARCHAR(MAX) NULL
    );
    CREATE TABLE dbo.GastosCaja (
        IdGasto INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idCaja INT NULL,
        FechaRegistro DATETIME2(3) NULL,
        Concepto NVARCHAR(MAX) NULL,
        Valor FLOAT(53) NULL,
        Observaciones NVARCHAR(MAX) NULL
    );
    CREATE TABLE dbo.ImpresionesComprobante (
        SolicitudId UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_ImpresionesComprobante PRIMARY KEY,
        IdTurno INT NOT NULL,
        AnchoMm INT NOT NULL,
        SolicitadoPor INT NOT NULL,
        Impresora NVARCHAR(MAX) NOT NULL,
        Estado NVARCHAR(MAX) NOT NULL,
        Texto NVARCHAR(MAX) NOT NULL,
        CreadoEn DATETIMEOFFSET(7) NOT NULL,
        ActualizadoEn DATETIMEOFFSET(7) NOT NULL,
        ErrorCodigo NVARCHAR(MAX) NULL
    );
    CREATE TABLE dbo.JornadaOperario (
        idJornada INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        HoraIngreso DATETIME2(3) NULL,
        HoraSalida DATETIME2(3) NULL,
        Estado INT NULL,
        idCaja INT NULL,
        idOperario INT NULL
    );
    CREATE TABLE dbo.LiquidacionOperario (
        idLiquidacionOperario INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idOperario INT NULL,
        FechaInicial DATETIME2(3) NULL,
        FechaFinal DATETIME2(3) NULL,
        TotalServicios INT NULL,
        TotalFacturado FLOAT(53) NULL,
        Comision FLOAT(53) NULL,
        Vales FLOAT(53) NULL,
        TotalPagado FLOAT(53) NULL,
        FechaLiquidacion DATETIME2(3) NULL
    );
    CREATE TABLE dbo.Modulos (
        ModuloId INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Descripcion NVARCHAR(150) NULL,
        IsDelete INT NULL,
        ModuloPadre INT NULL,
        OrdenPresentacion INT NULL,
        NombreMenuApp NVARCHAR(300) NULL
    );
    CREATE TABLE dbo.OperarioComisiones (
        IdOperarioComision INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idOperario INT NULL,
        Porcentaje FLOAT(53) NULL,
        DiaSemana INT NULL,
        FechaInicio DATETIME2(3) NULL,
        FechaFin DATETIME2(3) NULL
    );
    CREATE TABLE dbo.Operarios (
        idOperario INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Nombres NVARCHAR(80) NULL,
        Apellidos NVARCHAR(80) NULL,
        Email NVARCHAR(150) NULL,
        Celular NVARCHAR(20) NULL,
        isDelete INT NULL,
        UltimaAsignacion DATETIME2(3) NULL,
        CargaTrabajo INT NULL
    );
    CREATE TABLE dbo.PerfilModulo (
        PerfilModuloId INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ModuloId INT NULL,
        PerfilId INT NULL,
        isDeleted INT NULL
    );
    CREATE TABLE dbo.Perfiles (
        PerfilId INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        NombrePerfil NVARCHAR(150) NULL,
        isDelete INT NULL
    );
    CREATE TABLE dbo.PrecioServicioVehiculo (
        [IdServicioVehiculo ] INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idServicio INT NULL,
        IdTipoVehiculo INT NULL,
        Precio FLOAT(53) NULL,
        PrecioBaseComision FLOAT(53) NULL
    );
    CREATE TABLE dbo.RegistroPropinasOperarios (
        idPropina INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idOperario INT NULL,
        fechaRegistro DATETIME2(3) NULL,
        valorPropina FLOAT(53) NULL,
        observaciones NVARCHAR(MAX) NULL,
        idCaja INT NULL,
        isDelete INT NULL
    );
    CREATE TABLE dbo.Servicios (
        idServicio INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Nombre NVARCHAR(MAX) NULL,
        IsDelete INT NULL
    );
    CREATE TABLE dbo.TipoVehiculo (
        idTipoVehiculo INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Nombre NVARCHAR(50) NULL,
        IsDelete INT NULL
    );
    CREATE TABLE dbo.TurnoServicios (
        idTunoServicios INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idServicios INT NULL,
        IdTurno INT NULL,
        IsDeleted INT NULL
    );
    CREATE TABLE dbo.Turnos (
        IdTurno INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        NumeroTurno NVARCHAR(MAX) NULL,
        NombreCliente NVARCHAR(MAX) NULL,
        NumeroCelular NVARCHAR(MAX) NULL,
        CorreoElectronico NVARCHAR(MAX) NULL,
        Placa NVARCHAR(MAX) NULL,
        FechaHoraIngreso DATETIME2(3) NULL,
        FechaHoraFin DATETIME2(3) NULL,
        Marca NVARCHAR(MAX) NULL,
        NumeroOrden NVARCHAR(MAX) NULL,
        Valor FLOAT(53) NULL,
        ValorBaseComision FLOAT(53) NULL,
        PorcentajeComision FLOAT(53) NULL,
        ValorComision FLOAT(53) NULL,
        Pagado INT NULL,
        PagadoNomina INT NULL,
        Observaciones NVARCHAR(MAX) NULL,
        FechaHoraAsignacionOperario DATETIME2(3) NULL,
        Estado INT NULL,
        IdTipoVehiculo INT NULL,
        idOperario INT NULL,
        OperadorOcupado INT NULL,
        idCajaDiaria INT NULL,
        idClienteCredito INT NULL
    );
    CREATE TABLE dbo.TurnosDiarios (
        Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Fecha NVARCHAR(MAX) NULL,
        Consecutivo INT NULL
    );
    CREATE TABLE dbo.TurnosMovimientos (
        IdTurnoMovimientos INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        MontoPagado FLOAT(53) NULL,
        FechaMovimiento DATETIME2(3) NULL,
        IdTurno INT NULL,
        IdFormaPago INT NULL
    );
    CREATE TABLE dbo.Usuarios (
        idUser INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserName NVARCHAR(MAX) NULL,
        [password] NVARCHAR(MAX) NULL,
        Nombres NVARCHAR(80) NULL,
        Apellidos NVARCHAR(80) NULL,
        DisplayName NVARCHAR(150) NULL,
        Email NVARCHAR(500) NULL,
        isDelete INT NULL,
        PerfilId INT NULL
    );
    CREATE TABLE dbo.ValesOperarios (
        IdValeOperario INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        idOperario INT NULL,
        idCaja INT NULL,
        FechaRegsitro DATETIME2(3) NULL,
        Valor FLOAT(53) NULL,
        Motivo NVARCHAR(MAX) NULL,
        Descontado INT NULL,
        FechaDescuento DATETIME2(3) NULL
    );
    CREATE TABLE dbo.WhatsAppNotifications (
        Id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_WhatsAppNotifications PRIMARY KEY,
        IdTurno INT NOT NULL,
        DestinatarioNormalizado NVARCHAR(MAX) NOT NULL,
        Estado NVARCHAR(MAX) NOT NULL,
        Intentos INT NOT NULL,
        MessageIdExterno NVARCHAR(MAX) NULL,
        FbTraceId NVARCHAR(MAX) NULL,
        UltimoErrorCodigo NVARCHAR(MAX) NULL,
        CreadoEn DATETIMEOFFSET(7) NOT NULL,
        ActualizadoEn DATETIMEOFFSET(7) NOT NULL,
        ProximoIntentoEn DATETIMEOFFSET(7) NULL
    );

    CREATE INDEX AsistenciaOperario_idOperario ON dbo.AsistenciaOperario(idOperario);
    CREATE INDEX GastosCaja_idCaja ON dbo.GastosCaja(idCaja);
    CREATE UNIQUE INDEX IX_ComprobantesRetiro_Codigo ON dbo.ComprobantesRetiro(Codigo);
    CREATE UNIQUE INDEX IX_EmailNotifications_IdTurno ON dbo.EmailNotifications(IdTurno);
    CREATE UNIQUE INDEX IX_Facturas_IdTurno ON dbo.Facturas(IdTurno);
    CREATE UNIQUE INDEX IX_Facturas_Prefijo_Numero ON dbo.Facturas(Prefijo, Numero);
    CREATE INDEX IX_ImpresionesComprobante_IdTurno_CreadoEn ON dbo.ImpresionesComprobante(IdTurno, CreadoEn);
    CREATE UNIQUE INDEX IX_WhatsAppNotifications_IdTurno ON dbo.WhatsAppNotifications(IdTurno);
    CREATE INDEX JornadaOperario_idCaja ON dbo.JornadaOperario(idCaja);
    CREATE INDEX JornadaOperario_idOperario ON dbo.JornadaOperario(idOperario);
    CREATE INDEX LiquidacionOperario_idOperario ON dbo.LiquidacionOperario(idOperario);
    CREATE INDEX PerfilModulo_ModuloId ON dbo.PerfilModulo(ModuloId);
    CREATE INDEX PerfilModulo_PerfilId ON dbo.PerfilModulo(PerfilId);
    CREATE INDEX PrecioServicioVehiculo_IdTipoVehiculo ON dbo.PrecioServicioVehiculo(IdTipoVehiculo);
    CREATE INDEX PrecioServicioVehiculo_idServicio ON dbo.PrecioServicioVehiculo(idServicio);
    CREATE INDEX RegistroPropinasOperarios_idCaja ON dbo.RegistroPropinasOperarios(idCaja);
    CREATE INDEX RegistroPropinasOperarios_idOperario ON dbo.RegistroPropinasOperarios(idOperario);
    CREATE INDEX TurnoServicios_IdTurno ON dbo.TurnoServicios(IdTurno);
    CREATE INDEX TurnosMovimientos_IdFormaPago ON dbo.TurnosMovimientos(IdFormaPago);
    CREATE INDEX TurnosMovimientos_IdTurno ON dbo.TurnosMovimientos(IdTurno);
    CREATE INDEX Turnos_IdTipoVehiculo ON dbo.Turnos(IdTipoVehiculo);
    CREATE INDEX Turnos_idCajaDiaria ON dbo.Turnos(idCajaDiaria);
    CREATE INDEX Turnos_idClienteCredito ON dbo.Turnos(idClienteCredito);
    CREATE INDEX Turnos_idOperario ON dbo.Turnos(idOperario);
    CREATE INDEX Usuarios_PerfilId ON dbo.Usuarios(PerfilId);
    CREATE INDEX ValesOperarios_idCaja ON dbo.ValesOperarios(idCaja);
    CREATE INDEX ValesOperarios_idOperario ON dbo.ValesOperarios(idOperario);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO