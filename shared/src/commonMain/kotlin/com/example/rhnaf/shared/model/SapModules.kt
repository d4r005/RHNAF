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
