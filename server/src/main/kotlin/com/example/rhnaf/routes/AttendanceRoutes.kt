package com.example.rhnaf.routes

import com.example.rhnaf.api.HikvisionEventRequest
import com.example.rhnaf.api.HikvisionResponse
import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.domain.model.AttendanceLog
import com.example.rhnaf.service.AttendanceUseCase
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.SortOrder
import com.example.rhnaf.database.DebugLogTable

import io.ktor.server.plugins.*
import io.ktor.server.util.*

fun Route.attendanceRouting(attendanceUseCase: AttendanceUseCase) {
    // RUTA DE NIVEL RAÍZ PARA MÁXIMA COMPATIBILIDAD CON LECTORAS
    route("/hikvision") {
        post {
            val rawBody = call.receiveText()
            val clientIp = call.request.origin.remoteAddress
            
            DatabaseFactory.dbQuery {
                DebugLogTable.insert {
                    it[timestamp] = java.time.LocalDateTime.now().toString()
                    it[rawContent] = "RAIZ-POST | BODY: $rawBody"
                    it[sourceIp] = clientIp
                }
            }
            
            // Extraer ID si es posible
            val idEmpleado = if (rawBody.contains("employeeNoString")) {
                rawBody.substringAfter("employeeNoString\":\"").substringBefore("\"")
            } else if (rawBody.contains("<employeeNo>")) {
                rawBody.substringAfter("<employeeNo>").substringBefore("</employeeNo>")
            } else "UNKNOWN"

            if (idEmpleado != "UNKNOWN") {
                attendanceUseCase.registerCheckIn(idEmpleado, java.time.LocalDateTime.now().toString(), "HIK-ROOT", "Face")
            }
            
            call.respondText("{\"statusString\":\"OK\",\"statusCode\":1}", contentType = io.ktor.http.ContentType.Application.Json)
        }
        
        get {
            call.respondText("Radar NAF activo en puerto raíz. Esperando datos POST de la lectora.")
        }
    }

    route("/api/v1/asistencia") {
        // Loguear visitas al debug para probar que el radar funciona
        get("/debug") {
            try {
                DatabaseFactory.dbQuery {
                    DebugLogTable.insert {
                        it[timestamp] = java.time.LocalDateTime.now().toString()
                        it[rawContent] = "PÁGINA DEBUG VISITADA DESDE NAVEGADOR"
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
        
        // ... (resto de rutas de logs se mantienen igual)

        post("/sync") {
            try {
                // Sincronización manual con la lectora Hikvision
                val count = attendanceUseCase.syncWithDevice("10.141.1.230")
                call.respond(mapOf("status" to "success", "syncedCount" to count))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Endpoint para que la App Android y Web consulten los logs
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

        get("/debug") {
            try {
                val debug = DatabaseFactory.dbQuery {
                    DebugLogTable.selectAll().orderBy(DebugLogTable.id, SortOrder.DESC).limit(10).map {
                        "${it[DebugLogTable.timestamp]} | ${it[DebugLogTable.sourceIp]} | ${it[DebugLogTable.rawContent]}"
                    }
                }
                call.respond(debug)
            } catch (e: Exception) {
                call.respond(listOf("Error consultando debug: ${e.message}"))
            }
        }
    }
}
