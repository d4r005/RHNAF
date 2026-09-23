# RHNAF EHS: alcance integral y condiciones de entrega

Inspiración funcional: EHSoft, ACM Suite, GeISS/Twind, Prysmex, MassWare y Enkel. No se copian código, catálogos ni material propietario. Una pantalla no prueba cumplimiento legal.

## Entrega 0: ya disponible
- Inspecciones, incidentes, permisos de trabajo, EPP, capacitaciones, simulacros, matriz de riesgos, residuos, salud ocupacional y químicos.
- Matriz legal editable con vigencias básicas y catálogo inicial; evidencia documental vinculada a Google Drive; acceso restringido por rol.
- RLS de denegación por defecto para anon/authenticated de Supabase; acceso mediante backend propio. Revisar autenticación y auditoría antes de exponer datos sensibles.

## Entrega 1: indicadores EHS (implementada)
- Conteos verificables de incidentes, días perdidos declarados, capacitaciones pendientes/vencidas y hallazgos abiertos.
- Mostrar ausencia de horas trabajadas en lugar de inventar tasas de frecuencia/gravedad. Antes de publicar dichas tasas se requieren horas reales por período, fórmula aprobada y criterio para incidentes registrables.

## Entrega 2: planes de acción y cumplimiento legal operativo
- Identificar centro de trabajo, domicilio/estado, giro, actividades y riesgos con confirmación del responsable EHS/legal.
- Catálogo de NOMs, permisos y fundamento verificable con versión, jurisdicción, fecha de publicación y fuente oficial. Revisiones periódicas y vigencia normativa. El catálogo actual es sólo una lista inicial y puede estar desactualizado.
- Evaluación manual de aplicabilidad, justificación, responsable y evidencia disponible en la matriz. Pendientes: cuestionario versionado por centro, auditorías por requisito y permisos críticos, sujetos a revisión jurídica.
- Planes de acción: creación, vínculo con obligación/inspección/incidente, responsable, prioridad, fecha y cierre con evidencia HTTPS están implementados en la entrega 2 inicial. Falta aprobación y auditoría de cambios. Los avisos internos de vencimiento se implementaron; faltan notificaciones enviadas con consentimiento, canal y destinatario verificados.

## Entrega 3: operación y terceros
- Contratistas y proveedores: expediente básico por empresa/centro, enlace HTTPS, vigencia y estado de revisión implementados. Pendientes: requisitos documentales por actividad, permisos de trabajo vinculados, acceso físico e historial auditable. El expediente no concede acceso.
- EPP y constancias: entregas por trabajador, recibos/firmas verificables, caducidad, reposición y DC-3 revisada por RH; brigadas y planes de emergencia.
- Auditoría operativa móvil, registros sin conexión (con reconciliación), informes/exportación y tableros por planta/área.

## Entrega 4: analítica avanzada (sin NOM-035)
- Ratios de accidentabilidad sólo con denominadores auditables (horas trabajadas/días programados), definiciones acordadas y periodos comparables.
- Asistencia IA sólo como apoyo a redacción/clasificación, con revisión humana y fuentes trazables; nunca dictamina cumplimiento.

## Prerrequisitos de negocio para desplegar el conjunto
1. Responsable EHS y asesor legal que validen alcance de NOMs, aplicabilidad y metodología de cálculo.
2. Centros de trabajo, giro, actividades, estados/municipios, perfiles de permisos y matriz de responsables.
3. Horas trabajadas reales y definiciones de incidentes registrables para KPIs de frecuencia/gravedad.
4. Política de acceso/retención para los registros de salud ocupacional existentes; no desarrollar ni captar respuestas NOM-035.
5. Licencia/permiso para fuentes legales externas, integración documental y pruebas con datos de ejemplo sin información personal real.

La NOM-035 queda expresamente fuera de este roadmap por instrucción del dueño. Esto no es una conclusión legal sobre su aplicabilidad: un especialista debe evaluar las obligaciones externas, pero RHNAF no incorpora ese módulo. El catálogo inicial nuevo no la incluirá; no se borran obligaciones preexistentes ni evidencia histórica sin una instrucción expresa.

Cada entrega exige compilación, pruebas de autorización y verificación de despliegue antes de declararse lista. Orden secuencial salvo que el dueño cambie las prioridades. No se presupone equivalencia funcional completa ni una fecha de entrega.
