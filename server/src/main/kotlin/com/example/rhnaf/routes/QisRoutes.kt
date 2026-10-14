package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.QisChangeLogTable
import com.example.rhnaf.database.QisDocumentTable
import com.example.rhnaf.shared.model.QisChangeRecord
import com.example.rhnaf.shared.model.QisChangeRequest
import com.example.rhnaf.shared.model.QisDocument
import com.example.rhnaf.shared.model.QisDocumentRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * QIS - Quality Information System: control de documentos.
 *
 * Alta de documentos controlados (código único, versión, vigencias y
 * responsables), cambio de estado (Vigente/EnRevision/Obsoleto) y registro
 * de cambios con historial inmutable por trazabilidad ISO 9001.
 *
 * Escritura: ADMIN. Lectura: todos los roles autenticados (el sistema de
 * gestión es consultable por todo el personal, pero solo ADMIN controla los
 * documentos).
 */
private val QIS_WRITE = setOf(Roles.ADMIN)
private val QIS_TIPOS = setOf("Procedimiento", "Instructivo", "Formato", "Politica", "Manual", "Plan", "Registro", "Otro")
private val QIS_ESTADOS = setOf("Vigente", "EnRevision", "Obsoleto")

private fun docFrom(row: ResultRow) = QisDocument(
    id = row[QisDocumentTable.id],
    codigo = row[QisDocumentTable.codigo],
    titulo = row[QisDocumentTable.titulo],
    tipo = row[QisDocumentTable.tipo],
    proceso = row[QisDocumentTable.proceso],
    versionActual = row[QisDocumentTable.versionActual],
    fechaEmision = row[QisDocumentTable.fechaEmision],
    proximaRevision = row[QisDocumentTable.proximaRevision],
    estado = row[QisDocumentTable.estado],
    elaboradoPor = row[QisDocumentTable.elaboradoPor],
    aprobadoPor = row[QisDocumentTable.aprobadoPor],
    urlArchivo = row[QisDocumentTable.urlArchivo],
    notas = row[QisDocumentTable.notas]
)

private fun changeFrom(row: ResultRow) = QisChangeRecord(
    id = row[QisChangeLogTable.id],
    documentoId = row[QisChangeLogTable.documentoId],
    versionAnterior = row[QisChangeLogTable.versionAnterior],
    versionNueva = row[QisChangeLogTable.versionNueva],
    fechaCambio = row[QisChangeLogTable.fechaCambio],
    descripcion = row[QisChangeLogTable.descripcion],
    cambiadoPor = row[QisChangeLogTable.cambiadoPor],
    aprobadoPor = row[QisChangeLogTable.aprobadoPor]
)

/** Formato de versión: N.N (dos niveles, p. ej. 1.0, 2.3, 12.0). */
private fun versionValida(v: String) = Regex("""^\d{1,3}\.\d{1,2}$""").matches(v)

fun Route.qisRouting() {
    route("/api/v1/qis/documentos") {

        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val estado = call.request.queryParameters["estado"]
                val items = DatabaseFactory.dbQuery {
                    val q = QisDocumentTable.selectAll()
                    val filtered = if (estado != null) q.where { QisDocumentTable.estado eq estado } else q
                    filtered.orderBy(QisDocumentTable.codigo to SortOrder.ASC).map(::docFrom)
                }
                call.respond(items)
            }
        }

        post {
            safeApiCall(call) {
                requireRoleOr403(call, QIS_WRITE) ?: return@safeApiCall
                val req = call.receive<QisDocumentRequest>()
                if (req.codigo.isBlank() || req.codigo.length > 40) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Código requerido (máximo 40 caracteres)"))
                }
                if (req.titulo.isBlank() || req.titulo.length > 300) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Título requerido (máximo 300 caracteres)"))
                }
                if (req.tipo !in QIS_TIPOS) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Tipo inválido: ${QIS_TIPOS.joinToString(", ")}"))
                }
                val dup = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.codigo eq req.codigo }.limit(1).any()
                }
                if (dup) {
                    return@safeApiCall call.respond(HttpStatusCode.Conflict, mapOf("status" to "error", "message" to "Ya existe un documento con el código ${req.codigo}"))
                }
                DatabaseFactory.dbQuery {
                    val newId = QisDocumentTable.insert {
                        it[codigo] = req.codigo
                        it[titulo] = req.titulo
                        it[tipo] = req.tipo
                        it[proceso] = req.proceso
                        it[versionActual] = "1.0"
                        it[fechaEmision] = req.fechaEmision
                        it[proximaRevision] = req.proximaRevision
                        it[estado] = "Vigente"
                        it[elaboradoPor] = req.elaboradoPor
                        it[aprobadoPor] = req.aprobadoPor
                        it[urlArchivo] = req.urlArchivo
                        it[notas] = req.notas
                    } get QisDocumentTable.id
                    // El alta siempre deja la primera entrada del historial.
                    QisChangeLogTable.insert {
                        it[documentoId] = newId
                        it[versionAnterior] = "-"
                        it[versionNueva] = "1.0"
                        it[fechaCambio] = req.fechaEmision
                        it[descripcion] = "Alta del documento"
                        it[cambiadoPor] = req.elaboradoPor
                        it[aprobadoPor] = req.aprobadoPor
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }

        // Historial de cambios de un documento (nunca se modifica ni se borra).
        get("/{id}/cambios") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID inválido"))
                val existe = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.id eq id }.limit(1).any()
                }
                if (!existe) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))
                val cambios = DatabaseFactory.dbQuery {
                    QisChangeLogTable.selectAll().where { QisChangeLogTable.documentoId eq id }
                        .orderBy(QisChangeLogTable.id to SortOrder.DESC).map(::changeFrom)
                }
                call.respond(cambios)
            }
        }

        // Registro de un cambio de versión: actualiza la versión vigente del
        // documento, guarda la fila del historial y opcionalmente reemplaza
        // el archivo controlado.
        post("/{id}/cambios") {
            safeApiCall(call) {
                requireRoleOr403(call, QIS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID inválido"))
                val req = call.receive<QisChangeRequest>()
                if (!versionValida(req.versionNueva)) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Versión inválida, usa formato N.N (ej. 2.0)"))
                }
                if (req.descripcion.isBlank() || req.descripcion.length > 800) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "La descripción del cambio es requerida (máximo 800 caracteres)"))
                }
                val actual = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.id eq id }.singleOrNull()?.let(::docFrom)
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))
                if (req.versionNueva == actual.versionActual) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "La versión ${req.versionNueva} ya es la vigente"))
                }
                if (actual.estado == "Obsoleto") {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Un documento obsoleto no admite nuevas versiones; crea un documento nuevo"))
                }
                DatabaseFactory.dbQuery {
                    QisDocumentTable.update({ QisDocumentTable.id eq id }) {
                        it[versionActual] = req.versionNueva
                        if (req.fechaCambio.isNotBlank()) it[fechaEmision] = req.fechaCambio
                        if (req.nuevaUrlArchivo.isNotBlank()) it[urlArchivo] = req.nuevaUrlArchivo
                        it[estado] = "Vigente"
                        if (req.aprobadoPor.isNotBlank()) it[aprobadoPor] = req.aprobadoPor
                    }
                    QisChangeLogTable.insert {
                        it[documentoId] = id
                        it[versionAnterior] = actual.versionActual
                        it[versionNueva] = req.versionNueva
                        it[fechaCambio] = req.fechaCambio
                        it[descripcion] = req.descripcion
                        it[cambiadoPor] = req.cambiadoPor
                        it[aprobadoPor] = req.aprobadoPor
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }

        // Cambio de estado sin cambio de versión (p. ej. marcar Obsoleto
        // cuando el documento se sustituye por otro).
        patch("/{id}/estado") {
            safeApiCall(call) {
                requireRoleOr403(call, QIS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID inválido"))
                val estado = call.request.queryParameters["estado"] ?: ""
                if (estado !in QIS_ESTADOS) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Estado inválido: ${QIS_ESTADOS.joinToString(", ")}"))
                }
                val updated = DatabaseFactory.dbQuery {
                    QisDocumentTable.update({ QisDocumentTable.id eq id }) { it[QisDocumentTable.estado] = estado }
                }
                if (updated == 0) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))
                call.respond(mapOf("status" to "ok"))
            }
        }

        // Borrar un documento también borra su historial. Solo para altas
        // erróneas: el control correcto es marcarlo Obsoleto.
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, QIS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID inválido"))
                val existe = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.id eq id }.limit(1).any()
                }
                if (!existe) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))
                DatabaseFactory.dbQuery {
                    QisChangeLogTable.deleteWhere { QisChangeLogTable.documentoId eq id }
                    QisDocumentTable.deleteWhere { QisDocumentTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
