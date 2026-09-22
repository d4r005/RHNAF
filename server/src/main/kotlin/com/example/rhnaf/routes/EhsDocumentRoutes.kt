package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireAuthOr401
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsDocumentTable
import com.example.rhnaf.service.GoogleDriveService
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

private const val DRIVE_POINTER_PREFIX = "gdrive:"

/**
 * EHS - Evidencia Documental.
 *
 * Los archivos nuevos se almacenan en Google Drive. Supabase conserva solamente
 * metadatos y el ID de Drive dentro de content_base64 como "gdrive:<fileId>".
 * Reutilizar esa columna evita una migracion de esquema mientras Supabase esta
 * limitado por espacio. Los registros antiguos que contienen base64 siguen
 * descargandose normalmente, por lo que la migracion puede hacerse gradualmente.
 */
fun Route.ehsDocumentRouting() {
    route("/api/v1/ehs/documentos") {
        get {
            requireAuthOr401(call) ?: return@get
            val categoria = call.request.queryParameters["categoria"]
            val moduleType = call.request.queryParameters["moduleType"]
            val moduleRecordId = call.request.queryParameters["moduleRecordId"]?.toIntOrNull()

            // La BD de produccion puede seguir con el esquema anterior, sin
            // module_type/module_record_id. Intentamos el esquema nuevo y, si
            // esas columnas aun no existen, servimos los metadatos legacy.
            val items = try {
                loadDocumentMetadata(categoria, moduleType, moduleRecordId, includeModuleLink = true)
            } catch (e: Exception) {
                println("[EhsDocumentRoutes] Esquema legacy detectado al listar: ${e.message}")
                loadDocumentMetadata(categoria, null, null, includeModuleLink = false)
            }
            call.respond(items)
        }

        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                if (!GoogleDriveService.isConfigured()) {
                    return@safeApiCall call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "error", "message" to "Google Drive aun no esta conectado")
                    )
                }

                val req = call.receive<EhsDocumentUpload>()
                if (req.titulo.isBlank() || req.contentBase64.isBlank()) {
                    return@safeApiCall call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "Titulo y archivo son obligatorios")
                    )
                }
                val bytes = runCatching { Base64.getDecoder().decode(req.contentBase64) }.getOrNull()
                    ?: return@safeApiCall call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "El contenido del archivo no es base64 valido")
                    )
                if (bytes.isEmpty()) {
                    return@safeApiCall call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "El archivo esta vacio")
                    )
                }

                val folderId = GoogleDriveService.folderId!!
                val safeFileName = req.fileName.ifBlank { "evidencia" }
                val mimeType = req.mimeType.ifBlank { "application/octet-stream" }
                val driveFileId = GoogleDriveService.uploadFile(bytes, safeFileName, mimeType, folderId)
                    ?: return@safeApiCall call.respond(
                        HttpStatusCode.BadGateway,
                        mapOf("status" to "error", "message" to "Google Drive no pudo guardar el archivo")
                    )

                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val uploadedBy = call.request.header(HttpHeaders.Authorization)
                    ?.removePrefix("Bearer ")?.trim().orEmpty()
                val pointer = "$DRIVE_POINTER_PREFIX$driveFileId"

                try {
                    val id = try {
                        insertDocument(req, safeFileName, mimeType, bytes.size, uploadedBy, today, pointer, includeModuleLink = true)
                    } catch (e: Exception) {
                        println("[EhsDocumentRoutes] Insert con esquema nuevo fallo; intentando legacy: ${e.message}")
                        insertDocument(req, safeFileName, mimeType, bytes.size, uploadedBy, today, pointer, includeModuleLink = false)
                    }
                    call.respond(mapOf("status" to "ok", "id" to id.toString(), "storage" to "google_drive"))
                } catch (e: Exception) {
                    // Evitar archivos huerfanos si Supabase rechaza el registro.
                    GoogleDriveService.deleteFile(driveFileId)
                    throw e
                }
            }
        }

        // Migra de forma idempotente archivos antiguos guardados como base64.
        // Requiere que Supabase ya permita escrituras. Se limita por llamada
        // para no agotar memoria ni exceder el tiempo de respuesta del Space.
        post("/migrar-google-drive") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                if (!GoogleDriveService.isConfigured()) {
                    return@safeApiCall call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "error", "message" to "Google Drive aun no esta conectado")
                    )
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 25) ?: 10
                val legacyRows = DatabaseFactory.dbQuery {
                    EhsDocumentTable
                        .select(
                            EhsDocumentTable.id,
                            EhsDocumentTable.fileName,
                            EhsDocumentTable.mimeType,
                            EhsDocumentTable.contentBase64
                        )
                        .orderBy(EhsDocumentTable.id, SortOrder.ASC)
                        .mapNotNull { row ->
                            val stored = row[EhsDocumentTable.contentBase64]
                            if (stored.startsWith(DRIVE_POINTER_PREFIX)) null else LegacyDocument(
                                row[EhsDocumentTable.id],
                                row[EhsDocumentTable.fileName],
                                row[EhsDocumentTable.mimeType],
                                stored
                            )
                        }
                        .take(limit)
                }

                var migrated = 0
                val errors = mutableListOf<String>()
                for (legacy in legacyRows) {
                    val bytes = runCatching { Base64.getDecoder().decode(legacy.base64) }.getOrNull()
                    if (bytes == null) {
                        errors += "ID ${legacy.id}: base64 invalido"
                        continue
                    }
                    val driveId = GoogleDriveService.uploadFile(
                        bytes,
                        legacy.fileName.ifBlank { "evidencia-${legacy.id}" },
                        legacy.mimeType.ifBlank { "application/octet-stream" },
                        GoogleDriveService.folderId!!
                    )
                    if (driveId == null) {
                        errors += "ID ${legacy.id}: fallo al subir a Drive"
                        continue
                    }
                    try {
                        DatabaseFactory.dbQuery {
                            EhsDocumentTable.update({ EhsDocumentTable.id eq legacy.id }) {
                                it[contentBase64] = "$DRIVE_POINTER_PREFIX$driveId"
                            }
                        }
                        migrated++
                    } catch (e: Exception) {
                        GoogleDriveService.deleteFile(driveId)
                        errors += "ID ${legacy.id}: Supabase rechazo la actualizacion"
                    }
                }
                call.respond(
                    mapOf(
                        "status" to if (errors.isEmpty()) "ok" else "partial",
                        "migrated" to migrated,
                        "remainingInBatch" to (legacyRows.size - migrated),
                        "errors" to errors
                    )
                )
            }
        }

        get("/{id}/descargar") {
            requireAuthOr401(call) ?: return@get
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

            val storedContent = row[EhsDocumentTable.contentBase64]
            val bytes = if (storedContent.startsWith(DRIVE_POINTER_PREFIX)) {
                val fileId = storedContent.removePrefix(DRIVE_POINTER_PREFIX)
                GoogleDriveService.downloadFile(fileId)
            } else {
                runCatching { Base64.getDecoder().decode(storedContent) }.getOrNull()
            }
            if (bytes == null) {
                call.respond(
                    HttpStatusCode.BadGateway,
                    mapOf("status" to "error", "message" to "No fue posible recuperar el archivo")
                )
                return@get
            }

            val mime = row[EhsDocumentTable.mimeType].ifBlank { "application/octet-stream" }
            val contentType = runCatching { ContentType.parse(mime) }.getOrDefault(ContentType.Application.OctetStream)
            val safeName = row[EhsDocumentTable.fileName].replace("\"", "'")
            call.response.header(HttpHeaders.ContentDisposition, "inline; filename=\"$safeName\"")
            call.respondBytes(bytes, contentType)
        }

        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "ID invalido")
                    )
                val storedContent = DatabaseFactory.dbQuery {
                    EhsDocumentTable
                        .select(EhsDocumentTable.contentBase64)
                        .where { EhsDocumentTable.id eq id }
                        .singleOrNull()
                        ?.get(EhsDocumentTable.contentBase64)
                } ?: return@safeApiCall call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("status" to "error", "message" to "Evidencia no encontrada")
                )

                // Primero confirmamos que Supabase acepta la eliminacion. Luego
                // limpiamos Drive; asi una BD en solo-lectura no rompe el archivo.
                DatabaseFactory.dbQuery { EhsDocumentTable.deleteWhere { EhsDocumentTable.id eq id } }
                if (storedContent.startsWith(DRIVE_POINTER_PREFIX)) {
                    val driveDeleted = GoogleDriveService.deleteFile(storedContent.removePrefix(DRIVE_POINTER_PREFIX))
                    if (!driveDeleted) println("[EhsDocumentRoutes] Aviso: no se pudo borrar el archivo de Drive para evidencia $id")
                }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}

private suspend fun loadDocumentMetadata(
    categoria: String?,
    moduleType: String?,
    moduleRecordId: Int?,
    includeModuleLink: Boolean
): List<EhsDocument> = DatabaseFactory.dbQuery {
    val columns = mutableListOf<Expression<*>>(
        EhsDocumentTable.id,
        EhsDocumentTable.categoria,
        EhsDocumentTable.titulo,
        EhsDocumentTable.fecha,
        EhsDocumentTable.fileName,
        EhsDocumentTable.mimeType,
        EhsDocumentTable.fileSize,
        EhsDocumentTable.notas,
        EhsDocumentTable.uploadedBy,
        EhsDocumentTable.uploadedDate
    )
    if (includeModuleLink) {
        columns += EhsDocumentTable.moduleType
        columns += EhsDocumentTable.moduleRecordId
    }
    var query = EhsDocumentTable.select(columns)
    if (!categoria.isNullOrBlank()) query = query.andWhere { EhsDocumentTable.categoria eq categoria }
    if (includeModuleLink && !moduleType.isNullOrBlank()) query = query.andWhere { EhsDocumentTable.moduleType eq moduleType }
    if (includeModuleLink && moduleRecordId != null) query = query.andWhere { EhsDocumentTable.moduleRecordId eq moduleRecordId }
    query.orderBy(EhsDocumentTable.id, SortOrder.DESC).map {
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
            uploadedDate = it[EhsDocumentTable.uploadedDate],
            moduleType = if (includeModuleLink) it[EhsDocumentTable.moduleType] else "",
            moduleRecordId = if (includeModuleLink) it[EhsDocumentTable.moduleRecordId] else 0
        )
    }
}

private suspend fun insertDocument(
    req: EhsDocumentUpload,
    fileName: String,
    mimeType: String,
    size: Int,
    uploadedBy: String,
    uploadedDate: String,
    pointer: String,
    includeModuleLink: Boolean
): Int = DatabaseFactory.dbQuery {
    EhsDocumentTable.insert {
        it[categoria] = req.categoria.ifBlank { "Otro" }
        it[titulo] = req.titulo.trim()
        it[fecha] = req.fecha
        it[EhsDocumentTable.fileName] = fileName
        it[EhsDocumentTable.mimeType] = mimeType
        it[fileSize] = size
        it[notas] = req.notas
        it[EhsDocumentTable.uploadedBy] = uploadedBy
        it[EhsDocumentTable.uploadedDate] = uploadedDate
        if (includeModuleLink) {
            it[moduleType] = req.moduleType.trim().lowercase()
            it[moduleRecordId] = req.moduleRecordId
        }
        it[contentBase64] = pointer
    } get EhsDocumentTable.id
}

private data class LegacyDocument(val id: Int, val fileName: String, val mimeType: String, val base64: String)
