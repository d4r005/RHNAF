package com.example.rhnaf.routes

import com.example.rhnaf.api.HikvisionEventRequest
import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.DebugLogTable
import com.example.rhnaf.domain.model.AttendanceLog
import com.example.rhnaf.service.AttendanceUseCase
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

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
            attendanceUseCase.registerCheckIn(
                employeeId = employeeNo!!,
                timestamp = java.time.LocalDateTime.now().toString(),
                deviceSerial = deviceId,
                verifyMode = verifyMode
            )
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
                mapOf(
                    "synced" to result,
                    "message" to (
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
    }
}
