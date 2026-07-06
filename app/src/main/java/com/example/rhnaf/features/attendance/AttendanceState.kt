package com.example.rhnaf.features.attendance

import com.example.rhnaf.data.local.entities.AttendanceLogEntity

data class AttendanceState(
    val isLoading: Boolean = false,
    val logs: List<AttendanceLogEntity> = emptyList(),
    val targetEmployeeId: String = "",
    val nextCheckType: String = "ENTRADA", // Sugerencia dinámica en base al último registro
    val message: String? = null
)
