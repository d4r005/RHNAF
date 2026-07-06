package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val id: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String? = null,
    
    // Identidad y Legal
    val curp: String? = null,
    val rfc: String? = null,
    val nss: String? = null,
    val ine: String? = null,
    val license: String? = null,
    val readerId: String? = null, // ID vinculado en la lectora facial (ej. employeeNo)
    
    // Estructura Organizacional
    val position: String,
    val department: String,
    val supervisor: String? = null,
    val entryDate: String,
    val seniority: String? = null,
    val contractType: String? = null,
    val salary: Double? = null,
    
    // Datos Personales
    val maritalStatus: String? = null,
    val emergencyContact: String? = null,
    val email: String? = null,
    val phone: String? = null,
    
    // Historial y Salud
    val status: EmployeeStatus = EmployeeStatus.ACTIVE,
    val disciplinaryHistory: List<String> = emptyList(),
    val medicalHistory: String? = null,
    val certifications: List<String> = emptyList(),
    val skills: List<String> = listOf("Operación", "Seguridad"),
    val attritionRisk: Double = 0.15, // 0.0 to 1.0 (Mock predictive AI value)
    
    // EHS y Equipo
    val equipmentAssigned: List<String> = emptyList(),
    val trainingCompleted: List<String> = emptyList()
)

@Serializable
enum class EmployeeStatus {
    ACTIVE, INACTIVE, VACATION, LEAVE, SUSPENDED
}
