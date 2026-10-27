import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import com.example.rhnaf.shared.model.*
import com.example.rhnaf.domain.model.*
import kotlinx.coroutines.launch
import kotlinx.browser.window
import kotlinx.browser.document
import androidx.compose.runtime.mutableStateListOf

// DISEÑO NAF CONNECT - IDENTIDAD INDUSTRIAL MODERNA
val SidebarColor = Color("#0f172a") 
val SidebarActiveColor = Color("#2563eb")
val BackgroundColor = Color("#f1f5f9")
val CardShadow = "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"

enum class Language { ES, EN, ZH }

class Translations(val lang: Language) {
    private val es = mapOf(
        "dashboard" to "Panel de Control",
        "employees" to "Plantilla Personal",
        "recruitment" to "Reclutamiento",
        "attendance" to "Asistencia Facial",
        "payroll" to "Nómina y Pagos",
        "training" to "Capacitación",
        "performance" to "Desempeño",
        "incidents" to "Seguridad (EHS)",
        "vacations" to "Vacaciones",
        "documents" to "Expedientes",
        "reports" to "Reportes",
        "settings" to "Configuración",
        "welcome" to "Bienvenido",
            "login_wait" to "INICIANDO SESIÓN...",
        "login" to "INICIAR SESIÓN",
        "logout" to "Cerrar sesión",
        "email" to "CORREO ELECTRÓNICO",
        "password" to "CONTRASEÑA",
        "remember_me" to "Recordar usuario",
        "total_emp" to "Total Empleados",
        "active_emp" to "Empleados Activos",
        "vacancies" to "Vacantes Abiertas",
        "training_pending" to "Capacitación Pendiente",
        "incidents_today" to "Incidencias Hoy",
        "export_csv" to "Exportar CSV",
        "new_emp" to "+ Nuevo Empleado",
        "edit" to "Editar",
        "delete" to "Eliminar",
        "save" to "Guardar Cambios",
        "cancel" to "Cancelar",
        "back" to "Regresar",
        "profile_settings" to "Configuración de Perfil",
        "select_avatar" to "Seleccionar Avatar Profesional",
        "employee_photo" to "Foto del Personal",
        "display_name" to "Nombre a mostrar",
        "select_lang" to "Idioma del Sistema",
        "ai_analysis" to "Análisis Inteligente",
        "turnover_risk" to "Riesgo de Rotación (IA)",
        "skill_heatmap" to "Mapa de Habilidades",
        "predictive" to "Predictivo",
        "talent_market" to "Mercado de Talento",
        "esg_metrics" to "Sostenibilidad (ESG)",
        "pulse" to "Clima Laboral (Pulse)",
        "ai_assistant" to "Asistente NAF AI",
        "assets" to "Gestión de Activos (EPP)",
        "shifts" to "Turnos y Horarios",
        "benefits" to "Compensación y Beneficios",
        "workflows" to "Flujos de Aprobación",
        "warehouse" to "Almacén e Inventarios",
        "shipping" to "Embarques y Logística",
        "ferreteria" to "Ferretería e Insumos",
        "recepcion_mp" to "Recepción Materias Primas",

        // ===== QIS =====
        "qis_intro" to "Sistema de gestión de calidad: alta de documentos controlados con código único y versión, control de vigencias y registro de cambios por trazabilidad. El historial de cada documento es inmutable.",
        "qis_alta" to "Dar de alta un documento controlado",
        "qis_alta_hint" to "La versión inicial siempre es 1.0 y queda registrada en el historial como \"Alta del documento\". El código es único (p. ej. PRO-001).",
        "qis_btn_alta" to "+ Dar de alta",
        "qis_filtro" to "Filtrar por estado:",
        "qis_todos" to "Todos",
        "qis_vigentes" to "Vigentes",
        "qis_en_revision" to "En revisión",
        "qis_obsoletos" to "Obsoletos",
        "qis_doc_count" to "{n} documentos",
        "qis_ver" to "Ver", "qis_descargar" to "Descargar", "qis_editar" to "Editar",
        "qis_historial" to "Historial",
        "qis_estado" to "Estado:",
        "qis_cambios" to "Registro de cambios ({n})",
        "qis_sin_cambios" to "Sin cambios registrados.",
        "qis_nueva_version" to "Registrar nueva versión (actual: {v})",
        "qis_btn_cambio" to "Registrar cambio",
        "qis_alta_ok" to "Documento dado de alta en versión 1.0.",
        "qis_alta_falta" to "Código, título, fecha de emisión y elaboró son obligatorios.",
        "qis_cambio_falta" to "Versión, descripción y realizador son obligatorios.",
        "qis_ph_codigo" to "Código * (PRO-001)",
        "qis_ph_titulo" to "Título *",
        "qis_ph_proceso" to "Proceso / área",
        "qis_ph_emision" to "Emisión (dd/MM/aaaa) *",
        "qis_ph_revision" to "Próxima revisión (dd/MM/aaaa)",
        "qis_ph_elaboro" to "Elaboró *",
        "qis_ph_aprobo" to "Aprobó",
        "qis_ph_url" to "URL PDF en Drive (https://...)",
        "qis_ph_notas" to "Notas",
        "qis_ph_nueva_version" to "Nueva versión (N.N) *",
        "qis_ph_fecha" to "Fecha (dd/MM/aaaa)",
        "qis_ph_desc" to "Descripción del cambio *",
        "qis_ph_realizo" to "Realizó el cambio *",
        "qis_ph_nueva_url" to "Nueva URL del PDF (opcional)",
        // ===== Calendario =====
        "cal_titulo" to "Calendario de vencimientos",
        "cal_intro" to "Obligaciones legales, capacitaciones, simulacros, acciones, auditorías internas y contratistas. Da clic en un día para agregar un evento propio. Los permisos críticos se muestran en rojo.",
        "cal_hoy" to "Hoy",
        "cal_outlook" to "⤓ Outlook (.ics)",
        "cal_cargando" to "Cargando calendario...",
        "cal_mas" to "+{n} más",
        "cal_eventos_dia" to "Eventos del {fecha}",
        "cal_cerrar" to "Cerrar",
        "cal_tareas" to "{n} tarea(s)",
        "cal_borrar" to "Borrar",
        "cal_sin_eventos" to "Sin eventos ese día todavía.",
        "cal_agregar" to "+ Agregar evento",
        "cal_detalles" to "Detalles de {mes}",
        "cal_si" to "⚠ Sí",
        "cal_ph_titulo" to "Título del evento *",
        "cal_ph_detalle" to "Detalle",
        "cal_ph_resp" to "Responsable",
        // ===== Planes de accion =====
        "act_titulo" to "Planes de acción EHS",
        "act_intro" to "Da seguimiento a hallazgos, incidentes y obligaciones. El cierre exige una URL HTTPS de evidencia.",
        "act_vacio" to "No hay planes de acción registrados.",
        "act_en_progreso" to "En progreso",
        "act_cerrar" to "Cerrar con evidencia",
        "act_cerrada" to "Cerrada {fecha}. Evidencia: {url}",
        "act_ph_accion" to "Acción correctiva *",
        "act_ph_resp" to "Responsable *",
        "act_ph_limite" to "Fecha límite AAAA-MM-DD *",
        "act_ph_origen" to "ID del origen *",
        "act_ph_evidencia" to "URL HTTPS de evidencia para cerrar",
        // ===== Contratistas =====
        "con_titulo" to "Contratistas y proveedores",
        "con_intro" to "Expediente por empresa y centro de trabajo. Aprobar un expediente no concede acceso físico ni sustituye la validación de seguridad.",
        "con_crear" to "Crear expediente",
        "con_vacio" to "No hay empresas registradas.",
        "con_guardar" to "Guardar revisión",
        "con_ph_empresa" to "Empresa *",
        "con_ph_actividad" to "Actividad *",
        "con_ph_centro" to "Centro de trabajo *",
        "con_ph_resp" to "Responsable interno *",
        "con_ph_url" to "URL HTTPS del documento",
        "con_ph_vigencia" to "Vigencia AAAA-MM-DD",
        // ===== Indicadores =====
        "met_titulo" to "Indicadores EHS",
        "met_intro" to "Conteos derivados de los registros actuales, no tasas oficiales de accidentabilidad.",
        "met_cargando" to "Cargando indicadores...",
        "met_calidad_datos" to "Calidad de datos: {a} accidentes sin días perdidos válidos y {b} capacitaciones sin próxima fecha válida.",
        "met_if" to "Índice de frecuencia (IF)",
        "met_ig" to "Índice de gravedad (IG)",
        "met_cumplimiento" to "Cumplimiento legal",
        "met_aplicables" to "{n} obligaciones aplicables en la matriz",
        "met_permisos" to "Permisos críticos",
        "met_criticos" to "{v} vencidos · {p} por vencer",
        "met_incidentes_mes" to "Accidentes por mes (últimos 12 meses)",
        "met_dias_perdidos" to "Incluye {n} días perdidos en el período. Si la serie se ve vacía, registra la fecha de tus accidentes.",
        // ===== Tasas =====
        "rate_titulo" to "Tasas internas de accidentabilidad",
        "rate_intro" to "Carga horas reales y accidentes registrables validados por cada mes. Frecuencia = accidentes × 1,000,000 / horas; gravedad = días perdidos × 1,000,000 / horas. No son tasas oficiales ni se obtienen automáticamente de pre-nómina.",
        "rate_guardar" to "Guardar periodo",
        "rate_sin_horas" to "Sin horas validadas. No es posible calcular tasas todavía.",
        "rate_horas" to "Horas: {h} | Accidentes registrables: {a} | Días perdidos: {d}",
        "rate_frecuencia" to "Frecuencia: {f} por millón de horas | Gravedad: {g} días por millón de horas",
        "rate_ph_periodo" to "Periodo AAAA-MM *",
        "rate_ph_horas" to "Horas reales *",
        "rate_ph_accidentes" to "Accidentes registrables *",
        "rate_ph_dias" to "Días perdidos *",
        "rate_ph_fuente" to "Fuente de las horas *",
        "rate_ph_validado" to "Validado por *",
        "rate_ph_motivo" to "Motivo de revisión (si corrige el periodo)",
        // ===== Centro EHS =====
        "home_titulo" to "Centro EHS y cumplimiento",
        "home_intro" to "Seguridad, salud, ambiente y control documental en una sola operación.",
        "home_pilares" to "Operación por pilar",
        "home_gestion" to "Gestión y cumplimiento",
        "home_matriz_nota" to "La matriz es una herramienta de seguimiento; no determina por sí sola el cumplimiento legal.",
        "home_evidencia" to "Evidencia documental",
        "home_evidencia_intro" to "Archivos en Google Drive, metadatos y vínculos con auditorías internas, simulacros y capacitación.",
        "home_evidencia_permiso" to "Solo el personal de Seguridad puede cargar o gestionar evidencia documental.",
        // ===== Avisos =====
        "alert_titulo" to "Avisos de vencimiento EHS",
        "alert_intro" to "Avisos dentro de la aplicación, para registros vencidos o que vencen en 30 días. No se han enviado correos.",
        "alert_cargando" to "Cargando avisos...",
        "alert_vacio" to "No hay vencimientos registrados dentro de los próximos 30 días.",
        "alert_criticos" to "⚠ {n} permisos críticos requieren atención inmediata (riesgo de clausura o multa).",
        "bell_vacio" to "Nada vencido ni por vencer en los próximos días.",
        "bell_ver_todos" to "Ver todos los avisos →",
        "qis_h_codigo" to "Código",
        "qis_h_titulo" to "Título",
        "qis_h_tipo" to "Tipo",
        "qis_h_proceso" to "Proceso",
        "qis_h_version" to "Versión",
        "qis_h_estado" to "Estado",
        "qis_h_emision" to "Emisión",
        "qis_h_prox" to "Próx. revisión",
        "qis_h_pdf" to "PDF",
        "qis_h_cambios" to "Cambios",
        "qis_h_fecha" to "Fecha",
        "qis_h_desc" to "Descripción",
        "qis_h_cambio_por" to "Cambió",
        "qis_h_aprobo" to "Aprobó",
        "qis_estado_vigente" to "Vigente",
        "qis_estado_revision" to "En revisión",
        "qis_estado_obsoleto" to "Obsoleto",
        "rate_version" to "revisión",
        "qis_confirm_borrar" to "¿Borrar el documento {c}? Borra también su historial. Para retiro oficial usa \"Obsoleto\".",
        "qis_archivo" to "Archivo del documento:",
        "qis_archivo_hint" to "PDF, Word o Excel. Se guarda en Google Drive (QIS/año).",
        "qis_subir_archivo" to "Subir/actualizar archivo en Drive:",
        "qis_archivo_ok" to "Archivo guardado en Drive.",
        "qis_archivo_fallo" to "No se pudo subir el archivo a Drive",
        "qis_import_listado" to "Importar listado maestro (Excel)",
        "qis_import_hint" to "Sube tu Excel de documentos controlados; las columnas se detectan automáticamente (código, título, tipo, proceso, versión, fechas, elaboró, aprobó). Los códigos ya registrados se omiten y el Excel queda respaldado en Drive.",
        "qis_import_btn" to "Importar",
        "qis_import_ok" to "Importación: {n} documentos dados de alta, {d} omitidos (ya existían).",
        "qis_import_sin_archivo" to "Selecciona primero el archivo Excel.",
        "qis_import_error" to "No se pudo importar el listado",
        "qis_import_drive_error" to "(El respaldo en Drive falló; los datos sí quedaron registrados).",
        "cal_titulo_obligatorio" to "El título es obligatorio.",
        "cal_exportar_error" to "No se pudo exportar: {e}",
        "import_export" to "Importación y Exportación",
        "stock" to "Stock Actual",
        "suppliers" to "Proveedores",
        "customs" to "Aduanas y Logística",
        "safety_audits" to "Auditorías EHS",
        "incidents_log" to "Bitácora de Incidentes",
        "patrimonial" to "Seguridad Patrimonial",
        "cctv" to "Monitoreo CCTV",
        "guard_tours" to "Rondas de Guardia",
        "maintenance" to "Mantenimiento (CMMS)",
        "employee_portal" to "Portal del Empleado",
        "finance" to "Finanzas y Facturación",
        "energy" to "Gestión Energética",
        "machine_status" to "Estado de Maquinaria",
        "billing" to "Facturación Industrial",
        "self_service" to "Autoservicio",
        "user_mgmt" to "Gestión de Usuarios",
        "controlling" to "Controlling (CO)",
        "purchasing" to "Compras (MM)",
        "production_planning" to "Producción (PP)",
        "quality_management" to "Calidad (QM)",
        "gts_trade" to "Comercio Exterior (GTS)",
        "it_security_grc" to "Seguridad SAP / GRC",
        "financial_accounting" to "Contabilidad (FI)",
        "plant_maintenance" to "Mantenimiento Planta (PM)",
        "recruitment_sap" to "Reclutamiento (HCM)",
        "nav_employees" to "Empleados",
        "nav_attendance" to "Asistencia",
        "pre_nomina" to "Pre-Nómina",
        "ehs_home" to "Centro EHS",
        "ehs_seguridad" to "Seguridad",
        "ehs_salud" to "Salud Ocupacional",
        "ehs_ambiente" to "Medio Ambiente",
        "ehs_metrics" to "Indicadores EHS",
        "ehs_actions" to "Planes de acción",
        "ehs_contractors" to "Contratistas",
        "ehs_alerts" to "Avisos EHS",
        "ehs_rates" to "Tasas EHS",
        "ehs_documents" to "Evidencia documental",
        "ehs_calendar" to "Calendario",
        "legal_matrix" to "Matriz Legal",
        "stps_normas" to "Normas STPS",
        "qis_docs" to "QIS · Control de documentos",
        "lockers" to "Listado de Lockers",
        "group_hr" to "RECURSOS HUMANOS",
        "group_ehs" to "EHS Y CUMPLIMIENTO",
        "group_calidad" to "CALIDAD",
        "group_ops" to "OPERACIONES",
        "group_supply" to "SUMINISTRO Y LOGÍSTICA",
        "group_fin" to "FINANZAS",
        "group_admin" to "ADMINISTRACIÓN"
    )
    private val en = mapOf(
        "dashboard" to "Dashboard",
        "employees" to "Staff Directory",
        "recruitment" to "Recruitment",
        "attendance" to "Facial Attendance",
        "payroll" to "Payroll",
        "training" to "Training",
        "performance" to "Performance",
        "incidents" to "Safety (EHS)",
        "vacations" to "Vacations",
        "documents" to "Documents",
        "reports" to "Reports",
        "settings" to "Settings",
        "welcome" to "Welcome",
            "login_wait" to "SIGNING IN...",
        "login" to "LOG IN",
        "logout" to "Log out",
        "email" to "EMAIL ADDRESS",
        "password" to "PASSWORD",
        "remember_me" to "Remember me",
        "total_emp" to "Total Employees",
        "active_emp" to "Active Employees",
        "vacancies" to "Open Vacancies",
        "training_pending" to "Pending Training",
        "incidents_today" to "Incidents Today",
        "export_csv" to "Export CSV",
        "new_emp" to "+ New Employee",
        "edit" to "Edit",
        "delete" to "Delete",
        "save" to "Save Changes",
        "cancel" to "Cancel",
        "back" to "Go Back",
        "profile_settings" to "Profile Settings",
        "select_avatar" to "Select Professional Avatar",
        "employee_photo" to "Employee Photo",
        "display_name" to "Display Name",
        "select_lang" to "System Language",
        "ai_analysis" to "AI Analysis",
        "turnover_risk" to "Attrition Risk (AI)",
        "skill_heatmap" to "Skill Heatmap",
        "predictive" to "Predictive",
        "talent_market" to "Talent Marketplace",
        "esg_metrics" to "Sustainability (ESG)",
        "pulse" to "Employee Pulse",
        "ai_assistant" to "NAF AI Assistant",
        "assets" to "Asset Mgmt (PPE)",
        "shifts" to "Shifts & Scheduling",
        "benefits" to "Benefits & Compensation",
        "workflows" to "Approval Workflows",
        "warehouse" to "Warehouse & Inventory",
        "shipping" to "Shipping & Logistics",
        "import_export" to "Import & Export",
        "stock" to "Current Stock",
        "suppliers" to "Suppliers",
        "customs" to "Customs & Logistics",
        "safety_audits" to "EHS Audits",
        "incidents_log" to "Incident Log",
        "patrimonial" to "Asset Protection",
        "cctv" to "CCTV Monitoring",
        "guard_tours" to "Guard Patrols",
        "maintenance" to "Maintenance (CMMS)",
        "employee_portal" to "Employee Portal",
        "finance" to "Finance & Billing",
        "energy" to "Energy Management",
        "machine_status" to "Machine Status",
        "billing" to "Industrial Billing",
        "self_service" to "Self-Service",
        "user_mgmt" to "User Management",
        "controlling" to "Controlling (CO)",
        "purchasing" to "Purchasing (MM)",
        "production_planning" to "Production (PP)",
        "quality_management" to "Quality (QM)",
        "gts_trade" to "Global Trade (GTS)",
        "it_security_grc" to "SAP Security / GRC",
        "financial_accounting" to "Accounting (FI)",
        "plant_maintenance" to "Plant Maintenance (PM)",
        "recruitment_sap" to "Recruitment (HCM)",
        "nav_employees" to "Employees",
        "nav_attendance" to "Attendance",
        "pre_nomina" to "Pre-Payroll",
        "ehs_home" to "EHS Center",
        "ehs_seguridad" to "Safety",
        "ehs_salud" to "Occupational Health",
        "ehs_ambiente" to "Environment",
        "ehs_metrics" to "EHS Indicators",
        "ehs_actions" to "Action Plans",
        "ehs_contractors" to "Contractors",
        "ehs_alerts" to "EHS Notices",
        "ehs_rates" to "EHS Rates",
        "ehs_documents" to "Documentary Evidence",
        "ehs_calendar" to "Calendar",
        "legal_matrix" to "Legal Matrix",
        "stps_normas" to "STPS Standards",
        "qis_docs" to "QIS · Document Control",
        "lockers" to "Locker List",
        "group_hr" to "HUMAN RESOURCES",
        "group_ehs" to "EHS & COMPLIANCE",
        "group_calidad" to "QUALITY",
        "group_ops" to "OPERATIONS",
        "group_supply" to "SUPPLY & LOGISTICS",
        "group_fin" to "FINANCE",
        "group_admin" to "ADMINISTRATION",
        "ferreteria" to "Hardware & Supplies",
        "recepcion_mp" to "Raw Materials Reception",

        // ===== QIS =====
        "qis_intro" to "Quality management system: register controlled documents with a unique code and version, validity tracking and an immutable change log for traceability.",
        "qis_alta" to "Register a controlled document",
        "qis_alta_hint" to "The initial version is always 1.0 and is recorded in the history as \"Register document\". The code is unique (e.g. PRO-001).",
        "qis_btn_alta" to "+ Register",
        "qis_filtro" to "Filter by status:",
        "qis_todos" to "All",
        "qis_vigentes" to "Active",
        "qis_en_revision" to "Under review",
        "qis_obsoletos" to "Obsolete",
        "qis_doc_count" to "{n} documents",
        "qis_ver" to "View", "qis_descargar" to "Download", "qis_editar" to "Edit",
        "qis_historial" to "History",
        "qis_estado" to "Status:",
        "qis_cambios" to "Change log ({n})",
        "qis_sin_cambios" to "No changes recorded.",
        "qis_nueva_version" to "Register new version (current: {v})",
        "qis_btn_cambio" to "Register change",
        "qis_alta_ok" to "Document registered in version 1.0.",
        "qis_alta_falta" to "Code, title, issue date and prepared-by are required.",
        "qis_cambio_falta" to "Version, description and author are required.",
        "qis_ph_codigo" to "Code * (PRO-001)",
        "qis_ph_titulo" to "Title *",
        "qis_ph_proceso" to "Process / area",
        "qis_ph_emision" to "Issue date (dd/MM/yyyy) *",
        "qis_ph_revision" to "Next review (dd/MM/yyyy)",
        "qis_ph_elaboro" to "Prepared by *",
        "qis_ph_aprobo" to "Approved by",
        "qis_ph_url" to "PDF URL in Drive (https://...)",
        "qis_ph_notas" to "Notes",
        "qis_ph_nueva_version" to "New version (N.N) *",
        "qis_ph_fecha" to "Date (dd/MM/yyyy)",
        "qis_ph_desc" to "Change description *",
        "qis_ph_realizo" to "Changed by *",
        "qis_ph_nueva_url" to "New PDF URL (optional)",
        // ===== Calendario =====
        "cal_titulo" to "Due-date calendar",
        "cal_intro" to "Legal obligations, trainings, drills, actions, internal audits and contractors. Click a day to add your own event. Critical permits are shown in red.",
        "cal_hoy" to "Today",
        "cal_outlook" to "⤓ Outlook (.ics)",
        "cal_cargando" to "Loading calendar...",
        "cal_mas" to "+{n} more",
        "cal_eventos_dia" to "Events of {fecha}",
        "cal_cerrar" to "Close",
        "cal_tareas" to "{n} task(s)",
        "cal_borrar" to "Delete",
        "cal_sin_eventos" to "No events on that day yet.",
        "cal_agregar" to "+ Add event",
        "cal_detalles" to "Details of {mes}",
        "cal_si" to "⚠ Yes",
        "cal_ph_titulo" to "Event title *",
        "cal_ph_detalle" to "Detail",
        "cal_ph_resp" to "Owner",
        // ===== Planes de accion =====
        "act_titulo" to "EHS action plans",
        "act_intro" to "Track findings, incidents and obligations. Closing requires an HTTPS evidence URL.",
        "act_vacio" to "No action plans registered.",
        "act_en_progreso" to "In progress",
        "act_cerrar" to "Close with evidence",
        "act_cerrada" to "Closed {fecha}. Evidence: {url}",
        "act_ph_accion" to "Corrective action *",
        "act_ph_resp" to "Owner *",
        "act_ph_limite" to "Due date YYYY-MM-DD *",
        "act_ph_origen" to "Source ID *",
        "act_ph_evidencia" to "HTTPS evidence URL to close",
        // ===== Contratistas =====
        "con_titulo" to "Contractors and suppliers",
        "con_intro" to "File per company and work site. Approving a file does not grant physical access nor replace the safety validation.",
        "con_crear" to "Create file",
        "con_vacio" to "No companies registered.",
        "con_guardar" to "Save review",
        "con_ph_empresa" to "Company *",
        "con_ph_actividad" to "Activity *",
        "con_ph_centro" to "Work site *",
        "con_ph_resp" to "Internal owner *",
        "con_ph_url" to "HTTPS document URL",
        "con_ph_vigencia" to "Validity YYYY-MM-DD",
        // ===== Indicadores =====
        "met_titulo" to "EHS indicators",
        "met_intro" to "Counts derived from current records, not official accident rates.",
        "met_cargando" to "Loading indicators...",
        "met_calidad_datos" to "Data quality: {a} accidents without valid lost days and {b} trainings without a valid next date.",
        "met_if" to "Frequency index (FI)",
        "met_ig" to "Severity index (SI)",
        "met_cumplimiento" to "Legal compliance",
        "met_aplicables" to "{n} applicable obligations in the matrix",
        "met_permisos" to "Critical permits",
        "met_criticos" to "{v} expired · {p} due soon",
        "met_incidentes_mes" to "Accidents per month (last 12 months)",
        "met_dias_perdidos" to "Includes {n} lost days in the period. If the series looks empty, record your accidents dates.",
        // ===== Tasas =====
        "rate_titulo" to "Internal accident rates",
        "rate_intro" to "Enter real hours and validated recordable accidents per month. Frequency = accidents × 1,000,000 / hours; severity = lost days × 1,000,000 / hours. These are not official rates nor pulled automatically from pre-payroll.",
        "rate_guardar" to "Save period",
        "rate_sin_horas" to "No validated hours. Rates cannot be calculated yet.",
        "rate_horas" to "Hours: {h} | Recordable accidents: {a} | Lost days: {d}",
        "rate_frecuencia" to "Frequency: {f} per million hours | Severity: {g} days per million hours",
        "rate_ph_periodo" to "Period YYYY-MM *",
        "rate_ph_horas" to "Real hours *",
        "rate_ph_accidentes" to "Recordable accidents *",
        "rate_ph_dias" to "Lost days *",
        "rate_ph_fuente" to "Source of hours *",
        "rate_ph_validado" to "Validated by *",
        "rate_ph_motivo" to "Revision reason (if correcting the period)",
        // ===== Centro EHS =====
        "home_titulo" to "EHS and compliance center",
        "home_intro" to "Safety, health, environment and document control in one operation.",
        "home_pilares" to "Operation by pillar",
        "home_gestion" to "Management and compliance",
        "home_matriz_nota" to "The matrix is a tracking tool; it does not by itself determine legal compliance.",
        "home_evidencia" to "Document evidence",
        "home_evidencia_intro" to "Files in Google Drive, metadata and links with internal audits, drills and training.",
        "home_evidencia_permiso" to "Only Safety staff can upload or manage document evidence.",
        // ===== Avisos =====
        "alert_titulo" to "EHS due-date notices",
        "alert_intro" to "In-app notices for expired records or those due within 30 days. No emails have been sent.",
        "alert_cargando" to "Loading notices...",
        "alert_vacio" to "No due dates recorded within the next 30 days.",
        "alert_criticos" to "⚠ {n} critical permits require immediate attention (shutdown or fine risk).",
        "bell_vacio" to "Nothing overdue or due in the coming days.",
        "bell_ver_todos" to "See all notices →",
        "qis_h_codigo" to "Code",
        "qis_h_titulo" to "Title",
        "qis_h_tipo" to "Type",
        "qis_h_proceso" to "Process",
        "qis_h_version" to "Version",
        "qis_h_estado" to "Status",
        "qis_h_emision" to "Issued",
        "qis_h_prox" to "Next review",
        "qis_h_pdf" to "PDF",
        "qis_h_cambios" to "Changes",
        "qis_h_fecha" to "Date",
        "qis_h_desc" to "Description",
        "qis_h_cambio_por" to "Changed by",
        "qis_h_aprobo" to "Approved by",
        "qis_estado_vigente" to "Active",
        "qis_estado_revision" to "Under review",
        "qis_estado_obsoleto" to "Obsolete",
        "rate_version" to "rev.",
        "qis_confirm_borrar" to "Delete document {c}? This also deletes its history. For official withdrawal use \"Obsolete\".",
        "qis_archivo" to "Document file:",
        "qis_archivo_hint" to "PDF, Word or Excel. Stored in Google Drive (QIS/year).",
        "qis_subir_archivo" to "Upload/replace file in Drive:",
        "qis_archivo_ok" to "File saved to Drive.",
        "qis_archivo_fallo" to "Could not upload the file to Drive",
        "qis_import_listado" to "Import master list (Excel)",
        "qis_import_hint" to "Upload your controlled-documents Excel; columns are detected automatically (code, title, type, process, version, dates, prepared by, approved by). Existing codes are skipped and the Excel is backed up in Drive.",
        "qis_import_btn" to "Import",
        "qis_import_ok" to "Import: {n} documents registered, {d} skipped (already existed).",
        "qis_import_sin_archivo" to "Select the Excel file first.",
        "qis_import_error" to "Could not import the list",
        "qis_import_drive_error" to "(Drive backup failed; the data was registered anyway).",
        "cal_titulo_obligatorio" to "The title is required.",
        "cal_exportar_error" to "Export failed: {e}"
    )
    private val zh = mapOf(
        "dashboard" to "仪表板",
        "employees" to "员工名册",
        "recruitment" to "招聘管理",
        "attendance" to "人脸考勤",
        "payroll" to "薪资管理",
        "training" to "培训中心",
        "performance" to "绩效评估",
        "incidents" to "安全 (EHS)",
        "vacations" to "假期管理",
        "documents" to "文档中心",
        "reports" to "报告统计",
        "settings" to "系统设置",
        "welcome" to "欢迎",
            "login_wait" to "登入中…",
        "login" to "登入",
        "logout" to "登出",
        "email" to "电子邮件",
        "password" to "密码",
        "remember_me" to "记住我",
        "total_emp" to "总员工数",
        "active_emp" to "在职员工",
        "vacancies" to "招聘空缺",
        "training_pending" to "待完成培训",
        "incidents_today" to "今日事故",
        "export_csv" to "导出 CSV",
        "new_emp" to "+ 新增员工",
        "edit" to "编辑",
        "delete" to "删除",
        "save" to "保存更改",
        "cancel" to "取消",
        "back" to "返回",
        "profile_settings" to "个人资料设置",
        "select_avatar" to "选择专业头像",
        "employee_photo" to "员工照片",
        "display_name" to "显示名称",
        "select_lang" to "系统语言",
        "ai_analysis" to "智能分析",
        "turnover_risk" to "人员离职风险 (AI)",
        "skill_heatmap" to "技能热图",
        "predictive" to "预测性",
        "talent_market" to "内部人才市场",
        "esg_metrics" to "可持续发展 (ESG)",
        "pulse" to "员工满意度调查",
        "ai_assistant" to "NAF AI 助手",
        "assets" to "资产管理 (PPE)",
        "shifts" to "班次和排班",
        "benefits" to "福利与薪酬",
        "workflows" to "审批流",
        "warehouse" to "仓库与库存",
        "shipping" to "发货与物流",
        "import_export" to "进出口管理",
        "stock" to "当前库存",
        "suppliers" to "供应商",
        "customs" to "海关与物流",
        "safety_audits" to "安全审计 (EHS)",
        "incidents_log" to "事故记录",
        "patrimonial" to "资产安保",
        "cctv" to "视频监控 (CCTV)",
        "guard_tours" to "巡更管理",
        "maintenance" to "设备维护 (CMMS)",
        "employee_portal" to "员工自助服务",
        "finance" to "财务与计费",
        "energy" to "能源管理",
        "machine_status" to "机器状态",
        "billing" to "工业计费",
        "self_service" to "自助服务",
        "user_mgmt" to "用户管理",
        "controlling" to "成本控制 (CO)",
        "purchasing" to "采购 (MM)",
        "production_planning" to "生产计划 (PP)",
        "quality_management" to "质量管理 (QM)",
        "it_security_grc" to "SAP安全 / GRC",
        "financial_accounting" to "财务会计 (FI)",
        "plant_maintenance" to "工厂维护 (PM)",
        "recruitment_sap" to "招聘 (HCM)",
        "nav_employees" to "员工",
        "nav_attendance" to "考勤",
        "pre_nomina" to "预工资",
        "ehs_home" to "EHS 中心",
        "ehs_seguridad" to "安全",
        "ehs_salud" to "职业健康",
        "ehs_ambiente" to "环境",
        "ehs_metrics" to "EHS 指标",
        "ehs_actions" to "行动计划",
        "ehs_contractors" to "承包商",
        "ehs_alerts" to "EHS 通知",
        "ehs_rates" to "EHS 比率",
        "ehs_documents" to "文件证据",
        "ehs_calendar" to "日历",
        "legal_matrix" to "法律矩阵",
        "stps_normas" to "STPS 标准",
        "qis_docs" to "QIS · 文件控制",
        "lockers" to "储物柜清单",
        "group_hr" to "人力资源",
        "group_ehs" to "EHS 与合规",
        "group_calidad" to "质量",
        "group_ops" to "运营",
        "group_supply" to "供应链与物流",
        "group_fin" to "财务",
        "group_admin" to "管理",
        "ferreteria" to "五金工具与耗材",
        "recepcion_mp" to "原材料收货",

        // ===== QIS =====
        "qis_intro" to "质量管理体系：登记具有唯一编码和版本的受控文件，跟踪有效期并通过变更记录实现可追溯性。文件历史不可修改。",
        "qis_alta" to "登记受控文件",
        "qis_alta_hint" to "初始版本始终为 1.0，并以\"登记文件\"记入历史。编码唯一（如 PRO-001）。",
        "qis_btn_alta" to "+ 登记",
        "qis_filtro" to "按状态筛选：",
        "qis_todos" to "全部",
        "qis_vigentes" to "现行",
        "qis_en_revision" to "评审中",
        "qis_obsoletos" to "作废",
        "qis_doc_count" to "{n} 个文件",
        "qis_ver" to "查看", "qis_descargar" to "下载", "qis_editar" to "编辑",
        "qis_historial" to "历史",
        "qis_estado" to "状态：",
        "qis_cambios" to "变更记录（{n}）",
        "qis_sin_cambios" to "暂无变更记录。",
        "qis_nueva_version" to "登记新版本（当前：{v}）",
        "qis_btn_cambio" to "登记变更",
        "qis_alta_ok" to "文件已按版本 1.0 登记。",
        "qis_alta_falta" to "编码、标题、发布日期和编制人为必填项。",
        "qis_cambio_falta" to "版本、说明和变更人为必填项。",
        "qis_ph_codigo" to "编码 *（PRO-001）",
        "qis_ph_titulo" to "标题 *",
        "qis_ph_proceso" to "工序 / 区域",
        "qis_ph_emision" to "发布日期（日/月/年）*",
        "qis_ph_revision" to "下次评审（日/月/年）",
        "qis_ph_elaboro" to "编制人 *",
        "qis_ph_aprobo" to "批准人",
        "qis_ph_url" to "Drive 中的 PDF 网址（https://...）",
        "qis_ph_notas" to "备注",
        "qis_ph_nueva_version" to "新版本（N.N）*",
        "qis_ph_fecha" to "日期（日/月/年）",
        "qis_ph_desc" to "变更说明 *",
        "qis_ph_realizo" to "变更人 *",
        "qis_ph_nueva_url" to "新 PDF 网址（可选）",
        // ===== Calendario =====
        "cal_titulo" to "到期日历",
        "cal_intro" to "法律义务、培训、演练、行动、内部审核和承包商。点击某一天可添加自己的事件。关键许可以红色显示。",
        "cal_hoy" to "今天",
        "cal_outlook" to "⤓ Outlook (.ics)",
        "cal_cargando" to "正在加载日历...",
        "cal_mas" to "+{n} 更多",
        "cal_eventos_dia" to "{fecha} 的事件",
        "cal_cerrar" to "关闭",
        "cal_tareas" to "{n} 项任务",
        "cal_borrar" to "删除",
        "cal_sin_eventos" to "当天暂无事件。",
        "cal_agregar" to "+ 添加事件",
        "cal_detalles" to "{mes} 详情",
        "cal_si" to "⚠ 是",
        "cal_ph_titulo" to "事件标题 *",
        "cal_ph_detalle" to "详情",
        "cal_ph_resp" to "负责人",
        // ===== Planes de accion =====
        "act_titulo" to "EHS 行动计划",
        "act_intro" to "跟踪发现项、事故和义务。关闭需要 HTTPS 证据链接。",
        "act_vacio" to "暂无行动计划。",
        "act_en_progreso" to "进行中",
        "act_cerrar" to "凭证据关闭",
        "act_cerrada" to "已于 {fecha} 关闭。证据：{url}",
        "act_ph_accion" to "纠正措施 *",
        "act_ph_resp" to "负责人 *",
        "act_ph_limite" to "截止日期 年-月-日 *",
        "act_ph_origen" to "来源 ID *",
        "act_ph_evidencia" to "用于关闭的 HTTPS 证据网址",
        // ===== Contratistas =====
        "con_titulo" to "承包商与供应商",
        "con_intro" to "按公司和工地建立档案。批准档案不代表授予现场出入权限，也不替代安全验证。",
        "con_crear" to "创建档案",
        "con_vacio" to "暂无公司登记。",
        "con_guardar" to "保存审核",
        "con_ph_empresa" to "公司 *",
        "con_ph_actividad" to "活动 *",
        "con_ph_centro" to "工地 *",
        "con_ph_resp" to "内部负责人 *",
        "con_ph_url" to "HTTPS 文件网址",
        "con_ph_vigencia" to "有效期 年-月-日",
        // ===== Indicadores =====
        "met_titulo" to "EHS 指标",
        "met_intro" to "由当前记录统计得出，非官方事故率。",
        "met_cargando" to "正在加载指标...",
        "met_calidad_datos" to "数据质量：{a} 起事故缺少有效损失天数，{b} 项培训缺少有效的下次日期。",
        "met_if" to "频率指数（IF）",
        "met_ig" to "严重度指数（IG）",
        "met_cumplimiento" to "法律合规",
        "met_aplicables" to "矩阵中 {n} 项适用义务",
        "met_permisos" to "关键许可",
        "met_criticos" to "{v} 项过期 · {p} 项即将到期",
        "met_incidentes_mes" to "每月事故（近 12 个月）",
        "met_dias_perdidos" to "期间含 {n} 个损失工作日。若曲线为空，请补录事故日期。",
        // ===== Tasas =====
        "rate_titulo" to "内部事故率",
        "rate_intro" to "按月录入实际工时和经确认的可记录事故。频率 = 事故 × 1,000,000 / 工时；严重度 = 损失天数 × 1,000,000 / 工时。非官方数据，也不自动来自预工资。",
        "rate_guardar" to "保存期间",
        "rate_sin_horas" to "暂无已确认工时，尚无法计算事故率。",
        "rate_horas" to "工时：{h} | 可记录事故：{a} | 损失天数：{d}",
        "rate_frecuencia" to "频率：每百万工时 {f} | 严重度：每百万工时 {g} 天",
        "rate_ph_periodo" to "期间 年-月 *",
        "rate_ph_horas" to "实际工时 *",
        "rate_ph_accidentes" to "可记录事故 *",
        "rate_ph_dias" to "损失天数 *",
        "rate_ph_fuente" to "工时来源 *",
        "rate_ph_validado" to "确认人 *",
        "rate_ph_motivo" to "修订原因（如更正期间）",
        // ===== Centro EHS =====
        "home_titulo" to "EHS 与合规中心",
        "home_intro" to "安全、健康、环境与文件控制一体化运行。",
        "home_pilares" to "按支柱运行",
        "home_gestion" to "管理与合规",
        "home_matriz_nota" to "矩阵是跟踪工具，本身不决定法律合规性。",
        "home_evidencia" to "文件证据",
        "home_evidencia_intro" to "Google Drive 中的文件、元数据及与内部审核、演练和培训的关联。",
        "home_evidencia_permiso" to "仅安全人员可上传或管理文件证据。",
        // ===== Avisos =====
        "alert_titulo" to "EHS 到期提醒",
        "alert_intro" to "应用内提醒，针对已过期或 30 天内到期的记录。未发送邮件。",
        "alert_cargando" to "正在加载提醒...",
        "alert_vacio" to "未来 30 天内无到期记录。",
        "alert_criticos" to "⚠ {n} 项关键许可需立即处理（有停业或罚款风险）。",
        "bell_vacio" to "近期无过期或即将到期的项目。",
        "bell_ver_todos" to "查看全部提醒 →",
        "qis_h_codigo" to "编码",
        "qis_h_titulo" to "标题",
        "qis_h_tipo" to "类型",
        "qis_h_proceso" to "工序",
        "qis_h_version" to "版本",
        "qis_h_estado" to "状态",
        "qis_h_emision" to "发布",
        "qis_h_prox" to "下次评审",
        "qis_h_pdf" to "PDF",
        "qis_h_cambios" to "变更",
        "qis_h_fecha" to "日期",
        "qis_h_desc" to "说明",
        "qis_h_cambio_por" to "变更人",
        "qis_h_aprobo" to "批准人",
        "qis_estado_vigente" to "现行",
        "qis_estado_revision" to "评审中",
        "qis_estado_obsoleto" to "作废",
        "rate_version" to "修订",
        "qis_confirm_borrar" to "删除文件 {c}？其历史也将一并删除。正式停用请使用\"作废\"。",
        "qis_archivo" to "文档文件：",
        "qis_archivo_hint" to "PDF、Word 或 Excel。保存至 Google Drive（QIS/年份）。",
        "qis_subir_archivo" to "上传/更新 Drive 文件：",
        "qis_archivo_ok" to "文件已保存到 Drive。",
        "qis_archivo_fallo" to "无法将文件上传到 Drive",
        "qis_import_listado" to "导入主清单（Excel）",
        "qis_import_hint" to "上传受控文档 Excel，系统自动识别列（代码、标题、类型、流程、版本、日期、编制、批准）。已存在的代码将跳过，Excel 会备份到 Drive。",
        "qis_import_btn" to "导入",
        "qis_import_ok" to "导入完成：新登记 {n} 份，跳过 {d} 份（已存在）。",
        "qis_import_sin_archivo" to "请先选择 Excel 文件。",
        "qis_import_error" to "无法导入清单",
        "qis_import_drive_error" to "（Drive 备份失败，但数据已登记。）",
        "cal_titulo_obligatorio" to "标题为必填项。",
        "cal_exportar_error" to "导出失败：{e}",
        "gts_trade" to "外贸 (GTS)"
    )

    fun get(key: String): String {
        // Si la clave falta en el idioma activo, cae al espanol antes de
        // mostrar la clave cruda: nunca mas "recepcion_mp" en pantalla.
        return when(lang) {
            Language.ES -> es[key] ?: key
            Language.EN -> en[key] ?: es[key] ?: key
            Language.ZH -> zh[key] ?: es[key] ?: key
        }
    }
}

/**
 * Traduccion global: los modulos llaman I18n.get("clave") sin recibir `t` por
 * parametro. Main lo sincroniza en cada recomposicion con el idioma activo.
 */
object I18n {
    var t: Translations = Translations(Language.ES)
    fun get(key: String) = t.get(key)
}

enum class Module {
    DASHBOARD, EHS_HOME, EHS_SEGURIDAD, EHS_SALUD, EHS_AMBIENTE, EHS_METRICS, EHS_CALENDAR, EHS_ACTIONS, EHS_CONTRACTORS, EHS_ALERTS, EHS_RATES, LEGAL_MATRIX, STPS, EHS_DOCUMENTS, GRC_SECURITY,
    CONTROLLING, PURCHASING, PRODUCTION, QUALITY, GTS_TRADE,
    FINANCIAL_ACCOUNTING, PLANT_MAINTENANCE, RECRUITMENT_SAP, EMPLOYEES, ATTENDANCE, PRE_NOMINA,
    WAREHOUSE, SHIPPING, FERRETERIA, RECEPCION_MP, SETTINGS, USER_MGMT, QIS_DOCS, LOCKERS
}

enum class UserRole { ADMIN, RH, COMPRAS, MANTENIMIENTO, SEGURIDAD, EMPLEADO, ALMACEN, IMPORT_EXPORT, FINANZAS }

fun isModuleVisible(module: Module, role: UserRole): Boolean {
    if (role == UserRole.ADMIN) return true
    // RH: solo su modulo de Recursos Humanos.
    if (role == UserRole.RH) return module in listOf(Module.DASHBOARD, Module.RECRUITMENT_SAP, Module.EMPLOYEES, Module.ATTENDANCE, Module.PRE_NOMINA, Module.LOCKERS, Module.SETTINGS)
    // Almacen / Import-Export / Finanzas: cadena de suministro y logistica,
    // nada de EHS (EHS es solo para ADMIN y SEGURIDAD).
    if (role == UserRole.ALMACEN) return module in listOf(Module.DASHBOARD, Module.WAREHOUSE, Module.FERRETERIA, Module.RECEPCION_MP, Module.QIS_DOCS, Module.SETTINGS)
    if (role == UserRole.IMPORT_EXPORT) return module in listOf(Module.DASHBOARD, Module.WAREHOUSE, Module.SHIPPING, Module.FERRETERIA, Module.RECEPCION_MP, Module.QIS_DOCS, Module.SETTINGS)
    if (role == UserRole.FINANZAS) return module in listOf(Module.DASHBOARD, Module.FINANCIAL_ACCOUNTING, Module.WAREHOUSE, Module.SHIPPING, Module.FERRETERIA, Module.RECEPCION_MP, Module.QIS_DOCS, Module.SETTINGS)
    if (role == UserRole.SEGURIDAD) return module in listOf(Module.DASHBOARD, Module.EHS_HOME, Module.EHS_SEGURIDAD, Module.EHS_SALUD, Module.EHS_AMBIENTE, Module.EHS_METRICS, Module.EHS_CALENDAR, Module.EHS_ACTIONS, Module.EHS_CONTRACTORS, Module.EHS_ALERTS, Module.EHS_RATES, Module.LEGAL_MATRIX, Module.STPS, Module.EHS_DOCUMENTS, Module.LOCKERS, Module.QIS_DOCS, Module.SETTINGS)
    return module in listOf(Module.DASHBOARD, Module.EHS_HOME, Module.EHS_SEGURIDAD, Module.EHS_SALUD, Module.EHS_AMBIENTE, Module.EHS_DOCUMENTS, Module.QIS_DOCS, Module.SETTINGS)
}

// CONFIGURACIÓN DE URL DE BACKEND
// Apuntamos directamente al servidor de Hugging Face para que funcione desde nafconnect.pages.dev
val BACKEND_URL = "https://d4r005-rhnaf-industrial.hf.space"

// Token de la sesión actual. DefaultRequest lo agrega automáticamente a todas
// las llamadas posteriores al login, incluidos los módulos que comparten el cliente.
var apiAuthToken = ""

fun main() {
    val client = HttpClient(Js) {
        install(ContentNegotiation) { json() }
        defaultRequest {
            if (apiAuthToken.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiAuthToken")
            }
        }
    }

    renderComposable(rootElementId = "root") {
        var isLoggedIn by remember { mutableStateOf(false) }
        var userRole by remember { mutableStateOf(UserRole.EMPLEADO) }
        var activeModule by remember { mutableStateOf(Module.DASHBOARD) }
        
        var employees by remember { mutableStateOf(emptyList<Employee>()) }
        var userName by remember { mutableStateOf(window.localStorage.getItem("naf_user_name") ?: "Dario Robles") }
        var userAvatar by remember { mutableStateOf(window.localStorage.getItem("naf_user_avatar") ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix") }
        var currentLang by remember { mutableStateOf(Language.valueOf(window.localStorage.getItem("naf_lang") ?: "ES")) }
        val t = Translations(currentLang)
        I18n.t = t

        var authToken by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        if (!isLoggedIn) {
            var loginEnProceso by remember { mutableStateOf(false) }
            LoginScreen(t, loginEnProceso) { u, p, rememberMe ->
                loginEnProceso = true
                scope.launch {
                    try {
                        val resp = client.post("$BACKEND_URL/api/login") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("username" to u, "password" to p))
                        }
                        if (resp.status == HttpStatusCode.OK) {
                            // El servidor es la fuente de verdad del rol/nombre (viene de la tabla
                            // de usuarios real). Ya NO adivinamos el rol aquí con una lista fija de
                            // correos: eso causaba que cualquier cuenta nueva (Carlos, Andrea,
                            // Liliana, etc.) cayera siempre en el rol EMPLEADO por defecto.
                            val body: Map<String, String> = resp.body()
                            val roleFromServer = body["role"] ?: "EMPLEADO"
                            val name = body["name"] ?: "Colaborador"
                            val role = try { UserRole.valueOf(roleFromServer) } catch (e: Exception) { UserRole.EMPLEADO }
                            userRole = role
                            authToken = body["token"] ?: ""
                            apiAuthToken = authToken

                            // Siempre sobreescribimos el nombre mostrado con el que regresa el
                            // servidor para esta cuenta (evita que quede pegado el nombre de otra
                            // persona que inició sesión antes en el mismo navegador).
                            userName = name
                            window.localStorage.setItem("naf_user_name", name)

                            if (rememberMe) {
                                window.localStorage.setItem("naf_saved_email", u)
                            } else {
                                window.localStorage.removeItem("naf_saved_email")
                            }
                            employees = client.get("$BACKEND_URL/api/employees").body()
                            isLoggedIn = true
                        } else {
                            loginEnProceso = false
                            window.alert("El servidor respondió con error ${resp.status}. Es probable que el Space de Hugging Face no tenga este usuario registrado o esté corriendo la App de Inspección de Vehículos en lugar de RHNAF.")
                        }
                    } catch (e: Exception) { 
                        loginEnProceso = false
                        window.alert("Error de conexión: No se pudo alcanzar el servidor en $BACKEND_URL")
                    }
                }
            }
        } else {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    height(100.vh)
                    fontFamily("Inter", "Segoe UI", "sans-serif")
                    backgroundColor(BackgroundColor)
                }
            }) {
                // SIDEBAR
                Sidebar(activeModule, t, userRole) { 
                    activeModule = it 
                }

                // CONTENIDO PRINCIPAL
                Div({ style { flex(1); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); overflowY("auto") } }) {
                    TopBar(userName, userRole.name, userAvatar, t) {
                EhsAlertBell(client, userRole == UserRole.ADMIN || userRole == UserRole.SEGURIDAD) { activeModule = Module.EHS_ALERTS }
                // Cerrar sesion: limpia el token y los datos de sesion del navegador.
                Button({
                    style {
                        padding(6.px, 14.px); backgroundColor(Color.white); color(Color("#b91c1c"))
                        property("border", "1px solid #fecaca"); borderRadius(6.px); cursor("pointer")
                        fontSize(12.px); fontWeight("600")
                    }
                    onClick {
                        authToken = ""
                        apiAuthToken = ""
                        window.localStorage.removeItem("naf_user_name")
                        window.localStorage.removeItem("naf_user_avatar")
                        isLoggedIn = false
                        activeModule = Module.DASHBOARD
                        userRole = UserRole.EMPLEADO
                        employees = emptyList()
                    }
                }) { Text(t.get("logout")) }
            }

                    Div({ style { padding(32.px) } }) {
                        when (activeModule) {
                            Module.DASHBOARD -> DashboardView(employees, t)
                            Module.EHS_HOME -> EhsHomeModule(userRole) { activeModule = it }
                            Module.EHS_SEGURIDAD -> EhsAuditsModule(client, scope, t, userRole, EhsPillar.SEGURIDAD)
                            Module.EHS_SALUD -> EhsAuditsModule(client, scope, t, userRole, EhsPillar.SALUD)
                            Module.EHS_AMBIENTE -> EhsAuditsModule(client, scope, t, userRole, EhsPillar.AMBIENTE)
                            Module.EHS_METRICS -> EhsMetricsModule(client, scope, userName, userRole == UserRole.ADMIN || userRole == UserRole.SEGURIDAD)
                            Module.EHS_CALENDAR -> EhsCalendarModule(client, scope)
                            Module.EHS_ACTIONS -> EhsActionModule(client, scope)
                            Module.EHS_CONTRACTORS -> EhsContractorModule(client, scope)
                            Module.EHS_ALERTS -> EhsAlertsModule(client)
                            Module.EHS_RATES -> EhsRateModule(client, scope)
                            Module.EHS_DOCUMENTS -> EhsDocumentsModule(client, scope, userRole)
                            Module.STPS -> LegalMatrixModule(client, scope, fixedCategory = "STPS")
                            Module.LEGAL_MATRIX -> LegalMatrixModule(client, scope)
                            Module.GRC_SECURITY -> GrcSecurityModule(client, scope, t)
                            Module.QIS_DOCS -> QisModule(client, scope, userRole == UserRole.ADMIN)
                            Module.LOCKERS -> LockerModule(client, scope, userRole == UserRole.ADMIN || userRole == UserRole.RH || userRole == UserRole.SEGURIDAD)
                            Module.CONTROLLING -> ControllingModule(client, scope, t)
                            Module.PURCHASING -> PurchasingModule(client, scope, t)
                            Module.PRODUCTION -> ProductionModule(client, scope, t)
                            Module.QUALITY -> QualityModule(client, scope, t)
                            Module.GTS_TRADE -> GtsTradeModule(client, scope, t)
                            Module.FINANCIAL_ACCOUNTING -> FinancialAccountingModule(client, scope, t)
                            Module.PLANT_MAINTENANCE -> PlantMaintenanceModule(client, scope, t)
                            Module.RECRUITMENT_SAP -> RecruitmentSapModule(client, scope, t)
                            Module.EMPLOYEES -> EmployeeModule(employees, client, scope, t, userRole, authToken) { employees = it }
                            Module.ATTENDANCE -> AttendanceModule(client, scope, t)
                            Module.PRE_NOMINA -> PreNominaModule(client, scope, t, userRole)
                            Module.WAREHOUSE -> WarehouseModule(client, scope, t, userRole)
                            Module.SHIPPING -> ShippingModule(client, scope, t, userRole)
                            Module.FERRETERIA -> FerreteriaModule(client, scope, t)
                            Module.RECEPCION_MP -> RecepcionMpModule(client, scope, t)
                            Module.USER_MGMT -> UserMgmtModule(client, scope, t, authToken)
                            Module.SETTINGS -> {
                                // Foto real del empleado (sincronizada de la lectora Hikvision)
                                val empActual = employees.firstOrNull {
                                    ("${it.firstName} ${it.lastName}").equals(userName, ignoreCase = true)
                                } ?: employees.firstOrNull {
                                    userName.isNotBlank() && ("${it.firstName} ${it.lastName}").contains(userName, ignoreCase = true)
                                }
                                SettingsView(userName, userAvatar, currentLang, empActual?.photoUrl, { userName = it }, { userAvatar = it }, {
                                    currentLang = it
                                    window.localStorage.setItem("naf_lang", it.name)
                                }, t, onFichaPhoto = { dataUri ->
                                    val emp = empActual
                                    if (emp != null) scope.launch {
                                        try {
                                            client.post("$BACKEND_URL/api/employee/update") {
                                                contentType(ContentType.Application.Json)
                                                setBody(emp.copy(photoUrl = dataUri))
                                            }
                                            employees = client.get("$BACKEND_URL/api/employees").body()
                                        } catch (e: Exception) { }
                                    }
                                })
                            }
                        }
                    }
                }
                // FLOATING AI ASSISTANT - deshabilitado, estorbaba
                // AiAssistantWidget(t, client, scope)
            }
        }
    }
}

@Composable
fun Sidebar(active: Module, t: Translations, role: UserRole, onSelect: (Module) -> Unit) {
    Nav({
        style {
            width(270.px)
            backgroundColor(SidebarColor)
            color(Color.white)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        Div({ style { padding(28.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
            H2({ style { margin(0.px); fontSize(22.px); color(Color.white) } }) {
                Span({ style { fontWeight("900"); property("font-style", "italic") } }) { Text("NAF") }
                Span({ style { fontWeight("300"); color(Color("#94a3b8")); marginLeft(6.px) } }) { Text("CONNECT") }
            }
        }

        Input(InputType.Text) {
            style { property("margin", "0 20px 20px 20px"); padding(10.px, 14.px); backgroundColor(Color("#1e293b")); property("border", "1px solid #334155"); borderRadius(8.px); color(Color.white); property("outline", "none") }
            placeholder("Buscar...")
        }

        Div({ style { flex(1); overflowY("auto"); padding(0.px, 14.px) } }) {
            if (isModuleVisible(Module.DASHBOARD, role)) SidebarLink(t.get("dashboard"), Module.DASHBOARD, active == Module.DASHBOARD, onSelect)

            SidebarGroup(t.get("group_hr"), active, role, onSelect, listOf(
                Module.EMPLOYEES to t.get("nav_employees"),
                Module.ATTENDANCE to t.get("nav_attendance"),
                Module.PRE_NOMINA to t.get("pre_nomina"),
                Module.LOCKERS to t.get("lockers"),
                Module.RECRUITMENT_SAP to t.get("recruitment")
            ))
            SidebarGroup(t.get("group_ehs"), active, role, onSelect, listOf(
                Module.EHS_HOME to t.get("ehs_home"),
                Module.EHS_SEGURIDAD to t.get("ehs_seguridad"),
                Module.EHS_SALUD to t.get("ehs_salud"),
                Module.EHS_AMBIENTE to t.get("ehs_ambiente"),
                Module.EHS_METRICS to t.get("ehs_metrics"),
                Module.EHS_ACTIONS to t.get("ehs_actions"),
                Module.EHS_CONTRACTORS to t.get("ehs_contractors"),
                Module.EHS_ALERTS to t.get("ehs_alerts"),
                Module.EHS_RATES to t.get("ehs_rates"),
                Module.EHS_DOCUMENTS to t.get("ehs_documents"),
                Module.LOCKERS to t.get("lockers"),
                Module.EHS_CALENDAR to t.get("ehs_calendar"),
                Module.LEGAL_MATRIX to t.get("legal_matrix"),
                Module.STPS to t.get("stps_normas"),
                Module.GRC_SECURITY to t.get("it_security_grc")
            ))
            SidebarGroup(t.get("group_calidad"), active, role, onSelect, listOf(
                Module.QIS_DOCS to t.get("qis_docs")
            ))
            SidebarGroup(t.get("group_ops"), active, role, onSelect, listOf(
                Module.PRODUCTION to t.get("production_planning"),
                Module.QUALITY to t.get("quality_management"),
                Module.PLANT_MAINTENANCE to t.get("plant_maintenance")
            ))
            SidebarGroup(t.get("group_supply"), active, role, onSelect, listOf(
                Module.PURCHASING to t.get("purchasing"),
                Module.WAREHOUSE to t.get("warehouse"),
                Module.FERRETERIA to t.get("ferreteria"),
                Module.RECEPCION_MP to t.get("recepcion_mp"),
                Module.SHIPPING to t.get("shipping"),
                Module.GTS_TRADE to t.get("gts_trade")
            ))
            SidebarGroup(t.get("group_fin"), active, role, onSelect, listOf(
                Module.CONTROLLING to t.get("controlling"),
                Module.FINANCIAL_ACCOUNTING to t.get("financial_accounting")
            ))

            // Gestion de usuarios: FUERA de RH. Solo visible para ADMIN (isModuleVisible).
            SidebarGroup(t.get("group_admin"), active, role, onSelect, listOf(
                Module.USER_MGMT to t.get("user_mgmt")
            ))

            if (isModuleVisible(Module.SETTINGS, role)) SidebarLink(t.get("settings"), Module.SETTINGS, active == Module.SETTINGS, onSelect)
        }

        Div({ style { padding(20.px); property("border-top", "1px solid #1e293b") } }) {
            P({ style { fontSize(11.px); color(Color("#64748b")); textAlign("center") } }) { Text("NAF CONNECT v3.1") }
        }
    }
}

@Composable
fun SidebarGroup(title: String, active: Module, role: UserRole, onSelect: (Module) -> Unit, entries: List<Pair<Module, String>>) {
    val visibleEntries = entries.filter { isModuleVisible(it.first, role) }
    if (visibleEntries.isEmpty()) return
    var expanded by remember { mutableStateOf(visibleEntries.any { it.first == active }) }

    Div({ style { marginTop(10.px); marginBottom(6.px) } }) {
        Div({
            style { padding(9.px, 12.px); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); cursor("pointer"); color(Color("#94a3b8")); fontSize(11.px); fontWeight("700"); property("letter-spacing", "0.6px") }
            onClick { expanded = !expanded }
        }) {
            Text(title)
            Span { Text(if (expanded) "▾" else "▸") }
        }
        if (expanded) visibleEntries.forEach { (module, label) ->
            SidebarLink(label, module, active == module, onSelect)
        }
    }
}

@Composable
fun SidebarLink(label: String, mod: Module, isSelected: Boolean, onSelect: (Module) -> Unit) {
    Div({
        style {
            padding(10.px, 16.px)
            marginBottom(4.px)
            borderRadius(8.px)
            cursor("pointer")
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            gap(12.px)
            if (isSelected) backgroundColor(SidebarActiveColor) else backgroundColor(Color.transparent)
            property("transition", "all 0.2s")
        }
        onClick { onSelect(mod) }
    }) {
        Div({ style { width(18.px); height(18.px); backgroundColor(if (isSelected) Color.white else Color("#94a3b8")); borderRadius(4.px) } })
        Text(label)
    }
}

@Composable
fun DashboardView(employees: List<Employee>, t: Translations) {
    val totalEmployees = employees.size
    val activeEmployees = employees.count { it.status == EmployeeStatus.ACTIVE }
    val activePercent = if (totalEmployees > 0) (activeEmployees.toDouble() / totalEmployees * 100).toInt() else 0
    val highRiskCount = employees.count { it.attritionRisk > 0.7 }

    Div {
        // TOP CARDS
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(200.px, 1fr))")
                gap(20.px)
                property("margin-bottom", "24px")
            }
        }) {
            StatCard(t.get("total_emp"), "$totalEmployees", "Base de datos NAF", Color("#6366f1"))
            StatCard(t.get("active_emp"), "$activeEmployees", "$activePercent% del total", Color("#22c55e"))
            StatCard(t.get("turnover_risk"), "$highRiskCount", "${t.get("predictive")}: Crítico", if(highRiskCount > 0) Color("#ef4444") else Color("#22c55e"))
            StatCard(t.get("training_pending"), "2", "Próximos cursos", Color("#a855f7"))
            StatCard(t.get("incidents_today"), "0", "Sin reportes críticos", Color("#22c55e"))
        }

        // MIDDLE SECTION: AI INSIGHTS + WIDGETS
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "2fr 1fr 1fr")
                gap(24.px)
            }
        }) {
            // Skill Heatmap AI
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); property("margin-bottom", "20px") } }) {
                    H3({ style { margin(0.px); fontSize(16.px) } }) { Text(t.get("skill_heatmap") + " (AI)") }
                    Span({ style { color(Color.gray); fontSize(12.px) } }) { Text("Análisis de Talentos") }
                }
                Div({ style { height(200.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); padding(20.px); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); gap(12.px) } }) {
                    SkillBar("Operación de Planta", "92%", Color("#3b82f6"))
                    SkillBar("Seguridad EHS", "78%", Color("#ef4444"))
                    SkillBar("Mantenimiento Técnico", "45%", Color("#f59e0b"))
                    SkillBar("Liderazgo", "30%", Color("#10b981"))
                }
                P({ style { fontSize(11.px); color(Color.gray); marginTop(12.px) } }) { 
                    Text("Nota: Basado en certificaciones y desempeño histórico analizado por IA.") 
                }
            }

            // Birthdays
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                H3({ style { property("margin", "0 0 20px 0"); fontSize(16.px) } }) { Text("Cumpleaños del Mes") }
                BirthdayItem("Carlos Rodríguez", "15 de Mayo", true)
                BirthdayItem("María González", "18 de Mayo", false)
                BirthdayItem("Juan Pérez", "22 de Mayo", false)
                BirthdayItem("Ana López", "28 de Mayo", false)
            }

            // Important Alerts (Predictive)
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                H3({ style { property("margin", "0 0 20px 0"); fontSize(16.px) } }) { Text("Alertas Predictivas") }
                AlertItem("3 empleados con alta probabilidad de renuncia", Color("#ef4444"))
                AlertItem("Gap de habilidades detectado en Producción", Color("#f97316"))
                AlertItem("Certificación EHS vence en 3 días (5 pers.)", Color("#3b82f6"))
                AlertItem("Anomalía en registros de asistencia", Color("#f59e0b"))
            }
        }
    }
}

@Composable
fun SkillBar(label: String, prog: String, color: CSSColorValue) {
    Div {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); marginBottom(4.px) } }) {
            Span({ style { fontSize(12.px); fontWeight("bold") } }) { Text(label) }
            Span({ style { fontSize(11.px); color(Color.gray) } }) { Text(prog) }
        }
        Div({ style { height(6.px); width(100.percent); backgroundColor(Color("#e2e8f0")); borderRadius(3.px) } }) {
            Div({ style { height(100.percent); property("width", prog); backgroundColor(color); borderRadius(3.px) } })
        }
    }
}

@Composable
fun StatCard(label: String, value: String, sub: String, color: CSSColorValue) {
    Div({
        style {
            backgroundColor(Color.white); padding(20.px); borderRadius(12.px)
            property("box-shadow", CardShadow)
            display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
        }
    }) {
        Div {
            P({ style { margin(0.px); color(Color("#64728b")); fontSize(14.px) } }) { Text(label) }
            H2({ style { property("margin", "4px 0"); fontSize(24.px); fontWeight("bold") } }) { Text(value) }
            P({ style { margin(0.px); color(color); fontSize(12.px); fontWeight("500") } }) { Text(sub) }
        }
        Div({ style { width(48.px); height(48.px); backgroundColor(Color("#f1f5f9")); borderRadius(12.px) } })
    }
}

@Composable
fun MiniStat(label: String, value: String, sub: String, color: CSSColorValue) {
    Div {
        P({ style { margin(0.px); color(Color("#64728b")); fontSize(11.px) } }) { Text(label) }
        P({ style { property("margin", "2px 0"); fontSize(16.px); fontWeight("bold") } }) { Text(value) }
        P({ style { margin(0.px); color(color); fontSize(10.px) } }) { Text(sub) }
    }
}

@Composable
fun BirthdayItem(name: String, date: String, isToday: Boolean) {
    Div({
        style {
            display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px); marginBottom(16.px)
        }
    }) {
        Div({ style { width(36.px); height(36.px); backgroundColor(Color("#e2e8f0")); borderRadius(50.percent) } })
        Div({ style { flex(1) } }) {
            P({ style { margin(0.px); fontSize(14.px); fontWeight("500") } }) { Text(name) }
            P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text(date) }
        }
        if (isToday) {
            Span({ style { fontSize(10.px); backgroundColor(Color("#dbeafe")); color(Color("#1e40af")); padding(2.px, 6.px); borderRadius(4.px); fontWeight("bold") } }) { Text("Hoy") }
        }
    }
}

@Composable
fun AlertItem(text: String, color: CSSColorValue) {
    Div({
        style {
            display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px); alignItems(AlignItems.FlexStart)
        }
    }) {
        Div({ style { width(20.px); height(20.px); borderRadius(50.percent); property("border", "2px solid $color"); property("flex-shrink", "0") } })
        Div {
            P({ style { margin(0.px); fontSize(13.px); fontWeight("500") } }) { Text(text) }
            A(href = "#", { style { fontSize(11.px); color(SidebarActiveColor); textDecoration("none") } }) { Text("Ver detalles") }
        }
    }
}

@Composable
fun TopBar(user: String, role: String, avatarUrl: String, t: Translations, trailing: @Composable () -> Unit = {}) {
    Header({
        style {
            backgroundColor(Color.white); padding(12.px, 24.px); display(DisplayStyle.Flex)
            justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
            property("border-bottom", "1px solid #e2e8f0")
        }
    }) {
        Div {
            H2({ style { margin(0.px); fontSize(16.px); fontWeight("bold") } }) { Text("Panel NAF CONNECT") }
            P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text("Gestión Industrial de Talento") }
        }
        Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(20.px) } }) {
            trailing()
            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px) } }) {
                Div({ style { textAlign("right") } }) {
                    P({ style { margin(0.px); fontSize(14.px); fontWeight("600") } }) { Text(user) }
                    P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text(role) }
                }
                Div({ style { position(Position.Relative) } }) {
                    Img(src = avatarUrl) {
                        style { width(36.px); height(36.px); borderRadius(50.percent); property("object-fit", "cover"); backgroundColor(Color("#cbd5e1")) }
                    }
                    // Punto de estado Online
                    Div({
                        style {
                            width(10.px); height(10.px); backgroundColor(Color("#22c55e"))
                            borderRadius(50.percent); position(Position.Absolute); bottom(0.px); right(0.px)
                            property("border", "2px solid white")
                        }
                    })
                }
            }
        }
    }
}

private fun parseCSV(text: String): List<Employee> {
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.size <= 1) return emptyList()
    
    val firstLine = lines[0]
    val delimiter = if (firstLine.contains("\t")) "\t" else if (firstLine.contains(";")) ";" else ","
    
    return lines.drop(1).mapNotNull { line ->
        try {
            val parts = line.split(delimiter)
            if (parts.size < 2) return@mapNotNull null
            
            val id = parts.getOrNull(0)?.trim() ?: ""
            val fullName = parts.getOrNull(1)?.trim() ?: ""
            
            // Intentamos separar nombres de apellidos (Aproximación simple)
            val nameParts = fullName.split(" ")
            val fName = if (nameParts.size >= 3) nameParts.drop(2).joinToString(" ") else nameParts.lastOrNull() ?: ""
            val lName = if (nameParts.size >= 3) nameParts.take(2).joinToString(" ") else nameParts.dropLast(1).joinToString(" ")
            
            Employee(
                id = id,
                firstName = fName.ifEmpty { fullName },
                lastName = lName,
                rfc = parts.getOrNull(2)?.trim(),
                curp = parts.getOrNull(3)?.trim(),
                nss = parts.getOrNull(4)?.trim(),
                position = parts.getOrNull(6)?.trim() ?: "Operativo",
                department = "Producción", 
                entryDate = parts.getOrNull(8)?.trim() ?: parts.getOrNull(7)?.trim() ?: "2024-01-01",
                status = EmployeeStatus.ACTIVE,
                readerId = id
            )
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun StatusBadge(s: EmployeeStatus) {
    val (bg, txt) = when(s) {
        EmployeeStatus.ACTIVE -> Color("#dcfce7") to Color("#166534")
        EmployeeStatus.VACATION -> Color("#fef9c3") to Color("#854d0e")
        else -> Color("#fee2e2") to Color("#991b1b")
    }
    Span({
        style {
            padding(4.px, 12.px); borderRadius(20.px); color(txt); backgroundColor(bg); fontSize(11.px); fontWeight("bold")
        }
    }) { Text(s.name) }
}

@Composable
fun LoginScreen(t: Translations, cargando: Boolean = false, onLogin: (String, String, Boolean) -> Unit) {
    var u by remember { mutableStateOf(window.localStorage.getItem("naf_saved_email") ?: "") }
    var p by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(u.isNotEmpty()) }

    Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); height(100.vh); backgroundColor(SidebarColor) } }) {
        Div({ style { backgroundColor(Color.white); padding(48.px); borderRadius(24.px); width(400.px); property("box-shadow", "0 25px 50px -12px rgba(0, 0, 0, 0.5)") } }) {
            Div({ style { textAlign("center"); marginBottom(40.px) } }) {
                // LOGO NAF CONNECT (Réplica exacta del logo proporcionado)
                H1({ style { margin(0.px); fontSize(42.px); fontFamily("Inter", "sans-serif"); letterSpacing((-1).px) } }) { 
                    Span({ style { fontWeight("900"); property("font-style", "italic"); color(Color("#0f172a")) } }) { Text("NAF") }
                    Span({ style { fontWeight("300"); color(Color("#475569")); marginLeft(8.px); property("font-style", "normal") } }) { Text("CONNECT") }
                }
                P({ style { color(Color("#64728b")); marginTop(4.px); fontSize(14.px); letterSpacing(2.px); fontWeight("500") } }) { Text("PORTAL DE GESTIÓN") }
            }
            
            Label(attrs = { style { fontSize(12.px); fontWeight("600"); color(Color("#475569")); letterSpacing(0.5.px) } }) { Text(t.get("email")) }
            Input(InputType.Text) { 
                value(u)
                placeholder("usuario@dominio.com")
                style { width(100.percent); padding(12.px); property("margin", "8px 0 20px 0"); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box"); property("outline", "none"); backgroundColor(Color("#f8fafc")) }
                onInput { u = it.value } 
            }
            
            Label(attrs = { style { fontSize(12.px); fontWeight("600"); color(Color("#475569")); letterSpacing(0.5.px) } }) { Text(t.get("password")) }
            Input(InputType.Password) { 
                value(p)
                placeholder("••••••••")
                style { width(100.percent); padding(12.px); property("margin", "8px 0 20px 0"); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box"); property("outline", "none"); backgroundColor(Color("#f8fafc")) }
                onInput { p = it.value } 
            }

            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(8.px); marginBottom(20.px) } }) {
                Input(InputType.Checkbox) {
                    checked(rememberMe)
                    onInput { rememberMe = it.value }
                    style { cursor("pointer") }
                }
                Label(attrs = {
                    style { fontSize(13.px); color(Color("#64728b")); cursor("pointer") }
                }) {
                    Text(t.get("remember_me"))
                }
            }
            
            Button({ 
                if (cargando) attr("disabled", "true")
                style { 
                    width(100.percent); padding(16.px); backgroundColor(if (cargando) Color("#475569") else Color("#0f172a")); color(Color.white)
                    property("border", "none"); borderRadius(8.px); cursor(if (cargando) "wait" else "pointer")
                    fontWeight("bold"); fontSize(14.px); property("transition", "all 0.2s")
                    opacity(if (cargando) 0.75 else 1.0)
                }
                onClick { if (!cargando) onLogin(u, p, rememberMe) } 
            }) { Text(if (cargando) t.get("login_wait") else t.get("login")) }
            
            P({ style { textAlign("center"); marginTop(32.px); fontSize(11.px); color(Color("#94a3b8")) } }) { Text("© 2024 NAF CONNECT • SISTEMA INDUSTRIAL") }
        }
    }
}

@Composable
fun SettingsView(name: String, avatar: String, lang: Language, photoUrl: String?, onNameChange: (String) -> Unit, onAvatarChange: (String) -> Unit, onLangChange: (Language) -> Unit, t: Translations, onFichaPhoto: (String) -> Unit = {}) {
    var tempName by remember { mutableStateOf(name) }
    // Foto subida por el propio usuario (personal de oficina sin foto de lectora)
    var fotoSubida by remember { mutableStateOf<String?>(null) }
    // Foto real del personal; el avatar solo es respaldo si no hay foto registrada
    val fotoPerfil = fotoSubida ?: photoUrl?.takeIf { it.isNotBlank() } ?: avatar
    var tempLang by remember { mutableStateOf(lang) }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(16.px); property("box-shadow", CardShadow) } }) {
        H2 { Text(t.get("profile_settings")) }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Personalice su identidad en el portal NAF CONNECT.") }
        
        Div({ style { display(DisplayStyle.Flex); gap(40.px); alignItems(AlignItems.Center) } }) {
            // Avatar Actual
            Div({ style { textAlign("center") } }) {
                Img(src = fotoPerfil) {
                    style { width(120.px); height(120.px); borderRadius(50.percent); property("border", "4px solid $SidebarActiveColor"); marginBottom(16.px) }
                }
                P({ style { fontWeight("bold"); margin(0.px) } }) { Text(tempName) }
            }
            
            // Selector de Avatares
            Div({ style { flex(1) } }) {
                H4 { Text(t.get("employee_photo")) }
                Div({ style { display(DisplayStyle.Flex); gap(16.px); alignItems(AlignItems.Center); padding(12.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); property("border", "1px solid #e2e8f0") } }) {
                    if (fotoSubida != null) {
                        Img(src = fotoSubida!!) {
                            style { width(72.px); height(72.px); borderRadius(8.px); property("object-fit", "cover"); property("border", "2px solid $SidebarActiveColor") }
                        }
                        P({ style { color(Color.gray); fontSize(13.px); margin(0.px) } }) {
                            Text("Nueva foto lista. Presiona Guardar para aplicarla a tu perfil y tu ficha de empleado.")
                        }
                    } else if (photoUrl.isNullOrBlank()) {
                        P({ style { color(Color.gray); fontSize(13.px); margin(0.px, 0.px, 8.px, 0.px) } }) {
                            Text("Sin foto registrada para este usuario (la foto de lectora solo existe para personal de planta). Puedes subir tu propia foto:")
                        }
                        Input(InputType.File) {
                            attr("accept", "image/*")
                            style { fontSize(13.px) }
                            onChange { ev ->
                                val file = ev.target.files?.item(0) ?: return@onChange
                                val reader = js("new FileReader()")
                                reader.onload = { _: dynamic ->
                                    val img = js("new Image()")
                                    img.onload = { _: dynamic ->
                                        val doc = window.document.asDynamic()
                                        val canvas = doc.createElement("canvas")
                                        val maxD = window.asDynamic().Math.max(img.width, img.height) as Double
                                        val escala = window.asDynamic().Math.min(1.0, 320.0 / maxD)
                                        canvas.width = window.asDynamic().Math.max(1, img.width * escala)
                                        canvas.height = window.asDynamic().Math.max(1, img.height * escala)
                                        val ctx = canvas.getContext("2d")
                                        ctx.drawImage(img, 0.0, 0.0, canvas.width, canvas.height)
                                        fotoSubida = canvas.toDataURL("image/jpeg", 0.85) as String?
                                    }
                                    img.src = reader.result
                                }
                                reader.readAsDataURL(file)
                            }
                        }
                    } else {
                        Img(src = fotoPerfil) {
                            style { width(72.px); height(72.px); borderRadius(8.px); property("object-fit", "cover"); property("border", "2px solid $SidebarActiveColor") }
                        }
                        P({ style { color(Color.gray); fontSize(13.px); margin(0.px) } }) {
                            Text("Fotografia oficial registrada en la lectora Hikvision.")
                        }
                    }
                }

                H4({ style { marginTop(24.px) } }) { Text(t.get("select_lang")) }
                Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                    Language.entries.forEach { l ->
                        Button({
                            style {
                                padding(8.px, 16.px); borderRadius(6.px); cursor("pointer")
                                backgroundColor(if (l == tempLang) SidebarActiveColor else Color.white)
                                color(if (l == tempLang) Color.white else Color.black)
                                property("border", "1px solid #ddd")
                            }
                            onClick { tempLang = l }
                        }) { Text(l.name) }
                    }
                }
                
                H4({ style { marginTop(24.px) } }) { Text(t.get("display_name")) }
                Input(InputType.Text) {
                    value(tempName)
                    onInput { tempName = it.value }
                    style { width(100.percent); padding(12.px); borderRadius(8.px); property("border", "1px solid #e2e8f0"); marginBottom(24.px) }
                }

                Button({
                    style {
                        padding(12.px, 32.px); backgroundColor(Color("#22c55e")); color(Color.white)
                        property("border", "none"); borderRadius(8.px); cursor("pointer")
                        fontWeight("bold"); fontSize(14.px); width(100.percent)
                    }
                    onClick {
                        onNameChange(tempName)
                        onAvatarChange(fotoPerfil)
                        onLangChange(tempLang)
                        fotoSubida?.let { onFichaPhoto(it) }
                        window.localStorage.setItem("naf_user_name", tempName)
                        window.localStorage.setItem("naf_user_avatar", fotoPerfil)
                        window.localStorage.setItem("naf_lang", tempLang.name)
                        window.alert("Configuración guardada exitosamente.")
                    }
                }) { Text(t.get("save")) }
            }
        }
    }
}

@Composable
fun AiAssistantWidget(t: Translations, client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var isOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }

    Div({
        style {
            position(Position.Fixed); bottom(24.px); right(24.px); property("z-index", "1000")
        }
    }) {
        if (isOpen) {
            Div({
                style {
                    width(350.px); height(450.px); backgroundColor(Color.white); borderRadius(16.px)
                    property("box-shadow", "0 10px 25px -5px rgba(0, 0, 0, 0.2)")
                    display(DisplayStyle.Flex); flexDirection(FlexDirection.Column)
                    marginBottom(16.px); property("border", "1px solid #e2e8f0")
                }
            }) {
                Div({ style { padding(16.px); backgroundColor(Color("#0f172a")); color(Color.white); borderRadius(16.px, 16.px, 0.px, 0.px) } }) {
                    Text(t.get("ai_assistant"))
                }
                Div({ style { flex(1); padding(16.px); overflowY("auto"); fontSize(13.px) } }) {
                    if (response.isEmpty()) {
                        Text("¿En qué puedo ayudarte hoy? Ejemplo: '¿Quién tiene riesgo de renuncia?' o 'Genera reporte de capacitación'.")
                    } else {
                        Div({ style { padding(12.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px) } }) { Text(response) }
                    }
                }
                Div({ style { padding(16.px); property("border-top", "1px solid #e2e8f0") } }) {
                    Input(InputType.Text) {
                        placeholder("Pregunta a NAF AI...")
                        style { width(100.percent); padding(10.px); borderRadius(8.px); property("border", "1px solid #ddd") }
                        onInput { query = it.value }
                        onKeyDown { if (it.key == "Enter") { response = "Analizando datos de la planta..."; window.setTimeout({ response = "Basado en los datos actuales, el índice de rotación ha bajado 2% y Dario Robles tiene certificaciones al día." }, 1500) } }
                    }
                }
            }
        }
        
        Button({
            style {
                width(60.px); height(60.px); borderRadius(50.percent); backgroundColor(Color("#0f172a"))
                color(Color.white); fontSize(24.px); cursor("pointer"); property("border", "none")
                property("box-shadow", "0 4px 6px -1px rgba(0, 0, 0, 0.1)")
            }
            onClick { isOpen = !isOpen }
        }) { Text("✨") }
    }
}
