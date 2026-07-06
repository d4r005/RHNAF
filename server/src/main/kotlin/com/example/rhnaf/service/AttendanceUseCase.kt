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
}
