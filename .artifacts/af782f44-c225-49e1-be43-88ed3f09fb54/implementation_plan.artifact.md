# Plan de Corrección: Error de Conexión a Base de Datos (503 en Producción)

El error **503** y los logs del servidor indican que la aplicación no puede iniciar porque falla la conexión con la base de datos **Supabase**. El error específico `FATAL: (ENOTFOUND) tenant/user postgres.tudxbophpebusvnkyzcz not found` sugiere un problema con las credenciales o el estado del proyecto en Supabase.

## Análisis del Problema

1.  **Proyecto Pausado**: Supabase pausa los proyectos gratuitos tras periodos de inactividad. Los logs del usuario son de Julio y estamos en Agosto; es muy probable que el proyecto `tudxbophpebusvnkyzcz` esté pausado.
2.  **Robustez del Parsing**: La función `parsePostgresUrl` en `DatabaseFactory.kt` usa `split("@")`, lo cual fallará si la contraseña de la base de datos contiene el carácter `@`.
3.  **Configuración de Puerto**: Se está usando el puerto `5432` con el hostname del pooler. Aunque es válido para el "Session Pooler", en entornos de contenedores como Hugging Face se recomienda el "Transaction Pooler" en el puerto `6543` para evitar agotar las conexiones.

## Propuesta de Cambios

### 1. Mejorar `DatabaseFactory.kt`
*   Refactorizar `parsePostgresUrl` para que sea resistente a caracteres especiales en la contraseña (buscando el `@` desde el final).
*   Agregar logs de depuración seguros (que no muestren la contraseña completa) para identificar qué valores se están procesando.
*   Aumentar la resiliencia del pool de conexiones.

### 2. Verificación de Infraestructura (Usuario)
*   **IMPORTANTE**: El usuario debe entrar a su panel de [Supabase](https://supabase.com/dashboard) y verificar que el proyecto no esté en estado "Paused". Si lo está, debe reanudarlo ("Restore").
*   **IMPORTANTE**: Verificar que la variable `DATABASE_URL` en los "Secrets" de Hugging Face Space sea la correcta y use el formato: `postgresql://postgres.[ID]:[PASS]@aws-1-us-west-2.pooler.supabase.com:6543/postgres?pgbouncer=true`.

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
