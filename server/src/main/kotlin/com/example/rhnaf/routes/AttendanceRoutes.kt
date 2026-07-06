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

fun Route.attendanceRouting(attendanceUseCase: AttendanceUseCase) {
    route("/api/v1/asistencia") {
        
        // Este es el webhook para la lectora Hikvision
        // URL en la lectora: /api/v1/asistencia/hikvision
        post("/hikvision") {
            try {
                // 1. Cacha el JSON que la lectora manda a nafconnect.pages.dev
                val payload = call.receive<HikvisionEventRequest>()
                
                val idEmpleado = payload.AccessControllerEvent.employeeNoString
                val horaChecada = payload.dateTime
                
                // 2. Guarda el registro en la Base de Datos central del ERP via UseCase
                attendanceUseCase.registerCheckIn(
                    employeeId = idEmpleado, 
                    timestamp = horaChecada,
                    deviceSerial = payload.deviceID,
                    verifyMode = payload.AccessControllerEvent.currentVerifyMode
                )
                
                println("Asistencia guardada: Empleado $idEmpleado a las $horaChecada")
                
                // 3. Respóndele OK a la lectora (Importante para Hikvision)
                call.respond(HttpStatusCode.OK, HikvisionResponse(statusString = "OK", statusCode = 1))
                
            } catch (e: Exception) {
                application.log.error("Error procesando registro Hikvision", e)
                call.respond(HttpStatusCode.OK, HikvisionResponse(statusString = "Error", statusCode = 0))
            }
        }

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
    }
}
