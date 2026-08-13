# Walkthrough: Refinamiento de Asistencia (HIKVISIONWEB)

He implementado un filtrado estricto para eliminar los eventos "None" y he unificado la identificación de la lectora.

## Cambios Realizados

### 1. Filtrado de Eventos Inválidos
*   **Servidor**: El endpoint `/api/v1/asistencia/hikvision` ahora descarta automáticamente cualquier petición que contenga un ID de empleado como `"None"`, `"null"`, `"0"` o vacío.
*   **Lógica de Negocio**: `AttendanceUseCase` también incluye esta validación como segunda capa de seguridad.
*   **Script de Sincronización**: `attendance_sync.py` ahora filtra estos eventos localmente antes de intentar subirlos a la nube.

### 2. Renombrado a HIKVISIONWEB
*   Se cambió el identificador por defecto de `"HIK-WEB"` y `"LOCAL-SYNC"` a **`HIKVISIONWEB`**.
*   Ahora todos los registros provenientes de la terminal aparecerán con este nombre en la columna "Dispositivo" del portal.

### 3. Endpoint de Limpieza
*   Se creó un nuevo endpoint técnico: `POST /api/v1/asistencia/cleanup-invalid`.
*   Este comando borra retroactivamente cualquier registro basura que se haya colado previamente con IDs inválidos.

## Verificación Final
*   [x] Código compilado exitosamente.
*   [x] Cambios subidos a GitHub (Commit `de3084f`).
*   [ ] **Acción Requerida**: Una vez que el Space de Hugging Face esté "Running", los nuevos registros de "None" dejarán de aparecer.

> [!TIP]
> Los eventos que viste como "None" en tu captura eran ruidos de la terminal (major=3, minor=112, etc.). Con este cambio, el portal solo mostrará checadas reales de personas.
