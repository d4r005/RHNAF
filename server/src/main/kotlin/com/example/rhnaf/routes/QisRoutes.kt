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
import com.example.rhnaf.service.GoogleDriveService
import io.ktor.server.request.receiveMultipart
import io.ktor.http.content.*
import java.io.File
import java.util.concurrent.TimeUnit
import java.io.FileOutputStream
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.text.SimpleDateFormat

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


// ==================== helpers de importacion del listado maestro ====================

private fun hoyComoTexto(): String = java.time.LocalDate.now().let { "%02d/%02d/%04d".format(it.dayOfMonth, it.monthValue, it.year) }

private data class FilaQis(
    val codigo: String, val titulo: String, val tipo: String, val proceso: String,
    val version: String, val fechaEmision: String, val proximaRevision: String,
    val estado: String, val elaboradoPor: String, val aprobadoPor: String, val notas: String
)

/** Celda de Excel a texto; fechas con formato dd/MM/yyyy y numeros sin .0. */
private fun celdaATexto(cell: org.apache.poi.ss.usermodel.Cell?): String {
    if (cell == null) return ""
    return when (cell.cellType) {
        CellType.STRING -> cell.stringCellValue.trim()
        CellType.NUMERIC -> {
            if (DateUtil.isCellDateFormatted(cell)) {
                SimpleDateFormat("dd/MM/yyyy").format(cell.dateCellValue)
            } else {
                val d = cell.numericCellValue
                if (d == Math.floor(d) && !d.isInfinite() && Math.abs(d) < 1.0E12) d.toLong().toString() else d.toString()
            }
        }
        CellType.BOOLEAN -> cell.booleanCellValue.toString()
        else -> ""
    }
}

/** Ajusta el texto de version a formato N.N (1 -> 1.0; "3" -> "3.0"; "2.3" se respeta). */
private fun versionDesde(texto: String): String {
    val t = texto.trim().removeSuffix(".0").ifBlank { "1" }
    val m = Regex("""^(\d{1,3})$""").find(t)
    return if (m != null) "${t}.0" else t.take(12)
}

/** Normaliza el tipo de documento contra los tipos validos de QIS. */
private fun tipoDesde(texto: String): String {
    val t = texto.trim().lowercase()
    return when {
        t.contains("procedim") || t.contains("procedure") -> "Procedimiento"
        t.contains("instruct") || t.contains("work instr") || t.contains("wi") -> "Instructivo"
        t.contains("format") || t.contains("form") || t.contains("check") || t.contains("registro de") -> if (t.contains("registro")) "Registro" else "Formato"
        t.contains("polit") || t.contains("polic") -> "Politica"
        t.contains("manual") -> "Manual"
        t.contains("plan") -> "Plan"
        t.contains("registro") -> "Registro"
        else -> if (t.isBlank()) "Procedimiento" else "Otro"
    }
}

/** Normaliza el estado (Vigente/EnRevision/Obsoleto); vacio = Vigente. */
private fun estadoDesde(texto: String): String {
    val t = texto.trim().lowercase().replace(" ", "").replace("revisión", "revision").replace("á", "a").replace("é", "e")
    return when {
        t.contains("revision") -> "EnRevision"
        t.contains("obsole") || t.contains("cancel") || t.contains("baja") -> "Obsoleto"
        else -> "Vigente"
    }
}

/**
 * Lee el listado maestro de documentos controlados desde un Excel, con
 * deteccion flexible de columnas: busca en la primera hoja la fila de
 * encabezados (primera con >= 3 celdas de texto) y empareja cada columna
 * por palabras clave (codigo, titulo/documento, tipo, proceso, version,
 * emision, revision/vigencia, elaboro, aprobo, notas). Solo devuelve las
 * filas con codigo y titulo validos.
 */
private fun leerListadoMaestroQis(file: File): List<FilaQis> {
    WorkbookFactory.create(file).use { wb ->
        val hoja = wb.getSheetAt(0) ?: return emptyList()
        var filaHeaders = -1
        var cols: Map<String, Int> = emptyMap()
        for (r in 0 until hoja.lastRowNum.coerceAtLeast(0)) {
            val row = hoja.getRow(r) ?: continue
            val textos = (0 until row.lastCellNum.coerceAtLeast(0)).map { celdaATexto(row.getCell(it)).lowercase() }
            if (textos.count { it.isNotBlank() } < 3) continue
            val map = mutableMapOf<String, Int>()
            textos.forEachIndexed { i, h ->
                when {
                    (h.contains("codigo") || h.contains("código") || h.contains("clave") || h == "code" || h.contains("no.")) && !cols.containsKey("codigo") && !map.containsKey("codigo") -> map["codigo"] = i
                    (h.contains("titulo") || h.contains("título") || h.contains("documento") || h.contains("nombre") || h.contains("descripcion")) && !map.containsKey("titulo") -> map["titulo"] = i
                    h.contains("tipo") && !map.containsKey("tipo") -> map["tipo"] = i
                    h.contains("proceso") && !map.containsKey("proceso") -> map["proceso"] = i
                    h.contains("version") || h.contains("versi") || h.contains("rev.") || h.contains("revision actual") -> if (!map.containsKey("version")) map["version"] = i
                    (h.contains("emisi") || h.contains("emission") || h.contains("fecha alta") || h.contains("fecha de alta")) && !map.containsKey("emision") -> map["emision"] = i
                    (h.contains("revision") || h.contains("vencim") || h.contains("vigenc") || h.contains("proximo") || h.contains("próximo") || h.contains("proxima") || h.contains("próxima")) && !map.containsKey("revision") -> map["revision"] = i
                    h.contains("elabor") && !map.containsKey("elaborado") -> map["elaborado"] = i
                    h.contains("aprob") && !map.containsKey("aprobado") -> map["aprobado"] = i
                    (h.contains("nota") || h.contains("observ")) && !map.containsKey("notas") -> map["notas"] = i
                }
            }
            if (map.containsKey("codigo") && map.containsKey("titulo")) {
                filaHeaders = r; cols = map; break
            }
        }
        if (filaHeaders == -1) return emptyList()

        val out = mutableListOf<FilaQis>()
        for (r in (filaHeaders + 1)..hoja.lastRowNum) {
            val row = hoja.getRow(r) ?: continue
            fun col(k: String) = cols[k]?.let { celdaATexto(row.getCell(it)) } ?: ""
            val codigo = col("codigo").take(40)
            val titulo = col("titulo").take(300)
            if (codigo.isBlank() || titulo.isBlank()) continue
            // ignora filas de totales o repetidas de encabezado
            if (codigo.lowercase().contains("codigo") || codigo.lowercase().contains("código")) continue
            out.add(FilaQis(
                codigo = codigo,
                titulo = titulo,
                tipo = tipoDesde(col("tipo")),
                proceso = col("proceso").take(200),
                version = versionDesde(col("version")),
                fechaEmision = col("emision").take(20),
                proximaRevision = col("revision").take(20),
                estado = estadoDesde(col("estado")),
                elaboradoPor = col("elaborado").take(200),
                aprobadoPor = col("aprobado").take(200),
                notas = col("notas").take(500)
            ))
        }
        return out
    }
}

// ---------- VER COMO PDF ----------
// LibreOffice convierte Word/Excel/PowerPoint a PDF para verlo en el
// navegador; los PDF se entregan tal cual. Cada conversion corre con su
// propio perfil temporal para no chocar entre peticiones concurrentes.
private val EXT_CONVERTIBLES = setOf("docx", "doc", "xlsx", "xls", "pptx", "ppt", "txt", "csv", "odt", "ods", "odp")

// Word deja ambigua la fuente de los encabezados con chino
// (w:asciiTheme="minorEastAsia" con el tema de Asia vacio): Word los
// renderiza con el latino compacto de SimSun, pero LibreOffice resuelve
// otra cosa (DejaVu, ancha) y rompe el acomodo. En la COPIA TEMPORAL de
// conversion se fijan fuentes explicitas de metrica Times, que es como
// Word los ve. El archivo original en Drive no se toca.
private fun rematarFuentesAmbiguas(bytes: ByteArray): ByteArray {
    return try {
        val zin = java.util.zip.ZipInputStream(bytes.inputStream())
        val salida = java.io.ByteArrayOutputStream()
        val zout = java.util.zip.ZipOutputStream(salida)
        zin.use { entrada ->
            zout.use { salidaZip ->
                var e = entrada.nextEntry
                while (e != null) {
                    val contenido = entrada.readBytes()
                    val nuevo = if (e.name.endsWith(".xml")) {
                        String(contenido, Charsets.UTF_8)
                            .replace(
                                """<w:rFonts w:asciiTheme="minorEastAsia" w:hAnsiTheme="minorEastAsia" w:hint="eastAsia"/>""",
                                """<w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="SimSun" w:hint="eastAsia"/>"""
                            )
                            .replace(Regex("""w:asciiTheme="minorEastAsia""""), """w:ascii="Times New Roman"""")
                            .replace(Regex("""w:hAnsiTheme="minorEastAsia""""), """w:hAnsi="Times New Roman"""")
                            .toByteArray(Charsets.UTF_8)
                    } else contenido
                    salidaZip.putNextEntry(java.util.zip.ZipEntry(e.name))
                    salidaZip.write(nuevo)
                    salidaZip.closeEntry()
                    e = entrada.nextEntry
                }
            }
        }
        salida.toByteArray()
    } catch (ex: Exception) {
        println("[QisRoutes] No se pudo rematar fuentes ambiguas (se convierte tal cual): ${ex.message}")
        bytes
    }
}

// Detecta el formato REAL por contenido (no por nombre): hay gafetes
// guardados como ".docx" que por dentro son PowerPoint, y LibreOffice se
// guia por la extension del archivo temporal.
private fun extReal(bytes: ByteArray, extDeclarada: String): String {
    if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) return extDeclarada
    return try {
        val zin = java.util.zip.ZipInputStream(bytes.inputStream())
        zin.use { z ->
            val primera = z.nextEntry?.name ?: return extDeclarada
            when {
                primera.startsWith("ppt/") -> "pptx"
                primera.startsWith("word/") -> "docx"
                primera.startsWith("xl/") -> "xlsx"
                else -> extDeclarada
            }
        }
    } catch (e: Exception) { extDeclarada }
}

private fun convertirAPdf(bytesIn: ByteArray, extDeclarada: String): ByteArray? {
    val ext = extReal(bytesIn, extDeclarada)
    if (ext !in EXT_CONVERTIBLES) return null
    val bytes = if (ext == "docx") rematarFuentesAmbiguas(bytesIn) else bytesIn
    val dir = File.createTempFile("qisconv-", "").let { f -> f.delete(); File(f.absolutePath + "d") }
    return try {
        dir.mkdirs()
        val home = File(dir, "home").apply { mkdirs() }
        val entrada = File(dir, "doc.$ext")
        entrada.writeBytes(bytes)
        val proc = ProcessBuilder(
            "soffice", "--headless", "--norestore", "--nologo", "--nolockcheck",
            "--convert-to", "pdf", "--outdir", dir.absolutePath, entrada.absolutePath
        ).apply {
            environment()["HOME"] = home.absolutePath
            redirectErrorStream(true)
        }.start()
        proc.waitFor(120, TimeUnit.SECONDS)
        val pdf = File(dir, "doc.pdf")
        if (pdf.exists() && pdf.length() > 0) pdf.readBytes() else null
    } catch (e: Exception) {
        println("[QisRoutes] Error convirtiendo a PDF ($ext): ${e.message}")
        null
    } finally {
        dir.deleteRecursively()
    }
}

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
                val newId = DatabaseFactory.dbQuery {
                    val nid = QisDocumentTable.insert {
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
                        it[documentoId] = nid
                        it[versionAnterior] = "-"
                        it[versionNueva] = "1.0"
                        it[fechaCambio] = req.fechaEmision
                        it[descripcion] = "Alta del documento"
                        it[cambiadoPor] = req.elaboradoPor
                        it[aprobadoPor] = req.aprobadoPor
                    }
                    nid
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok", "id" to newId.toString()))
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


        // ---------- ARCHIVO DEL DOCUMENTO (Google Drive) ----------
        //
        // Sube el documento controlado como tal (PDF, Word, Excel...) desde
        // el propio modulo QIS y lo almacena en Drive bajo QIS/<año>. El
        // registro guarda un puntero gdrive:<id>; el archivo nunca se borra
        // al subir una version nueva: cada version queda respaldada en Drive
        // y el historial de cambios conserva la trazabilidad.
        post("/{id}/archivo") {
            safeApiCall(call) {
                requireRoleOr403(call, QIS_WRITE) ?: return@safeApiCall
                if (!GoogleDriveService.isConfigured()) {
                    return@safeApiCall call.respond(HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "error", "message" to "Google Drive aun no esta conectado; configura la conexion en Ajustes"))
                }
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID invalido"))
                val doc = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.id eq id }.singleOrNull()?.let(::docFrom)
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))

                var fileName = ""
                var mimeType = "application/octet-stream"
                var bytes = ByteArray(0)
                var seen = 0
                val temp = File.createTempFile("rhnaf-qis-", ".upload")
                try {
                    call.receiveMultipart(formFieldLimit = 500L * 1024 * 1024).forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                if (seen++ > 0) return@forEachPart
                                fileName = (part.originalFileName ?: "documento")
                                    .substringAfterLast('/').substringAfterLast('\\').take(200)
                                mimeType = part.contentType?.toString() ?: "application/octet-stream"
                                part.streamProvider().copyTo(FileOutputStream(temp))
                            }
                            else -> {}
                        }
                        part.dispose()
                    }
                    bytes = temp.readBytes()
                } finally {
                    temp.delete()
                }
                if (bytes.isEmpty()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Archivo vacio o no recibido"))
                }

                val anio = doc.fechaEmision.substringAfterLast('/').take(4).toIntOrNull()?.takeIf { it in 1900..2100 } ?: java.time.Year.now().value
                val folder = GoogleDriveService.categoryYearFolder("QIS", anio)
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                        mapOf("status" to "error", "message" to "No fue posible crear la carpeta QIS/$anio en Drive"))
                val nombreEnDrive = "${doc.codigo}_v${doc.versionActual}_${fileName}"
                val fileId = GoogleDriveService.uploadFile(bytes, nombreEnDrive, mimeType, folder)
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                        mapOf("status" to "error", "message" to "No fue posible subir el archivo a Drive"))
                val pointer = "gdrive:$fileId::${fileName}"
                DatabaseFactory.dbQuery {
                    QisDocumentTable.update({ QisDocumentTable.id eq id }) { it[urlArchivo] = pointer }
                }
                call.respond(mapOf("status" to "ok", "url" to pointer))
            }
        }

        // Descarga / vista del archivo controlado. Si es un puntero de Drive
        // lo transmite desde ahi; si es una URL externa redirige.
        get("/{id}/pdf") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID invalido"))
                val doc = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.id eq id }.singleOrNull()?.let(::docFrom)
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))
                if (!doc.urlArchivo.startsWith("gdrive:")) {
                    return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "El documento no tiene archivo adjunto"))
                }
                val fileId = doc.urlArchivo.removePrefix("gdrive:").substringBefore("::")
                val nombreArchivo = doc.urlArchivo.substringAfter("::", "documento")
                val bytes = GoogleDriveService.downloadFile(fileId)
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway, mapOf("status" to "error", "message" to "No fue posible descargar el archivo desde Drive"))
                val ext = nombreArchivo.substringAfterLast('.', "").lowercase()
                val pdf = if (ext == "pdf") bytes else convertirAPdf(bytes, ext)
                if (pdf == null) {
                    return@safeApiCall call.respond(HttpStatusCode.UnprocessableEntity, mapOf("status" to "error", "message" to "No fue posible convertir el .$ext a PDF"))
                }
                call.response.headers.append(HttpHeaders.ContentDisposition, "inline; filename=\"${doc.codigo}.pdf\"")
                call.respondBytes(pdf, ContentType.Application.Pdf)
            }
        }

        get("/{id}/archivo") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID invalido"))
                val doc = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.id eq id }.singleOrNull()?.let(::docFrom)
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))
                val url = doc.urlArchivo
                when {
                    url.startsWith("gdrive:") -> {
                        val sinPrefijo = url.removePrefix("gdrive:")
                        val fileId = sinPrefijo.substringBefore("::")
                        val nombreArchivo = sinPrefijo.substringAfter("::", "documento_${doc.codigo}")
                            .ifBlank { "documento_${doc.codigo}" }
                        val descargado = GoogleDriveService.downloadFileWithType(fileId)
                            ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                                mapOf("status" to "error", "message" to "No fue posible descargar el archivo desde Drive"))
                        val (bytes, mime) = descargado
                        // filename ASCII de respaldo + filename* UTF-8: asi los
                        // nombres con caracteres chinos no se pierden como '????'.
                        val nombreAscii = nombreArchivo.replace("\"", "_").filter { it.code in 32..126 }.ifBlank { "documento_${doc.codigo}" }
                        val nombreUtf8 = java.net.URLEncoder.encode(nombreArchivo.replace("\"", "_"), "UTF-8").replace("+", "%20")
                        call.response.headers.append(HttpHeaders.ContentDisposition,
                            "attachment; filename=\"$nombreAscii\"; filename*=UTF-8''$nombreUtf8")
                        val ct = runCatching { ContentType.parse(mime) }.getOrDefault(ContentType.Application.OctetStream)
                        call.respondBytes(bytes, contentType = ct)
                    }
                    url.startsWith("http") -> call.respondRedirect(url)
                    else -> return@safeApiCall call.respond(HttpStatusCode.NotFound,
                        mapOf("status" to "error", "message" to "El documento no tiene archivo adjunto"))
                }
            }
        }


        // ---------- IMPORTAR LISTADO MAESTRO (Excel) ----------
        //
        // Sube el listado maestro de documentos controlados (.xlsx/.xls)
        // directamente desde el modulo QIS: da de alta en lote los
        // documentos que no existan (con su entrada de historial), omite
        // los que ya estan registrados (por codigo, sin duplicar) y guarda
        // el propio Excel como respaldo en Drive bajo QIS/<año>.
        post("/importar-listado") {
            safeApiCall(call) {
                requireRoleOr403(call, QIS_WRITE) ?: return@safeApiCall
                val temp = File.createTempFile("rhnaf-qis-import-", ".upload")
                var fileName = ""
                var mimeType = ""
                try {
                    var seen = 0
                    call.receiveMultipart(formFieldLimit = 500L * 1024 * 1024).forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                if (seen++ > 0) return@forEachPart
                                fileName = (part.originalFileName ?: "listado.xlsx")
                                    .substringAfterLast('/').substringAfterLast('\\').take(200)
                                mimeType = part.contentType?.toString()
                                    ?: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                part.streamProvider().copyTo(FileOutputStream(temp))
                            }
                            else -> {}
                        }
                        part.dispose()
                    }
                    if (!temp.exists() || temp.length() == 0L) {
                        return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                            mapOf("status" to "error", "message" to "Archivo vacio o no recibido"))
                    }
                    val filas = leerListadoMaestroQis(temp)
                    if (filas.isEmpty()) {
                        return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf(
                            "status" to "error",
                            "message" to "No se encontro ninguna fila valida: se requiere una columna de codigo y una de titulo"
                        ))
                    }

                    var creados = 0
                    var omitidos = 0
                    val yaExistentes = DatabaseFactory.dbQuery {
                        QisDocumentTable.selectAll().map { it[QisDocumentTable.codigo] }.toSet()
                    }
                    for (f in filas) {
                        if (f.codigo in yaExistentes) { omitidos++; continue }
                        DatabaseFactory.dbQuery {
                            val newId = QisDocumentTable.insert {
                                it[codigo] = f.codigo
                                it[titulo] = f.titulo
                                it[tipo] = f.tipo
                                it[proceso] = f.proceso
                                it[versionActual] = f.version
                                it[fechaEmision] = f.fechaEmision
                                it[proximaRevision] = f.proximaRevision
                                it[estado] = f.estado
                                it[elaboradoPor] = f.elaboradoPor
                                it[aprobadoPor] = f.aprobadoPor
                                it[urlArchivo] = ""
                                it[notas] = f.notas
                            } get QisDocumentTable.id
                            QisChangeLogTable.insert {
                                it[documentoId] = newId
                                it[versionAnterior] = "-"
                                it[versionNueva] = f.version
                                it[fechaCambio] = f.fechaEmision.ifBlank { hoyComoTexto() }
                                it[descripcion] = "Alta del documento (importacion del listado maestro)"
                                it[cambiadoPor] = f.elaboradoPor.ifBlank { "Importacion QIS" }
                                it[aprobadoPor] = f.aprobadoPor
                            }
                        }
                        creados++
                    }

                    // Respaldo del listado maestro en Drive. Si Drive no esta
                    // disponible la importacion ya quedo aplicada y se reporta.
                    var driveStatus = "ok"
                    var driveMsg = ""
                    if (GoogleDriveService.isConfigured()) {
                        val anio = java.time.Year.now().value
                        val ok = runCatching {
                            val folder = GoogleDriveService.categoryYearFolder("QIS", anio)
                            val fileId = folder?.let { GoogleDriveService.uploadFile(temp.readBytes(), fileName, mimeType, it) }
                            fileId != null
                        }.getOrDefault(false)
                        if (!ok) { driveStatus = "error"; driveMsg = "No fue posible respaldar el Excel en Drive" }
                    } else {
                        driveStatus = "no-configurado"
                    }

                    call.respond(mapOf(
                        "status" to "ok",
                        "creados" to creados.toString(),
                        "omitidos" to omitidos.toString(),
                        "filas" to filas.size.toString(),
                        "drive" to driveStatus,
                        "driveMensaje" to driveMsg
                    ))
                } finally {
                    temp.delete()
                }
            }
        }

        // Cambio de estado sin cambio de versión (p. ej. marcar Obsoleto
        // cuando el documento se sustituye por otro).
        // ---------- EDITAR DATOS DEL DOCUMENTO ----------
        // Corrige codigo, titulo, tipo, proceso o notas de un documento ya
        // registrado. El cambio queda en el historial. versionActual y
        // urlArchivo solo cambian por sus flujos propios.
        put("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, QIS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "ID invalido"))
                val req = call.receive<QisDocumentRequest>()
                if (req.codigo.isBlank() || req.codigo.length > 40) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Codigo requerido (maximo 40 caracteres)"))
                }
                if (req.titulo.isBlank() || req.titulo.length > 300) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Titulo requerido (maximo 300 caracteres)"))
                }
                if (req.tipo !in QIS_TIPOS) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Tipo invalido: ${QIS_TIPOS.joinToString(", ")}"))
                }
                val doc = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { QisDocumentTable.id eq id }.singleOrNull()?.let(::docFrom)
                } ?: return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Documento no encontrado"))
                val dup = DatabaseFactory.dbQuery {
                    QisDocumentTable.selectAll().where { (QisDocumentTable.codigo eq req.codigo) and (QisDocumentTable.id neq id) }.limit(1).any()
                }
                if (dup) {
                    return@safeApiCall call.respond(HttpStatusCode.Conflict, mapOf("status" to "error", "message" to "Ya existe otro documento con el codigo ${req.codigo}"))
                }
                val hoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))
                DatabaseFactory.dbQuery {
                    QisDocumentTable.update({ QisDocumentTable.id eq id }) {
                        it[codigo] = req.codigo
                        it[titulo] = req.titulo
                        it[tipo] = req.tipo
                        it[proceso] = req.proceso
                        it[notas] = req.notas
                    }
                    QisChangeLogTable.insert {
                        it[documentoId] = id
                        it[versionAnterior] = doc.versionActual
                        it[versionNueva] = doc.versionActual
                        it[fechaCambio] = hoy
                        it[descripcion] = "Correccion de datos del documento"
                        it[cambiadoPor] = req.elaboradoPor.ifBlank { "Portal" }
                        it[aprobadoPor] = req.aprobadoPor
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

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
