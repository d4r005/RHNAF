# Plan de Refinamiento de Asistencia (Hikvision)

Este plan detalla los ajustes para filtrar eventos inválidos ("None") de la lectora y asegurar que el origen de los datos se identifique correctamente como "HIKVISIONWEB".

## Análisis del Problema

1.  **Eventos "None"**: La lectora envía eventos de sistema (aperturas de puerta, alarmas) que no están asociados a un empleado. Estos aparecen como "None" en el portal y deben ser ignorados.
2.  **Identificación de Origen**: El usuario solicita que los registros provenientes de la lectora se identifiquen como "HIKVISIONWEB" (o similar).
3.  **Filtrado de Eventos**: Solo se deben procesar checadas válidas de "Check-in" y "Check-out".

## Proposed Changes

### [Server Component]

#### [MODIFY] [AttendanceRoutes.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/routes/AttendanceRoutes.kt)
*   Actualizar `handleHikvisionRequest` para ignorar peticiones donde el ID de empleado sea `"None"`, `"null"`, `"0"` o esté vacío.
*   Cambiar el valor por defecto de `deviceId` a `"HIKVISIONWEB"`.

#### [MODIFY] [AttendanceUseCase.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/service/AttendanceUseCase.kt)
*   Añadir una validación en `registerCheckIn` para descartar IDs inválidos antes de intentar la inserción.
*   Si el nombre del empleado no se encuentra, pero el registro viene de la lectora, asignar un nombre descriptivo por defecto.

### [Script Component]

#### [MODIFY] [attendance_sync.py](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/scripts/attendance_sync.py)
*   Cambiar `"LOCAL-SYNC"` por `"HIKVISIONWEB"` en el payload enviado a la nube.
*   Robustecer el filtrado local para no enviar eventos que no tengan un ID de empleado válido.

## Verification Plan

### Manual Verification
1.  **Limpieza de Base de Datos**: Ejecutar un comando para borrar los registros basura actuales (`employee_id = 'None'`).
2.  **Prueba de Envío**: Enviar un evento manual con ID "None" y verificar que el servidor lo ignore (respondiendo 200 OK para no bloquear la terminal, pero sin guardar en DB).
3.  **Verificación en Portal**: Confirmar que en la tabla de asistencia ya no aparecen filas con "None" y que la columna "Dispositivo" muestra "HIKVISIONWEB".
