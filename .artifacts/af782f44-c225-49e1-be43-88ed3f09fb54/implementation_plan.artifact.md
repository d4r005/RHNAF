# Plan de Mejora EHS: Expansión a Nivel EHSSoft

Este plan detalla la expansión del módulo de EHS para incluir Gestión Ambiental, Salud Ocupacional y Manejo de Sustancias Químicas, alineando RHNAF con estándares internacionales (ISO 45001/14001).

## Análisis de la Expansión

Para competir con soluciones como EHSSoft, añadiremos tres pilares fundamentales:
1.  **Ambiente**: Control de residuos y cumplimiento ambiental.
2.  **Salud**: Vigilancia médica y exámenes periódicos.
3.  **Químicos**: Inventario de Hojas de Seguridad (MSDS) para cumplimiento legal.

## Proposed Changes

### [Shared Component]
Actualizar modelos de datos serializables.
#### [MODIFY] [SapModules.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/shared/src/commonMain/kotlin/com/example/rhnaf/shared/model/SapModules.kt)
*   Añadir `WasteManifest`, `MedicalExam`, y `ChemicalProduct`.

---

### [Server Component]
Persistencia y APIs.
#### [MODIFY] [SapModulesTables.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/database/SapModulesTables.kt)
*   Crear `EnvironmentalWasteTable`, `OccupationalHealthTable` y `ChemicalInventoryTable`.
#### [MODIFY] [DatabaseFactory.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/database/DatabaseFactory.kt)
*   Inicializar las nuevas tablas en `SchemaUtils.createMissingTablesAndColumns`.
#### [MODIFY] [SapModulesRoutes.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/server/src/main/kotlin/com/example/rhnaf/routes/SapModulesRoutes.kt)
*   Implementar los endpoints CRUD para los nuevos módulos.

---

### [Web Component]
Interfaz de usuario.
#### [MODIFY] [SapModulesUi.kt](file:///C:/Users/dtruj/AndroidStudioProjects/RHNAF/web/src/jsMain/kotlin/SapModulesUi.kt)
*   Añadir las pestañas: "Medio Ambiente", "Salud Ocupacional" y "Sustancias Químicas".
*   Implementar tablas y formularios de registro para cada una.

## Verification Plan

### Automated Tests
*   Ejecutar build local para asegurar que los modelos @Serializable compilan correctamente.
*   Verificar que las rutas del servidor responden (Mocking HttpClient).

### Manual Verification
*   Confirmar que al entrar al módulo EHS aparecen las nuevas 3 pestañas.
*   Probar el registro de un residuo ambiental y un examen médico.
*   **Finalización**: Realizar `git commit` y `git push` como se solicitó.

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
