# Tareas: Implementación de Sincronización On-Demand (Opción A)

- `[x]` Backend (Servidor Ktor):
    - `[x]` Crear `SystemTaskTable` en `SapModulesTables.kt`.
    - `[x]` Registrar la tabla en `DatabaseFactory.kt`.
    - `[x]` Implementar endpoints de tareas (`request-sync`, `poll-task`, `update-task`) en `AttendanceRoutes.kt`.
- `[x]` Script (Python):
    - `[x]` Añadir lógica de polling al servidor en `attendance_sync.py`.
    - `[x]` Implementar ejecución automática de fetch ante órdenes externas.
- `[x]` Frontend (Web Compose):
    - `[x]` Vincular botón "Get Events" a la API de tareas.
    - `[x]` Añadir feedback visual de sincronización en progreso.
- `[ ]` Verificación y Push:
    - `[ ]` Probar flujo completo (Web -> Server -> Python -> Lectora -> Server -> Web).
    - `[ ]` Realizar Commit and Push.
