package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

// ============================================================
// Tablas Exposed de los modulos estilo SAP integrados a RHNAF
// ============================================================

// FI - Financial Accounting (Contabilidad Financiera)
object JournalEntryTable : Table("fi_journal_entries") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val cuenta = varchar("cuenta", 200)
    val concepto = varchar("concepto", 400).default("")
    val tipo = varchar("tipo", 50).default("")
    val monto = varchar("monto", 100).default("")
    val referencia = varchar("referencia", 200).default("")

    override val primaryKey = PrimaryKey(id)
}

// CO - Controlling (Control de Costos)
object CostCenterTable : Table("co_cost_centers") {
    val id = integer("id").autoIncrement()
    val codigo = varchar("codigo", 50)
    val nombre = varchar("nombre", 200)
    val departamento = varchar("departamento", 200).default("")
    val presupuestoMensual = varchar("presupuesto_mensual", 100).default("")
    val gastoActual = varchar("gasto_actual", 100).default("")

    override val primaryKey = PrimaryKey(id)
}

// MM - Materials Management (Compras)
object PurchaseOrderTable : Table("mm_purchase_orders") {
    val id = integer("id").autoIncrement()
    val numero = varchar("numero", 100)
    val proveedor = varchar("proveedor", 200)
    val fecha = varchar("fecha", 50).default("")
    val descripcion = varchar("descripcion", 400).default("")
    val montoTotal = varchar("monto_total", 100).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// PP - Production Planning (Planificación de Producción)
object ProductionOrderTable : Table("pp_production_orders") {
    val id = integer("id").autoIncrement()
    val numero = varchar("numero", 100)
    val producto = varchar("producto", 200)
    val cantidadPlan = varchar("cantidad_plan", 50).default("")
    val cantidadProducida = varchar("cantidad_producida", 50).default("")
    val centroTrabajo = varchar("centro_trabajo", 200).default("")
    val fechaInicio = varchar("fecha_inicio", 50).default("")
    val fechaFin = varchar("fecha_fin", 50).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// QM - Quality Management (Gestión de Calidad)
object QualityInspectionTable : Table("qm_quality_inspections") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val loteProducto = varchar("lote_producto", 200)
    val inspector = varchar("inspector", 200).default("")
    val resultado = varchar("resultado", 50).default("")
    val observaciones = varchar("observaciones", 400).default("")

    override val primaryKey = PrimaryKey(id)
}

// PM - Plant Maintenance (Mantenimiento de Planta)
object MaintenanceOrderTable : Table("pm_maintenance_orders") {
    val id = integer("id").autoIncrement()
    val equipo = varchar("equipo", 200)
    val tipo = varchar("tipo", 50).default("")
    val fechaProgramada = varchar("fecha_programada", 50).default("")
    val fechaRealizada = varchar("fecha_realizada", 50).default("")
    val tecnico = varchar("tecnico", 200).default("")
    val estado = varchar("estado", 50).default("")
    val notas = varchar("notas", 400).default("")

    override val primaryKey = PrimaryKey(id)
}

// EWM - Extended Warehouse Management (Gestión Avanzada de Almacenes)
object WarehouseTaskTable : Table("ewm_warehouse_tasks") {
    val id = integer("id").autoIncrement()
    val tipo = varchar("tipo", 50)
    val bin = varchar("bin_ubicacion", 100).default("")
    val sku = varchar("sku", 200).default("")
    val cantidad = varchar("cantidad", 50).default("")
    val asignadoA = varchar("asignado_a", 200).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// HCM - Human Capital Management (Reclutamiento)
object RecruitmentVacancyTable : Table("hcm_recruitment_vacancies") {
    val id = integer("id").autoIncrement()
    val puesto = varchar("puesto", 200)
    val departamento = varchar("departamento", 200).default("")
    val fechaApertura = varchar("fecha_apertura", 50).default("")
    val vacantes = varchar("vacantes", 50).default("")
    val candidatosPostulados = varchar("candidatos_postulados", 50).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// GTS - Global Trade Services (Comercio Exterior)
object CustomsDeclarationTable : Table("gts_customs_declarations") {
    val id = integer("id").autoIncrement()
    val numeroPedimento = varchar("numero_pedimento", 100)
    val fecha = varchar("fecha", 50).default("")
    val cliente = varchar("cliente", 200).default("")
    val paisDestino = varchar("pais_destino", 100).default("")
    val valorAduana = varchar("valor_aduana", 100).default("")
    val regimen = varchar("regimen", 100).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// ============================================================
// EHS - Environment, Health & Safety (7 sub-modulos)
// ============================================================

// EHS-1. Inspecciones de Seguridad (mejorada)
object SafetyInspectionTable : Table("ehs_safety_inspections") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val tipoInspeccion = varchar("tipo_inspeccion", 50).default("")
    val area = varchar("area", 200).default("")
    val inspector = varchar("inspector", 200).default("")
    val hallazgos = varchar("hallazgos", 400).default("")
    val riesgo = varchar("riesgo", 50).default("")
    val accionesCorrectivas = varchar("acciones_correctivas", 500).default("")
    val fechaCierre = varchar("fecha_cierre", 50).default("")
    val evidencia = varchar("evidencia", 500).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS-2. Incidentes y Accidentes
object SafetyIncidentTable : Table("ehs_safety_incidents") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val tipo = varchar("tipo", 50).default("")
    val severidad = varchar("severidad", 50).default("")
    val personaAfectada = varchar("persona_afectada", 200).default("")
    val departamento = varchar("departamento", 200).default("")
    val parteCuerpo = varchar("parte_cuerpo", 200).default("")
    val diasPerdidos = varchar("dias_perdidos", 20).default("")
    val descripcion = varchar("descripcion", 500).default("")
    val causaRaiz = varchar("causa_raiz", 500).default("")
    val accionesCorrectivas = varchar("acciones_correctivas", 500).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS-3. Permisos de Trabajo
object WorkPermitTable : Table("ehs_work_permits") {
    val id = integer("id").autoIncrement()
    val tipo = varchar("tipo", 100).default("")
    val solicitante = varchar("solicitante", 200).default("")
    val autorizadoPor = varchar("autorizado_por", 200).default("")
    val fechaInicio = varchar("fecha_inicio", 50).default("")
    val fechaFin = varchar("fecha_fin", 50).default("")
    val area = varchar("area", 200).default("")
    val riesgosIdentificados = varchar("riesgos_identificados", 500).default("")
    val eppRequerido = varchar("epp_requerido", 500).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS-4. Entrega de EPP (Equipo de Proteccion Personal)
object PpeDeliveryTable : Table("ehs_ppe_deliveries") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val empleado = varchar("empleado", 200).default("")
    val tipoEpp = varchar("tipo_epp", 200).default("")
    val talla = varchar("talla", 20).default("")
    val proximaReposicion = varchar("proxima_reposicion", 50).default("")
    val firma = varchar("firma", 200).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS-5. Capacitaciones de Seguridad
object SafetyTrainingTable : Table("ehs_safety_trainings") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val tema = varchar("tema", 300).default("")
    val instructor = varchar("instructor", 200).default("")
    val asistentes = varchar("asistentes", 50).default("")
    val vigenciaMeses = varchar("vigencia_meses", 20).default("")
    val proximaFecha = varchar("proxima_fecha", 50).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS-6. Simulacros de Emergencia
object EmergencyDrillTable : Table("ehs_emergency_drills") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val tipo = varchar("tipo", 100).default("")
    val participantes = varchar("participantes", 50).default("")
    val tiempoEvacuacion = varchar("tiempo_evacuacion", 50).default("")
    val resultado = varchar("resultado", 300).default("")
    val observaciones = varchar("observaciones", 500).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS-7. Matriz de Riesgos / IPER
object RiskMatrixTable : Table("ehs_risk_matrix") {
    val id = integer("id").autoIncrement()
    val area = varchar("area", 200).default("")
    val proceso = varchar("proceso", 200).default("")
    val riesgoIdentificado = varchar("riesgo_identificado", 500).default("")
    val probabilidad = varchar("probabilidad", 50).default("")
    val severidad = varchar("severidad", 50).default("")
    val nivelRiesgo = varchar("nivel_riesgo", 50).default("")
    val controles = varchar("controles", 500).default("")
    val responsable = varchar("responsable", 200).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// GRC - SAP Security / GRC (Gobierno, Riesgo y Cumplimiento)
object AccessAuditLogTable : Table("grc_access_audit_log") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val usuario = varchar("usuario", 200)
    val accion = varchar("accion", 200).default("")
    val modulo = varchar("modulo", 100).default("")
    val resultado = varchar("resultado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// --- EXPANSION EHS (Nivel EHSSoft) ---

// EHS-Ambiente. Gestion de Residuos
object EnvironmentalWasteTable : Table("ehs_environmental_waste") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val residuo = varchar("residuo", 200)
    val tipo = varchar("tipo", 100).default("")
    val cantidad = varchar("cantidad", 50).default("")
    val unidad = varchar("unidad", 20).default("kg")
    val transportista = varchar("transportista", 200).default("")
    val destinoFinal = varchar("destino_final", 200).default("")
    val numeroManifiesto = varchar("numero_manifiesto", 100).default("")
    val estado = varchar("estado", 50).default("Pendiente")

    override val primaryKey = PrimaryKey(id)
}

// EHS-Salud. Vigilancia Medica
object OccupationalHealthTable : Table("ehs_occupational_health") {
    val id = integer("id").autoIncrement()
    val empleadoId = varchar("empleado_id", 50)
    val nombreEmpleado = varchar("nombre_empleado", 200).default("")
    val fecha = varchar("fecha", 50)
    val tipoExamen = varchar("tipo_examen", 100).default("")
    val resultado = varchar("resultado", 100).default("")
    val observaciones = varchar("observaciones", 500).default("")
    val proximaCita = varchar("proxima_cita", 50).default("")
    val medico = varchar("medico", 200).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS-Quimicos. Inventario MSDS
object Dc3ConstanciaTable : Table("ehs_dc3_constancias") {
    val id = integer("id").autoIncrement()
    val trabajador = varchar("trabajador", 200).default("")
    val tema = varchar("tema", 300).default("")
    val fecha = varchar("fecha", 50)
    val horas = varchar("horas", 20).default("")
    val responsable = varchar("responsable", 200).default("")
    val evidenciaUrl = varchar("evidencia_url", 500).default("")

    override val primaryKey = PrimaryKey(id)
}

object EhsCustomEventTable : Table("ehs_custom_events") {
    val id = integer("id").autoIncrement()
    val fecha = varchar("fecha", 50)
    val tipo = varchar("tipo", 50).default("evento")
    val titulo = varchar("titulo", 300).default("")
    val detalle = varchar("detalle", 500).default("")
    val responsable = varchar("responsable", 200).default("")
    val estado = varchar("estado", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

object ChemicalInventoryTable : Table("ehs_chemical_inventory") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 200)
    val fabricante = varchar("fabricante", 200).default("")
    val areaUso = varchar("area_uso", 200).default("")
    val nivelRiesgo = varchar("nivel_riesgo", 50).default("")
    val hojaSeguridadUrl = varchar("hoja_seguridad_url", 500).default("")
    val estado = varchar("estado", 50).default("Activo")
    val ultimaRevision = varchar("ultima_revision", 50).default("")

    override val primaryKey = PrimaryKey(id)
}

// --- INFRAESTRUCTURA DE TAREAS REMOTAS (Puente Nube-Planta) ---
object SystemTaskTable : Table("system_tasks") {
    val id = integer("id").autoIncrement()
    val taskType = varchar("task_type", 100) // e.g. "SYNC_ATTENDANCE"
    val status = varchar("status", 50).default("PENDING") // PENDING, BUSY, DONE, ERROR
    val params = varchar("params", 500).default("")
    val result = varchar("result", 500).default("")
    val updatedAt = varchar("updated_at", 100).default("")

    override val primaryKey = PrimaryKey(id)
}

// ============================================================
// EHS - Matriz Legal Dinámica (NOMs STPS / SEMARNAT / PROFEPA / Protección Civil)
// ============================================================
object LegalMatrixTable : Table("ehs_legal_matrix") {
    val id = integer("id").autoIncrement()
    val clave = varchar("clave", 100)
    val titulo = varchar("titulo", 400).default("")
    val categoria = varchar("categoria", 50).default("STPS")
    val aplica = varchar("aplica", 20).default("Pendiente")
    val justificacion = varchar("justificacion", 500).default("")
    val frecuenciaRevision = varchar("frecuencia_revision", 50).default("Anual")
    val fechaEmision = varchar("fecha_emision", 50).default("")
    val fechaVigencia = varchar("fecha_vigencia", 50).default("")
    val diasAlertaPrevia = integer("dias_alerta_previa").default(30)
    val documentoUrl = varchar("documento_url", 500).default("")
    val responsable = varchar("responsable", 200).default("")
    val notas = varchar("notas", 500).default("")
    // Permiso critico (licencias ambientales, dictamen PC, etc.): si vence,
    // hay riesgo real de clausura/multa -> alerta anticipada y visibilidad
    // prioritaria en calendario, avisos y dashboards.
    val esCritico = bool("es_critico").default(false)
    // Biblioteca legal: URL al texto oficial de la norma (DOF, gob.mx, etc.)
    val urlNorma = varchar("url_norma", 500).default("")

    override val primaryKey = PrimaryKey(id)
}

// EHS - Evidencia documental: archivos (PDF/imagen) que respaldan simulacros,
// estudios, capacitaciones, dictamenes, etc. La columna content_base64 conserva
// base64 legacy o un puntero compacto gdrive:<fileId> para archivos nuevos.
object EhsDocumentTable : Table("ehs_documents") {
    val id = integer("id").autoIncrement()
    val categoria = varchar("categoria", 50).default("Otro")
    val titulo = varchar("titulo", 300)
    val fecha = varchar("fecha", 20).default("")
    val anio = integer("anio").default(0)
    val fileName = varchar("file_name", 300).default("")
    val mimeType = varchar("mime_type", 100).default("application/octet-stream")
    val fileSize = integer("file_size").default(0)
    val notas = varchar("notas", 500).default("")
    val uploadedBy = varchar("uploaded_by", 200).default("")
    val uploadedDate = varchar("uploaded_date", 20).default("")
    // Enlace polimórfico al registro EHS dueño de esta evidencia.
    // Ej.: module_type="inspection", module_record_id=15.
    val moduleType = varchar("module_type", 50).default("")
    val moduleRecordId = integer("module_record_id").default(0)
    val contentBase64 = text("content_base64")
    override val primaryKey = PrimaryKey(id)
}

// EHS - Acciones correctivas vinculadas con hallazgos, incidentes y obligaciones.
// Conservamos el histórico; no habilitamos borrado desde la API.
// Auditorías con checklist: verificación por criterio con hallazgos
// de no conformidad y vínculo a planes de acción.
object EhsChecklistTable : Table("ehs_checklists") {
    val id = integer("id").autoincrement()
    val titulo = varchar("titulo", 300)
    val area = varchar("area", 200).default("")
    val fecha = varchar("fecha", 50)
    val auditor = varchar("auditor", 200).default("")
    val estado = varchar("estado", 20).default("Abierta")   // Abierta / Cerrada
    val observaciones = varchar("observaciones", 1000).default("")

    override val primaryKey = PrimaryKey(id)
}

object EhsChecklistItemTable : Table("ehs_checklist_items") {
    val id = integer("id").autoincrement()
    val checklistId = integer("checklist_id")
    val punto = varchar("punto", 500)
    val resultado = varchar("resultado", 20).default("Pendiente")   // Pendiente / Conforme / NoConforme / NoAplica
    val hallazgo = varchar("hallazgo", 1000).default("")
    val responsable = varchar("responsable", 200).default("")
    val fechaCompromiso = varchar("fecha_compromiso", 50).default("")
    val accionId = integer("accion_id").default(0)

    override val primaryKey = PrimaryKey(id)
}

object EhsActionTable : Table("ehs_action_plans") {
    val id = integer("id").autoIncrement()
    val titulo = varchar("titulo", 300)
    val descripcion = varchar("descripcion", 1000).default("")
    val origenTipo = varchar("origen_tipo", 30).default("manual")
    val origenId = integer("origen_id").default(0)
    val responsable = varchar("responsable", 200)
    val fechaLimite = varchar("fecha_limite", 10)
    val prioridad = varchar("prioridad", 20).default("Media")
    val estado = varchar("estado", 20).default("Abierta")
    val evidenciaUrl = varchar("evidencia_url", 500).default("")
    val fechaCierre = varchar("fecha_cierre", 10).default("")
    override val primaryKey = PrimaryKey(id)
}

// Expediente de proveedor/contratista por centro. El estado informativo NO habilita acceso físico.
object EhsContractorTable : Table("ehs_contractors") {
    val id = integer("id").autoIncrement()
    val empresa = varchar("empresa", 250)
    val actividad = varchar("actividad", 250)
    val centroTrabajo = varchar("centro_trabajo", 200)
    val responsableInterno = varchar("responsable_interno", 200)
    val documentoUrl = varchar("documento_url", 500).default("")
    val vigenciaDocumento = varchar("vigencia_documento", 10).default("")
    val estado = varchar("estado", 20).default("Pendiente")
    val notas = varchar("notas", 500).default("")
    override val primaryKey = PrimaryKey(id)
}

// Tasas internas: entradas auditables y versionadas por mes, NO derivadas
// automáticamente de nómina o accidentes sin revisión del responsable.
object EhsRatePeriodTable : Table("ehs_rate_periods") {
    val id = integer("id").autoIncrement()
    val periodo = varchar("periodo", 7)
    val version = integer("version")
    val horasTrabajadas = double("horas_trabajadas")
    val accidentesRegistrables = integer("accidentes_registrables")
    val diasPerdidos = integer("dias_perdidos")
    val fuenteHoras = varchar("fuente_horas", 300)
    val validadoPor = varchar("validado_por", 200)
    val motivoRevision = varchar("motivo_revision", 500).default("")
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex("ux_ehs_rate_period_version", periodo, version) }
}
