package com.example.rhnaf.shared.model

import kotlinx.serialization.Serializable

// ============================================================
// Listado de Lockers (casilleros de empleados) - EHS y RH
// Cada casillero tiene una asignacion vigente y una bitacora
// inmutable de TODAS las personas a quienes les ha pertenecido.
// ============================================================

@Serializable
data class Locker(
    val numero: Int = 0,
    val estado: String = "Libre",          // Libre | Asignado
    val employeeId: String = "",           // asignacion vigente
    val nombre: String = "",
    val fechaCandado: String = "",         // dd/MM/yyyy
    val llaveCopia: String = "Si",         // Si | No
    val fechaPrestamoLlave: String = "",
    val fechaDevolucionLlave: String = "",
    val notas: String = ""
)

// Una fila de la bitacora: una persona en un casillero durante un lapso.
@Serializable
data class LockerHistoryRecord(
    val id: Int = 0,
    val numero: Int = 0,
    val employeeId: String = "",
    val nombre: String = "",
    val fechaCandado: String = "",
    val llaveCopia: String = "",
    val fechaPrestamoLlave: String = "",
    val fechaDevolucionLlave: String = "",
    val fechaInicio: String = "",          // cuando se le asigno
    val fechaFin: String = "",             // cuando se libero ("" = vigente)
    val motivoFin: String = "",
    val registradoPor: String = ""
)

@Serializable
data class LockerAssignRequest(
    val employeeId: String = "",
    val nombre: String = "",
    val fechaCandado: String = "",
    val llaveCopia: String = "Si",
    val notas: String = ""
)

@Serializable
data class LockerReleaseRequest(val motivo: String = "")

@Serializable
data class LockerKeyRequest(
    val fechaPrestamoLlave: String = "",
    val fechaDevolucionLlave: String = ""
)

// Fila de importacion masiva (desde el listado maestro Excel).
@Serializable
data class LockerImportRow(
    val numero: Int = 0,
    val employeeId: String = "",
    val nombre: String = "",
    val fechaCandado: String = "",
    val llaveCopia: String = "Si",
    val fechaPrestamoLlave: String = "",
    val fechaDevolucionLlave: String = ""
)

@Serializable
data class LockerImportRequest(val filas: List<LockerImportRow> = emptyList())

@Serializable
data class LockerSummary(
    val total: Int = 0,
    val asignados: Int = 0,
    val libres: Int = 0
)
