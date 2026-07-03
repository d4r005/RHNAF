package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val id: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String? = null,
    val curp: String? = null,
    val rfc: String? = null,
    val nss: String? = null,
    val ine: String? = null,
    val license: String? = null,
    val position: String,
    val department: String,
    val supervisor: String? = null,
    val entryDate: String,
    val seniority: String? = null,
    val contractType: String? = null,
    val salary: Double? = null,
    val maritalStatus: String? = null,
    val emergencyContact: String? = null,
    val status: EmployeeStatus = EmployeeStatus.ACTIVE,
    val email: String? = null,
    val phone: String? = null,
    val disciplinaryHistory: List<String> = emptyList(),
    val medicalHistory: String? = null
)

enum class EmployeeStatus {
    ACTIVE, INACTIVE, VACATION, LEAVE
}
