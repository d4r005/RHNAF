package com.example.rhnaf.routes

import com.example.rhnaf.api.HikvisionEventRequest
import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.DebugLogTable
import com.example.rhnaf.database.EmployeeTable
import com.example.rhnaf.database.SystemTaskTable
import com.example.rhnaf.domain.model.AttendanceLog
import com.example.rhnaf.domain.model.ImportResult
import com.example.rhnaf.domain.model.SyncResult
import com.example.rhnaf.service.AttendanceUseCase
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

// Estructura de una fila ya normalizada, lista para insertar
private data class ImportedAttendanceRow(
    val employeeId: String,
    val name: String,
    val department: String,
    val timestamp: String,
    val deviceSerial: String,
    val status: String,
    val customName: String
)

/**
 * Parsea el CSV exportado por el software de asistencia de Hikvision (formato:
 * Person ID,Name,Department,Time,Attendance Status,Attendance Check Point,
 * Custom Name,Data Source,Handling Type,Temperature,Abnormal).
 *
 * Es tolerante a: el apóstrofe inicial que Excel agrega al Person ID ('114),
 * distinto orden/mayúsculas de encabezado, líneas vacías, y separa por coma
 * simple (los valores de este export no traen comas dentro de campos).
 */
private fun parseAttendanceCsv(rawText: String): Pair<List<ImportedAttendanceRow>, Int> {
    val lines = rawText.split("\r\n", "\n").map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList<ImportedAttendanceRow>() to 0

    val header = lines.first().split(",").map { it.trim().lowercase() }
    fun colIndex(vararg names: String): Int {
        for (name in names) {
            val idx = header.indexOf(name)
            if (idx >= 0) return idx
        }
        return -1
    }

    val idxPersonId = colIndex("person id", "personid")
    val idxName = colIndex("name")
    val idxDepartment = colIndex("department")
    val idxTime = colIndex("time")
    val idxStatus = colIndex("attendance status", "status")
    val idxCheckPoint = colIndex("attendance check point", "check point", "device")
    val idxCustomName = colIndex("custom name", "customname")

    val dataLines = lines.drop(1)
    var invalidCount = 0

    val rows = dataLines.mapNotNull { line ->
        val parts = line.split(",")
        val personId = parts.getOrNull(if (idxPersonId >= 0) idxPersonId else 0)
            ?.trim()?.trimStart('\'')
        val name = (if (idxName >= 0) parts.getOrNull(idxName) else null)?.trim() ?: ""
        val department = (if (idxDepartment >= 0) parts.getOrNull(idxDepartment) else null)?.trim() ?: ""
        val time = parts.getOrNull(if (idxTime >= 0) idxTime else 3)?.trim()
        val status = parts.getOrNull(if (idxStatus >= 0) idxStatus else 4)?.trim() ?: "Check-in"
        val checkPoint = parts.getOrNull(if (idxCheckPoint >= 0) idxCheckPoint else 5)?.trim()
            ?.ifBlank { "CSV-IMPORT" } ?: "CSV-IMPORT"
        val customName = (if (idxCustomName >= 0) parts.getOrNull(idxCustomName) else null)?.trim() ?: ""

        if (personId.isNullOrBlank() || time.isNullOrBlank()) {
            invalidCount++
            return@mapNotNull null
        }

        // Normalizamos "2026-06-29 06:42:50" -> "2026-06-29T06:42:50" para que
        // coincida con el formato ISO que ya usa el resto del sistema.
        val isoTimestamp = if (time.contains(" ") && !time.contains("T")) {
            time.replace(" ", "T")
        } else time

        ImportedAttendanceRow(
            employeeId = personId,
            name = name,
            department = department,
            timestamp = isoTimestamp,
            deviceSerial = checkPoint,
            status = status,
            customName = customName
        )
    }

    return rows to invalidCount
}

fun Route.attendanceRouting(attendanceUseCase: AttendanceUseCase) {

    // Extrae el JSON del evento sin importar si viene como body plano
    // o como multipart/form-data (formato típico de las lectoras Hikvision
    // cuando además mandan una foto adjunta en el mismo POST).
    suspend fun extractEventJson(call: ApplicationCall): Pair<String, String> {
        val contentType = call.request.contentType()
        val clientIp = call.request.local.remoteHost

        if (contentType.match(ContentType.MultiPart.FormData)) {
            var jsonPart = ""
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FormItem &&
                    (part.name == "event_log" || part.name == "param" || part.value.contains("employeeNoString"))
                ) {
                    jsonPart = part.value
                }
                part.dispose()
            }
            return jsonPart to clientIp
        }

        // Fallback: body plano (JSON o XML)
        return call.receiveText() to clientIp
    }

    suspend fun handleHikvisionRequest(call: ApplicationCall) {
        val (rawBody, clientIp) = extractEventJson(call)

        // Guardamos SIEMPRE el crudo para poder diagnosticar qué manda la lectora
        DatabaseFactory.dbQuery {
            DebugLogTable.insert {
                it[timestamp] = java.time.LocalDateTime.now().toString()
                it[rawContent] = "HIK-POST | BODY: $rawBody"
                it[sourceIp] = clientIp
            }
        }

        var employeeNo: String? = null
        var deviceId = "HIKVISIONWEB"
        var verifyMode = "Face"
        var employeeName = ""
        var attendanceStatus = ""
        var eventTime = java.time.LocalDateTime.now().toString()

        // 1) Intentamos deserializar el JSON estructurado (formato ISAPI estándar)
        runCatching {
            val event = lenientJson.decodeFromString<HikvisionEventRequest>(rawBody)
            employeeNo = event.AccessControllerEvent.employeeNoString
            deviceId = event.deviceID
            verifyMode = event.AccessControllerEvent.currentVerifyMode
            employeeName = event.AccessControllerEvent.name ?: ""
            attendanceStatus = event.AccessControllerEvent.attendanceStatus ?: ""
            eventTime = event.dateTime
        }

        // 2) Si falla, caemos al parsing manual (JSON suelto o XML), como red de seguridad
        if (employeeNo.isNullOrBlank()) {
            employeeNo = when {
                rawBody.contains("employeeNoString") ->
                    rawBody.substringAfter("employeeNoString\"").substringAfter(":").substringAfter("\"").substringBefore("\"")
                rawBody.contains("<employeeNo>") ->
                    rawBody.substringAfter("<employeeNo>").substringBefore("</employeeNo>")
                else -> null
            }
            // Intentamos extraer tambien la fecha si el parsing estructurado fallo
            if (rawBody.contains("dateTime")) {
                eventTime = rawBody.substringAfter("dateTime\"").substringAfter(":").substringAfter("\"").substringBefore("\"")
            }
        }

        // Inferir Check-in/Check-out del nombre del checkpoint si no viene explicito
        // (igual que IVMS-4200: "Entrance" = Check-in, "Exit" = Check-out)
        if (attendanceStatus.isBlank() && deviceId.isNotBlank()) {
            val devLower = deviceId.lowercase()
            attendanceStatus = when {
                devLower.contains("exit") -> "Check-out"
                devLower.contains("entrance") -> "Check-in"
                else -> ""
            }
        }

        if (!employeeNo.isNullOrBlank() && 
            employeeNo!!.lowercase() != "none" && 
            employeeNo!!.lowercase() != "null" && 
            employeeNo!! != "0") {
            // IMPORTANTE: nunca dejamos que una excepcion aqui tumbe la respuesta con un 500.
            // Si algo truena (ej. un valor demasiado largo para una columna, un campo
            // inesperado, etc.) lo atrapamos, lo dejamos escrito en DebugLogTable con el
            // detalle exacto del error, y de todas formas respondemos 200 OK -- asi la
            // lectora/el script de sync nunca se quedan reintentando en bucle, y nosotros
            // vemos en /api/v1/asistencia/debug exactamente que fallo y por que.
            try {
                val saved = attendanceUseCase.registerCheckIn(
                    employeeId = employeeNo!!,
                    timestamp = eventTime,
                    deviceSerial = deviceId,
                    verifyMode = verifyMode,
                    name = employeeName,
                    attendanceStatus = attendanceStatus
                )
                if (!saved) {
                    DatabaseFactory.dbQuery {
                        DebugLogTable.insert {
                            it[timestamp] = java.time.LocalDateTime.now().toString()
                            it[rawContent] = "HIK-POST RECHAZADO (ya tiene Check-in y Check-out hoy) | employeeNo=$employeeNo"
                            it[sourceIp] = clientIp
                        }
                    }
                }
            } catch (e: Exception) {
                val detail = "HIK-POST ERROR AL GUARDAR | employeeNo=$employeeNo deviceId=$deviceId verifyMode=$verifyMode " +
                    "| excepcion=${e::class.simpleName}: ${e.message} | BODY: $rawBody"
                runCatching {
                    DatabaseFactory.dbQuery {
                        DebugLogTable.insert {
                            it[timestamp] = java.time.LocalDateTime.now().toString()
                            it[rawContent] = detail.take(4000)
                            it[sourceIp] = clientIp
                        }
                    }
                }
            }
        } else {
            // Deja rastro de los eventos que no se pudieron mapear a un empleado
            DatabaseFactory.dbQuery {
                DebugLogTable.insert {
                    it[timestamp] = java.time.LocalDateTime.now().toString()
                    it[rawContent] = "HIK-POST SIN employeeNo RECONOCIDO | BODY: $rawBody"
                    it[sourceIp] = clientIp
                }
            }
        }

        // Hikvision espera SIEMPRE 200 OK con este formato, o reintenta/deja de mandar eventos
        call.respondText("{\"statusString\":\"OK\",\"statusCode\":1}", contentType = ContentType.Application.Json)
    }

    route("/hikvision") {
        post { handleHikvisionRequest(call) }
        get { call.respondText("Radar raíz activo") }
    }

    route("/api/v1/asistencia") {
        post("/hikvision") { handleHikvisionRequest(call) }

        get("/debug") {
            val debug = DatabaseFactory.dbQuery {
                DebugLogTable.selectAll().orderBy(DebugLogTable.id, SortOrder.DESC).limit(20).map {
                    "${it[DebugLogTable.timestamp]} | ${it[DebugLogTable.sourceIp]} | ${it[DebugLogTable.rawContent]}"
                }
            }
            call.respond(debug)
        }

        get("/logs") {
            val pid = call.request.queryParameters["pid"]
            val name = call.request.queryParameters["name"]
            val dept = call.request.queryParameters["dept"]
            val from = call.request.queryParameters["from"] // YYYY-MM-DD
            val to = call.request.queryParameters["to"]     // YYYY-MM-DD
            val source = call.request.queryParameters["source"]

            val logs = DatabaseFactory.dbQuery {
                val query = AttendanceLogTable.selectAll()
                
                if (!pid.isNullOrBlank()) query.andWhere { AttendanceLogTable.employeeId eq pid }
                if (!name.isNullOrBlank()) query.andWhere { AttendanceLogTable.name.lowerCase() like "%${name.lowercase()}%" }
                if (!dept.isNullOrBlank()) query.andWhere { AttendanceLogTable.department.lowerCase() like "%${dept.lowercase()}%" }
                if (!from.isNullOrBlank()) query.andWhere { AttendanceLogTable.timestamp greaterEq from }
                if (!to.isNullOrBlank()) query.andWhere { AttendanceLogTable.timestamp lessEq to + "T23:59:59" }
                if (!source.isNullOrBlank()) query.andWhere { AttendanceLogTable.deviceSerial eq source }

                query.orderBy(AttendanceLogTable.timestamp, SortOrder.DESC)
                    .orderBy(AttendanceLogTable.id, SortOrder.DESC).map {
                    AttendanceLog(
                        id = it[AttendanceLogTable.id].toString(),
                        employeeId = it[AttendanceLogTable.employeeId],
                        name = it[AttendanceLogTable.name],
                        department = it[AttendanceLogTable.department],
                        timestamp = it[AttendanceLogTable.timestamp],
                        attendanceStatus = it[AttendanceLogTable.attendanceStatus],
                        deviceSerial = it[AttendanceLogTable.deviceSerial],
                        verifyMode = it[AttendanceLogTable.verifyMode],
                        customName = it[AttendanceLogTable.customName]
                    )
                }
            }
            call.respond(logs)
        }

        // Limpieza TOTAL de la base de datos de asistencia
        delete("/all") {
            val deleted = attendanceUseCase.deleteAllAttendance()
            call.respond(mapOf("mensaje" to "Base de datos de asistencia vaciada correctamente.", "registros_eliminados" to deleted.toString()))
        }

        // El servidor vive en la nube (Hugging Face) y la lectora Hikvision vive
        // en la red LOCAL de la planta (10.141.1.230) -> desde aqui es IMPOSIBLE
        // alcanzarla por IP directamente (no hay ruta de red). Por eso este endpoint
        // NO hace un "pull" magico a la lectora: solo informa el estado real y
        // deja claro que la sincronizacion real se logra de una de estas dos formas:
        //   1) Tiempo real: configurar en la propia lectora (menu de Linkage/ISAPI)
        //      que haga PUSH de sus eventos a esta URL -> /api/v1/asistencia/hikvision
        //   2) Historico: correr attendance_sync.py desde una PC en la red de planta
        post("/sync") {
            val result = attendanceUseCase.syncWithDevice("10.141.1.230")
            call.respond(
                SyncResult(
                    synced = result,
                    message = (
                        "El servidor esta en la nube y la lectora esta en la red local de la planta, " +
                        "por lo que no se puede jalar (pull) directo por IP. Para recibir asistencias reales: " +
                        "1) configura en la lectora el envio (push) de eventos hacia " +
                        "https://d4r005-rhnaf-industrial.hf.space/api/v1/asistencia/hikvision, o " +
                        "2) corre attendance_sync.py desde una computadora conectada a la red de la planta " +
                        "para subir el historico."
                    )
                )
            )
        }

        // Limpieza retroactiva: aplica la regla de 1 Check-in + 1 Check-out por dia
        // a los datos que ya estaban guardados antes de que existiera esta regla.
        post("/normalize") {
            val deleted = attendanceUseCase.normalizeDailyLimits()
            call.respond(mapOf("registros_eliminados" to deleted.toString(), "mensaje" to "Se dejaron solo 1 Check-in y 1 Check-out por empleado por dia."))
        }

        // Repara registros historicos con Name/Department/Attendance Status vacios
        // (llegaron antes de que existieran estas columnas, o la lectora no manda el nombre).
        // Seguro de correr varias veces.
        post("/backfill-metadata") {
            val updated = attendanceUseCase.backfillMissingMetadata()
            call.respond(mapOf("registros_actualizados" to updated.toString(), "mensaje" to "Se completaron Name/Department/Attendance Status faltantes en registros historicos."))
        }

        // Limpia registros invalidos ("None", "null", vacios) que se colaron por ruido de sistema
        post("/cleanup-invalid") {
            val deleted = DatabaseFactory.dbQuery {
                AttendanceLogTable.deleteWhere { 
                    (employeeId eq "") or 
                    (employeeId.lowerCase() eq "none") or 
                    (employeeId.lowerCase() eq "null") or 
                    (employeeId eq "0")
                }
            }
            call.respond(mapOf("registros_eliminados" to deleted.toString(), "mensaje" to "Se eliminaron registros con IDs invalidos."))
        }

        // --- GESTION DE TAREAS REMOTAS (PUENTE NUBE-PLANTA) ---

        // Crea una peticion de sincronizacion para que el script local la ejecute
        post("/request-sync") {
            val taskId = DatabaseFactory.dbQuery {
                SystemTaskTable.insert {
                    it[taskType] = "SYNC_ATTENDANCE"
                    it[status] = "PENDING"
                    it[updatedAt] = java.time.LocalDateTime.now().toString()
                } get SystemTaskTable.id
            }
            call.respond(mapOf("taskId" to taskId.toString(), "status" to "PENDING"))
        }

        // Obtiene la ultima tarea pendiente (usado por el script de Python)
        get("/poll-task") {
            val task = DatabaseFactory.dbQuery {
                SystemTaskTable.selectAll()
                    .where { SystemTaskTable.status eq "PENDING" }
                    .orderBy(SystemTaskTable.id, SortOrder.ASC)
                    .limit(1)
                    .map {
                        mapOf(
                            "id" to it[SystemTaskTable.id].toString(),
                            "type" to it[SystemTaskTable.taskType],
                            "params" to it[SystemTaskTable.params]
                        )
                    }.firstOrNull()
            }
            if (task != null) call.respond(task) else call.respond(HttpStatusCode.NoContent)
        }

        // Actualiza el estado de una tarea (usado por el script de Python)
        post("/update-task") {
            val data = call.receive<Map<String, String>>()
            val id = data["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
            val newStatus = data["status"] ?: "DONE"
            val resultMsg = data["result"] ?: ""

            DatabaseFactory.dbQuery {
                SystemTaskTable.update({ SystemTaskTable.id eq id }) {
                    it[status] = newStatus
                    it[result] = resultMsg
                    it[updatedAt] = java.time.LocalDateTime.now().toString()
                }
            }
            call.respond(HttpStatusCode.OK)
        }

        // Consulta el estado de una tarea especifica (usado por la Web)
        get("/task-status/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val status = DatabaseFactory.dbQuery {
                SystemTaskTable.selectAll().where { SystemTaskTable.id eq id }.map {
                    mapOf("status" to it[SystemTaskTable.status], "result" to it[SystemTaskTable.result])
                }.firstOrNull()
            }
            if (status != null) call.respond(status) else call.respond(HttpStatusCode.NotFound)
        }

        // ========== EXPORTACION PARA NOMINA / AUDITORIA ==========

        // Elimina registros falsos de LOCAL-SYNC de un dia especifico
        delete("/local-sync/{day}") {
            val day = call.parameters["day"] ?: java.time.LocalDate.now().toString()
            val deleted = attendanceUseCase.deleteLocalSyncForDay(day)
            call.respond(mapOf(
                "dia" to day,
                "registros_eliminados" to deleted.toString(),
                "mensaje" to "Se eliminaron " + deleted + " registros falsos de LOCAL-SYNC del dia " + day + "."
            ))
        }

        // Elimina TODOS los registros de un dia especifico
        delete("/day/{day}") {
            val day = call.parameters["day"] ?: java.time.LocalDate.now().toString()
            val deleted = attendanceUseCase.deleteAllForDay(day)
            call.respond(mapOf(
                "dia" to day,
                "registros_eliminados" to deleted.toString(),
                "mensaje" to "Se eliminaron " + deleted + " registros del dia " + day + "."
            ))
        }

        // Export CSV: 1 Check-in (mas temprano) + 1 Check-out (mas tardio) por empleado por dia
        get("/export/csv") {
            val today = java.time.LocalDate.now().toString()
            val from = call.parameters["from"] ?: today
            val to = call.parameters["to"] ?: today

            val summary = attendanceUseCase.exportDailySummary(from + "T00:00:00", to + "T23:59:59")

            val csv = buildString {
                appendLine("Employee ID,Name,Department,Date,Check-In,Check-Out,Total Checks")
                for (row in summary) {
                    val ci = row.checkIn ?: ""
                    val co = row.checkOut ?: ""
                    appendLine(row.employeeId + "," + row.name + "," + row.department + "," + row.date + "," + ci + "," + co + "," + row.totalChecks)
                }
            }

            call.response.header("Content-Disposition", "attachment; filename=reporte_asistencia_" + from + "_a_" + to + ".csv")
            call.respondText(csv, contentType = ContentType.Text.CSV)
        }

        // Export Raw CSV: todas las checadas filtradas sin agrupar
        get("/export/raw/csv") {
            val pid = call.request.queryParameters["pid"]
            val name = call.request.queryParameters["name"]
            val dept = call.request.queryParameters["dept"]
            val from = call.request.queryParameters["from"]
            val to = call.request.queryParameters["to"]

            val logs = DatabaseFactory.dbQuery {
                val query = AttendanceLogTable.selectAll()
                if (!pid.isNullOrBlank()) query.andWhere { AttendanceLogTable.employeeId eq pid }
                if (!name.isNullOrBlank()) query.andWhere { AttendanceLogTable.name.lowerCase() like "%${name.lowercase()}%" }
                if (!dept.isNullOrBlank()) query.andWhere { AttendanceLogTable.department.lowerCase() like "%${dept.lowercase()}%" }
                if (!from.isNullOrBlank()) query.andWhere { AttendanceLogTable.timestamp greaterEq from }
                if (!to.isNullOrBlank()) query.andWhere { AttendanceLogTable.timestamp lessEq to + "T23:59:59" }

                query.orderBy(AttendanceLogTable.timestamp, SortOrder.DESC)
                    .orderBy(AttendanceLogTable.id, SortOrder.DESC).map {
                    listOf(
                        it[AttendanceLogTable.employeeId],
                        it[AttendanceLogTable.name],
                        it[AttendanceLogTable.department],
                        it[AttendanceLogTable.timestamp],
                        it[AttendanceLogTable.attendanceStatus],
                        it[AttendanceLogTable.verifyMode],
                        it[AttendanceLogTable.deviceSerial]
                    ).joinToString(",")
                }
            }

            val csv = buildString {
                appendLine("Employee ID,Name,Department,Timestamp,Status,Method,Device")
                logs.forEach { appendLine(it) }
            }

            call.response.header("Content-Disposition", "attachment; filename=asistencias_raw.csv")
            call.respondText(csv, contentType = ContentType.Text.CSV)
        }

        // Export PDF: reporte HTML formateado para impresion a PDF
        get("/export/pdf") {
            val today = java.time.LocalDate.now().toString()
            val from = call.parameters["from"] ?: today
            val to = call.parameters["to"] ?: today

            val summary = attendanceUseCase.exportDailySummary(from + "T00:00:00", to + "T23:59:59")

            val sb = StringBuilder()
            sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            sb.append("<title>Reporte de Asistencia " + from + " a " + to + "</title>")
            sb.append("<style>")
            sb.append("@page { size: A4; margin: 15mm; }")
            sb.append("body { font-family: 'Helvetica', sans-serif; font-size: 11px; color: #333; }")
            sb.append("h1 { font-size: 18px; text-align: center; margin-bottom: 5px; }")
            sb.append("h2 { font-size: 12px; text-align: center; color: #666; font-weight: normal; margin-bottom: 20px; }")
            sb.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }")
            sb.append("th { background: #2c3e50; color: white; padding: 6px 8px; text-align: left; font-size: 10px; }")
            sb.append("td { padding: 5px 8px; border-bottom: 1px solid #ddd; }")
            sb.append("tr:nth-child(even) { background: #f9f9f9; }")
            sb.append(".header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }")
            sb.append(".logo { font-size: 16px; font-weight: bold; color: #2c3e50; }")
            sb.append(".date { font-size: 10px; color: #999; }")
            sb.append(".total { margin-top: 15px; font-weight: bold; text-align: right; }")
            sb.append("@media print { .no-print { display: none; } }")
            sb.append(".btn { background: #2c3e50; color: white; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; }")
            sb.append("</style>")
            sb.append("</head><body>")
            sb.append("<div class='header'><span class='logo'>NAF - Reporte de Asistencia</span>")
            val genDate = java.time.LocalDateTime.now().toString().substring(0, 16).replace("T", " ")
            sb.append("<span class='date'>Generado: " + genDate + "</span></div>")
            sb.append("<h1>Reporte de Asistencia</h1>")
            sb.append("<h2>Periodo: " + from + " a " + to + "</h2>")
            sb.append("<button class='btn no-print' onclick='window.print()'>Imprimir / Guardar PDF</button>")
            sb.append("<table><thead><tr>")
            sb.append("<th>#</th><th>Employee ID</th><th>Nombre</th><th>Departamento</th><th>Fecha</th><th>Check-In</th><th>Check-Out</th><th>Checadas</th>")
            sb.append("</tr></thead><tbody>")
            for ((index, row) in summary.withIndex()) {
                sb.append("<tr>")
                sb.append("<td>" + (index + 1) + "</td>")
                sb.append("<td>" + row.employeeId + "</td>")
                sb.append("<td>" + row.name + "</td>")
                sb.append("<td>" + row.department + "</td>")
                sb.append("<td>" + row.date + "</td>")
                val checkInTime = row.checkIn?.substringAfter("T")?.substring(0, 8) ?: "-"
                val checkOutTime = row.checkOut?.substringAfter("T")?.substring(0, 8) ?: "-"
                sb.append("<td>" + checkInTime + "</td>")
                sb.append("<td>" + checkOutTime + "</td>")
                sb.append("<td>" + row.totalChecks + "</td>")
                sb.append("</tr>")
            }
            sb.append("</tbody></table>")
            sb.append("<div class='total'>Total de registros: " + summary.size + "</div>")
            sb.append("</body></html>")

            call.respondText(sb.toString(), contentType = ContentType.Text.Html)
        }



        // Importación manual: sube el CSV que exporta el software de asistencia
        // de la lectora (mismo formato de "checadas"). Ruta de respaldo mientras
        // se resuelve el push/pull automático en tiempo real.
        post("/import-csv") {
            val rawText = call.receiveText()
            val (rows, invalidCount) = parseAttendanceCsv(rawText)

            if (rows.isEmpty()) {
                call.respond(
                    ImportResult(
                        totalRows = 0,
                        imported = 0,
                        skippedDuplicates = 0,
                        skippedInvalid = invalidCount,
                        message = "No se encontraron filas válidas. Verifica que el CSV tenga las columnas 'Person ID' y 'Time'."
                    )
                )
                return@post
            }

            val employeeIds = rows.map { it.employeeId }.distinct()
            // Procesamos en orden cronológico por empleado para que la primera checada del
            // día quede como Check-in y la segunda como Check-out (regla de 1 + 1 por día).
            val sortedRows = rows.sortedWith(compareBy({ it.employeeId }, { it.timestamp }))

            var imported = 0
            var duplicates = 0
            var cappedByDailyLimit = 0

            DatabaseFactory.dbQuery {
                // Cruzamos con la ficha del empleado (por readerId o id) para completar
                // Name/Department cuando el CSV los trae vacios.
                val employeeInfoById = HashMap<String, Pair<String, String>>()
                EmployeeTable
                    .selectAll()
                    .where { (EmployeeTable.readerId inList employeeIds) or (EmployeeTable.id inList employeeIds) }
                    .forEach { row ->
                        val fullName = "${row[EmployeeTable.firstName]} ${row[EmployeeTable.lastName]}".trim()
                        val dept = row[EmployeeTable.department]
                        row[EmployeeTable.readerId]?.let { employeeInfoById[it] = fullName to dept }
                        employeeInfoById[row[EmployeeTable.id]] = fullName to dept
                    }

                // Traemos de una sola vez las combinaciones (empleado, timestamp) ya existentes
                // para esos empleados, y así evitar duplicar checadas ya guardadas antes
                // (por ejemplo si se vuelve a subir un rango de fechas que se traslapa).
                val existingKeys = AttendanceLogTable
                    .select(AttendanceLogTable.employeeId, AttendanceLogTable.timestamp)
                    .where { AttendanceLogTable.employeeId inList employeeIds }
                    .map { "${it[AttendanceLogTable.employeeId]}|${it[AttendanceLogTable.timestamp]}" }
                    .toHashSet()

                // Conteo actual de checadas por (empleado, día) ya guardadas, para respetar
                // el límite de 1 Check-in + 1 Check-out aunque se importe en varias tandas.
                val dayCounts = HashMap<String, Int>()
                AttendanceLogTable
                    .select(AttendanceLogTable.employeeId, AttendanceLogTable.timestamp)
                    .where { AttendanceLogTable.employeeId inList employeeIds }
                    .forEach {
                        val day = it[AttendanceLogTable.timestamp].substringBefore("T").substringBefore(" ")
                        val dayKey = "${it[AttendanceLogTable.employeeId]}|$day"
                        dayCounts[dayKey] = (dayCounts[dayKey] ?: 0) + 1
                    }

                for (row in sortedRows) {
                    val key = "${row.employeeId}|${row.timestamp}"
                    if (existingKeys.contains(key)) {
                        duplicates++
                        continue
                    }

                    val day = row.timestamp.substringBefore("T").substringBefore(" ")
                    val dayKey = "${row.employeeId}|$day"
                    val countToday = dayCounts[dayKey] ?: 0

                    if (countToday >= 2) {
                        cappedByDailyLimit++
                        continue
                    }

                    val slot = if (countToday == 0) "Check-in" else "Check-out"

                    existingKeys.add(key)
                    dayCounts[dayKey] = countToday + 1

                    val empInfo = employeeInfoById[row.employeeId]
                    val resolvedName = row.name.ifBlank { empInfo?.first ?: "" }
                    val resolvedDept = row.department.ifBlank { empInfo?.second ?: "" }

                    AttendanceLogTable.insert {
                        it[employeeId] = row.employeeId
                        it[name] = resolvedName
                        it[department] = resolvedDept
                        it[timestamp] = row.timestamp
                        it[deviceSerial] = row.deviceSerial
                        it[verifyMode] = "CSV-IMPORT"
                        it[attendanceStatus] = slot
                        it[customName] = row.customName
                    }
                    imported++
                }
            }

            call.respond(
                ImportResult(
                    totalRows = rows.size + invalidCount,
                    imported = imported,
                    skippedDuplicates = duplicates,
                    skippedInvalid = invalidCount,
                    skippedDailyLimit = cappedByDailyLimit,
                    message = "Importación completa: $imported checadas nuevas guardadas (1 Check-in + 1 Check-out por día)" +
                        (if (duplicates > 0) ", $duplicates ya existían" else "") +
                        (if (cappedByDailyLimit > 0) ", $cappedByDailyLimit se rechazaron por exceder el límite diario" else "") +
                        (if (invalidCount > 0) ", $invalidCount filas inválidas" else "") + "."
                )
            )
        }
    }
}
