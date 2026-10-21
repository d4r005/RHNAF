package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsDocumentTable
import com.example.rhnaf.database.LockerHistoryTable
import com.example.rhnaf.database.LockerTable
import com.example.rhnaf.service.GoogleDriveService
import com.example.rhnaf.shared.model.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Listado de Lockers (casilleros de empleados) para EHS y RH.
 *
 * - LockerTable: estado vigente de cada casillero.
 * - LockerHistoryTable: bitacora INMUTABLE. Cada asignacion abre una fila; al
 *   liberar o reasignar se CIERRA (fecha_fin), nunca se borra. Asi siempre se
 *   sabe a quienes les ha pertenecido un casillero aunque hoy este libre.
 * - Documentos (listado maestro / evidencia): se suben a Google Drive en la
 *   carpeta Lockers/AAAA y quedan registrados en ehs_documents.
 *
 * Lectura: todos los autenticados con acceso al modulo. Escritura: ADMIN, RH, SEGURIDAD.
 */
private val LOCKER_WRITE = setOf(Roles.ADMIN, Roles.RH, Roles.SEGURIDAD)
private const val LOCKER_DRIVE_PREFIX = "gdrive:"
private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun hoy(): String = LocalDate.now(ZoneId.of("America/Mexico_City")).format(DATE_FMT)

private fun lockerFrom(r: ResultRow) = Locker(
    numero = r[LockerTable.numero],
    estado = r[LockerTable.estado],
    employeeId = r[LockerTable.employeeId],
    nombre = r[LockerTable.nombre],
    fechaCandado = r[LockerTable.fechaCandado],
    llaveCopia = r[LockerTable.llaveCopia],
    fechaPrestamoLlave = r[LockerTable.fechaPrestamoLlave],
    fechaDevolucionLlave = r[LockerTable.fechaDevolucionLlave],
    notas = r[LockerTable.notas]
)

private fun historyFrom(r: ResultRow) = LockerHistoryRecord(
    id = r[LockerHistoryTable.id],
    numero = r[LockerHistoryTable.numero],
    employeeId = r[LockerHistoryTable.employeeId],
    nombre = r[LockerHistoryTable.nombre],
    fechaCandado = r[LockerHistoryTable.fechaCandado],
    llaveCopia = r[LockerHistoryTable.llaveCopia],
    fechaPrestamoLlave = r[LockerHistoryTable.fechaPrestamoLlave],
    fechaDevolucionLlave = r[LockerHistoryTable.fechaDevolucionLlave],
    fechaInicio = r[LockerHistoryTable.fechaInicio],
    fechaFin = r[LockerHistoryTable.fechaFin],
    motivoFin = r[LockerHistoryTable.motivoFin],
    registradoPor = r[LockerHistoryTable.registradoPor]
)

private fun quien(call: ApplicationCall): String =
    call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")?.trim().orEmpty().take(200)

private fun String.limpia(max: Int) = this.trim().take(max)

private fun fechaValida(s: String) = s.isBlank() || runCatching { LocalDate.parse(s, DATE_FMT) }.isSuccess

/** Cierra la asignacion vigente de un casillero en la bitacora (si existe). */
private fun cerrarAsignacionVigente(numero: Int, motivo: String, fecha: String) {
    LockerHistoryTable.update({ (LockerHistoryTable.numero eq numero) and (LockerHistoryTable.fechaFin eq "") }) {
        it[fechaFin] = fecha
        it[motivoFin] = motivo
    }
}

/** Texto de una celda sin importar su tipo (fechas -> dd/MM/yyyy, numeros enteros sin ".0"). */
private fun celdaTexto(row: Row, col: Int): String {
    val c = row.getCell(col) ?: return ""
    return when (c.cellType) {
        CellType.STRING -> c.stringCellValue.trim()
        CellType.NUMERIC ->
            if (DateUtil.isCellDateFormatted(c)) {
                c.localDateTimeCellValue.toLocalDate().format(DATE_FMT)
            } else {
                val d = c.numericCellValue
                if (d == Math.floor(d)) d.toLong().toString() else d.toString()
            }
        CellType.FORMULA -> runCatching {
            if (DateUtil.isCellDateFormatted(c)) c.localDateTimeCellValue.toLocalDate().format(DATE_FMT)
            else c.numericCellValue.let { if (it == Math.floor(it)) it.toLong().toString() else it.toString() }
        }.getOrElse { runCatching { c.stringCellValue.trim() }.getOrDefault("") }
        else -> ""
    }
}

/**
 * Lee el listado maestro (Hoja1): N° casillero | N° empleado | Nombre | Fecha candado |
 * Llave copia | Fecha prestamo llave | Fecha devolucion llave. Ignora encabezados y filas
 * sin numero de casillero; "Libre" / nombre vacio => casillero disponible.
 */
private fun leerListadoLockers(bytes: ByteArray): List<LockerImportRow> {
    val filas = mutableListOf<LockerImportRow>()
    WorkbookFactory.create(bytes.inputStream()).use { wb ->
        val sheet = wb.getSheetAt(0)
        for (r in 0..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val numero = celdaTexto(row, 0).toIntOrNull() ?: continue   // salta titulos/encabezados
            filas += LockerImportRow(
                numero = numero,
                employeeId = celdaTexto(row, 1),
                nombre = celdaTexto(row, 2),
                fechaCandado = celdaTexto(row, 3).let { if (Regex("""^\d{2}/\d{2}/\d{4}$""").matches(it)) it else "" },
                llaveCopia = celdaTexto(row, 4).ifBlank { "Si" },
                fechaPrestamoLlave = celdaTexto(row, 5).let { if (Regex("""^\d{2}/\d{2}/\d{4}$""").matches(it)) it else "" },
                fechaDevolucionLlave = celdaTexto(row, 6).let { if (Regex("""^\d{2}/\d{2}/\d{4}$""").matches(it)) it else "" }
            )
        }
    }
    return filas
}

/** Aplica filas de importacion a la BD (idempotente). Devuelve contadores. */
private fun aplicarImportacion(filas: List<LockerImportRow>, fecha: String, usuario: String): Map<String, String> {
    var creados = 0; var asignados = 0; var liberados = 0; var sinCambio = 0
    for (f in filas.distinctBy { it.numero }) {
        if (f.numero !in 1..9999) continue
        val nombre = f.nombre.limpia(200)
        val libre = nombre.isBlank() || nombre.equals("libre", ignoreCase = true)
        val actual = LockerTable.selectAll().where { LockerTable.numero eq f.numero }.singleOrNull()
        if (actual == null) { LockerTable.insert { it[numero] = f.numero }; creados++ }
        val estadoActual = actual?.get(LockerTable.estado) ?: "Libre"
        val nombreActual = actual?.get(LockerTable.nombre) ?: ""
        val candadoActual = actual?.get(LockerTable.fechaCandado) ?: ""
        if (libre) {
            if (estadoActual == "Asignado") {
                cerrarAsignacionVigente(f.numero, "Liberado (importación)", fecha)
                LockerTable.update({ LockerTable.numero eq f.numero }) {
                    it[estado] = "Libre"; it[employeeId] = ""; it[LockerTable.nombre] = ""
                    it[fechaCandado] = ""; it[fechaPrestamoLlave] = ""; it[fechaDevolucionLlave] = ""
                }
                liberados++
            } else sinCambio++
            continue
        }
        val fc = if (fechaValida(f.fechaCandado)) f.fechaCandado else ""
        val llave = if (f.llaveCopia.equals("No", ignoreCase = true)) "No" else "Si"
        val pres = if (fechaValida(f.fechaPrestamoLlave)) f.fechaPrestamoLlave else ""
        val dev = if (fechaValida(f.fechaDevolucionLlave)) f.fechaDevolucionLlave else ""
        val mismo = estadoActual == "Asignado" && nombreActual.equals(nombre, ignoreCase = true) && candadoActual == fc
        if (mismo) {
            LockerTable.update({ LockerTable.numero eq f.numero }) {
                it[llaveCopia] = llave; it[fechaPrestamoLlave] = pres; it[fechaDevolucionLlave] = dev
            }
            sinCambio++
            continue
        }
        cerrarAsignacionVigente(f.numero, "Reasignado (importación)", fecha)
        LockerTable.update({ LockerTable.numero eq f.numero }) {
            it[estado] = "Asignado"; it[employeeId] = f.employeeId.limpia(50); it[LockerTable.nombre] = nombre
            it[fechaCandado] = fc; it[llaveCopia] = llave; it[fechaPrestamoLlave] = pres; it[fechaDevolucionLlave] = dev
        }
        LockerHistoryTable.insert {
            it[LockerHistoryTable.numero] = f.numero
            it[LockerHistoryTable.employeeId] = f.employeeId.limpia(50)
            it[LockerHistoryTable.nombre] = nombre
            it[fechaCandado] = fc; it[llaveCopia] = llave
            it[fechaPrestamoLlave] = pres; it[fechaDevolucionLlave] = dev
            it[fechaInicio] = if (fc.isNotBlank()) fc else fecha
            it[fechaFin] = ""
            it[registradoPor] = usuario
        }
        asignados++
    }
    return mapOf("creados" to creados.toString(), "asignados" to asignados.toString(),
        "liberados" to liberados.toString(), "sinCambio" to sinCambio.toString())
}

fun Route.lockerRouting() {
    route("/api/v1/lockers") {

        // Listado completo + resumen (total / asignados / libres).
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val items = DatabaseFactory.dbQuery {
                    LockerTable.selectAll().orderBy(LockerTable.numero to SortOrder.ASC).map(::lockerFrom)
                }
                call.respond(items)
            }
        }

        get("/resumen") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val items = DatabaseFactory.dbQuery { LockerTable.selectAll().map(::lockerFrom) }
                call.respond(LockerSummary(
                    total = items.size,
                    asignados = items.count { it.estado == "Asignado" },
                    libres = items.count { it.estado == "Libre" }
                ))
            }
        }

        // Alta de un casillero nuevo (queda libre).
        post("/{numero}") {
            safeApiCall(call) {
                requireRoleOr403(call, LOCKER_WRITE) ?: return@safeApiCall
                val numero = call.parameters["numero"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Número inválido"))
                if (numero !in 1..9999) return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Número fuera de rango"))
                val existe = DatabaseFactory.dbQuery { LockerTable.selectAll().where { LockerTable.numero eq numero }.limit(1).any() }
                if (existe) return@safeApiCall call.respond(HttpStatusCode.Conflict, mapOf("status" to "error", "message" to "El casillero $numero ya existe"))
                DatabaseFactory.dbQuery { LockerTable.insert { it[LockerTable.numero] = numero } }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }

        // Asignar (o reasignar) un casillero. Si estaba ocupado, la asignacion
        // anterior se CIERRA en la bitacora antes de abrir la nueva.
        put("/{numero}/asignar") {
            safeApiCall(call) {
                requireRoleOr403(call, LOCKER_WRITE) ?: return@safeApiCall
                val numero = call.parameters["numero"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Número inválido"))
                val req = call.receive<LockerAssignRequest>()
                val nombre = req.nombre.limpia(200)
                if (nombre.isBlank()) return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Nombre del empleado requerido"))
                if (!fechaValida(req.fechaCandado)) return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Fecha de candado inválida (dd/MM/aaaa)"))
                val llave = if (req.llaveCopia == "No") "No" else "Si"
                val fecha = hoy()
                val usuario = quien(call)
                val ok = DatabaseFactory.dbQuery {
                    val existe = LockerTable.selectAll().where { LockerTable.numero eq numero }.limit(1).any()
                    if (!existe) return@dbQuery false
                    cerrarAsignacionVigente(numero, "Reasignado", fecha)
                    LockerTable.update({ LockerTable.numero eq numero }) {
                        it[estado] = "Asignado"
                        it[employeeId] = req.employeeId.limpia(50)
                        it[LockerTable.nombre] = nombre
                        it[fechaCandado] = req.fechaCandado
                        it[llaveCopia] = llave
                        it[fechaPrestamoLlave] = ""
                        it[fechaDevolucionLlave] = ""
                        it[notas] = req.notas.limpia(500)
                    }
                    LockerHistoryTable.insert {
                        it[LockerHistoryTable.numero] = numero
                        it[LockerHistoryTable.employeeId] = req.employeeId.limpia(50)
                        it[LockerHistoryTable.nombre] = nombre
                        it[fechaCandado] = req.fechaCandado
                        it[llaveCopia] = llave
                        it[fechaInicio] = fecha
                        it[fechaFin] = ""
                        it[registradoPor] = usuario
                    }
                    true
                }
                if (!ok) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Casillero no encontrado"))
                call.respond(mapOf("status" to "ok"))
            }
        }

        // Liberar: el casillero queda Libre pero la asignacion se conserva
        // CERRADA en la bitacora (con fecha y motivo). Nunca se borra.
        put("/{numero}/liberar") {
            safeApiCall(call) {
                requireRoleOr403(call, LOCKER_WRITE) ?: return@safeApiCall
                val numero = call.parameters["numero"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Número inválido"))
                val motivo = call.receive<LockerReleaseRequest>().motivo.limpia(300).ifBlank { "Liberado" }
                val fecha = hoy()
                val estadoPrevio = DatabaseFactory.dbQuery {
                    val row = LockerTable.selectAll().where { LockerTable.numero eq numero }.singleOrNull() ?: return@dbQuery null
                    cerrarAsignacionVigente(numero, motivo, fecha)
                    LockerTable.update({ LockerTable.numero eq numero }) {
                        it[estado] = "Libre"
                        it[employeeId] = ""
                        it[nombre] = ""
                        it[fechaCandado] = ""
                        it[llaveCopia] = "Si"
                        it[fechaPrestamoLlave] = ""
                        it[fechaDevolucionLlave] = ""
                        it[notas] = ""
                    }
                    row[LockerTable.estado]
                }
                if (estadoPrevio == null) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "Casillero no encontrado"))
                call.respond(mapOf("status" to "ok"))
            }
        }

        // Prestamo / devolucion de la llave copia. Se refleja tambien en la
        // asignacion vigente de la bitacora.
        put("/{numero}/llave") {
            safeApiCall(call) {
                requireRoleOr403(call, LOCKER_WRITE) ?: return@safeApiCall
                val numero = call.parameters["numero"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Número inválido"))
                val req = call.receive<LockerKeyRequest>()
                if (!fechaValida(req.fechaPrestamoLlave) || !fechaValida(req.fechaDevolucionLlave)) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Fecha inválida (dd/MM/aaaa)"))
                }
                val ok = DatabaseFactory.dbQuery {
                    val row = LockerTable.selectAll().where { LockerTable.numero eq numero }.singleOrNull() ?: return@dbQuery false
                    if (row[LockerTable.estado] != "Asignado") return@dbQuery false
                    LockerTable.update({ LockerTable.numero eq numero }) {
                        it[fechaPrestamoLlave] = req.fechaPrestamoLlave
                        it[fechaDevolucionLlave] = req.fechaDevolucionLlave
                    }
                    LockerHistoryTable.update({ (LockerHistoryTable.numero eq numero) and (LockerHistoryTable.fechaFin eq "") }) {
                        it[fechaPrestamoLlave] = req.fechaPrestamoLlave
                        it[fechaDevolucionLlave] = req.fechaDevolucionLlave
                    }
                    true
                }
                if (!ok) return@safeApiCall call.respond(HttpStatusCode.Conflict, mapOf("status" to "error", "message" to "El casillero no existe o está libre"))
                call.respond(mapOf("status" to "ok"))
            }
        }

        // Historial completo de un casillero: todas las personas a quienes les ha pertenecido.
        get("/{numero}/historial") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val numero = call.parameters["numero"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Número inválido"))
                val items = DatabaseFactory.dbQuery {
                    LockerHistoryTable.selectAll().where { LockerHistoryTable.numero eq numero }
                        .orderBy(LockerHistoryTable.id to SortOrder.DESC).map(::historyFrom)
                }
                call.respond(items)
            }
        }

        // Historial global (auditoria): todo lo que ha pasado en todos los casilleros,
        // opcionalmente filtrado por empleado (para saber qué casilleros ha tenido una persona).
        get("/historial") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val emp = call.request.queryParameters["empleado"]?.trim().orEmpty()
                val items = DatabaseFactory.dbQuery {
                    val q = LockerHistoryTable.selectAll()
                    val f = if (emp.isNotBlank()) q.where { LockerHistoryTable.employeeId eq emp } else q
                    f.orderBy(LockerHistoryTable.id to SortOrder.DESC).limit(2000).map(::historyFrom)
                }
                call.respond(items)
            }
        }

        // ---------- RETIRADOS POR PERIODO ----------
        // Historial cerrado (fechaFin) dentro del rango [inicio, fin] (yyyy-mm-dd).
        fun parseD(d: String): java.time.LocalDate? = runCatching {
            val t = d.trim().split("/")
            if (t.size != 3) return@runCatching null
            java.time.LocalDate.of(t[2].toInt(), t[1].toInt(), t[0].toInt())
        }.getOrNull()

        suspend fun retiradosEntre(inicio: String, fin: String): List<com.example.rhnaf.shared.model.LockerHistoryRecord> {
            val desde = java.time.LocalDate.parse(inicio)
            val hasta = java.time.LocalDate.parse(fin)
            return DatabaseFactory.dbQuery {
                LockerHistoryTable.selectAll().map(::historyFrom)
            }.filter { h ->
                h.fechaFin.isNotBlank() && parseD(h.fechaFin)?.let { it in desde..hasta } == true
            }
        }

        get("/retirados") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val inicio = call.request.queryParameters["inicio"] ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Falta inicio (yyyy-mm-dd)"))
                val fin = call.request.queryParameters["fin"] ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Falta fin (yyyy-mm-dd)"))
                call.respond(retiradosEntre(inicio, fin))
            }
        }

        // CSV para Excel (con BOM para acentos): reporte descargable de retirados.
        get("/retirados/csv") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val inicio = call.request.queryParameters["inicio"] ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Falta inicio (yyyy-mm-dd)"))
                val fin = call.request.queryParameters["fin"] ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Falta fin (yyyy-mm-dd)"))
                val items = retiradosEntre(inicio, fin)
                fun csv(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
                val lineas = mutableListOf<String>()
                lineas.add("N° de casillero;Nombre del titular;N° de empleado;Fecha de liberación;Motivo de liberación;Fecha de asignación;Llave copia;Registrado por")
                items.forEach { h ->
                    lineas.add(listOf(h.numero.toString(), h.nombre, h.employeeId, h.fechaFin, h.motivoFin, h.fechaInicio, h.llaveCopia, h.registradoPor).joinToString(";") { csv(it) })
                }
                val cuerpo = "\ufeff" + lineas.joinToString("\r\n")
                call.response.headers.append(HttpHeaders.ContentDisposition, "attachment; filename=\"lockers_retirados_${inicio}_a_${fin}.csv\"")
                call.respondBytes(cuerpo.toByteArray(Charsets.UTF_8), ContentType("text","csv"))
            }
        }

        // Importacion masiva desde el listado maestro. Idempotente: crea los
        // casilleros que falten y solo abre asignacion nueva si la persona o
        // fecha de candado cambió respecto a lo vigente (no duplica la bitacora).
        post("/importar") {
            safeApiCall(call) {
                requireRoleOr403(call, LOCKER_WRITE) ?: return@safeApiCall
                val req = call.receive<LockerImportRequest>()
                if (req.filas.isEmpty() || req.filas.size > 2000) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "El archivo no contiene filas válidas (máximo 2000)"))
                }
                val resultado = DatabaseFactory.dbQuery { aplicarImportacion(req.filas, hoy(), quien(call)) }
                call.respond(resultado + ("status" to "ok"))
            }
        }

        // Importa el listado maestro (.xlsx/.xls) directo desde el archivo: lo lee,
        // actualiza casilleros + bitacora y ademas lo respalda en Drive (Lockers/AAAA).
        // Si Drive no esta conectado o falla, la importacion igual se aplica y se avisa.
        post("/importar-archivo") {
            safeApiCall(call) {
                requireRoleOr403(call, LOCKER_WRITE) ?: return@safeApiCall
                val maxBytes = 25L * 1024 * 1024
                var fileName = "listado-lockers.xlsx"
                var mimeType = "application/octet-stream"
                var bytes: ByteArray? = null
                var filesSeen = 0
                call.receiveMultipart(formFieldLimit = maxBytes).forEachPart { part ->
                    try {
                        if (part is PartData.FileItem) {
                            filesSeen++
                            if (filesSeen == 1) {
                                fileName = (part.originalFileName ?: fileName).substringAfterLast('/').substringAfterLast('\\').take(300)
                                mimeType = part.contentType?.toString()?.take(100) ?: mimeType
                                bytes = part.streamProvider().use { it.readNBytes((maxBytes + 1).toInt()) }
                            }
                        }
                    } finally { part.dispose() }
                }
                val data = bytes
                if (filesSeen != 1 || data == null || data.isEmpty() || data.size > maxBytes) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "Se requiere exactamente un archivo Excel válido (máx. 25 MiB)"))
                }
                val ext = fileName.substringAfterLast('.', "").lowercase()
                if (ext != "xlsx" && ext != "xls") {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "Formato no soportado: usa Excel .xlsx o .xls"))
                }
                val filas = try { leerListadoLockers(data) } catch (e: Exception) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "No se pudo leer el Excel: ${e.message?.take(120)}"))
                }
                if (filas.isEmpty() || filas.size > 2000) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to "No se encontraron casilleros en la primera hoja (columna A = N° de casillero)"))
                }
                val usuario = quien(call)
                val resultado = DatabaseFactory.dbQuery { aplicarImportacion(filas, hoy(), usuario) }

                // Respaldo en Drive (no bloquea la importacion).
                var drive = "no-configurado"
                if (GoogleDriveService.isConfigured()) {
                    drive = try {
                        val year = LocalDate.now(ZoneId.of("America/Mexico_City")).year
                        val folder = GoogleDriveService.categoryYearFolder("Lockers", year)
                        val tmp = File.createTempFile("rhnaf-locker-", ".upload")
                        try {
                            tmp.writeBytes(data)
                            val driveId = if (folder != null) GoogleDriveService.uploadLargeFile(tmp, fileName, mimeType, folder) else null
                            if (driveId == null) "error" else {
                                DatabaseFactory.dbQuery {
                                    EhsDocumentTable.insert {
                                        it[categoria] = "Lockers"
                                        it[titulo] = "Listado maestro de lockers"
                                        it[fecha] = hoy(); it[anio] = year
                                        it[EhsDocumentTable.fileName] = fileName
                                        it[EhsDocumentTable.mimeType] = mimeType
                                        it[fileSize] = data.size
                                        it[notas] = "Importado: ${resultado["asignados"]} asignados, ${resultado["liberados"]} liberados"
                                        it[uploadedBy] = usuario; it[uploadedDate] = hoy()
                                        it[moduleType] = "locker"; it[moduleRecordId] = 0
                                        it[contentBase64] = "$LOCKER_DRIVE_PREFIX$driveId"
                                    }
                                }
                                "ok"
                            }
                        } finally { tmp.delete() }
                    } catch (e: Exception) { "error" }
                }
                call.respond(resultado + mapOf("status" to "ok", "drive" to drive, "filas" to filas.size.toString()))
            }
        }

        // ---------- DOCUMENTOS (a Google Drive) ----------

        // Sube el listado maestro o cualquier documento de lockers a Drive
        // (carpeta Lockers/AAAA) y lo registra en ehs_documents (categoria Lockers).
        post("/documentos") {
            safeApiCall(call) {
                requireRoleOr403(call, LOCKER_WRITE) ?: return@safeApiCall
                if (!GoogleDriveService.isConfigured()) {
                    return@safeApiCall call.respond(HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "error", "message" to "Google Drive aún no está conectado"))
                }
                val temp = File.createTempFile("rhnaf-locker-", ".upload")
                try {
                    val fields = mutableMapOf<String, String>()
                    var fileName = ""
                    var mimeType = "application/octet-stream"
                    var fileSize = 0L
                    var invalid = false
                    var filesSeen = 0
                    val maxBytes = 100L * 1024 * 1024
                    call.receiveMultipart(formFieldLimit = maxBytes).forEachPart { part ->
                        try {
                            when (part) {
                                is PartData.FormItem -> if (part.name != null) fields[part.name!!] = part.value
                                is PartData.FileItem -> {
                                    filesSeen++
                                    if (filesSeen > 1) { invalid = true; return@forEachPart }
                                    fileName = (part.originalFileName ?: "listado-lockers")
                                        .substringAfterLast('/').substringAfterLast('\\').take(300)
                                    mimeType = part.contentType?.toString()?.take(100) ?: "application/octet-stream"
                                    part.streamProvider().use { input ->
                                        FileOutputStream(temp).use { output ->
                                            val buffer = ByteArray(1024 * 1024)
                                            while (true) {
                                                val n = input.read(buffer)
                                                if (n < 0) break
                                                fileSize += n
                                                if (fileSize > maxBytes) { invalid = true; break }
                                                output.write(buffer, 0, n)
                                            }
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        } finally { part.dispose() }
                    }
                    if (invalid || fileSize == 0L || filesSeen != 1) {
                        return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                            mapOf("status" to "error", "message" to "Se requiere exactamente un archivo válido (máx. 100 MiB)"))
                    }
                    val year = LocalDate.now(ZoneId.of("America/Mexico_City")).year
                    val titulo = fields["titulo"]?.trim().orEmpty().ifBlank { fileName }.take(300)
                    val folder = GoogleDriveService.categoryYearFolder("Lockers", year)
                        ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                            mapOf("status" to "error", "message" to "No fue posible crear Lockers/$year en Drive"))
                    val driveId = GoogleDriveService.uploadLargeFile(temp, fileName, mimeType, folder)
                        ?: return@safeApiCall call.respond(HttpStatusCode.BadGateway,
                            mapOf("status" to "error", "message" to "Drive no confirmó la subida del archivo"))
                    val usuario = quien(call)
                    try {
                        val id = DatabaseFactory.dbQuery {
                            EhsDocumentTable.insert {
                                it[categoria] = "Lockers"
                                it[EhsDocumentTable.titulo] = titulo
                                it[fecha] = hoy()
                                it[anio] = year
                                it[EhsDocumentTable.fileName] = fileName
                                it[EhsDocumentTable.mimeType] = mimeType
                                it[EhsDocumentTable.fileSize] = fileSize.toInt()
                                it[notas] = fields["notas"].orEmpty().take(500)
                                it[uploadedBy] = usuario
                                it[uploadedDate] = hoy()
                                it[moduleType] = "locker"
                                it[moduleRecordId] = 0
                                it[contentBase64] = "$LOCKER_DRIVE_PREFIX$driveId"
                            } get EhsDocumentTable.id
                        }
                        call.respond(mapOf("status" to "ok", "id" to id.toString(), "folder" to "Lockers/$year"))
                    } catch (e: Exception) {
                        GoogleDriveService.deleteFile(driveId)
                        throw e
                    }
                } finally { temp.delete() }
            }
        }

        // Documentos de lockers ya subidos (para listarlos/descargarlos desde la pantalla).
        get("/documentos") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val items = DatabaseFactory.dbQuery {
                    EhsDocumentTable.selectAll()
                        .where { (EhsDocumentTable.categoria eq "Lockers") and (EhsDocumentTable.moduleType eq "locker") }
                        .orderBy(EhsDocumentTable.id to SortOrder.DESC)
                        .map {
                            mapOf(
                                "id" to it[EhsDocumentTable.id].toString(),
                                "titulo" to it[EhsDocumentTable.titulo],
                                "fileName" to it[EhsDocumentTable.fileName],
                                "fecha" to it[EhsDocumentTable.uploadedDate],
                                "subidoPor" to it[EhsDocumentTable.uploadedBy]
                            )
                        }
                }
                call.respond(items)
            }
        }
    }
}
