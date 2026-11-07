# Creacion De Base De Datos

Scripts de estructura vacia, revisados contra el esquema actual de SplashCar
el 2026-11-06: 30 tablas y 27 indices, incluidos perfiles/modulos, cobros,
notificaciones, comprobantes, impresion, empresa y facturacion en papel.
No incluyen usuarios, claves, catalogos, clientes ni datos fiscales.
No son scripts de migracion o reparacion. Respaldar antes de trabajar con bases
existentes; no ejecutar pruebas sobre la base operativa.

## SQLite

[carwash-sqlite.sql](carwash-sqlite.sql) se ejecuta con SQLite 3. Desde la raiz
del repositorio, este comando crea un archivo nuevo:

```powershell
sqlite3 -bail "CarWash.nueva.db" ".read docs/database/carwash-sqlite.sql"
```

Tambien puede abrir una base nueva en DB Browser for SQLite, ejecutar el archivo
SQL en la pestaña Execute SQL y guardar los cambios.
SQLite crea el archivo; no utiliza CREATE DATABASE ni requiere servidor.
El script es transaccional y permite repetirlo con IF NOT EXISTS. No borra filas,
pero tampoco corrige columnas o restricciones de una base preexistente.

Conserva tipos/nombres del legado, incluidas fechas declaradas BIGINT (SQLite
acepta tambien los textos de fecha que escribe la API), FLOAT y la columna
`IdServicioVehiculo ` con espacio final. Conserva `Direcion`, `FechaRegsitro`
e `idTunoServicios` sin renombrar campos que utiliza el aplicativo.

## SQL Server

[carwash-sqlserver.sql](carwash-sqlserver.sql) requiere SQL Server 2016 o posterior,
sqlcmd y una cuenta con permiso para crear bases. Usa autenticacion integrada,
sin contrasenas en el script. Desde la raiz:

```powershell
sqlcmd -S "SERVIDOR\INSTANCIA" -E -b -v DatabaseName="CarWash" -i "docs/database/carwash-sqlserver.sql"
```

Reemplazar SERVIDOR\INSTANCIA por la instancia de destino; para una instancia
local predeterminada usar localhost. DatabaseName debe ser un nombre sencillo,
solo letras ASCII, numeros y guion bajo. No usar el nombre de una base existente.
Si existe, el script rechaza la ejecucion y se detiene con ON ERROR EXIT.
No elimina ni sustituye bases. Un fallo despues de CREATE DATABASE revierte
las tablas de la transaccion, pero puede dejar la base nueva vacia.

En SSMS, habilitar Query > SQLCMD Mode y anteponer esta linea al ejecutar:

```sql
:setvar DatabaseName "CarWash"
```

Para conexiones cifradas usar un certificado de servidor valido. Solo en una
prueba local con certificado autofirmado puede agregarse -C a sqlcmd.

SQL Server usa IDENTITY, UNIQUEIDENTIFIER, NVARCHAR, DATE, DATETIME2(3) y
DATETIMEOFFSET(7). Conserva FLOAT(53) para importes/porcentajes flotantes del
legado y flags INT, sin introducir redondeos o nuevas reglas de negocio.
Numero de factura y siguiente numero usan BIGINT. Los indices unicos de codigo
y prefijo conservan comparacion binaria para cadenas ASCII del contrato actual.
Ambos esquemas conservan las dos restricciones de registro unico Id=1.
No se agregan claves foraneas: el esquema legado no las declara.

**La API actual solo usa SQLite.** Este script crea el esquema SQL Server, pero
no habilita ese motor en la aplicacion ni migra registros. Para usarlo se debe
adaptar el proveedor EF, las consultas SQL nativas, el arranque y las conversiones
de fechas; no basta con cambiar la cadena de conexion.

## Verificacion Ejecutada

- SQLite: creacion y repeticion sin errores; 30 tablas vacias, 27 indices,
  integrity_check correcto y foreign_key_check sin resultados. Columnas, tipos,
  nulabilidad, claves primarias y columnas de indices comparados en solo lectura
  con el esquema existente.
- SQL Server 17.0.1145.4: base temporal unica, 30 tablas vacias, 27 indices,
  dos restricciones CHECK y DBCC CHECKDB correcto. Reejecucion rechazada;
  ON ERROR EXIT probado tambien sin -b. Base de prueba eliminada.
- No se modificaron bases operativas, configuraciones ni procesos de la API.
  No se ejecutaron cobros, facturas, mensajes ni impresiones reales.