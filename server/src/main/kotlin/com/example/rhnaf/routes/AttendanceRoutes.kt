package com.example.rhnaf.routes

import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.domain.model.AttendanceLog
import com.example.rhnaf.service.AttendanceUseCase
import com.example.rhnaf.database.DebugLogTable
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.*

fun Route.attendanceRouting(attendanceUseCase: AttendanceUseCase) {
    
    suspend fun handleHikvisionRequest(call: ApplicationCall) {
        val rawBody = call.receiveText()
        val clientIp = call.request.origin.remoteAddress
        
        DatabaseFactory.dbQuery {
            DebugLogTable.insert {
                it[timestamp] = java.time.LocalDateTime.now().toString()
                it[rawContent] = "HIK-POST | BODY: $rawBody"
                it[sourceIp] = clientIp
            }
        }
        
        val idEmpleado = if (rawBody.contains("employeeNoString")) {
            rawBody.substringAfter("employeeNoString\":\"").substringBefore("\"")
        } else if (rawBody.contains("<employeeNo>")) {
            rawBody.substringAfter("<employeeNo>").substringBefore("</employeeNo>")
        } else "UNKNOWN"

        if (idEmpleado != "UNKNOWN" && idEmpleado.isNotEmpty()) {
            attendanceUseCase.registerCheckIn(idEmpleado, java.time.LocalDateTime.now().toString(), "HIK-WEB", "Face")
        }
        
        call.respondText("{\"statusString\":\"OK\",\"statusCode\":1}", contentType = ContentType.Application.Json)
    }

    route("/hikvision") {
        post { handleHikvisionRequest(call) }
        get { call.respondText("Radar raíz activo") }
    }

    route("/api/v1/asistencia") {
        post("/hikvision") { handleHikvisionRequest(call) }

        get("/debug") {
            try {
                DatabaseFactory.dbQuery {
                    DebugLogTable.insert {
                        it[timestamp] = java.time.LocalDateTime.now().toString()
                        it[rawContent] = "DEBUG PAGE VISIT"
                        it[sourceIp] = call.request.origin.remoteAddress
                    }
                }
                val debug = DatabaseFactory.dbQuery {
                    DebugLogTable.selectAll().orderBy(DebugLogTable.id, SortOrder.DESC).limit(20).map {
                        "${it[DebugLogTable.timestamp]} | ${it[DebugLogTable.sourceIp]} | ${it[DebugLogTable.rawContent]}"
                    }
                }
                call.respond(debug)
            } catch (e: Exception) {
                call.respond(listOf("Error: ${e.message}"))
            }
        }

        get("/logs") {
            try {
                val logs = DatabaseFactory.dbQuery {
                    AttendanceLogTable.selectAll().map {
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
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
