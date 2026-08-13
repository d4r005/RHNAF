# Plan de Implementación: "Get Events from Device" (Opción A)

Este plan detalla cómo hacer que el botón "Get Events from Device" de la página web active realmente el script de Python en tu PC de la planta para jalar los datos de la lectora bajo demanda.

## Análisis Técnico (Modo Puente)

Como el servidor en la nube no puede llegar a la lectora local, usaremos un sistema de **"Tareas Pendientes"**:
1.  **Web**: Crea una tarea de sincronización en la base de datos.
2.  **Servidor**: Guarda la orden como "PENDIENTE".
3.  **Script Python**: Revisa el servidor periódicamente. Al ver una orden "PENDIENTE", la ejecuta (conecta a la lectora) y sube el resultado.
4.  **Web**: Muestra que la sincronización ha terminado.

## Proposed Changes

### [Server Component]

#### [MODIFY] [SapModulesTables.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/database/SapModulesTables.kt)
*   Añadir `SystemTaskTable` para gestionar órdenes remotas.
    *   Campos: `id`, `task_type`, `status` (PENDING, BUSY, DONE, ERROR), `params`, `result`, `updated_at`.

#### [MODIFY] [AttendanceRoutes.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/routes/AttendanceRoutes.kt)
*   **POST `/api/v1/asistencia/request-sync`**: Crea la orden.
*   **GET `/api/v1/asistencia/sync-status`**: Devuelve el estado de la última orden.
*   **GET `/api/v1/asistencia/poll-task`**: (Para Python) Obtiene la siguiente tarea pendiente.
*   **POST `/api/v1/asistencia/update-task`**: (Para Python) Actualiza el resultado.

### [Script Component]

#### [MODIFY] [attendance_sync.py](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/scripts/attendance_sync.py)
*   Añadir lógica de escucha: el script consultará al servidor cada 5-10 segundos buscando tareas "PENDING".
*   Al detectar una tarea, realizará el `fetch_events` de la lectora y reportará el éxito/error al servidor.

### [Web Component]

#### [MODIFY] [AttendanceModule.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/web/src/jsMain/kotlin/AttendanceModule.kt)
*   Vincular el botón "Get Events from Device" a la creación de la tarea.
*   Mostrar un indicador de carga ("Sincronizando con planta...") mientras la tarea no esté en estado `DONE` o `ERROR`.

## Verification Plan

### Manual Verification
1.  **Activación**: Presionar el botón en la web.
2.  **Respuesta**: Verificar en la consola de Python (en la planta) que el script detecta la orden y empieza a leer la lectora.
3.  **Resultado**: Confirmar que los nuevos registros aparecen en la tabla de la web y el botón vuelve a su estado normal.
