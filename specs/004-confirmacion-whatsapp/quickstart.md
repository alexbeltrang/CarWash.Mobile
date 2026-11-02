# Validación rápida del correo

## Prerrequisitos

1. API CarWash compilable y SQLite de pruebas.
2. Valores ficticios para plantilla y nombre del autolavado.
3. Credenciales SMTP ausentes en pruebas automatizadas.

## Pruebas automatizadas

En `CarWashApi`, ejecutar el proyecto de pruebas existente con Graph API
reemplazado por un mock:

```powershell
dotnet test
```

Comprobar creación única, payload con placa/servicios/total, conservación del
ingreso ante timeout, reintentos acotados, errores permanentes, idempotencia y
actualización por webhook. No registrar teléfonos completos, cuerpos ni tokens.

En la app móvil:

```powershell
.\gradlew.bat :app:test
```

Comprobar que la UI exige correo, conserva el éxito del ingreso y muestra por
separado `NOT_APPLICABLE`, `PENDING`, `ACCEPTED`, `DELIVERED` y `FAILED`.

## Prueba viva autorizada

Solo después de configurar un buzón de prueba y credenciales SMTP seguras.
Verificar el correo con un ingreso controlado y no usar datos de clientes sin
autorización.
