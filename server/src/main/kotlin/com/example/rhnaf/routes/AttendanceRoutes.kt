package com.example.rhnaf.routes

import com.example.rhnaf.api.HikvisionEventRequest
import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.DebugLogTable
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

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

// Estructura de una fila ya normalizada, lista para insertar
private data class ImportedAttendanceRow(
    val employeeId: String,
    val timestamp: String,
    val deviceSerial: String,
    val status: String
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
    val idxTime = colIndex("time")
    val idxStatus = colIndex("attendance status", "status")
    val idxCheckPoint = colIndex("attendance check point", "check point", "device")

    val dataLines = lines.drop(1)
    var invalidCount = 0

    val rows = dataLines.mapNotNull { line ->
        val parts = line.split(",")
        val personId = parts.getOrNull(if (idxPersonId >= 0) idxPersonId else 0)
            ?.trim()?.trimStart('\'')
        val time = parts.getOrNull(if (idxTime >= 0) idxTime else 3)?.trim()
        val status = parts.getOrNull(if (idxStatus >= 0) idxStatus else 4)?.trim() ?: "Check-in"
        val checkPoint = parts.getOrNull(if (idxCheckPoint >= 0) idxCheckPoint else 5)?.trim()
            ?.ifBlank { "CSV-IMPORT" } ?: "CSV-IMPORT"

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
            timestamp = isoTimestamp,
            deviceSerial = checkPoint,
            status = status
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
        var deviceId = "HIK-WEB"
        var verifyMode = "Face"

        // 1) Intentamos deserializar el JSON estructurado (formato ISAPI estándar)
        runCatching {
            val event = lenientJson.decodeFromString<HikvisionEventRequest>(rawBody)
            employeeNo = event.AccessControllerEvent.employeeNoString
            deviceId = event.deviceID
            verifyMode = event.AccessControllerEvent.currentVerifyMode
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
        }

        if (!employeeNo.isNullOrBlank()) {
            val saved = attendanceUseCase.registerCheckIn(
                employeeId = employeeNo!!,
                timestamp = java.time.LocalDateTime.now().toString(),
                deviceSerial = deviceId,
                verifyMode = verifyMode
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
            val logs = DatabaseFactory.dbQuery {
                AttendanceLogTable.selectAll().orderBy(AttendanceLogTable.id, SortOrder.DESC).map {
                    AttendanceLog(
                        id = it[AttendanceLogTable.id].toString(),
                        employeeId = it[AttendanceLogTable.employeeId],
                        timestamp = it[AttendanceLogTable.timestamp],
                        deviceSerial = it[AttendanceLogTable.deviceSerial],
                        verifyMode = it[AttendanceLogTable.verifyMode]
                    )
                }
            }
            call.respond(logs)
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

                    AttendanceLogTable.insert {
                        it[employeeId] = row.employeeId
                        it[timestamp] = row.timestamp
                        it[deviceSerial] = row.deviceSerial
                        it[verifyMode] = slot
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
