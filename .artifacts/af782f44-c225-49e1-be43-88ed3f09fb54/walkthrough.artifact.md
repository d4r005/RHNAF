# Walkthrough: Expansión de Módulo EHS

He elevado el módulo de EHS a un nivel corporativo, integrando funcionalidades clave para la gestión ambiental y salud ocupacional.

## Cambios Realizados

### 1. Gestión Ambiental (Residuos)
*   Se añadió la pestaña **"Medio Ambiente"**.
*   Permite el registro de manifiestos de residuos (tipo, cantidad, transportista, destino final).
*   Ideal para el cumplimiento de normativas de disposición de residuos industriales.

### 2. Salud Ocupacional
*   Se añadió la pestaña **"Salud Ocupacional"**.
*   Módulo para rastrear exámenes médicos de ingreso, periódicos y seguimiento de citas médicas para los empleados.

### 3. Gestión de Sustancias Químicas (MSDS)
*   Se añadió la pestaña **"Químicos"**.
*   Inventario de productos químicos utilizados en la planta con enlace directo a sus Hojas de Seguridad (MSDS) en PDF.
*   Incluye clasificación de nivel de riesgo.

### 4. Robustez de Infraestructura
*   Se actualizó la persistencia para incluir 3 nuevas tablas en la base de datos de Supabase.
*   Se implementaron los endpoints CRUD correspondientes en el servidor Ktor.

## Próximos Pasos Recomendados
- Cargar los PDFs de las Hojas de Seguridad (MSDS) en un repositorio público o S3 y vincular los links en el módulo de Químicos.
- Integrar reportes automáticos de indicadores de seguridad (TRIR, LTIR) basados en los incidentes registrados.
