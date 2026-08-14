# Plan de Ajuste de Orden de Asistencia: Más Reciente Primero

Este plan detalla el cambio en el orden de visualización de los registros de asistencia para mostrar primero los eventos más recientes (orden cronológico descendente).

## Análisis del Requerimiento

1.  **Orden de Visualización**: El usuario solicita que el listado se muestre de la fecha más reciente a la más antigua.
2.  **Impacto en Backend**: Se debe modificar la consulta SQL en el servidor para usar `SortOrder.DESC` en lugar de `SortOrder.ASC`.
3.  **Exportaciones**: Se actualizará también el orden en la exportación de datos crudos (CSV) para mantener la consistencia.

## Proposed Changes

### [Server Component]

#### [MODIFY] [AttendanceRoutes.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/routes/AttendanceRoutes.kt)
*   Cambiar el orden de `SortOrder.ASC` a `SortOrder.DESC` en el endpoint de listado (`/logs`).
*   Cambiar el orden de `SortOrder.ASC` a `SortOrder.DESC` en el endpoint de exportación de datos crudos (`/export/raw/csv`).

## Verification Plan

### Manual Verification
1.  **Carga de Pantalla**: Al entrar al portal, los registros de Agosto deben aparecer en la página 1, y los de Enero en las últimas páginas.
2.  **Exportación**: Descargar el CSV y verificar que los registros superiores son los más recientes.
