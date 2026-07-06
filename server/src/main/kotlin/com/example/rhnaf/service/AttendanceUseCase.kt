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
        // En un entorno real, aquí usaríamos Ktor Client para llamar a la ISAPI de Hikvision
        // Ejemplo: GET http://admin:password@$deviceIp/ISAPI/AccessControl/AcsEvent?format=json
        
        // Simulamos la recuperación de 3 eventos que no habían sido pusheados
        val now = java.time.LocalDateTime.now().toString()
        val mockEvents = listOf("162", "163", "164") 
        
        mockEvents.forEach { id ->
            registerCheckIn(id, now, "DS-K1T341-SYNC", "Face")
        }
        
        return mockEvents.size
    }
}
