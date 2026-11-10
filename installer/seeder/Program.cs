using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Data.Sqlite;

if (args.Length == 2 && args[0] == "--backup")
{
    return DatabaseBackup.Run(args[1]);
}

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

static class DatabaseBackup
{
    public static int Run(string configurationPath)
    {
        string? pending = null;
        string? directory = null;
        DateTime? lastSuccess = null;
        FileStream? exclusive = null;
        try
        {
            var settings = JsonSerializer.Deserialize<BackupSettings>(File.ReadAllText(configurationPath))
                ?? throw new InvalidOperationException();
            directory = Path.GetFullPath(settings.BackupDirectory);
            var database = Path.GetFullPath(settings.DatabasePath);
            var installation = Path.GetFullPath(settings.InstallDir);
            if (!Path.IsPathFullyQualified(settings.BackupDirectory) || !Path.IsPathFullyQualified(settings.DatabasePath) ||
                directory.TrimEnd('\\') == Path.GetPathRoot(directory)?.TrimEnd('\\') ||
                IsWithin(directory, installation) || IsWithin(directory, Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), "SplashCar")) ||
                directory.Contains("\\AppData\\Local\\CarWash", StringComparison.OrdinalIgnoreCase) ||
                directory.Contains("\\AppData\\Roaming\\CarWash", StringComparison.OrdinalIgnoreCase) || IsWithin(database, directory))
                throw new InvalidOperationException();
            CheckPath(directory);
            CheckPath(database);
            Directory.CreateDirectory(directory);
            exclusive = new FileStream(Path.Combine(directory, "CarWash-backup.lock"), FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None);
            var statusPath = Path.Combine(directory, "backup-status.json");
            if (File.Exists(statusPath))
            {
                try { lastSuccess = JsonSerializer.Deserialize<BackupStatus>(File.ReadAllText(statusPath))?.LastSuccessUtc; }
                catch (JsonException) { }
            }
            var timestamp = DateTime.UtcNow;
            var name = "CarWash-" + timestamp.ToString("yyyyMMdd-HHmmss-fffffff") + "-" + Guid.NewGuid().ToString("N");
            pending = Path.Combine(directory, ".pending-" + name);
            Directory.CreateDirectory(pending);
            var snapshot = Path.Combine(pending, "CarWash.db");
            using (var source = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = database, Mode = SqliteOpenMode.ReadOnly, Pooling = false, DefaultTimeout = 30 }.ToString()))
            using (var destination = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = snapshot, Mode = SqliteOpenMode.ReadWriteCreate, Pooling = false }.ToString()))
            {
                source.Open();
                destination.Open();
                source.BackupDatabase(destination);
                using var integrity = destination.CreateCommand();
                integrity.CommandText = "PRAGMA integrity_check";
                if (!string.Equals(Convert.ToString(integrity.ExecuteScalar()), "ok", StringComparison.Ordinal))
                    throw new InvalidOperationException();
            }
            var files = new Dictionary<string, string> { ["CarWash.db"] = Hash(snapshot) };
            foreach (var sourceFile in settings.ConfigFiles)
            {
                CheckPath(sourceFile);
                if (!File.Exists(sourceFile)) continue;
                var target = "config-" + files.Count + "-" + Path.GetFileName(sourceFile);
                File.Copy(sourceFile, Path.Combine(pending, target));
                files[target] = Hash(Path.Combine(pending, target));
            }
            File.WriteAllText(Path.Combine(pending, "backup.json"), JsonSerializer.Serialize(new BackupManifest(installation, timestamp, files)));
            Directory.Move(pending, Path.Combine(directory, name));
            pending = null;
            Prune(directory, installation, timestamp);
            File.WriteAllText(statusPath, JsonSerializer.Serialize(new BackupStatus(true, timestamp, timestamp)));
            Console.WriteLine("Respaldo verificado: " + name);
            return 0;
        }
        catch
        {
            if (exclusive != null && directory != null && Directory.Exists(directory))
            {
                try { File.WriteAllText(Path.Combine(directory, "backup-status.json"), JsonSerializer.Serialize(new BackupStatus(false, DateTime.UtcNow, lastSuccess))); }
                catch { }
            }
            Console.Error.WriteLine("No se pudo completar el respaldo. Revise permisos, espacio disponible y estado de la base.");
            return 1;
        }
        finally
        {
            if (pending != null && Directory.Exists(pending))
            {
                try { CheckTree(pending); Directory.Delete(pending, true); }
                catch { }
            }
            exclusive?.Dispose();
        }
    }

    static void Prune(string directory, string installation, DateTime timestamp)
    {
        var backups = new List<(string Path, BackupManifest Manifest)>();
        foreach (var path in Directory.EnumerateDirectories(directory, "CarWash-*"))
        {
            CheckTree(path);
            var marker = Path.Combine(path, "backup.json");
            if (!File.Exists(marker)) continue;
            var manifest = JsonSerializer.Deserialize<BackupManifest>(File.ReadAllText(marker));
            if (manifest != null && string.Equals(manifest.InstallDir, installation, StringComparison.OrdinalIgnoreCase))
                backups.Add((path, manifest));
        }
        var today = timestamp.ToLocalTime().Date;
        var oldestDay = today.AddDays(-30);
        var ordered = backups.OrderByDescending(item => item.Manifest.CreatedUtc)
            .Select(item => (item.Path, Day: item.Manifest.CreatedUtc.ToLocalTime().Date)).ToArray();
        var keep = ordered.Where(item => item.Day == today)
            .Concat(ordered.Where(item => item.Day >= oldestDay && item.Day < today)
                .GroupBy(item => item.Day).Select(group => group.First()))
            .Select(item => item.Path).ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var item in ordered.Where(item => !keep.Contains(item.Path))) Directory.Delete(item.Path, true);
    }

    static bool IsWithin(string path, string parent) => string.Equals(path.TrimEnd(Path.DirectorySeparatorChar), parent.TrimEnd(Path.DirectorySeparatorChar), StringComparison.OrdinalIgnoreCase) ||
        path.StartsWith(parent.TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase);

    static string Hash(string path)
    {
        using var stream = File.OpenRead(path);
        return Convert.ToHexString(SHA256.HashData(stream));
    }

    static void CheckPath(string path)
    {
        for (var current = Path.GetFullPath(path); current != null; current = Path.GetDirectoryName(current))
            if ((File.Exists(current) || Directory.Exists(current)) && (File.GetAttributes(current) & FileAttributes.ReparsePoint) != 0)
                throw new InvalidOperationException();
    }

    static void CheckTree(string path)
    {
        CheckPath(path);
        foreach (var child in Directory.EnumerateFileSystemEntries(path))
        {
            CheckPath(child);
            if (Directory.Exists(child)) CheckTree(child);
        }
    }

    record BackupSettings(string InstallDir, string DatabasePath, string BackupDirectory, string[] ConfigFiles);
    record BackupManifest(string InstallDir, DateTime CreatedUtc, Dictionary<string, string> Files);
    record BackupStatus(bool Success, DateTime AttemptUtc, DateTime? LastSuccessUtc);
}
