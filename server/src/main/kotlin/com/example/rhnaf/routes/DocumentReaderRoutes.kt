package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.service.DocumentReaderService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.ByteArrayOutputStream

/** Preview-only extraction. Nothing is stored in Drive or the database. */
fun Route.documentReaderRouting() {
    post("/api/v1/documentos/extraer") {
        requireRoleOr403(call, Roles.ALL) ?: return@post
        val maxBytes = 15 * 1024 * 1024
        var bytes: ByteArray? = null
        var name = ""
        var invalid = false
        call.receiveMultipart(formFieldLimit = maxBytes.toLong()).forEachPart { part ->
            try {
                if (part is PartData.FileItem) {
                    if (bytes != null || invalid) { invalid = true; return@forEachPart }
                    name = (part.originalFileName ?: "").substringAfterLast('/').substringAfterLast('\\')
                    val output = ByteArrayOutputStream()
                    part.streamProvider().use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            if (output.size() + n > maxBytes) { invalid = true; break }
                            output.write(buffer, 0, n)
                        }
                    }
                    if (!invalid) bytes = output.toByteArray()
                }
            } finally { part.dispose() }
        }
        val content = bytes
        if (invalid || content == null || content.isEmpty() || name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Adjunta un solo documento de hasta 15 MiB"))
            return@post
        }
        try {
            val rows = DocumentReaderService.extractRows(content, name)
            call.respond(mapOf(
                "fileName" to name,
                "rows" to rows.take(300),
                "totalRows" to rows.size,
                "truncated" to (rows.size > 300),
                "message" to "Extracción preliminar. Verifica los datos antes de registrar; no se guardó ningún archivo ni registro."
            ))
        } catch (e: DocumentReaderService.UnsupportedDocumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Formato no soportado")))
        } catch (e: Exception) {
            application.log.warn("No se pudo leer documento: $name", e)
            call.respond(HttpStatusCode.UnprocessableEntity, mapOf("message" to "No se pudo extraer texto de este archivo. Si es un PDF escaneado, requiere OCR."))
        }
    }
}
