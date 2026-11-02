using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Data.Sqlite;

// Uso: CarWashSeeder <ruta-db> <archivo-credenciales.json>
// El JSON ({"Usuario":"..","Clave":".."}) se borra al terminar para no dejar la clave en disco.
// Crea el perfil Administrador y el usuario inicial solo si la base no tiene usuarios activos.
if (args.Length != 2)
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
