# RHNAF EHS: alcance integral y condiciones de entrega

Inspiración funcional: EHSoft, ACM Suite, GeISS/Twind, Prysmex, MassWare y Enkel. No se copian código, catálogos ni material propietario. Una pantalla no prueba cumplimiento legal.

## Entrega 0: ya disponible
- Inspecciones, incidentes, permisos de trabajo, EPP, capacitaciones, simulacros, matriz de riesgos, residuos, salud ocupacional y químicos.
- Matriz legal editable con vigencias básicas y catálogo inicial; evidencia documental vinculada a Google Drive; acceso restringido por rol.
- RLS de denegación por defecto para anon/authenticated de Supabase; acceso mediante backend propio. Revisar autenticación y auditoría antes de exponer datos sensibles.

## Entrega 1: indicadores EHS (en esta rama)
- Conteos verificables de incidentes, días perdidos declarados, capacitaciones pendientes/vencidas y hallazgos abiertos.
- Mostrar ausencia de horas trabajadas en lugar de inventar tasas de frecuencia/gravedad. Antes de publicar dichas tasas se requieren horas reales por período, fórmula aprobada y criterio para incidentes registrables.

## Entrega 2: cumplimiento legal operativo
- Identificar centro de trabajo, domicilio/estado, giro, actividades y riesgos con confirmación del responsable EHS/legal.
- Catálogo de NOMs, permisos y fundamento verificable con versión, jurisdicción, fecha de publicación y fuente oficial. Revisiones periódicas y vigencia normativa. El catálogo actual es sólo una lista inicial y puede estar desactualizado.
- Cuestionario de aplicabilidad versionado con justificación, evidencia y revisión humana; auditorías por requisito; permisos críticos, evidencia y vencimientos.
- Planes de acción con responsables, prioridad, aprobación, fecha, cierre verificable y notificaciones sin revelar datos sensibles.

## Entrega 3: operación y terceros
- Contratistas y proveedores: expedientes, requisitos por actividad, verificación documental, vigencias, permisos de trabajo, accesos autorizados y trazabilidad por centro.
- EPP y constancias: entregas por trabajador, recibos/firmas verificables, caducidad, reposición y DC-3 revisada por RH; brigadas y planes de emergencia.
- Auditoría operativa móvil, registros sin conexión (con reconciliación), informes/exportación y tableros por planta/área.

## Entrega 4: NOM-035 y analítica avanzada
- Cuestionarios NOM-035 con versión oficial, cohortes/tamaños mínimos, consentimiento, accesos muy restrictivos, resultados agregados, plazos y revisión legal y de privacidad antes de recolectar respuestas. No usar tablas generales de empleados ni exponer respuestas individuales por defecto.
- Ratios de accidentabilidad sólo con denominadores auditables (horas trabajadas/días programados), definiciones acordadas y periodos comparables.
- Asistencia IA sólo como apoyo a redacción/clasificación, con revisión humana y fuentes trazables; nunca dictamina cumplimiento.

## Prerrequisitos de negocio para desplegar el conjunto
1. Responsable EHS y asesor legal que validen alcance de NOMs, aplicabilidad y metodología de cálculo.
2. Centros de trabajo, giro, actividades, estados/municipios, perfiles de permisos y matriz de responsables.
3. Horas trabajadas reales y definiciones de incidentes registrables para KPIs de frecuencia/gravedad.
4. Política de acceso/retención para salud y NOM-035; aprobación antes de captar datos sensibles.
5. Licencia/permiso para fuentes legales externas, integración documental y pruebas con datos de ejemplo sin información personal real.

Cada entrega exige compilación, pruebas de autorización y verificación de despliegue antes de declararse lista. Orden secuencial salvo que el dueño cambie las prioridades. No se presupone equivalencia funcional completa ni una fecha de entrega.
