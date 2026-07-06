package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object AttendanceLogTable : Table("attendance_logs") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 50)
    val timestamp = varchar("timestamp", 50)
    val deviceSerial = varchar("device_serial", 50)
    val verifyMode = varchar("verify_mode", 20)
    
    override val primaryKey = PrimaryKey(id)
}
