package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

// ============================================================
// Modelos de datos de los modulos estilo SAP integrados a RHNAF
// (FI, CO, MM, PP, QM, PM, EWM, WM, HCM, GTS, EHS, GRC)
// ============================================================

// FI - Financial Accounting (Contabilidad Financiera)
@Serializable
data class JournalEntry(
    val id: Int = 0,
    val fecha: String,
    val cuenta: String,
    val concepto: String = "",
    val tipo: String = "",
    val monto: String = "",
    val referencia: String = ""
)

// CO - Controlling (Control de Costos)
@Serializable
data class CostCenter(
    val id: Int = 0,
    val codigo: String,
    val nombre: String,
    val departamento: String = "",
    val presupuestoMensual: String = "",
    val gastoActual: String = ""
)

// MM - Materials Management (Compras)
@Serializable
data class PurchaseOrder(
    val id: Int = 0,
    val numero: String,
    val proveedor: String,
    val fecha: String = "",
    val descripcion: String = "",
    val montoTotal: String = "",
    val estado: String = ""
)

// PP - Production Planning (Planificación de Producción)
@Serializable
data class ProductionOrder(
    val id: Int = 0,
    val numero: String,
    val producto: String,
    val cantidadPlan: String = "",
    val cantidadProducida: String = "",
    val centroTrabajo: String = "",
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val estado: String = ""
)

// QM - Quality Management (Gestión de Calidad)
@Serializable
data class QualityInspection(
    val id: Int = 0,
    val fecha: String,
    val loteProducto: String,
    val inspector: String = "",
    val resultado: String = "",
    val observaciones: String = ""
)

// PM - Plant Maintenance (Mantenimiento de Planta)
@Serializable
data class MaintenanceOrder(
    val id: Int = 0,
    val equipo: String,
    val tipo: String = "",
    val fechaProgramada: String = "",
    val fechaRealizada: String = "",
    val tecnico: String = "",
    val estado: String = "",
    val notas: String = ""
)

// EWM - Extended Warehouse Management (Gestión Avanzada de Almacenes)
@Serializable
data class WarehouseTask(
    val id: Int = 0,
    val tipo: String,
    val bin: String = "",
    val sku: String = "",
    val cantidad: String = "",
    val asignadoA: String = "",
    val estado: String = ""
)

// HCM - Human Capital Management (Reclutamiento)
@Serializable
data class RecruitmentVacancy(
    val id: Int = 0,
    val puesto: String,
    val departamento: String = "",
    val fechaApertura: String = "",
    val vacantes: String = "",
    val candidatosPostulados: String = "",
    val estado: String = ""
)

// GTS - Global Trade Services (Comercio Exterior)
@Serializable
data class CustomsDeclaration(
    val id: Int = 0,
    val numeroPedimento: String,
    val fecha: String = "",
    val cliente: String = "",
    val paisDestino: String = "",
    val valorAduana: String = "",
    val regimen: String = "",
    val estado: String = ""
)

// EHS - Environment, Health & Safety
// 1. Inspecciones de Seguridad (mejorada)
@Serializable
data class SafetyInspection(
    val id: Int = 0,
    val fecha: String,
    val tipoInspeccion: String = "",
    val area: String = "",
    val inspector: String = "",
    val hallazgos: String = "",
    val riesgo: String = "",
    val accionesCorrectivas: String = "",
    val fechaCierre: String = "",
    val evidencia: String = "",
    val estado: String = ""
)

@Serializable
data class OperationalDocumentReadResult(
    val categoria: String = "",
    val campos: List<String> = emptyList(),
    val filas: List<Map<String, String>> = emptyList(),
    val advertencias: List<String> = emptyList()
)

// 2. Incidentes y Accidentes
@Serializable
data class SafetyIncident(
    val id: Int = 0,
    val fecha: String,
    val tipo: String = "",
    val severidad: String = "",
    val personaAfectada: String = "",
    val departamento: String = "",
    val parteCuerpo: String = "",
    val diasPerdidos: String = "",
    val descripcion: String = "",
    val causaRaiz: String = "",
    val accionesCorrectivas: String = "",
    val estado: String = ""
)

// 3. Permisos de Trabajo
@Serializable
data class WorkPermit(
    val id: Int = 0,
    val tipo: String = "",
    val solicitante: String = "",
    val autorizadoPor: String = "",
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val area: String = "",
    val riesgosIdentificados: String = "",
    val eppRequerido: String = "",
    val estado: String = ""
)

// 4. Entrega de EPP (Equipo de Proteccion Personal)
@Serializable
data class PpeDelivery(
    val id: Int = 0,
    val fecha: String,
    val empleado: String = "",
    val tipoEpp: String = "",
    val talla: String = "",
    val proximaReposicion: String = "",
    val firma: String = ""
)

// 5. Capacitaciones de Seguridad
@Serializable
data class SafetyTraining(
    val id: Int = 0,
    val fecha: String,
    val tema: String = "",
    val instructor: String = "",
    val asistentes: String = "",
    val vigenciaMeses: String = "",
    val proximaFecha: String = "",
    val estado: String = ""
)

// 5b. Constancias DC-3 (capacitación interna)
@Serializable
data class Dc3Constancia(
    val id: Int = 0,
    val trabajador: String = "",
    val tema: String = "",
    val fecha: String = "",
    val horas: String = "",
    val responsable: String = "",
    val evidenciaUrl: String = ""
)

// 5c. Eventos propios del calendario EHS (capturados al dar clic en un día)
@Serializable
data class EhsCustomEvent(
    val id: Int = 0,
    val fecha: String,
    val tipo: String = "evento",
    val titulo: String = "",
    val detalle: String = "",
    val responsable: String = "",
    val estado: String = ""
)

// 6. Simulacros de Emergencia
@Serializable
data class EmergencyDrill(
    val id: Int = 0,
    val fecha: String,
    val tipo: String = "",
    val participantes: String = "",
    val tiempoEvacuacion: String = "",
    val resultado: String = "",
    val observaciones: String = "",
    val estado: String = ""
)

// 7. Matriz de Riesgos / IPER
@Serializable
data class RiskMatrix(
    val id: Int = 0,
    val area: String = "",
    val proceso: String = "",
    val riesgoIdentificado: String = "",
    val probabilidad: String = "",
    val severidad: String = "",
    val nivelRiesgo: String = "",
    val controles: String = "",
    val responsable: String = "",
    val estado: String = ""
)

// Auditorías con checklist (estilo ACM/Prysmex): puntos de verificación,
// hallazgos de no conformidad y vínculo con planes de acción.
@Serializable
data class EhsChecklist(
    val id: Int = 0,
    val titulo: String = "",
    val area: String = "",
    val fecha: String = "",
    val auditor: String = "",
    val estado: String = "Abierta",
    val observaciones: String = ""
)

@Serializable
data class EhsChecklistItem(
    val id: Int = 0,
    val checklistId: Int = 0,
    val punto: String = "",
    val resultado: String = "Pendiente",
    val hallazgo: String = "",
    val responsable: String = "",
    val fechaCompromiso: String = "",
    val accionId: Int = 0
)

// GRC - SAP Security / GRC (Gobierno, Riesgo y Cumplimiento)
@Serializable
data class AccessAuditLog(
    val id: Int = 0,
    val fecha: String,
    val usuario: String,
    val accion: String = "",
    val modulo: String = "",
    val resultado: String = ""
)

// --- EXPANSION EHS (Nivel EHSSoft) ---

// EHS-Ambiente. Gestion de Residuos
@Serializable
data class WasteManifest(
    val id: Int = 0,
    val fecha: String,
    val residuo: String,
    val tipo: String = "", // Peligroso, No peligroso, Reciclable
    val cantidad: String = "",
    val unidad: String = "kg",
    val transportista: String = "",
    val destinoFinal: String = "",
    val numeroManifiesto: String = "",
    val estado: String = "Pendiente"
)

// EHS-Salud. Vigilancia Medica
@Serializable
data class MedicalExam(
    val id: Int = 0,
    val empleadoId: String,
    val nombreEmpleado: String = "",
    val fecha: String,
    val tipoExamen: String = "", // Ingreso, Periodico, Egreso
    val resultado: String = "", // Apto, Apto con restricciones, No apto
    val observaciones: String = "",
    val proximaCita: String = "",
    val medico: String = ""
)

// EHS-Quimicos. Inventario MSDS
@Serializable
data class ChemicalProduct(
    val id: Int = 0,
    val nombre: String,
    val fabricante: String = "",
    val areaUso: String = "",
    val nivelRiesgo: String = "", // 0-4 NFPA
    val hojaSeguridadUrl: String = "",
    val estado: String = "Activo",
    val ultimaRevision: String = ""
)

// ============================================================
// WORKFLOW DE APROBACIONES
// ============================================================

@Serializable
data class ApprovalRequest(
    val id: Int = 0,
    val entityType: String,        // PURCHASE_ORDER, WORK_PERMIT, SHIPPING_ORDER, etc.
    val entityId: Int,
    val entityTable: String = "",
    val estado: String = "PENDIENTE",  // PENDIENTE, EN_REVISION, APROBADO, RECHAZADO
    val solicitadoPor: String = "",
    val fechaSolicitud: String = "",
    val aprobadoPor: String = "",
    val fechaAprobacion: String = "",
    val comentarios: String = "",
    val prioridad: String = "MEDIA"  // BAJA, MEDIA, ALTA, URGENTE
)

@Serializable
data class DocumentLog(
    val id: Int = 0,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val tablaOrigen: String = "",
    val registroId: Int = 0,
    val usuario: String = "",
    val fecha: String = "",
    val descripcion: String = ""
)

// ============================================================
// EHS - MATRIZ LEGAL DINÁMICA (estilo EHSoft / ACM Suite)
// ============================================================
// Catálogo de obligaciones normativas (NOMs de STPS, SEMARNAT,
// PROFEPA, Protección Civil) con vigencias, evidencia documental
// y cálculo automático de % de cumplimiento + alertas de vencimiento.

@Serializable
data class LegalMatrixItem(
    val id: Int = 0,
    val clave: String,                    // ej: "NOM-001-STPS-2008"
    val titulo: String = "",              // nombre completo de la norma/obligación
    val categoria: String = "STPS",       // STPS, SEMARNAT, PROFEPA, ProteccionCivil, Estatal
    val aplica: String = "Pendiente",     // Si, No, Pendiente (resultado del cuestionario de aplicabilidad)
    val justificacion: String = "",       // por qué aplica o no
    val frecuenciaRevision: String = "Anual", // Anual, Semestral, Trimestral, Unica
    val fechaEmision: String = "",        // fecha del último estudio/dictamen/documento
    val fechaVigencia: String = "",       // fecha de vencimiento
    val diasAlertaPrevia: Int = 30,       // dias antes del vencimiento para alertar
    val documentoUrl: String = "",        // link a la evidencia (PDF de dictamen, estudio, etc.)
    val responsable: String = "",
    val notas: String = "",
    val esCritico: Boolean = false,        // permiso crítico: riesgo de clausura si vence
    val urlNorma: String = "",            // URL al texto oficial de la norma (DOF/gob.mx)
    // --- Extensiones estilo EHSoft ---
    val subCategoria: String = "",        // grupo interno: Seguridad, Salud ocupacional, Ambiental...
    val tipoObligacion: String = "",     // Permiso, Registro, Dictamen, Manifiesto, Informe, Programa, Cumplimiento continuo
    val autoridad: String = "",           // STPS, SEMARNAT, PROFEPA, Protección Civil estatal/municipal...
    val responsableEmail: String = "",   // correo para recordatorios de vencimiento
    val nDocumentos: Int = 0,             // CALCULADO: número de documentos de cumplimiento adjuntos
    val estado: String = ""               // CALCULADO por el servidor: Vigente, PorVencer, Vencido, NoAplica, Pendiente
)

@Serializable
data class LegalMatrixSummary(
    val totalObligaciones: Int = 0,
    val aplicables: Int = 0,
    val vigentes: Int = 0,
    val porVencer: Int = 0,
    val vencidos: Int = 0,
    val noAplica: Int = 0,
    val pendientes: Int = 0,
    val porcentajeCumplimiento: Double = 0.0,
    val porCategoria: List<CategoryCompliance> = emptyList(),
    val proximosAVencer: List<LegalMatrixItem> = emptyList()
)

@Serializable
data class CategoryCompliance(
    val categoria: String,
    val aplicables: Int = 0,
    val vigentes: Int = 0,
    val porcentaje: Double = 0.0,
    val subCategorias: List<SubCategoryCompliance> = emptyList()
)

@Serializable
data class SubCategoryCompliance(
    val subCategoria: String,
    val aplicables: Int = 0,
    val vigentes: Int = 0,
    val porcentaje: Double = 0.0
)

// Marco legal por artículo/fundamento: cada obligación puede citar varias leyes
// (federal, estatal, municipal) con su referencia exacta y enlace oficial.
@Serializable
data class LegalMatrixRef(
    val id: Int = 0,
    val matrizId: Int = 0,
    val nivel: String = "Federal",       // Federal, Estatal, Municipal
    val referencia: String = "",          // "Art. 37 Fracc. II", "Reglamento 102 ..."
    val nombreLey: String = "",
    val url: String = "",
    val creadoPor: String = ""
)

// Documento de cumplimiento por obligación (soporta varios archivos con
// histórico: cada uno con su propia fecha de expedición/vigencia y recordatorio).
@Serializable
data class LegalMatrixDoc(
    val id: Int = 0,
    val matrizId: Int = 0,
    val documentId: Int = 0,             // FK a ehs_documents (archivo en Drive)
    val nombre: String = "",
    val tipoDocumento: String = "",      // Dictamen, Permiso, Estudio, Registro, Manifiesto, Otro
    val fechaExpedicion: String = "",    // yyyy-MM-dd
    val fechaVigencia: String = "",     // yyyy-MM-dd (vencimiento del documento)
    val recordatorioDias: Int = 30,       // días antes del vencimiento para enviar recordatorio
    val comentario: String = "",
    val subidoPor: String = "",
    val subidoFecha: String = ""
)

// Detalle completo de una obligación para el panel de ficha (estilo EHSoft).
@Serializable
data class LegalMatrixDetalle(
    val obligacion: LegalMatrixItem,
    val referencias: List<LegalMatrixRef> = emptyList(),
    val documentos: List<LegalMatrixDoc> = emptyList(),
    val tareas: List<LegalMatrixTarea> = emptyList()
)

@Serializable
data class LegalMatrixTarea(
    val id: Int = 0,
    val titulo: String = "",
    val responsable: String = "",
    val fechaLimite: String = "",
    val prioridad: String = "Media",
    val estado: String = "Abierta"
)

// Resultado del envío de recordatorios por correo.
@Serializable
data class EhsReminderSummary(
    val smtpConfigurado: Boolean = false,
    val enviados: Int = 0,
    val destinatarios: List<String> = emptyList(),
    val avisosDetectados: Int = 0,
    val mensaje: String = ""
)

// ============================================================
// EHS - Evidencia Documental
// Archivos que respaldan el cumplimiento EHS: simulacros realizados,
// estudios (ruido, iluminacion, aguas), capacitaciones, dictamenes, etc.
// ============================================================
@Serializable
data class EhsDocument(
    val id: Int = 0,
    val categoria: String = "Otro",        // Simulacro, Capacitacion, Estudio, Inspeccion, Dictamen, ExamenMedico, Otro
    val titulo: String = "",
    val fecha: String = "",
    val anio: Int = 0,                // dd/MM/yyyy del documento
    val fileName: String = "",
    val mimeType: String = "",
    val fileSize: Int = 0,                 // bytes
    val notas: String = "",
    val uploadedBy: String = "",
    val uploadedDate: String = "",         // dd/MM/yyyy de cuando se subio
    val moduleType: String = "",           // inspection, drill, training, etc.
    val moduleRecordId: Int = 0             // ID del registro concreto asociado
)

// Vincular manualmente una evidencia ya subida con cualquier registro EHS
// existente (o des-vincularla con moduleRecordId = 0).
@Serializable
data class EhsDocumentLinkRequest(
    val moduleType: String,
    val moduleRecordId: Int
)

@Serializable
data class EhsDocumentUpload(
    val categoria: String = "Otro",
    val titulo: String,
    val fecha: String = "",
    val anio: Int = 0,
    val notas: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val fileSize: Int = 0,
    val contentBase64: String,
    val moduleType: String = "",
    val moduleRecordId: Int = 0
)
