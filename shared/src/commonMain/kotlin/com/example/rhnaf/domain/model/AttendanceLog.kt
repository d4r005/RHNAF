package com.example.rhnaf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceLog(
    val id: String? = null,
    val employeeId: String,          // Person ID
    val name: String = "",           // Name
    val department: String = "",     // Department
    val timestamp: String,           // Time (ISO)
    val attendanceStatus: String = "", // Attendance Status (Check-in / Check-out)
    val deviceSerial: String,        // Attendance Check Point
    val verifyMode: String,          // Metodo de verificacion (Face, Card, CSV-IMPORT...)
    val customName: String = ""      // Custom Name
)
