package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object AttendanceLogTable : Table("attendance_logs") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 100)
    val name = varchar("name", 200).default("")
    val department = varchar("department", 150).default("")
    val timestamp = varchar("timestamp", 50)
    val attendanceStatus = varchar("attendance_status", 30).default("")
    // Ampliados: la lectora real manda deviceName/currentVerifyMode mas largos que los
    // valores de prueba (ej. "faceOrCardOrFpOrPw"), lo que causaba
    // "value too long for type character varying" -> 500 al sincronizar historico real.
    val deviceSerial = varchar("device_serial", 150)
    val verifyMode = varchar("verify_mode", 100)
    val customName = varchar("custom_name", 200).default("")

    override val primaryKey = PrimaryKey(id)
}
