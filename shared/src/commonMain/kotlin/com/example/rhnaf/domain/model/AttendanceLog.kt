package com.example.rhnaf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceLog(
    val id: String? = null,
    val employeeId: String,       // Mapea el 'employeeNoString' de la lectora
    val timestamp: String,        // ISO Date enviado por la lectora
    val deviceSerial: String,     // Identificador de la lectora ("K58575982")
    val verifyMode: String        // "face", "card", etc.
)
