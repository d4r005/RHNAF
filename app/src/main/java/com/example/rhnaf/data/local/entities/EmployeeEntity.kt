package com.example.rhnaf.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
    val curp: String?,
    val rfc: String?,
    val nss: String?,
    val ine: String?,
    val license: String?,
    val position: String,
    val department: String,
    val supervisor: String?,
    val entryDate: String,
    val seniority: String?,
    val contractType: String?,
    val salary: Double?,
    val maritalStatus: String?,
    val emergencyContact: String?,
    val status: EmployeeStatus,
    val email: String?,
    val phone: String?,
    val disciplinaryHistory: List<String>,
    val medicalHistory: String?
)

fun EmployeeEntity.toDomain() = Employee(
    id = id,
    firstName = firstName,
    lastName = lastName,
    photoUrl = photoUrl,
    curp = curp,
    rfc = rfc,
    nss = nss,
    ine = ine,
    license = license,
    position = position,
    department = department,
    supervisor = supervisor,
    entryDate = entryDate,
    seniority = seniority,
    contractType = contractType,
    salary = salary,
    maritalStatus = maritalStatus,
    emergencyContact = emergencyContact,
    status = status,
    email = email,
    phone = phone,
    disciplinaryHistory = disciplinaryHistory,
    medicalHistory = medicalHistory
)

fun Employee.toEntity() = EmployeeEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    photoUrl = photoUrl,
    curp = curp,
    rfc = rfc,
    nss = nss,
    ine = ine,
    license = license,
    position = position,
    department = department,
    supervisor = supervisor,
    entryDate = entryDate,
    seniority = seniority,
    contractType = contractType,
    salary = salary,
    maritalStatus = maritalStatus,
    emergencyContact = emergencyContact,
    status = status,
    email = email,
    phone = phone,
    disciplinaryHistory = disciplinaryHistory,
    medicalHistory = medicalHistory
)
