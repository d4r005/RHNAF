# Plan de Corrección y Migración: Nueva Supabase (`vrqxemvsizitimvvqttd`)

El servidor está fallando (error 503) debido a que el proyecto de Supabase anterior (`tudxbophpebusvnkyzcz`) no es accesible. Vamos a migrar la base de datos a la nueva instancia proporcionada.

## Análisis de la Migración

1.  **Nueva Instancia**: `https://vrqxemvsizitimvvqttd.supabase.co`.
2.  **Identificador de Proyecto**: `vrqxemvsizitimvvqttd`.
3.  **Información Faltante**: Para la conexión JDBC del servidor Ktor, se necesita la **Contraseña de la Base de Datos** (la que definiste al crear el proyecto en Supabase). La `anon key` proporcionada es para el cliente web/móvil y no sirve para la conexión directa de base de datos que usa el servidor.
4.  **Robustez del Parsing**: La función `parsePostgresUrl` actual falla si la contraseña tiene el carácter `@`. Corregiremos esto usando `lastIndexOf`.

## User Review Required

> [!IMPORTANT]
> Para completar la migración, necesito que configures la nueva `DATABASE_URL` en los **Settings > Secrets** de tu Hugging Face Space. El formato debe ser:
> `postgresql://postgres.[PROYECTO]:[PASSWORD]@aws-1-us-west-2.pooler.supabase.com:6543/postgres?pgbouncer=true`
> (Sustituyendo `[PROYECTO]` por `vrqxemvsizitimvvqttd` y `[PASSWORD]` por tu contraseña real).

## Open Questions

*   **¿Tienes la contraseña de la base de datos?** Sin ella, el servidor no podrá conectar aunque actualicemos el código.
*   **¿Deseas que también configuremos el cliente Android/Web para usar la Supabase directamente?** Por ahora, solo se usa como almacenamiento del servidor.

## Proposed Changes

### [Server Component]

#### [MODIFY] [DatabaseFactory.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/database/DatabaseFactory.kt)
*   Actualizar la lógica de parsing de URL.
*   Mejorar la configuración de HikariCP para entornos cloud.

## Verification Plan

### Manual Verification
1.  Pedir al usuario que verifique el estado del proyecto en Supabase.
2.  Desplegar los cambios a GitHub (que dispararán el sync a HF).
3.  Revisar los logs en tiempo real de Hugging Face para confirmar que el pool de conexiones se inicializa correctamente.
