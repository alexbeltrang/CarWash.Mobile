; Instalador de CarWash API. Compilar con installer\build.ps1.
#define AppName "CarWash API"
#define AppVersion "1.0.0"

[Setup]
AppId={{6B0F2C1E-7A53-4C57-9E5B-3D1C6C0A4F21}
AppName={#AppName}
AppVersion={#AppVersion}
DefaultDirName={autopf}\CarWashApi
DisableProgramGroupPage=yes
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=output
OutputBaseFilename=CarWashApi-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayName={#AppName}

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "publicfw"; Description: "Permitir tambien en redes de perfil Publico (no recomendado)"; Flags: unchecked

[Dirs]
Name: "{commonappdata}\SplashCar"; Permissions: users-modify

[Files]
Source: "staging\api\*"; DestDir: "{app}"; Excludes: "*.pdb"; Flags: recursesubdirs ignoreversion
Source: "run-api.ps1"; DestDir: "{app}"; Flags: ignoreversion
Source: "install-service.ps1"; DestDir: "{app}"; Flags: ignoreversion
Source: "uninstall-service.ps1"; DestDir: "{app}"; Flags: ignoreversion
; Base vacia (solo esquema); no se reemplaza si ya existe una base con datos.
Source: "CarWash.schema.db"; DestDir: "{commonappdata}\SplashCar"; DestName: "CarWash.db"; Flags: onlyifdoesntexist uninsneveruninstall

[Run]
Filename: "notepad.exe"; Parameters: """{app}\direcciones.txt"""; Description: "Ver las direcciones para la app movil"; Flags: postinstall nowait skipifsilent

[UninstallRun]
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\uninstall-service.ps1"""; Flags: runhidden waituntilterminated; RunOnceId: "RemoveCarWashApi"

[UninstallDelete]
Type: files; Name: "{app}\install.log"
Type: files; Name: "{app}\direcciones.txt"

[Code]
const
  DefaultUser = 'admin';
  DefaultPassword = 'Admin2026*';

var
  UserPage: TInputQueryWizardPage;
  EmailPage: TInputQueryWizardPage;

function PublicFlag(Param: String): String;
begin
  if WizardIsTaskSelected('publicfw') then Result := ' -PublicFirewall' else Result := '';
end;

procedure InitializeWizard;
begin
  // Valores iniciales editables; en instalacion silenciosa: /USUARIO=nombre /CLAVE=clave
  UserPage := CreateInputQueryPage(wpSelectTasks, 'Usuario inicial',
    'Usuario con el que se ingresa a la aplicacion CarWash',
    'Solo se crea si la base de datos no tiene usuarios. Cambie la clave por defecto ' +
    'si el equipo es accesible a otras personas. Use de 4 a 64 caracteres ASCII.');
  UserPage.Add('Usuario:', False);
  UserPage.Add('Contrasena:', True);
  UserPage.Add('Confirmar contrasena:', True);
  UserPage.Values[0] := ExpandConstant('{param:Usuario|' + DefaultUser + '}');
  UserPage.Values[1] := ExpandConstant('{param:Clave|' + DefaultPassword + '}');
  UserPage.Values[2] := UserPage.Values[1];

  EmailPage := CreateInputQueryPage(UserPage.ID, 'Correo de notificaciones (opcional)',
    'Cuenta que envia el correo al registrar un ingreso',
    'Deje los campos vacios para omitirlo o para conservar la configuracion existente. ' +
    'Para Gmail use una contrasena de aplicacion, no la contrasena normal. ' +
    'Se guarda solo en este equipo, en un archivo accesible unicamente por administradores.');
  EmailPage.Add('Cuenta de correo:', False);
  EmailPage.Add('Contrasena de aplicacion:', True);
end;

function ValidCredential(Value: String): Boolean;
var
  I: Integer;
begin
  Result := (Length(Value) >= 4) and (Length(Value) <= 64) and (Value = Trim(Value));
  for I := 1 to Length(Value) do
    if (Ord(Value[I]) < 32) or (Ord(Value[I]) > 126) then Result := False;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = UserPage.ID then
  begin
    if not ValidCredential(UserPage.Values[0]) or not ValidCredential(UserPage.Values[1]) then
    begin
      MsgBox('Usuario y contrasena deben tener de 4 a 64 caracteres ASCII, sin espacios al inicio ni al final.', mbError, MB_OK);
      Result := False;
    end
    else if UserPage.Values[1] <> UserPage.Values[2] then
    begin
      MsgBox('Las contrasenas no coinciden.', mbError, MB_OK);
      Result := False;
    end;
  end;
end;

function JsonEscape(Value: String): String;
begin
  Result := Value;
  StringChangeEx(Result, '\', '\\', True);
  StringChangeEx(Result, '"', '\"', True);
end;

procedure SaveEmailSettings;
var
  FilePath, Account, Json: String;
  ResultCode: Integer;
begin
  Account := Trim(EmailPage.Values[0]);
  if (Account = '') or (EmailPage.Values[1] = '') then Exit;
  FilePath := ExpandConstant('{commonappdata}\SplashCar\email.json');
  Json := '{"Username":"' + JsonEscape(Account) + '","Password":"' + JsonEscape(EmailPage.Values[1]) +
    '","FromAddress":"' + JsonEscape(Account) + '"}';
  SaveStringToFile(FilePath, Json, False);
  Exec(ExpandConstant('{sys}\icacls.exe'), '"' + FilePath + '" /inheritance:r /grant:r *S-1-5-18:F *S-1-5-32-544:F',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

procedure SeedInitialUser;
var
  CredentialsFile: String;
  ResultCode: Integer;
begin
  // El sembrador borra este archivo al terminar y no crea nada si ya existen usuarios.
  CredentialsFile := ExpandConstant('{tmp}\cw-seed.json');
  SaveStringToFile(CredentialsFile, '{"Usuario":"' + JsonEscape(UserPage.Values[0]) +
    '","Clave":"' + JsonEscape(UserPage.Values[1]) + '"}', False);
  if not Exec(ExpandConstant('{app}\CarWashSeeder.exe'),
      '"' + ExpandConstant('{commonappdata}\SplashCar\CarWash.db') + '" "' + CredentialsFile + '"',
      '', SW_HIDE, ewWaitUntilTerminated, ResultCode) or (ResultCode <> 0) then
  begin
    DeleteFile(CredentialsFile);
    SuppressibleMsgBox('No se pudo crear el usuario inicial (codigo ' + IntToStr(ResultCode) +
      '). La API se instalara de todos modos; cree el usuario manualmente.', mbError, MB_OK, IDOK);
  end;
end;

procedure StartService;
var
  ResultCode: Integer;
begin
  WizardForm.StatusLabel.Caption := 'Configurando e iniciando la API...';
  Exec('powershell.exe', '-NoProfile -ExecutionPolicy Bypass -File "' + ExpandConstant('{app}\install-service.ps1') +
    '" -InstallDir "' + ExpandConstant('{app}') + '" -Port 5197' + PublicFlag(''),
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ResultCode: Integer;
begin
  // Detiene una instalacion previa para poder reemplazar los archivos.
  Exec(ExpandConstant('{sys}\schtasks.exe'), '/End /TN CarWashApi', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Result := '';
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then SaveEmailSettings;
  if CurStep = ssPostInstall then
  begin
    SeedInitialUser;
    StartService;
  end;
end;
