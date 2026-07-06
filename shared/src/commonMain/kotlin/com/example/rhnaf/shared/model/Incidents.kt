package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyIncident(
    val employeeId: String,
    val weekNumber: Int,
    val year: Int,
    val attendance: List<String>, // L, M, M, J, V, S, D (e.g., "1", "0", "V", "D")
    val punctualityBonus: Double = 0.0,
    val attendanceBonus: Double = 0.0,
    val sundayPremium: Double = 0.0,
    val extraHours: Double = 0.0,
    val foodAllowance: Double = 0.0,
    val weekendBonus: Double = 0.0,
    val perfectAttendance: Boolean = false,
    val absences: Int = 0,
    val observations: String = "",
    val deductions: Double = 0.0,
    val pending: Double = 0.0,
    val infonavit: Double = 0.0,
    val otherDiscounts: Double = 0.0
)

@Serializable
data class AttendanceReport(
    val month: Int,
    val year: Int,
    val totalAbsences: Int,
    val attendancePercentage: Double,
    val employeeName: String
)
