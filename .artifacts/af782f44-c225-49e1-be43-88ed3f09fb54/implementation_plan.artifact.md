# Plan de Reingeniería del Módulo de Asistencia (Estilo iVMS-4200)

Este plan detalla la transformación del módulo de asistencia para permitir la visualización de todos los eventos, filtrado avanzado profesional, paginación y exportación de datos en bruto.

## Análisis del Problema

1.  **Visibilidad Limitada**: Actualmente el sistema limita la vista a 200 registros y aplica una lógica de consolidación que oculta eventos intermedios necesarios para pruebas.
2.  **Filtrado Básico**: Se requiere un panel de búsqueda que replique la funcionalidad del iVMS-4200 (Rango de tiempo, Departamento, Nombre, ID de Persona, Origen).
3.  **Gestión de Datos**: El usuario necesita un "borrón y cuenta nueva" (Clear Database) para reiniciar pruebas de sincronización.
4.  **Reportes**: Se requiere exportar la información *filtrada* tanto en PDF como en CSV.

## Proposed Changes

### [Server Component]

#### [MODIFY] [AttendanceUseCase.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/service/AttendanceUseCase.kt)
*   Añadir `deleteAllAttendance()`: Comando para vaciar la tabla `attendance_logs`.
*   Añadir `getFilteredLogs(...)`: Método flexible que acepte parámetros de búsqueda y devuelva una lista paginada de eventos.

#### [MODIFY] [AttendanceRoutes.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/routes/AttendanceRoutes.kt)
*   **DELETE `/api/v1/asistencia/all`**: Nuevo endpoint para limpieza total.
*   **GET `/api/v1/asistencia/logs`**: Actualizar para recibir `start`, `end`, `dept`, `name`, `pid`, `source`, `limit`, `offset`.
*   **GET `/api/v1/asistencia/export/raw/csv`**: Exportación de datos filtrados en bruto.
*   **GET `/api/v1/asistencia/export/raw/pdf`**: Generación de reporte PDF con el mismo estilo profesional.

### [Web Component]

#### [MODIFY] [AttendanceModule.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/web/src/jsMain/kotlin/AttendanceModule.kt)
*   **Panel de Búsqueda**: Crear una interfaz oscura/profesional arriba de la tabla con los campos:
    *   Start Time / End Time (Inputs de texto con formato).
    *   Department (Dropdown dinámico).
    *   Name e ID (Inputs de búsqueda).
    *   Data Source (HIKVISIONWEB, CSV, App Movil).
*   **Tabla de Datos**:
    *   Mostrar todos los eventos (sin consolidación de Check-in/out por ahora, para pruebas).
    *   Implementar **Paginación** (Botones de Página Anterior/Siguiente y Número de Página).
*   **Acciones**:
    *   Botón "Search" (Lupa).
    *   Botón "Reset" (Limpiar filtros).
    *   Botón "Clear Database" (Rojo, con confirmación).
    *   Botones de descarga (PDF/CSV) vinculados a los filtros actuales.

## Verification Plan

### Automated Tests
*   Verificar que el servidor compila y los endpoints responden con JSON vacío tras un `DELETE /all`.

### Manual Verification
1.  **Limpieza**: Presionar "Clear Database", confirmar, y verificar que la tabla queda vacía.
2.  **Sincronización**: Correr el script de Python y verificar que aparecen cientos de registros.
3.  **Filtrado**: Probar el filtro de "Person ID" y "Rango de fechas" para asegurar que la tabla se actualiza.
4.  **Paginación**: Navegar entre páginas para ver registros antiguos.
5.  **Exportación**: Descargar el PDF y verificar que contiene solo los datos filtrados en la pantalla.
