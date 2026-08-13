# Tareas: Reingeniería del Módulo de Asistencia (Estilo iVMS)

- `[/]` Backend (Servidor Ktor):
    - `[ ]` Añadir endpoint `DELETE /api/v1/asistencia/all` para limpieza total.
    - `[ ]` Actualizar `GET /api/v1/asistencia/logs` con soporte para filtros dinámicos (fecha, depto, id, nombre).
    - `[ ]` Implementar endpoints de exportación `GET /export/raw/csv` y `GET /export/raw/pdf`.
- `[ ]` Frontend (Web Compose):
    - `[ ]` Crear el nuevo panel de búsqueda estilo iVMS-4200.
    - `[ ]` Implementar lógica de paginación en la tabla.
    - `[ ]` Añadir botones de descarga vinculados a los filtros.
    - `[ ]` Añadir botón de "Clear Database" con advertencia.
- `[ ]` Verificación y Push:
    - `[ ]` Probar limpieza y re-sincronización.
    - `[ ]` Realizar Commit and Push.
