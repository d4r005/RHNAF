package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

// ============================================================
// Tabla de Aprobaciones (Workflow)
// ============================================================
// Permite que cualquier registro en el sistema pase por
// un flujo de aprobación (ej: órdenes de compra, permisos de trabajo,
// pedidos de embarque, etc.)
//
// Flujo de estados:
//   PENDIENTE → EN_REVISION → APROBADO / RECHAZADO

object ApprovalWorkflowTable : Table("wf_approvals") {
    val id = integer("id").autoIncrement()
    val entityType = varchar("entity_type", 50)
    val entityId = integer("entity_id")
    val entityTable = varchar("entity_table", 100).default("")
    val estado = varchar("estado", 20).default("PENDIENTE")
    val solicitadoPor = varchar("solicitado_por", 200).default("")
    val fechaSolicitud = varchar("fecha_solicitud", 50).default("")
    val aprobadoPor = varchar("aprobado_por", 200).default("")
    val fechaAprobacion = varchar("fecha_aprobacion", 50).default("")
    val comentarios = varchar("comentarios", 500).default("")
    val prioridad = varchar("prioridad", 20).default("MEDIA")

    override val primaryKey = PrimaryKey(id)
}

// ============================================================
// Log de Documentos (Trazabilidad estilo SAP)
// ============================================================

object DocumentLogTable : Table("sys_document_log") {
    val id = integer("id").autoIncrement()
    val tipoDocumento = varchar("tipo_documento", 50)
    val numeroDocumento = varchar("numero_documento", 100)
    val tablaOrigen = varchar("tabla_origen", 100).default("")
    val registroId = integer("registro_id").default(0)
    val usuario = varchar("usuario", 200).default("")
    val fecha = varchar("fecha", 50).default("")
    val descripcion = varchar("descripcion", 500).default("")

    override val primaryKey = PrimaryKey(id)
}
