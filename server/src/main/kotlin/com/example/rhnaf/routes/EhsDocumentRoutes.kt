package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireAuthOr401
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsDocumentTable
import com.example.rhnaf.database.SafetyInspectionTable
import com.example.rhnaf.database.SafetyIncidentTable
import com.example.rhnaf.database.WorkPermitTable
import com.example.rhnaf.database.PpeDeliveryTable
import com.example.rhnaf.database.SafetyTrainingTable
import com.example.rhnaf.database.EmergencyDrillTable
import com.example.rhnaf.database.RiskMatrixTable
import com.example.rhnaf.database.EnvironmentalWasteTable
import com.example.rhnaf.database.OccupationalHealthTable
import com.example.rhnaf.database.ChemicalInventoryTable
import com.example.rhnaf.database.Dc3ConstanciaTable
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.io.File
import java.io.FileOutputStream
import io.ktor.http.content.*

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
            val role = requireAuthOr401(call) ?: return@get
            val categoria = call.request.queryParameters["categoria"]
            val moduleType = call.request.queryParameters["moduleType"]
            val moduleRecordId = call.request.queryParameters["moduleRecordId"]?.toIntOrNull()
            if (categoria == "ExamenMedico" && role !in Roles.EHS_WRITE) {
                call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Acceso reservado a Seguridad"))
                return@get
            }

            // La BD de produccion puede seguir con el esquema anterior, sin
            // module_type/module_record_id. Intentamos el esquema nuevo y, si
            // esas columnas aun no existen, servimos los metadatos legacy.
            val items = try {
                loadDocumentMetadata(categoria, moduleType, moduleRecordId, includeModuleLink = true, excludeMedical = role !in Roles.EHS_WRITE)
            } catch (e: Exception) {
                println("[EhsDocumentRoutes] Esquema legacy detectado al listar: ${e.message}")
                loadDocumentMetadata(categoria, null, null, includeModuleLink = false, excludeMedical = role !in Roles.EHS_WRITE)
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

                // La ruta JSON antigua también respeta Normativa/año. Si el cliente
                // no proporciona año ni fecha, rechazamos para no falsear históricos.
                val documentYear = req.anio.takeIf { it == -1 || it in 1900..2100 }
                    ?: runCatching { LocalDate.parse(req.fecha, DateTimeFormatter.ofPattern("dd/MM/uuuu")).year }.getOrNull()
                if (documentYear == null || (documentYear != -1 && documentYear !in 1900..2100) ||
                    documentYear == -1 && req.fecha.isNotBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "Indica el anio documental antes de subir"))
                }
                val categoriaFolder = req.categoria.ifBlank { "Otro" }
                val oldFolderName = if (documentYear == -1) "General" else documentYear.toString()
                val folderId = GoogleDriveService.categoryYearFolder(categoriaFolder, documentYear)
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                        mapOf("status" to "error", "message" to "No se pudo crear $categoriaFolder/$oldFolderName"))
                val safeFileName = req.fileName.ifBlank { "evidencia" }
                val mimeType = req.mimeType.ifBlank { "application/octet-stream" }
                val driveFileId = GoogleDriveService.uploadFile(bytes, safeFileName, mimeType, folderId)
                    ?: return@safeApiCall call.respond(
                        HttpStatusCode.BadGateway,
                        mapOf("status" to "error", "message" to "Google Drive no pudo guardar el archivo")
                    )

                val today = LocalDate.now(ZoneId.of("America/Mexico_City")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val uploadedBy = call.request.header(HttpHeaders.Authorization)
                    ?.removePrefix("Bearer ")?.trim().orEmpty()
                val pointer = "$DRIVE_POINTER_PREFIX$driveFileId"

                try {
                    val id = try {
                        insertDocument(req, safeFileName, mimeType, bytes.size, uploadedBy, today, pointer, includeModuleLink = true, year = documentYear)
                    } catch (e: Exception) {
                        println("[EhsDocumentRoutes] Insert con esquema nuevo fallo; intentando legacy: ${e.message}")
                        insertDocument(req, safeFileName, mimeType, bytes.size, uploadedBy, today, pointer, includeModuleLink = false, year = documentYear)
                    }
                    call.respond(mapOf("status" to "ok", "id" to id.toString(), "storage" to "google_drive"))
                } catch (e: Exception) {
                    // Evitar archivos huerfanos si Supabase rechaza el registro.
                    GoogleDriveService.deleteFile(driveFileId)
                    throw e
                }
            }
        }

        // Carga grande por multipart: hasta 500 MiB por archivo. El body pasa a
        // un fichero temporal (no base64 ni ByteArray gigante) antes de Drive.
        post("/subir-archivo") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                if (!GoogleDriveService.isConfigured()) {
                    return@safeApiCall call.respond(HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "error", "message" to "Google Drive aun no esta conectado"))
                }
                val temp = File.createTempFile("rhnaf-evidence-", ".upload")
                try {
                    val fields = mutableMapOf<String, String>()
                    var fileName = ""
                    var mimeType = "application/octet-stream"
                    var fileSize = 0L
                    var invalidFile = false
                    var filesSeen = 0
                    call.receiveMultipart(formFieldLimit = 500L * 1024 * 1024).forEachPart { part ->
                        try {
                            when (part) {
                                is PartData.FormItem -> if (part.name != null) fields[part.name!!] = part.value
                                is PartData.FileItem -> {
                                    filesSeen++
                                    if (filesSeen > 1) { invalidFile = true; return@forEachPart }
                                    fileName = (part.originalFileName ?: "evidencia")
                                        .substringAfterLast('/').substringAfterLast('\\').take(300)
                                    mimeType = part.contentType?.toString()?.take(100) ?: "application/octet-stream"
                                    part.streamProvider().use { input ->
                                        FileOutputStream(temp).use { output ->
                                            val buffer = ByteArray(1024 * 1024)
                                            while (true) {
                                                val n = input.read(buffer)
                                                if (n < 0) break
                                                fileSize += n
                                                if (fileSize > 500L * 1024 * 1024) { invalidFile = true; break }
                                                output.write(buffer, 0, n)
                                            }
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        } finally { part.dispose() }
                    }
                    val year = fields["anio"]?.toIntOrNull()
                    val title = fields["titulo"]?.trim().orEmpty()
                    val date = fields["fecha"].orEmpty()
                    if (invalidFile || fileSize == 0L || filesSeen != 1 || title.isBlank() ||
                        year == null || (year != -1 && year !in 1900..2100) || date.isNotBlank() &&
                        (year == -1 || runCatching { LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/uuuu")) }
                            .getOrNull()?.year != year)) {
                        return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                            mapOf("status" to "error", "message" to "Archivo (max. 500 MiB), titulo y anio validos obligatorios; la fecha debe coincidir con el anio"))
                    }
                    val categoriaFolder = fields["categoria"]?.trim().orEmpty().ifBlank { "Otro" }
                    val folderName = if (year == -1) "General" else year.toString()
                    val folder = GoogleDriveService.categoryYearFolder(categoriaFolder, year)
                        ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                            mapOf("status" to "error", "message" to "No fue posible encontrar o crear $categoriaFolder/$folderName en Drive"))
                    val driveId = GoogleDriveService.uploadLargeFile(temp, fileName, mimeType, folder)
                        ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                            mapOf("status" to "error", "message" to "Drive no confirmo la subida del archivo"))
                    val today = LocalDate.now(ZoneId.of("America/Mexico_City")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    val uploadedBy = call.request.header(HttpHeaders.Authorization)
                        ?.removePrefix("Bearer ")?.trim().orEmpty()
                    val req = EhsDocumentUpload(
                        categoria = fields["categoria"].orEmpty().ifBlank { "Otro" }, titulo = title,
                        fecha = date, notas = fields["notas"].orEmpty(),
                        moduleType = fields["moduleType"].orEmpty(),
                        moduleRecordId = fields["moduleRecordId"]?.toIntOrNull() ?: 0,
                        contentBase64 = ""
                    )
                    try {
                        val id = insertDocument(req, fileName, mimeType, fileSize.toInt(), uploadedBy,
                            today, "$DRIVE_POINTER_PREFIX$driveId", includeModuleLink = true, year = year)
                        call.respond(mapOf("status" to "ok", "id" to id.toString(),
                            "anio" to year.toString(), "folder" to "$categoriaFolder/$folderName"))
                    } catch (e: Exception) {
                        GoogleDriveService.deleteFile(driveId)
                        throw e
                    }
                } finally { temp.delete() }
            }
        }

        // Reclasificar un archivo existente sin duplicarlo ni perder el vínculo
        // gdrive:<id>. La categoría debe ser elegida explícitamente para evitar
        // adivinar a partir de nombres ambiguos o fotografías sin descripción.
        patch("/{id}/categoria") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("message" to "ID inválido"))
                val categoria = call.receive<Map<String, String>>()["categoria"]?.trim().orEmpty()
                val allowed = setOf("Inspeccion", "Capacitacion", "Simulacro", "Estudio", "Dictamen",
                    "ExamenMedico", "Incidente", "PermisoTrabajo", "EPP", "Residuos", "Riesgos",
                    "Quimicos", "Auditoria", "Normativa", "DC3", "Otro")
                if (categoria !in allowed) return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                    mapOf("message" to "Categoría desconocida"))
                val row = DatabaseFactory.dbQuery {
                    EhsDocumentTable.selectAll().where { EhsDocumentTable.id eq id }.singleOrNull()
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("message" to "Evidencia inexistente"))
                val previous = row[EhsDocumentTable.categoria]
                if (previous == categoria) return@safeApiCall call.respond(mapOf("status" to "ok"))
                val pointer = row[EhsDocumentTable.contentBase64]
                if (!pointer.startsWith(DRIVE_POINTER_PREFIX)) return@safeApiCall call.respond(
                    HttpStatusCode.Conflict, mapOf("message" to "Migra el archivo antiguo a Drive antes de reclasificar"))
                val year = row[EhsDocumentTable.anio]
                val target = GoogleDriveService.categoryYearFolder(categoria, year)
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                        mapOf("message" to "No se pudo crear la carpeta de destino"))
                val fileId = pointer.removePrefix(DRIVE_POINTER_PREFIX)
                val oldParent = GoogleDriveService.moveFile(fileId, target)
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                        mapOf("message" to "No se pudo mover el archivo en Drive"))
                try {
                    DatabaseFactory.dbQuery {
                        val changed = EhsDocumentTable.update({ (EhsDocumentTable.id eq id) and
                            (EhsDocumentTable.categoria eq previous) }) {
                            it[EhsDocumentTable.categoria] = categoria
                            it[EhsDocumentTable.moduleType] = ""
                            it[EhsDocumentTable.moduleRecordId] = 0
                        }
                        check(changed == 1) { "La evidencia cambió durante la operación" }
                    }
                } catch (e: Exception) {
                    if (oldParent != target) GoogleDriveService.moveFile(fileId, oldParent)
                    throw e
                }
                call.respond(mapOf("status" to "ok", "categoria" to categoria))
            }
        }

        // Completa manualmente el año/fecha documental cuando el archivo no lo
        // trae (p. ej. evidencias en la carpeta "General"). Sin esto, esas
        // evidencias nunca pueden vincularse automaticamente a un registro
        // porque el sistema nunca inventa una fecha que el documento no tiene.
        patch("/{id}/fecha") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("message" to "ID inválido"))
                val body = call.receive<Map<String, String>>()
                val anio = body["anio"]?.trim()?.toIntOrNull()
                val fecha = body["fecha"]?.trim().orEmpty()
                if ((anio == null || anio < 2000 || anio > 2100) && fecha.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("message" to "Indica un año valido (2000-2100) o una fecha"))
                }
                val exists = DatabaseFactory.dbQuery {
                    EhsDocumentTable.selectAll().where { EhsDocumentTable.id eq id }.singleOrNull()
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("message" to "Evidencia inexistente"))
                DatabaseFactory.dbQuery {
                    EhsDocumentTable.update({ EhsDocumentTable.id eq id }) {
                        if (anio != null) it[EhsDocumentTable.anio] = anio
                        if (fecha.isNotBlank()) it[EhsDocumentTable.fecha] = fecha
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // Vincula una evidencia ya subida con un registro estructurado (p.ej. un
        // quimico, una inspeccion) sin volver a subir el archivo. Idempotente:
        // reintentar con el mismo destino no falla.
        patch("/{id}/vincular") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("message" to "ID inválido"))
                val body = call.receive<Map<String, String>>()
                val moduleType = body["moduleType"]?.trim().orEmpty()
                val moduleRecordId = body["moduleRecordId"]?.toIntOrNull()
                if (moduleType.isBlank() || moduleRecordId == null || moduleRecordId <= 0) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("message" to "moduleType y moduleRecordId (>0) son obligatorios"))
                }
                val exists = DatabaseFactory.dbQuery {
                    EhsDocumentTable.selectAll().where { EhsDocumentTable.id eq id }.singleOrNull()
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("message" to "Evidencia inexistente"))
                DatabaseFactory.dbQuery {
                    EhsDocumentTable.update({ EhsDocumentTable.id eq id }) {
                        it[EhsDocumentTable.moduleType] = moduleType
                        it[EhsDocumentTable.moduleRecordId] = moduleRecordId
                    }
                }
                call.respond(mapOf("status" to "ok"))
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
            val role = requireAuthOr401(call) ?: return@get
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

            if (row[EhsDocumentTable.categoria] == "ExamenMedico" && role !in Roles.EHS_WRITE) {
                call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Acceso reservado a Seguridad"))
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

        // Vincular manualmente cualquier evidencia (con o sin registro previo)
        // a cualquier registro EHS existente. Antes solo se creaba el vinculo
        // automaticamente al generar el registro desde Drive; el usuario debe
        // poder corregirlo o completarlo a mano en cualquier momento.
        post("/{id}/vincular") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID invalido"))
                val req = call.receive<com.example.rhnaf.shared.model.EhsDocumentLinkRequest>()

                if (req.moduleRecordId == 0) {
                    val updated = DatabaseFactory.dbQuery {
                        EhsDocumentTable.update({ EhsDocumentTable.id eq id }) {
                            it[EhsDocumentTable.moduleType] = ""
                            it[EhsDocumentTable.moduleRecordId] = 0
                        }
                    }
                    if (updated == 0) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Evidencia no encontrada"))
                    return@safeApiCall call.respond(mapOf("status" to "ok"))
                }

                val existeDestino = DatabaseFactory.dbQuery {
                    when (req.moduleType) {
                        "inspection" -> SafetyInspectionTable.selectAll().where { SafetyInspectionTable.id eq req.moduleRecordId }.limit(1).any()
                        "incident" -> SafetyIncidentTable.selectAll().where { SafetyIncidentTable.id eq req.moduleRecordId }.limit(1).any()
                        "permit" -> WorkPermitTable.selectAll().where { WorkPermitTable.id eq req.moduleRecordId }.limit(1).any()
                        "ppe" -> PpeDeliveryTable.selectAll().where { PpeDeliveryTable.id eq req.moduleRecordId }.limit(1).any()
                        "training" -> SafetyTrainingTable.selectAll().where { SafetyTrainingTable.id eq req.moduleRecordId }.limit(1).any()
                        "drill" -> EmergencyDrillTable.selectAll().where { EmergencyDrillTable.id eq req.moduleRecordId }.limit(1).any()
                        "risk" -> RiskMatrixTable.selectAll().where { RiskMatrixTable.id eq req.moduleRecordId }.limit(1).any()
                        "waste" -> EnvironmentalWasteTable.selectAll().where { EnvironmentalWasteTable.id eq req.moduleRecordId }.limit(1).any()
                        "health" -> OccupationalHealthTable.selectAll().where { OccupationalHealthTable.id eq req.moduleRecordId }.limit(1).any()
                        "chemical" -> ChemicalInventoryTable.selectAll().where { ChemicalInventoryTable.id eq req.moduleRecordId }.limit(1).any()
                        "dc3" -> Dc3ConstanciaTable.selectAll().where { Dc3ConstanciaTable.id eq req.moduleRecordId }.limit(1).any()
                        else -> false
                    }
                }
                if (!existeDestino) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "El registro destino no existe"))
                }

                val updated = DatabaseFactory.dbQuery {
                    EhsDocumentTable.update({ EhsDocumentTable.id eq id }) {
                        it[EhsDocumentTable.moduleType] = req.moduleType
                        it[EhsDocumentTable.moduleRecordId] = req.moduleRecordId
                    }
                }
                if (updated == 0) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Evidencia no encontrada"))
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}

private suspend fun loadDocumentMetadata(
    categoria: String?,
    moduleType: String?,
    moduleRecordId: Int?,
    includeModuleLink: Boolean,
    excludeMedical: Boolean = false
): List<EhsDocument> = DatabaseFactory.dbQuery {
    val columns = mutableListOf<Expression<*>>(
        EhsDocumentTable.id,
        EhsDocumentTable.categoria,
        EhsDocumentTable.titulo,
        EhsDocumentTable.fecha,
        EhsDocumentTable.anio,
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
    if (excludeMedical) query = query.andWhere { EhsDocumentTable.categoria neq "ExamenMedico" }
    if (includeModuleLink && !moduleType.isNullOrBlank()) query = query.andWhere { EhsDocumentTable.moduleType eq moduleType }
    if (includeModuleLink && moduleRecordId != null) query = query.andWhere { EhsDocumentTable.moduleRecordId eq moduleRecordId }
    query.orderBy(EhsDocumentTable.id, SortOrder.DESC).map {
        EhsDocument(
            id = it[EhsDocumentTable.id],
            categoria = it[EhsDocumentTable.categoria],
            titulo = it[EhsDocumentTable.titulo],
            fecha = it[EhsDocumentTable.fecha],
            anio = it[EhsDocumentTable.anio],
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
    includeModuleLink: Boolean,
    year: Int = 0
): Int = DatabaseFactory.dbQuery {
    EhsDocumentTable.insert {
        it[categoria] = req.categoria.ifBlank { "Otro" }
        it[titulo] = req.titulo.trim()
        it[fecha] = req.fecha
        it[anio] = year
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
