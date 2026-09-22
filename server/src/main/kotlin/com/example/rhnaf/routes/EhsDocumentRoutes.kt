package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsDocumentTable
import com.example.rhnaf.shared.model.EhsDocument
import com.example.rhnaf.shared.model.EhsDocumentUpload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * EHS - Evidencia Documental
 * Subida/listado/descarga/eliminacion de archivos que respaldan el cumplimiento:
 * simulacros realizados, estudios (ruido, iluminacion, aguas), capacitaciones,
 * dictamenes de proteccion civil, examenes medicos, etc.
 *
 * El archivo viaja como base64 en JSON (mismo patron que las fotos de empleado)
 * y se sirve de vuelta via /{id}/descargar con el MIME correcto para que el
 * navegador lo muestre inline (PDF) o lo descargue.
 */
fun Route.ehsDocumentRouting() {
    route("/api/v1/ehs/documentos") {

        // Lista (sin el contenido base64, solo metadatos). Filtro opcional por categoria.
        get {
            val categoria = call.request.queryParameters["categoria"]
            val items = DatabaseFactory.dbQuery {
                val base = EhsDocumentTable.selectAll()
                val filtered = if (categoria.isNullOrBlank()) base else base.where { EhsDocumentTable.categoria eq categoria }
                filtered.orderBy(EhsDocumentTable.id, SortOrder.DESC).map {
                    EhsDocument(
                        id = it[EhsDocumentTable.id],
                        categoria = it[EhsDocumentTable.categoria],
                        titulo = it[EhsDocumentTable.titulo],
                        fecha = it[EhsDocumentTable.fecha],
                        fileName = it[EhsDocumentTable.fileName],
                        mimeType = it[EhsDocumentTable.mimeType],
                        fileSize = it[EhsDocumentTable.fileSize],
                        notas = it[EhsDocumentTable.notas],
                        uploadedBy = it[EhsDocumentTable.uploadedBy],
                        uploadedDate = it[EhsDocumentTable.uploadedDate]
                    )
                }
            }
            call.respond(items)
        }

        // Sube una evidencia (base64 en JSON). Requiere rol EHS_WRITE (ADMIN/SEGURIDAD).
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val req = call.receive<EhsDocumentUpload>()
                if (req.titulo.isBlank() || req.contentBase64.isBlank()) {
                    return@safeApiCall call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "Titulo y archivo son obligatorios")
                    )
                }
                val size = Base64.getDecoder().runCatching { decode(req.contentBase64).size }.getOrDefault(req.fileSize)
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val id = DatabaseFactory.dbQuery {
                    EhsDocumentTable.insert {
                        it[categoria] = req.categoria.ifBlank { "Otro" }
                        it[titulo] = req.titulo.trim()
                        it[fecha] = req.fecha
                        it[fileName] = req.fileName.ifBlank { "evidencia" }
                        it[mimeType] = req.mimeType.ifBlank { "application/octet-stream" }
                        it[fileSize] = if (req.fileSize > 0) req.fileSize else size
                        it[notas] = req.notas
                        it[uploadedDate] = today
                        it[contentBase64] = req.contentBase64
                    } get EhsDocumentTable.id
                }
                call.respond(mapOf("status" to "ok", "id" to id))
            }
        }

        // Descarga/visualiza el archivo original con su MIME (el navegador abre PDF inline).
        get("/{id}/descargar") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID invalido"))
                return@get
            }
            val row = DatabaseFactory.dbQuery {
                EhsDocumentTable.selectAll().where { EhsDocumentTable.id eq id }.singleOrNull()
            }
            if (row == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Evidencia no encontrada"))
                return@get
            }
            val bytes = runCatching { Base64.getDecoder().decode(row[EhsDocumentTable.contentBase64]) }
                .getOrNull() ?: run {
                call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "error", "message" to "Archivo corrupto"))
                return@get
            }
            val mime = row[EhsDocumentTable.mimeType].ifBlank { "application/octet-stream" }
            val contentType = runCatching { ContentType.parse(mime) }.getOrDefault(ContentType.Application.OctetStream)
            val safeName = row[EhsDocumentTable.fileName].replace("\"", "'")
            call.response.header(HttpHeaders.ContentDisposition, "inline; filename=\"$safeName\"")
            call.respondBytes(bytes, contentType)
        }

        // Elimina una evidencia. Requiere rol EHS_WRITE.
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID invalido"))
                DatabaseFactory.dbQuery { EhsDocumentTable.deleteWhere { EhsDocumentTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
