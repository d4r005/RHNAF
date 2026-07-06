package com.example.rhnaf.service

import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import org.jetbrains.exposed.sql.insert

class AttendanceUseCase {
    suspend fun registerCheckIn(employeeId: String, timestamp: String, deviceSerial: String = "UNKNOWN", verifyMode: String = "UNKNOWN") {
        DatabaseFactory.dbQuery {
            AttendanceLogTable.insert {
                it[AttendanceLogTable.employeeId] = employeeId
                it[AttendanceLogTable.timestamp] = timestamp
                it[AttendanceLogTable.deviceSerial] = deviceSerial
                it[AttendanceLogTable.verifyMode] = verifyMode
            }
        }
    }

    suspend fun syncWithDevice(deviceIp: String): Int {
        // IP de la lectora configurada: 10.141.1.230
        // En un entorno real, aquí usaríamos Ktor Client para llamar a la ISAPI de Hikvision
        // Ejemplo: GET http://admin:password@$deviceIp/ISAPI/AccessControl/AcsEvent?format=json
        
        // Simulamos la recuperación de eventos recientes con formato de fecha correcto
        val now = java.time.LocalDateTime.now()
        val mockEvents = listOf(
            "162" to now.minusMinutes(5).toString(),
            "163" to now.minusMinutes(10).toString(),
            "164" to now.minusMinutes(15).toString()
        ) 
        
        mockEvents.forEach { (id, time) ->
            registerCheckIn(id, time, "DS-K1T341-SYNC", "Face")
        }
        
        return mockEvents.size
    }
}
