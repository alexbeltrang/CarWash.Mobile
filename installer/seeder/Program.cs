using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Data.Sqlite;

if (args.Length == 3 && args[0] == "--create-schema")
{
    var created = false;
    try
    {
        var sql = File.ReadAllText(args[2]);
        using (new FileStream(args[1], FileMode.CreateNew, FileAccess.Write)) { }
        created = true;
        using (var schema = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = args[1], Mode = SqliteOpenMode.ReadWrite, Pooling = false }.ToString()))
        {
            schema.Open();
            using var command = schema.CreateCommand();
            command.CommandText = sql;
            command.ExecuteNonQuery();
        }
        VerifyEmpty(args[1]);
        Console.WriteLine("Esquema creado sin registros.");
        return 0;
    }
    catch (Exception failure)
    {
        if (created) File.Delete(args[1]);
        Console.Error.WriteLine(failure.Message);
        return 1;
    }
}
if (args.Length == 2 && args[0] == "--verify-empty")
{
    try { VerifyEmpty(args[1]); Console.WriteLine("Base vacia verificada."); return 0; }
    catch (Exception failure) { Console.Error.WriteLine(failure.Message); return 1; }
}
if (args.Length == 2 && args[0] == "--verify-schema")
{
    try { VerifyCompatible(args[1]); Console.WriteLine("Base compatible verificada."); return 0; }
    catch (Exception failure) { Console.Error.WriteLine(failure.Message); return 1; }
}

// Uso: CarWashSeeder <ruta-db> <archivo-credenciales.json>
// El JSON ({"Usuario":"..","Clave":".."}) se borra al terminar para no dejar la clave en disco.
// Crea el perfil Administrador y el usuario inicial solo si la base no tiene usuarios activos.
if (args.Length != 2 || args[0].StartsWith("--", StringComparison.Ordinal))
{
    Console.Error.WriteLine("Uso: CarWashSeeder <ruta-db> <archivo-credenciales.json>");
    return 2;
}

var dbPath = args[0];
var credentialsPath = args[1];
try
{
    var credentials = JsonSerializer.Deserialize<Credentials>(File.ReadAllText(credentialsPath))
        ?? throw new InvalidOperationException("Credenciales vacias.");
    if (!IsValid(credentials.Usuario) || !IsValid(credentials.Clave))
        throw new InvalidOperationException("Usuario y clave deben ser ASCII imprimible, sin espacios en los extremos y de 4 a 64 caracteres.");

    using var connection = new SqliteConnection($"Data Source={dbPath};Pooling=False");
    connection.Open();

    var active = Scalar(connection, "SELECT COUNT(*) FROM Usuarios WHERE COALESCE(isDelete, 0) = 0");
    if (active > 0)
    {
        Console.WriteLine("La base ya tiene usuarios; no se crea ninguno.");
        return 0;
    }

    using var transaction = connection.BeginTransaction();
    var profileId = Scalar(connection, "SELECT PerfilId FROM Perfiles WHERE COALESCE(isDelete, 0) = 0 ORDER BY PerfilId LIMIT 1", transaction);
    if (profileId == 0)
    {
        Execute(connection, transaction, "INSERT INTO Perfiles (NombrePerfil, isDelete) VALUES ('Administrador', 0)");
        profileId = Scalar(connection, "SELECT last_insert_rowid()", transaction);
    }

    Execute(connection, transaction,
        "INSERT INTO Usuarios (UserName, password, Nombres, Apellidos, DisplayName, Email, isDelete, PerfilId) " +
        "VALUES ($user, $password, 'Administrador', '', 'Administrador', '', 0, $profile)",
        ("$user", Encrypt(credentials.Usuario)), ("$password", Encrypt(credentials.Clave)), ("$profile", profileId));
    transaction.Commit();
    Console.WriteLine("Usuario inicial creado.");
    return 0;
}
catch (Exception failure)
{
    Console.Error.WriteLine(failure.Message);
    return 1;
}
finally
{
    try { File.Delete(credentialsPath); } catch (IOException) { }
}

static void VerifyCompatible(string path)
{
    using var connection = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = path, Mode = SqliteOpenMode.ReadOnly, Pooling = false }.ToString());
    connection.Open();
    using var command = connection.CreateCommand();
    command.CommandText = "PRAGMA quick_check";
    if (Convert.ToString(command.ExecuteScalar()) != "ok") throw new InvalidOperationException("La base no supera quick_check.");
    if (Scalar(connection, "SELECT COUNT(*) FROM sqlite_schema WHERE type = 'table' AND name IN ('Usuarios','Perfiles','Turnos','TurnoServicios','Servicios','TipoVehiculo')") != 6)
        throw new InvalidOperationException("La base no contiene las tablas CarWash requeridas.");
}

static void VerifyEmpty(string path)
{
    VerifyCompatible(path);
    using var connection = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = path, Mode = SqliteOpenMode.ReadOnly, Pooling = false }.ToString());
    connection.Open();
    var tables = new List<string>();
    using (var command = connection.CreateCommand())
    {
        command.CommandText = "SELECT name FROM sqlite_schema WHERE type = 'table' AND name NOT LIKE 'sqlite_%'";
        using var reader = command.ExecuteReader();
        while (reader.Read()) tables.Add(reader.GetString(0));
    }
    if (tables.Count == 0) throw new InvalidOperationException("El esquema no contiene tablas.");
    foreach (var table in tables)
        if (Scalar(connection, "SELECT COUNT(*) FROM \"" + table.Replace("\"", "\"\"") + "\"") != 0)
            throw new InvalidOperationException("La base contiene registros; no se puede empaquetar.");
}

static bool IsValid(string? value) =>
    value is { Length: >= 4 and <= 64 } && value == value.Trim() && value.All(c => c >= ' ' && c < 127);

// Mismo algoritmo que FunctionsEncrip.Cifrado del WinForm (RC2 CBC, clave y vector fijos, Base64).
static string Encrypt(string text)
{
    using var rc2 = RC2.Create();
    rc2.Mode = CipherMode.CBC;
    rc2.Padding = PaddingMode.PKCS7;
    using var encryptor = rc2.CreateEncryptor(Encoding.ASCII.GetBytes("C4rW4sh"), Encoding.ASCII.GetBytes("20270430"));
    var data = Encoding.ASCII.GetBytes(text);
    return Convert.ToBase64String(encryptor.TransformFinalBlock(data, 0, data.Length));
}

static long Scalar(SqliteConnection connection, string sql, SqliteTransaction? transaction = null)
{
    using var command = connection.CreateCommand();
    command.Transaction = transaction;
    command.CommandText = sql;
    return Convert.ToInt64(command.ExecuteScalar() ?? 0L);
}

static void Execute(SqliteConnection connection, SqliteTransaction transaction, string sql, params (string Name, object Value)[] parameters)
{
    using var command = connection.CreateCommand();
    command.Transaction = transaction;
    command.CommandText = sql;
    foreach (var (name, value) in parameters) command.Parameters.AddWithValue(name, value);
    command.ExecuteNonQuery();
}

record Credentials(string Usuario, string Clave);
