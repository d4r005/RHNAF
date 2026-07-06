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
        
        // Eliminamos los datos de prueba (mock) para mostrar solo información verídica
        // que la lectora envíe vía Push a este servidor.
        
        return 0 // Retornamos 0 ya que la sincronización manual vía IP local no es posible desde la nube
    }
}
