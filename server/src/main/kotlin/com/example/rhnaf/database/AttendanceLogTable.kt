package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object AttendanceLogTable : Table("attendance_logs") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 50)
    val name = varchar("name", 200).default("")
    val department = varchar("department", 150).default("")
    val timestamp = varchar("timestamp", 50)
    val attendanceStatus = varchar("attendance_status", 30).default("")
    val deviceSerial = varchar("device_serial", 50)
    val verifyMode = varchar("verify_mode", 30)
    val customName = varchar("custom_name", 200).default("")

    override val primaryKey = PrimaryKey(id)
}
