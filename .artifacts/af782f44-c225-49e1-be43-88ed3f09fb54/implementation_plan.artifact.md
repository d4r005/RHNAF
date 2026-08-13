# Plan de Optimización UI y Visualización Completa (Asistencia)

Este plan detalla los ajustes para compactar el panel de búsqueda, fijar el rango de fechas desde el inicio del año y cambiar el orden de visualización para ver los datos desde el más antiguo.

## Análisis de los Requerimientos

1.  **Rango de Fechas**: El usuario necesita ver todo desde el **1 de enero de 2026** por defecto.
2.  **Orden de Datos**: Cambiar la visualización para mostrar primero los registros más antiguos (orden cronológico ascendente).
3.  **Espacio UI**: El panel de búsqueda actual es muy alto. Lo re-organizaremos para que sea más compacto y eficiente.

## Proposed Changes

### [Web Component]

#### [MODIFY] [AttendanceModule.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/web/src/jsMain/kotlin/AttendanceModule.kt)
*   **Fecha por Defecto**: Cambiar `startTime` inicial a `2026-01-01 00:00:00`.
*   **Compactar Panel**: Re-organizar los filtros en un grid de 3 columnas para reducir el desplazamiento vertical.
*   **Paginación**: Asegurar que la paginación funcione correctamente con el nuevo orden.

### [Server Component]

#### [MODIFY] [AttendanceRoutes.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/routes/AttendanceRoutes.kt)
*   **Orden Ascendente**: Cambiar `.orderBy(AttendanceLogTable.id, SortOrder.DESC)` a `SortOrder.ASC` en el endpoint `/logs` para mostrar los datos desde el más antiguo.

## Verification Plan

### Manual Verification
1.  **Carga Inicial**: Al entrar, la tabla debe mostrar registros empezando por los de enero (si existen) o los primeros capturados.
2.  **Visual**: Confirmar que el panel ya no requiere hacer scroll para ver la tabla.
3.  **Filtros**: Verificar que al presionar "Reset" se regresa a la fecha del 1 de enero.
