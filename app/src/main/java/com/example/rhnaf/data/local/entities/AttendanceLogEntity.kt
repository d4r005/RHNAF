package com.example.rhnaf.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val type: AttendanceType
)

enum class AttendanceType {
    CLOCK_IN, CLOCK_OUT
}
