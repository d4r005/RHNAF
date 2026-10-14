package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

/**
 * QIS - Quality Information System.
 * Control de documentos del sistema de gestión: cada documento controlado
 * tiene código único, versión vigente, vigencias y responsables. El historial
 * de cambios vive en QisChangeLogTable.
 */
object QisDocumentTable : Table("qis_documents") {
    val id = integer("id").autoIncrement()
    val codigo = varchar("codigo", 40)                  // PRO-001; único
    val titulo = varchar("titulo", 300)
    val tipo = varchar("tipo", 40).default("Procedimiento")
    val proceso = varchar("proceso", 200).default("")
    val versionActual = varchar("version_actual", 12).default("1.0")
    val fechaEmision = varchar("fecha_emision", 20).default("")
    val proximaRevision = varchar("proxima_revision", 20).default("")
    val estado = varchar("estado", 20).default("Vigente") // Vigente/EnRevision/Obsoleto
    val elaboradoPor = varchar("elaborado_por", 200).default("")
    val aprobadoPor = varchar("aprobado_por", 200).default("")
    val urlArchivo = varchar("url_archivo", 500).default("")
    val notas = varchar("notas", 500).default("")
    override val primaryKey = PrimaryKey(id)
}

/**
 * QIS - Registro de cambios por documento. Cada alta o nueva versión agrega
 * una fila aquí; nunca se borra el historial (trazabilidad ISO 9001).
 */
object QisChangeLogTable : Table("qis_change_log") {
    val id = integer("id").autoIncrement()
    val documentoId = integer("documento_id")          // FK lógica a qis_documents
    val versionAnterior = varchar("version_anterior", 12).default("")
    val versionNueva = varchar("version_nueva", 12).default("")
    val fechaCambio = varchar("fecha_cambio", 20).default("")
    val descripcion = varchar("descripcion", 800).default("")
    val cambiadoPor = varchar("cambiado_por", 200).default("")
    val aprobadoPor = varchar("aprobado_por", 200).default("")
    override val primaryKey = PrimaryKey(id)
}
