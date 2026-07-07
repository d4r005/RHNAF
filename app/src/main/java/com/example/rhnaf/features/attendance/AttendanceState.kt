package com.example.rhnaf.features.attendance

import com.example.rhnaf.data.local.entities.AttendanceLogEntity
import com.example.rhnaf.domain.model.AttendanceLog

data class AttendanceState(
    val isLoading: Boolean = false,
    val logs: List<AttendanceLogEntity> = emptyList(),
    // Checadas reales tomadas del servidor central (lectora facial + capturas manuales
    // de todo el personal). Esto es lo que "refleja" la asistencia de la planta.
    val remoteLogs: List<AttendanceLog> = emptyList(),
    val targetEmployeeId: String = "",
    val nextCheckType: String = "ENTRADA", // Sugerencia dinámica en base al último registro
    val message: String? = null
)
