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
